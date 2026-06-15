package com.vtc.openapi.domain.task.model.vo;

import lombok.Data;

@Data
public class JumpHostEntry {

    private String ip;
    private String protocol;
    private Integer port;
    private String username;
    private String password;
}
