package com.cc.job.xo.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobEdgeVo implements Serializable {

    private Long id;

    private Long jobParentId;

    private Long fromNodeId;

    private Long endNodeId;
}
