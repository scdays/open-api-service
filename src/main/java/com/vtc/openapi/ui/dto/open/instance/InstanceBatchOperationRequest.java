package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

/**
 * 实例批量操作请求。
 */
@Data
@ApiModel(description = "实例批量操作请求")
public class InstanceBatchOperationRequest {

    @ApiModelProperty(value = "批量操作条目，最多 100 条", required = true)
    private List<BatchItem> items;

    @Data
    @ApiModel(description = "批量操作单条")
    public static class BatchItem {

        @ApiModelProperty(value = "实例唯一 ID", required = true)
        private String vulInfoID;
        @ApiModelProperty(value = "验证结果（verify / verify-fix 时必填）")
        private String verifyResult;
        @ApiModelProperty(value = "修复方式（remediate 时可选）")
        private String srcMethod;
        @ApiModelProperty(value = "修复说明（remediate 时可选）")
        private String remedDesc;
        @ApiModelProperty(value = "修复链接（remediate 时可选）")
        private String fixLnk;
    }
}