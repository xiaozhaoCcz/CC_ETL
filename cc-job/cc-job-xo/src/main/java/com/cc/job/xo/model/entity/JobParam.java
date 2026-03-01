package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

/**
 * 参数/密钥：统一存储，任务或 DataX 中可用 ${key} 引用，执行时替换
 */
@TableName("job_param")
public class JobParam extends BaseEntity {

    private String paramKey;
    private String paramValue;
    private String comment;

    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }
    public String getParamValue() { return paramValue; }
    public void setParamValue(String paramValue) { this.paramValue = paramValue; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
