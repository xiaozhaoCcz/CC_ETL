package com.cc.job.xo.model.result;

import java.io.Serializable;
import java.util.Objects;

/**
 * Glue节点执行结果
 * 
 * <p>包含脚本执行的输出、错误、退出码和执行结果
 *
 * @author cc-job-team
 */
public class GlueResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 标准输出（stdout） */
    private String output;
    
    /** 错误输出（stderr） */
    private String error;
    
    /** 退出码（0=成功，非0=失败） */
    private Integer exitCode;
    
    /** 脚本执行返回的结果数据（通过回调返回的任意数据） */
    private Object result;
    
    /** 脚本类型：Java, Shell, Python, PHP, Nodejs, PowerShell, C# */
    private String scriptType;
    
    /** 脚本源码（可选，用于调试） */
    private String scriptSource;
    
    /** 脚本执行时间（毫秒） */
    private Long executionTime;
    
    /** 日志输出（如果脚本有日志） */
    private String logOutput;

    public GlueResult() {
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }

    public String getScriptSource() {
        return scriptSource;
    }

    public void setScriptSource(String scriptSource) {
        this.scriptSource = scriptSource;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public String getLogOutput() {
        return logOutput;
    }

    public void setLogOutput(String logOutput) {
        this.logOutput = logOutput;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlueResult that = (GlueResult) o;
        return Objects.equals(exitCode, that.exitCode) &&
                Objects.equals(scriptType, that.scriptType) &&
                Objects.equals(executionTime, that.executionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exitCode, scriptType, executionTime);
    }

    @Override
    public String toString() {
        return "GlueResult{" +
                "exitCode=" + exitCode +
                ", scriptType='" + scriptType + '\'' +
                ", executionTime=" + executionTime +
                ", hasOutput=" + (output != null && !output.isEmpty()) +
                ", hasError=" + (error != null && !error.isEmpty()) +
                ", hasResult=" + (result != null) +
                '}';
    }
}
