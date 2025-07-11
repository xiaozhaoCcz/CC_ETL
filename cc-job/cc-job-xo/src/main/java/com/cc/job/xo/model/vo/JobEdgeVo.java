package com.cc.job.xo.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobEdgeVo implements Serializable {

    private String id;

    private Long jobParentId;

    private String fromNodeId;

    private String endNodeId;
}
