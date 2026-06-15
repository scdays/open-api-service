package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.botany.spore.mybatis.pojo.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("open_export")
public class OpenExportPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String exportId;
    private String partnerId;
    private String taskId;
    private String extTaskId;
    private Integer reportTemplateId;
    @TableField("`format`")
    private String format;
    private String exportStage;
    private String dataType;
    private String status;
    private Integer recordCount;
    private Date expiresAt;
    private String storagePath;
    private String downloadUrl;
    private String errorMessage;
    private String verifyFixJobId;
    private Date generatedAt;
    private Date createdAt;
    private Date updatedAt;
}
