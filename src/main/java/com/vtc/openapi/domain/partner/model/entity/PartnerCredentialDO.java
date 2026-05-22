package com.vtc.openapi.domain.partner.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("partner_credential")
public class PartnerCredentialDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partnerId;

    private String clientId;

    /** secret 哈希，明文仅创建时返回一次 */
    private String clientSecretHash;

    private String status;

    private Date expiresAt;

    private Date createdAt;

    private Date updatedAt;
}
