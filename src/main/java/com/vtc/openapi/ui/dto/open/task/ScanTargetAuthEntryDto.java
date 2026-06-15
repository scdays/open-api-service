package com.vtc.openapi.ui.dto.open.task;

import lombok.Data;

import java.util.List;

@Data
public class ScanTargetAuthEntryDto {

    private String ip;
    private String protocol;
    private Integer port;
    private String username;
    private String password;
    private List<JumpHostDto> jumpHosts;
}
