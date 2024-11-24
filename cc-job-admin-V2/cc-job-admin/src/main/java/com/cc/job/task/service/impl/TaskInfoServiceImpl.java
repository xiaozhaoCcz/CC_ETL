package com.cc.job.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.common.exception.BusinessException;
import com.cc.job.core.cron.CronExpression;
import com.cc.job.task.enums.*;
import com.cc.job.task.jobhandler.TaskRankXxlJob;
import com.cc.job.task.mapper.TaskLogglueMapper;
import com.cc.job.task.model.dto.TaskEdgeDto;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.dto.TaskNodeDto;
import com.cc.job.task.model.entity.*;
import com.cc.job.task.model.form.TaskGlueForm;
import com.cc.job.task.model.vo.TaskNodeVo;
import com.cc.job.task.service.TaskEdgeService;
import com.cc.job.task.service.TaskGroupService;
import com.cc.job.task.service.TaskNodeService;
import com.cc.job.task.thread.JobScheduleHelper;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.cc.job.task.utils.I18nUtil;
import com.cc.job.task.websocket.WebSocketServer;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskInfoMapper;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.model.form.TaskInfoForm;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;

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
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, TaskInfo> implements TaskInfoService {

    private final TaskGroupService taskGroupService;

    private final TaskNodeService taskNodeService;

    private final TaskEdgeService taskEdgeService;

    private final TaskLogglueMapper taskLogglueMapper;

    private final WebSocketServer webSocketServer;

    private final TaskInfoMapper taskInfoMapper;

    /**
     * 获取task_info分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<TaskInfoVO>} task_info分页列表
     */
    @Override
    public IPage<TaskInfoVO> getTaskInfoPage(TaskInfoQuery queryParams) {
//        Page<TaskInfoVO> pageVO = this.baseMapper.getTaskInfoPage(
//                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
//                queryParams
//        );
        Page<TaskInfoVO> pageVO = new Page<>();
        LambdaQueryWrapper<TaskInfo> wrapper = new LambdaQueryWrapper<>();
        if (queryParams.getJobGroup() != null) {
            wrapper.eq(TaskInfo::getJobGroup, queryParams.getJobGroup());
        }
        if (queryParams.getTriggerStatus() != null) {
            wrapper.eq(TaskInfo::getTriggerStatus, queryParams.getTriggerStatus());
        }
        if (StringUtils.isNotBlank(queryParams.getAuthor())) {
            wrapper.like(TaskInfo::getAuthor, queryParams.getAuthor());
        }
        if (StringUtils.isNotBlank(queryParams.getJobDesc())) {
            wrapper.like(TaskInfo::getJobDesc, queryParams.getJobDesc());
        }
        if (StringUtils.isNotBlank(queryParams.getExecutorHandler())) {
            wrapper.eq(TaskInfo::getExecutorHandler, queryParams.getExecutorHandler());
        }

        wrapper.in(TaskInfo::getJobType, 0, 2);
        wrapper.in(TaskInfo::getIsNode, "N");
        wrapper.orderByDesc(TaskInfo::getUpdateTime);

        Page<TaskInfo> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<TaskInfo> records = page.getRecords();
        List<TaskInfoVO> voList = records.stream().map(v -> BeanUtil.copyProperties(v, TaskInfoVO.class)).toList();
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
    public TaskInfoForm getTaskInfoFormData(Long id) {
        TaskInfo entity = this.getById(id);
        TaskInfoForm taskInfoForm = BeanUtil.copyProperties(entity, TaskInfoForm.class);
        if (entity.getJobType() == 2) {
            List<TaskNode> taskNodeList = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, id));
            List<TaskEdge> taskEdgeList = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, id));
            List<TaskNodeVo> taskNodeVoList = new ArrayList<>();
            List<Long> taskIds = taskNodeList.stream().map(TaskNode::getTaskId).toList();
            List<TaskInfo> taskInfos = this.listByIds(taskIds);
            Map<Long, String> taskMap = taskInfos.stream().collect(Collectors.toMap(TaskInfo::getId, TaskInfo::getJobDesc));
            for (TaskNode node : taskNodeList) {
                TaskNodeVo taskNodeVo = BeanUtil.copyProperties(node, TaskNodeVo.class);
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
    public boolean saveTaskInfo(TaskInfoForm formData) {
        TaskInfo taskInfo = baseSaveTaskInfo(formData);
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
    public boolean updateTaskInfo(Long id, TaskInfoForm formData) {
        // valid trigger
        TaskInfo existsJobInfo = baseUpdateTaskInfo(id, formData);
        updateChild(existsJobInfo);
        return true;
    }

    public void updateChild(TaskInfo taskInfo) {
        if (taskInfo.getJobType() == 2 && "Y".equalsIgnoreCase(taskInfo.getIsNode())) {
            //下面的子节点全部更新
            List<TaskInfo> taskInfos = this.list(new LambdaQueryWrapper<TaskInfo>().eq(TaskInfo::getParentId, taskInfo.getId()));
            for (TaskInfo info : taskInfos) {
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
        TaskInfo taskInfo = this.getById(jobId);
        if (taskInfo.getJobType() == 2) {
            List<TaskInfo> taskInfos = this.list(new LambdaQueryWrapper<TaskInfo>().eq(TaskInfo::getParentId, jobId));
            if (taskInfos.isEmpty()) {
                return;
            }
            List<Long> childTaskIds = taskInfos.stream().map(TaskInfo::getId).toList();

            taskNodeService.remove(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));
            taskEdgeService.remove(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskInfo.getId()));

            for (Long childTaskId : childTaskIds) {
                delNodes(childTaskId);
            }
        }

        this.removeById(jobId);
    }

    @Override
    public boolean triggerJob(TaskInfoTriggerDto taskInfoTriggerDto) {

        TaskInfo taskInfo = this.getById(taskInfoTriggerDto.getId());
        if (taskInfo == null) {
            return false;
        }

        if(taskInfo.getJobType()==2&&taskInfo.getRankTriggerStatus()==1){
            throw new BusinessException("当前任务正在运行中～");
        }

        if (taskInfo.getJobType() == 2) {
            TaskRankXxlJob.removeStopMap(taskInfo.getId());
        }
        // force cover job param
        if (taskInfoTriggerDto.getExecutorParam() == null) {
            taskInfoTriggerDto.setExecutorParam("");
        }

        JobTriggerPoolHelper.trigger(taskInfoTriggerDto.getId().intValue(), TriggerTypeEnum.MANUAL, -1, null, taskInfoTriggerDto.getExecutorParam(), taskInfoTriggerDto.getAddressList());

        taskInfo.setRankTriggerStatus(1);
        this.updateById(taskInfo);
        return true;
    }

    @Override
    public boolean startTask(Long id) {
        TaskInfo xxlJobInfo = this.getById(id);

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
        TaskInfo xxlJobInfo = this.getById(id);
        xxlJobInfo.setTriggerStatus(0);
        xxlJobInfo.setTriggerLastTime(0L);
        xxlJobInfo.setTriggerNextTime(0L);
        if(xxlJobInfo.getJobType()==2){
            this.stopTaskSet(id);
        }
        return this.updateById(xxlJobInfo);
    }

    @Override
    public List<String> nextTriggerTime(String scheduleType, String scheduleConf) {
        TaskInfo paramXxlJobInfo = new TaskInfo();
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
    public boolean saveTaskSet(TaskInfoForm formData) {
        TaskInfo taskInfo = baseSaveTaskInfo(formData);
        if (StringUtils.isBlank(formData.getNodes())) {
            throw new BusinessException("任务节点不能为空");
        }

        taskInfo.setJobType(2);
        this.save(taskInfo);
        taskInfo.setExecutorParam(String.valueOf(taskInfo.getId()));
        this.updateById(taskInfo);

        List<Map> nodeList = JSONUtil.parseArray(formData.getNodes()).toList(Map.class);
        List<Map> edgeList = JSONUtil.parseArray(formData.getEdges()).toList(Map.class);

        List<TaskNodeDto> taskNodeDtoList = new ArrayList<>();
        List<TaskEdgeDto> taskEdgeDtoList = new ArrayList<>();

        nodeList.forEach(item -> {
            TaskNodeDto node = new TaskNodeDto();
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
            TaskEdgeDto edge = new TaskEdgeDto();
            edge.setTaskParentId(taskInfo.getId());
            edge.setFromNodeId((String) item.get("source"));
            edge.setEndNodeId((String) item.get("target"));
            taskEdgeDtoList.add(edge);
        });

        //计算节点的出度和人度

        for (TaskNodeDto taskNodeDto : taskNodeDtoList) {
            taskNodeDto.setNodeOutDegree(taskEdgeDtoList.stream().filter(v -> v.getFromNodeId().equalsIgnoreCase(taskNodeDto.getId())).count());
            taskNodeDto.setNodeInDegree(taskEdgeDtoList.stream().filter(v -> v.getEndNodeId().equalsIgnoreCase(taskNodeDto.getId())).count());
        }

        addNode(taskNodeDtoList, taskEdgeDtoList, taskInfo);
        return true;
    }

    private void addNode(List<TaskNodeDto> nodeList, List<TaskEdgeDto> edgeList, TaskInfo parentTask) {
        Map<String, Long> nodeMap = new HashMap<>();

        for (TaskNodeDto taskNode : nodeList) {
            TaskInfo taskInfo = this.getById(taskNode.getTaskId());

            TaskInfo copyTaskInfo = BeanUtil.copyProperties(taskInfo, TaskInfo.class);
            copyTaskInfo.setId(null);
            copyTaskInfo.setIsNode("Y");
            copyTaskInfo.setParentId(parentTask.getId());
            this.save(copyTaskInfo);

            if (taskInfo.getJobType() == 2) {
                List<TaskNode> childNodes = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));
                List<TaskEdge> childEdges = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskInfo.getId()));

                List<TaskNodeDto> taskNodeDtos = BeanUtil.copyToList(childNodes, TaskNodeDto.class, CopyOptions.create());
                List<TaskEdgeDto> taskEdgeDtos = BeanUtil.copyToList(childEdges, TaskEdgeDto.class, CopyOptions.create());
                addNode(taskNodeDtos, taskEdgeDtos, copyTaskInfo);
                copyTaskInfo.setExecutorParam(String.valueOf(copyTaskInfo.getId()));
                this.updateById(copyTaskInfo);
            }

            TaskNode copyTaskNode = BeanUtil.copyProperties(taskNode, TaskNode.class, "id");
            copyTaskNode.setId(null);
            copyTaskNode.setTaskId(copyTaskInfo.getId());
            copyTaskNode.setTaskParentId(parentTask.getId());
            taskNodeService.save(copyTaskNode);
            nodeMap.put(String.valueOf(taskNode.getId()), copyTaskNode.getId());
        }

        for (TaskEdgeDto taskEdge : edgeList) {
            TaskEdge edge = new TaskEdge();
            edge.setTaskParentId(parentTask.getId());
            edge.setFromNodeId(nodeMap.get(String.valueOf(taskEdge.getFromNodeId())));
            edge.setEndNodeId(nodeMap.get(String.valueOf(taskEdge.getEndNodeId())));
            taskEdgeService.save(edge);
        }
    }


    private TaskInfo baseSaveTaskInfo(TaskInfoForm formData) {
        TaskGroup taskGroup = taskGroupService.getById(formData.getJobGroup());
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
                    TaskInfo childJobInfo = this.getById(Integer.parseInt(childJobIdItem));
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
        TaskInfo taskInfo = BeanUtil.copyProperties(formData, TaskInfo.class);
        taskInfo.setIsNode("N");
        return taskInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskSet(Long id, TaskInfoForm formData) {
        // valid trigger
        TaskInfo existsJobInfo = baseUpdateTaskInfo(id, formData);
        if (StringUtils.isBlank(formData.getNodes())) {
            throw new BusinessException("任务节点不能为空");
        }
        this.updateById(existsJobInfo);
        // 更新节点
        List<Map> nodeList = JSONUtil.parseArray(formData.getNodes()).toList(Map.class);
        List<Map> edgeList = JSONUtil.parseArray(formData.getEdges()).toList(Map.class);

        // 得到数据库中的节点
        List<TaskNode> nodeFromDbList = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, id));

        List<TaskNode> taskUpdateNodeList = new ArrayList<>();
        List<TaskEdge> taskAddEdgeList = new ArrayList<>();

        Map<String, Long> nodeMap = new HashMap<>();

        nodeList.forEach(item -> {
            TaskNode node = new TaskNode();
            String nodeId = String.valueOf(item.get("id"));
            node.setTaskParentId(id);
            Map<String, Object> position = (Map<String, Object>) item.get("position");
            node.setNodePositionX(Double.valueOf(String.valueOf(position.get("x"))));
            node.setNodePositionY(Double.valueOf(String.valueOf(position.get("y"))));
            Map<String, Object> data = (Map<String, Object>) item.get("data");
            //node.setTaskId(Long.parseLong(String.valueOf(data.get("taskId"))));
            long taskId = Long.parseLong(String.valueOf(data.get("taskId")));
            if (nodeId.startsWith("node:")) {
                TaskInfo copyTaskInfo = this.getById(taskId);
                copyTaskInfo.setId(null);
                copyTaskInfo.setParentId(id);
                copyTaskInfo.setIsNode("Y");
                this.save(copyTaskInfo);
                node.setTaskId(copyTaskInfo.getId());

                if (copyTaskInfo.getJobType() == 2) {
                    List<TaskNode> taskNodeList = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskId));
                    List<TaskEdge> taskEdgeList = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, taskId));
                    List<TaskNodeDto> taskNodeDtos = BeanUtil.copyToList(taskNodeList, TaskNodeDto.class);
                    List<TaskEdgeDto> taskEdgeDtos = BeanUtil.copyToList(taskEdgeList, TaskEdgeDto.class);
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
            TaskEdge edge = new TaskEdge();
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
        taskEdgeService.remove(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId, id));
        taskEdgeService.saveBatch(taskAddEdgeList);

        List<Long> nodeIds = taskUpdateNodeList.stream().map(TaskNode::getId).toList();
        List<TaskNode> delNodeDbs = nodeFromDbList.stream().filter(v -> !nodeIds.contains(v.getId())).toList();
        if (!delNodeDbs.isEmpty()) {
            for (TaskNode delNodeDb : delNodeDbs) {
                TaskInfo delTaskInfo = this.getById(delNodeDb.getTaskId());
                if (delTaskInfo.getJobType() == 2) {
                    delNodes(delTaskInfo.getId());
                }
            }
            this.removeBatchByIds(delNodeDbs.stream().map(TaskNode::getTaskId).toList());
            taskNodeService.removeBatchByIds(delNodeDbs.stream().map(TaskNode::getId).toList());
        }
        taskNodeService.updateBatchById(taskUpdateNodeList);

        // 处理边
        List<TaskNode> nodeFromDbList2 = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, id));
        nodeFromDbList2.forEach(item -> {
            item.setNodeInDegree(taskAddEdgeList.stream().filter(v -> v.getEndNodeId().equals(item.getId())).count());
            item.setNodeOutDegree(taskAddEdgeList.stream().filter(v -> v.getFromNodeId().equals(item.getId())).count());
        });
        return taskNodeService.updateBatchById(nodeFromDbList2);
    }

    @Override
    public boolean stopTaskSet(Long id) {
        int flag = taskInfoMapper.stopTaskSet(id);
        if(flag>0){
            XxlJobExecutor.removeJobThread(id.intValue(), "stop task" + id);
            List<Long> allTaskInfoIds = new ArrayList<>();
            // 得到当前任务的所有子任务
            getChildTaskInfos(id, allTaskInfoIds);
            allTaskInfoIds.forEach(item -> TaskRankXxlJob.processStopMap(id, true, item));
            webSocketServer.onClose(id);
        }
        return true;
    }

    private void getChildTaskInfos(Long id, List<Long> allTaskInfoIds) {
        List<TaskInfo> taskInfos = this.list(new LambdaQueryWrapper<TaskInfo>().eq(TaskInfo::getParentId, id));
        for (TaskInfo taskInfo : taskInfos) {
            if (taskInfo.getJobType() == 2) {
                getChildTaskInfos(taskInfo.getId(), allTaskInfoIds);
            }
        }
        List<Long> ids = taskInfos.stream().map(TaskInfo::getId).toList();
        allTaskInfoIds.addAll(ids);
    }

    @Override
    public boolean saveGlueSource(TaskGlueForm formData) {
        TaskInfo taskInfo = this.getById(formData.getTaskId());
        taskInfo.setGlueRemark(formData.getGlueRemark());
        taskInfo.setGlueSource(formData.getGlueSource());
        taskInfo.setGlueUpdatetime(LocalDateTime.now());
        this.updateById(taskInfo);
        TaskLogglue taskLogglue = new TaskLogglue();
        taskLogglue.setGlueSource(formData.getGlueSource());
        taskLogglue.setGlueRemark(formData.getGlueRemark());
        taskLogglue.setJobId(formData.getTaskId());
        taskLogglue.setGlueType(taskInfo.getGlueType());
        taskLogglueMapper.insert(taskLogglue);
        return true;
    }

    @Override
    public List<TaskLogglue> getGlueList(Long id) {
        return taskLogglueMapper.selectList(new LambdaQueryWrapper<TaskLogglue>().eq(TaskLogglue::getJobId, id));
    }

    @NotNull
    private TaskInfo baseUpdateTaskInfo(Long id, TaskInfoForm formData) {
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
                    TaskInfo childJobInfo = this.getById(Integer.parseInt(childJobIdItem));
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
        TaskGroup jobGroup = taskGroupService.getById(formData.getJobGroup());
        if (jobGroup == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_unvalid"));
        }

        // stage job info
        TaskInfo existsJobInfo = this.getById(id);
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
