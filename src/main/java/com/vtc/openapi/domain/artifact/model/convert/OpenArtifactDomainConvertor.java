package com.vtc.openapi.domain.artifact.model.convert;

import com.botany.spore.ddd.domain.model.convertor.IDomainConvertor;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.infra.dao.po.OpenArtifactPO;
import org.springframework.stereotype.Component;

@Component
public class OpenArtifactDomainConvertor implements IDomainConvertor<OpenArtifactDO, OpenArtifactPO> {

    @Override
    public OpenArtifactPO doToPo(OpenArtifactDO domainObject) {
        return IDomainConvertor.super.doToPo(domainObject);
    }

    @Override
    public OpenArtifactDO poToDo(OpenArtifactPO po) {
        return IDomainConvertor.super.poToDo(po);
    }
}
