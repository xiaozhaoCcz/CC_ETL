package com.example.nodefx.service;

import com.example.nodefx.util.ApiUtil;

/**
 * @author xiaozhao
 */
public abstract class BaseService {

    public ApiUtil apiUtil;

    public BaseService() {
        this.apiUtil = ApiUtil.getInstance();
    }

}
