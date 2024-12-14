package com.cc.job.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.JobEdgeMapper;
import com.cc.job.task.model.entity.JobEdge;
import com.cc.job.task.service.JobEdgeService;
import org.springframework.stereotype.Service;

@Service
public class JobEdgeServiceImpl extends ServiceImpl<JobEdgeMapper, JobEdge> implements JobEdgeService {
}
