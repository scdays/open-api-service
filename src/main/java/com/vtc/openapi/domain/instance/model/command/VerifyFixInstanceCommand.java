package com.vtc.openapi.domain.instance.model.command;

import lombok.Data;

/**
 * 核验修复命令。
 */
@Data
public class VerifyFixInstanceCommand {
    private String vulInfoId;
    /** mock 同步模式内部使用；公网 API 不传 */
    private String verifyResult;
    private String transferTime;
    private String remark;
}
