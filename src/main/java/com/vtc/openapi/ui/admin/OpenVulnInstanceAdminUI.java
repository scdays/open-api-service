package com.vtc.openapi.ui.admin;

import com.vtc.openapi.app.service.IOpenVulnInstanceAdminAppService;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockVulnInstanceOpsRowDto;
import com.vtc.openapi.ui.dto.admin.OpenVulnInstanceStateLogDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/admin/open-vuln-instances")
@Validated
@Api(tags = "OPEN 漏洞实例运营")
public class OpenVulnInstanceAdminUI {

    private final IOpenVulnInstanceAdminAppService appService;

    public OpenVulnInstanceAdminUI(IOpenVulnInstanceAdminAppService appService) {
        this.appService = appService;
    }

    @ApiOperation("查询漏洞实例（运营多选：验证/处置/修复核验）")
    @GetMapping
    public ApiResponse<List<MockVulnInstanceOpsRowDto>> listInstances(
            @RequestParam("partnerId") String partnerId,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "vulInfoStat", required = false) Integer vulInfoStat,
            @RequestParam(value = "limit", defaultValue = "200") int limit) {
        return appService.listInstancesForOps(partnerId, taskId, vulInfoStat, limit);
    }

    @ApiOperation("查询漏洞实例状态跃迁历史（open_vuln_instance_log）")
    @GetMapping("/{vulInfoId}/state-logs")
    public ApiResponse<List<OpenVulnInstanceStateLogDto>> listStateLogs(
            @PathVariable("vulInfoId") String vulInfoId,
            @RequestParam("partnerId") String partnerId,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return appService.listStateLogs(partnerId, vulInfoId, limit);
    }
}
