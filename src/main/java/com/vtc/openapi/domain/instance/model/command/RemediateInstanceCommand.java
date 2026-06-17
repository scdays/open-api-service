package com.vtc.openapi.domain.instance.model.command;

import java.util.Map;
import lombok.Data;

/**
 * 修复实例命令。
 */
@Data
public class RemediateInstanceCommand {
    private String vulInfoId;
    /** 处置目标状态：5=已修复，9=修复失败/备案；未传时由 lvRsn 推断 */
    private Integer vulInfoStat;
    private Integer srcMethod;
    private String remedDesc;
    private String fixLnk;
    private String defDev;
    private String remedTime;
    private Integer lvRsn;
    private String archiveReason;
    private String approvedBy;
    private String recordAt;
    private Map<String, Object> provincialFields;
    private Integer srcTktRole;
    private Integer dstTktRole;
    private String assignerDept;
    private String assignerEmail;
    private String assignerPhone;
    private String handlerDept;
    private String handlerEmail;
    private String handlerPhone;
    private String transferTime;
    private String remark;
}
