package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobCanvasBookmark;

import java.util.List;

/**
 * 画布书签服务接口
 */
public interface JobCanvasBookmarkService extends IService<JobCanvasBookmark> {

    /**
     * 按任务组ID获取书签列表
     *
     * @param taskGroupId 任务组ID
     * @return 书签列表
     */
    List<JobCanvasBookmark> listByTaskGroupId(Long taskGroupId);
}
