package com.vtc.openapi.domain.task.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Component
@Scope("prototype")
public class OpenTaskDO extends BaseDO {

    private Long id;

    private String taskId;

    private String partnerId;

    private String extTaskId;

    private String engineTaskId;

    private String taskName;

    private String targetType;

    private Integer vulnType;

    private String targetsJson;

    private String status;

    private Integer progress;

    private Integer scanTemplateId;

    private String callbackUrl;

    private String optionsJson;

    private String errorMessage;

    private Date startedAt;

    private Date finishedAt;

    private Date createdAt;

    private Date updatedAt;
}
