package com.cc.job.task.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobEdgeVo implements Serializable {

    private Long id;

    private Long taskParentId;

    private Long fromNodeId;

    private Long endNodeId;
}
