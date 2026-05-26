package com.vtc.openapi.app.convert;

import com.botany.spore.ddd.app.convertor.IAppConvertor;
import com.vtc.openapi.domain.partner.model.entity.PartnerCredentialDO;
import com.vtc.openapi.ui.dto.admin.PartnerCredentialDTO;
import org.springframework.stereotype.Component;

@Component
public class PartnerCredentialAppConvertor implements IAppConvertor<PartnerCredentialDTO, PartnerCredentialDO> {

    @Override
    public PartnerCredentialDTO doToDto(PartnerCredentialDO domainObject) {
        return IAppConvertor.super.doToDto(domainObject);
    }

    @Override
    public PartnerCredentialDO dtoToDo(PartnerCredentialDTO dto) {
        return IAppConvertor.super.dtoToDo(dto);
    }
}
