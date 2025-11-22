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
        JobGroupSnapshot snapshot = new JobGroupSnapshot();
        snapshot.setJobId(jobId);
        snapshot.setRandomId(randomId);
        snapshot.setNodesJson(nodesJson);
        snapshot.setEdgesJson(edgesJson);
        snapshot.setTriggerUserId(triggerUserId);
        snapshot.setCreateTime(LocalDateTime.now());
        snapshot.setUpdateTime(LocalDateTime.now());

        this.save(snapshot);

        return snapshot.getId();
    }

    @Override
    public JobGroupSnapshot getSnapshot(Long jobId, String randomId) {
        LambdaQueryWrapper<JobGroupSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobGroupSnapshot::getJobId, jobId)
                .eq(JobGroupSnapshot::getRandomId, randomId)
                .eq(JobGroupSnapshot::getIsDeleted, 0)
                .orderByDesc(JobGroupSnapshot::getCreateTime)
                .last("LIMIT 1");

        JobGroupSnapshot snapshot = this.getOne(wrapper);

        if (snapshot == null) {
            log.warn("[Snapshot] 快照不存在 - jobId: {}, randomId: {}", jobId, randomId);
        }

        return snapshot;
    }

    @Override
    public boolean deleteSnapshot(Long jobId, String randomId) {
        LambdaQueryWrapper<JobGroupSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobGroupSnapshot::getJobId, jobId)
                .eq(JobGroupSnapshot::getRandomId, randomId)
                .eq(JobGroupSnapshot::getIsDeleted, 0);

        JobGroupSnapshot snapshot = this.getOne(wrapper);
        if (snapshot != null) {
            snapshot.setIsDeleted(1);
            boolean result = this.updateById(snapshot);
            return result;
        }

        log.warn("[Snapshot] 快照不存在，无法删除 - jobId: {}, randomId: {}", jobId, randomId);
        return false;
    }
}

