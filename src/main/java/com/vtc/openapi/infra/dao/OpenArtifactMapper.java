package com.vtc.openapi.infra.dao;

import com.botany.spore.mybatis.IBaseMapper;
import com.vtc.openapi.infra.dao.po.OpenArtifactPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpenArtifactMapper extends IBaseMapper<OpenArtifactPO> {
}
