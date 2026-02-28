package com.cc.job.admin.task.service;

import java.util.List;

/**
 * 权限服务：全局 RBAC + 资源级权限
 */
public interface PermissionService {

    /**
     * 判断用户是否拥有某权限码（仅查角色-权限，全局）
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 获取用户对某类资源在某操作下允许访问的 resource_id 列表。
     * 若用户拥有对应全局权限（如 part:view），返回 null 表示“全部允许”，调用方不做 resource_id 过滤；
     * 否则查 job_resource_permission 返回该用户被授权的 resource_id 列表，空列表表示无任何资源权限。
     *
     * @param userId         用户ID
     * @param resourceType   PART / JOB_INFO / JOB_NODE
     * @param permissionType VIEW / EDIT / DELETE / EXECUTE
     * @return null 表示全部允许；非 null 表示仅允许的 resource_id 列表（可能为空）
     */
    List<Long> getAllowedResourceIds(Long userId, String resourceType, String permissionType);

    /**
     * 校验用户是否可访问指定资源：有全局权限返回 true；否则检查 resourceId 是否在 getAllowedResourceIds 中
     */
    boolean canAccessResource(Long userId, String resourceType, String permissionType, Long resourceId);

    /**
     * 获取用户拥有的全部权限码（用于前端按钮级权限）
     */
    List<String> getPermissionCodes(Long userId);
}
