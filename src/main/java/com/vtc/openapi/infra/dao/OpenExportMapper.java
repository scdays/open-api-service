package com.vtc.openapi.infra.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vtc.openapi.infra.dao.po.OpenExportPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpenExportMapper extends BaseMapper<OpenExportPO> {
}
