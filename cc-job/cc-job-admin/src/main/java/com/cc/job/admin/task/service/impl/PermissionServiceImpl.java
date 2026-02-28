package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.service.PermissionService;
import com.cc.job.xo.mapper.JobPermissionMapper;
import com.cc.job.xo.mapper.JobResourcePermissionMapper;
import com.cc.job.xo.mapper.JobRolePermissionMapper;
import com.cc.job.xo.mapper.JobUserRoleMapper;
import com.cc.job.xo.model.entity.JobPermission;
import com.cc.job.xo.model.entity.JobResourcePermission;
import com.cc.job.xo.model.entity.JobRolePermission;
import com.cc.job.xo.model.entity.JobUserRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限服务实现：全局角色权限 + 资源级权限
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Map<String, String> RESOURCE_ACTION_TO_CODE = Map.ofEntries(
            Map.entry("PART_VIEW", PermissionConstants.PART_VIEW),
            Map.entry("PART_EDIT", PermissionConstants.PART_EDIT),
            Map.entry("PART_DELETE", PermissionConstants.PART_DELETE),
            Map.entry("JOB_INFO_VIEW", PermissionConstants.JOB_INFO_VIEW),
            Map.entry("JOB_INFO_EDIT", PermissionConstants.JOB_INFO_EDIT),
            Map.entry("JOB_INFO_DELETE", PermissionConstants.JOB_INFO_DELETE),
            Map.entry("JOB_INFO_EXECUTE", PermissionConstants.JOB_INFO_EXECUTE),
            Map.entry("JOB_NODE_VIEW", PermissionConstants.JOB_NODE_VIEW),
            Map.entry("JOB_NODE_EDIT", PermissionConstants.JOB_NODE_EDIT),
            Map.entry("JOB_NODE_DELETE", PermissionConstants.JOB_NODE_DELETE),
            Map.entry("SYSTEM_MANAGE", PermissionConstants.PERMISSION_MANAGE)
    );

    private final JobUserRoleMapper jobUserRoleMapper;
    private final JobRolePermissionMapper jobRolePermissionMapper;
    private final JobPermissionMapper jobPermissionMapper;
    private final JobResourcePermissionMapper jobResourcePermissionMapper;

    public PermissionServiceImpl(JobUserRoleMapper jobUserRoleMapper,
                                 JobRolePermissionMapper jobRolePermissionMapper,
                                 JobPermissionMapper jobPermissionMapper,
                                 JobResourcePermissionMapper jobResourcePermissionMapper) {
        this.jobUserRoleMapper = jobUserRoleMapper;
        this.jobRolePermissionMapper = jobRolePermissionMapper;
        this.jobPermissionMapper = jobPermissionMapper;
        this.jobResourcePermissionMapper = jobResourcePermissionMapper;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }
        Set<String> codes = getPermissionCodesByUserId(userId);
        return codes.contains(permissionCode);
    }

    @Override
    public List<Long> getAllowedResourceIds(Long userId, String resourceType, String permissionType) {
        if (userId == null || resourceType == null || permissionType == null) {
            return Collections.emptyList();
        }
        String key = resourceType + "_" + permissionType;
        String permissionCode = RESOURCE_ACTION_TO_CODE.get(key);
        if (permissionCode != null && hasPermission(userId, permissionCode)) {
            return null; // 全部允许，调用方不按 id 过滤
        }
        List<JobResourcePermission> list = jobResourcePermissionMapper.selectList(
                new LambdaQueryWrapper<JobResourcePermission>()
                        .eq(JobResourcePermission::getUserId, userId)
                        .eq(JobResourcePermission::getResourceType, resourceType)
                        .eq(JobResourcePermission::getPermissionType, permissionType)
                        .eq(JobResourcePermission::getIsDeleted, 0)
        );
        return list.stream().map(JobResourcePermission::getResourceId).distinct().toList();
    }

    @Override
    public boolean canAccessResource(Long userId, String resourceType, String permissionType, Long resourceId) {
        if (userId == null || resourceId == null) {
            return false;
        }
        List<Long> allowed = getAllowedResourceIds(userId, resourceType, permissionType);
        if (allowed == null) {
            return true; // 全部允许
        }
        return allowed.contains(resourceId);
    }

    @Override
    public List<String> getPermissionCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return new ArrayList<>(getPermissionCodesByUserId(userId));
    }

    private Set<String> getPermissionCodesByUserId(Long userId) {
        List<JobUserRole> userRoles = jobUserRoleMapper.selectList(
                new LambdaQueryWrapper<JobUserRole>().eq(JobUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = userRoles.stream().map(JobUserRole::getRoleId).distinct().toList();
        List<JobRolePermission> rolePerms = jobRolePermissionMapper.selectList(
                new LambdaQueryWrapper<JobRolePermission>().in(JobRolePermission::getRoleId, roleIds)
        );
        if (rolePerms.isEmpty()) {
            return Set.of();
        }
        List<Long> permIds = rolePerms.stream().map(JobRolePermission::getPermissionId).distinct().toList();
        List<JobPermission> perms = jobPermissionMapper.selectBatchIds(permIds);
        return perms.stream()
                .map(JobPermission::getPermissionCode)
                .filter(code -> code != null)
                .collect(Collectors.toSet());
    }
}
