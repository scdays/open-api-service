package com.vtc.openapi.app.convert;

import com.botany.spore.ddd.app.convertor.IAppConvertor;
import com.vtc.openapi.domain.open.model.entity.ApiOperationDO;
import com.vtc.openapi.ui.dto.admin.ApiOperationDTO;
import org.springframework.stereotype.Component;

@Component
public class ApiOperationAppConvertor implements IAppConvertor<ApiOperationDTO, ApiOperationDO> {

    @Override
    public ApiOperationDTO doToDto(ApiOperationDO domainObject) {
        return IAppConvertor.super.doToDto(domainObject);
    }

    @Override
    public ApiOperationDO dtoToDo(ApiOperationDTO dto) {
        return IAppConvertor.super.dtoToDo(dto);
    }
}
