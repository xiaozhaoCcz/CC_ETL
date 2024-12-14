package com.cc.job.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.JobNodeMapper;
import com.cc.job.task.model.entity.JobNode;
import com.cc.job.task.service.JobNodeService;
import org.springframework.stereotype.Service;

@Service
public class JobNodeServiceImpl extends ServiceImpl<JobNodeMapper, JobNode> implements JobNodeService {
}
