package org.maram.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户预算实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_budget")
public class UserBudget {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键ID

    @TableField("user_id")
    private Long userId; // 用户ID

    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "预算金额必须为正数")
    private BigDecimal budgetAmount; // 预算金额

    @NotBlank(message = "预算类型不能为空")
    private String budgetType; // 预算类型: MONTHLY-月度, QUARTERLY-季度, YEARLY-年度

    @NotNull(message = "预算开始日期不能为空")
    private LocalDate startDate; // 预算开始日期

    @NotNull(message = "预算结束日期不能为空")
    private LocalDate endDate; // 预算结束日期

    @TableField("alert_threshold")
    private BigDecimal alertThreshold; // 预警阈值百分比(如80表示80%)

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间

    @TableLogic
    @TableField("deleted")
    private Integer deleted; // 逻辑删除标志

    // --- 非数据库字段 ---
    @TableField(exist = false)
    private BigDecimal usedAmount; // 已使用金额

    @TableField(exist = false)
    private BigDecimal remainingAmount; // 剩余金额

    @TableField(exist = false)
    private BigDecimal usagePercentage; // 使用百分比

    @TableField(exist = false)
    private Boolean isOverBudget; // 是否超预算

    @TableField(exist = false)
    private Boolean isNearThreshold; // 是否接近预警阈值

    /**
     * 预算类型枚举
     */
    public enum BudgetType {
        MONTHLY("MONTHLY", "月度预算"),
        QUARTERLY("QUARTERLY", "季度预算"),
        YEARLY("YEARLY", "年度预算");

        private final String code;
        private final String desc;

        BudgetType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public static BudgetType getByCode(String code) {
            for (BudgetType type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 计算使用百分比
     */
    public BigDecimal calculateUsagePercentage() {
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (usedAmount == null) {
            return BigDecimal.ZERO;
        }
        return usedAmount.divide(budgetAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 计算剩余金额
     */
    public BigDecimal calculateRemainingAmount() {
        if (budgetAmount == null) {
            return BigDecimal.ZERO;
        }
        if (usedAmount == null) {
            return budgetAmount;
        }
        return budgetAmount.subtract(usedAmount);
    }

    /**
     * 检查是否超预算
     */
    public Boolean checkIsOverBudget() {
        if (usedAmount == null || budgetAmount == null) {
            return false;
        }
        return usedAmount.compareTo(budgetAmount) > 0;
    }

    /**
     * 检查是否接近预警阈值
     */
    public Boolean checkIsNearThreshold() {
        if (usedAmount == null || budgetAmount == null || alertThreshold == null) {
            return false;
        }
        BigDecimal thresholdAmount = budgetAmount.multiply(alertThreshold.divide(new BigDecimal("100")));
        return usedAmount.compareTo(thresholdAmount) >= 0;
    }
}
