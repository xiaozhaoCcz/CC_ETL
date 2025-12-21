package com.cc.job.xo.model.form;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

/**
 * JobEdge表单对象
 *
 * @author ccjob
 * @since 2024-12-XX
 */
@Schema(description = "JobEdge表单对象")
public class JobEdgeForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父任务组ID")
    @NotNull(message = "父任务组ID不能为空")
    private Long jobParentId;

    @Schema(description = "源节点ID（jobId）")
    @NotNull(message = "源节点ID不能为空")
    private Long fromNodeId;

    @Schema(description = "目标节点ID（jobId）")
    @NotNull(message = "目标节点ID不能为空")
    private Long endNodeId;

    @Schema(description = "起始锚点位置")
    private String startPoint;

    @Schema(description = "结束锚点位置")
    private String endPoint;

    @Schema(description = "连线属性")
    private String properties;

    @Schema(description = "连线点列表")
    private String pointsList;

    public Long getJobParentId() {
        return jobParentId;
    }

    public void setJobParentId(Long jobParentId) {
        this.jobParentId = jobParentId;
    }

    public Long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(Long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public Long getEndNodeId() {
        return endNodeId;
    }

    public void setEndNodeId(Long endNodeId) {
        this.endNodeId = endNodeId;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getPointsList() {
        return pointsList;
    }

    public void setPointsList(String pointsList) {
        this.pointsList = pointsList;
    }
}

