package com.cc.job.xo.model.vo;


import java.io.Serializable;
import java.util.List;

public class JobPartVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String label;

    private Integer type;

    private List<JobPartVo> children;

    private String ext1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<JobPartVo> getChildren() {
        return children;
    }

    public void setChildren(List<JobPartVo> children) {
        this.children = children;
    }

    public String getExt1() {
        return ext1;
    }

    public void setExt1(String ext1) {
        this.ext1 = ext1;
    }
}
