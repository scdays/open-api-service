package com.vtc.openapi.infra.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("partner_task_map")
public class PartnerTaskMapPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String partnerId;
    private String extTaskId;
    private String platformTaskId;
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getExtTaskId() {
        return extTaskId;
    }

    public void setExtTaskId(String extTaskId) {
        this.extTaskId = extTaskId;
    }

    public String getPlatformTaskId() {
        return platformTaskId;
    }

    public void setPlatformTaskId(String platformTaskId) {
        this.platformTaskId = platformTaskId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
