package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色-权限关联（联合主键 role_id, permission_id）
 */
@TableName("job_role_permission")
public class JobRolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private Long permissionId;
    private LocalDateTime createTime;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
