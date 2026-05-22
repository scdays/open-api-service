package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vtc.openapi.domain.partner.model.entity.PartnerCapabilityDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.infra.dao.PartnerCapabilityMapper;
import com.vtc.openapi.infra.dao.PartnerCredentialMapper;
import com.vtc.openapi.infra.dao.PartnerMapper;
import com.vtc.openapi.infra.dao.PartnerWebhookConfigMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PartnerRepository {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    private final PartnerMapper partnerMapper;
    private final PartnerCapabilityMapper capabilityMapper;
    private final PartnerCredentialMapper credentialMapper;
    private final PartnerWebhookConfigMapper webhookConfigMapper;

    public PartnerRepository(PartnerMapper partnerMapper,
                             PartnerCapabilityMapper capabilityMapper,
                             PartnerCredentialMapper credentialMapper,
                             PartnerWebhookConfigMapper webhookConfigMapper) {
        this.partnerMapper = partnerMapper;
        this.capabilityMapper = capabilityMapper;
        this.credentialMapper = credentialMapper;
        this.webhookConfigMapper = webhookConfigMapper;
    }

    public PartnerDO findByPartnerId(String partnerId) {
        return partnerMapper.selectOne(new LambdaQueryWrapper<PartnerDO>()
                .eq(PartnerDO::getPartnerId, partnerId));
    }

    public void insertPartner(PartnerDO partner) {
        partnerMapper.insert(partner);
    }

    public void updatePartner(PartnerDO partner) {
        partner.setUpdatedAt(new Date());
        partnerMapper.updateById(partner);
    }

    public List<PartnerDO> listPartners(int offset, int limit) {
        return partnerMapper.selectList(new LambdaQueryWrapper<PartnerDO>()
                .orderByDesc(PartnerDO::getCreatedAt)
                .last("LIMIT " + limit + " OFFSET " + offset));
    }

    public long countPartners() {
        return partnerMapper.selectCount(null);
    }

    public List<String> listCapabilities(String partnerId) {
        List<PartnerCapabilityDO> rows = capabilityMapper.selectList(
                new LambdaQueryWrapper<PartnerCapabilityDO>()
                        .eq(PartnerCapabilityDO::getPartnerId, partnerId));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream().map(PartnerCapabilityDO::getCapability).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceCapabilities(String partnerId, List<String> capabilities) {
        capabilityMapper.delete(new LambdaQueryWrapper<PartnerCapabilityDO>()
                .eq(PartnerCapabilityDO::getPartnerId, partnerId));
        if (CollectionUtils.isEmpty(capabilities)) {
            return;
        }
        Date now = new Date();
        for (String cap : capabilities) {
            if (!StringUtils.hasText(cap)) {
                continue;
            }
            PartnerCapabilityDO row = new PartnerCapabilityDO();
            row.setPartnerId(partnerId);
            row.setCapability(cap.trim());
            row.setCreatedAt(now);
            capabilityMapper.insert(row);
        }
    }

    public PartnerCredentialDO findCredentialByClientId(String clientId) {
        return credentialMapper.selectOne(new LambdaQueryWrapper<PartnerCredentialDO>()
                .eq(PartnerCredentialDO::getClientId, clientId));
    }

    public void insertCredential(PartnerCredentialDO credential) {
        credentialMapper.insert(credential);
    }

    public String findCallbackUrl(String partnerId) {
        PartnerWebhookConfigDO config = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<PartnerWebhookConfigDO>()
                        .eq(PartnerWebhookConfigDO::getPartnerId, partnerId));
        return config != null ? config.getCallbackUrl() : null;
    }

    public void upsertCallbackUrl(String partnerId, String callbackUrl) {
        if (!StringUtils.hasText(callbackUrl)) {
            return;
        }
        PartnerWebhookConfigDO existing = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<PartnerWebhookConfigDO>()
                        .eq(PartnerWebhookConfigDO::getPartnerId, partnerId));
        Date now = new Date();
        if (existing == null) {
            PartnerWebhookConfigDO row = new PartnerWebhookConfigDO();
            row.setPartnerId(partnerId);
            row.setCallbackUrl(callbackUrl);
            row.setUpdatedAt(now);
            webhookConfigMapper.insert(row);
        } else {
            existing.setCallbackUrl(callbackUrl);
            existing.setUpdatedAt(now);
            webhookConfigMapper.updateById(existing);
        }
    }

}
