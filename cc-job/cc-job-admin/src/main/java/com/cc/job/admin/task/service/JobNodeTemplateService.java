package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobNodeTemplate;

import java.util.List;

/**
 * 节点模板服务接口
 */
public interface JobNodeTemplateService extends IService<JobNodeTemplate> {

    /**
     * 按分类和用户筛选模板列表（当前用户可见：自己创建的 + 公开的）
     *
     * @param category    分类（可为 null 表示全部）
     * @param createUserId 当前用户ID（可为 null，则只返回公开模板）
     * @return 模板列表
     */
    List<JobNodeTemplate> listByCategoryAndUser(String category, Long createUserId);
}
