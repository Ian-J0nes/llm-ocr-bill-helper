package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.entity.UserBudget;
import org.maram.bill.mapper.UserBudgetMapper;
import org.maram.bill.service.UserBudgetService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户预算服务实现类
 */
@Service
@Slf4j
public class UserBudgetServiceImpl extends ServiceImpl<UserBudgetMapper, UserBudget> implements UserBudgetService {

    private static final String ERROR_USER_ID_REQUIRED = "用户ID不能为空";
    private static final String ERROR_BUDGET_AMOUNT_INVALID = "预算金额必须大于0";
    private static final String ERROR_BUDGET_TYPE_REQUIRED = "预算类型不能为空";
    private static final String ERROR_BUDGET_TYPE_INVALID = "无效的预算类型";
    private static final String ERROR_START_DATE_REQUIRED = "开始日期不能为空";
    private static final String ERROR_END_DATE_REQUIRED = "结束日期不能为空";
    private static final String ERROR_DATE_RANGE_INVALID = "开始日期不能晚于结束日期";
    private static final String ERROR_THRESHOLD_INVALID = "预警阈值必须在0-100之间";
    private static final String ERROR_TIME_CONFLICT = "该时间段已存在相同类型的预算";

    private static final int THRESHOLD_MIN = 0;
    private static final int THRESHOLD_MAX = 100;

    @Override
    public UserBudget getById(Long id) {
        log.debug("根据ID获取预算: {}", id);
        UserBudget budget = baseMapper.selectById(id);
        return budget != null ? calculateBudgetUsage(budget) : null;
    }

    @Override
    public List<UserBudget> listByUserId(Long userId) {
        log.debug("获取用户预算列表: userId={}", userId);
        List<UserBudget> budgets = baseMapper.selectByUserId(userId);
        return calculateBudgetUsageList(budgets);
    }

    @Override
    public List<UserBudget> listByUserIdAndType(Long userId, String budgetType) {
        log.debug("获取用户预算: userId={}, type={}", userId, budgetType);
        List<UserBudget> budgets = baseMapper.selectByUserIdAndType(userId, budgetType);
        return calculateBudgetUsageList(budgets);
    }

    @Override
    public List<UserBudget> getActiveBudgets(Long userId, LocalDate date) {
        log.debug("获取有效预算: userId={}, date={}", userId, date);
        List<UserBudget> budgets = baseMapper.selectActiveByUserIdAndDate(userId, date);
        return calculateBudgetUsageList(budgets);
    }

    @Override
    public List<UserBudget> getCurrentActiveBudgets(Long userId) {
        return getActiveBudgets(userId, LocalDate.now());
    }

    @Override
    public Page<UserBudget> page(Page<UserBudget> page, Long userId) {
        log.debug("分页查询用户预算: userId={}, page={}, size={}", userId, page.getCurrent(), page.getSize());
        
        LambdaQueryWrapper<UserBudget> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBudget::getUserId, userId)
                   .orderByDesc(UserBudget::getStartDate);
                   
        Page<UserBudget> result = baseMapper.selectPage(page, queryWrapper);
        result.setRecords(calculateBudgetUsageList(result.getRecords()));
        
