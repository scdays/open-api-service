package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VulPassOpenScanTargets {

    private String hosts;
    private List<Map<String, Object>> auth;
}
