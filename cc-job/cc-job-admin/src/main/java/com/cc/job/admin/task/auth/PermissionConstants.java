package com.cc.job.admin.task.auth;

/**
 * 权限码与资源类型常量（与 job_permission 表一致）
 */
public final class PermissionConstants {

    private PermissionConstants() {
    }

    /** 资源类型 */
    public static final String RESOURCE_PART = "PART";
    public static final String RESOURCE_JOB_INFO = "JOB_INFO";
    public static final String RESOURCE_JOB_NODE = "JOB_NODE";

    /** 操作类型 */
    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_EDIT = "EDIT";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_EXECUTE = "EXECUTE";

    /** 权限码 */
    public static final String PART_VIEW = "part:view";
    public static final String PART_EDIT = "part:edit";
    public static final String PART_DELETE = "part:delete";
    public static final String JOB_INFO_VIEW = "job_info:view";
    public static final String JOB_INFO_EDIT = "job_info:edit";
    public static final String JOB_INFO_DELETE = "job_info:delete";
    public static final String JOB_INFO_EXECUTE = "job_info:execute";
    public static final String JOB_NODE_VIEW = "job_node:view";
    public static final String JOB_NODE_EDIT = "job_node:edit";
    public static final String JOB_NODE_DELETE = "job_node:delete";

    /** 权限管理（仅管理员可进入权限管理页面） */
    public static final String PERMISSION_MANAGE = "permission:manage";

    /** 超级管理员用户ID（不可删除、不可取消管理员角色） */
    public static final long SUPER_ADMIN_USER_ID = 1L;
    /** 超级管理员角色ID（不可删除） */
    public static final long SUPER_ADMIN_ROLE_ID = 1L;
}
