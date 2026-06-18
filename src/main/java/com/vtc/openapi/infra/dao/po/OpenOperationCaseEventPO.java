package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.botany.spore.mybatis.pojo.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("open_operation_case_event")
public class OpenOperationCaseEventPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String caseId;
    private String eventType;
    private String eventPayloadJson;
    private Date createdAt;
}
