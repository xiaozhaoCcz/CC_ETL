package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

/**
 * 资源级权限：仅能看/操作某几个分区/任务
 */
@TableName("job_resource_permission")
public class JobResourcePermission extends BaseEntity {

    /** 资源类型：PART / JOB_INFO / JOB_NODE */
    private String resourceType;
    /** 资源ID */
    private Long resourceId;
    /** 被授权用户ID */
    private Long userId;
    /** 被授权角色ID（可选） */
    private Long roleId;
    /** 权限类型：VIEW / EDIT / DELETE / EXECUTE */
    private String permissionType;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }
}
