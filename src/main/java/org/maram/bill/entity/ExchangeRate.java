package org.maram.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 汇率实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("exchange_rates")
public class ExchangeRate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("base_currency_code")
    private String baseCurrencyCode; // 基础货币代码

    @TableField("target_currency_code")
    private String targetCurrencyCode; // 目标货币代码

    @TableField("rate")
    private BigDecimal rate; // 汇率值

    @TableField("api_source")
    private String apiSource; // API来源

    @TableField("last_updated_from_api")
    private LocalDateTime lastUpdatedFromApi; // API最后更新时间

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间
}