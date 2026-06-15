package com.vtc.openapi.infra.adapter.task;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.result.ParsedScanTaskFileResult;
import com.vtc.openapi.domain.task.model.vo.ScanTaskTargets;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * 解析 §5.1.1 / 附录 G 扫描任务 XML（最小 F0 校验）。
 */
@Component
public class ScanTaskXmlParser {

    public ParsedScanTaskFileResult parse(String fileXml) {
        if (!StringUtils.hasText(fileXml)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "file 不能为空");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(fileXml.trim())));
            Element root = doc.getDocumentElement();
            if (root == null || !"scanTask".equals(root.getNodeName())) {
                throw invalidFile("根元素须为 scanTask");
            }

            Element server = firstChildElement(root, "server");
            if (server == null) {
                throw invalidFile("缺少 server 元素");
            }
            String taskName = textContent(server, "taskName");
            if (!StringUtils.hasText(taskName)) {
                throw invalidFile("server/taskName 不能为空");
            }
            String hosts = textContent(server, "targets");
            if (!StringUtils.hasText(hosts)) {
                throw invalidFile("server/targets 不能为空");
            }

            Element rootTargets = firstChildElement(root, "targets");
            if (rootTargets == null) {
                throw invalidFile("缺少根级 targets 元素（可为空）");
            }

            boolean inlineScanStage = hasInlineScanStage(server);
            Integer scanTemplateId = intContent(root, "scanTemplateId");
            Integer reportTemplateId = intContent(root, "reportTemplateId");
            boolean templateModeA = scanTemplateId != null && scanTemplateId > 0
                    && reportTemplateId != null && reportTemplateId > 0;
            Element report = firstChildElement(root, "report");
            if (templateModeA && (inlineScanStage || report != null)) {
                throw invalidFile("模板模式 A（scanTemplateId/reportTemplateId）与模式 B（内联扫描阶段/report）不可同时使用");
            }
            if (!templateModeA && inlineScanStage && report == null) {
                throw invalidFile("内联扫描阶段须同时提供 report 元素");
            }

            ScanTaskTargets targets = new ScanTaskTargets();
            targets.setHosts(hosts.trim());

            ParsedScanTaskFileResult parsed = new ParsedScanTaskFileResult();
            parsed.setTaskName(taskName.trim());
            parsed.setTargets(targets);
            parsed.setPriority(textContent(server, "priority"));
            parsed.setCallbackUrl(textContent(server, "callbackUrl"));
            parsed.setScanTemplateId(scanTemplateId);
            parsed.setReportTemplateId(reportTemplateId);
            parsed.setFileXml(fileXml);
            return parsed;
        } catch (OpenApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalidFile("file XML 解析失败: " + ex.getMessage());
        }
    }

    private static boolean hasInlineScanStage(Element server) {
        return firstChildElement(server, "liveProbe") != null
                || firstChildElement(server, "portScan") != null
                || firstChildElement(server, "vulnScan") != null
                || firstChildElement(server, "pwdGuess") != null;
    }

    private static Element firstChildElement(Element parent, String name) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static String textContent(Element parent, String childName) {
        Element child = firstChildElement(parent, childName);
        if (child == null) {
            return null;
        }
        String text = child.getTextContent();
        return text != null ? text.trim() : null;
    }

    private static Integer intContent(Element parent, String childName) {
        String text = textContent(parent, childName);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw invalidFile(childName + " 须为整数");
        }
    }

    private static OpenApiException invalidFile(String message) {
        return new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, message);
    }
}
