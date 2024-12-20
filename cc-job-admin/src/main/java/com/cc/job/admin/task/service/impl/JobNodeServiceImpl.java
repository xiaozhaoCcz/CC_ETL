package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobNodeMapper;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.service.JobNodeService;
import org.springframework.stereotype.Service;

@Service
public class JobNodeServiceImpl extends ServiceImpl<JobNodeMapper, JobNode> implements JobNodeService {
}
