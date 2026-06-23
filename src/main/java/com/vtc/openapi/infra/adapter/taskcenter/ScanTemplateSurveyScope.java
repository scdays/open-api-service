package com.vtc.openapi.infra.adapter.taskcenter;

import org.springframework.util.StringUtils;

/**
 * 按扫描模板 / 子任务类型限定从 VTC 拉取与落库的数据范围。
 * <ul>
 *   <li>1001 漏洞扫描：存活 + 端口 + 系统漏洞</li>
 *   <li>1002 存活探测：仅存活</li>
 *   <li>1003 端口扫描：存活 + 端口（不含系统漏洞）</li>
 * </ul>
 */
public final class ScanTemplateSurveyScope {

    private final boolean aliveProbe;
    private final boolean portScan;
    private final boolean vulnScan;

    private ScanTemplateSurveyScope(boolean aliveProbe, boolean portScan, boolean vulnScan) {
        this.aliveProbe = aliveProbe;
        this.portScan = portScan;
        this.vulnScan = vulnScan;
    }

    public static ScanTemplateSurveyScope fromScanTemplateId(Integer scanTemplateId) {
        if (scanTemplateId == null) {
            return full();
        }
        switch (scanTemplateId) {
            case 1002:
                return aliveOnly();
            case 1003:
                return portAndAlive();
            case 1001:
            default:
                return full();
        }
    }

    public static ScanTemplateSurveyScope fromCenterTaskType(String centerTaskType) {
        if (!StringUtils.hasText(centerTaskType)) {
            return full();
        }
        switch (centerTaskType.trim().toLowerCase()) {
            case "alive":
                return aliveOnly();
            case "port":
                return portAndAlive();
            case "vuln":
                return full();
            default:
                return full();
        }
    }

    /** 修复核验复扫：仅需漏洞结果做指纹比对。 */
    public static ScanTemplateSurveyScope vulnScanOnly() {
        return new ScanTemplateSurveyScope(false, false, true);
    }

    public static ScanTemplateSurveyScope full() {
        return new ScanTemplateSurveyScope(true, true, true);
    }

    public static ScanTemplateSurveyScope aliveOnly() {
        return new ScanTemplateSurveyScope(true, false, false);
    }

    public static ScanTemplateSurveyScope portAndAlive() {
        return new ScanTemplateSurveyScope(true, true, false);
    }

    public boolean needsAliveProbe() {
        return aliveProbe;
    }

    public boolean needsPortScan() {
        return portScan;
    }

    public boolean needsVulnScan() {
        return vulnScan;
    }
}
