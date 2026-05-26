package com.vtc.openapi.domain.partner.repository;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.domain.repository.IDatabaseRepository;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;

import java.util.List;

/**
 * Partner 聚合仓储（主表 + 能力/凭证/Webhook 关联表）。
 */
public interface IPartnerRepository extends IDatabaseRepository<PartnerDO> {

    PartnerDO findByPartnerId(String partnerId);

    PageInfo<PartnerDO> pageOrderByCreatedDesc(PageInfo<PartnerDO> pageInfo);

    PageInfo<PartnerDO> pageByFilters(PageInfo<PartnerDO> pageInfo, String partnerId, String partnerName, String status);

    List<String> listCapabilities(String partnerId);

    void replaceCapabilities(String partnerId, List<String> capabilities);

    PartnerCredentialDO findCredentialByClientId(String clientId);

    void insertCredential(PartnerCredentialDO credential);

    List<PartnerCredentialDO> listCredentials(String partnerId);

    String findCallbackUrl(String partnerId);

    void upsertCallbackUrl(String partnerId, String callbackUrl);
}
