package com.vtc.openapi.domain.instance.model.command;

import lombok.Data;

/**
 * 修复实例命令。
 */
@Data
public class RemediateInstanceCommand {
    private String vulInfoId;
    private String srcMethod;
    private String remedDesc;
    private String fixLnk;
}