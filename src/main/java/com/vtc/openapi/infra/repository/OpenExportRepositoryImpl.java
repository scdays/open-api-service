package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.infra.dao.OpenExportFileMapper;
import com.vtc.openapi.infra.dao.OpenExportMapper;
import com.vtc.openapi.infra.dao.po.OpenExportFilePO;
import com.vtc.openapi.infra.dao.po.OpenExportPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class OpenExportRepositoryImpl implements IOpenExportRepository {

    private final OpenExportMapper exportMapper;
    private final OpenExportFileMapper exportFileMapper;

    public OpenExportRepositoryImpl(OpenExportMapper exportMapper, OpenExportFileMapper exportFileMapper) {
        this.exportMapper = exportMapper;
        this.exportFileMapper = exportFileMapper;
    }

    @Override
    public OpenExportDO findByExportId(String exportId) {
        if (!StringUtils.hasText(exportId)) {
            return null;
        }
        OpenExportPO po = exportMapper.selectOne(new LambdaQueryWrapper<OpenExportPO>()
                .eq(OpenExportPO::getExportId, exportId));
        return ConvertHelper.convert(po, OpenExportDO.class);
    }

    @Override
    public OpenExportDO findByPartnerAndExportId(String partnerId, String exportId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(exportId)) {
            return null;
        }
        OpenExportPO po = exportMapper.selectOne(new LambdaQueryWrapper<OpenExportPO>()
                .eq(OpenExportPO::getPartnerId, partnerId)
                .eq(OpenExportPO::getExportId, exportId));
        return ConvertHelper.convert(po, OpenExportDO.class);
    }

    @Override
    public OpenExportDO findByStageAndFormat(String partnerId, String taskId, String exportStage, String format) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(taskId)) {
            return null;
        }
        LambdaQueryWrapper<OpenExportPO> wrapper = new LambdaQueryWrapper<OpenExportPO>()
                .eq(OpenExportPO::getPartnerId, partnerId)
                .eq(OpenExportPO::getTaskId, taskId)
                .eq(OpenExportPO::getExportStage, exportStage)
                .eq(OpenExportPO::getFormat, format);
        OpenExportPO po = exportMapper.selectOne(wrapper);
        return ConvertHelper.convert(po, OpenExportDO.class);
    }

    @Override
    public void saveExport(OpenExportDO export) {
        exportMapper.insert(ConvertHelper.convert(export, OpenExportPO.class));
    }

    @Override
    public void updateExport(OpenExportDO export) {
        exportMapper.updateById(ConvertHelper.convert(export, OpenExportPO.class));
    }

    @Override
    public void saveExportFile(OpenExportFileDO file) {
        exportFileMapper.insert(ConvertHelper.convert(file, OpenExportFilePO.class));
    }

    @Override
    public OpenExportFileDO findFileByExportId(String exportId) {
        if (!StringUtils.hasText(exportId)) {
            return null;
        }
        OpenExportFilePO po = exportFileMapper.selectOne(new LambdaQueryWrapper<OpenExportFilePO>()
                .eq(OpenExportFilePO::getExportId, exportId));
        return ConvertHelper.convert(po, OpenExportFileDO.class);
    }

    @Override
    public PageInfo<OpenExportDO> pageByTask(String partnerId, String taskId, int page, int size) {
        Page<OpenExportPO> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<OpenExportPO> wrapper = new LambdaQueryWrapper<OpenExportPO>()
                .eq(OpenExportPO::getPartnerId, partnerId)
                .eq(OpenExportPO::getTaskId, taskId)
                .orderByDesc(OpenExportPO::getCreatedAt);
        Page<OpenExportPO> result = exportMapper.selectPage(mpPage, wrapper);
        PageInfo<OpenExportDO> pageInfo = new PageInfo<>();
        pageInfo.setCurrent(page);
        pageInfo.setSize(size);
        pageInfo.setTotal(result.getTotal());
        pageInfo.setRecords(ConvertHelper.convertList(result.getRecords(), OpenExportDO.class));
        return pageInfo;
    }
}
