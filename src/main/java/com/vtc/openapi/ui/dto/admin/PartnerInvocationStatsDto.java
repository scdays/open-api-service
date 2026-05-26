package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("PartnerInvocationStatsDto")
public class PartnerInvocationStatsDto {

    @ApiModelProperty("Partner ID")
    private String partnerId;

    @ApiModelProperty("今日总调用量")
    private long todayTotal;

    @ApiModelProperty("今日成功调用量")
    private long todaySuccess;

    @ApiModelProperty("今日成功率（0~1）")
    private double todaySuccessRate;

    @ApiModelProperty("Top 错误码")
    private List<ErrorCodeStatDto> topErrorCodes;

    @ApiModelProperty("近 7 日趋势")
    private List<DailyTrendDto> dailyTrend;

    @Data
    @ApiModel("ErrorCodeStatDto")
    public static class ErrorCodeStatDto {

        @ApiModelProperty("业务错误码")
        private Integer responseCode;

        @ApiModelProperty("出现次数")
        private Long count;
    }

    @Data
    @ApiModel("DailyTrendDto")
    public static class DailyTrendDto {

        @ApiModelProperty("统计日期（yyyy-MM-dd）")
        private String date;

        @ApiModelProperty("总调用量")
        private long totalCount;

        @ApiModelProperty("成功调用量")
        private long successCount;

        @ApiModelProperty("失败调用量")
        private long failCount;

        @ApiModelProperty("成功率（0~1）")
        private double successRate;
    }
}
