package com.cc.job.xo.model.dto;

import java.util.Objects;

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

    public LoginResult() {
    }

    public LoginResult(String accessToken, String tokenType, String refreshToken, Long expires, Long userId, String username) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.refreshToken = refreshToken;
        this.expires = expires;
        this.userId = userId;
        this.username = username;
    }

    public static LoginResultBuilder builder() {
        return new LoginResultBuilder();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(Long expires) {
        this.expires = expires;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginResult that = (LoginResult) o;
        return Objects.equals(accessToken, that.accessToken) &&
                Objects.equals(tokenType, that.tokenType) &&
                Objects.equals(refreshToken, that.refreshToken) &&
                Objects.equals(expires, that.expires) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, tokenType, refreshToken, expires, userId, username);
    }

    @Override
    public String toString() {
        return "LoginResult{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", expires=" + expires +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                '}';
    }

    public static class LoginResultBuilder {
        private String accessToken;
        private String tokenType;
        private String refreshToken;
        private Long expires;
        private Long userId;
        private String username;

        LoginResultBuilder() {
        }

        public LoginResultBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public LoginResultBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public LoginResultBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public LoginResultBuilder expires(Long expires) {
            this.expires = expires;
            return this;
        }

        public LoginResultBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public LoginResultBuilder username(String username) {
            this.username = username;
            return this;
        }

        public LoginResult build() {
            return new LoginResult(accessToken, tokenType, refreshToken, expires, userId, username);
        }
    }
}
