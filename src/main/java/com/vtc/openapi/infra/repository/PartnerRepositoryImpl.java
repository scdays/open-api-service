package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.repository.DatabaseRepositoryImpl;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.partner.model.entity.PartnerCapabilityDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import com.vtc.openapi.domain.partner.repository.IPartnerRepository;
import com.vtc.openapi.infra.dao.PartnerCapabilityMapper;
import com.vtc.openapi.infra.dao.PartnerCredentialMapper;
import com.vtc.openapi.infra.dao.PartnerMapper;
import com.vtc.openapi.infra.dao.PartnerWebhookConfigMapper;
import com.vtc.openapi.infra.dao.po.PartnerCapabilityPO;
import com.vtc.openapi.infra.dao.po.PartnerCredentialPO;
import com.vtc.openapi.infra.dao.po.PartnerPO;
import com.vtc.openapi.infra.dao.po.PartnerWebhookConfigPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PartnerRepositoryImpl
        extends DatabaseRepositoryImpl<PartnerMapper, PartnerDO, PartnerPO>
        implements IPartnerRepository {
    private final PartnerCapabilityMapper capabilityMapper;
    private final PartnerCredentialMapper credentialMapper;
    private final PartnerWebhookConfigMapper webhookConfigMapper;
    public PartnerRepositoryImpl(PartnerCapabilityMapper capabilityMapper,
                                 PartnerCredentialMapper credentialMapper,
                                 PartnerWebhookConfigMapper webhookConfigMapper) {
        this.capabilityMapper = capabilityMapper;
        this.credentialMapper = credentialMapper;
        this.webhookConfigMapper = webhookConfigMapper;
    }

    @Override
    public PartnerDO findByPartnerId(String partnerId) {
        PartnerPO po = baseMapper.selectOne(new LambdaQueryWrapper<PartnerPO>()
                .eq(PartnerPO::getPartnerId, partnerId));
        return ConvertHelper.convert(po, PartnerDO.class);
    }

    @Override
    public PageInfo<PartnerDO> pageOrderByCreatedDesc(PageInfo<PartnerDO> pageInfo) {
        PageInfo<PartnerPO> poPage = ConvertHelper.convertPageInfo(pageInfo, PartnerPO.class);
        LambdaQueryWrapper<PartnerPO> wrapper = new LambdaQueryWrapper<PartnerPO>()
                .orderByDesc(PartnerPO::getCreatedAt);
        PageInfo<PartnerPO> result = baseMapper.selectPage(poPage, wrapper);
        return ConvertHelper.convertPageInfo(result, PartnerDO.class);
    }

    @Override
    public PageInfo<PartnerDO> pageByFilters(PageInfo<PartnerDO> pageInfo,
                                             String partnerId,
                                             String partnerName,
                                             String status) {
        PageInfo<PartnerPO> poPage = ConvertHelper.convertPageInfo(pageInfo, PartnerPO.class);
        LambdaQueryWrapper<PartnerPO> wrapper = new LambdaQueryWrapper<PartnerPO>()
                .orderByDesc(PartnerPO::getCreatedAt);
        if (StringUtils.hasText(partnerId)) {
            wrapper.eq(PartnerPO::getPartnerId, partnerId);
        }
        if (StringUtils.hasText(partnerName)) {
            wrapper.like(PartnerPO::getPartnerName, partnerName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PartnerPO::getStatus, status);
        }
        PageInfo<PartnerPO> result = baseMapper.selectPage(poPage, wrapper);
        return ConvertHelper.convertPageInfo(result, PartnerDO.class);
    }

    @Override
    public List<String> listCapabilities(String partnerId) {
        List<PartnerCapabilityPO> rows = capabilityMapper.selectList(
                new LambdaQueryWrapper<PartnerCapabilityPO>()
                        .eq(PartnerCapabilityPO::getPartnerId, partnerId));
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        return rows.stream().map(PartnerCapabilityPO::getCapability).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceCapabilities(String partnerId, List<String> capabilities) {
        capabilityMapper.delete(new LambdaQueryWrapper<PartnerCapabilityPO>()
                .eq(PartnerCapabilityPO::getPartnerId, partnerId));
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
            capabilityMapper.insert(ConvertHelper.convert(row, PartnerCapabilityPO.class));
        }

    }

    @Override
    public PartnerCredentialDO findCredentialByClientId(String clientId) {
        PartnerCredentialPO po = credentialMapper.selectOne(new LambdaQueryWrapper<PartnerCredentialPO>()
                .eq(PartnerCredentialPO::getClientId, clientId));
        return ConvertHelper.convert(po, PartnerCredentialDO.class);
    }

    @Override
    public void insertCredential(PartnerCredentialDO credential) {
        credentialMapper.insert(ConvertHelper.convert(credential, PartnerCredentialPO.class));
    }

    @Override
    public List<PartnerCredentialDO> listCredentials(String partnerId) {
        List<PartnerCredentialPO> rows = credentialMapper.selectList(
                new LambdaQueryWrapper<PartnerCredentialPO>()
                        .eq(PartnerCredentialPO::getPartnerId, partnerId)
                        .orderByDesc(PartnerCredentialPO::getCreatedAt));
        return ConvertHelper.convertList(rows, PartnerCredentialDO.class);
    }

    @Override
    public String findCallbackUrl(String partnerId) {
        PartnerWebhookConfigPO config = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<PartnerWebhookConfigPO>()
                        .eq(PartnerWebhookConfigPO::getPartnerId, partnerId));
        return config != null ? config.getCallbackUrl() : null;
    }

    @Override
    public void upsertCallbackUrl(String partnerId, String callbackUrl) {
        if (!StringUtils.hasText(callbackUrl)) {
            return;
        }
        PartnerWebhookConfigPO existing = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<PartnerWebhookConfigPO>()
                        .eq(PartnerWebhookConfigPO::getPartnerId, partnerId));
        Date now = new Date();
        if (existing == null) {
            PartnerWebhookConfigDO row = new PartnerWebhookConfigDO();
            row.setPartnerId(partnerId);
            row.setCallbackUrl(callbackUrl);
            row.setUpdatedAt(now);
            webhookConfigMapper.insert(ConvertHelper.convert(row, PartnerWebhookConfigPO.class));
        } else {
            existing.setCallbackUrl(callbackUrl);
            existing.setUpdatedAt(now);
            webhookConfigMapper.updateById(existing);
        }

    }

    @Override
    public PartnerWebhookConfigDO findWebhookConfig(String partnerId) {
        PartnerWebhookConfigPO config = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<PartnerWebhookConfigPO>()
                        .eq(PartnerWebhookConfigPO::getPartnerId, partnerId));
        return config != null ? ConvertHelper.convert(config, PartnerWebhookConfigDO.class) : null;
    }

}
