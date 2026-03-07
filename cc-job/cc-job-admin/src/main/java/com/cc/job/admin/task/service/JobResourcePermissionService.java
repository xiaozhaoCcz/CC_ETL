package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobResourcePermission;

import java.util.List;

/**
 * 资源级权限服务
 */
public interface JobResourcePermissionService extends IService<JobResourcePermission> {

    List<JobResourcePermission> listByUserId(Long userId, String resourceType);

    void grant(Long userId, String resourceType, Long resourceId, String permissionType);

    void revoke(Long id);
}
