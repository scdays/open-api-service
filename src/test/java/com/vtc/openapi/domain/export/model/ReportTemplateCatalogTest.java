package com.vtc.openapi.domain.export.model;

import org.junit.Assert;
import org.junit.Test;

public class ReportTemplateCatalogTest {

    @Test
    public void jsonTemplateProducesJsonOnly() {
        Assert.assertArrayEquals(new String[] {"json"},
                ReportTemplateCatalog.resolveFormats(2001));
    }

    @Test
    public void xmlTemplateProducesXmlOnly() {
        Assert.assertArrayEquals(new String[] {"xml"},
                ReportTemplateCatalog.resolveFormats(2002));
    }

    @Test
    public void missingTemplateProducesBothFormats() {
        Assert.assertArrayEquals(new String[] {"json", "xml"},
                ReportTemplateCatalog.resolveFormats(0));
        Assert.assertArrayEquals(new String[] {"json", "xml"},
                ReportTemplateCatalog.resolveFormats(null));
    }

    @Test
    public void unknownTemplateDefaultsToJson() {
        Assert.assertArrayEquals(new String[] {"json"},
                ReportTemplateCatalog.resolveFormats(2999));
    }
}
