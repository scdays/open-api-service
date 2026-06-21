package com.vtc.openapi.domain.export.repository;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;

public interface IOpenExportRepository {

    OpenExportDO findByExportId(String exportId);

    OpenExportDO findByPartnerAndExportId(String partnerId, String exportId);

    OpenExportDO findByStageAndFormat(String partnerId, String taskId, String exportStage, String format);

    /** 按子任务 + 阶段查外发记录（原始报告归档查重/取记录用）。 */
    OpenExportDO findBySubAndStage(String partnerId, String subId, String exportStage);

    void saveExport(OpenExportDO export);

    void updateExport(OpenExportDO export);

    void saveExportFile(OpenExportFileDO file);

    /** 更新外发文件记录（retry 归档复用 exportId 时更新 fileField 等）。 */
    void updateExportFile(OpenExportFileDO file);

    OpenExportFileDO findFileByExportId(String exportId);

    PageInfo<OpenExportDO> pageByTask(String partnerId, String taskId, int page, int size);
}
