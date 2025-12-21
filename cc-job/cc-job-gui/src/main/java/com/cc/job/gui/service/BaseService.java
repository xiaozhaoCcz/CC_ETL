package com.cc.job.gui.service;

import com.cc.job.gui.util.ApiUtil;

/**
 * 基础服务类
 * 
 * <p>所有服务类的基类，提供通用的 API 调用能力
 * 
 * @author xiaozhao
 */
public abstract class BaseService {

    protected ApiUtil apiUtil;
    protected HttpClientUtil httpClient;

    public BaseService() {
        this.apiUtil = ApiUtil.getInstance();
        this.httpClient = new HttpClientUtil(apiUtil);
    }
}
