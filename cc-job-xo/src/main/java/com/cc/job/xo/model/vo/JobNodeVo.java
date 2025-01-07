package com.cc.job.xo.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobNodeVo implements Serializable {

    private Long id;

    private Long jobId;

    private String jobName;

    private Long jobParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

    private Integer sort;

    private String nodePatentId;

}
