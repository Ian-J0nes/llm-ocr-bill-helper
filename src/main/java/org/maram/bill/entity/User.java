package org.maram.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户账户实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("openid")
    private String openid;
    @TableField("username")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username; // 用户名/登录名
    @Email(message = "邮箱格式不正确")
    private String email; // 邮箱

    @Pattern(regexp = "^1[3-9]/d{9}$", message = "手机号格式不正确")
    private String phoneNumber; // 手机号
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname; // 用户昵称

    @TableField("role")
    private String role; // 用户角色

    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatarUrl; // 用户头像URL
    @TableField("status")
    private String status; // 用户状态

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt; // 最后登录时间

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("ai_model")
    private String aiModel; // AI模型名称

    @TableField("ai_temperature")
    private Double aiTemperature; // AI温度参数

    @TableField("ai_config_updated_at")
    private LocalDateTime aiConfigUpdatedAt; // AI配置最后更新时间
}
