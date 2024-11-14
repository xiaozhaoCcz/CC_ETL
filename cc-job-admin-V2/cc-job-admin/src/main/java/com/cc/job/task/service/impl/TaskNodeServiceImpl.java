package com.cc.job.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskNodeMapper;
import com.cc.job.task.model.entity.TaskNode;
import com.cc.job.task.service.TaskNodeService;
import org.springframework.stereotype.Service;

@Service
public class TaskNodeServiceImpl extends ServiceImpl<TaskNodeMapper, TaskNode> implements TaskNodeService {
}
