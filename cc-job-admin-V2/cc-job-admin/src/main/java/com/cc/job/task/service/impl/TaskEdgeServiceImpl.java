package com.cc.job.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskEdgeMapper;
import com.cc.job.task.model.entity.TaskEdge;
import com.cc.job.task.service.TaskEdgeService;
import org.springframework.stereotype.Service;

@Service
public class TaskEdgeServiceImpl extends ServiceImpl<TaskEdgeMapper, TaskEdge> implements TaskEdgeService {
}
