package org.maram.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.maram.bill.entity.UserBudget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户预算Mapper
 */
@Mapper
public interface UserBudgetMapper extends BaseMapper<UserBudget> {

    /**
     * 根据用户ID查询预算列表
     * @param userId 用户ID
     * @return 预算列表
     */
    @Select("SELECT * FROM user_budget WHERE user_id = #{userId} AND deleted = 0 ORDER BY start_date DESC")
    List<UserBudget> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和预算类型查询预算
     * @param userId 用户ID
     * @param budgetType 预算类型
     * @return 预算列表
     */
    @Select("SELECT * FROM user_budget WHERE user_id = #{userId} AND budget_type = #{budgetType} AND deleted = 0 ORDER BY start_date DESC")
    List<UserBudget> selectByUserIdAndType(@Param("userId") Long userId, @Param("budgetType") String budgetType);

    /**
     * 查询指定日期范围内的有效预算
     * @param userId 用户ID
     * @param date 查询日期
     * @return 有效预算列表
     */
    @Select("SELECT * FROM user_budget WHERE user_id = #{userId} AND start_date <= #{date} AND end_date >= #{date} AND deleted = 0")
    List<UserBudget> selectActiveByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询用户在指定日期范围内的支出总额
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 支出总额
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM bill " +
            "WHERE user_id = #{userId} AND transaction_type = 'expense' " +
            "AND issue_date >= #{startDate} AND issue_date <= #{endDate} AND deleted = 0")
    BigDecimal selectExpenseAmountByDateRange(@Param("userId") Long userId, 
                                            @Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);

    /**
     * 检查用户在指定时间段是否已有预算
     * @param userId 用户ID
     * @param budgetType 预算类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param excludeId 排除的预算ID（用于更新时排除自身）
     * @return 冲突的预算数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM user_budget " +
            "WHERE user_id = #{userId} AND budget_type = #{budgetType} " +
            "AND deleted = 0 " +
            "AND ((start_date &lt;= #{startDate} AND end_date &gt;= #{startDate}) " +
            "OR (start_date &lt;= #{endDate} AND end_date &gt;= #{endDate}) " +
            "OR (start_date &gt;= #{startDate} AND end_date &lt;= #{endDate})) " +
            "<if test='excludeId != null'>" +
            "AND id != #{excludeId} " +
            "</if>" +
            "</script>")
    long countConflictingBudgets(@Param("userId") Long userId, 
                               @Param("budgetType") String budgetType,
                               @Param("startDate") LocalDate startDate, 
                               @Param("endDate") LocalDate endDate,
                               @Param("excludeId") Long excludeId);

    /**
     * 查询即将到期的预算（7天内到期）
     * @param userId 用户ID
     * @return 即将到期的预算列表
     */
    @Select("SELECT * FROM user_budget WHERE user_id = #{userId} " +
            "AND end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
            "AND deleted = 0 ORDER BY end_date ASC")
    List<UserBudget> selectExpiringBudgets(@Param("userId") Long userId);
}
