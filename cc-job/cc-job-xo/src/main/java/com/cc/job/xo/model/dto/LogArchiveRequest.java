package com.cc.job.xo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 日志归档请求：删除早于指定天数的日志
 */
@Schema(description = "日志归档请求")
public class LogArchiveRequest {

    @Schema(description = "保留最近 N 天，早于该天数的日志将被删除", example = "90")
    private int olderThanDays = 90;

    public int getOlderThanDays() {
        return olderThanDays;
    }

    public void setOlderThanDays(int olderThanDays) {
        this.olderThanDays = olderThanDays;
    }
}
