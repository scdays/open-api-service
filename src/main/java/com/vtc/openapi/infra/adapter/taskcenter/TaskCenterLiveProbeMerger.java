package com.vtc.openapi.infra.adapter.taskcenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 合并 VTC 存活成功/失败 IP 与任务目标 hosts，保证 TaskExport liveProbeResults 完整性。
 */
@Component
public class TaskCenterLiveProbeMerger {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterLiveProbeMerger.class);

    public static class MergeInput {
        private Set<String> successIps = new LinkedHashSet<>();
        private Set<String> failIps = new LinkedHashSet<>();
        private List<String> taskHosts = new ArrayList<>();
        private boolean successQueryOk = true;
        private boolean failQueryOk = true;

        public Set<String> getSuccessIps() {
            return successIps;
        }

        public void setSuccessIps(Set<String> successIps) {
            this.successIps = successIps != null ? successIps : new LinkedHashSet<>();
        }

        public Set<String> getFailIps() {
            return failIps;
        }

        public void setFailIps(Set<String> failIps) {
            this.failIps = failIps != null ? failIps : new LinkedHashSet<>();
        }

        public List<String> getTaskHosts() {
            return taskHosts;
        }

        public void setTaskHosts(List<String> taskHosts) {
            this.taskHosts = taskHosts != null ? taskHosts : new ArrayList<>();
        }

        public boolean isSuccessQueryOk() {
            return successQueryOk;
        }

        public void setSuccessQueryOk(boolean successQueryOk) {
            this.successQueryOk = successQueryOk;
        }

        public boolean isFailQueryOk() {
            return failQueryOk;
        }

        public void setFailQueryOk(boolean failQueryOk) {
            this.failQueryOk = failQueryOk;
        }
    }

    /**
     * @return address -> alive
     */
    public Map<String, Boolean> merge(MergeInput input) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        if (input == null) {
            return result;
        }
        Set<String> success = normalizeSet(input.getSuccessIps());
        Set<String> fail = normalizeSet(input.getFailIps());
        Set<String> baseline = normalizeList(input.getTaskHosts());

        for (String ip : success) {
            result.put(ip, true);
        }
        for (String ip : fail) {
            if (result.containsKey(ip) && Boolean.TRUE.equals(result.get(ip))) {
                log.warn("live probe conflict: {} in both success and fail lists, keep alive=true", ip);
                continue;
            }
            result.put(ip, false);
        }
        for (String ip : baseline) {
            if (!result.containsKey(ip)) {
                if (!input.isSuccessQueryOk() && !input.isFailQueryOk()) {
                    result.put(ip, false);
                } else if (!input.isSuccessQueryOk()) {
                    result.put(ip, false);
                } else if (!input.isFailQueryOk()) {
                    result.put(ip, true);
                } else {
                    result.put(ip, false);
                }
                log.debug("live probe baseline host missing from VTC lists: {}", ip);
            }
        }
        if (!input.isSuccessQueryOk()) {
            log.warn("live probe success IP query failed, merged with fail+baseline only");
        }
        if (!input.isFailQueryOk()) {
            log.warn("live probe fail IP query failed, merged with success+baseline only");
        }
        return result;
    }

    private static Set<String> normalizeSet(Set<String> ips) {
        Set<String> out = new LinkedHashSet<>();
        if (ips == null) {
            return out;
        }
        for (String ip : ips) {
            String n = normalize(ip);
            if (StringUtils.hasText(n)) {
                out.add(n);
            }
        }
        return out;
    }

    private static Set<String> normalizeList(List<String> hosts) {
        Set<String> out = new LinkedHashSet<>();
        if (hosts == null) {
            return out;
        }
        for (String host : hosts) {
            String n = normalize(host);
            if (StringUtils.hasText(n)) {
                out.add(n);
            }
        }
        return out;
    }

    static String normalize(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        String trimmed = address.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.toLowerCase();
    }
}
