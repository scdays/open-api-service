package com.vtc.openapi.domain.export.repository;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;

public interface IOpenExportRepository {

    OpenExportDO findByExportId(String exportId);

    OpenExportDO findByPartnerAndExportId(String partnerId, String exportId);

    OpenExportDO findByStageAndFormat(String partnerId, String taskId, String exportStage, String format);

    void saveExport(OpenExportDO export);

    void updateExport(OpenExportDO export);

    void saveExportFile(OpenExportFileDO file);

    OpenExportFileDO findFileByExportId(String exportId);

    PageInfo<OpenExportDO> pageByTask(String partnerId, String taskId, int page, int size);
}
