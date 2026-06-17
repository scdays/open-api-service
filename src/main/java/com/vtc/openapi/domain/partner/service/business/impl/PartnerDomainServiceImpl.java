package com.vtc.openapi.domain.partner.service.business.impl;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.domain.service.DomainServiceImpl;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.partner.model.PartnerConstants;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.model.support.WebhookSecretGenerator;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.domain.partner.service.business.IPartnerDomainService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class PartnerDomainServiceImpl
        extends DomainServiceImpl<IPartnerRepository, PartnerDO>
        implements IPartnerDomainService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PartnerDO createPartner(PartnerDO partner, List<String> capabilities, String defaultCallbackUrl) {
        if (databaseRepository.findByPartnerId(partner.getPartnerId()) != null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 已存在");
        }
        Date now = new Date();
        partner.setStatus(PartnerConstants.STATUS_ACTIVE);
        if (partner.getRateLimitQps() == null) {
            partner.setRateLimitQps(100);
        }
        partner.setCreatedAt(now);
        partner.setUpdatedAt(now);
        try {
            databaseRepository.save(partner);
        } catch (DuplicateKeyException ex) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "partnerId 已存在");
        }
        if (!CollectionUtils.isEmpty(capabilities)) {
            databaseRepository.replaceCapabilities(partner.getPartnerId(), capabilities);
        }
        databaseRepository.upsertCallbackUrl(partner.getPartnerId(), defaultCallbackUrl);
        return databaseRepository.findByPartnerId(partner.getPartnerId());
    }

    @Override
    public PartnerDO requireByPartnerId(String partnerId) {
        PartnerDO partner = databaseRepository.findByPartnerId(partnerId);
        if (partner == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "Partner 不存在");
        }
        return partner;
    }

    @Override
    public PageInfo<PartnerDO> pagePartners(PageInfo<PartnerDO> pageInfo) {
        return databaseRepository.pageOrderByCreatedDesc(pageInfo);
    }

    @Override
    public PageInfo<PartnerDO> pagePartners(PageInfo<PartnerDO> pageInfo,
                                            String partnerId,
                                            String partnerName,
                                            String status) {
        return databaseRepository.pageByFilters(pageInfo, partnerId, partnerName, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PartnerDO updatePartner(String partnerId, PartnerDO patch, List<String> capabilities, String defaultCallbackUrl) {
        PartnerDO partner = requireByPartnerId(partnerId);
        if (StringUtils.hasText(patch.getPartnerName())) {
            partner.setPartnerName(patch.getPartnerName());
        }
        if (StringUtils.hasText(patch.getStatus())) {
            partner.setStatus(patch.getStatus());
        }
        if (patch.getRateLimitQps() != null) {
            partner.setRateLimitQps(patch.getRateLimitQps());
        }
        partner.setUpdatedAt(new Date());
        databaseRepository.updateById(partner);
        if (capabilities != null) {
            databaseRepository.replaceCapabilities(partnerId, capabilities);
        }
        if (defaultCallbackUrl != null) {
            databaseRepository.upsertCallbackUrl(partnerId, defaultCallbackUrl);
        }
        return requireByPartnerId(partnerId);
    }

    @Override
    public List<String> listCapabilities(String partnerId) {
        return databaseRepository.listCapabilities(partnerId);
    }

    @Override
    public String findCallbackUrl(String partnerId) {
        return databaseRepository.findCallbackUrl(partnerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PartnerCredentialDO createCredential(String partnerId, String clientId, String clientSecretHash) {
        PartnerDO partner = requireByPartnerId(partnerId);
        if (!PartnerConstants.STATUS_ACTIVE.equals(partner.getStatus())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "Partner 非 ACTIVE，禁止签发凭证");
        }
        Date now = new Date();
        PartnerCredentialDO credential = new PartnerCredentialDO();
        credential.setPartnerId(partnerId);
        credential.setClientId(clientId);
        credential.setClientSecretHash(clientSecretHash);
        credential.setStatus(PartnerConstants.STATUS_ACTIVE);
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);
        databaseRepository.insertCredential(credential);
        return credential;
    }

    @Override
    public List<PartnerCredentialDO> listCredentials(String partnerId) {
        requireByPartnerId(partnerId);
        return databaseRepository.listCredentials(partnerId);
    }

    @Override
    public PartnerCredentialDO findCredentialByClientId(String clientId) {
        return databaseRepository.findCredentialByClientId(clientId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String assignWebhookSecretIfAbsent(String partnerId) {
        requireByPartnerId(partnerId);
        if (databaseRepository.hasWebhookSecret(partnerId)) {
            return null;
        }
        String secret = WebhookSecretGenerator.generate();
        databaseRepository.saveWebhookSecret(partnerId, secret);
        return secret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String rotateWebhookSecret(String partnerId) {
        requireByPartnerId(partnerId);
        String secret = WebhookSecretGenerator.generate();
        databaseRepository.saveWebhookSecret(partnerId, secret);
        return secret;
    }

    @Override
    public boolean hasWebhookSecret(String partnerId) {
        requireByPartnerId(partnerId);
        return databaseRepository.hasWebhookSecret(partnerId);
    }
}
