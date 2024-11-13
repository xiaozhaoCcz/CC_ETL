package com.xxl.job.core.handler.impl;


import cn.hutool.http.HttpUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhaowenpeng
 */
public class ApiJobHandler extends IJobHandler {
    private static Logger logger = LoggerFactory.getLogger(ApiJobHandler.class);
    private final String api;

    public ApiJobHandler(String api) {
        this.api = api;
    }
    @Override
    public void execute() throws Exception {
        System.out.println("ApiJobHandler 任务运行"+api);
        // TODO 需要修改参数传递
        String s = HttpUtil.get("http://175.178.249.190/yanhuo/platform/category/getCategoryTreeData");
        System.out.println("返回结果"+s);
        XxlJobHelper.log(s);
    }

    @Override
    public void init() throws Exception {
       logger.info("Starting apiJobHandler......");
    }

    @Override
    public void destroy() throws Exception {
       logger.info("destroy apiJobHandler......");
    }
}
