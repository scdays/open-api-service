package com.vtc.openapi.domain.partner.service.business;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.domain.service.IDomainService;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;

import java.util.List;

/**
 * Partner 管理领域服务。
 */
public interface IPartnerDomainService extends IDomainService<PartnerDO> {

    PartnerDO createPartner(PartnerDO partner, List<String> capabilities, String defaultCallbackUrl);

    PartnerDO requireByPartnerId(String partnerId);

    PageInfo<PartnerDO> pagePartners(PageInfo<PartnerDO> pageInfo);

    PageInfo<PartnerDO> pagePartners(PageInfo<PartnerDO> pageInfo, String partnerId, String partnerName, String status);

    PartnerDO updatePartner(String partnerId, PartnerDO patch, List<String> capabilities, String defaultCallbackUrl);

    List<String> listCapabilities(String partnerId);

    String findCallbackUrl(String partnerId);

    PartnerCredentialDO createCredential(String partnerId, String clientId, String clientSecretHash);

    List<PartnerCredentialDO> listCredentials(String partnerId);

    PartnerCredentialDO findCredentialByClientId(String clientId);

    String assignWebhookSecretIfAbsent(String partnerId);

    String rotateWebhookSecret(String partnerId);

    boolean hasWebhookSecret(String partnerId);
}
