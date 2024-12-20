package com.cc.job.executor.handler;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class ApiHandler {

    final JobInfoMapper jobInfoMapper;

    @XxlJob("runApiHandler")
    public void  runApiHandler(){
        long jobId = XxlJobHelper.getJobId();
        JobInfo jobInfo = jobInfoMapper.selectById(jobId);
        if(jobInfo==null){
            throw new RuntimeException("jobInfo is null");
        }
        String reqHeader = jobInfo.getReqHeader();
        String reqUrl = jobInfo.getReqUrl();
        String reqType = jobInfo.getReqType();
        String reqBody = jobInfo.getReqBody();
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
}
