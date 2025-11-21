package org.maram.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 货币实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("currencies")
public class Currency {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("code")
    private String code; // 货币代码

    @TableField("name")
    private String name; // 货币名称

    @TableField("symbol")
    private String symbol; // 货币符号

    @TableField("is_active")
    private boolean active = true; // 是否激活

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间
}