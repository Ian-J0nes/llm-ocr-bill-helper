package org.maram.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.maram.bill.common.validation.ValidTransactionType;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账单信息实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bill")
public class Bill {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键

    // --- 关联信息 ---
    private Long userId; // 关联的用户ID (必需)

    @TableField("file_id")
    private Long fileId; // 关联的原始文件ID

    @NotBlank(message = "交易类型不能为空")
    @ValidTransactionType
    private String transactionType; // 交易类型

    @TableField("category_id")
    private Long categoryId; // 分类ID

    // --- 票据基本信息 ---
    @TableField("name")
    private String name; // 账单名称/摘要 (用户可定义或OCR提取)

    @TableField("invoice_number")
    private String invoiceNumber; // 发票号码/账单编号

    @TableField("supplier_name")
    private String supplierName; // 供应商/销售方名称

    @TableField("bill_type")
    private String billType; // 账单类型

    // --- 金额相关  ---
    @NotNull(message = "总金额不能为空")
    @Positive(message = "总金额必须为正数")
    private BigDecimal totalAmount; // 总金额

    @TableField("tax_amount")
    private BigDecimal taxAmount;   // 税额

    @TableField("net_amount")
    private BigDecimal netAmount;   // 不含税金额

    @TableField("currency_code")
    private String currencyCode; // 货币代码

    // --- 日期相关 ---
    @TableField("issue_date")
    private LocalDate issueDate;    // 开票日期/账单日期

    // --- 状态与备注 ---
    @TableField("notes")
    private String notes; // 用户备注

    @TableField("payment_status")
    private String paymentStatus; // 支付状态 (例如: "UNPAID", "PAID", "PARTIALLY_PAID") - 可用枚举

    @TableField("review_status")
    private String reviewStatus;  // 校对状态 (例如: "PENDING_REVIEW", "REVIEWED", "CONFIRMED") - 可用枚举

    @TableField("accounting_status")
    private String accountingStatus; // 记账状态 (例如: "PENDING_ACCOUNTING", "POSTED", "FAILED") - 可用枚举

    // --- 系统字段 ---
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 更新时间

    @TableLogic
    @TableField("deleted")
    private Integer deleted; // 逻辑删除标志

    // --- 关联实体（非数据库字段）---
    @TableField(exist = false)
    private BillCategory category; // 分类信息（用于联表查询）
}
