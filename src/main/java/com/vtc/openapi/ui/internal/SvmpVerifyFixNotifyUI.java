package com.vtc.openapi.ui.internal;

import com.vtc.openapi.app.service.IVerifyFixNotifyAppService;
import com.vtc.openapi.ui.dto.internal.VerifyFixCompletedNotifyRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "SVMP 内部回调（vul-pass → open-api）")
@RestController
@RequestMapping("/internal/svmp/v1/verify-fix/jobs")
public class SvmpVerifyFixNotifyUI {

    private final IVerifyFixNotifyAppService appService;

    public SvmpVerifyFixNotifyUI(IVerifyFixNotifyAppService appService) {
        this.appService = appService;
    }

    @ApiOperation("verify-fix 完成通知（触发 EXPORT_READY + INSTANCE_VERIFY_FIX_COMPLETED）")
    @PostMapping("/{jobId}/notify")
    public Map<String, Object> notifyCompleted(@PathVariable("jobId") String jobId,
                                               @RequestBody VerifyFixCompletedNotifyRequest request) {
        return appService.notifyCompleted(jobId, request);
    }
}