        return result;
    }

    @Override
    public boolean save(UserBudget userBudget) {
        log.debug("保存预算: {}", userBudget);
        
        validateBudgetOrThrow(userBudget);
        checkTimeConflictOrThrow(userBudget.getUserId(), userBudget.getBudgetType(), 
                                 userBudget.getStartDate(), userBudget.getEndDate(), null);
        
        return baseMapper.insert(userBudget) > 0;
    }

    @Override
    public boolean updateById(UserBudget userBudget) {
        log.debug("更新预算: {}", userBudget);
        
        validateBudgetOrThrow(userBudget);
        checkTimeConflictOrThrow(userBudget.getUserId(), userBudget.getBudgetType(), 
                                 userBudget.getStartDate(), userBudget.getEndDate(), userBudget.getId());
        
        return baseMapper.updateById(userBudget) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        log.debug("删除预算: {}", id);
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public String validateBudget(UserBudget userBudget) {
        if (userBudget.getUserId() == null) {
            return ERROR_USER_ID_REQUIRED;
        }
        
        if (userBudget.getBudgetAmount() == null || userBudget.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ERROR_BUDGET_AMOUNT_INVALID;
        }
        
        if (!StringUtils.hasText(userBudget.getBudgetType())) {
            return ERROR_BUDGET_TYPE_REQUIRED;
        }
        
        if (UserBudget.BudgetType.getByCode(userBudget.getBudgetType()) == null) {
            return ERROR_BUDGET_TYPE_INVALID;
        }
        
        if (userBudget.getStartDate() == null) {
            return ERROR_START_DATE_REQUIRED;
        }
        
        if (userBudget.getEndDate() == null) {
            return ERROR_END_DATE_REQUIRED;
        }
        
        if (userBudget.getStartDate().isAfter(userBudget.getEndDate())) {
            return ERROR_DATE_RANGE_INVALID;
        }
        
        if (userBudget.getAlertThreshold() != null) {
            int percentage = userBudget.getAlertThreshold().intValue();
            if (percentage < THRESHOLD_MIN || percentage > THRESHOLD_MAX) {
                return ERROR_THRESHOLD_INVALID;
            }
        }
        
        return null;
    }

    @Override
    public boolean hasConflictingBudgets(Long userId, String budgetType, LocalDate startDate, LocalDate endDate, Long excludeId) {
        long count = baseMapper.countConflictingBudgets(userId, budgetType, startDate, endDate, excludeId);
        return count > 0;
    }

    @Override
    public UserBudget calculateBudgetUsage(UserBudget userBudget) {
        if (userBudget == null) {
            return null;
        }
        
        BigDecimal usedAmount = baseMapper.selectExpenseAmountByDateRange(
            userBudget.getUserId(), userBudget.getStartDate(), userBudget.getEndDate());
        
        userBudget.setUsedAmount(usedAmount != null ? usedAmount : BigDecimal.ZERO);
        userBudget.setRemainingAmount(userBudget.calculateRemainingAmount());
        userBudget.setUsagePercentage(userBudget.calculateUsagePercentage());
        userBudget.setIsOverBudget(userBudget.checkIsOverBudget());
        userBudget.setIsNearThreshold(userBudget.checkIsNearThreshold());
        
        return userBudget;
    }

    @Override
    public BudgetStatistics getBudgetStatistics(Long userId) {
        log.debug("获取预算统计: userId={}", userId);
        
        List<UserBudget> allBudgets = listByUserId(userId);
        List<UserBudget> activeBudgets = getCurrentActiveBudgets(userId);
        
        int totalBudgets = allBudgets.size();
        int activeBudgetsCount = activeBudgets.size();
        
        long overBudgets = activeBudgets.stream()
            .filter(budget -> Boolean.TRUE.equals(budget.getIsOverBudget()))
            .count();
        
        long nearThresholdBudgets = activeBudgets.stream()
            .filter(budget -> Boolean.TRUE.equals(budget.getIsNearThreshold()))
            .count();
        
        return new BudgetStatistics(totalBudgets, activeBudgetsCount, (int) overBudgets, (int) nearThresholdBudgets);
    }

    @Override
    public List<UserBudget> getExpiringBudgets(Long userId) {
        log.debug("获取即将到期预算: userId={}", userId);
        List<UserBudget> budgets = baseMapper.selectExpiringBudgets(userId);
        return calculateBudgetUsageList(budgets);
    }

    @Override
    public List<UserBudget> checkBudgetAlerts(Long userId) {
        log.debug("检查预算预警: userId={}", userId);
        List<UserBudget> activeBudgets = getCurrentActiveBudgets(userId);
        
        return activeBudgets.stream()
            .filter(budget -> Boolean.TRUE.equals(budget.getIsOverBudget()) 
                           || Boolean.TRUE.equals(budget.getIsNearThreshold()))
            .collect(Collectors.toList());
    }

    private List<UserBudget> calculateBudgetUsageList(List<UserBudget> budgets) {
        return budgets.stream()
            .map(this::calculateBudgetUsage)
            .collect(Collectors.toList());
    }

    private void validateBudgetOrThrow(UserBudget userBudget) {
        String validationError = validateBudget(userBudget);
        if (validationError != null) {
            log.warn("预算数据验证失败: {}", validationError);
            throw new IllegalArgumentException(validationError);
        }
    }

    private void checkTimeConflictOrThrow(Long userId, String budgetType, LocalDate startDate, LocalDate endDate, Long excludeId) {
        if (hasConflictingBudgets(userId, budgetType, startDate, endDate, excludeId)) {
            log.warn("预算时间冲突: userId={}, type={}, startDate={}, endDate={}", userId, budgetType, startDate, endDate);
            throw new IllegalArgumentException(ERROR_TIME_CONFLICT);
        }
    }
}
