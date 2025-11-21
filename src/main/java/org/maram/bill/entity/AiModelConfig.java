package org.maram.bill.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * AI模型配置实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_model_config")
public class AiModelConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键ID

    @TableField("model_name")
    private String modelName; // 模型名称

    @TableField("model_display_name")
    private String modelDisplayName; // 模型显示名称

    @TableField("model_description")
    private String modelDescription; // 模型描述

    @TableField("max_tokens")
    private Integer maxTokens; // 最大token数量

    @TableField("default_temperature")
    private Double defaultTemperature; // 默认温度值

    @TableField("min_temperature")
    private Double minTemperature; // 最小温度值

    @TableField("max_temperature")
    private Double maxTemperature; // 最大温度值

    @TableField("cost_per_1k_input_tokens")
    private BigDecimal costPer1kInputTokens; // 每1000输入token成本

    @TableField("cost_per_1k_output_tokens")
    private BigDecimal costPer1kOutputTokens; // 每1000输出token成本

    @TableField("supports_vision")
    private Boolean supportsVision; // 是否支持视觉功能

    @TableField("supports_function_calling")
    private Boolean supportsFunctionCalling; // 是否支持函数调用

    @TableField("context_window")
    private Integer contextWindow; // 上下文窗口大小

    @TableField("status")
    private String status; // 状态

    @TableField("sort_order")
    private Integer sortOrder; // 排序顺序

    @TableField("is_default")
    private Boolean isDefault; // 是否为默认模型

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间

    @TableLogic
    @TableField("deleted")
    private Integer deleted; // 逻辑删除标志
}