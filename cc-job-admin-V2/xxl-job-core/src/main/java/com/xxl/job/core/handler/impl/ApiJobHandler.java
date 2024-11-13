package com.xxl.job.core.handler.impl;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhaowenpeng
 */
public class ApiJobHandler extends IJobHandler {
    private static Logger logger = LoggerFactory.getLogger(ApiJobHandler.class);
    private final String reqUrl;

    private final String reqType;

    private final String reqHeader;

    private final String reqBody;

    public ApiJobHandler(String reqUrl, String reqType, String reqHeader, String reqBody) {
        this.reqUrl = reqUrl;
        this.reqType = reqType;
        this.reqHeader = reqHeader;
        this.reqBody = reqBody;
    }

    @Override
    public void execute() throws Exception {
//        System.out.println("ApiJobHandler 任务运行"+reqUrl);
//        // TODO 需要修改参数传递
//        String s = HttpUtil.get("http://175.178.249.190/yanhuo/platform/category/getCategoryTreeData");
//        System.out.println("返回结果"+s);
//        XxlJobHelper.log(s);
        JSONArray jsonArray = JSONUtil.parseArray(reqHeader);
        List<Map> headerList = jsonArray.toList(Map.class);
        Map<String,String> headers = new HashMap<>();
        if(!headerList.isEmpty()){
            for (Map data : headerList) {
                String key = (String) data.get("columnKey");
                String value = (String) data.get("columnValue");
                headers.put(key, value);
            }
        }
        String result = null;
        if("GET".equalsIgnoreCase(reqType)){
            result =  HttpRequest.get(reqUrl)
                    .addHeaders(headers)
                    .execute()
                    .body();
        }else if("POST".equalsIgnoreCase( reqType)){
           result= HttpRequest.post(reqUrl)
                    .addHeaders(headers)
                    .form(reqBody)
                    .execute()
                    .body();
        }
        XxlJobHelper.log(result);
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
