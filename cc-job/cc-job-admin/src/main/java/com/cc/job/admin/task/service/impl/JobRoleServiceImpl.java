package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.service.JobRoleService;
import com.cc.job.xo.mapper.JobRoleMapper;
import com.cc.job.xo.model.entity.JobRole;
import org.springframework.stereotype.Service;

@Service
public class JobRoleServiceImpl extends ServiceImpl<JobRoleMapper, JobRole> implements JobRoleService {
}
