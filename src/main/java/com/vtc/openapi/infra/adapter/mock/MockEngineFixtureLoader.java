package com.vtc.openapi.infra.adapter.mock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 从 {@code mock/engine/bundles/ * /instances.json} 加载 fixture，供 mock 适配器与 P1 实例入库使用。
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockEngineFixtureLoader {

    private static final Logger log = LoggerFactory.getLogger(MockEngineFixtureLoader.class);

    private final OpenApiProperties properties;
    private final ResourceLoader resourceLoader;

    private final Map<String, MockEngineBundle> bundlesById = new LinkedHashMap<>();

    public MockEngineFixtureLoader(OpenApiProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadBundles() {
        bundlesById.clear();
        String dataDir = properties.getEngine().getMock().getDataDir();
        if (!StringUtils.hasText(dataDir)) {
            log.warn("mock data-dir 未配置，使用空 fixture");
            return;
        }
        String base = dataDir.endsWith("/") ? dataDir : dataDir + "/";
        String pattern = base + "bundles/*/instances.json";
        try {
            Resource[] resources = org.springframework.core.io.support.ResourcePatternUtils
                    .getResourcePatternResolver(resourceLoader)
                    .getResources(resolvePattern(pattern));
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                MockEngineBundle bundle = parseBundle(resource);
                if (bundle != null && StringUtils.hasText(bundle.getBundleId())) {
                    bundlesById.put(bundle.getBundleId(), bundle);
                    log.info("Loaded mock bundle '{}' with {} instances from {}",
                            bundle.getBundleId(), bundle.getInstances().size(), resource.getDescription());
                }
            }
        } catch (IOException ex) {
            log.warn("加载 mock fixture 失败: {}", ex.getMessage());
        }
        if (bundlesById.isEmpty()) {
            log.warn("未加载任何 mock bundle，请检查 {}", pattern);
        }
    }

    public MockEngineBundle resolveBundle(String extTaskId, String taskName) {
        return resolveBundle(extTaskId, taskName, null, null, null);
    }

    public MockEngineBundle resolveBundle(String extTaskId, String taskName,
                                          Integer scanTemplateId, Integer reportTemplateId,
                                          Integer vulnType) {
        MockEngineBundle best = null;
        int bestScore = 0;
        for (MockEngineBundle bundle : bundlesById.values()) {
            int score = bundle.matchScore(extTaskId, taskName, scanTemplateId, reportTemplateId, vulnType);
            if (score > bestScore) {
                bestScore = score;
                best = bundle;
            }
        }
        if (best != null) {
            return best;
        }
        String defaultId = properties.getEngine().getMock().getDefaultBundle();
        MockEngineBundle fallback = bundlesById.get(defaultId);
        if (fallback != null) {
            return fallback;
        }
        return bundlesById.isEmpty() ? null : bundlesById.values().iterator().next();
    }

    public MockEngineBundle getBundle(String bundleId) {
        return bundlesById.get(bundleId);
    }

    public List<MockEngineBundle> listBundles() {
        return Collections.unmodifiableList(new ArrayList<>(bundlesById.values()));
    }

    /** 供管理/调试 reload（生产 fixture 热更新后可调用） */
    public void reload() {
        loadBundles();
    }

    private MockEngineBundle parseBundle(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            String json = readUtf8(in);
            JSONObject root = JSON.parseObject(json);
            MockEngineBundle bundle = new MockEngineBundle();
            bundle.setBundleId(root.getString("bundleId"));
            if (!StringUtils.hasText(bundle.getBundleId())) {
                bundle.setBundleId(guessBundleId(resource));
            }
            bundle.setDescription(root.getString("description"));
            JSONObject match = root.getJSONObject("match");
            if (match != null) {
                bundle.setExtTaskIdPrefix(match.getString("extTaskIdPrefix"));
                bundle.setTaskNameContains(match.getString("taskNameContains"));
                bundle.setScanTemplateId(match.getInteger("scanTemplateId"));
                if (match.getJSONArray("reportTemplateIds") != null) {
                    bundle.setReportTemplateIds(match.getJSONArray("reportTemplateIds").toJavaList(Integer.class));
                }
                if (match.getJSONArray("vulnTypes") != null) {
                    bundle.setVulnTypes(match.getJSONArray("vulnTypes").toJavaList(Integer.class));
                }
            }
            JSONArray instances = root.getJSONArray("instances");
            if (instances != null) {
                bundle.setInstances(instances.toJavaList(JSONObject.class));
            }
            return bundle;
        }
    }

    private static String guessBundleId(Resource resource) {
        String path = resource.getDescription();
        int bundlesIdx = path.indexOf("bundles/");
        if (bundlesIdx < 0) {
            return "default";
        }
        String tail = path.substring(bundlesIdx + "bundles/".length());
        int slash = tail.indexOf('/');
        return slash > 0 ? tail.substring(0, slash) : "default";
    }

    private static String readUtf8(InputStream in) {
        Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private static String resolvePattern(String pattern) {
        if (pattern.startsWith("classpath:") || pattern.startsWith("file:")) {
            return pattern;
        }
        return "classpath:" + pattern;
    }
}
