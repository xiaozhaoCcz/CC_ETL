package com.cc.job.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.common.exception.BusinessException;
import com.cc.job.core.cron.CronExpression;
import com.cc.job.task.enums.*;
import com.cc.job.task.mapper.TaskEdgeMapper;
import com.cc.job.task.mapper.TaskNodeMapper;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskEdge;
import com.cc.job.task.model.entity.TaskGroup;
import com.cc.job.task.model.entity.TaskNode;
import com.cc.job.task.service.TaskEdgeService;
import com.cc.job.task.service.TaskGroupService;
import com.cc.job.task.service.TaskNodeService;
import com.cc.job.task.thread.JobScheduleHelper;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.cc.job.task.utils.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.thread.TriggerCallbackThread;
import com.xxl.job.core.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.task.mapper.TaskInfoMapper;
import com.cc.job.task.service.TaskInfoService;
import com.cc.job.task.model.entity.TaskInfo;
import com.cc.job.task.model.form.TaskInfoForm;
import com.cc.job.task.model.query.TaskInfoQuery;
import com.cc.job.task.model.vo.TaskInfoVO;
import com.cc.job.task.converter.TaskInfoConverter;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
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

    private final TaskInfoConverter taskInfoConverter;

    private final TaskGroupService taskGroupService;

    private final TaskNodeService taskNodeService;

    private final TaskEdgeService taskEdgeService;

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


        Page<TaskInfo> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<TaskInfo> records = page.getRecords();
        List<TaskInfoVO> voList = records.stream().map(v->BeanUtil.copyProperties(v,TaskInfoVO.class)).toList();
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
        return BeanUtil.copyProperties(entity,TaskInfoForm.class);
    }

    /**
     * 新增task_info
     *
     * @param formData task_info表单对象
     * @return
     */
    @Override
    public boolean saveTaskInfo(TaskInfoForm formData) {
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
        TaskInfo taskInfo = BeanUtil.copyProperties(formData,TaskInfo.class);
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
    public boolean updateTaskInfo(Long id, TaskInfoForm formData) {
        // valid trigger
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

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(formData.getExecutorRouteStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid"));
        }
        if (MisfireStrategyEnum.match(formData.getMisfireStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid"));
        }
        if (ExecutorBlockStrategyEnum.match(formData.getExecutorBlockStrategy(), null) == null) {
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
        existsJobInfo.setJobGroup(formData.getJobGroup());
        existsJobInfo.setJobDesc(formData.getJobDesc());
        existsJobInfo.setAuthor(formData.getAuthor());
        existsJobInfo.setAlarmEmail(formData.getAlarmEmail());
        existsJobInfo.setScheduleType(formData.getScheduleType());
        existsJobInfo.setScheduleConf(formData.getScheduleConf());
        existsJobInfo.setMisfireStrategy(formData.getMisfireStrategy());
        existsJobInfo.setExecutorRouteStrategy(formData.getExecutorRouteStrategy());
        existsJobInfo.setExecutorHandler(formData.getExecutorHandler());
        existsJobInfo.setExecutorParam(formData.getExecutorParam());
        existsJobInfo.setExecutorBlockStrategy(formData.getExecutorBlockStrategy());
        existsJobInfo.setExecutorTimeout(formData.getExecutorTimeout());
        existsJobInfo.setExecutorFailRetryCount(formData.getExecutorFailRetryCount());
        existsJobInfo.setChildJobid(formData.getChildJobid());
        existsJobInfo.setTriggerNextTime(nextTriggerTime);
        existsJobInfo.setJobType(formData.getJobType());
        existsJobInfo.setReqType(formData.getReqType());
        existsJobInfo.setParentId(formData.getParentId());
        existsJobInfo.setReqHeader(formData.getReqHeader());
        existsJobInfo.setReqBody(formData.getReqBody());
        existsJobInfo.setReqUrl(formData.getReqUrl());
        return this.updateById(existsJobInfo);
    }

    /**
     * 删除task_info
     *
     * @param ids task_infoID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteTaskInfos(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的task_info数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

    @Override
    public boolean triggerJob(TaskInfoTriggerDto taskInfoTriggerDto) {

//        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(jobId);
//        if (xxlJobInfo == null) {
//            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
//        }
        // force cover job param
        if (taskInfoTriggerDto.getExecutorParam() == null) {
            taskInfoTriggerDto.setExecutorParam("");
        }

        JobTriggerPoolHelper.trigger(taskInfoTriggerDto.getId().intValue(), TriggerTypeEnum.MANUAL, -1, null, taskInfoTriggerDto.getExecutorParam(), taskInfoTriggerDto.getAddressList());
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
    public boolean stopTask(Long id) {
        TaskInfo xxlJobInfo = this.getById(id);

        xxlJobInfo.setTriggerStatus(0);
        xxlJobInfo.setTriggerLastTime(0L);
        xxlJobInfo.setTriggerNextTime(0L);
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
    public boolean runTaskSet(Long id) {

        System.out.println("runTask"+id);
        List<TaskNode> nodes = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId,id));
        List<TaskEdge> edges = taskEdgeService.list(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId,id));


        List<TaskNode> startNodes = nodes.stream().filter(v -> v.getNodeInDegree().equals(0L)).toList();

        List<Long> visited = new CopyOnWriteArrayList<>();
        Queue<Long> queue = new ConcurrentLinkedQueue<>();

        Map<Long, List<Long>> nodeMap = new HashMap<>();

        Map<Long, CompletableFuture<TaskNode>> futureMap = new HashMap<>();

        startNodes.forEach(item -> {
            queue.add(item.getId());
            visited.add(item.getId());
        });

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();

        while (!queue.isEmpty()) {
            List<Long> currentLevelNodes = new ArrayList<>();
            while (!queue.isEmpty()) {
                Long poll = queue.poll();
                List<Long> collect = edges.stream().filter(v -> v.getEndNodeId().equals(poll)).map(TaskEdge::getFromNodeId).toList();
                nodeMap.put(poll, collect);
                currentLevelNodes.add(poll);
            }

            for (Long node : currentLevelNodes) {
                futures.add(executor.submit(() -> {
                    for (long neighbor : getNeighbors(node, edges)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            futures.clear();
        }

        System.out.println(visited);
        System.out.println(nodeMap);

        nodeMap.forEach((k, v) -> {
            TaskNode currentNode = nodes.stream().filter(item -> item.getId().equals(k)).findFirst().orElse(null);
            CompletableFuture<TaskNode> future = CompletableFuture.supplyAsync(() -> {
                for (long d : v) {
                    //阻塞等待
                    try {
                        futureMap.get(d).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                //执行任务
                runT(k);
                return currentNode;
            });
            futureMap.put(k, future);
        });
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]));
        try {
            voidCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTaskSet(TaskInfoForm formData) {
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
        TaskInfo taskInfo = BeanUtil.copyProperties(formData,TaskInfo.class);
        this.save(taskInfo);

        List<Map> nodeList = JSONUtil.parseArray(formData.getNodes()).toList(Map.class);
        List<Map> edgeList = JSONUtil.parseArray(formData.getEdges()).toList(Map.class);


        List<TaskEdge> taskEdgeList = new ArrayList<>();

        Map<String,Long> nodeMap = new HashMap<>();

        nodeList.forEach(item -> {
            TaskNode node = new TaskNode();
            String nodeId = (String) item.get("id");
            node.setTaskParentId(taskInfo.getId());
            Map<String, Object> data = (Map<String, Object>) item.get("data");
            Map<String, Object> position = (Map<String, Object>) item.get("position");
            node.setTaskId(Long.parseLong(String.valueOf(data.get("taskId"))));
            node.setNodePositionX(Double.valueOf(String.valueOf(position.get("x"))));
            node.setNodePositionY(Double.valueOf(String.valueOf(position.get("y"))));
            if(nodeId.startsWith("node:")){
                taskNodeService.save(node);
                nodeMap.put(nodeId,node.getId());
            }
        });

        edgeList.forEach(item -> {
            TaskEdge edge = new TaskEdge();
            edge.setTaskParentId(taskInfo.getId());
            String sourceId = (String) item.get("source");
            if(sourceId.startsWith("node:")){
                edge.setFromNodeId(nodeMap.get(sourceId));
            }
            String targetId = (String) item.get("target");
            if(targetId.startsWith("node:")){
                edge.setEndNodeId(nodeMap.get(targetId));
            }
            taskEdgeList.add(edge);
        });
        taskEdgeService.saveBatch(taskEdgeList);

        List<TaskNode> nodeFromDbList = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, taskInfo.getId()));
        nodeFromDbList.forEach(item -> {
            item.setNodeInDegree(taskEdgeList.stream().filter(v -> v.getEndNodeId().equals(item.getId())).count());
            item.setNodeOutDegree(taskEdgeList.stream().filter(v -> v.getFromNodeId().equals(item.getId())).count());
        });
        return taskNodeService.updateBatchById(nodeFromDbList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskSet( Long id,TaskInfoForm formData) {
        // valid trigger
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

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(formData.getExecutorRouteStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid"));
        }
        if (MisfireStrategyEnum.match(formData.getMisfireStrategy(), null) == null) {
            throw new BusinessException(I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid"));
        }
        if (ExecutorBlockStrategyEnum.match(formData.getExecutorBlockStrategy(), null) == null) {
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
        existsJobInfo.setJobGroup(formData.getJobGroup());
        existsJobInfo.setJobDesc(formData.getJobDesc());
        existsJobInfo.setAuthor(formData.getAuthor());
        existsJobInfo.setAlarmEmail(formData.getAlarmEmail());
        existsJobInfo.setScheduleType(formData.getScheduleType());
        existsJobInfo.setScheduleConf(formData.getScheduleConf());
        existsJobInfo.setMisfireStrategy(formData.getMisfireStrategy());
        existsJobInfo.setExecutorRouteStrategy(formData.getExecutorRouteStrategy());
        existsJobInfo.setExecutorHandler(formData.getExecutorHandler());
        existsJobInfo.setExecutorParam(formData.getExecutorParam());
        existsJobInfo.setExecutorBlockStrategy(formData.getExecutorBlockStrategy());
        existsJobInfo.setExecutorTimeout(formData.getExecutorTimeout());
        existsJobInfo.setExecutorFailRetryCount(formData.getExecutorFailRetryCount());
        existsJobInfo.setChildJobid(formData.getChildJobid());
        existsJobInfo.setTriggerNextTime(nextTriggerTime);
        existsJobInfo.setJobType(formData.getJobType());
        existsJobInfo.setReqType(formData.getReqType());
        existsJobInfo.setParentId(formData.getParentId());
        existsJobInfo.setReqHeader(formData.getReqHeader());
        existsJobInfo.setReqBody(formData.getReqBody());
        existsJobInfo.setReqUrl(formData.getReqUrl());
        this.updateById(existsJobInfo);

        // 更新节点
        List<Map<String, Object>> nodeList = JSONUtil.toBean(formData.getNodes(), List.class);
        List<Map<String, Object>> edgeList = JSONUtil.toBean(formData.getEdges(), List.class);

        // 得到数据库中的节点
        List<TaskNode> nodeFromDbList = taskNodeService.list(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskParentId, id));

        List<TaskNode> taskUpdateNodeList = new ArrayList<>();
        List<TaskEdge> taskAddEdgeList = new ArrayList<>();


        Map<String,Long> nodeMap = new HashMap<>();

        nodeList.forEach(item -> {
            TaskNode node = new TaskNode();
            String nodeId = (String) item.get("id");
            node.setTaskParentId(id);
            Map<String, Object> position = (Map<String, Object>) item.get("position");
            node.setNodePositionX(Double.valueOf(String.valueOf(position.get("x"))));
            node.setNodePositionY(Double.valueOf(String.valueOf(position.get("y"))));
            Map<String, Object> data = (Map<String, Object>) item.get("data");
            node.setTaskId(Long.parseLong((String) data.get("taskId")));
            if(nodeId.startsWith("node:")){
                taskNodeService.save(node);
                nodeMap.put(nodeId,node.getId());
            }else {
                node.setId(Long.parseLong(nodeId));
                taskUpdateNodeList.add(node);
            }
        });

        edgeList.forEach(item -> {
            TaskEdge edge = new TaskEdge();
            edge.setTaskParentId(id);
            String sourceId = (String) item.get("source");
            if(sourceId.startsWith("node:")){
                edge.setFromNodeId(nodeMap.get(sourceId));
            }else{
                edge.setFromNodeId(Long.parseLong(sourceId));
            }
            String targetId = (String) item.get("target");
            if(targetId.startsWith("node:")){
                edge.setEndNodeId(nodeMap.get(targetId));
            }else{
                edge.setEndNodeId(Long.parseLong(targetId));
            }
            taskAddEdgeList.add(edge);
        });

        // 得到数据库中的边
        taskEdgeService.remove(new LambdaQueryWrapper<TaskEdge>().eq(TaskEdge::getTaskParentId,id));
        taskEdgeService.saveBatch(taskAddEdgeList);


        List<Long> nodeIds = taskUpdateNodeList.stream().map(TaskNode::getId).toList();
        List<TaskNode> delNodeDbs = nodeFromDbList.stream().filter(v -> !nodeIds.contains(v.getId())).toList();
        if(!delNodeDbs.isEmpty()){
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


    // 使用线程的方式模拟
    private void runT(Long k) {
        System.out.println("start node" + k);

        TaskInfoTriggerDto taskInfoTriggerDto = new TaskInfoTriggerDto();
        taskInfoTriggerDto.setId(k);
        triggerJob(taskInfoTriggerDto);

        try {
            Thread futureThread = null;
            FutureTask<Boolean> futureTask = new FutureTask<Boolean>(() -> {
                Label:
                while (true){
                    Vector<ReturnT<Long>> vector = TriggerCallbackThread.vector;
                    List<ReturnT<Long>> list = new ArrayList<>(vector);
                    for (ReturnT<Long> res : list) {
                        if(res.getContent().equals(k)){
                            System.out.println("end node" + k);
                            TriggerCallbackThread.vector.remove(res);
                            break Label;
                        }
                    }
                }
                return true;
            });
            futureThread = new Thread(futureTask);
            futureThread.start();

            Boolean tempResult = futureTask.get();
        }catch (Exception e){

        }
    }


    private List<Long> getNeighbors(Long node, List<TaskEdge> edges) {
        return  edges.stream().filter(v -> v.getFromNodeId().equals(node)).map(TaskEdge::getEndNodeId).collect(Collectors.toList());
    }

}
