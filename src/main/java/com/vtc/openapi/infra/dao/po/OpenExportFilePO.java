package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.botany.spore.mybatis.pojo.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("open_export_file")
public class OpenExportFilePO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String exportId;
    private String realTaskId;
    private String partnerId;
    private String filePosition;
    private String fileField;
    private String fileMetadata;
    private Integer fileType;
    private Date createTime;
    private Date updateTime;
}
