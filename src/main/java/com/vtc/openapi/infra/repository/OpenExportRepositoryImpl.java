package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.infra.dao.OpenExportFileMapper;
import com.vtc.openapi.infra.dao.OpenExportMapper;
import com.vtc.openapi.infra.dao.po.OpenExportFilePO;
import com.vtc.openapi.infra.dao.po.OpenExportPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Collection;
import java.util.List;

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
    public OpenExportDO findBySubAndStage(String partnerId, String subId, String exportStage) {
        if (!StringUtils.hasText(subId) || !StringUtils.hasText(exportStage)) {
            return null;
        }
        LambdaQueryWrapper<OpenExportPO> wrapper = new LambdaQueryWrapper<OpenExportPO>()
                .eq(OpenExportPO::getSubId, subId)
                .eq(OpenExportPO::getExportStage, exportStage);
        if (StringUtils.hasText(partnerId)) {
            wrapper.eq(OpenExportPO::getPartnerId, partnerId);
        }
        OpenExportPO po = exportMapper.selectOne(wrapper);
        return ConvertHelper.convert(po, OpenExportDO.class);
    }

    @Override
    public void saveExport(OpenExportDO export) {
        exportMapper.insert(ConvertHelper.convert(export, OpenExportPO.class));
    }

    @Override
    public void updateExport(OpenExportDO export) {
        OpenExportPO po = ConvertHelper.convert(export, OpenExportPO.class);
        exportMapper.updateById(po);
        // updateById 默认忽略 null，errorMessage 为空时需显式清空，避免归档恢复后旧错误残留
        if (export.getId() != null && !StringUtils.hasText(export.getErrorMessage())) {
            exportMapper.update(null, new LambdaUpdateWrapper<OpenExportPO>()
                    .eq(OpenExportPO::getId, export.getId())
                    .set(OpenExportPO::getErrorMessage, null));
        }
    }

    @Override
    public void saveExportFile(OpenExportFileDO file) {
        exportFileMapper.insert(ConvertHelper.convert(file, OpenExportFilePO.class));
    }

    @Override
    public void updateExportFile(OpenExportFileDO file) {
        exportFileMapper.updateById(ConvertHelper.convert(file, OpenExportFilePO.class));
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
                .ne(OpenExportPO::getExportStage, ExportStage.RAW_SCAN_ARCHIVE)
                .orderByDesc(OpenExportPO::getCreatedAt);
        Page<OpenExportPO> result = exportMapper.selectPage(mpPage, wrapper);
        PageInfo<OpenExportDO> pageInfo = new PageInfo<>();
        pageInfo.setCurrent(page);
        pageInfo.setSize(size);
        pageInfo.setTotal(result.getTotal());
        pageInfo.setRecords(ConvertHelper.convertList(result.getRecords(), OpenExportDO.class));
        return pageInfo;
    }

    @Override
    public List<OpenExportDO> listByWebhookEventIds(Collection<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<OpenExportPO> pos = exportMapper.selectList(new LambdaQueryWrapper<OpenExportPO>()
                .in(OpenExportPO::getWebhookEventId, eventIds));
        return ConvertHelper.convertList(pos, OpenExportDO.class);
    }
}
