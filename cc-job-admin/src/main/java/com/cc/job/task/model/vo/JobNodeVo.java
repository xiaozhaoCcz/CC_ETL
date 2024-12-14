package com.cc.job.task.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobNodeVo implements Serializable {

    private Long id;

    private Long taskId;

    private String taskName;

    private Long taskParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

    private Integer sort;

    private String nodePatentId;

}
