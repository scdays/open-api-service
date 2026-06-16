package com.vtc.openapi.domain.export.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Maps platform reportTemplateId to export serialization format(s).
 * See API doc appendix H.2: 2001=json, 2002=xml.
 */
public final class ReportTemplateCatalog {

    public static final int JSON_TEMPLATE_ID = 2001;
    public static final int XML_TEMPLATE_ID = 2002;

    private ReportTemplateCatalog() {
    }

    /**
     * @return lowercase format names to generate for the task, e.g. {@code json} or {@code xml}
     */
    public static String[] resolveFormats(Integer reportTemplateId) {
        if (reportTemplateId == null || reportTemplateId <= 0) {
            return new String[] {"json", "xml"};
        }
        if (reportTemplateId == JSON_TEMPLATE_ID) {
            return new String[] {"json"};
        }
        if (reportTemplateId == XML_TEMPLATE_ID) {
            return new String[] {"xml"};
        }
        return new String[] {"json"};
    }

    public static String resolvePrimaryFormat(Integer reportTemplateId) {
        String[] formats = resolveFormats(reportTemplateId);
        return formats.length > 0 ? formats[0] : "json";
    }

    public static Set<String> resolveFormatSet(Integer reportTemplateId) {
        Set<String> set = new LinkedHashSet<>();
        Collections.addAll(set, resolveFormats(reportTemplateId));
        return set;
    }
}
