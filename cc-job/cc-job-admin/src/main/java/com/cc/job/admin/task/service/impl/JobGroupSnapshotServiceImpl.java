package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.service.JobGroupSnapshotService;
import com.cc.job.xo.mapper.JobGroupSnapshotMapper;
import com.cc.job.xo.model.entity.JobGroupSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务组快照服务实现类
 *
 * @author ccjob
 * @since 2025-01-XX
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobGroupSnapshotServiceImpl extends ServiceImpl<JobGroupSnapshotMapper, JobGroupSnapshot>
        implements JobGroupSnapshotService {

    @Override
    public Long createSnapshot(Long jobId, String randomId, String nodesJson, String edgesJson, String triggerUserId) {
        log.info("[Snapshot] 创建任务组快照 - jobId: {}, randomId: {}", jobId, randomId);

        JobGroupSnapshot snapshot = new JobGroupSnapshot();
        snapshot.setJobId(jobId);
        snapshot.setRandomId(randomId);
        snapshot.setNodesJson(nodesJson);
        snapshot.setEdgesJson(edgesJson);
        snapshot.setTriggerUserId(triggerUserId);
        snapshot.setCreateTime(LocalDateTime.now());
        snapshot.setUpdateTime(LocalDateTime.now());

        this.save(snapshot);

        log.info("[Snapshot] 快照创建成功 - snapshotId: {}, jobId: {}, randomId: {}", snapshot.getId(), jobId, randomId);
        return snapshot.getId();
    }

    @Override
    public JobGroupSnapshot getSnapshot(Long jobId, String randomId) {
        log.debug("[Snapshot] 查询快照 - jobId: {}, randomId: {}", jobId, randomId);

        LambdaQueryWrapper<JobGroupSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobGroupSnapshot::getJobId, jobId)
                .eq(JobGroupSnapshot::getRandomId, randomId)
                .eq(JobGroupSnapshot::getIsDeleted, 0)
                .orderByDesc(JobGroupSnapshot::getCreateTime)
                .last("LIMIT 1");

        JobGroupSnapshot snapshot = this.getOne(wrapper);

        if (snapshot != null) {
            log.debug("[Snapshot] 快照查询成功 - snapshotId: {}, jobId: {}, randomId: {}", snapshot.getId(), jobId, randomId);
        } else {
            log.warn("[Snapshot] 快照不存在 - jobId: {}, randomId: {}", jobId, randomId);
        }

        return snapshot;
    }

    @Override
    public boolean deleteSnapshot(Long jobId, String randomId) {
        log.info("[Snapshot] 删除快照 - jobId: {}, randomId: {}", jobId, randomId);

        LambdaQueryWrapper<JobGroupSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobGroupSnapshot::getJobId, jobId)
                .eq(JobGroupSnapshot::getRandomId, randomId)
                .eq(JobGroupSnapshot::getIsDeleted, 0);

        JobGroupSnapshot snapshot = this.getOne(wrapper);
        if (snapshot != null) {
            snapshot.setIsDeleted(1);
            boolean result = this.updateById(snapshot);
            log.info("[Snapshot] 快照删除{} - snapshotId: {}, jobId: {}, randomId: {}", 
                    result ? "成功" : "失败", snapshot.getId(), jobId, randomId);
            return result;
        }

        log.warn("[Snapshot] 快照不存在，无法删除 - jobId: {}, randomId: {}", jobId, randomId);
        return false;
    }
}

