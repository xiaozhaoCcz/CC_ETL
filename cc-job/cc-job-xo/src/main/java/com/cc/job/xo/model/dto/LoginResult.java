package com.cc.job.xo.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResult {
    private String accessToken;

    private String tokenType;

    private String refreshToken;

    private Long expires;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
}
