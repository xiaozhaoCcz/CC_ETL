package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobCompose;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobEdgeForm;
import com.cc.job.xo.model.form.JobInfoForm;

import java.util.List;
import java.util.Map;

public interface JobComposeService extends IService<JobCompose> {

    boolean saveJobCompose(JobInfoForm formData);

    boolean updateJobCompose(Long id, JobInfoForm formData);

    Map<String,Object> getJobCompose(Map<String,Object> formMap);

    boolean validateJobComposeEdge(String nodes,String edges);

    JobNode saveJobNode(JobInfoForm formData);

    Long updateJobNode(Long jobId, Long nodeId);

    List<Long> pauseJobs(Long[] jobIds);

    void deleteJobNode(Long nodeId);

    Map<String, Object> saveJobNodeAndJobEdges(Map<String, Object> formMap);

    /**
     * 保存连线
     * @param formData 连线表单数据
     * @return 保存后的连线实体
     */
    JobEdge saveJobEdge(JobEdgeForm formData);

    /**
     * 创建条件节点
     * @param parentTaskGroupId 父任务组ID
     * @param conditionName 条件节点名称
     * @param conditionExpression 条件表达式
     * @param expressionType 表达式类型（SIMPLE/SCRIPT）
     * @param conditionType 条件类型（IF/WHILE/FOREACH）
     * @param x 节点X坐标
     * @param y 节点Y坐标
     * @return 创建的JobNode信息（包含jobId和nodeId）
     */
    Map<String, Object> createConditionNode(Long parentTaskGroupId, String conditionName, 
                                           String conditionExpression, String expressionType, 
                                           String conditionType, double x, double y);
}
