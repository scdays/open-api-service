package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * task-center 模式：实例读写走 open_vuln_instance（VTC 回收入库），不依赖 vul-pass Feign。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class VulnInstanceGatewayTaskCenterImpl implements IVulnInstanceGateway {

    private static final Logger log = LoggerFactory.getLogger(VulnInstanceGatewayTaskCenterImpl.class);

    private final IOpenVulnInstanceRepository vulnInstanceRepository;

    public VulnInstanceGatewayTaskCenterImpl(IOpenVulnInstanceRepository vulnInstanceRepository) {
        this.vulnInstanceRepository = vulnInstanceRepository;
        log.info("VulnInstanceGateway: TASK-CENTER mode (open_vuln_instance db-backed)");
    }

    @Override
    public InstancePageResult searchInstances(SearchInstanceCommand command) {
        String partnerId = PartnerContext.requirePartnerId();
        return vulnInstanceRepository.searchFromDb(partnerId, command);
    }

    @Override
    public InstanceItemResult findByVulInfoId(String partnerId, String vulInfoId) {
        if (!StringUtils.hasText(vulInfoId)) {
            return null;
        }
        String effectivePartnerId = StringUtils.hasText(partnerId)
                ? partnerId.trim()
                : PartnerContext.getPartnerId();
        if (!StringUtils.hasText(effectivePartnerId)) {
            return null;
        }
        return InstanceItemConverter.fromSnapshot(
                vulnInstanceRepository.findByPartnerAndVulInfoId(effectivePartnerId, vulInfoId.trim()));
    }

    @Override
    public void updateInstance(Long id, int vulInfoStat, String method, String remedDesc) {
        String partnerId = PartnerContext.requirePartnerId();
        requirePersisted(id, partnerId);
        vulnInstanceRepository.updateState(id, partnerId, vulInfoStat, method, remedDesc);
        log.debug("task-center updateInstance: id={} vulInfoStat={}", id, vulInfoStat);
    }

    @Override
    public void updateRemediateInstance(Long id, int vulInfoStat, RemediateInstanceCommand command) {
        String partnerId = PartnerContext.requirePartnerId();
        requirePersisted(id, partnerId);
        vulnInstanceRepository.updateRemediateState(id, partnerId, vulInfoStat, command);
        log.debug("task-center updateRemediateInstance: id={} vulInfoStat={}", id, vulInfoStat);
    }

    private void requirePersisted(Long id, String partnerId) {
        if (id == null || vulnInstanceRepository.findByIdAndPartner(id, partnerId) == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "实例不存在或未入库");
        }
    }
}
