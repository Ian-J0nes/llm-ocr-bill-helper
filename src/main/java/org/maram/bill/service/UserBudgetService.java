package org.maram.bill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.maram.bill.entity.UserBudget;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户预算服务接口
 */
public interface UserBudgetService {

    /**
     * 根据ID获取预算
     * @param id 预算ID
     * @return 预算信息
     */
    UserBudget getById(Long id);

    /**
     * 获取用户所有预算列表
     * @param userId 用户ID
     * @return 预算列表（包含使用情况）
     */
    List<UserBudget> listByUserId(Long userId);

    /**
     * 根据预算类型获取用户预算
     * @param userId 用户ID
     * @param budgetType 预算类型
     * @return 预算列表
     */
    List<UserBudget> listByUserIdAndType(Long userId, String budgetType);

    /**
     * 获取指定日期的有效预算
     * @param userId 用户ID
     * @param date 查询日期
     * @return 有效预算列表
     */
    List<UserBudget> getActiveBudgets(Long userId, LocalDate date);

    /**
     * 获取当前有效预算
     * @param userId 用户ID
     * @return 当前有效预算列表
     */
    List<UserBudget> getCurrentActiveBudgets(Long userId);

    /**
     * 分页查询用户预算
     * @param page 分页参数
     * @param userId 用户ID
     * @return 分页预算列表
     */
    Page<UserBudget> page(Page<UserBudget> page, Long userId);

    /**
     * 保存预算
     * @param userBudget 预算信息
     * @return 是否成功
     */
    boolean save(UserBudget userBudget);

    /**
     * 更新预算
     * @param userBudget 预算信息
     * @return 是否成功
     */
    boolean updateById(UserBudget userBudget);

    /**
     * 删除预算
     * @param id 预算ID
     * @return 是否成功
     */
    boolean removeById(Long id);

    /**
     * 验证预算数据
     * @param userBudget 预算信息
     * @return 验证结果消息，null表示验证通过
     */
    String validateBudget(UserBudget userBudget);

    /**
     * 检查预算时间冲突
     * @param userId 用户ID
     * @param budgetType 预算类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param excludeId 排除的预算ID
     * @return 是否有冲突
     */
    boolean hasConflictingBudgets(Long userId, String budgetType, LocalDate startDate, LocalDate endDate, Long excludeId);

    /**
     * 计算预算使用情况
     * @param userBudget 预算信息
     * @return 包含使用情况的预算信息
     */
    UserBudget calculateBudgetUsage(UserBudget userBudget);

    /**
     * 获取预算统计信息
     * @param userId 用户ID
     * @return 预算统计
     */
    BudgetStatistics getBudgetStatistics(Long userId);

    /**
     * 获取即将到期的预算
     * @param userId 用户ID
     * @return 即将到期的预算列表
     */
    List<UserBudget> getExpiringBudgets(Long userId);

    /**
     * 检查预算预警
     * @param userId 用户ID
     * @return 需要预警的预算列表
     */
    List<UserBudget> checkBudgetAlerts(Long userId);

    /**
     * 预算统计信息类
     */
    class BudgetStatistics {
        private int totalBudgets; // 总预算数
        private int activeBudgets; // 有效预算数
        private int overBudgets; // 超预算数
        private int nearThresholdBudgets; // 接近预警阈值数

        // 构造函数、getter和setter
        public BudgetStatistics() {}

        public BudgetStatistics(int totalBudgets, int activeBudgets, int overBudgets, int nearThresholdBudgets) {
            this.totalBudgets = totalBudgets;
            this.activeBudgets = activeBudgets;
            this.overBudgets = overBudgets;
            this.nearThresholdBudgets = nearThresholdBudgets;
        }

        public int getTotalBudgets() { return totalBudgets; }
        public void setTotalBudgets(int totalBudgets) { this.totalBudgets = totalBudgets; }

        public int getActiveBudgets() { return activeBudgets; }
        public void setActiveBudgets(int activeBudgets) { this.activeBudgets = activeBudgets; }

        public int getOverBudgets() { return overBudgets; }
        public void setOverBudgets(int overBudgets) { this.overBudgets = overBudgets; }

        public int getNearThresholdBudgets() { return nearThresholdBudgets; }
        public void setNearThresholdBudgets(int nearThresholdBudgets) { this.nearThresholdBudgets = nearThresholdBudgets; }
    }
}
