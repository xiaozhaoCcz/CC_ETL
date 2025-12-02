package com.cc.job.admin.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.service.*;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.admin.cron.CronExpression;
import com.cc.job.admin.task.enums.*;
import com.cc.job.xo.mapper.JobLogMapper;
import com.cc.job.xo.mapper.JobLogglueMapper;
import com.cc.job.xo.model.dto.JobEdgeDto;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.dto.JobNodeDto;
import com.cc.job.xo.model.entity.*;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.xo.model.vo.JobNodeVo;
import com.cc.job.admin.task.thread.JobScheduleHelper;
import com.cc.job.admin.task.thread.JobTriggerPoolHelper;
import com.cc.job.admin.task.utils.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import com.xxl.job.core.util.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private final JobGroupService jobGroupService;

    private final JobNodeService jobNodeService;

    private final JobEdgeService jobEdgeService;

    private final JobLogglueMapper jobLogglueMapper;

    private final JobInfoMapper jobInfoMapper;

    private final JobLogMapper jobLogMapper;

    private final JobGroupSnapshotService jobGroupSnapshotService;

    private final String ADMIN_ADDRESS = "http://%s:%s/xxl-job-admin/";
    @Value("${server.port}")
    private int port;
    
    // ⭐ 用于异步调用停止接口的线程池
    private static final ExecutorService STOP_JOB_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "StopJobExecutor-Thread");
        t.setDaemon(true);
        return t;
    });

    static final Map<String,String> NODE_TYPE_MAP = new HashMap<>(){{
        put("SQL","custom-sql");
        put("API","custom-api");
        put("BEAN","custom-bean");
        put("GLUE_GROOVY","custom-java");
        put("GLUE_SHELL","custom-shell");
        put("GLUE_PYTHON","custom-python");
        put("GLUE_PHP","custom-php");
        put("GLUE_NODEJS","custom-nodejs");
        put("GLUE_POWERSHELL","custom-powershell");
        // 自定义：任务组容器节点
        put("CUSTOM_GROUP","custom-group");
    }};


    /**
     * 获取task_info分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage< JobInfoVO >} task_info分页列表
     */
    @Override
    public IPage<JobInfoVO> getJobInfoPage(JobInfoQuery queryParams) {
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
    public JobInfoForm getJobInfoForm(Long id) {
        JobInfo entity = this.getById(id);
        JobInfoForm taskInfoForm = BeanUtil.copyProperties(entity, JobInfoForm.class);
        if (entity!=null && entity.getJobType() == 2) {
            List<JobNode> taskNodeList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, id));
            List<JobEdge> taskEdgeList = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, id));
            List<JobNodeVo> taskNodeVoList = new ArrayList<>();
            List<Long> taskIds = taskNodeList.stream().map(JobNode::getJobId).toList();
            List<JobInfo> taskInfos = this.listByIds(taskIds);
            Map<Long, String> taskMap = taskInfos.stream().collect(Collectors.toMap(JobInfo::getId, JobInfo::getJobDesc));
            for (JobNode node : taskNodeList) {
                JobNodeVo taskNodeVo = BeanUtil.copyProperties(node, JobNodeVo.class);
                taskNodeVo.setJobName(taskMap.get(node.getJobId()));
                taskNodeVoList.add(taskNodeVo);
            }
            taskInfoForm.setNodes(JSONUtil.toJsonStr(taskNodeVoList));
            taskInfoForm.setEdges(JSONUtil.toJsonStr(taskEdgeList));
        }
        JobNode jobNode = jobNodeService.getOne(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobId, id));
        if(jobNode != null) {
            taskInfoForm.setNodePositionX(jobNode.getNodePositionX());
            taskInfoForm.setNodePositionY(jobNode.getNodePositionY());
            // 返回节点ID供前端展示
            taskInfoForm.setNodeId(String.valueOf(jobNode.getId()));
        }
        // 补充运行时长（毫秒）
        if (entity != null) {
            taskInfoForm.setRunTime(entity.getRunTime());
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
    public long saveJobInfo(JobInfoForm formData) {
        JobInfo taskInfo = baseSaveJobInfo(formData);
        this.save(taskInfo);
        return taskInfo.getId();
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
    public boolean updateJobInfo(Long id, JobInfoForm formData) {
        // valid trigger
        JobInfo existsJobInfo = baseUpdateJobInfo(id, formData);

        if (StringUtils.isNotBlank(formData.getGlueRemark())) {
            //插入glueSource
            JobGlueForm glueForm = new JobGlueForm();
            glueForm.setTaskId(id);
            glueForm.setGlueSource(formData.getGlueSource());
            glueForm.setGlueType(formData.getGlueType());
            glueForm.setGlueRemark(formData.getGlueRemark());
            this.saveGlueSource(glueForm);
        }
        updateChild(existsJobInfo);
        JobNode node = jobNodeService.getOne(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobId, id));
        if(node!=null){
            //需要更新节点类型
            node.setNodeType(NODE_TYPE_MAP.get(formData.getGlueType()));
            jobNodeService.updateById(node);
        }
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
    public boolean deleteJobInfos(String ids) {
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

    @Override
    public void delNodes(Long jobId) {
        JobInfo taskInfo = this.getById(jobId);
        if (taskInfo!=null&&taskInfo.getJobType() == 2) {
            List<JobInfo> taskInfos = this.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobId));
            if (!taskInfos.isEmpty()) {
                List<Long> childTaskIds = taskInfos.stream().map(JobInfo::getId).toList();

                jobNodeService.remove(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, taskInfo.getId()));
                jobEdgeService.remove(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, taskInfo.getId()));

                for (Long childTaskId : childTaskIds) {
                    delNodes(childTaskId);
                }
            }
        }

        this.removeById(jobId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String triggerJob(JobInfoTriggerDto taskInfoTriggerDto) {
        Long jobId = taskInfoTriggerDto.getId();
        String randomId = taskInfoTriggerDto.getExecutorParam();

        // 【数据库行锁】使用 FOR UPDATE 查询，防止并发执行
        JobInfo taskInfo = jobInfoMapper.selectByIdForUpdate(jobId);
        if (taskInfo == null) {
            throw new BusinessException("任务不存在");
        }

        // 检查是否正在运行（使用行锁后，这里是线程安全的）
        if (taskInfo.getJobType() == 2 && taskInfo.getRankTriggerStatus() == 1) {
            throw new BusinessException("当前任务正在运行中，请等待完成后再运行");
        }

        // force cover job param
        if (taskInfoTriggerDto.getExecutorParam() == null) {
            taskInfoTriggerDto.setExecutorParam("");
            randomId = "";
        }

        if (GlueTypeEnum.DATAX.getDesc().equalsIgnoreCase(taskInfo.getGlueType())) {
            taskInfoTriggerDto.setExecutorParam(taskInfo.getExecutorParam());
            randomId = taskInfo.getExecutorParam();
        }

        // 【快照模式】如果是任务组，创建快照
        if (taskInfo.getJobType() == 2 && StringUtils.isNotBlank(randomId)) {
            try {
                String nodesJson = getNodesJsonForSnapshot(jobId);
                String edgesJson = getEdgesJsonForSnapshot(jobId);
                String triggerUserIdStr = taskInfoTriggerDto.getTriggerUserId() != null 
                    ? String.valueOf(taskInfoTriggerDto.getTriggerUserId()) 
                    : null;
                jobGroupSnapshotService.createSnapshot(
                    jobId,
                    randomId,
                    nodesJson,
                    edgesJson,
                    triggerUserIdStr
                );
            } catch (Exception e) {
                log.error("[Snapshot] 创建任务组快照失败 - jobId: {}, randomId: {}", jobId, randomId, e);
                throw new BusinessException("创建任务组快照失败: " + e.getMessage());
            }
        }

        // ⭐ 创建 JobLog 记录（在触发前创建，以便返回日志ID）
        JobLog jobLog = new JobLog();
        jobLog.setJobGroup(taskInfo.getJobGroup());
        jobLog.setJobId(taskInfo.getId());
        jobLog.setTriggerTime(LocalDateTime.now());
        jobLog.setTriggerCode(0);
        jobLog.setHandleCode(0);
        jobLogMapper.insert(jobLog);
        Long logId = jobLog.getId();
        
        log.debug("[JobInfoService] 创建任务日志记录 - jobId: {}, logId: {}", taskInfo.getId(), logId);

        String ip = IpUtil.getIp();
        String adminAddress = String.format(ADMIN_ADDRESS, ip, port);
        JobTriggerPoolHelper.trigger(taskInfoTriggerDto.getId().intValue(), TriggerTypeEnum.MANUAL, -1, null, taskInfoTriggerDto.getExecutorParam(), taskInfoTriggerDto.getAddressList(), 1, adminAddress);

        // 只在任务组（jobType == 2）时记录触发用户ID
        if (taskInfo.getJobType() == 2 && taskInfoTriggerDto.getTriggerUserId() != null) {
            taskInfo.setTriggerUserId(taskInfoTriggerDto.getTriggerUserId());
        }
        
        // 原子性设置运行状态（在事务中，行锁保护）
        taskInfo.setRankTriggerStatus(1);
        this.updateById(taskInfo);

        // 返回日志ID（字符串格式）
        return String.valueOf(logId);
    }


    @Override
    public boolean startJob(Long id) {
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
    public boolean stopJob(Long id) {
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

    /**
     * 旧的新增任务组方法，新方法在JobComposeService中
     *
     * @param formData
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveJobCompose(JobInfoForm formData) {
        JobInfo taskInfo = baseSaveJobInfo(formData);
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
            node.setJobParentId(taskInfo.getId());
            Map<String, Object> data = (Map<String, Object>) item.get("data");
            Map<String, Object> position = (Map<String, Object>) item.get("position");
            node.setNodePositionX(Double.valueOf(String.valueOf(position.get("x"))));
            node.setNodePositionY(Double.valueOf(String.valueOf(position.get("y"))));
            long taskId = Long.parseLong(String.valueOf(data.get("jobId")));
            node.setJobId(taskId);
            taskNodeDtoList.add(node);
        });

        edgeList.forEach(item -> {
            JobEdgeDto edge = new JobEdgeDto();
            edge.setJobParentId(taskInfo.getId());
            edge.setFromNodeId((String) item.get("source"));
            edge.setEndNodeId((String) item.get("target"));
            taskEdgeDtoList.add(edge);
        });

        //计算节点的出度和人度
        // 优化：预先构建边映射，避免在循环中重复遍历（O(n²) -> O(n)）
        Map<String, Long> outDegreeMap = new HashMap<>();
        Map<String, Long> inDegreeMap = new HashMap<>();
        
        for (JobEdgeDto edge : taskEdgeDtoList) {
            // 统计出度
            outDegreeMap.merge(edge.getFromNodeId(), 1L, Long::sum);
            // 统计入度
            inDegreeMap.merge(edge.getEndNodeId(), 1L, Long::sum);
        }
        
        // 批量设置节点的入度和出度
        for (JobNodeDto taskNodeDto : taskNodeDtoList) {
            taskNodeDto.setNodeOutDegree(outDegreeMap.getOrDefault(taskNodeDto.getId(), 0L));
            taskNodeDto.setNodeInDegree(inDegreeMap.getOrDefault(taskNodeDto.getId(), 0L));
        }

        addNode(taskNodeDtoList, taskEdgeDtoList, taskInfo);
        return true;
    }

    private void addNode(List<JobNodeDto> nodeList, List<JobEdgeDto> edgeList, JobInfo parentTask) {
        Map<String, Long> nodeMap = new HashMap<>();
        
        // 优化：批量查询所有需要的JobInfo，避免N+1查询问题
        List<Long> jobIds = nodeList.stream().map(JobNodeDto::getJobId).distinct().toList();
        if (jobIds.isEmpty()) {
            return;
        }
        List<JobInfo> jobInfos = this.listByIds(jobIds);
        Map<Long, JobInfo> jobInfoMap = jobInfos.stream()
            .collect(Collectors.toMap(JobInfo::getId, n -> n, (existing, replacement) -> existing));

        // 批量保存JobInfo和JobNode
        List<JobInfo> newJobInfos = new ArrayList<>();
        List<JobNode> newJobNodes = new ArrayList<>();
        // 用于记录每个taskNode对应的newJobInfo索引
        Map<JobNodeDto, Integer> nodeToJobInfoIndex = new HashMap<>();

        for (JobNodeDto taskNode : nodeList) {
            JobInfo taskInfo = jobInfoMap.get(taskNode.getJobId());
            if (taskInfo == null) {
                continue; // 跳过无效的节点
            }

            JobInfo copyTaskInfo = BeanUtil.copyProperties(taskInfo, JobInfo.class);
            copyTaskInfo.setId(null);
            copyTaskInfo.setIsNode("Y");
            copyTaskInfo.setParentId(parentTask.getId());
            newJobInfos.add(copyTaskInfo);
            nodeToJobInfoIndex.put(taskNode, newJobInfos.size() - 1);
        }
        
        // 批量保存JobInfo（MyBatis-Plus会自动回填ID）
        this.saveBatch(newJobInfos);
        
        // 处理嵌套的任务组和创建JobNode
        for (JobNodeDto taskNode : nodeList) {
            JobInfo taskInfo = jobInfoMap.get(taskNode.getJobId());
            if (taskInfo == null) {
                continue;
            }
            
            // 获取对应的新JobInfo
            Integer index = nodeToJobInfoIndex.get(taskNode);
            if (index == null || index >= newJobInfos.size()) {
                continue;
            }
            JobInfo copyTaskInfo = newJobInfos.get(index);

            if (taskInfo.getJobType() == 2) {
                List<JobNode> childNodes = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, taskInfo.getId()));
                List<JobEdge> childEdges = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, taskInfo.getId()));

                List<JobNodeDto> taskNodeDtos = BeanUtil.copyToList(childNodes, JobNodeDto.class, CopyOptions.create());
                List<JobEdgeDto> taskEdgeDtos = BeanUtil.copyToList(childEdges, JobEdgeDto.class, CopyOptions.create());
                addNode(taskNodeDtos, taskEdgeDtos, copyTaskInfo);
                copyTaskInfo.setExecutorParam(String.valueOf(copyTaskInfo.getId()));
                this.updateById(copyTaskInfo);
            }

            JobNode copyTaskNode = BeanUtil.copyProperties(taskNode, JobNode.class, "id");
            copyTaskNode.setId(null);
            copyTaskNode.setJobId(copyTaskInfo.getId());
            copyTaskNode.setJobParentId(parentTask.getId());
            newJobNodes.add(copyTaskNode);
        }
        
        // 批量保存JobNode（MyBatis-Plus会自动回填ID）
        jobNodeService.saveBatch(newJobNodes);
        
        // 更新nodeMap，使用保存后的ID
        int nodeIndex = 0;
        for (JobNodeDto taskNode : nodeList) {
            if (nodeIndex < newJobNodes.size()) {
                JobNode savedNode = newJobNodes.get(nodeIndex);
                nodeMap.put(String.valueOf(taskNode.getId()), savedNode.getId());
                nodeIndex++;
            }
        }

        // 批量保存JobEdge
        List<JobEdge> newEdges = new ArrayList<>();
        for (JobEdgeDto taskEdge : edgeList) {
            Long fromNodeId = nodeMap.get(String.valueOf(taskEdge.getFromNodeId()));
            Long endNodeId = nodeMap.get(String.valueOf(taskEdge.getEndNodeId()));
            if (fromNodeId != null && endNodeId != null) {
            JobEdge edge = new JobEdge();
            edge.setJobParentId(parentTask.getId());
                edge.setFromNodeId(fromNodeId);
                edge.setEndNodeId(endNodeId);
                newEdges.add(edge);
            }
        }
        if (!newEdges.isEmpty()) {
            jobEdgeService.saveBatch(newEdges);
        }
    }


    @Override
    public JobInfo baseSaveJobInfo(JobInfoForm formData) {
        JobGroup taskGroup = jobGroupService.getById(formData.getJobGroup());
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
            List<Integer> validJobIds = new ArrayList<>();
            
            // 优化：批量查询所有子任务，避免N+1查询问题
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    validJobIds.add(Integer.parseInt(childJobIdItem));
                } else {
                    throw new BusinessException(
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // 批量查询所有子任务
            if (!validJobIds.isEmpty()) {
                List<JobInfo> childJobInfos = this.listByIds(validJobIds.stream().map(Long::valueOf).toList());
                Map<Long, JobInfo> childJobInfoMap = childJobInfos.stream()
                    .collect(Collectors.toMap(JobInfo::getId, n -> n));
                
                // 验证所有子任务是否存在
                for (Integer jobId : validJobIds) {
                    if (!childJobInfoMap.containsKey(Long.valueOf(jobId))) {
                        throw new BusinessException(MessageFormat.format(
                            (I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), 
                            String.valueOf(jobId)));
            }
                }
            }

            // 优化：使用StringBuilder替代字符串拼接
            StringBuilder temp = new StringBuilder();
            for (int i = 0; i < childJobIds.length; i++) {
                if (i > 0) {
                    temp.append(",");
                }
                temp.append(childJobIds[i]);
            }

            formData.setChildJobid(temp.toString());
        }
        formData.setGlueUpdatetime(LocalDateTime.now());
        JobInfo taskInfo = BeanUtil.copyProperties(formData, JobInfo.class);
        taskInfo.setIsNode("N");
        return taskInfo;
    }

    /**
     * 旧的修改任务组方法，新方法在JobComposeService中
     *
     * @param id
     * @param formData
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateJobCompose(Long id, JobInfoForm formData) {
        // valid trigger
        JobInfo existsJobInfo = baseUpdateJobInfo(id, formData);
        if (StringUtils.isBlank(formData.getNodes())) {
            throw new BusinessException("任务节点不能为空");
        }
        this.updateById(existsJobInfo);
        // 更新节点
        List<Map> nodeList = JSONUtil.parseArray(formData.getNodes()).toList(Map.class);
        List<Map> edgeList = JSONUtil.parseArray(formData.getEdges()).toList(Map.class);

        // 得到数据库中的节点
        List<JobNode> nodeFromDbList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, id));

        List<JobNode> taskUpdateNodeList = new ArrayList<>();
        List<JobEdge> taskAddEdgeList = new ArrayList<>();

        Map<String, Long> nodeMap = new HashMap<>();

        nodeList.forEach(item -> {
            JobNode node = new JobNode();
            String nodeId = String.valueOf(item.get("id"));
            node.setJobParentId(id);
            Map<String, Object> position = (Map<String, Object>) item.get("position");
            node.setNodePositionX(Double.valueOf(String.valueOf(position.get("x"))));
            node.setNodePositionY(Double.valueOf(String.valueOf(position.get("y"))));
            Map<String, Object> data = (Map<String, Object>) item.get("data");
            //node.setJobId(Long.parseLong(String.valueOf(data.get("taskId"))));
            long taskId = Long.parseLong(String.valueOf(data.get("jobId")));
            if (nodeId.startsWith("node:")) {
                JobInfo copyTaskInfo = this.getById(taskId);
                copyTaskInfo.setId(null);
                copyTaskInfo.setParentId(id);
                copyTaskInfo.setIsNode("Y");
                this.save(copyTaskInfo);
                node.setJobId(copyTaskInfo.getId());
                // 修复：新创建的节点，triggerStatus设置为-1表示未运行状态（白色背景）
                node.setTriggerStatus(-1);

                if (copyTaskInfo.getJobType() == 2) {
                    List<JobNode> taskNodeList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, taskId));
                    List<JobEdge> taskEdgeList = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, taskId));
                    List<JobNodeDto> taskNodeDtos = BeanUtil.copyToList(taskNodeList, JobNodeDto.class);
                    List<JobEdgeDto> taskEdgeDtos = BeanUtil.copyToList(taskEdgeList, JobEdgeDto.class);
                    addNode(taskNodeDtos, taskEdgeDtos, copyTaskInfo);
                    copyTaskInfo.setExecutorParam(String.valueOf(copyTaskInfo.getId()));
                    this.updateById(copyTaskInfo);
                }

                jobNodeService.save(node);
                nodeMap.put(nodeId, node.getId());
            } else {
                node.setJobId(taskId);
                node.setId(Long.parseLong(nodeId));
                taskUpdateNodeList.add(node);
            }
        });

        edgeList.forEach(item -> {
            JobEdge edge = new JobEdge();
            edge.setJobParentId(id);
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
        jobEdgeService.remove(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, id));
        jobEdgeService.saveBatch(taskAddEdgeList);

        List<Long> nodeIds = taskUpdateNodeList.stream().map(JobNode::getId).toList();
        List<JobNode> delNodeDbs = nodeFromDbList.stream().filter(v -> !nodeIds.contains(v.getId())).toList();
        if (!delNodeDbs.isEmpty()) {
            for (JobNode delNodeDb : delNodeDbs) {
                JobInfo delTaskInfo = this.getById(delNodeDb.getJobId());
                if (delTaskInfo.getJobType() == 2) {
                    delNodes(delTaskInfo.getId());
                }
            }
            this.removeBatchByIds(delNodeDbs.stream().map(JobNode::getJobId).toList());
            jobNodeService.removeBatchByIds(delNodeDbs.stream().map(JobNode::getId).toList());
        }
        jobNodeService.updateBatchById(taskUpdateNodeList);

        // 处理边
        List<JobNode> nodeFromDbList2 = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, id));
        
        // 优化：预先构建边映射，避免在forEach中重复遍历（O(n²) -> O(n)）
        Map<Long, Long> inDegreeMap = new HashMap<>();
        Map<Long, Long> outDegreeMap = new HashMap<>();
        
        for (JobEdge edge : taskAddEdgeList) {
            // 统计入度
            inDegreeMap.merge(edge.getEndNodeId(), 1L, Long::sum);
            // 统计出度
            outDegreeMap.merge(edge.getFromNodeId(), 1L, Long::sum);
        }
        
        // 批量设置节点的入度和出度
        for (JobNode item : nodeFromDbList2) {
            item.setNodeInDegree(inDegreeMap.getOrDefault(item.getId(), 0L));
            item.setNodeOutDegree(outDegreeMap.getOrDefault(item.getId(), 0L));
        }
        
        return jobNodeService.updateBatchById(nodeFromDbList2);
    }

    @Override
    public boolean stopJobCompose(Long id, String randomId) {
        try {
            // 1. 更新任务状态
            int flag = jobInfoMapper.stopJobCompose(id);
            
            // 2. 获取任务信息和执行器组信息
            JobInfo jobInfo = this.getById(id);
            if (jobInfo == null) {
                log.error("停止任务组失败 - 任务不存在: {}", id);
                return false;
            }
            
            JobGroup jobGroup = jobGroupService.getById(jobInfo.getJobGroup());
            if (jobGroup == null) {
                log.error("停止任务组失败 - 执行器组不存在: {}", jobInfo.getJobGroup());
                return false;
            }
            
            // 3. ⭐ 异步调用executor-compose的停止接口（不阻塞主线程）
            String addressList = jobGroup.getAddressList();
            if (StringUtils.isNotBlank(addressList)) {
                String[] addresses = addressList.split(",");
                final Long finalJobId = id;
                final String finalRandomId = randomId;
                
                // 异步调用所有执行器的停止接口
                for (String address : addresses) {
                    if (StringUtils.isBlank(address)) {
                        continue;
                    }
                    
                    final String finalAddress = address.trim();
                    // 异步执行，不等待结果
                    STOP_JOB_EXECUTOR.submit(() -> {
                        try {
                            String url = finalAddress + "/api/jobgroup/stop";
                            HttpResponse response = cn.hutool.http.HttpRequest.post(url)
                                    .form("jobId", finalJobId)
                                    .form("randomId", finalRandomId)
                                    .timeout(5000)
                                    .execute();
                            
                            if (response.isOk()) {
                                log.info("成功调用停止接口 - jobId: {}, randomId: {}, address: {}", 
                                        finalJobId, finalRandomId, finalAddress);
                            } else {
                                log.error("调用停止接口失败 - jobId: {}, randomId: {}, address: {}, status: {}", 
                                        finalJobId, finalRandomId, finalAddress, response.getStatus());
                            }
                        } catch (Exception e) {
                            log.error("调用停止接口异常 - jobId: {}, randomId: {}, address: {}", 
                                    finalJobId, finalRandomId, finalAddress, e);
                        }
                    });
                }
            }
            
            // ⭐ 立即返回，不等待 HTTP 调用完成
            return flag > 0;
        } catch (Exception e) {
            log.error("停止任务组异常 - jobId: {}, randomId: {}", id, randomId, e);
            return false;
        }
    }

    @Override
    public boolean saveGlueSource(JobGlueForm formData) {
        JobInfo taskInfo = this.getById(formData.getTaskId());
        taskInfo.setGlueRemark(formData.getGlueRemark());
        taskInfo.setGlueSource(formData.getGlueSource());
        taskInfo.setGlueUpdatetime(LocalDateTime.now());
        // 如果传入了glueType，则更新taskInfo的glueType
        if (formData.getGlueType() != null && !formData.getGlueType().isEmpty()) {
            taskInfo.setGlueType(formData.getGlueType());
        }
        this.updateById(taskInfo);
        JobLogglue taskLogglue = new JobLogglue();
        taskLogglue.setGlueSource(formData.getGlueSource());
        taskLogglue.setGlueRemark(formData.getGlueRemark());
        taskLogglue.setJobId(formData.getTaskId());
        // 优先使用传入的glueType，如果没有则使用taskInfo中的glueType
        String glueType = formData.getGlueType();
        if (glueType == null || glueType.isEmpty()) {
            glueType = taskInfo.getGlueType();
        }
        taskLogglue.setGlueType(glueType);
        jobLogglueMapper.insert(taskLogglue);
        return true;
    }

    @Override
    public List<JobLogglue> getGlueList(Long id) {
        return jobLogglueMapper.selectList(new LambdaQueryWrapper<JobLogglue>().eq(JobLogglue::getJobId, id));
    }
    
    @Override
    public List<JobLogglue> getGlueList(Long id, String glueType) {
        LambdaQueryWrapper<JobLogglue> wrapper = new LambdaQueryWrapper<JobLogglue>()
                .eq(JobLogglue::getJobId, id)
                .orderByDesc(JobLogglue::getCreateTime); // 按创建时间倒序排列
        
        // 如果指定了GLUE类型，则按类型过滤
        if (glueType != null && !glueType.trim().isEmpty()) {
            wrapper.eq(JobLogglue::getGlueType, glueType);
        }
        
        return jobLogglueMapper.selectList(wrapper);
    }

    @Override
    public List<Long> initData() {
        //获取初始化的数据
        long triggerIng = this.count(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getTriggerStatus, 1));
        
        // 优化：使用数据库查询替代内存过滤，避免查询所有日志
        long successCount = jobLogMapper.selectCount(
            new LambdaQueryWrapper<JobLog>().eq(JobLog::getHandleCode, ReturnT.SUCCESS_CODE)
        );
        long failCount = jobLogMapper.selectCount(
            new LambdaQueryWrapper<JobLog>().eq(JobLog::getHandleCode, ReturnT.FAIL_CODE)
        );
        
        return List.of(successCount, failCount, triggerIng);
    }

    @Override
    public boolean pauseJob(Long id, Integer isPause) {
        int i = jobInfoMapper.pauseJob(id, isPause);
        return i > 0;
    }

    @Override
    public JobInfo baseUpdateJobInfo(Long id, JobInfoForm formData) {
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
        JobGroup jobGroup = jobGroupService.getById(formData.getJobGroup());
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
        boolean scheduleDataNotChanged = formData.getScheduleType().equals(existsJobInfo.getScheduleType()) && existsJobInfo.getScheduleConf() != null && formData.getScheduleConf().equals(existsJobInfo.getScheduleConf());
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
        return existsJobInfo;
    }

    /**
     * 获取任务组的节点JSON（用于快照）
     *
     * @param jobId 任务组ID
     * @return 节点JSON字符串
     */
    private String getNodesJsonForSnapshot(Long jobId) {
        List<JobNode> nodes = jobNodeService.list(
            new LambdaQueryWrapper<JobNode>()
                .eq(JobNode::getJobParentId, jobId)
                .eq(JobNode::getIsDeleted, 0)
        );
        return JSONUtil.toJsonStr(nodes);
    }

    /**
     * 获取任务组的边JSON（用于快照）
     *
     * @param jobId 任务组ID
     * @return 边JSON字符串
     */
    private String getEdgesJsonForSnapshot(Long jobId) {
        List<JobEdge> edges = jobEdgeService.list(
            new LambdaQueryWrapper<JobEdge>()
                .eq(JobEdge::getJobParentId, jobId)
                .eq(JobEdge::getIsDeleted, 0)
        );
        return JSONUtil.toJsonStr(edges);
    }
}
