package com.cc.job.admin.task.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.service.JobUserService;
import com.cc.job.admin.task.utils.JwtUtil;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.mapper.JobResourcePermissionMapper;
import com.cc.job.xo.mapper.JobUserMapper;
import com.cc.job.xo.mapper.JobUserRoleMapper;
import com.cc.job.xo.model.dto.LoginResult;
import com.cc.job.xo.model.entity.JobUser;
import com.cc.job.xo.model.entity.JobResourcePermission;
import com.cc.job.xo.model.entity.JobUserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 *
 * @author ccjob
 * @since 2024-11-01
 */
@Service
public class JobUserServiceImpl extends ServiceImpl<JobUserMapper, JobUser> implements JobUserService {

    private static final Logger log = LoggerFactory.getLogger(JobUserServiceImpl.class);

    private final JobUserRoleMapper jobUserRoleMapper;
    private final JobResourcePermissionMapper jobResourcePermissionMapper;

    public JobUserServiceImpl(JobUserRoleMapper jobUserRoleMapper, JobResourcePermissionMapper jobResourcePermissionMapper) {
        this.jobUserRoleMapper = jobUserRoleMapper;
        this.jobResourcePermissionMapper = jobResourcePermissionMapper;
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @Override
    public LoginResult login(String username, String password) {
        // 参数验证
        Assert.notBlank(username, "用户名不能为空");
        Assert.notBlank(password, "密码不能为空");

        // 查询用户
        JobUser user = findByUsername(username);
        if (user == null) {
            log.warn("登录失败 - 用户不存在: {}", username);
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!checkPassword(password, user.getPassword())) {
            log.warn("登录失败 - 密码错误: {}", username);
            throw new BusinessException("用户名或密码错误");
        }

        // 生成JWT Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 计算过期时间（24小时）
        long expiresIn = 24 * 60 * 60 * 1000; // 24小时（毫秒）

        // 返回登录结果
        return LoginResult.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expires(expiresIn)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    public JobUser findByUsername(String username) {
        LambdaQueryWrapper<JobUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobUser::getUsername, username);
        return this.getOne(wrapper);
    }

    /**
     * 验证密码
     * 支持两种方式：
     * 1. BCrypt加密验证（推荐）
     * 2. 明文密码比对（开发/测试环境）
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 数据库中的密码
     * @return true-密码正确，false-密码错误
     */
    @Override
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }

        // 检查是否为BCrypt加密的密码（BCrypt密码以$2a$、$2b$或$2y$开头）
        if (encodedPassword.startsWith("$2a$") || 
            encodedPassword.startsWith("$2b$") || 
            encodedPassword.startsWith("$2y$")) {
            // BCrypt验证
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } else {
            // 明文密码比对（仅用于开发/测试环境）
            log.warn("检测到明文密码，建议使用BCrypt加密！用户密码: {}", encodedPassword.substring(0, Math.min(3, encodedPassword.length())) + "***");
            return rawPassword.equals(encodedPassword);
        }
    }

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @return 注册结果
     */
    @Override
    public LoginResult register(String username, String password) {
        // 参数验证
        Assert.notBlank(username, "用户名不能为空");
        Assert.notBlank(password, "密码不能为空");
        
        // 用户名长度验证
        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException("用户名长度必须在3-20个字符之间");
        }
        
        // 密码强度验证
        if (password.length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }

        // 检查用户名是否已存在
        JobUser existingUser = findByUsername(username);
        if (existingUser != null) {
            log.warn("注册失败 - 用户名已存在: {}", username);
            throw new BusinessException("用户名已存在");
        }

        // 创建新用户
        JobUser newUser = new JobUser();
        newUser.setUsername(username);
        newUser.setPassword(encodePassword(password));
        
        // 保存到数据库
        boolean saved = this.save(newUser);
        if (!saved) {
            log.error("注册失败 - 数据库保存失败: {}", username);
            throw new BusinessException("注册失败，请稍后重试");
        }

        // 自动登录，生成JWT Token
        String token = JwtUtil.generateToken(newUser.getId(), newUser.getUsername());
        long expiresIn = 24 * 60 * 60 * 1000; // 24小时

        return LoginResult.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expires(expiresIn)
                .userId(newUser.getId())
                .username(newUser.getUsername())
                .build();
    }

    /**
     * 密码加密（BCrypt）
     * 辅助方法：用于注册或修改密码时加密
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encodePassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    @Override
    public JobUser createUserByAdmin(String username, String password) {
        Assert.notBlank(username, "用户名不能为空");
        Assert.notBlank(password, "密码不能为空");
        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException("用户名长度必须在3-20个字符之间");
        }
        if (password.length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        JobUser existing = findByUsername(username);
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        JobUser user = new JobUser();
        user.setUsername(username);
        user.setPassword(encodePassword(password));
        save(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (PermissionConstants.SUPER_ADMIN_USER_ID == userId) {
            throw new BusinessException("不能删除超级管理员用户");
        }
        jobUserRoleMapper.delete(new LambdaQueryWrapper<JobUserRole>().eq(JobUserRole::getUserId, userId));
        jobResourcePermissionMapper.delete(new LambdaQueryWrapper<JobResourcePermission>().eq(JobResourcePermission::getUserId, userId));
        removeById(userId);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        Assert.notBlank(newPassword, "新密码不能为空");
        if (newPassword.length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        JobUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(encodePassword(newPassword));
        updateById(user);
    }
}

