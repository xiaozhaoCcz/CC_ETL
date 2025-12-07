package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.service.JobPartService;
import com.cc.job.xo.mapper.JobPartMapper;
import com.cc.job.xo.model.dto.PartitionExportData;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.entity.JobPart;
import com.cc.job.xo.model.vo.JobPartVo;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xiaozhao
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobPartServiceImpl extends ServiceImpl<JobPartMapper, JobPart> implements JobPartService {

    private final JobInfoService jobInfoService;

    private final JobNodeService jobNodeService;

    private final JobEdgeService jobEdgeService;

    @Override
    public List<JobPartVo> getTree() {
        List<JobPartVo> jobPartVos = new ArrayList<>();
        // 获取所有的分区
        List<JobPart> jobPartList = this.list();

        List<JobInfo> jobInfoList = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getJobType, 2).eq(JobInfo::getNodeFlag, "N"));

        Map<Integer, List<JobInfo>> jobInfoMap = jobInfoList.stream().collect(Collectors.groupingBy(JobInfo::getJobPartId));

        List<Long> jobInfoIds = jobInfoList.stream().map(JobInfo::getId).toList();

        List<JobNode> jobNodeList = new ArrayList<>();
        List<JobEdge> jobEdgeList = new ArrayList<>();
        Map<Long, String> jobNodeDescMap = new HashMap<>();
        if(!jobInfoIds.isEmpty()){
            jobNodeList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().in(JobNode::getJobParentId, jobInfoIds));
            jobEdgeList = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().in(JobEdge::getJobParentId, jobInfoIds));
            Map<Long, Long> jobIdToJobNodeIdMap = jobNodeList.stream().collect(Collectors.toMap(JobNode::getJobId, JobNode::getId));
            if(!jobIdToJobNodeIdMap.isEmpty()){
                List<JobInfo> jobInfoList2 = jobInfoService.listByIds(jobIdToJobNodeIdMap.keySet());
                Map<Long, String> jobInfoDescMap = jobInfoList2.stream().collect(Collectors.toMap(JobInfo::getId, JobInfo::getJobDesc));
                jobInfoDescMap.forEach((k, v) -> jobNodeDescMap.put(jobIdToJobNodeIdMap.get(k), v));
            }
        }

        Map<Long, List<JobNode>> jobNodeMap = jobNodeList.stream().collect(Collectors.groupingBy(JobNode::getJobParentId));
        Map<Long, List<JobEdge>> jobEdgeMap = jobEdgeList.stream().collect(Collectors.groupingBy(JobEdge::getJobParentId));

        for (JobPart jobPart : jobPartList) {
            JobPartVo jobPartVo = new JobPartVo();
            jobPartVo.setId(jobPart.getId());
            jobPartVo.setLabel(jobPart.getJobPartName().trim());
            jobPartVo.setType(0);
            List<JobInfo> jobInfoList1 = jobInfoMap.get(Integer.parseInt(String.valueOf(jobPart.getId())));
            List<JobPartVo> childrenJobPartVo = new ArrayList<>();
            if (jobInfoList1 != null) {
                //找到子节点
                for (JobInfo jobInfo : jobInfoList1) {
                    JobPartVo jobPartVo1 = new JobPartVo();
                    jobPartVo1.setId(jobInfo.getId());
                    jobPartVo1.setLabel(jobInfo.getJobDesc().trim());
                    jobPartVo1.setType(1);

                    List<JobPartVo> childrenJobPartVo1 = new ArrayList<>();
                    //找到对应的节点和边
                    JobPartVo jobPartVoNode = new JobPartVo();
                    jobPartVoNode.setId(Long.parseLong(jobInfo.getId()+"01"));
                    jobPartVoNode.setLabel("任务");
                    jobPartVoNode.setType(2);

                    List<JobNode> jobNodes = jobNodeMap.get(jobInfo.getId());
                    List<JobPartVo> childrenJobPartVo2 = new ArrayList<>();
                    if (jobNodes != null) {
                        for (JobNode jobNode : jobNodes) {
                            JobPartVo jobPartVoNode1 = new JobPartVo();
                            jobPartVoNode1.setId(jobNode.getId());
                            jobPartVoNode1.setLabel(jobNodeDescMap.get(jobNode.getId()).trim());
                            jobPartVoNode1.setType(4);
                            jobPartVoNode1.setExt1(String.valueOf(jobNode.getJobId()));
                            childrenJobPartVo2.add(jobPartVoNode1);
                        }
                    }
                    jobPartVoNode.setChildren(childrenJobPartVo2);


                    JobPartVo jobPartVoEdge = new JobPartVo();
                    jobPartVoEdge.setId(Long.parseLong(jobInfo.getId()+"02"));
                    jobPartVoEdge.setLabel("关系");
                    jobPartVoEdge.setType(3);

                    List<JobEdge> jobEdges = jobEdgeMap.get(jobInfo.getId());
                    List<JobPartVo> childrenJobPartVo3 = new ArrayList<>();
                    if(jobEdges != null) {
                        for (JobEdge jobEdge : jobEdges) {
                            JobPartVo jobPartVoEdge1 = new JobPartVo();
                            jobPartVoEdge1.setId(jobEdge.getId());
                            jobPartVoEdge1.setLabel((jobNodeDescMap.get(jobEdge.getFromNodeId()) + "➡" + jobNodeDescMap.get(jobEdge.getEndNodeId())).trim());
                            jobPartVoEdge1.setType(5);
                            childrenJobPartVo3.add(jobPartVoEdge1);
                        }
                    }
                    jobPartVoEdge.setChildren(childrenJobPartVo3);

                    childrenJobPartVo1.add(jobPartVoNode);
                    childrenJobPartVo1.add(jobPartVoEdge);
                    jobPartVo1.setChildren(childrenJobPartVo1);

                    childrenJobPartVo.add(jobPartVo1);
                }
            }
            jobPartVo.setChildren(childrenJobPartVo);
            jobPartVos.add(jobPartVo);
        }
        return jobPartVos;
    }

    @Override
    public Object getChildren(Long id, Integer type) {
        if(type==0){
            List<JobPartVo> list = new ArrayList<>();
            List<JobInfo> jobInfoList = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getJobPartId, id));
            for (JobInfo jobInfo : jobInfoList) {
                JobPartVo jobPartVo = new JobPartVo();
                jobPartVo.setId(jobInfo.getId());
                jobPartVo.setLabel(jobInfo.getJobDesc().trim());
                jobPartVo.setType(1);
                list.add(jobPartVo);
                return list;
            }
        }else if(type==1){
            Map<String,Object> resMap = new HashMap<>();
            List<JobNode> jobNodeList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, id));
            List<JobEdge> jobEdgeList = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, id));
            Map<Long, Long> jobIdToJobNodeIdMap = jobNodeList.stream().collect(Collectors.toMap(JobNode::getJobId, JobNode::getId));
            List<JobInfo> jobInfoList = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, id));
            Map<Long, String> jobInfoDescMap = jobInfoList.stream().collect(Collectors.toMap(JobInfo::getId, JobInfo::getJobDesc));
            Map<Long,String>  jobNodeDescMap = new HashMap<>();
            jobInfoDescMap.forEach((k, v) -> jobNodeDescMap.put(jobIdToJobNodeIdMap.get(k), v));

            List<JobPartVo> childrenJobPartVo2 = new ArrayList<>();
            for (JobNode jobNode : jobNodeList) {
                JobPartVo jobPartVoNode1 = new JobPartVo();
                jobPartVoNode1.setId(jobNode.getId());
                jobPartVoNode1.setLabel(jobNodeDescMap.get(jobNode.getId()).trim());
                jobPartVoNode1.setType(4);
                childrenJobPartVo2.add(jobPartVoNode1);
            }


            List<JobPartVo> childrenJobPartVo3 = new ArrayList<>();
            for (JobEdge jobEdge : jobEdgeList) {
                JobPartVo jobPartVoEdge1 = new JobPartVo();
                jobPartVoEdge1.setId(jobEdge.getId());
                jobPartVoEdge1.setLabel((jobNodeDescMap.get(jobEdge.getFromNodeId())+"->"+jobNodeDescMap.get(jobEdge.getEndNodeId())).trim());
                jobPartVoEdge1.setType(5);
                childrenJobPartVo3.add(jobPartVoEdge1);
            }

            resMap.put("nodes", childrenJobPartVo2);
            resMap.put("edges", childrenJobPartVo3);

            return resMap;
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        //删除任务分区同时要删除下面的所有子任务组
        List<JobInfo> jobInfoList = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getJobPartId, id));
        List<Long> ids = jobInfoList.stream().map(JobInfo::getId).toList();
        // delete jobGroup
        for (Long jobId : ids) {
            jobInfoService.delNodes(jobId);
        }
        this.removeById(id);
    }

    private static final String SECRET_KEY = "1234567890abcdef"; // 256-bit key
    private static final String ALGORITHM = "AES";

    /**
     * 导出分区数据
     * 导出分区下的所有任务组、任务节点和边的关系
     */
    @Override
    public byte[] exportData(Long id) {
        // 1. 获取分区信息
        JobPart jobPart = this.getById(id);
        if (jobPart == null) {
            throw new RuntimeException("分区不存在: " + id);
        }

        // 2. 构建导出数据结构
        PartitionExportData exportData = new PartitionExportData();
        
        // 2.1 设置分区信息
        PartitionExportData.PartitionInfo partitionInfo = 
            new PartitionExportData.PartitionInfo();
        partitionInfo.setId(jobPart.getId());
        partitionInfo.setJobPartName(jobPart.getJobPartName());
        partitionInfo.setSort(jobPart.getSort());
        exportData.setPartition(partitionInfo);

        // 2.2 获取分区下的所有任务组（jobType=2, isNode="N"）
        List<JobInfo> taskGroupList = jobInfoService.list(
            new LambdaQueryWrapper<JobInfo>()
                .eq(JobInfo::getJobPartId, id)
                .eq(JobInfo::getJobType, 2)
                .eq(JobInfo::getNodeFlag, "N")
        );

        List<PartitionExportData.TaskGroupInfo> taskGroupInfoList = new ArrayList<>();

        // 2.3 遍历每个任务组，获取其节点和边
        for (JobInfo taskGroup : taskGroupList) {
            PartitionExportData.TaskGroupInfo taskGroupInfo = 
                new PartitionExportData.TaskGroupInfo();

            // 2.3.1 设置任务组基本信息
            PartitionExportData.TaskInfoData taskGroupData = 
                convertToTaskInfoData(taskGroup);
            taskGroupInfo.setTaskGroupData(taskGroupData);

            // 2.3.2 获取任务组下的所有节点（JobNode）
            List<JobNode> jobNodeList = jobNodeService.list(
                new LambdaQueryWrapper<JobNode>()
                    .eq(JobNode::getJobParentId, taskGroup.getId())
            );

            List<PartitionExportData.NodeInfo> nodeInfoList = new ArrayList<>();
            for (JobNode jobNode : jobNodeList) {
                PartitionExportData.NodeInfo nodeInfo = 
                    new PartitionExportData.NodeInfo();
                
                // 设置JobNode信息
                nodeInfo.setNodeId(jobNode.getId());
                nodeInfo.setJobId(jobNode.getJobId());
                nodeInfo.setJobParentId(jobNode.getJobParentId());
                nodeInfo.setNodePositionX(jobNode.getNodePositionX());
                nodeInfo.setNodePositionY(jobNode.getNodePositionY());
                nodeInfo.setNodeInDegree(jobNode.getNodeInDegree());
                nodeInfo.setNodeOutDegree(jobNode.getNodeOutDegree());
                nodeInfo.setSort(jobNode.getSort());
                nodeInfo.setChildren(jobNode.getChildren());
                nodeInfo.setProperties(jobNode.getProperties());
                nodeInfo.setNodeType(jobNode.getNodeType());
                nodeInfo.setTriggerStatus(jobNode.getTriggerStatus());

                // 获取节点对应的JobInfo信息
                JobInfo nodeJobInfo = jobInfoService.getById(jobNode.getJobId());
                if (nodeJobInfo != null) {
                    nodeInfo.setTaskInfo(convertToTaskInfoData(nodeJobInfo));
                }

                nodeInfoList.add(nodeInfo);
            }
            taskGroupInfo.setNodes(nodeInfoList);

            // 2.3.3 获取任务组下的所有边（JobEdge）
            List<JobEdge> jobEdgeList = jobEdgeService.list(
                new LambdaQueryWrapper<JobEdge>()
                    .eq(JobEdge::getJobParentId, taskGroup.getId())
            );

            List<PartitionExportData.EdgeInfo> edgeInfoList = new ArrayList<>();
            for (JobEdge jobEdge : jobEdgeList) {
                PartitionExportData.EdgeInfo edgeInfo = 
                    new PartitionExportData.EdgeInfo();
                edgeInfo.setId(jobEdge.getId());
                edgeInfo.setJobParentId(jobEdge.getJobParentId());
                edgeInfo.setFromNodeId(jobEdge.getFromNodeId());
                edgeInfo.setEndNodeId(jobEdge.getEndNodeId());
                edgeInfo.setPointsList(jobEdge.getPointsList());
                edgeInfo.setProperties(jobEdge.getProperties());
                edgeInfo.setStartPoint(jobEdge.getStartPoint());
                edgeInfo.setEndPoint(jobEdge.getEndPoint());
                edgeInfoList.add(edgeInfo);
            }
            taskGroupInfo.setEdges(edgeInfoList);

            taskGroupInfoList.add(taskGroupInfo);
        }

        exportData.setTaskGroups(taskGroupInfoList);

        // 3. 序列化为JSON并加密
        Gson gson = new Gson();
        String json = gson.toJson(exportData);
        
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(json.getBytes());
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
        return encryptedBytes;
    }

    /**
     * 将JobInfo转换为TaskInfoData
     */
    private PartitionExportData.TaskInfoData convertToTaskInfoData(JobInfo jobInfo) {
        PartitionExportData.TaskInfoData taskInfoData = 
            new PartitionExportData.TaskInfoData();
        taskInfoData.setId(jobInfo.getId());
        taskInfoData.setJobGroup(jobInfo.getJobGroup());
        taskInfoData.setJobDesc(jobInfo.getJobDesc());
        taskInfoData.setAuthor(jobInfo.getAuthor());
        taskInfoData.setAlarmEmail(jobInfo.getAlarmEmail());
        taskInfoData.setScheduleType(jobInfo.getScheduleType());
        taskInfoData.setScheduleConf(jobInfo.getScheduleConf());
        taskInfoData.setMisfireStrategy(jobInfo.getMisfireStrategy());
        taskInfoData.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        taskInfoData.setExecutorHandler(jobInfo.getExecutorHandler());
        taskInfoData.setExecutorParam(jobInfo.getExecutorParam());
        taskInfoData.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        taskInfoData.setExecutorTimeout(jobInfo.getExecutorTimeout());
        taskInfoData.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        taskInfoData.setGlueType(jobInfo.getGlueType());
        taskInfoData.setGlueSource(jobInfo.getGlueSource());
        taskInfoData.setGlueRemark(jobInfo.getGlueRemark());
        taskInfoData.setChildJobId(jobInfo.getChildJobId());
        taskInfoData.setTriggerStatus(jobInfo.getTriggerStatus());
        taskInfoData.setTriggerLastTime(jobInfo.getTriggerLastTime());
        taskInfoData.setTriggerNextTime(jobInfo.getTriggerNextTime());
        taskInfoData.setJobType(jobInfo.getJobType());
        taskInfoData.setParentId(jobInfo.getParentId());
        taskInfoData.setReqType(jobInfo.getReqType());
        taskInfoData.setReqHeader(jobInfo.getReqHeader());
        taskInfoData.setReqBody(jobInfo.getReqBody());
        taskInfoData.setReqUrl(jobInfo.getReqUrl());
        taskInfoData.setNodeFlag(jobInfo.getNodeFlag());
        taskInfoData.setJdbcDatasourceId(jobInfo.getJdbcDatasourceId());
        taskInfoData.setIncrementType(jobInfo.getIncrementType());
        taskInfoData.setIncrementContent(jobInfo.getIncrementContent());
        taskInfoData.setRunTime(jobInfo.getRunTime());
        taskInfoData.setPauseStatus(jobInfo.getPauseStatus());
        taskInfoData.setJobPartId(jobInfo.getJobPartId());
        taskInfoData.setTriggerUserId(jobInfo.getTriggerUserId());
        return taskInfoData;
    }

    /**
     * 导入分区数据
     * 从文件中读取数据并重新创建所有节点和边（不依赖已有数据库）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importData(MultipartFile file) {
        // 1. 解密文件
        Cipher cipher = null;
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        byte[] decryptedBytes = null;
        String json = null;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            decryptedBytes = cipher.doFinal(file.getBytes());
            json = new String(decryptedBytes, "UTF-8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException | IOException e) {
            throw new RuntimeException("解密文件失败", e);
        }

        // 2. 解析JSON数据
        Gson gson = new Gson();
        PartitionExportData exportData = 
            gson.fromJson(json, PartitionExportData.class);
        
        if (exportData == null || exportData.getPartition() == null) {
            throw new RuntimeException("导入数据格式错误");
        }

        // 3. 创建新分区（不依赖原有数据）
        JobPart newPartition = new JobPart();
        newPartition.setJobPartName(exportData.getPartition().getJobPartName());
        newPartition.setSort(exportData.getPartition().getSort());
        this.save(newPartition);
        Long newPartitionId = newPartition.getId();

        // 4. ID映射表：原ID -> 新ID
        Map<Long, Long> oldToNewJobInfoIdMap = new HashMap<>(); // JobInfo ID映射
        Map<Long, Long> oldToNewNodeIdMap = new HashMap<>(); // JobNode ID映射
        Map<Long, Long> oldToNewEdgeIdMap = new HashMap<>(); // JobEdge ID映射

        // 5. 遍历所有任务组，重新创建
        if (exportData.getTaskGroups() != null) {
            for (PartitionExportData.TaskGroupInfo taskGroupInfo : exportData.getTaskGroups()) {
                // 5.1 创建任务组（JobInfo，jobType=2, isNode="N"）
                JobInfo newTaskGroup = convertToJobInfo(taskGroupInfo.getTaskGroupData());
                newTaskGroup.setId(null); // 清除ID，让数据库自动生成
                newTaskGroup.setJobPartId(Integer.parseInt(String.valueOf(newPartitionId)));
                newTaskGroup.setJobType(2);
                newTaskGroup.setNodeFlag("N");
                newTaskGroup.setTriggerStatus(0); // 默认停止状态
                jobInfoService.save(newTaskGroup);
                
                // 获取新生成的任务组ID
                Long newTaskGroupId = newTaskGroup.getId();
                if (newTaskGroupId == null) {
                    throw new RuntimeException("创建任务组失败：未生成ID");
                }
                
                // 更新executorParam为新的ID
                newTaskGroup.setExecutorParam(String.valueOf(newTaskGroupId));
                jobInfoService.updateById(newTaskGroup);
                
                Long oldTaskGroupId = taskGroupInfo.getTaskGroupData().getId();
                oldToNewJobInfoIdMap.put(oldTaskGroupId, newTaskGroupId);

                // 5.2 创建任务组下的所有节点
                if (taskGroupInfo.getNodes() != null) {
                    for (PartitionExportData.NodeInfo nodeInfo : taskGroupInfo.getNodes()) {
                        // 5.2.1 创建节点对应的JobInfo
                        JobInfo newNodeJobInfo = convertToJobInfo(nodeInfo.getTaskInfo());
                        newNodeJobInfo.setId(null);
                        newNodeJobInfo.setParentId(newTaskGroupId); // 设置父任务组ID
                        newNodeJobInfo.setTriggerStatus(0); // 默认停止状态
                        jobInfoService.save(newNodeJobInfo);
                        
                        Long newJobInfoId = newNodeJobInfo.getId();
                        Long oldJobInfoId = nodeInfo.getTaskInfo().getId();
                        oldToNewJobInfoIdMap.put(oldJobInfoId, newJobInfoId);

                        // 5.2.2 创建JobNode
                        JobNode newJobNode = new JobNode();
                        newJobNode.setJobId(newJobInfoId);
                        newJobNode.setJobParentId(newTaskGroupId);
                        newJobNode.setNodePositionX(nodeInfo.getNodePositionX());
                        newJobNode.setNodePositionY(nodeInfo.getNodePositionY());
                        newJobNode.setNodeInDegree(nodeInfo.getNodeInDegree());
                        newJobNode.setNodeOutDegree(nodeInfo.getNodeOutDegree());
                        newJobNode.setSort(nodeInfo.getSort());
                        newJobNode.setChildren(nodeInfo.getChildren());
                        newJobNode.setProperties(nodeInfo.getProperties());
                        newJobNode.setNodeType(nodeInfo.getNodeType());
                        newJobNode.setTriggerStatus(nodeInfo.getTriggerStatus());
                        jobNodeService.save(newJobNode);

                        Long newNodeId = newJobNode.getId();
                        Long oldNodeId = nodeInfo.getNodeId();
                        oldToNewNodeIdMap.put(oldNodeId, newNodeId);
                    }
                }

                // 5.3 创建任务组下的所有边
                if (taskGroupInfo.getEdges() != null) {
                    for (PartitionExportData.EdgeInfo edgeInfo : taskGroupInfo.getEdges()) {
                        // 映射节点ID
                        Long newFromNodeId = oldToNewNodeIdMap.get(edgeInfo.getFromNodeId());
                        Long newEndNodeId = oldToNewNodeIdMap.get(edgeInfo.getEndNodeId());
                        
                        if (newFromNodeId != null && newEndNodeId != null) {
                            JobEdge newJobEdge = new JobEdge();
                            newJobEdge.setJobParentId(newTaskGroupId);
                            newJobEdge.setFromNodeId(newFromNodeId);
                            newJobEdge.setEndNodeId(newEndNodeId);
                            newJobEdge.setPointsList(edgeInfo.getPointsList());
                            newJobEdge.setProperties(edgeInfo.getProperties());
                            newJobEdge.setStartPoint(edgeInfo.getStartPoint());
                            newJobEdge.setEndPoint(edgeInfo.getEndPoint());
                            jobEdgeService.save(newJobEdge);

                            Long newEdgeId = newJobEdge.getId();
                            Long oldEdgeId = edgeInfo.getId();
                            oldToNewEdgeIdMap.put(oldEdgeId, newEdgeId);
                        }
                    }
                }

                // 5.4 更新任务组节点的children字段（如果有嵌套任务组）
                // 这里需要处理嵌套任务组的情况，暂时先跳过
            }
        }
    }

    /**
     * 将TaskInfoData转换为JobInfo
     */
    private JobInfo convertToJobInfo(PartitionExportData.TaskInfoData taskInfoData) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobGroup(taskInfoData.getJobGroup());
        jobInfo.setJobDesc(taskInfoData.getJobDesc());
        jobInfo.setAuthor(taskInfoData.getAuthor());
        jobInfo.setAlarmEmail(taskInfoData.getAlarmEmail());
        jobInfo.setScheduleType(taskInfoData.getScheduleType());
        jobInfo.setScheduleConf(taskInfoData.getScheduleConf());
        jobInfo.setMisfireStrategy(taskInfoData.getMisfireStrategy());
        jobInfo.setExecutorRouteStrategy(taskInfoData.getExecutorRouteStrategy());
        jobInfo.setExecutorHandler(taskInfoData.getExecutorHandler());
        jobInfo.setExecutorParam(taskInfoData.getExecutorParam());
        jobInfo.setExecutorBlockStrategy(taskInfoData.getExecutorBlockStrategy());
        jobInfo.setExecutorTimeout(taskInfoData.getExecutorTimeout());
        jobInfo.setExecutorFailRetryCount(taskInfoData.getExecutorFailRetryCount());
        jobInfo.setGlueType(taskInfoData.getGlueType());
        jobInfo.setGlueSource(taskInfoData.getGlueSource());
        jobInfo.setGlueRemark(taskInfoData.getGlueRemark());
        jobInfo.setChildJobId(taskInfoData.getChildJobId());
        jobInfo.setTriggerStatus(taskInfoData.getTriggerStatus());
        jobInfo.setTriggerLastTime(taskInfoData.getTriggerLastTime());
        jobInfo.setTriggerNextTime(taskInfoData.getTriggerNextTime());
        jobInfo.setJobType(taskInfoData.getJobType());
        jobInfo.setParentId(taskInfoData.getParentId());
        jobInfo.setReqType(taskInfoData.getReqType());
        jobInfo.setReqHeader(taskInfoData.getReqHeader());
        jobInfo.setReqBody(taskInfoData.getReqBody());
        jobInfo.setReqUrl(taskInfoData.getReqUrl());
        jobInfo.setNodeFlag(taskInfoData.getNodeFlag());
        jobInfo.setJdbcDatasourceId(taskInfoData.getJdbcDatasourceId());
        jobInfo.setIncrementType(taskInfoData.getIncrementType());
        jobInfo.setIncrementContent(taskInfoData.getIncrementContent());
        jobInfo.setRunTime(taskInfoData.getRunTime());
        jobInfo.setPauseStatus(taskInfoData.getPauseStatus());
        jobInfo.setJobPartId(taskInfoData.getJobPartId());
        jobInfo.setTriggerUserId(taskInfoData.getTriggerUserId());
        return jobInfo;
    }
}
