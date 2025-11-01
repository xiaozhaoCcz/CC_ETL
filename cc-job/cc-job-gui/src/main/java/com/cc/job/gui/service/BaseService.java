package com.cc.job.gui.service;

import com.cc.job.gui.util.ApiUtil;

/**
 * @author xiaozhao
 */
public abstract class BaseService {

    public ApiUtil apiUtil;

    public BaseService() {
        this.apiUtil = ApiUtil.getInstance();
    }

}
