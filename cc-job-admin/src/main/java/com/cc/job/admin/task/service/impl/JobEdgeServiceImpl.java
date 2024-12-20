package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobEdgeMapper;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.admin.task.service.JobEdgeService;
import org.springframework.stereotype.Service;

@Service
public class JobEdgeServiceImpl extends ServiceImpl<JobEdgeMapper, JobEdge> implements JobEdgeService {
}
