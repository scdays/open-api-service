package com.vtc.openapi.domain.task.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ScanTargetAuthEntry {

    private String ip;
    private String protocol;
    private Integer port;
    private String username;
    private String password;
    private List<JumpHostEntry> jumpHosts;
}
