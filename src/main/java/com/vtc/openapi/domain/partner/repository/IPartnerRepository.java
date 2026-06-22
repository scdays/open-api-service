package com.vtc.openapi.domain.partner.repository;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.domain.repository.IDatabaseRepository;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;

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

    /**
     * 新增/更新 Partner Webhook 配置（callbackUrl + downloadableStages）。
     * 任一参数为 null 时保持原值不变；均非 null 时整体覆盖。
     */
    void upsertWebhookConfig(String partnerId, String callbackUrl, String downloadableStages);

    /**
     * 查询 Partner Webhook 配置（callbackUrl + webhookSecret）。
     * 用于 Webhook 投递时 HMAC-SHA256 验签（文档 §6）。
     *
     * @return 配置对象；未配置时返回 null
     */
    PartnerWebhookConfigDO findWebhookConfig(String partnerId);

    boolean hasWebhookSecret(String partnerId);

    void saveWebhookSecret(String partnerId, String webhookSecret);
}
