package com.cc.job.admin.common.converter;

import cn.hutool.core.bean.BeanUtil;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.vo.JobInfoVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务对象转换器
 * 
 * 负责 Entity、Form、VO 之间的转换
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Component
public class JobConverter {

    /**
     * Form 转 Entity
     *
     * @param form 表单对象
     * @return 实体对象
     */
    public JobInfo toEntity(JobInfoForm form) {
        if (form == null) {
            return null;
        }
        return BeanUtil.copyProperties(form, JobInfo.class);
    }

    /**
     * Entity 转 VO
     *
     * @param entity 实体对象
     * @return VO对象
     */
    public JobInfoVO toVO(JobInfo entity) {
        if (entity == null) {
            return null;
        }
        return BeanUtil.copyProperties(entity, JobInfoVO.class);
    }

    /**
     * Entity 转 Form
     *
     * @param entity 实体对象
     * @return 表单对象
     */
    public JobInfoForm toForm(JobInfo entity) {
        if (entity == null) {
            return null;
        }
        return BeanUtil.copyProperties(entity, JobInfoForm.class);
    }

    /**
     * Entity列表 转 VO列表
     *
     * @param entities 实体列表
     * @return VO列表
     */
    public List<JobInfoVO> toVOList(List<JobInfo> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 更新实体属性
     *
     * @param form   表单对象
     * @param entity 实体对象
     */
    public void updateEntity(JobInfoForm form, JobInfo entity) {
        if (form == null || entity == null) {
            return;
        }
        
        BeanUtil.copyProperties(form, entity, "id", "createTime");
    }
}

