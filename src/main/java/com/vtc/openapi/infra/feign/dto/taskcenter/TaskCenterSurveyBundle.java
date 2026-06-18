package com.vtc.openapi.infra.feign.dto.taskcenter;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class TaskCenterSurveyBundle {

    private String surveyId;
    private List<Map<String, Object>> vulnScanResultList = new ArrayList<>();
    private List<Map<String, Object>> vulnDatabaseList = new ArrayList<>();
    private Set<String> successIps = new HashSet<>();
    private Set<String> failIps = new HashSet<>();
    private List<Map<String, Object>> portScanRows = new ArrayList<>();
    /** VTC 成功 IP 查询是否成功（用于存活合并容错） */
    private boolean successIpsQueryOk = true;
    /** VTC 失败 IP 查询是否成功 */
    private boolean failIpsQueryOk = true;
    private boolean portScanQueryOk = true;
}
