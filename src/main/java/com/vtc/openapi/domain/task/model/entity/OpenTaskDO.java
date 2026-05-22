package com.vtc.openapi.domain.task.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 开放平台任务（与 infra.dao.po.OpenTaskPO 对应，领域层引用）。
 */
@Data
@TableName("open_task")
public class OpenTaskDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String partnerId;

    private String extTaskId;

    private String engineTaskId;

    private String taskName;

    private String status;

    private Date createdAt;
}
