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
public class PartnerTaskMapDO extends BaseDO {

    private Long id;

    private String partnerId;

    private String extTaskId;

    private String platformTaskId;

    private Date createdAt;
}
