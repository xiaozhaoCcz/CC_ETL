package com.cc.job.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.core.cron.CronExpression;
import com.cc.job.task.enums.*;
import com.cc.job.task.handler.JobGroupXxlJob;
import com.cc.job.xo.mapper.JobLogglueMapper;
import com.cc.job.xo.model.dto.JobEdgeDto;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.dto.JobNodeDto;
import com.cc.job.xo.model.entity.*;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.xo.model.vo.JobNodeVo;
import com.cc.job.task.service.JobEdgeService;
import com.cc.job.task.service.JobGroupService;
import com.cc.job.task.service.JobNodeService;
import com.cc.job.task.thread.JobScheduleHelper;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.cc.job.task.utils.I18nUtil;
import com.cc.tasktool.executor.Async;
import com.cc.tasktool.wrapper.WorkerWrapper;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.task.service.JobInfoService;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import org.springframework.transaction.annotation.Transactional;


/**
 * task_info服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobInfoServiceImpl extends ServiceImpl<JobInfoMapper, JobInfo> implements JobInfoService {

    private final JobGroupService taskGroupService;

    private final JobNodeService taskNodeService;

    private final JobEdgeService taskEdgeService;

    private final JobLogglueMapper taskLogglueMapper;

    private final JobInfoMapper taskInfoMapper;

    private final RedisTemplate redisTemplate;

    /**
     * 获取task_info分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage< JobInfoVO >} task_info分页列表
     */
    @Override
    public IPage<JobInfoVO> getTaskInfoPage(JobInfoQuery queryParams) {
//        Page<TaskInfoVO> pageVO = this.baseMapper.getTaskInfoPage(
//                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
//                queryParams
//        );
        Page<JobInfoVO> pageVO = new Page<>();
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        if (queryParams.getJobGroup() != null) {
            wrapper.eq(JobInfo::getJobGroup, queryParams.getJobGroup());
        }
        if (queryParams.getTriggerStatus() != null) {
            wrapper.eq(JobInfo::getTriggerStatus, queryParams.getTriggerStatus());
        }
        if (StringUtils.isNotBlank(queryParams.getAuthor())) {
            wrapper.like(JobInfo::getAuthor, queryParams.getAuthor());
        }
        if (StringUtils.isNotBlank(queryParams.getJobDesc())) {
            wrapper.like(JobInfo::getJobDesc, queryParams.getJobDesc());
        }
        if (StringUtils.isNotBlank(queryParams.getExecutorHandler())) {
            wrapper.eq(JobInfo::getExecutorHandler, queryParams.getExecutorHandler());
        }

        wrapper.in(JobInfo::getJobType, 0, 2);
        wrapper.in(JobInfo::getIsNode, "N");
        wrapper.orderByDesc(JobInfo::getUpdateTime);

        Page<JobInfo> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<JobInfo> records = page.getRecords();
        List<JobInfoVO> voList = records.stream().map(v -> BeanUtil.copyProperties(v, JobInfoVO.class)).toList();
        pageVO.setRecords(voList);
        pageVO.setTotal(page.getTotal());
        return pageVO;
    }

    /**
     * 获取task_info表单数据
     *
     * @param id task_infoID
     * @return
     */
    @Override
    public JobInfoForm getTaskInfoFormData(Long id) {
        JobInfo entity = this.getById(id);
        JobInfoForm taskInfoForm = BeanUtil.copyProperties(entity, JobInfoForm.class);
        if (entity.getJobType() == 2) {
            List<JobNode> taskNodeList = taskNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, id));
            List<JobEdge> taskEdgeList = taskEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getTaskParentId, id));
            List<JobNodeVo> taskNodeVoList = new ArrayList<>();
            List<Long> taskIds = taskNodeList.stream().map(JobNode::getTaskId).toList();
            List<JobInfo> taskInfos = this.listByIds(taskIds);
            Map<Long, String> taskMap = taskInfos.stream().collect(Collectors.toMap(JobInfo::getId, JobInfo::getJobDesc));
            for (JobNode node : taskNodeList) {
                JobNodeVo taskNodeVo = BeanUtil.copyProperties(node, JobNodeVo.class);
                taskNodeVo.setTaskName(taskMap.get(node.getTaskId()));
                taskNodeVoList.add(taskNodeVo);
            }
            taskInfoForm.setNodes(JSONUtil.toJsonStr(taskNodeVoList));
            taskInfoForm.setEdges(JSONUtil.toJsonStr(taskEdgeList));
        }
        return taskInfoForm;
    }

    /**
     * 新增task_info
     *
     * @param formData task_info表单对象
     * @return
     */
    @Override
    public boolean saveTaskInfo(JobInfoForm formData) {
        JobInfo taskInfo = baseSaveTaskInfo(formData);
        return this.save(taskInfo);
    }

    private boolean isNumeric(String str) {
        try {
            int result = Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 更新task_info
     *
     * @param id       task_infoID
     * @param formData task_info表单对象
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskInfo(Long id, JobInfoForm formData) {
        // valid trigger
        JobInfo existsJobInfo = baseUpdateTaskInfo(id, formData);
        updateChild(existsJobInfo);
        return true;
    }

    public void updateChild(JobInfo taskInfo) {
        if (taskInfo.getJobType() == 2 && "Y".equalsIgnoreCase(taskInfo.getIsNode())) {
            //下面的子节点全部更新
            List<JobInfo> taskInfos = this.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, taskInfo.getId()));
            for (JobInfo info : taskInfos) {
                info.setMisfireStrategy(taskInfo.getMisfireStrategy());
                info.setExecutorTimeout(taskInfo.getExecutorTimeout());
                info.setExecutorBlockStrategy(taskInfo.getExecutorBlockStrategy());
                info.setExecutorFailRetryCount(taskInfo.getExecutorFailRetryCount());
                updateChild(info);
            }
        }
        this.updateById(taskInfo);
    }

    /**
     * 删除task_info
     *
     * @param ids task_infoID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTaskInfos(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_info数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        for (Long jobId : idList) {
            delNodes(jobId);
        }
        return true;
    }

    private void delNodes(Long jobId) {
        JobInfo taskInfo = this.getById(jobId);
        if (taskInfo.getJobType() == 2) {
            List<JobInfo> taskInfos = this.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
            if (taskInfos.isEmpty()) {
                return;
            }
            List<Long> childTaskIds = taskInfos.stream().map(JobInfo::getId).toList();

            taskNodeService.remove(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, taskInfo.getId()));
            taskEdgeService.remove(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getTaskParentId, taskInfo.getId()));

            for (Long childTaskId : childTaskIds) {
                delNodes(childTaskId);
            }
        }

        this.removeById(jobId);
    }

    @Override
    public boolean triggerJob(JobInfoTriggerDto taskInfoTriggerDto) {

        JobInfo taskInfo = this.getById(taskInfoTriggerDto.getId());
        if (taskInfo == null) {
            return false;
        }

        if (taskInfo.getJobType() == 2 && taskInfo.getRankTriggerStatus() == 1) {
            throw new BusinessException("当前任务正在运行中～");
        }

        // force cover job param
        if (taskInfoTriggerDto.getExecutorParam() == null) {
            taskInfoTriggerDto.setExecutorParam("");
        }

        if(GlueTypeEnum.DATAX.getDesc().equalsIgnoreCase(taskInfo.getGlueType())){
            taskInfoTriggerDto.setExecutorParam(taskInfo.getExecutorParam());
        }

        JobTriggerPoolHelper.trigger(taskInfoTriggerDto.getId().intValue(), TriggerTypeEnum.MANUAL, -1, null, taskInfoTriggerDto.getExecutorParam(), taskInfoTriggerDto.getAddressList());

        taskInfo.setRankTriggerStatus(1);
        this.updateById(taskInfo);
        return true;
    }

    @Override
    public boolean startTask(Long id) {
        JobInfo xxlJobInfo = this.getById(id);

        // valid
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(xxlJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            throw new BusinessException(I18nUtil.getString("schedule_type_none_limit_start"));
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = 0;
        try {
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(xxlJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
        }

        xxlJobInfo.setTriggerStatus(1);
        xxlJobInfo.setTriggerLastTime(0L);
        xxlJobInfo.setTriggerNextTime(nextTriggerTime);
        return this.updateById(xxlJobInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopTask(Long id) {
        JobInfo xxlJobInfo = this.getById(id);
        xxlJobInfo.setTriggerStatus(0);
        xxlJobInfo.setTriggerLastTime(0L);
        xxlJobInfo.setTriggerNextTime(0L);
        return this.updateById(xxlJobInfo);
    }

    @Override
    public List<String> nextTriggerTime(String scheduleType, String scheduleConf) {
        JobInfo paramXxlJobInfo = new JobInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid") + e.getMessage());
        }
        return result;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTaskSet(JobInfoForm formData) {
        JobInfo taskInfo = baseSaveTaskInfo(formData);
        if (StringUtils.isBlank(formData.getNodes())) {
            throw new BusinessException("任务节点不能为空");
        }

        taskInfo.setJobType(2);
        this.save(taskInfo);
        taskInfo.setExecutorParam(String.valueOf(taskInfo.getId()));
        this.updateById(taskInfo);

        List<Map> nodeList = JSONUtil.parseArray(formData.getNodes()).toList(Map.class);
        List<Map> edgeList = JSONUtil.parseArray(formData.getEdges()).toList(Map.class);

        List<JobNodeDto> taskNodeDtoList = new ArrayList<>();
        List<JobEdgeDto> taskEdgeDtoList = new ArrayList<>();

        nodeList.forEach(item -> {
            JobNodeDto node = new JobNodeDto();
            String nodeId = (String) item.get("id");
            node.setId(nodeId);
            node.setTaskParentId(taskInfo.getId());
            Map<String, Object> data = (Map<String, Object>) item.get("data");
            Map<String, Object> position = (Map<String, Object>) item.get("position");
            node.setNodePositionX(Double.valueOf(String.valueOf(position.get("x"))));
            node.setNodePositionY(Double.valueOf(String.valueOf(position.get("y"))));
            long taskId = Long.parseLong(String.valueOf(data.get("taskId")));
            node.setTaskId(taskId);
            taskNodeDtoList.add(node);
        });

        edgeList.forEach(item -> {
            JobEdgeDto edge = new JobEdgeDto();
            edge.setTaskParentId(taskInfo.getId());
            edge.setFromNodeId((String) item.get("source"));
            edge.setEndNodeId((String) item.get("target"));
            taskEdgeDtoList.add(edge);
        });

        //计算节点的出度和人度

        for (JobNodeDto taskNodeDto : taskNodeDtoList) {
            taskNodeDto.setNodeOutDegree(taskEdgeDtoList.stream().filter(v -> v.getFromNodeId().equalsIgnoreCase(taskNodeDto.getId())).count());
            taskNodeDto.setNodeInDegree(taskEdgeDtoList.stream().filter(v -> v.getEndNodeId().equalsIgnoreCase(taskNodeDto.getId())).count());
        }

        addNode(taskNodeDtoList, taskEdgeDtoList, taskInfo);
        return true;
    }

    private void addNode(List<JobNodeDto> nodeList, List<JobEdgeDto> edgeList, JobInfo parentTask) {
        Map<String, Long> nodeMap = new HashMap<>();

        for (JobNodeDto taskNode : nodeList) {
            JobInfo taskInfo = this.getById(taskNode.getTaskId());

            JobInfo copyTaskInfo = BeanUtil.copyProperties(taskInfo, JobInfo.class);
            copyTaskInfo.setId(null);
            copyTaskInfo.setIsNode("Y");
            copyTaskInfo.setParentId(parentTask.getId());
            this.save(copyTaskInfo);

            if (taskInfo.getJobType() == 2) {
                List<JobNode> childNodes = taskNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, taskInfo.getId()));
                List<JobEdge> childEdges = taskEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getTaskParentId, taskInfo.getId()));

                List<JobNodeDto> taskNodeDtos = BeanUtil.copyToList(childNodes, JobNodeDto.class, CopyOptions.create());
                List<JobEdgeDto> taskEdgeDtos = BeanUtil.copyToList(childEdges, JobEdgeDto.class, CopyOptions.create());
                addNode(taskNodeDtos, taskEdgeDtos, copyTaskInfo);
                copyTaskInfo.setExecutorParam(String.valueOf(copyTaskInfo.getId()));
                this.updateById(copyTaskInfo);
            }

            JobNode copyTaskNode = BeanUtil.copyProperties(taskNode, JobNode.class, "id");
            copyTaskNode.setId(null);
            copyTaskNode.setTaskId(copyTaskInfo.getId());
            copyTaskNode.setTaskParentId(parentTask.getId());
            taskNodeService.save(copyTaskNode);
            nodeMap.put(String.valueOf(taskNode.getId()), copyTaskNode.getId());
        }

        for (JobEdgeDto taskEdge : edgeList) {
            JobEdge edge = new JobEdge();
            edge.setTaskParentId(parentTask.getId());
            edge.setFromNodeId(nodeMap.get(String.valueOf(taskEdge.getFromNodeId())));
            edge.setEndNodeId(nodeMap.get(String.valueOf(taskEdge.getEndNodeId())));
            taskEdgeService.save(edge);
        }
    }


    private JobInfo baseSaveTaskInfo(JobInfoForm formData) {
        JobGroup taskGroup = taskGroupService.getById(formData.getJobGroup());
        if (taskGroup == null) {
            throw new BusinessException(I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup"));
        }

        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(formData.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (formData.getScheduleConf() == null || !CronExpression.isValidExpression(formData.getScheduleConf())) {
                throw new BusinessException("Cron" + I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE) {
            if (formData.getScheduleConf() == null) {
                throw new BusinessException(I18nUtil.getString("schedule_type"));
            }
            try {
                int fixSecond = Integer.parseInt(formData.getScheduleConf());
                if (fixSecond < 1) {
                    throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
                }
            } catch (Exception e) {
                throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
            }
        }

        // valid job
        if (GlueTypeEnum.match(formData.getGlueType()) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid"));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(formData.getGlueType()) && (formData.getExecutorHandler() == null || formData.getExecutorHandler().trim().length() == 0)) {
            throw new BusinessException(I18nUtil.getString("system_please_input") + "JobHandler");
        }
        // 》fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(formData.getGlueType()) && formData.getGlueSource() != null) {
            formData.setGlueSource(formData.getGlueSource().replaceAll("\r", ""));
        }

        if (GlueTypeEnum.API == GlueTypeEnum.match(formData.getGlueType()) && (StringUtils.isBlank(formData.getReqUrl()) || StringUtils.isBlank(formData.getReqType()))) {
            throw new BusinessException("请求地址和请求类型不能为空");
        }

        if (ExecutorRouteStrategyEnum.match(formData.getExecutorRouteStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid"));

        }
        if (MisfireStrategyEnum.match(formData.getMisfireStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid"));

        }
        if (ExecutorBlockStrategyEnum.match(formData.getExecutorBlockStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid"));
        }

        if (formData.getChildJobid() != null && formData.getChildJobid().trim().length() > 0) {
            String[] childJobIds = formData.getChildJobid().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    JobInfo childJobInfo = this.getById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        throw new BusinessException(MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));

                    }
                } else {
                    throw new BusinessException(
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item : childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length() - 1);

            formData.setChildJobid(temp);
        }
        formData.setGlueUpdatetime(LocalDateTime.now());
        JobInfo taskInfo = BeanUtil.copyProperties(formData, JobInfo.class);
        taskInfo.setIsNode("N");
        return taskInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskSet(Long id, JobInfoForm formData) {
        // valid trigger
        JobInfo existsJobInfo = baseUpdateTaskInfo(id, formData);
        if (StringUtils.isBlank(formData.getNodes())) {
            throw new BusinessException("任务节点不能为空");
        }
        this.updateById(existsJobInfo);
        // 更新节点
        List<Map> nodeList = JSONUtil.parseArray(formData.getNodes()).toList(Map.class);
        List<Map> edgeList = JSONUtil.parseArray(formData.getEdges()).toList(Map.class);

        // 得到数据库中的节点
        List<JobNode> nodeFromDbList = taskNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, id));

        List<JobNode> taskUpdateNodeList = new ArrayList<>();
        List<JobEdge> taskAddEdgeList = new ArrayList<>();

        Map<String, Long> nodeMap = new HashMap<>();

        nodeList.forEach(item -> {
            JobNode node = new JobNode();
            String nodeId = String.valueOf(item.get("id"));
            node.setTaskParentId(id);
            Map<String, Object> position = (Map<String, Object>) item.get("position");
            node.setNodePositionX(Double.valueOf(String.valueOf(position.get("x"))));
            node.setNodePositionY(Double.valueOf(String.valueOf(position.get("y"))));
            Map<String, Object> data = (Map<String, Object>) item.get("data");
            //node.setTaskId(Long.parseLong(String.valueOf(data.get("taskId"))));
            long taskId = Long.parseLong(String.valueOf(data.get("taskId")));
            if (nodeId.startsWith("node:")) {
                JobInfo copyTaskInfo = this.getById(taskId);
                copyTaskInfo.setId(null);
                copyTaskInfo.setParentId(id);
                copyTaskInfo.setIsNode("Y");
                this.save(copyTaskInfo);
                node.setTaskId(copyTaskInfo.getId());

                if (copyTaskInfo.getJobType() == 2) {
                    List<JobNode> taskNodeList = taskNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, taskId));
                    List<JobEdge> taskEdgeList = taskEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getTaskParentId, taskId));
                    List<JobNodeDto> taskNodeDtos = BeanUtil.copyToList(taskNodeList, JobNodeDto.class);
                    List<JobEdgeDto> taskEdgeDtos = BeanUtil.copyToList(taskEdgeList, JobEdgeDto.class);
                    addNode(taskNodeDtos, taskEdgeDtos, copyTaskInfo);
                    copyTaskInfo.setExecutorParam(String.valueOf(copyTaskInfo.getId()));
                    this.updateById(copyTaskInfo);
                }

                taskNodeService.save(node);
                nodeMap.put(nodeId, node.getId());
            } else {
                node.setTaskId(taskId);
                node.setId(Long.parseLong(nodeId));
                taskUpdateNodeList.add(node);
            }
        });

        edgeList.forEach(item -> {
            JobEdge edge = new JobEdge();
            edge.setTaskParentId(id);
            String sourceId = (String) item.get("source");
            if (sourceId.startsWith("node:")) {
                edge.setFromNodeId(nodeMap.get(sourceId));
            } else {
                edge.setFromNodeId(Long.parseLong(sourceId));
            }
            String targetId = (String) item.get("target");
            if (targetId.startsWith("node:")) {
                edge.setEndNodeId(nodeMap.get(targetId));
            } else {
                edge.setEndNodeId(Long.parseLong(targetId));
            }
            taskAddEdgeList.add(edge);
        });

        // 得到数据库中的边
        taskEdgeService.remove(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getTaskParentId, id));
        taskEdgeService.saveBatch(taskAddEdgeList);

        List<Long> nodeIds = taskUpdateNodeList.stream().map(JobNode::getId).toList();
        List<JobNode> delNodeDbs = nodeFromDbList.stream().filter(v -> !nodeIds.contains(v.getId())).toList();
        if (!delNodeDbs.isEmpty()) {
            for (JobNode delNodeDb : delNodeDbs) {
                JobInfo delTaskInfo = this.getById(delNodeDb.getTaskId());
                if (delTaskInfo.getJobType() == 2) {
                    delNodes(delTaskInfo.getId());
                }
            }
            this.removeBatchByIds(delNodeDbs.stream().map(JobNode::getTaskId).toList());
            taskNodeService.removeBatchByIds(delNodeDbs.stream().map(JobNode::getId).toList());
        }
        taskNodeService.updateBatchById(taskUpdateNodeList);

        // 处理边
        List<JobNode> nodeFromDbList2 = taskNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getTaskParentId, id));
        nodeFromDbList2.forEach(item -> {
            item.setNodeInDegree(taskAddEdgeList.stream().filter(v -> v.getEndNodeId().equals(item.getId())).count());
            item.setNodeOutDegree(taskAddEdgeList.stream().filter(v -> v.getFromNodeId().equals(item.getId())).count());
        });
        return taskNodeService.updateBatchById(nodeFromDbList2);
    }

    @Override
    public boolean stopTaskSet(Long id, String randomId) {
        int flag = taskInfoMapper.stopTaskSet(id);
        XxlJobExecutor.removeJobThread(id.intValue(), "stop task" + id);

        if (redisTemplate.hasKey(id + ":" + randomId)) {
            WorkerWrapper<Long, String> workWrapper = JobGroupXxlJob.getWorkWrapper(id, randomId);
            if (workWrapper != null) {
                log.info(">>>>>>>>> stop task:{}", workWrapper.getId());
                Async.stopWork(workWrapper);
                JobGroupXxlJob.removeWorkWrapper(id, randomId);
                redisTemplate.delete(id + ":" + randomId);
            }
        }

        return true;
    }

    @Override
    public boolean saveGlueSource(JobGlueForm formData) {
        JobInfo taskInfo = this.getById(formData.getTaskId());
        taskInfo.setGlueRemark(formData.getGlueRemark());
        taskInfo.setGlueSource(formData.getGlueSource());
        taskInfo.setGlueUpdatetime(LocalDateTime.now());
        this.updateById(taskInfo);
        JobLogglue taskLogglue = new JobLogglue();
        taskLogglue.setGlueSource(formData.getGlueSource());
        taskLogglue.setGlueRemark(formData.getGlueRemark());
        taskLogglue.setJobId(formData.getTaskId());
        taskLogglue.setGlueType(taskInfo.getGlueType());
        taskLogglueMapper.insert(taskLogglue);
        return true;
    }

    @Override
    public List<JobLogglue> getGlueList(Long id) {
        return taskLogglueMapper.selectList(new LambdaQueryWrapper<JobLogglue>().eq(JobLogglue::getJobId, id));
    }

    private JobInfo baseUpdateTaskInfo(Long id, JobInfoForm formData) {
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(formData.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (formData.getScheduleConf() == null || !CronExpression.isValidExpression(formData.getScheduleConf())) {
                throw new BusinessException("Cron" + I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE /*|| scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
            if (formData.getScheduleConf() == null) {
                throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
            }
            try {
                int fixSecond = Integer.parseInt(formData.getScheduleConf());
                if (fixSecond < 1) {
                    throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
                }
            } catch (Exception e) {
                throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
            }
        }

        if (GlueTypeEnum.match(formData.getGlueType()) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid"));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(formData.getGlueType()) && (formData.getExecutorHandler() == null || formData.getExecutorHandler().trim().length() == 0)) {
            throw new BusinessException(I18nUtil.getString("system_please_input") + "JobHandler");
        }
        // 》fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(formData.getGlueType()) && formData.getGlueSource() != null) {
            formData.setGlueSource(formData.getGlueSource().replaceAll("\r", ""));
        }

        if (GlueTypeEnum.API == GlueTypeEnum.match(formData.getGlueType()) && (StringUtils.isBlank(formData.getReqUrl()) || StringUtils.isBlank(formData.getReqType()))) {
            throw new BusinessException("请求地址和请求类型不能为空");
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(formData.getExecutorRouteStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid"));
        }
        if (MisfireStrategyEnum.match(formData.getMisfireStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid"));
        }
        if (ExecutorBlockStrategyEnum.match(formData.getExecutorBlockStrategy(), null) == null && !"DO_NOTHING".equalsIgnoreCase(formData.getExecutorBlockStrategy())) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid"));
        }

        // 》ChildJobId valid
        if (formData.getChildJobid() != null && formData.getChildJobid().trim().length() > 0) {
            String[] childJobIds = formData.getChildJobid().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    JobInfo childJobInfo = this.getById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        throw new BusinessException(
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    throw new BusinessException(
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item : childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length() - 1);

            formData.setChildJobid(temp);
        }

        // group valid
        JobGroup jobGroup = taskGroupService.getById(formData.getJobGroup());
        if (jobGroup == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_unvalid"));
        }

        // stage job info
        JobInfo existsJobInfo = this.getById(id);
        if (existsJobInfo == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found"));
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = existsJobInfo.getTriggerNextTime();
        boolean scheduleDataNotChanged = formData.getScheduleType().equals(existsJobInfo.getScheduleType()) && formData.getScheduleConf().equals(existsJobInfo.getScheduleConf());
        if (existsJobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
            try {
                existsJobInfo.setScheduleConf(formData.getScheduleConf());
                Date nextValidTime = JobScheduleHelper.generateNextValidTime(existsJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BusinessException(I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid"));
            }
        }

        BeanUtil.copyProperties(formData, existsJobInfo);
        existsJobInfo.setGlueUpdatetime(LocalDateTime.now());
        existsJobInfo.setTriggerNextTime(nextTriggerTime);
        existsJobInfo.setIsNode("N");
        return existsJobInfo;
    }

}
