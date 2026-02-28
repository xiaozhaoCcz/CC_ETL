package com.xxl.job.core.handler.impl;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.glue.CSharpGlueFactory;
import com.xxl.job.core.handler.IJobHandler;

/**
 * C# GLUE 任务处理器
 * 类似 GlueJobHandler，但用于执行 C# 代码
 * 
 * @author cc-job-team
 */
public class CSharpGlueJobHandler extends IJobHandler {

    private int jobId;
    private long glueUpdatetime;
    private String gluesource;
    private String contextData;  // 执行上下文数据（JSON格式）

    public CSharpGlueJobHandler(int jobId, long glueUpdatetime, String gluesource) {
        this(jobId, glueUpdatetime, gluesource, null);
    }
    
    public CSharpGlueJobHandler(int jobId, long glueUpdatetime, String gluesource, String contextData) {
        this.jobId = jobId;
        this.glueUpdatetime = glueUpdatetime;
        this.gluesource = gluesource;
        this.contextData = contextData;
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public void execute() throws Exception {
        XxlJobHelper.log("----------- C# glue.version:" + glueUpdatetime + " -----------");
        CSharpGlueFactory.executeCSharpGlue(gluesource, jobId, glueUpdatetime, contextData);
    }

    @Override
    public void init() throws Exception {
        // C# GLUE 不需要初始化
    }

    @Override
    public void destroy() throws Exception {
        // C# GLUE 不需要销毁
    }
}

