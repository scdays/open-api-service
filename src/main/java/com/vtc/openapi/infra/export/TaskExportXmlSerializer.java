package com.vtc.openapi.infra.export;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class TaskExportXmlSerializer {

    @SuppressWarnings("unchecked")
    public byte[] serialize(Map<String, Object> taskExportRoot) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<TaskExport>");
        Object inner = taskExportRoot.get("taskExport");
        if (inner instanceof Map) {
            appendMapChildren(sb, (Map<String, Object>) inner, 1);
        }
        sb.append("</TaskExport>");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private void appendMapChildren(StringBuilder sb, Map<String, Object> map, int indent) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            appendValue(sb, entry.getKey(), entry.getValue(), indent);
        }
    }

    @SuppressWarnings("unchecked")
    private void appendValue(StringBuilder sb, String tag, Object value, int indent) {
        String pad = repeat(indent);
        if (value == null) {
            return;
        }
        if (value instanceof Map) {
            sb.append(pad).append('<').append(tag).append(">\n");
            appendMapChildren(sb, (Map<String, Object>) value, indent + 2);
            sb.append(pad).append("</").append(tag).append(">\n");
        } else if (value instanceof Collection) {
            sb.append(pad).append('<').append(tag).append(">\n");
            String singular = singularTag(tag);
            for (Object item : (Collection<?>) value) {
                appendValue(sb, singular, item, indent + 2);
            }
            sb.append(pad).append("</").append(tag).append(">\n");
        } else {
            sb.append(pad).append('<').append(tag).append('>')
                    .append(escapeXml(String.valueOf(value)))
                    .append("</").append(tag).append(">\n");
        }
    }

    private static String singularTag(String plural) {
        if (plural.endsWith("ies")) {
            return plural.substring(0, plural.length() - 3) + "y";
        }
        if (plural.endsWith("s") && plural.length() > 1) {
            return plural.substring(0, plural.length() - 1);
        }
        return plural;
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String repeat(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
