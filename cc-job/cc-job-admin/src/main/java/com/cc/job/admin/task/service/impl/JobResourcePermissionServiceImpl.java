package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.service.JobResourcePermissionService;
import com.cc.job.xo.mapper.JobResourcePermissionMapper;
import com.cc.job.xo.model.entity.JobResourcePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobResourcePermissionServiceImpl extends ServiceImpl<JobResourcePermissionMapper, JobResourcePermission>
        implements JobResourcePermissionService {

    @Override
    public List<JobResourcePermission> listByUserId(Long userId, String resourceType) {
        LambdaQueryWrapper<JobResourcePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobResourcePermission::getUserId, userId);
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(JobResourcePermission::getResourceType, resourceType);
        }
        wrapper.eq(JobResourcePermission::getIsDeleted, 0);
        wrapper.orderByAsc(JobResourcePermission::getResourceType, JobResourcePermission::getResourceId);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(Long userId, String resourceType, Long resourceId, String permissionType) {
        if (userId == null || resourceType == null || resourceId == null || permissionType == null) {
            throw new IllegalArgumentException("userId, resourceType, resourceId, permissionType 不能为空");
        }
        long count = this.count(new LambdaQueryWrapper<JobResourcePermission>()
                .eq(JobResourcePermission::getUserId, userId)
                .eq(JobResourcePermission::getResourceType, resourceType)
                .eq(JobResourcePermission::getResourceId, resourceId)
                .eq(JobResourcePermission::getPermissionType, permissionType)
                .eq(JobResourcePermission::getIsDeleted, 0));
        if (count > 0) {
            return;
        }
        JobResourcePermission entity = new JobResourcePermission();
        entity.setUserId(userId);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setPermissionType(permissionType);
        this.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        if (id == null) {
            return;
        }
        this.removeById(id);
    }
}
