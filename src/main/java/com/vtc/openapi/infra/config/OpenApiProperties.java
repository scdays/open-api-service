package com.vtc.openapi.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "open-api")
public class OpenApiProperties {

    private final Svmp svmp = new Svmp();
    private final Token token = new Token();
    private final Idempotency idempotency = new Idempotency();
    private final Admin admin = new Admin();
    private final Engine engine = new Engine();
    private final Export export = new Export();
    private final FileSharing fileSharing = new FileSharing();
    private final PartnerGateway partnerGateway = new PartnerGateway();
    private final Webhook webhook = new Webhook();

    public Svmp getSvmp() {
        return svmp;
    }

    public Token getToken() {
        return token;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Engine getEngine() {
        return engine;
    }

    public Export getExport() {
        return export;
    }

    public FileSharing getFileSharing() {
        return fileSharing;
    }

    public PartnerGateway getPartnerGateway() {
        return partnerGateway;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    /**
     * 引擎适配模式：mock（联调/fixture）或 vul-pass（生产）。
     */
    public static class Engine {
        /** mock | vul-pass */
        private String adapterMode = "vul-pass";
        private final Mock mock = new Mock();

        public String getAdapterMode() {
            return adapterMode;
        }

        public void setAdapterMode(String adapterMode) {
            this.adapterMode = adapterMode;
        }

        public Mock getMock() {
            return mock;
        }

        public static class Mock {
            /** classpath:mock/engine 或 file:/path/to/mock */
            private String dataDir = "classpath:mock/engine";
            private String defaultBundle = "default";
            /**
             * auto：delay 后自动 FINISHED 并 ingest；
             * manual：保持 RUNNING，运营导入 XML 后 Admin API 触发 FINISHED + ingest。
             */
            private String ingestMode = "auto";
            /** NSFocus XML → instances.json，manual 导入时 subprocess 调用 */
            private String importScriptPath = "";
            /** Python 可执行文件，默认 python */
            private String pythonCommand = "python";
            /**
             * java：内嵌 NsfocusMockXmlParser（默认）；
             * python：subprocess 调用 import-nsfocus-xml-to-mock-bundle.py。
             */
            private String xmlImportMode = "java";
            /** auto | vul | pwd | live | port */
            private String xmlImportProfile = "auto";

            public String getXmlImportMode() {
                return xmlImportMode;
            }

            public void setXmlImportMode(String xmlImportMode) {
                this.xmlImportMode = xmlImportMode;
            }

            public boolean isJavaXmlImportMode() {
                return !"python".equalsIgnoreCase(xmlImportMode);
            }

            public String getXmlImportProfile() {
                return xmlImportProfile;
            }

            public void setXmlImportProfile(String xmlImportProfile) {
                this.xmlImportProfile = xmlImportProfile;
            }
            /** 任务创建后多少秒返回 FINISHED（仅 ingest-mode=auto） */
            private int taskFinishDelaySeconds = 5;
            /** P1：任务 FINISHED 后从 fixture 入库实例 */
            private boolean autoIngestInstancesOnFinish = true;
            /** Mock 验证/核验复扫完成后触发外发的延迟秒数 */
            private int verifyScanDelaySeconds = 3;

            public String getIngestMode() {
                return ingestMode;
            }

            public void setIngestMode(String ingestMode) {
                this.ingestMode = ingestMode;
            }

            public boolean isManualIngestMode() {
                return "manual".equalsIgnoreCase(ingestMode);
            }

            public String getImportScriptPath() {
                return importScriptPath;
            }

            public void setImportScriptPath(String importScriptPath) {
                this.importScriptPath = importScriptPath;
            }

            public String getPythonCommand() {
                return pythonCommand;
            }

            public void setPythonCommand(String pythonCommand) {
                this.pythonCommand = pythonCommand;
            }

            public int getVerifyScanDelaySeconds() {
                return verifyScanDelaySeconds;
            }

            public void setVerifyScanDelaySeconds(int verifyScanDelaySeconds) {
                this.verifyScanDelaySeconds = verifyScanDelaySeconds;
            }

            public String getDataDir() {
                return dataDir;
            }

            public void setDataDir(String dataDir) {
                this.dataDir = dataDir;
            }

            public String getDefaultBundle() {
                return defaultBundle;
            }

            public void setDefaultBundle(String defaultBundle) {
                this.defaultBundle = defaultBundle;
            }

            public int getTaskFinishDelaySeconds() {
                return taskFinishDelaySeconds;
            }

            public void setTaskFinishDelaySeconds(int taskFinishDelaySeconds) {
                this.taskFinishDelaySeconds = taskFinishDelaySeconds;
            }

            public boolean isAutoIngestInstancesOnFinish() {
                return autoIngestInstancesOnFinish;
            }

            public void setAutoIngestInstancesOnFinish(boolean autoIngestInstancesOnFinish) {
                this.autoIngestInstancesOnFinish = autoIngestInstancesOnFinish;
            }
        }
    }

    public static class Svmp {
        private String engineServiceName = "vul-pass";
        /** vul-pass Controller 根路径；真实接口为 /vul-scan-task/* */
        private String enginePathPrefix = "";
        private final Dispatch dispatch = new Dispatch();

        public String getEngineServiceName() {
            return engineServiceName;
        }

        public void setEngineServiceName(String engineServiceName) {
            this.engineServiceName = engineServiceName;
        }

        public String getEnginePathPrefix() {
            return enginePathPrefix;
        }

        public void setEnginePathPrefix(String enginePathPrefix) {
            this.enginePathPrefix = enginePathPrefix;
        }

        public Dispatch getDispatch() {
            return dispatch;
        }

        /**
         * 开放平台创建任务 → vul-pass {@code POST /vul-scan-task/dispatch} 所需部侧工单上下文。
         * orderId 须在 vul-pass 指令库中已存在（格式 {@code 1-31-19位数字}）。
         */
        public static class Dispatch {
            private String orderId;
            private Integer tskPhase = 1;
            private Integer procMethod = 1022;
            private java.util.List<String> engHashes = java.util.Collections.singletonList("NA");

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

            public java.util.List<String> getEngHashes() {
                return engHashes;
            }

            public void setEngHashes(java.util.List<String> engHashes) {
                this.engHashes = engHashes;
            }
        }
    }

    public static class Token {
        /** accessToken 有效秒数，默认 24h */
        private long expiresInSeconds = 86400L;

        public long getExpiresInSeconds() {
            return expiresInSeconds;
        }

        public void setExpiresInSeconds(long expiresInSeconds) {
            this.expiresInSeconds = expiresInSeconds;
        }
    }

    /** 实例写操作 Idempotency-Key 缓存配置（文档 §4.2） */
    public static class Idempotency {
        /** 缓存有效期秒数，默认 24h */
        private long ttlSeconds = 86400L;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Admin {
        /**
         * 内网管理 API 密钥；请求头 {@code X-Internal-Admin-Key} 须匹配。
         * 生产环境由 Nacos 覆盖，禁止公网暴露 /internal/admin/**。
         */
        private String apiKey = "change-me-internal-admin-key";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Export {
        private boolean enabled = true;
        private int ttlDays = 7;
        /**
         * open-api: Partner 网关 /api/open/v1/exports/{exportId}/download；
         * file-sharing: 平台 Nginx 直链 file-sharing-center。
         */
        private String downloadUrlMode = "open-api";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTtlDays() {
            return ttlDays;
        }

        public void setTtlDays(int ttlDays) {
            this.ttlDays = ttlDays;
        }

        public String getDownloadUrlMode() {
            return downloadUrlMode;
        }

        public void setDownloadUrlMode(String downloadUrlMode) {
            this.downloadUrlMode = downloadUrlMode;
        }
    }

    public static class PartnerGateway {
        /** Partner 对外网关根地址，如 http://172.16.2.4:35770 */
        private String publicBaseUrl = "http://127.0.0.1:35770";

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class FileSharing {
        private String publicBaseUrl = "http://127.0.0.1";
        private String downloadUsername = "admin";

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public String getDownloadUsername() {
            return downloadUsername;
        }

        public void setDownloadUsername(String downloadUsername) {
            this.downloadUsername = downloadUsername;
        }
    }

    public static class Webhook {
        private boolean enabled = true;
        /** 是否启用内置 Webhook 测试接收端（生产建议关闭） */
        private boolean testReceiverEnabled = true;
        /** 测试接收端所在服务根地址，如 http://127.0.0.1:35780 */
        private String testReceiverBaseUrl = "http://127.0.0.1:35780";
        /** 测试接收端路径，Partner defaultCallbackUrl 可填完整 URL */
        private String testReceiverPath = "/internal/dev/webhook/receive";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTestReceiverEnabled() {
            return testReceiverEnabled;
        }

        public void setTestReceiverEnabled(boolean testReceiverEnabled) {
            this.testReceiverEnabled = testReceiverEnabled;
        }

        public String getTestReceiverBaseUrl() {
            return testReceiverBaseUrl;
        }

        public void setTestReceiverBaseUrl(String testReceiverBaseUrl) {
            this.testReceiverBaseUrl = testReceiverBaseUrl;
        }

        public String getTestReceiverPath() {
            return testReceiverPath;
        }

        public void setTestReceiverPath(String testReceiverPath) {
            this.testReceiverPath = testReceiverPath;
        }
    }
}
