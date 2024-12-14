package com.cc.job.task.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobEdgeDto implements Serializable {

    private String id;

    private Long taskParentId;

    private String fromNodeId;

    private String endNodeId;
}
