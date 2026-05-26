package com.vtc.openapi.app.convert;

import com.botany.spore.ddd.app.convertor.IAppConvertor;
import com.vtc.openapi.domain.open.model.entity.ApiInvocationDO;
import com.vtc.openapi.ui.dto.admin.InvocationDTO;
import org.springframework.stereotype.Component;

@Component
public class InvocationAppConvertor implements IAppConvertor<InvocationDTO, ApiInvocationDO> {

    @Override
    public InvocationDTO doToDto(ApiInvocationDO domainObject) {
        return IAppConvertor.super.doToDto(domainObject);
    }

    @Override
    public ApiInvocationDO dtoToDo(InvocationDTO dto) {
        return IAppConvertor.super.dtoToDo(dto);
    }
}
