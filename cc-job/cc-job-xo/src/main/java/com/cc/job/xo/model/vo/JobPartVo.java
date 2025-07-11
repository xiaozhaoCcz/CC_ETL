package com.cc.job.xo.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class JobPartVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String label;

    private Integer type;

    private List<JobPartVo> children;

    private String ext1;
}
