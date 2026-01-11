package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;

import java.io.Serial;

/**
 * 画布书签实体类
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
@TableName("job_canvas_bookmark")
public class JobCanvasBookmark extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务组ID
     */
    private Long taskGroupId;

    /**
     * 书签名称
     */
    private String bookmarkName;

    /**
     * 书签类型：position-位置书签，node-节点书签
     */
    private String bookmarkType;

    /**
     * 目标节点ID（节点书签专用）
     */
    private Long targetNodeId;

    /**
     * 画布X坐标（位置书签专用）
     */
    private Double canvasPositionX;

    /**
     * 画布Y坐标（位置书签专用）
     */
    private Double canvasPositionY;

    /**
     * 缩放级别
     */
    private Double zoomLevel;

    /**
     * 书签描述
     */
    private String description;

    /**
     * 创建用户ID
     */
    private Long createUserId;

    public Long getTaskGroupId() {
        return taskGroupId;
    }

    public void setTaskGroupId(Long taskGroupId) {
        this.taskGroupId = taskGroupId;
    }

    public String getBookmarkName() {
        return bookmarkName;
    }

    public void setBookmarkName(String bookmarkName) {
        this.bookmarkName = bookmarkName;
    }

    public String getBookmarkType() {
        return bookmarkType;
    }

    public void setBookmarkType(String bookmarkType) {
        this.bookmarkType = bookmarkType;
    }

    public Long getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Long targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public Double getCanvasPositionX() {
        return canvasPositionX;
    }

    public void setCanvasPositionX(Double canvasPositionX) {
        this.canvasPositionX = canvasPositionX;
    }

    public Double getCanvasPositionY() {
        return canvasPositionY;
    }

    public void setCanvasPositionY(Double canvasPositionY) {
        this.canvasPositionY = canvasPositionY;
    }

    public Double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(Double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }
}
