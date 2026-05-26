package com.vtc.openapi.domain.open.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.infra.dao.po.WebhookDeliveryLogPO;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryLogDomainConvertor implements IDomainConvertor<WebhookDeliveryLogDO, WebhookDeliveryLogPO> {

    @Override
    public WebhookDeliveryLogPO doToPo(WebhookDeliveryLogDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public WebhookDeliveryLogDO poToDo(WebhookDeliveryLogPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
