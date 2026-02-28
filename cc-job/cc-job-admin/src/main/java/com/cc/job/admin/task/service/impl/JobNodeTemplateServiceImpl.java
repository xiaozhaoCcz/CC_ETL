package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.service.JobNodeTemplateService;
import com.cc.job.xo.mapper.JobNodeTemplateMapper;
import com.cc.job.xo.model.entity.JobNodeTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobNodeTemplateServiceImpl extends ServiceImpl<JobNodeTemplateMapper, JobNodeTemplate> implements JobNodeTemplateService {

    @Override
    public List<JobNodeTemplate> listByCategoryAndUser(String category, Long createUserId) {
        LambdaQueryWrapper<JobNodeTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(category)) {
            wrapper.eq(JobNodeTemplate::getTemplateCategory, category);
        }
        if (createUserId != null) {
            wrapper.and(w -> w.eq(JobNodeTemplate::getCreateUserId, createUserId).or().eq(JobNodeTemplate::getIsPublic, 1));
        } else {
            wrapper.eq(JobNodeTemplate::getIsPublic, 1);
        }
        wrapper.orderByDesc(JobNodeTemplate::getCreateTime);
        return list(wrapper);
    }
}
