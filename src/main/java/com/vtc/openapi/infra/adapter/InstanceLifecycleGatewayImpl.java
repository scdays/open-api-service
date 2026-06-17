package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.instance.gateway.IInstanceLifecycleGateway;
import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.VerifyFixSubmitResult;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.feign.IVulPassOpenInstanceFeign;
import com.vtc.openapi.infra.feign.dto.VulPassVerifyFixRequest;
import com.vtc.openapi.infra.feign.dto.VulPassVerifyFixResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnExpression("'${open-api.engine.adapter-mode:vul-pass}'.equals('vul-pass') "
        + "&& '${open-api.svmp.orchestration.enabled:false}'.equals('true')")
public class InstanceLifecycleGatewayImpl implements IInstanceLifecycleGateway {

    private static final Logger log = LoggerFactory.getLogger(InstanceLifecycleGatewayImpl.class);
    private static final int STAT_FIXED = 5;

    private final IVulPassOpenInstanceFeign openInstanceFeign;
    private final OpenApiProperties properties;

    public InstanceLifecycleGatewayImpl(IVulPassOpenInstanceFeign openInstanceFeign,
                                        OpenApiProperties properties) {
        this.openInstanceFeign = openInstanceFeign;
        this.properties = properties;
    }

    @Override
    public boolean isAsyncVerifyFixEnabled() {
        return properties.getSvmp().getOrchestration().isEnabled();
    }

    @Override
    public VerifyFixSubmitResult submitVerifyFix(String partnerId, VerifyFixInstanceCommand command) {
        VulPassVerifyFixRequest request = new VulPassVerifyFixRequest();
        request.setPartnerId(partnerId);
        request.setRemark(command.getRemark());
        request.setTransferTime(command.getTransferTime());
        try {
            VulPassVerifyFixResponse response = openInstanceFeign.verifyFix(command.getVulInfoId(), request);
            if (response == null || !"ACCEPTED".equalsIgnoreCase(response.getStatus())) {
                String msg = response != null ? response.getMessage() : "empty response";
                throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,
                        StringUtils.hasText(msg) ? msg : "verify-fix rejected");
            }
            VerifyFixSubmitResult result = new VerifyFixSubmitResult();
            result.setVulInfoId(command.getVulInfoId());
            result.setPreviousStat(STAT_FIXED);
            result.setCurrentStat(response.getCurrentStatus() != null ? response.getCurrentStatus() : STAT_FIXED);
            result.setVerifyFixJobId(response.getVerifyFixJobId());
            result.setVerifyFixStatus(response.getVerifyFixStatus());
            return result;
        } catch (OpenApiException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.warn("vul-pass verify-fix failed: status={}", ex.status());
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "verify-fix 受理失败");
        } catch (Exception ex) {
            log.warn("vul-pass verify-fix error", ex);
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "verify-fix 受理失败");
        }
    }
}
