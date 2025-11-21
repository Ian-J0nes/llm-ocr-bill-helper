package org.maram.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 原始票据文件实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("invoice_file")
public class InvoiceFile {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName; // 原始文件名
    
    @TableField("file_url")
    private String fileUrl; // 图床Url
    
    @TableField("file_type")
    private String fileType; // 文件类型
    
    @TableField("file_size")
    private Long fileSize;   // 文件大小 (单位: 字节)

    @TableField("user_id")
    private Long userId;   // 上传文件的用户ID

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间

    @TableLogic
    @TableField("deleted")
    private Integer deleted; // 逻辑删除标志
}
