package com.cc.job.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.common.exception.BusinessException;
import com.cc.job.core.cron.CronExpression;
import com.cc.job.task.enums.*;
import com.cc.job.task.model.dto.TaskInfoTriggerDto;
import com.cc.job.task.model.entity.TaskEdge;
import com.cc.job.task.model.entity.TaskGroup;
import com.cc.job.task.model.entity.TaskNode;
import com.cc.job.task.service.TaskGroupService;
import com.cc.job.task.thread.JobScheduleHelper;
import com.cc.job.task.thread.JobTriggerPoolHelper;
import com.cc.job.task.utils.I18nUtil;
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
        List<TaskNode> nodes = new ArrayList<>();
        List<TaskEdge> edges = new ArrayList<>();

        TaskNode node1 = new TaskNode(1L, 1.0,1.0, 0, 0,0);

        TaskNode node2 = new TaskNode(2L, 1.0,1.0, 0, 0,0);
        TaskNode node3 = new TaskNode(3L, 1.0,1.0, 0, 0,0);
        TaskNode node4 = new TaskNode(4L, 1.0,1.0, 0, 0,0);
        TaskNode node5 = new TaskNode(5L, 1.0,1.0, 0, 0,0);
        TaskNode node6 = new TaskNode(6L, 1.0,1.0, 0, 0,0);
        TaskNode node7 = new TaskNode(7L, 1.0,1.0, 0, 0,0);
        TaskNode node8 = new TaskNode(8L, 1.0,1.0, 0, 0,0);
        TaskNode node9 = new TaskNode(9L, 1.0,1.0, 0, 0,0);
        TaskNode node10 = new TaskNode(10L, 1.0,1.0, 0, 0,0);

        node1.setId(1L);
        node2.setId(2L);
        node3.setId(3L);
        node4.setId(4L);
        node5.setId(5L);
        node6.setId(6L);
        node7.setId(7L);
        node8.setId(8L);
        node9.setId(9L);
        node10.setId(10L);


        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);
        nodes.add(node5);
        nodes.add(node6);
        nodes.add(node7);
        nodes.add(node8);
        nodes.add(node9);
        nodes.add(node10);

        TaskEdge edge1 = new TaskEdge(1L,1L, 2L,LocalDateTime.now());
        TaskEdge edge2 = new TaskEdge(2L,1L, 3L,LocalDateTime.now());
        TaskEdge edge3 = new TaskEdge(3L,1L, 4L,LocalDateTime.now());
        TaskEdge edge4 = new TaskEdge(4L,1L, 5L,LocalDateTime.now());
        TaskEdge edge5 = new TaskEdge(5L,1L, 6L,LocalDateTime.now());
        TaskEdge edge6 = new TaskEdge(6L,2L, 7L,LocalDateTime.now());
        TaskEdge edge7 = new TaskEdge(7L,3L, 7L,LocalDateTime.now());
        TaskEdge edge8 = new TaskEdge(8L,4L, 8L,LocalDateTime.now());
        TaskEdge edge9 = new TaskEdge(9L,5L, 8L,LocalDateTime.now());
        TaskEdge edge10 = new TaskEdge(10L,6L, 9L,LocalDateTime.now());
        TaskEdge edge11 = new TaskEdge(11L,7L, 10L,LocalDateTime.now());
        TaskEdge edge12 = new TaskEdge(12L,8L, 10L,LocalDateTime.now());
        TaskEdge edge13 = new TaskEdge(13L,9L, 10L,LocalDateTime.now());
        edges.add(edge1);
        edges.add(edge2);
        edges.add(edge3);
        edges.add(edge4);
        edges.add(edge5);
        edges.add(edge6);
        edges.add(edge7);
        edges.add(edge8);
        edges.add(edge9);
        edges.add(edge10);
        edges.add(edge11);
        edges.add(edge12);
        edges.add(edge13);


        // 计算节点的出度和入度
        for (TaskNode node : nodes) {
            long inCount = edges.stream().filter(v -> Objects.equals(v.getEndNodeId(), node.getId())).count();
            long outCount = edges.stream().filter(v -> Objects.equals(v.getFromNodeId(), node.getId())).count();
            node.setNodeInDegree((int) inCount);
            node.setNodeOutDegree((int) outCount);
        }

        List<TaskNode> startNodes = nodes.stream().filter(v -> v.getNodeInDegree() == 0).toList();

        List<Long> visited = new CopyOnWriteArrayList<>();
        Queue<Long> queue = new ConcurrentLinkedQueue<>();

        Map<Long, List<Long>> nodeMap = new HashMap<>();

        Map<Long, CompletableFuture<TaskNode>> futureMap = new HashMap<>();

        startNodes.forEach(item -> {
            queue.add(item.getId());
            visited.add(item.getId());
        });

//        Map<Integer, Integer> distances = new ConcurrentHashMap<>();
//        distances.put(1, 0);
//        Queue<Integer> queue = new ConcurrentLinkedQueue<>();
//        queue.add(1);

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


    // 使用线程的方式模拟
    private void runT(Long k) {
        System.out.println("start node" + k);

        TaskInfoTriggerDto taskInfoTriggerDto = new TaskInfoTriggerDto();
        taskInfoTriggerDto.setId(k);
        triggerJob(taskInfoTriggerDto);

        try {
            Thread futureThread = null;
            FutureTask<Boolean> futureTask = new FutureTask<Boolean>(() -> {

                while (true){
                    Vector<Long> vector = TriggerCallbackThread.vector;
                    if(vector.contains(k)){
                        System.out.println("end node" + k);
                        vector.remove(k);
                        break;
                    }
                }
                return true;
            });
            futureThread = new Thread(futureTask);
            futureThread.start();

            Boolean tempResult = futureTask.get(10, TimeUnit.SECONDS);
        }catch (Exception e){

        }
    }


    private List<Long> getNeighbors(Long node, List<TaskEdge> edges) {
        return  edges.stream().filter(v -> v.getFromNodeId().equals(node)).map(TaskEdge::getEndNodeId).collect(Collectors.toList());
    }

}
