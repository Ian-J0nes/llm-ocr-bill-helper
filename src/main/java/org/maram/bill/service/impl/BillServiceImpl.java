package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.entity.Bill;
import org.maram.bill.entity.BillCategory;
import org.maram.bill.mapper.BillMapper;
import org.maram.bill.service.BillCategoryService;
import org.maram.bill.service.BillService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 账单服务实现类
 */
@Service
@Slf4j
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements BillService {

    private static final String ERROR_USER_ID_REQUIRED = "用户ID不能为空";

    private final BillCategoryService billCategoryService;

    public BillServiceImpl(BillCategoryService billCategoryService) {
        this.billCategoryService = billCategoryService;
    }

    @Override
    public Bill getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<Bill> listByUserId(Long userId) {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("issue_date", "create_time"); // 保持排序
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public Long saveAndReturnId(Bill bill) {
        if (bill.getUserId() == null) {
            log.error("保存账单失败: 用户ID为空");
            throw new IllegalArgumentException(ERROR_USER_ID_REQUIRED);
        }
        
        int result = baseMapper.insert(bill);
        if (result > 0 && bill.getId() != null) {
            return bill.getId();
        }
        
        log.error("账单保存失败 (insert返回0或ID未回填)");
        return null;
    }

    @Override
    @Transactional
    public boolean updateById(Bill bill) {
        Bill existingBill = baseMapper.selectById(bill.getId());
        if (existingBill == null) {
            log.warn("更新账单失败: 未找到ID为 {} 的账单", bill.getId());
            return false;
        }

        BeanUtils.copyProperties(bill, existingBill, "id", "userId", "createTime", "updatedTime", "deleted");

        return baseMapper.updateById(existingBill) > 0;
    }

    @Override
    @Transactional
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 分页查询用户账单
     *
     * @param pageRequest 分页请求
     * @param userId 用户ID
     * @param transactionType 交易类型
     * @param billType 账单类型
     * @param categoryId 分类ID
     * @param startDateStr 开始日期字符串 (yyyy-MM-dd)
     * @param endDateStr 结束日期字符串 (yyyy-MM-dd)
     * @return 账单分页结果
     */
    @Override
    public Page<Bill> pageUserBills(Page<Bill> pageRequest, Long userId, String transactionType, String billType, Long categoryId, String startDateStr, String endDateStr) {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        if (StringUtils.hasText(transactionType)) {
            queryWrapper.eq("transaction_type", transactionType);
        }
        if (StringUtils.hasText(billType)) {
            queryWrapper.eq("bill_type", billType);
        }
        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId);
        }

        log.info("分页查询账单: userId={}, transactionType={}, billType={}, categoryId={}, startDate={}, endDate={}",
                userId, transactionType, billType, categoryId, startDateStr, endDateStr);

        try {
            if (StringUtils.hasText(startDateStr)) {
                LocalDate startDate = LocalDate.parse(startDateStr);
                queryWrapper.apply("issue_date >= {0}", startDate);
                log.info("应用开始日期过滤: >= {}", startDate);
            }
            if (StringUtils.hasText(endDateStr)) {
                LocalDate endDate = LocalDate.parse(endDateStr);
                queryWrapper.apply("issue_date <= {0}", endDate);
                log.info("应用结束日期过滤: <= {}", endDate);
            }
        } catch (DateTimeParseException e) {
            log.error("日期格式解析错误: startDate={}, endDate={}", startDateStr, endDateStr, e);
        }

        queryWrapper.orderByDesc("issue_date", "id");

        log.info("生成的SQL查询条件: {}", queryWrapper.getCustomSqlSegment());

        return baseMapper.selectPage(pageRequest, queryWrapper);
    }

    @Override
    public Bill getByFileId(Long fileId) {
        if (fileId == null) {
            return null;
        }
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id", fileId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean existsByFileId(Long fileId) {
        if (fileId == null) {
            return false;
        }
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id", fileId);
        return baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public List<Bill> list() {
        return baseMapper.selectList(null);
    }

    @Override
    public boolean isValidCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            return true; // 允许分类为空
        }

        BillCategory category = billCategoryService.getById(categoryId);
        if (category == null) {
            return false;
        }

        // 检查是否是系统分类或用户自己的分类
        return category.getUserId() == null || userId.equals(category.getUserId());
    }

    @Override
    public long countBillsByCategory(Long categoryId, Long userId) {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId);
        } else {
            queryWrapper.isNull("category_id");
        }
        return baseMapper.selectCount(queryWrapper);
    }

    @Override
    public List<Bill> listByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        if (startDate != null) {
            queryWrapper.ge("issue_date", startDate);
        }
        if (endDate != null) {
            queryWrapper.le("issue_date", endDate);
        }
        queryWrapper.orderByDesc("issue_date");
        return baseMapper.selectList(queryWrapper);
    }
}