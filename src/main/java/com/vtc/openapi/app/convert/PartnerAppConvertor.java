package com.vtc.openapi.app.convert;

import com.botany.spore.ddd.app.convertor.IAppConvertor;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.ui.dto.admin.PartnerDTO;
import org.springframework.stereotype.Component;

@Component
public class PartnerAppConvertor implements IAppConvertor<PartnerDTO, PartnerDO> {

    @Override
    public PartnerDTO doToDto(PartnerDO domainObject) {
        return IAppConvertor.super.doToDto(domainObject);
    }

    @Override
    public PartnerDO dtoToDo(PartnerDTO dto) {
        return IAppConvertor.super.dtoToDo(dto);
    }
}
