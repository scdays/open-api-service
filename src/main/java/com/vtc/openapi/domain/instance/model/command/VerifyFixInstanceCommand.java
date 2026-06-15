package com.vtc.openapi.domain.instance.model.command;

import lombok.Data;

/**
 * 核验修复命令。
 */
@Data
public class VerifyFixInstanceCommand {
    private String vulInfoId;
    private String verifyResult;
}