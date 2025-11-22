package com.cc.job.xo.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class JobNodeVo implements Serializable {

    private String id;

    private Long jobId;

    private String jobName;

    private Long jobParentId;

    private Double nodePositionX;

    private Double nodePositionY;

    private Long nodeInDegree;

    private Long nodeOutDegree;

    private Integer sort;

    private String nodePatentId;

    private String properties;

    private String children;

    private String nodeType;

    private Integer isPause;

    private List<JobNodeVo> childrenNodes = new ArrayList<>();

    /**
     * 节点运行状态：0=失败, 1=成功, 2=运行中
     */
    private Integer triggerStatus;
}
