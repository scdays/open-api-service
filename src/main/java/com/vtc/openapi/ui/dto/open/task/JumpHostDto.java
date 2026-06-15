package com.vtc.openapi.ui.dto.open.task;

import lombok.Data;

@Data
public class JumpHostDto {

    private String ip;
    private String protocol;
    private Integer port;
    private String username;
    private String password;
}
