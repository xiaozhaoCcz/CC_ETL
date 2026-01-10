package com.cc.job.xo.model.result;

import java.io.Serializable;
import java.util.Objects;

/**
 * 统一节点结果结构
 * 
 * <p>所有节点执行结果的统一数据结构，包含通用元数据和类型特定的结果对象
 *
 * @author cc-job-team
 */
public class NodeResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 状态码：200=成功，其他=失败 */
    private Integer code;
    
    /** 消息 */
    private String message;
    
    /** 是否成功 */
    private Boolean success;
    
    /** 执行耗时（毫秒） */
    private Long duration;
    
    /** 执行时间戳 */
    private Long timestamp;
    
    /** 通用数据字段 */
    private Object data;
    
    /** SQL节点专用结果 */
    private SqlResult sqlResult;
    
    /** API节点专用结果 */
    private ApiResult apiResult;
    
    /** Bean节点专用结果 */
    private BeanResult beanResult;
    
    /** Glue节点专用结果 */
    private GlueResult glueResult;

    public NodeResult() {
    }

    public NodeResult(Integer code, String message, Boolean success, Long duration, Long timestamp) {
        this.code = code;
        this.message = message;
        this.success = success;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    /**
     * 创建成功结果
     */
    public static NodeResult success(String message, Long duration) {
        NodeResult result = new NodeResult();
        result.setCode(200);
        result.setMessage(message);
        result.setSuccess(true);
        result.setDuration(duration);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 创建失败结果
     */
    public static NodeResult failure(String message, Long duration) {
        NodeResult result = new NodeResult();
        result.setCode(500);
        result.setMessage(message);
        result.setSuccess(false);
        result.setDuration(duration);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public SqlResult getSqlResult() {
        return sqlResult;
    }

    public void setSqlResult(SqlResult sqlResult) {
        this.sqlResult = sqlResult;
    }

    public ApiResult getApiResult() {
        return apiResult;
    }

    public void setApiResult(ApiResult apiResult) {
        this.apiResult = apiResult;
    }

    public BeanResult getBeanResult() {
        return beanResult;
    }

    public void setBeanResult(BeanResult beanResult) {
        this.beanResult = beanResult;
    }

    public GlueResult getGlueResult() {
        return glueResult;
    }

    public void setGlueResult(GlueResult glueResult) {
        this.glueResult = glueResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeResult that = (NodeResult) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(message, that.message) &&
                Objects.equals(success, that.success) &&
                Objects.equals(duration, that.duration) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, success, duration, timestamp);
    }

    @Override
    public String toString() {
        return "NodeResult{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", duration=" + duration +
                ", timestamp=" + timestamp +
                ", sqlResult=" + sqlResult +
                ", apiResult=" + apiResult +
                ", beanResult=" + beanResult +
                ", glueResult=" + glueResult +
                '}';
    }
}
