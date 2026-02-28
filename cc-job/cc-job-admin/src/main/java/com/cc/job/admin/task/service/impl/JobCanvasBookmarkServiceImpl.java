package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.service.JobCanvasBookmarkService;
import com.cc.job.xo.mapper.JobCanvasBookmarkMapper;
import com.cc.job.xo.model.entity.JobCanvasBookmark;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobCanvasBookmarkServiceImpl extends ServiceImpl<JobCanvasBookmarkMapper, JobCanvasBookmark> implements JobCanvasBookmarkService {

    @Override
    public List<JobCanvasBookmark> listByTaskGroupId(Long taskGroupId) {
        LambdaQueryWrapper<JobCanvasBookmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobCanvasBookmark::getTaskGroupId, taskGroupId);
        wrapper.orderByDesc(JobCanvasBookmark::getCreateTime);
        return list(wrapper);
    }
}
