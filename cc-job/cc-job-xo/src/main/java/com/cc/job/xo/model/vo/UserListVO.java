package com.cc.job.xo.model.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户列表项（不含密码） */
public class UserListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
