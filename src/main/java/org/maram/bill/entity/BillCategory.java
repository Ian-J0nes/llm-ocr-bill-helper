package org.maram.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 账单分类实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bill_category")
public class BillCategory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键ID

    @TableField("user_id")
    private Long userId; // 用户ID，NULL表示系统分类

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度必须在1到50之间")
    private String categoryName; // 分类名称

    @TableField("category_code")
    private String categoryCode; // 分类编码

    @TableField("sort_order")
    private Integer sortOrder; // 排序序号

    @TableField("status")
    private Integer status; // 状态：0-禁用 1-启用

    @TableField("description")
    private String description; // 分类描述

    @TableField("is_system")
    private Integer isSystem; // 是否系统分类：0-否 1-是

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 更新时间

    @TableLogic
    @TableField("deleted")
    private Integer deleted; // 逻辑删除标志



    /**
     * 状态枚举
     */
    public enum Status {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final Integer code;
        private final String desc;

        Status(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public static Status getByCode(Integer code) {
            for (Status status : values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }
}
