package com.cc.job.xo.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobNodeDto implements Serializable {

    private String id;

    private Long taskId;

    private Long taskParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;
}
