package com.vtc.openapi.infra.adapter.mock.nsfocus;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * NSFocus Aurora XML to mock instance JSON (aligned with import-nsfocus-xml-to-mock-bundle.py).
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class NsfocusMockXmlParser {

    public static final String NSFOCUS_VENDOR = "\u7eff\u76df\u79d1\u6280";

    private static final Logger log = LoggerFactory.getLogger(NsfocusMockXmlParser.class);

    private static final Pattern PORT_PATTERN = Pattern.compile(
            "^(6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|6[0-4][0-9]{3}|"
                    + "[1-5][0-9]{4}|[1-9][0-9]{0,3}|0)$");

    private static final double LOW_RISK = 4.0;
    private static final double MODERATE_RISK = 7.0;

    public NsfocusMockParseResult parse(byte[] xmlBytes, String profile, int limit, String sourceXmlName) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "XML content is empty");
        }
        Document doc = parseDocument(xmlBytes);
        Element root = doc.getDocumentElement();
        Element report = findReport(root);
        if (report == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "invalid NSFocus XML: missing report");
        }
        String vendor = childText(report, "vendor");
        if (!isNsfocusVendor(vendor)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "unsupported XML vendor (expect NSFocus): " + vendor);
        }

        Element taskElem = findFirstChild(report, "task");
        String taskId = childText(taskElem, "id");
        if (!StringUtils.hasText(taskId)) {
            taskId = "mock";
        }
        String taskName = childText(taskElem, "name");
        if (!StringUtils.hasText(taskName)) {
            taskName = sourceXmlName != null ? sourceXmlName.replace(".xml", "") : "mock-task";
        }
        String taskType = childText(taskElem, "task_type");
        String transferTime = parseScanTime(childText(taskElem, "time_end_scan"));

        String effectiveProfile = resolveProfile(profile, taskType);
        String scanKind = scanKindFor(effectiveProfile, taskType);

        NsfocusMockParseResult result = new NsfocusMockParseResult();
        result.setTaskId(taskId);
        result.setTaskName(taskName);
        result.setTaskType(taskType);
        result.setProfile(effectiveProfile);
        result.setScanKind(scanKind);
        result.setSourceXml(sourceXmlName);
        result.setTransferTime(transferTime);
        result.setVendor(vendor);

        Element targets = findFirstChild(report, "targets");
        if (targets == null) {
            return result;
        }
        int seq = 0;
        List<JSONObject> instances = new ArrayList<>();
        NodeList targetNodes = targets.getChildNodes();
        for (int i = 0; i < targetNodes.getLength(); i++) {
            Node node = targetNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE || !"target".equals(node.getNodeName())) {
                continue;
            }
            Element target = (Element) node;
            List<JSONObject> batch = parseTargetInstances(
                    target, taskId, taskType, transferTime, limit, seq, effectiveProfile, scanKind);
            for (JSONObject inst : batch) {
                if (limit > 0 && instances.size() >= limit) {
                    break;
                }
                instances.add(inst);
            }
            if (limit > 0 && instances.size() >= limit) {
                break;
            }
            seq = instances.size();
        }
        result.setInstances(instances);
        log.info("Nsfocus mock XML parsed: taskId={} profile={} instances={}", taskId, effectiveProfile, instances.size());
        return result;
    }

    private List<JSONObject> parseTargetInstances(Element target, String taskId, String taskType,
                                                  String transferTime, int limit, int seqStart,
                                                  String profile, String scanKind) {
        if ("live".equals(profile)) {
            return parseLiveInstances(target, taskId, transferTime, limit, seqStart);
        }
        if ("port".equals(profile)) {
            return parsePortInstances(target, taskId, transferTime, limit, seqStart);
        }
        String ip = childText(target, "ip");
        if (!StringUtils.hasText(ip)) {
            ip = "0.0.0.0";
        }
        Map<String, VulnDetail> details = loadVulnDetails(target);
        List<VulnScanned> scannedList = loadVulnScanned(target);
        List<JSONObject> instances = new ArrayList<>();
        int seq = seqStart;
        boolean weak = "pwd".equals(profile) || isWeakPasswordScan(taskType, target);

        if (weak) {
            Map<String, VulnScanned> scannedByService = indexScannedByService(scannedList);
            for (PasswordResult pwd : loadPasswordResults(target)) {
                if (!StringUtils.hasText(pwd.type)) {
                    continue;
                }
                VulnScanned scanned = findScannedForPassword(pwd.type, scannedList, scannedByService);
                if (scanned == null) {
                    continue;
                }
                VulnDetail detail = details.get(scanned.vulId);
                if (limit > 0 && instances.size() >= limit) {
                    break;
                }
                seq++;
                instances.add(makeInstance(seq, scanKind, taskId, ip, transferTime, scanned, detail, pwd));
            }
            if (instances.isEmpty()) {
                for (VulnScanned scanned : scannedList) {
                    VulnDetail detail = details.get(scanned.vulId);
                    if (detail == null && !StringUtils.hasText(scanned.messString)) {
                        continue;
                    }
                    if (limit > 0 && instances.size() >= limit) {
                        break;
                    }
                    seq++;
                    instances.add(makeInstance(seq, scanKind, taskId, ip, transferTime, scanned, detail, null));
                }
            }
        } else {
            for (VulnScanned scanned : scannedList) {
                VulnDetail detail = details.get(scanned.vulId);
                if (detail == null && !StringUtils.hasText(scanned.messString)) {
                    continue;
                }
                if (limit > 0 && instances.size() >= limit) {
                    break;
                }
                seq++;
                instances.add(makeInstance(seq, scanKind, taskId, ip, transferTime, scanned, detail, null));
            }
        }
        return instances;
    }

    private List<JSONObject> parseLiveInstances(Element target, String taskId, String transferTime,
                                                int limit, int seqStart) {
        String ip = childText(target, "ip");
        if (!StringUtils.hasText(ip)) {
            ip = "0.0.0.0";
        }
        List<JSONObject> instances = new ArrayList<>();
        if (limit > 0 && instances.size() >= limit) {
            return instances;
        }
        int seq = seqStart + 1;
        String vulInfoId = "VI-live-" + taskId + "-" + ip.replace(".", "-") + "-" + seq;
        JSONObject inst = baseInstance(seq, vulInfoId, "LIVE-" + taskId, "Host alive: " + ip,
                1, "LIVE-PROBE", ip, 0, "ICMP", transferTime);
        inst.put("extVulnRef", "liveProbe=true");
        instances.add(inst);
        return instances;
    }

    private List<JSONObject> parsePortInstances(Element target, String taskId, String transferTime,
                                                int limit, int seqStart) {
        String ip = childText(target, "ip");
        if (!StringUtils.hasText(ip)) {
            ip = "0.0.0.0";
        }
        List<JSONObject> instances = new ArrayList<>();
        Set<String> cache = new HashSet<>();
        Element appendix = findFirstChild(target, "appendix_info");
        if (appendix == null) {
            return instances;
        }
        int seq = seqStart;
        NodeList infoNodes = appendix.getChildNodes();
        for (int i = 0; i < infoNodes.getLength(); i++) {
            Node node = infoNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE || !"info".equals(node.getNodeName())) {
                continue;
            }
            Element info = (Element) node;
            String infoName = childText(info, "info_name");
            if (!isPortAppendix(infoName)) {
                continue;
            }
            AppendixTable table = parseAppendixTable(info);
            int idxPort = indexOfColumn(table.names, "port", "\u7aef\u53e3");
            int idxProto = indexOfColumn(table.names, "protocol", "\u534f\u8bae");
            int idxSvc = indexOfColumn(table.names, "service", "\u670d\u52a1");
            int idxState = indexOfColumn(table.names, "state", "\u72b6\u6001");
            for (List<String> row : table.rows) {
                if (row.size() <= idxPort) {
                    continue;
                }
                String portStr = row.get(idxPort);
                if (!PORT_PATTERN.matcher(portStr).matches()) {
                    continue;
                }
                String state = idxState < row.size() ? row.get(idxState).toLowerCase(Locale.ROOT) : "open";
                if (StringUtils.hasText(state) && !"open".equals(state) && !"opened".equals(state)) {
                    continue;
                }
                String proto = idxProto < row.size() ? row.get(idxProto) : "tcp";
                String service = idxSvc < row.size() ? row.get(idxSvc) : proto;
                String key = portStr + ":" + proto + ":" + service;
                if (cache.contains(key)) {
                    continue;
                }
                cache.add(key);
                if (limit > 0 && instances.size() >= limit) {
                    break;
                }
                seq++;
                String vulInfoId = "VI-port-" + taskId + "-" + portStr + "-" + seq;
                JSONObject inst = baseInstance(seq, vulInfoId, "PORT-" + portStr,
                        truncate("Open port: " + portStr + "/" + proto + " " + service, 200),
                        1, "PORT-SCAN", ip, parsePort(portStr), service, transferTime);
                inst.put("vulTransProto", proto != null ? proto.toUpperCase(Locale.ROOT) : null);
                inst.put("extVulnRef", state);
                instances.add(inst);
            }
        }
        return instances;
    }

    private static JSONObject makeInstance(int seq, String scanKind, String taskId, String ip,
                                           String transferTime, VulnScanned scanned,
                                           VulnDetail detail, PasswordResult pwd) {
        String vulId = scanned.vulId;
        int port = parsePort(scanned.port);
        String name = detail != null && StringUtils.hasText(detail.name)
                ? detail.name
                : (StringUtils.hasText(scanned.messString) ? scanned.messString : "NSFocus-" + vulId);
        double risk = detail != null ? detail.riskPoints : ("pwd".equals(scanKind) ? 5.0 : 0.0);
        String orgVulId = detail != null && StringUtils.hasText(detail.orgVulId())
                ? detail.orgVulId()
                : "NSFOCUS-" + vulId;
        String vulInfoId = String.format("VI-%s-%s-%s-%d-%d", scanKind, taskId, vulId, port, seq);
        JSONObject inst = baseInstance(seq, vulInfoId, "VUL-" + vulId, truncate(name, 200),
                riskToVulLevel(risk), truncate(orgVulId, 64), ip, port,
                StringUtils.hasText(scanned.service) ? scanned.service : scanned.protocol, transferTime);
        if (StringUtils.hasText(scanned.messString)) {
            inst.put("extVulnRef", truncate(scanned.messString, 500));
        }
        if (pwd != null) {
            inst.put("username", pwd.username);
            inst.put("password", pwd.password);
            inst.put("pwdType", pwd.type);
        }
        return inst;
    }

    private static JSONObject baseInstance(int seq, String vulInfoId, String vulId, String vulName,
                                           int vulLevel, String orgVulId, String ip, int port,
                                           String service, String transferTime) {
        JSONObject inst = new JSONObject();
        inst.put("id", seq);
        inst.put("vulInfoID", vulInfoId);
        inst.put("vulInfoId", vulInfoId);
        inst.put("vulID", vulId);
        inst.put("vulId", vulId);
        inst.put("vulInfoStat", 1);
        inst.put("vulName", vulName);
        inst.put("vulLevel", vulLevel);
        inst.put("orgVulId", orgVulId);
        inst.put("vulNetAddr", ip);
        inst.put("vulPort", port);
        inst.put("vulSvc", service);
        inst.put("isAccess", 0);
        inst.put("transferTime", transferTime);
        inst.put("vulnDisposalId", vulInfoId);
        return inst;
    }

    private Map<String, VulnDetail> loadVulnDetails(Element target) {
        Map<String, VulnDetail> details = new HashMap<>();
        Element detailRoot = findFirstChild(target, "vuln_detail");
        if (detailRoot == null) {
            return details;
        }
        NodeList nodes = detailRoot.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE || !"vuln".equals(node.getNodeName())) {
                continue;
            }
            Element vuln = (Element) node;
            String vulId = childText(vuln, "vul_id");
            if (!StringUtils.hasText(vulId)) {
                continue;
            }
            VulnDetail d = new VulnDetail();
            d.vulId = vulId;
            d.name = childText(vuln, "name");
            d.cveId = childText(vuln, "cve_id");
            d.cnnvd = childText(vuln, "cnnvd");
            String riskRaw = childText(vuln, "risk_points");
            try {
                d.riskPoints = StringUtils.hasText(riskRaw) ? Double.parseDouble(riskRaw.trim()) : 0.0;
            } catch (NumberFormatException ignored) {
                d.riskPoints = 0.0;
            }
            details.put(vulId, d);
        }
        return details;
    }

    private List<VulnScanned> loadVulnScanned(Element target) {
        List<VulnScanned> scanned = new ArrayList<>();
        Set<String> cache = new HashSet<>();
        Element root = findFirstChild(target, "vuln_scanned");
        if (root == null) {
            return scanned;
        }
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE || !"vuln".equals(node.getNodeName())) {
                continue;
            }
            Element vuln = (Element) node;
            VulnScanned vs = new VulnScanned();
            vs.port = childText(vuln, "port");
            vs.vulId = childText(vuln, "vul_id");
            vs.protocol = childText(vuln, "protocol");
            vs.service = childText(vuln, "service");
            vs.messString = childText(vuln, "mess_string");
            if (!PORT_PATTERN.matcher(vs.port).matches()) {
                continue;
            }
            String key = vs.port + ":" + vs.vulId + ":" + vs.protocol + ":" + vs.service;
            if (cache.contains(key)) {
                continue;
            }
            cache.add(key);
            scanned.add(vs);
        }
        return scanned;
    }

    private List<PasswordResult> loadPasswordResults(Element target) {
        List<PasswordResult> results = new ArrayList<>();
        Element root = findFirstChild(target, "password_results");
        if (root == null) {
            return results;
        }
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE || !"password_result".equals(node.getNodeName())) {
                continue;
            }
            Element elem = (Element) node;
            PasswordResult pwd = new PasswordResult();
            pwd.type = childText(elem, "type");
            if (!StringUtils.hasText(pwd.type)) {
                continue;
            }
            pwd.username = childText(elem, "username");
            pwd.password = childText(elem, "password");
            results.add(pwd);
        }
        return results;
    }

    private static boolean isWeakPasswordScan(String taskType, Element target) {
        if ("4".equals(taskType)) {
            return true;
        }
        Element root = findFirstChild(target, "password_results");
        if (root == null) {
            return false;
        }
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE
                    && "password_result".equals(nodes.item(i).getNodeName())) {
                return true;
            }
        }
        return false;
    }

    private static AppendixTable parseAppendixTable(Element info) {
        AppendixTable table = new AppendixTable();
        Element nameRoot = findFirstChild(info, "record_result_name");
        if (nameRoot != null) {
            NodeList nameNodes = nameRoot.getChildNodes();
            for (int i = 0; i < nameNodes.getLength(); i++) {
                Node n = nameNodes.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE && "name".equals(n.getNodeName())) {
                    table.names.add(n.getTextContent() != null ? n.getTextContent().trim() : "");
                }
            }
        }
        NodeList rrNodes = info.getElementsByTagName("record_results");
        for (int i = 0; i < rrNodes.getLength(); i++) {
            Node n = rrNodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE || !info.equals(n.getParentNode())) {
                continue;
            }
            Element rr = (Element) n;
            NodeList resultNodes = rr.getChildNodes();
            for (int j = 0; j < resultNodes.getLength(); j++) {
                Node rn = resultNodes.item(j);
                if (rn.getNodeType() != Node.ELEMENT_NODE || !"result".equals(rn.getNodeName())) {
                    continue;
                }
                Element result = (Element) rn;
                List<String> values = new ArrayList<>();
                NodeList valueNodes = result.getChildNodes();
                for (int k = 0; k < valueNodes.getLength(); k++) {
                    Node vn = valueNodes.item(k);
                    if (vn.getNodeType() == Node.ELEMENT_NODE && "value".equals(vn.getNodeName())) {
                        values.add(vn.getTextContent() != null ? vn.getTextContent().trim() : "");
                    }
                }
                if (!values.isEmpty()) {
                    table.rows.add(values);
                }
            }
        }
        return table;
    }

    private static Map<String, VulnScanned> indexScannedByService(List<VulnScanned> scannedList) {
        Map<String, VulnScanned> scannedByService = new HashMap<>();
        for (VulnScanned scanned : scannedList) {
            if (StringUtils.hasText(scanned.service)) {
                scannedByService.put(scanned.service.toUpperCase(Locale.ROOT), scanned);
            }
            if (StringUtils.hasText(scanned.protocol)) {
                scannedByService.putIfAbsent(scanned.protocol.toUpperCase(Locale.ROOT), scanned);
            }
        }
        return scannedByService;
    }

    private static VulnScanned findScannedForPassword(String pwdType, List<VulnScanned> scannedList,
                                                      Map<String, VulnScanned> scannedByService) {
        String typeKey = pwdType.toUpperCase(Locale.ROOT);
        VulnScanned scanned = scannedByService.get(typeKey);
        if (scanned != null) {
            return scanned;
        }
        String typeLower = pwdType.toLowerCase(Locale.ROOT);
        for (VulnScanned candidate : scannedList) {
            if (StringUtils.hasText(candidate.messString)
                    && candidate.messString.toLowerCase(Locale.ROOT).contains(typeLower)) {
                return candidate;
            }
        }
        return null;
    }

    private static Element findReport(Element root) {
        Element data = findFirstChild(root, "data");
        Element report = findFirstChild(data, "report");
        if (report != null) {
            return report;
        }
        NodeList reports = root.getElementsByTagName("report");
        if (reports.getLength() > 0) {
            return (Element) reports.item(0);
        }
        return null;
    }

    public static boolean isNsfocusVendor(String vendor) {
        if (!StringUtils.hasText(vendor)) {
            return false;
        }
        String trimmed = vendor.trim();
        if (NSFOCUS_VENDOR.equals(trimmed)) {
            return true;
        }
        return trimmed.contains("\u7eff\u76df") || trimmed.toLowerCase(Locale.ROOT).contains("nsfocus");
    }

    private static int indexOfColumn(List<String> names, String en, String zh) {
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            if (n == null) {
                continue;
            }
            if (n.toLowerCase(Locale.ROOT).equals(en) || n.contains(zh)) {
                return i;
            }
        }
        return en.equals("port") ? 0 : en.equals("protocol") ? 1 : en.equals("service") ? 2 : 3;
    }

    private static boolean isPortAppendix(String infoName) {
        if (!StringUtils.hasText(infoName)) {
            return false;
        }
        String lower = infoName.toLowerCase(Locale.ROOT);
        return lower.contains("port") || infoName.contains("\u7aef\u53e3");
    }

    private static String resolveProfile(String profile, String taskType) {
        if (!StringUtils.hasText(profile) || "auto".equalsIgnoreCase(profile)) {
            return "4".equals(taskType) ? "pwd" : "vul";
        }
        return profile.toLowerCase(Locale.ROOT);
    }

    private static String scanKindFor(String profile, String taskType) {
        if ("live".equals(profile) || "port".equals(profile)) {
            return profile;
        }
        if ("4".equals(taskType) || "pwd".equals(profile)) {
            return "pwd";
        }
        return "vul";
    }

    private static int riskToVulLevel(double risk) {
        if (risk >= MODERATE_RISK) {
            return 4;
        }
        if (risk >= LOW_RISK) {
            return 3;
        }
        return 2;
    }

    private static int parsePort(String portStr) {
        try {
            return Math.max(0, Integer.parseInt(portStr != null ? portStr.trim() : "0"));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String parseScanTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "1716192000";
        }
        String trimmed = raw.trim();
        for (String pattern : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss"}) {
            try {
                LocalDateTime dt = LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern(pattern));
                return String.valueOf(dt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
            } catch (Exception ignored) {
                // try next
            }
        }
        return "1716192000";
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static Document parseDocument(byte[] xmlBytes) {
        byte[] normalized = stripBom(xmlBytes);
        try {
            return parseDocumentBytes(normalized);
        } catch (OpenApiException first) {
            if (looksUtf8(normalized)) {
                throw first;
            }
            try {
                String decoded = new String(normalized, Charset.forName("GBK"));
                return parseDocumentBytes(decoded.getBytes(StandardCharsets.UTF_8));
            } catch (OpenApiException second) {
                throw first;
            }
        }
    }

    private static Document parseDocumentBytes(byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(bytes));
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception ex) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,
                    "failed to parse XML: " + ex.getMessage());
        }
    }

    private static byte[] stripBom(byte[] xmlBytes) {
        if (xmlBytes == null || xmlBytes.length < 3) {
            return xmlBytes;
        }
        if (xmlBytes[0] == (byte) 0xEF && xmlBytes[1] == (byte) 0xBB && xmlBytes[2] == (byte) 0xBF) {
            byte[] stripped = new byte[xmlBytes.length - 3];
            System.arraycopy(xmlBytes, 3, stripped, 0, stripped.length);
            return stripped;
        }
        return xmlBytes;
    }

    private static boolean looksUtf8(byte[] bytes) {
        try {
            String head = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8);
            return head.contains("<aurora") || head.contains("<report");
        } catch (Exception ignored) {
            return true;
        }
    }

    private static Element findFirstChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static String childText(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                String text = child.getTextContent();
                return text != null ? text.trim() : null;
            }
        }
        return null;
    }

    private static class VulnScanned {
        String port;
        String vulId;
        String protocol;
        String service;
        String messString;
    }

    private static class VulnDetail {
        String vulId;
        String name;
        String cveId;
        String cnnvd;
        double riskPoints;

        String orgVulId() {
            if (StringUtils.hasText(cveId)) {
                return cveId;
            }
            if (StringUtils.hasText(cnnvd)) {
                return cnnvd;
            }
            return null;
        }
    }

    private static class PasswordResult {
        String type;
        String username;
        String password;
    }

    private static class AppendixTable {
        List<String> names = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
    }
}
