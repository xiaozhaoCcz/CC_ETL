package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("job_user")
public class JobUser extends BaseEntity {

    private String username;

    private String password;

    private String ex1;

    private String ex2;
}
