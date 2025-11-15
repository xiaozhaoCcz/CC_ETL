package com.cc.job.xo.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobEdgeVo implements Serializable {

    private String id;

    private Long jobParentId;

    private String fromNodeId;

    private String endNodeId;
    
    private String startPoint;  // 起始锚点
    
    private String endPoint;    // 结束锚点
    
    private String properties;  // 边属性
}
