package com.vtc.openapi.infra.feign.dto;

/**
 * 映射 vul-pass {@code VulTaskDispatchAssetDTO}。
 */
public class VulPassDispatchAsset {

    private String assetType;
    private String assetId;
    private String protocol;
    private String port;
    private String assetIp;

    public VulPassDispatchAsset() {
    }

    public VulPassDispatchAsset(String assetIp) {
        this.assetIp = assetIp;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAssetIp() {
        return assetIp;
    }

    public void setAssetIp(String assetIp) {
        this.assetIp = assetIp;
    }
}
