package com.vtc.openapi.infra.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vtc.openapi.infra.dao.po.OpenExportFilePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpenExportFileMapper extends BaseMapper<OpenExportFilePO> {
}
