package com.vtc.openapi.app.convert;

import com.botany.spore.ddd.app.convertor.IAppConvertor;
import com.vtc.openapi.domain.open.model.entity.WebhookDeliveryLogDO;
import com.vtc.openapi.ui.dto.admin.WebhookDeliveryLogDTO;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryLogAppConvertor implements IAppConvertor<WebhookDeliveryLogDTO, WebhookDeliveryLogDO> {

    @Override
    public WebhookDeliveryLogDTO doToDto(WebhookDeliveryLogDO domainObject) {
        return IAppConvertor.super.doToDto(domainObject);
    }

    @Override
    public WebhookDeliveryLogDO dtoToDo(WebhookDeliveryLogDTO dto) {
        return IAppConvertor.super.dtoToDo(dto);
    }
}
