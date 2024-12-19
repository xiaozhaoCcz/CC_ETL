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
}
