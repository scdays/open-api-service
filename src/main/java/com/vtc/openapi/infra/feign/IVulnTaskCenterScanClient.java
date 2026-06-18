package com.vtc.openapi.infra.feign;

import com.botany.spore.core.result.Result;
import com.vtc.openapi.infra.feign.dto.taskcenter.SocOutsideScanRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * vuln-task-center 事件接口（见漏扫任务事件接口文档）。
 * <p>通过 Nacos 服务名直连 vuln-task-center，勿配置网关地址（:7000 会要求 JWT，后台下发无登录态会 401）。</p>
 */
@FeignClient(name = "vuln-task-center", contextId = "openApiTaskCenterClient")
public interface IVulnTaskCenterScanClient {

    @PostMapping("/event/scan/task/outside/soc/scan")
    Result<Map<String, Object>> createSocScan(@RequestBody SocOutsideScanRequest request);

    @GetMapping("/event/scan/task/survey/query/vulnScanResult")
    Map<String, Object> queryVulnScanResult(@RequestParam("surveyId") String surveyId,
                                            @RequestParam("current") String current);

    @GetMapping("/event/scan/task/survey/query/success/ips")
    Set<String> querySuccessIps(@RequestParam("surveyId") String surveyId);

    @GetMapping("/event/scan/task/survey/query/fail/ips")
    Set<String> queryFailIps(@RequestParam("surveyId") String surveyId);

    @GetMapping("/event/scan/task/survey/query/ip/port")
    List<Map<String, Object>> queryScanPorts(@RequestParam("surveyId") String surveyId);

    @GetMapping("/event/scan/task/getId/{taskId}")
    Map<String, Object> getTaskById(@PathVariable("taskId") String taskId);

    @GetMapping("/event/scan/task/survey/getId/{surveyId}")
    Map<String, Object> getSurveyById(@PathVariable("surveyId") String surveyId);

    /** 内部：获取计划最新 survey（轮询用） */
    @GetMapping("/v1/scan/task/vulnScan/surveyList/{taskId}")
    Map<String, Object> surveyList(@PathVariable("taskId") String taskId,
                                   @RequestParam(value = "current", defaultValue = "1") long current,
                                   @RequestParam(value = "size", defaultValue = "1") long size);
}
