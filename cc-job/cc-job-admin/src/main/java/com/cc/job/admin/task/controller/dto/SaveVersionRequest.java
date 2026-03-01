package com.cc.job.admin.task.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 保存任务组为版本的请求体
 */
@Schema(description = "保存为版本请求")
public class SaveVersionRequest {

    @Schema(description = "任务组ID", required = true)
    private Long jobId;

    @Schema(description = "版本名称", example = "发布前备份")
    private String versionName;

    @Schema(description = "节点JSON", required = true)
    private String nodesJson;

    @Schema(description = "边JSON", required = true)
    private String edgesJson;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getNodesJson() {
        return nodesJson;
    }

    public void setNodesJson(String nodesJson) {
        this.nodesJson = nodesJson;
    }

    public String getEdgesJson() {
        return edgesJson;
    }

    public void setEdgesJson(String edgesJson) {
        this.edgesJson = edgesJson;
    }
}
