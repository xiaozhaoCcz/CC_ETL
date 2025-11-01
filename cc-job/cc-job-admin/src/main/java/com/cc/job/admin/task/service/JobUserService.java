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
}

