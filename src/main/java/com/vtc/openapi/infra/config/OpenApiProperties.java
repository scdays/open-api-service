package com.vtc.openapi.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "open-api")
public class OpenApiProperties {

    private final Svmp svmp = new Svmp();
    private final Token token = new Token();
    private final Idempotency idempotency = new Idempotency();
    private final Admin admin = new Admin();
    private final Engine engine = new Engine();

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
            /** 任务创建后多少秒返回 FINISHED */
            private int taskFinishDelaySeconds = 5;
            /** P1：任务 FINISHED 后从 fixture 入库实例 */
            private boolean autoIngestInstancesOnFinish = true;

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
}
