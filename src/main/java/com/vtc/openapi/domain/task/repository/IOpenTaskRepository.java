package com.vtc.openapi.domain.task.repository;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.domain.repository.IDatabaseRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.PartnerTaskMapDO;
import com.vtc.openapi.domain.task.model.query.OpenTaskAdminQuery;
import com.vtc.openapi.domain.task.model.query.OpenTaskListQuery;

import java.util.List;

public interface IOpenTaskRepository extends IDatabaseRepository<OpenTaskDO> {

    OpenTaskDO findByTaskId(String taskId);

    PartnerTaskMapDO findTaskMap(String partnerId, String extTaskId);

    void saveTaskMap(PartnerTaskMapDO map);

    PageInfo<OpenTaskDO> pageByPartner(String partnerId, OpenTaskListQuery query);

    PageInfo<OpenTaskDO> pageForAdmin(OpenTaskAdminQuery query);

    OpenTaskDO findByEngineTaskId(String engineTaskId);

    /** 按状态查询任务（轮询重试下发失败等） */
    java.util.List<OpenTaskDO> listByStatus(String status, int limit);

    /** 清空任务级失败原因（MyBatis-Plus updateById 无法将列更新为 null） */
    void clearErrorMessage(Long id);

    void updateCaseId(String taskId, String caseId);

    java.util.List<OpenTaskDO> listWithoutCaseId(int limit);
}
