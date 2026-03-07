package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.dto.LoginResult;
import com.cc.job.xo.model.entity.JobUser;

/**
 * 用户服务接口
 *
 * @author ccjob
 * @since 2024-11-01
 */
public interface JobUserService extends IService<JobUser> {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    LoginResult login(String username, String password);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    JobUser findByUsername(String username);

    /**
     * 验证密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return true-密码正确，false-密码错误
     */
    boolean checkPassword(String rawPassword, String encodedPassword);
    
    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @return 注册结果
     */
    LoginResult register(String username, String password);

    /**
     * 管理员创建用户（不自动登录）
     *
     * @param username 用户名
     * @param password 初始密码
     * @return 新用户
     */
    JobUser createUserByAdmin(String username, String password);

    /**
     * 管理员删除用户（禁止删除超级管理员；会解除该用户的角色与资源权限）
     *
     * @param userId 用户ID
     */
    void deleteUserById(Long userId);

    /**
     * 管理员重置用户密码
     *
     * @param userId   用户ID
     * @param newPassword 新密码
     */
    void resetPassword(Long userId, String newPassword);
}

