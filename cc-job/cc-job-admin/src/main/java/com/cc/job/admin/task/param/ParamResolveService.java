package com.cc.job.admin.task.param;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.xo.mapper.JobParamMapper;
import com.cc.job.xo.model.entity.JobParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行时替换 ${key} 为 job_param 表中的值
 */
@Service
public class ParamResolveService {

    private final JobParamMapper jobParamMapper;

    public ParamResolveService(JobParamMapper jobParamMapper) {
        this.jobParamMapper = jobParamMapper;
    }

    public String resolve(String text) {
        if (text == null || text.isEmpty()) return text;
        LambdaQueryWrapper<JobParam> q = new LambdaQueryWrapper<>();
        q.eq(JobParam::getIsDeleted, 0);
        List<JobParam> all = jobParamMapper.selectList(q);
        Map<String, String> map = new HashMap<>();
        for (JobParam p : all) {
            if (p.getParamKey() != null) map.put(p.getParamKey(), p.getParamValue() != null ? p.getParamValue() : "");
        }
        return com.cc.job.admin.task.controller.JobParamController.replacePlaceholders(text, map);
    }
}
