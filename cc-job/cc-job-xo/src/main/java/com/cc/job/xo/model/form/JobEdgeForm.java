package com.cc.job.xo.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

/**
 * JobEdge表单对象
 *
 * @author ccjob
 * @since 2024-12-XX
 */
@Getter
@Setter
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
}

