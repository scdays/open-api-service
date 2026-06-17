package com.vtc.openapi.domain.instance.model.command;

import lombok.Data;

/**
 * 验证实例命令。
 */
@Data
public class VerifyInstanceCommand {
    private String vulInfoId;
    private Integer vulnType;
    private String verifyResult;
    private Integer srcMethod;
    private String transferTime;
    private String operator;
    private String remark;
}
