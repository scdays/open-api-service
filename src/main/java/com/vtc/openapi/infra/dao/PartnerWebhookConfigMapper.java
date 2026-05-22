package com.vtc.openapi.infra.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vtc.openapi.domain.partner.model.entity.PartnerWebhookConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PartnerWebhookConfigMapper extends BaseMapper<PartnerWebhookConfigDO> {
}
