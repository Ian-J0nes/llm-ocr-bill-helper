package org.maram.bill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.entity.UserBudget;
import org.maram.bill.service.UserBudgetService;
import org.maram.bill.common.utils.Result;
import org.maram.bill.common.utils.ResultCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户预算控制器
 */
@RestController
@RequestMapping("/user-budget")
@Slf4j
@RequiredArgsConstructor
public class UserBudgetController {

    private final UserBudgetService userBudgetService;
    private final UserContext userContext;

    /**
     * 获取用户预算列表
     */
    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) String budgetType,
            @RequestParam(value = "current", required = false) Long current,
            @RequestParam(value = "size", required = false) Long size) {
        return withUser(userId -> {
            if (current == null && size == null) {
                log.info("获取用户ID: {} 的预算列表, 类型: {}", userId, budgetType);
                List<UserBudget> budgets = (budgetType != null && !budgetType.trim().isEmpty())
                        ? userBudgetService.listByUserIdAndType(userId, budgetType)
                        : userBudgetService.listByUserId(userId);
                return Result.success(budgets);
            }

            long pageNum = (current == null || current <= 0) ? 1 : current;
            long pageSize = (size == null || size <= 0) ? 10 : size;
            log.info("分页查询用户ID: {} 的预算: 第{}页, 每页{}条", userId, pageNum, pageSize);
            Page<UserBudget> pageRequest = new Page<>(pageNum, pageSize);
            Page<UserBudget> budgetPage = userBudgetService.page(pageRequest, userId);
            return Result.success(budgetPage);
        });
    }

    /**
     * 根据ID获取预算详情
     */
    @GetMapping("/{id}")
    public Result<UserBudget> getById(@PathVariable Long id) {
        return withBudget(id, (userId, budget) -> {
            log.info("用户ID: {} 获取预算详情: {}", userId, id);
            return Result.success(budget);
        });
    }

    /**
     * 新增预算
     */
    @PostMapping
    public Result<String> add(@Valid @RequestBody UserBudget userBudget) {
        return withUser(userId -> {
            userBudget.setUserId(userId);
            log.info("用户ID: {} 新增预算: {}", userId, userBudget);
            boolean success = userBudgetService.save(userBudget);
            if (success) {
                return Result.success("预算创建成功");
            }
            log.warn("用户ID: {} 新增预算失败", userId);
            return Result.error("预算创建失败");
        });
    }

    /**
     * 更新预算
     */
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @Valid @RequestBody UserBudget userBudget) {
        userBudget.setId(id);
        if (userBudget.getId() == null) {
            log.warn("更新预算失败：预算ID不能为空");
            return Result.error("预算ID不能为空");
        }
        return withBudget(userBudget.getId(), (userId, existingBudget) -> {
            userBudget.setUserId(userId);
            log.info("用户ID: {} 更新预算: {}", userId, userBudget);
            boolean success = userBudgetService.updateById(userBudget);
            if (success) {
                return Result.success("预算更新成功");
            }
            log.warn("用户ID: {} 更新预算失败", userId);
            return Result.error("预算更新失败");
        });
    }

    /**
     * 删除预算
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return withBudget(id, (userId, existingBudget) -> {
            log.info("用户ID: {} 删除预算: {}", userId, id);
            boolean success = userBudgetService.removeById(id);
            if (success) {
                log.info("用户ID: {} 删除预算成功: {}", userId, id);
                return Result.success("预算删除成功");
            }
            log.warn("用户ID: {} 删除预算失败: {}", userId, id);
            return Result.error("预算删除失败");
        });
    }

    /**
     * 获取当前有效预算
     */
    @GetMapping("/active")
    public Result<List<UserBudget>> getActiveBudgets() {
        return withUser(userId -> Result.success(userBudgetService.getCurrentActiveBudgets(userId)));
    }

    /**
     * 获取预算统计信息
     */
    @GetMapping("/statistics")
    public Result<UserBudgetService.BudgetStatistics> getStatistics() {
        return withUser(userId -> Result.success(userBudgetService.getBudgetStatistics(userId)));
    }

    /**
     * 获取预算预警信息
     */
    @GetMapping("/alerts")
    public Result<List<UserBudget>> getBudgetAlerts() {
        return withUser(userId -> Result.success(userBudgetService.checkBudgetAlerts(userId)));
    }

    /**
     * 获取即将到期的预算
     */
    @GetMapping("/expiring")
    public Result<List<UserBudget>> getExpiringBudgets() {
        return withUser(userId -> Result.success(userBudgetService.getExpiringBudgets(userId)));
    }

    private <T> Result<T> withUser(Function<Long, Result<T>> action) {
        return userContext.currentUserId()
                .map(action)
                .orElseGet(() -> Result.error(ResultCode.UNAUTHORIZED));
    }

    private <T> Result<T> withBudget(Long budgetId, BiFunction<Long, UserBudget, Result<T>> action) {
        return withUser(userId -> {
            UserBudget budget = userBudgetService.getById(budgetId);
            if (budget == null) {
                log.warn("预算不存在: {}", budgetId);
                return Result.error("预算不存在");
            }
            if (!userId.equals(budget.getUserId())) {
                log.warn("用户ID: {} 无权访问预算: {}", userId, budgetId);
                return Result.error(ResultCode.FORBIDDEN);
            }
            return action.apply(userId, budget);
        });
    }
}
