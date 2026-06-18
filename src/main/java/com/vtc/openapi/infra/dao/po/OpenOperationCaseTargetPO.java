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
@TableName("open_operation_case_target")
public class OpenOperationCaseTargetPO extends BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String caseId;
    private String targetKey;
    private String targetStatus;
    private Integer prevStat;
    private Integer resultStat;
    private String payloadJson;
    private Date createdAt;
}
