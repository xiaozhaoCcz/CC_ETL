package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobGroupSnapshot;

import java.util.List;

/**
 * 任务组快照服务接口
 *
 * @author ccjob
 * @since 2025-01-XX
 */
public interface JobGroupSnapshotService extends IService<JobGroupSnapshot> {

    /**
     * 创建任务组快照
     *
     * @param jobId        任务组ID
     * @param randomId     批次ID
     * @param nodesJson    节点JSON
     * @param edgesJson    边JSON
     * @param triggerUserId 触发用户ID
     * @return 快照ID
     */
    Long createSnapshot(Long jobId, String randomId, String nodesJson, String edgesJson, String triggerUserId);

    /**
     * 根据任务组ID和批次ID获取快照
     *
     * @param jobId    任务组ID
     * @param randomId 批次ID
     * @return 快照对象
     */
    JobGroupSnapshot getSnapshot(Long jobId, String randomId);

    /**
     * 删除快照（软删除）
     *
     * @param jobId    任务组ID
     * @param randomId 批次ID
     * @return 是否成功
     */
    boolean deleteSnapshot(Long jobId, String randomId);

    /** 保存为版本（randomId = "ver_" + versionName），用于版本与回滚 */
    Long saveAsVersion(Long jobId, String versionName, String nodesJson, String edgesJson, String userId);

    /** 列出任务组的手动版本列表（randomId 以 "ver_" 开头） */
    List<JobGroupSnapshot> listVersions(Long jobId, int limit);
}

