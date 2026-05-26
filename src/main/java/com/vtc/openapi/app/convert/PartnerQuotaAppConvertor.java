package com.vtc.openapi.app.convert;

import com.botany.spore.ddd.app.convertor.IAppConvertor;
import com.vtc.openapi.domain.partner.model.entity.PartnerDO;
import com.vtc.openapi.ui.dto.admin.PartnerQuotaDTO;
import org.springframework.stereotype.Component;

@Component
public class PartnerQuotaAppConvertor implements IAppConvertor<PartnerQuotaDTO, PartnerDO> {

    @Override
    public PartnerQuotaDTO doToDto(PartnerDO domainObject) {
        return IAppConvertor.super.doToDto(domainObject);
    }

    @Override
    public PartnerDO dtoToDo(PartnerQuotaDTO dto) {
        return IAppConvertor.super.dtoToDo(dto);
    }
}
