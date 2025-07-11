package com.cc.job.admin.task.controller;

import cn.hutool.core.lang.Pair;
import com.cc.job.admin.task.handler.JobGroupXxlJob;
import com.cc.job.admin.task.thread.JobLogHelper;
import com.cc.job.admin.task.thread.JobLogThreadListener;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by xuxueli on 17/5/10.
 */
@Controller
@RequestMapping("/xxl-job-admin/api")
public class JobApiController {

    @Resource
    private  AdminBiz adminBiz;

    /**
     * api
     *
     * @param uri
     * @param data
     * @return
     */
    @RequestMapping("/{uri}")
    @ResponseBody
    public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {

        // valid
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (uri==null || uri.trim().length()==0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
//        if (XxlJobAdminConfig.getAdminConfig().getAccessToken()!=null
//                && XxlJobAdminConfig.getAdminConfig().getAccessToken().trim().length()>0
//                && !XxlJobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN))) {
//            return new ReturnT<>(ReturnT.FAIL_CODE, "The access token is wrong.");
//        }

        // services mapping
        if ("callback".equals(uri)) {
            List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class, HandleCallbackParam.class);
            return adminBiz.callback(callbackParamList);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else if("addJobGroupData".equals(uri)){
            Pair<String,Boolean> pair = GsonTool.fromJson(data, Pair.class);
            JobGroupXxlJob.addJobData(pair.getKey(), pair.getValue());
            return new ReturnT<>(ReturnT.SUCCESS_CODE, "success");
        }else if("jobLogId".equals(uri)){
            Pair<String,String> pair = GsonTool.fromJson(data, Pair.class);
            JobLogHelper.addJobLog(pair);
            return new ReturnT<>(ReturnT.SUCCESS_CODE, "success");
        }
        else {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping("+ uri +") not found.");
        }

    }

}
