package com.vtc.openapi.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "open-api")
public class OpenApiProperties {

    private final Svmp svmp = new Svmp();
    private final Token token = new Token();
    private final Admin admin = new Admin();

    public Svmp getSvmp() {
        return svmp;
    }

    public Token getToken() {
        return token;
    }

    public Admin getAdmin() {
        return admin;
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
