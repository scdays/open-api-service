package com.vtc.openapi.infra.feign.dto;

import java.util.List;

/**
 * 映射 vul-pass {@code VulTaskDispatchDTO}（不含 extTaskId）。
 */
public class VulPassDispatchRequest {

    private Integer tskType;
    private Integer tskModel;
    private Integer tskAction;
    private String tskName;
    private String orderId;
    private Integer tskPhase;
    private Integer procMethod;
    private String transId;
    private List<String> engHashes;
    private List<VulPassDispatchAsset> assetList;

    public Integer getTskType() {
        return tskType;
    }

    public void setTskType(Integer tskType) {
        this.tskType = tskType;
    }

    public Integer getTskModel() {
        return tskModel;
    }

    public void setTskModel(Integer tskModel) {
        this.tskModel = tskModel;
    }

    public Integer getTskAction() {
        return tskAction;
    }

    public void setTskAction(Integer tskAction) {
        this.tskAction = tskAction;
    }

    public String getTskName() {
        return tskName;
    }

    public void setTskName(String tskName) {
        this.tskName = tskName;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getTskPhase() {
        return tskPhase;
    }

    public void setTskPhase(Integer tskPhase) {
        this.tskPhase = tskPhase;
    }

    public Integer getProcMethod() {
        return procMethod;
    }

    public void setProcMethod(Integer procMethod) {
        this.procMethod = procMethod;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public List<String> getEngHashes() {
        return engHashes;
    }

    public void setEngHashes(List<String> engHashes) {
        this.engHashes = engHashes;
    }

    public List<VulPassDispatchAsset> getAssetList() {
        return assetList;
    }

    public void setAssetList(List<VulPassDispatchAsset> assetList) {
        this.assetList = assetList;
    }
}
