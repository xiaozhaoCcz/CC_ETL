package com.cc.job.gui.model;

/**
 * 运行中的任务组信息
 * 用于跟踪多个同时运行的任务组
 */
public class RunningJobGroup {
    private Long jobId;
    private String jobName;
    private String randomId;
    private Long logId;
    private java.util.Timer logTimer;
    private int fromLineNum;
    private int pullFailCount;
    private boolean isRunning;
    private String triggerUserId;  // 触发任务的用户ID
    
    public RunningJobGroup(Long jobId, String jobName, String randomId) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.randomId = randomId;
        this.fromLineNum = 0;
        this.pullFailCount = 0;
        this.isRunning = true;
    }
    
    public RunningJobGroup(Long jobId, String jobName, String randomId, String triggerUserId) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.randomId = randomId;
        this.triggerUserId = triggerUserId;
        this.fromLineNum = 0;
        this.pullFailCount = 0;
        this.isRunning = true;
    }
    
    public Long getJobId() {
        return jobId;
    }
    
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
    
    public String getJobName() {
        return jobName;
    }
    
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    
    public String getRandomId() {
        return randomId;
    }
    
    public void setRandomId(String randomId) {
        this.randomId = randomId;
    }
    
    public Long getLogId() {
        return logId;
    }
    
    public void setLogId(Long logId) {
        this.logId = logId;
    }
    
    public java.util.Timer getLogTimer() {
        return logTimer;
    }
    
    public void setLogTimer(java.util.Timer logTimer) {
        this.logTimer = logTimer;
    }
    
    public int getFromLineNum() {
        return fromLineNum;
    }
    
    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }
    
    public int getPullFailCount() {
        return pullFailCount;
    }
    
    public void setPullFailCount(int pullFailCount) {
        this.pullFailCount = pullFailCount;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setRunning(boolean running) {
        isRunning = running;
    }
    
    public String getTriggerUserId() {
        return triggerUserId;
    }
    
    public void setTriggerUserId(String triggerUserId) {
        this.triggerUserId = triggerUserId;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (logTimer != null) {
            logTimer.cancel();
            logTimer = null;
        }
        isRunning = false;
    }
}

