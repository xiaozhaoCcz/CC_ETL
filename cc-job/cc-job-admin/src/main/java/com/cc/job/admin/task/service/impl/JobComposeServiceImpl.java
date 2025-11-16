package com.cc.job.admin.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.service.JobComposeService;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobLogglue;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobEdgeForm;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.vo.JobEdgeVo;
import com.cc.job.xo.model.vo.JobNodeVo;
import com.cc.job.xo.mapper.JobLogglueMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.cc.job.admin.task.service.impl.JobInfoServiceImpl.NODE_TYPE_MAP;

@Service
@AllArgsConstructor
public class JobComposeServiceImpl implements JobComposeService {

    final JobInfoService jobInfoService;

    final JobNodeService jobNodeService;

    final JobEdgeService jobEdgeService;
    
    final JobLogglueMapper jobLogglueMapper;

    @Data
    public static class LfNode {
        private String id;
        private String text;
        private String type;
        private Double x;
        private Double y;
        private String properties;
        private String children;
    }

    @Data
    public static class LfEdge {
        private String id;
        private String pointsList;
        private String properties;
        private String sourceNodeId;
        private String targetNodeId;
        private String type;
        private String startPoint;
        private String endPoint;
        private String sourceAnchorId;
        private String targetAnchorId;
    }

    private final String DYNAMIC_GROUP = "CustomGroup";

    private final String JOB_ID = "jobId";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveJobCompose(JobInfoForm formData) {

        boolean b = validateJobComposeEdge(formData.getNodes(), formData.getEdges());
        if (!b) {
            throw new BusinessException("任务组边不合法");
        }

        JobInfo jobInfo = jobInfoService.baseSaveJobInfo(formData);

        jobInfo.setJobType(2);
        jobInfoService.save(jobInfo);
        jobInfo.setExecutorParam(String.valueOf(jobInfo.getId()));
        jobInfoService.updateById(jobInfo);

        if (StringUtils.isBlank(formData.getNodes())) {
            return true;
        }

        // 添加任务组
        List<LfNode> lfNodes = JSONUtil.parseArray(formData.getNodes()).toList(LfNode.class);
        List<LfEdge> lfEdges = JSONUtil.parseArray(formData.getEdges()).toList(LfEdge.class);

        Map<String, List<LfNode>> groupNodeMap = lfNodes.stream().collect(Collectors.groupingBy(LfNode::getType));
        List<LfNode> dynamicGroupNodes = groupNodeMap.get(DYNAMIC_GROUP);
        List<String> nodeIds = new ArrayList<>();
        if (dynamicGroupNodes != null) {
            dynamicGroupNodes.forEach(node -> {
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                nodeIds.addAll(childIds);
            });
        }

        List<LfNode> nodeList = lfNodes.stream().filter(n -> !nodeIds.contains(n.getId())).toList();
        List<String> firstNodes = nodeList.stream().map(LfNode::getId).toList();
        List<LfEdge> edgeList = lfEdges.stream().filter(e -> firstNodes.contains(e.getSourceNodeId()) || firstNodes.contains(e.getTargetNodeId())).toList();

        operateToSaveJobCompose(jobInfo, nodeList, edgeList, lfNodes, lfEdges);
        return true;
    }


    private List<Long> operateToSaveJobCompose(JobInfo jobInfo, List<LfNode> nodeList, List<LfEdge> edgeList, List<LfNode> lfNodes, List<LfEdge> lfEdges) {
        Map<String, Long> nodeIdMap = new HashMap<>();
        
        // 优化：批量查询所有需要的JobInfo，避免N+1查询问题
        List<Long> jobIds = nodeList.stream()
            .map(node -> {
                Map<String, Object> properties = JSONUtil.toBean(node.getProperties(), Map.class);
                return Long.parseLong(String.valueOf(properties.get(JOB_ID)));
            })
            .distinct()
            .toList();
        
        if (!jobIds.isEmpty()) {
            List<JobInfo> jobInfos = jobInfoService.listByIds(jobIds);
            Map<Long, JobInfo> jobInfoMap = jobInfos.stream()
                .collect(Collectors.toMap(JobInfo::getId, n -> n, (existing, replacement) -> existing));
            
            // 批量保存JobInfo
            List<JobInfo> newJobInfos = new ArrayList<>();
            Map<Long, JobInfo> originalToNewMap = new HashMap<>();
            
            for (LfNode node : nodeList) {
                Map<String, Object> properties = JSONUtil.toBean(node.getProperties(), Map.class);
                Long jobId = Long.parseLong(String.valueOf(properties.get(JOB_ID)));
                JobInfo jobInfo1 = jobInfoMap.get(jobId);
                if (jobInfo1 == null) {
                    continue;
                }
                
                JobInfo copyJobInfo = BeanUtil.copyProperties(jobInfo1, JobInfo.class, "id");
                copyJobInfo.setIsNode("Y");
                copyJobInfo.setParentId(jobInfo.getId());
                newJobInfos.add(copyJobInfo);
                originalToNewMap.put(jobId, copyJobInfo);
            }
            
            // 批量保存JobInfo
            if (!newJobInfos.isEmpty()) {
                jobInfoService.saveBatch(newJobInfos);
            }
            
            // 处理节点
            List<JobNode> newJobNodes = new ArrayList<>();
            for (LfNode node : nodeList) {
                Map<String, Object> properties = JSONUtil.toBean(node.getProperties(), Map.class);
                Long jobId = Long.parseLong(String.valueOf(properties.get(JOB_ID)));
                JobInfo copyJobInfo = originalToNewMap.get(jobId);
                if (copyJobInfo == null) {
                    continue;
                }
                
                JobNode jobNode = new JobNode();
                jobNode.setJobId(copyJobInfo.getId());
                jobNode.setJobParentId(jobInfo.getId());
                jobNode.setNodePositionX(node.x);
                jobNode.setNodePositionY(node.y);
                jobNode.setNodeType(node.type);
                // 修复：新创建的节点，triggerStatus设置为-1表示未运行状态（白色背景）
                jobNode.setTriggerStatus(-1);
                Map<String, Object> propertiesMap = JSONUtil.toBean(node.properties, Map.class);
                propertiesMap.put(JOB_ID, copyJobInfo.getId());

                if (DYNAMIC_GROUP.equalsIgnoreCase(node.getType())) {
                    List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                    List<LfNode> childNodes = lfNodes.stream().filter(n -> childIds.contains(n.getId())).toList();
                    List<LfEdge> childEdges = lfEdges.stream().filter(e -> childIds.contains(e.getSourceNodeId()) || childIds.contains(e.targetNodeId)).toList();
                    List<Long> childJobIds = operateToSaveJobCompose(copyJobInfo, childNodes, childEdges, lfNodes, lfEdges);
                    propertiesMap.put("children", JSONUtil.toJsonStr(childJobIds));
                    jobNode.setChildren(JSONUtil.toJsonStr(childJobIds));
                    copyJobInfo.setJobType(2);
                    copyJobInfo.setExecutorParam(String.valueOf(copyJobInfo.getId()));
                    jobInfoService.updateById(copyJobInfo);
                }
                jobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
                newJobNodes.add(jobNode);
            }
            
            // 批量保存JobNode
            if (!newJobNodes.isEmpty()) {
                jobNodeService.saveBatch(newJobNodes);
                // 更新nodeIdMap
                for (int i = 0; i < nodeList.size() && i < newJobNodes.size(); i++) {
                    nodeIdMap.put(nodeList.get(i).getId(), newJobNodes.get(i).getId());
                }
            }
        }

        // 添加任务组边
        List<JobEdge> jobEdgeList = new ArrayList<>();
        for (LfEdge edge : edgeList) {
            Long sourceJobId = nodeIdMap.get(edge.getSourceNodeId());
            Long targetJobId = nodeIdMap.get(edge.getTargetNodeId());
            if (sourceJobId != null && targetJobId != null) {
                JobEdge jobEdge = new JobEdge();
                jobEdge.setFromNodeId(sourceJobId);
                jobEdge.setEndNodeId(targetJobId);
                jobEdge.setJobParentId(jobInfo.getId());
                jobEdge.setProperties(edge.properties);
                jobEdge.setPointsList(edge.pointsList);
                jobEdge.setStartPoint(edge.startPoint);
                jobEdge.setEndPoint(edge.endPoint);
                jobEdgeList.add(jobEdge);
            }
        }

        jobEdgeService.saveBatch(jobEdgeList);

        List<JobNode> nodeFromDbList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobInfo.getId()));
        
        // 优化：预先构建边映射，避免在forEach中重复遍历（O(n²) -> O(n)）
        Map<Long, Long> inDegreeMap = new HashMap<>();
        Map<Long, Long> outDegreeMap = new HashMap<>();
        
        for (JobEdge edge : jobEdgeList) {
            // 统计入度
            inDegreeMap.merge(edge.getEndNodeId(), 1L, Long::sum);
            // 统计出度
            outDegreeMap.merge(edge.getFromNodeId(), 1L, Long::sum);
        }
        
        // 批量设置节点的入度和出度
        for (JobNode item : nodeFromDbList) {
            item.setNodeInDegree(inDegreeMap.getOrDefault(item.getId(), 0L));
            item.setNodeOutDegree(outDegreeMap.getOrDefault(item.getId(), 0L));
        }
        
        jobNodeService.updateBatchById(nodeFromDbList);
        return nodeIdMap.values().stream().toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateJobCompose(Long id, JobInfoForm formData) {
        boolean b = validateJobComposeEdge(formData.getNodes(), formData.getEdges());
        if (!b) {
            throw new BusinessException("任务组边不合法");
        }
        JobInfo jobInfo = jobInfoService.baseUpdateJobInfo(id, formData);
//        if (StringUtils.isBlank(formData.getNodes())) {
//            throw new BusinessException("任务节点不能为空");
//        }
        jobInfoService.updateById(jobInfo);

        if (StringUtils.isBlank(formData.getNodes())) {
            return true;
        }

        // 添加任务组
        List<LfNode> lfNodes = JSONUtil.parseArray(formData.getNodes()).toList(LfNode.class);
        List<LfEdge> lfEdges = JSONUtil.parseArray(formData.getEdges()).toList(LfEdge.class);

        Map<String, List<LfNode>> groupNodeMap = lfNodes.stream().collect(Collectors.groupingBy(LfNode::getType));
        List<LfNode> dynamicGroupNodes = groupNodeMap.get(DYNAMIC_GROUP);
        List<String> nodeIds = new ArrayList<>();
        if (dynamicGroupNodes != null) {
            dynamicGroupNodes.forEach(node -> {
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                nodeIds.addAll(childIds);
            });
        }

        List<LfNode> nodeList = lfNodes.stream().filter(n -> !nodeIds.contains(n.getId())).toList();
        List<String> firstNodes = nodeList.stream().map(LfNode::getId).toList();
        List<LfEdge> edgeList = lfEdges.stream().filter(e -> firstNodes.contains(e.getSourceNodeId()) || firstNodes.contains(e.getTargetNodeId())).toList();
        operateToUpdateJobCompose(jobInfo, nodeList, edgeList, lfNodes, lfEdges);
        return true;
    }


    private List<Long> operateToUpdateJobCompose(JobInfo jobInfo, List<LfNode> nodeList, List<LfEdge> edgeList, List<LfNode> lfNodes, List<LfEdge> lfEdges) {
        Map<String, Long> nodeIdMap = new HashMap<>();
        List<JobNode> nodeFromDb = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobInfo.getId()));
        List<JobNode> updateNodes = new ArrayList<>();

        for (LfNode node : nodeList) {
            Map<String, Object> properties = JSONUtil.toBean(node.getProperties(), Map.class);
            Object jobIdObj = properties.get(JOB_ID);
            if (jobIdObj == null) {
                throw new BusinessException("存在未选择任务的节点");
            }
            Long jobId = Long.parseLong(String.valueOf(jobIdObj));
            JobInfo jobInfo1 = jobInfoService.getById(jobId);
            if (node.getId().contains("-")) {
                JobInfo copyJobInfo = BeanUtil.copyProperties(jobInfo1, JobInfo.class, "id");
                copyJobInfo.setIsNode("Y");
                copyJobInfo.setParentId(jobInfo.getId());
                jobInfoService.save(copyJobInfo);

                JobNode jobNode = new JobNode();
                jobNode.setJobId(copyJobInfo.getId());
                jobNode.setJobParentId(jobInfo.getId());
                jobNode.setNodePositionX(node.x);
                jobNode.setNodePositionY(node.y);
                jobNode.setNodeType(node.type);
                // 修复：新创建的节点，triggerStatus设置为-1表示未运行状态（白色背景）
                jobNode.setTriggerStatus(-1);
                Map<String, Object> propertiesMap = JSONUtil.toBean(node.properties, Map.class);
                propertiesMap.put(JOB_ID, copyJobInfo.getId());

                if (DYNAMIC_GROUP.equalsIgnoreCase(node.getType())) {
                    List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                    List<LfNode> childNodes = lfNodes.stream().filter(n -> childIds.contains(n.getId())).toList();
                    List<LfEdge> childEdges = lfEdges.stream().filter(e -> childIds.contains(e.getSourceNodeId()) || childIds.contains(e.targetNodeId)).toList();
                    List<Long> childJobIds = operateToSaveJobCompose(copyJobInfo, childNodes, childEdges, lfNodes, lfEdges);
                    propertiesMap.put("children", JSONUtil.toJsonStr(childJobIds));
                    jobNode.setChildren(JSONUtil.toJsonStr(childJobIds));
                    copyJobInfo.setJobType(2);
                    copyJobInfo.setExecutorParam(String.valueOf(copyJobInfo.getId()));
                    jobInfoService.updateById(copyJobInfo);
                }
                jobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
                jobNodeService.save(jobNode);
                nodeIdMap.put(node.getId(), jobNode.getId());
            } else {
                // ⭐ 修复：尝试通过nodeId查找节点，如果找不到，再尝试通过jobId查找（处理粘贴节点的情况）
                Long nodeIdLong = Long.parseLong(node.getId());
                JobNode jobNode = nodeFromDb.stream().filter(n -> n.getId().equals(nodeIdLong)).findFirst().orElse(null);
                
                // 如果通过nodeId找不到，尝试通过jobId查找（粘贴的节点可能已经保存，但nodeId可能不匹配）
                if (jobNode == null) {
                    Map<String, Object> nodeProperties = JSONUtil.toBean(node.getProperties(), Map.class);
                    Object nodeJobIdObj = nodeProperties.get(JOB_ID);
                    if (nodeJobIdObj != null) {
                        Long nodeJobId = Long.parseLong(String.valueOf(nodeJobIdObj));
                        // 通过jobId查找节点（粘贴的节点可能已经保存）
                        jobNode = nodeFromDb.stream().filter(n -> n.getJobId().equals(nodeJobId)).findFirst().orElse(null);
                    }
                }
                
                // ⭐ 修复：如果找不到已存在的节点，可能是新粘贴的节点，需要创建新节点
                if (jobNode == null) {
                    // 创建新节点（类似包含"-"的逻辑）
                    JobInfo copyJobInfo = BeanUtil.copyProperties(jobInfo1, JobInfo.class, "id");
                    copyJobInfo.setIsNode("Y");
                    copyJobInfo.setParentId(jobInfo.getId());
                    jobInfoService.save(copyJobInfo);

                    JobNode newJobNode = new JobNode();
                    newJobNode.setJobId(copyJobInfo.getId());
                    newJobNode.setJobParentId(jobInfo.getId());
                    newJobNode.setNodePositionX(node.x);
                    newJobNode.setNodePositionY(node.y);
                    newJobNode.setNodeType(node.type);
                    // 修复：新创建的节点，triggerStatus设置为-1表示未运行状态（白色背景）
                    newJobNode.setTriggerStatus(-1);
                    Map<String, Object> propertiesMap = JSONUtil.toBean(node.properties, Map.class);
                    propertiesMap.put(JOB_ID, copyJobInfo.getId());

                    if (DYNAMIC_GROUP.equalsIgnoreCase(node.getType())) {
                        List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                        List<LfNode> childNodes = lfNodes.stream().filter(n -> childIds.contains(n.getId())).toList();
                        List<LfEdge> childEdges = lfEdges.stream().filter(e -> childIds.contains(e.getSourceNodeId()) || childIds.contains(e.targetNodeId)).toList();
                        List<Long> childJobIds = operateToSaveJobCompose(copyJobInfo, childNodes, childEdges, lfNodes, lfEdges);
                        propertiesMap.put("children", JSONUtil.toJsonStr(childJobIds));
                        newJobNode.setChildren(JSONUtil.toJsonStr(childJobIds));
                        copyJobInfo.setJobType(2);
                        copyJobInfo.setExecutorParam(String.valueOf(copyJobInfo.getId()));
                        jobInfoService.updateById(copyJobInfo);
                    }
                    newJobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
                    jobNodeService.save(newJobNode);
                    nodeIdMap.put(node.getId(), newJobNode.getId());
                } else {
                    // 更新已存在的节点
                    JobInfo copyJobInfo = BeanUtil.copyProperties(jobInfo1, JobInfo.class, "id","parentId","jobPartId");
                    copyJobInfo.setId(jobNode.getJobId());
                    jobInfoService.updateById(copyJobInfo);
                    Map<String, Object> propertiesMap = JSONUtil.toBean(node.properties, Map.class);
                    jobNode.setNodePositionX(node.x);
                    jobNode.setNodePositionY(node.y);
                    jobNode.setNodeType(node.type);
                    if (DYNAMIC_GROUP.equalsIgnoreCase(node.getType())) {
                        List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                        List<LfNode> childNodes = lfNodes.stream().filter(n -> childIds.contains(n.getId())).toList();
                        List<LfEdge> childEdges = lfEdges.stream().filter(e -> childIds.contains(e.getSourceNodeId()) || childIds.contains(e.targetNodeId)).toList();
                        List<Long> childJobIds = operateToUpdateJobCompose(jobInfo1, childNodes, childEdges, lfNodes, lfEdges);
                        jobNode.setChildren(JSONUtil.toJsonStr(childJobIds));
                        propertiesMap.put("children", JSONUtil.toJsonStr(childJobIds));
                    }
                    jobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
                    updateNodes.add(jobNode);
                    nodeIdMap.put(node.getId(), jobNode.getId());
                }
            }
        }

        jobEdgeService.remove(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobInfo.getId()));

        List<JobEdge> jobEdgeList = new ArrayList<>();
        for (LfEdge edge : edgeList) {
            Long sourceJobId = nodeIdMap.get(edge.getSourceNodeId());
            Long targetJobId = nodeIdMap.get(edge.getTargetNodeId());
            // ⭐ 修复：只有当sourceJobId和targetJobId都不为null时才创建边，避免保存无效的边
            if (sourceJobId != null && targetJobId != null) {
                JobEdge jobEdge = new JobEdge();
                jobEdge.setFromNodeId(sourceJobId);
                jobEdge.setEndNodeId(targetJobId);
                jobEdge.setJobParentId(jobInfo.getId());
                jobEdge.setProperties(edge.properties);
                jobEdge.setPointsList(edge.pointsList);
                jobEdge.setStartPoint(edge.startPoint);
                jobEdge.setEndPoint(edge.endPoint);
                jobEdgeList.add(jobEdge);
            }
        }

        jobEdgeService.saveBatch(jobEdgeList);

        List<Long> updateNodeIds = updateNodes.stream().map(JobNode::getId).toList();
        List<JobNode> delNodeDbs = nodeFromDb.stream().filter(n -> !updateNodeIds.contains(n.getId())).toList();

        if (!delNodeDbs.isEmpty()) {
            for (JobNode delNodeDb : delNodeDbs) {
                if (DYNAMIC_GROUP.equalsIgnoreCase(delNodeDb.getNodeType())) {
                    jobInfoService.delNodes(delNodeDb.getJobId());
                }
            }
            jobInfoService.removeBatchByIds(delNodeDbs.stream().map(JobNode::getJobId).toList());
            jobNodeService.removeBatchByIds(delNodeDbs.stream().map(JobNode::getId).toList());
        }
        jobNodeService.updateBatchById(updateNodes);

        // 处理边
        List<JobNode> nodeFromDbList2 = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobInfo.getId()));
        
        // 优化：预先构建边映射，避免在forEach中重复遍历（O(n²) -> O(n)）
        Map<Long, Long> inDegreeMap = new HashMap<>();
        Map<Long, Long> outDegreeMap = new HashMap<>();
        
        for (JobEdge edge : jobEdgeList) {
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
        
        jobNodeService.updateBatchById(nodeFromDbList2);
        return nodeIdMap.values().stream().toList();
    }

    @Override
    public Map<String, Object> getJobCompose(Map<String, Object> formMap) {
        Map<String, Object> res = new HashMap<>();
        List<JobNodeVo> nodeVos = new ArrayList<>();
        List<JobEdgeVo> edgeVos = new ArrayList<>();
        Long id = Long.parseLong(String.valueOf(formMap.get("id")));
        int type = Integer.parseInt(String.valueOf(formMap.get("type")));
        String randomId = type == 0 ? "" : UUID.fastUUID() + ":";
        getJobCompose(id, nodeVos, edgeVos, randomId);
        //创建一个父亲节点
        // 修复：使用忽略大小写的匹配来查找任务组节点（因为数据库中的node_type可能是custom-group小写）
        List<JobNodeVo> dynamicGroupNodes = nodeVos.stream()
            .filter(node -> node.getNodeType() != null && 
                           DYNAMIC_GROUP.equalsIgnoreCase(node.getNodeType()))
            .collect(Collectors.toList());
        
        // 收集所有任务组节点的子节点ID（需要过滤掉，因为它们属于任务组节点，不属于根任务组）
        Set<String> childNodeIdsToFilter = new HashSet<>();
        if (dynamicGroupNodes != null && !dynamicGroupNodes.isEmpty()) {
            for (JobNodeVo groupNode : dynamicGroupNodes) {
                String children = groupNode.getChildren();
                if (StringUtils.isNotBlank(children)) {
                    List<String> childIds = JSONUtil.parseArray(children).toList(String.class);
                    childNodeIdsToFilter.addAll(childIds);
                }
            }
        }
        
        // 过滤掉任务组节点的子节点，但保留任务组节点本身
        // 注意：任务组节点的子节点的job_parent_id应该是任务组节点的jobId，而不是根任务组的jobId
        // 所以我们需要通过children字段来识别哪些节点是任务组节点的子节点
        List<JobNodeVo> nodeList = nodeVos.stream()
            .filter(n -> {
                // 保留任务组节点本身
                if (n.getNodeType() != null && DYNAMIC_GROUP.equalsIgnoreCase(n.getNodeType())) {
                    return true;
                }
                // 过滤掉任务组节点的子节点
                return !childNodeIdsToFilter.contains(n.getId());
            })
            .collect(Collectors.toList());
        List<String> firstNodes = nodeList.stream().map(JobNodeVo::getId).toList();

        JobInfo jobInfo = jobInfoService.getById(id);
        JobNodeVo jobNodeVo = new JobNodeVo();
        jobNodeVo.setId(randomId + DYNAMIC_GROUP);
        jobNodeVo.setJobId(jobInfo.getId());
        jobNodeVo.setChildren(JSONUtil.toJsonStr(firstNodes));
        jobNodeVo.setNodeType(DYNAMIC_GROUP);
        jobNodeVo.setJobName(jobInfo.getJobDesc());
        Map<String, Object> properties = new HashMap<>();
        properties.put(JOB_ID, String.valueOf(jobInfo.getId()));
        properties.put("children", JSONUtil.toJsonStr(firstNodes));
        properties.put("isRestrict", true);
        properties.put("autoResize", true);
        //计算最大高度和最大宽度
//        double[] styleArr = getMaxWidthHeight(nodeVos);
//        //！！！设置节点的位置，一定要除2，前端真的巨难
//        jobNodeVo.setNodePositionX(styleArr[3] + (styleArr[1] - styleArr[3]) / 2);
//        jobNodeVo.setNodePositionY(styleArr[0] + (styleArr[2] - styleArr[0]) / 2);
//        properties.put("height", styleArr[2] - styleArr[0] + 10);
//        properties.put("width", styleArr[1] - styleArr[3] + 10);
//        jobNodeVo.setProperties(JSONUtil.toJsonStr(properties));

        if (type == 1) {
            nodeVos.add(jobNodeVo);
        }
        
        // 只有 type=1 (运行时模式) 才需要调整节点位置
        // type=0 (普通查看模式) 直接返回数据库中的原始位置
        if (type == 1) {
            Object xObj = formMap.get("x");
            Object yObj = formMap.get("y");
            if (xObj != null && yObj != null) {
                double x = Double.parseDouble(String.valueOf(xObj));
                double y = Double.parseDouble(String.valueOf(yObj));
                double[] nodeXY = new double[]{x, y};
                updateNodeXY(nodeVos, nodeXY, new double[]{jobNodeVo.getNodePositionX(), jobNodeVo.getNodePositionY()});
            }
        }
        
        res.put("jobNode", jobNodeVo);
        res.put("nodes", nodeVos);
        res.put("edges", edgeVos);
        return res;
    }

    @Override
    public boolean validateJobComposeEdge(String nodes, String edges) {
        if(StringUtils.isBlank(nodes)){
            return true;
        }
        List<LfNode> lfNodes = JSONUtil.parseArray(nodes).toList(LfNode.class);
        List<LfEdge> lfEdges = JSONUtil.parseArray(edges).toList(LfEdge.class);

        Map<String, List<LfNode>> groupNodeMap = lfNodes.stream().collect(Collectors.groupingBy(LfNode::getType));
        List<LfNode> dynamicGroupNodes = groupNodeMap.get(DYNAMIC_GROUP);
        List<List<String>> nodeIds = new ArrayList<>();
        if (dynamicGroupNodes != null) {
            dynamicGroupNodes.forEach(node -> {
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                nodeIds.add(childIds);
            });
        }

        for (LfEdge edge : lfEdges) {
            for (List<String> childIds : nodeIds) {
                if ((childIds.contains(edge.sourceNodeId) && !childIds.contains(edge.targetNodeId)) || (childIds.contains(edge.targetNodeId) && !childIds.contains(edge.sourceNodeId))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public JobNode saveJobNode(JobInfoForm formData) {
        JobInfo jobInfo = BeanUtil.copyProperties(formData, JobInfo.class);
        jobInfo.setGlueUpdatetime(LocalDateTime.now());
        jobInfo.setIsNode("Y");
        jobInfo.setIsPause(0);
        jobInfoService.save(jobInfo);

        if (StringUtils.isNotBlank(formData.getGlueRemark())) {
            //插入glueSource
            JobGlueForm glueForm = new JobGlueForm();
            glueForm.setTaskId(jobInfo.getId());
            glueForm.setGlueSource(formData.getGlueSource());
            glueForm.setGlueType(formData.getGlueType());
            glueForm.setGlueRemark(formData.getGlueRemark());
            jobInfoService.saveGlueSource(glueForm);
        }
        
        JobNode jobNode = new JobNode();
        jobNode.setJobId(jobInfo.getId());
        jobNode.setJobParentId(formData.getParentId());
        jobNode.setNodeType(NODE_TYPE_MAP.get(formData.getGlueType()));
        jobNode.setNodePositionX(formData.getNodePositionX()==null?(double)0:formData.getNodePositionX());
        jobNode.setNodePositionY(formData.getNodePositionY()==null?(double)0:formData.getNodePositionY());
        // 修复：新创建的节点，triggerStatus设置为-1表示未运行状态（白色背景）
        jobNode.setTriggerStatus(-1);
        Map<String,Object> properties = new HashMap<>();
        properties.put(JOB_ID, jobInfo.getId());
        properties.put("width",160);
        properties.put("height",90);
        jobNode.setProperties(JSONUtil.toJsonStr(properties));
        jobNodeService.save(jobNode);
        return jobNode;
    }

    @Override
    public Long updateJobNode(Long jobId, Long nodeId) {
        JobInfo jobInfo = jobInfoService.getById(jobId);
        JobNode jobNode = jobNodeService.getById(nodeId);
        JobInfo copyJobInfo = BeanUtil.copyProperties(jobInfo, JobInfo.class, "id","parentId","jobPartId");
        copyJobInfo.setId(jobNode.getJobId());
        jobInfoService.updateById(copyJobInfo);
        return jobNode.getJobId();
    }

    @Override
    public List<Long> pauseJobs(Long[] jobIds) {
        List<Long> list = new ArrayList<>();
        List<JobInfo> jobInfoList = jobInfoService.listByIds(Arrays.asList(jobIds));
        jobInfoList.forEach(jobInfo -> {
            if(jobInfo.getIsPause()==1){
                list.add(jobInfo.getId());
            }
        });
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobNode(Long nodeId) {
        JobNode jobNode = jobNodeService.getById(nodeId);
        if(jobNode==null){
            throw new BusinessException("当前节点不存在");
        }
        //删除任务
        jobInfoService.removeById(jobNode.getJobId());
        //删除节点
        jobNodeService.removeById(nodeId);
        //删除与之相关的边
        jobEdgeService.remove(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getFromNodeId,nodeId).or().eq(JobEdge::getEndNodeId,nodeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveJobNodeAndJobEdges(Map<String, Object> formMap) {
        Object nodes = formMap.get("nodes");
        Object edges = formMap.get("edges");
        Object parentId = formMap.get("jobId");
        Map<Long,Long> nodeIds = new HashMap<>();
        Map<String,Object> result = new HashMap<>();

        Object[] objects = ((ArrayList<?>) nodes).toArray();
        List<JobNode> jobNodes = new ArrayList<>();
        List<JobInfo> newJobInfos = new ArrayList<>();
        
        // 优化：批量查询所有需要的JobInfo，避免N+1查询问题
        List<Long> jobIds = new ArrayList<>();
        List<Map<String, Object>> nodeDataList = new ArrayList<>();
        
        for (Object object : objects) {
            Map<String, Object> objectMap = (Map<String, Object>) object;
            Map<String, Object> propertiesMap = (Map<String, Object>) objectMap.get("properties");
            Object jobId = propertiesMap.get("jobId");
            if (jobId != null) {
                jobIds.add(Long.parseLong(String.valueOf(jobId)));
                nodeDataList.add(objectMap);
            }
        }
        
        // 批量查询JobInfo
        Map<Long, JobInfo> jobInfoMap = new HashMap<>();
        if (!jobIds.isEmpty()) {
            List<JobInfo> jobInfos = jobInfoService.listByIds(jobIds);
            jobInfoMap = jobInfos.stream()
                .collect(Collectors.toMap(JobInfo::getId, n -> n, (existing, replacement) -> existing));
        }
        
        // 批量创建JobInfo和JobNode
        for (Map<String, Object> objectMap : nodeDataList) {
            Object id = objectMap.get("id");
            Object properties = objectMap.get("properties");
            Object x = objectMap.get("x");
            Object y = objectMap.get("y");

            Map<String, Object> propertiesMap = (Map<String, Object>) properties;
            Object width = propertiesMap.get("width");
            Object height = propertiesMap.get("height");
            Object jobId = propertiesMap.get("jobId");
            Object glueType = objectMap.get("type");
            
            // 添加新的节点
            Long jobIdLong = Long.parseLong(String.valueOf(jobId));
            JobInfo originalJobInfo = jobInfoMap.get(jobIdLong);
            if (originalJobInfo == null) {
                continue;
            }
            
            JobInfo jobInfo = BeanUtil.copyProperties(originalJobInfo, JobInfo.class);
            jobInfo.setJobDesc(jobInfo.getJobDesc() + "_copy");
            jobInfo.setParentId(Long.parseLong(String.valueOf(parentId)));
            jobInfo.setIsNode("Y");
            jobInfo.setIsPause(0);
            jobInfo.setId(null);
            newJobInfos.add(jobInfo);
        }
        
        // 批量保存JobInfo
        if (!newJobInfos.isEmpty()) {
            jobInfoService.saveBatch(newJobInfos);
        }
        
        // 创建JobNode
        int jobInfoIndex = 0;
        for (Map<String, Object> objectMap : nodeDataList) {
            if (jobInfoIndex >= newJobInfos.size()) {
                break;
            }
            
            Object id = objectMap.get("id");
            Object properties = objectMap.get("properties");
            Object x = objectMap.get("x");
            Object y = objectMap.get("y");

            Map<String, Object> propertiesMap = (Map<String, Object>) properties;
            Object width = propertiesMap.get("width");
            Object height = propertiesMap.get("height");
            Object glueType = objectMap.get("type");
            
            JobInfo newJobInfo = newJobInfos.get(jobInfoIndex);
            JobNode jobNode = new JobNode();
            jobNode.setJobId(newJobInfo.getId());
            jobNode.setJobParentId(Long.parseLong(String.valueOf(parentId)));
            jobNode.setNodeType(String.valueOf(glueType));
            jobNode.setNodePositionX(x == null ? (double) 0 : (Double.parseDouble(String.valueOf(x)) + 50));
            jobNode.setNodePositionY(y == null ? (double) 0 : (Double.parseDouble(String.valueOf(y)) + 50));
            // 修复：新创建的节点，triggerStatus设置为-1表示未运行状态（白色背景）
            jobNode.setTriggerStatus(-1);
            Map<String, Object> propertieMap = new HashMap<>();
            propertieMap.put(JOB_ID, newJobInfo.getId());
            propertieMap.put("width", width);
            propertieMap.put("height", height);
            jobNode.setProperties(JSONUtil.toJsonStr(propertieMap));
            jobNodes.add(jobNode);
            nodeIds.put(Long.parseLong(String.valueOf(id)), null); // 先占位
            jobInfoIndex++;
        }
        
        // 批量保存JobNode
        if (!jobNodes.isEmpty()) {
            jobNodeService.saveBatch(jobNodes);
            // 更新nodeIds映射
            int nodeIndex = 0;
            for (Map<String, Object> objectMap : nodeDataList) {
                if (nodeIndex < jobNodes.size()) {
                    Object id = objectMap.get("id");
                    nodeIds.put(Long.parseLong(String.valueOf(id)), jobNodes.get(nodeIndex).getId());
                    nodeIndex++;
                }
            }
        }

        // 批量查询和保存JobEdge
        Object[] objects2 = ((ArrayList<?>) edges).toArray();
        List<JobEdge> jobEdges = new ArrayList<>();
        List<Long> sourceNodeIds = new ArrayList<>();
        List<Long> targetNodeIds = new ArrayList<>();
        
        for (Object object : objects2) {
            Map<String, Object> objectMap = (Map<String, Object>) object;
            Object sourceNodeId = objectMap.get("sourceNodeId");
            Object targetNodeId = objectMap.get("targetNodeId");
            sourceNodeIds.add(Long.parseLong(String.valueOf(sourceNodeId)));
            targetNodeIds.add(Long.parseLong(String.valueOf(targetNodeId)));
        }
        
        // 批量查询JobEdge
        Map<String, JobEdge> edgeMap = new HashMap<>();
        if (!sourceNodeIds.isEmpty() && !targetNodeIds.isEmpty()) {
            for (int i = 0; i < sourceNodeIds.size() && i < targetNodeIds.size(); i++) {
                Long fromNodeId = sourceNodeIds.get(i);
                Long endNodeId = targetNodeIds.get(i);
                JobEdge jobEdge = jobEdgeService.getOne(
                    new LambdaQueryWrapper<JobEdge>()
                        .eq(JobEdge::getFromNodeId, fromNodeId)
                        .eq(JobEdge::getEndNodeId, endNodeId)
                );
                if (jobEdge != null) {
                    edgeMap.put(fromNodeId + ":" + endNodeId, jobEdge);
                }
            }
        }
        
        // 创建新的JobEdge
        for (Object object : objects2) {
            Map<String, Object> objectMap = (Map<String, Object>) object;
            Object sourceNodeId = objectMap.get("sourceNodeId");
            Object targetNodeId = objectMap.get("targetNodeId");
            
            Long fromNodeId = Long.parseLong(String.valueOf(sourceNodeId));
            Long endNodeId = Long.parseLong(String.valueOf(targetNodeId));
            JobEdge originalEdge = edgeMap.get(fromNodeId + ":" + endNodeId);
            
            if (originalEdge != null) {
                Long newFromNodeId = nodeIds.get(originalEdge.getFromNodeId());
                Long newEndNodeId = nodeIds.get(originalEdge.getEndNodeId());
                if (newFromNodeId != null && newEndNodeId != null) {
                    JobEdge newJobEdge = new JobEdge();
                    newJobEdge.setFromNodeId(newFromNodeId);
                    newJobEdge.setEndNodeId(newEndNodeId);
                    newJobEdge.setJobParentId(Long.parseLong(String.valueOf(parentId)));
                    jobEdges.add(newJobEdge);
                }
            }
        }
        
        // 批量保存JobEdge
        if (!jobEdges.isEmpty()) {
            jobEdgeService.saveBatch(jobEdges);
        }
        
        result.put("nodes", jobNodes);
        result.put("edges", jobEdges);
        result.put("nodeIds", nodeIds);
        return result;
    }

    public static Object getPropertyValue(Object obj, String propertyName) {
        if (obj == null || propertyName == null) return null;

        try {
            Class<?> clazz = obj.getClass();
            Field field = clazz.getDeclaredField(propertyName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, Object> convertToMapExcludeNull(Object obj) {
        if (obj == null) return null;

        Map<String, Object> map = new HashMap<>();
        try {
            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    map.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }


    /**
     * !!!!重点:获取任务组的宽度和高度，并获取任务组起始位置
     *
     * @param nodeVos
     * @return
     */
    private double[] getMaxWidthHeight(List<JobNodeVo> nodeVos) {
        double top = Double.MAX_VALUE;
        double bottom = 0;
        double left = Double.MAX_VALUE;
        double right = 0;

        for (JobNodeVo nodeVo : nodeVos) {
            String properties = nodeVo.getProperties();
            Map<String, Object> propertiesMap = JSONUtil.toBean(properties, Map.class);
            double width = Double.parseDouble(String.valueOf(propertiesMap.get("width")));
            double height = Double.parseDouble(String.valueOf(propertiesMap.get("height")));
            if (nodeVo.getNodePositionY() - height / 2 < top) {
                top = nodeVo.getNodePositionY() - height / 2;
            }
            if (nodeVo.getNodePositionY() + height / 2 > bottom) {
                bottom = nodeVo.getNodePositionY() + height / 2;
            }
            if (nodeVo.getNodePositionX() - width / 2 < left) {
                left = nodeVo.getNodePositionX() - width / 2;
            }
            if (nodeVo.getNodePositionX() + width / 2 > right) {
                right = nodeVo.getNodePositionX() + width / 2;
            }
        }
        return new double[]{top, right, bottom, left};
    }

    private void updateNodeXY(List<JobNodeVo> nodeVoList, double[] nodeXY, double[] sourceNodeXY) {
        double sourceX = sourceNodeXY[0];
        double sourceY = sourceNodeXY[1];
        double x = nodeXY[0];
        double y = nodeXY[1];
        for (JobNodeVo jobNodeVo : nodeVoList) {
            Double nodePositionX = jobNodeVo.getNodePositionX();
            Double nodePositionY = jobNodeVo.getNodePositionY();
            jobNodeVo.setNodePositionX(nodePositionX - sourceX + x);
            jobNodeVo.setNodePositionY(nodePositionY - sourceY + y);
        }
    }

    /**
     * 优化版本：批量查询所有层级的数据，避免N+1查询问题
     * 
     * @param id 根任务组ID
     * @param nodeVos 节点VO列表（输出参数）
     * @param edgeVos 边VO列表（输出参数）
     * @param randomId 随机ID前缀
     */
    public void getJobCompose(Long id, List<JobNodeVo> nodeVos, List<JobEdgeVo> edgeVos, String randomId) {
        // 1. 收集所有需要查询的任务组ID（包括嵌套的任务组）
        Set<Long> allJobIds = new HashSet<>();
        allJobIds.add(id);
        collectAllJobIds(id, allJobIds);
        
        // 2. 批量查询所有节点和边（一次性查询，避免递归查询）
        List<JobNode> allJobNodes = jobNodeService.list(
            new LambdaQueryWrapper<JobNode>().in(JobNode::getJobParentId, allJobIds)
        );
        List<JobEdge> allJobEdges = jobEdgeService.list(
            new LambdaQueryWrapper<JobEdge>().in(JobEdge::getJobParentId, allJobIds)
        );
        
        // 2.1 修复：如果发现任务组节点，需要将它们的jobId也添加到allJobIds中，以便查询它们的子节点
        // 因为任务组节点的子节点的job_parent_id是任务组节点的jobId，而不是任务组节点的job_parent_id
        Set<Long> groupJobIds = new HashSet<>();
        for (JobNode node : allJobNodes) {
            if (DYNAMIC_GROUP.equalsIgnoreCase(node.getNodeType())) {
                Long groupJobId = node.getJobId();
                if (groupJobId != null && !allJobIds.contains(groupJobId)) {
                    groupJobIds.add(groupJobId);
                }
            }
        }
        // 如果发现新的任务组节点，需要查询它们的子节点
        if (!groupJobIds.isEmpty()) {
            allJobIds.addAll(groupJobIds);
            List<JobNode> additionalNodes = jobNodeService.list(
                new LambdaQueryWrapper<JobNode>().in(JobNode::getJobParentId, groupJobIds)
            );
            List<JobEdge> additionalEdges = jobEdgeService.list(
                new LambdaQueryWrapper<JobEdge>().in(JobEdge::getJobParentId, groupJobIds)
            );
            allJobNodes.addAll(additionalNodes);
            allJobEdges.addAll(additionalEdges);
        }
        
        // 3. 批量查询所有JobInfo
        Set<Long> jobInfoIds = allJobNodes.stream().map(JobNode::getJobId).collect(Collectors.toSet());
        if (jobInfoIds.isEmpty()) {
            return;
        }
        List<JobInfo> allJobInfos = jobInfoService.listByIds(new ArrayList<>(jobInfoIds));
        Map<Long, JobInfo> jobInfoMap = allJobInfos.stream()
            .collect(Collectors.toMap(JobInfo::getId, n -> n, (existing, replacement) -> existing));
        
        // 4. 构建层级映射（按父ID分组）
        Map<Long, List<JobNode>> nodesByParent = allJobNodes.stream()
            .collect(Collectors.groupingBy(JobNode::getJobParentId));
        Map<Long, List<JobEdge>> edgesByParent = allJobEdges.stream()
            .collect(Collectors.groupingBy(JobEdge::getJobParentId));
        
        // 5. 递归构建结果（不再查询数据库，只处理内存数据）
        buildNodeVosRecursive(id, nodesByParent, edgesByParent, jobInfoMap, nodeVos, edgeVos, randomId);
    }
    
    /**
     * 收集所有任务组ID（包括嵌套的任务组）- 优化版本
     * 一次性查询所有节点，然后在内存中递归，避免多次数据库查询
     * 
     * @param jobId 当前任务组ID
     * @param allJobIds 所有任务组ID集合（输出参数）
     */
    private void collectAllJobIds(Long jobId, Set<Long> allJobIds) {
        // 如果已经收集过，直接返回（避免重复查询）
        if (allJobIds.contains(jobId)) {
            return;
        }
        
        List<JobNode> nodes = jobNodeService.list(
            new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobId)
        );
        
        for (JobNode node : nodes) {
            if (DYNAMIC_GROUP.equalsIgnoreCase(node.getNodeType())) {
                Long childJobId = node.getJobId();
                if (childJobId != null && allJobIds.add(childJobId)) {
                    // 递归收集子任务组的ID
                    collectAllJobIds(childJobId, allJobIds);
                }
            }
        }
    }
    
    /**
     * 收集所有任务组ID（超级优化版本：一次性查询所有相关节点）
     * 如果节点数量非常大，可以使用这个版本
     * 
     * @param rootJobId 根任务组ID
     * @param allJobIds 所有任务组ID集合（输出参数）
     */
    private void collectAllJobIdsOptimized(Long rootJobId, Set<Long> allJobIds) {
        // 1. 先查询根任务组的节点
        List<JobNode> rootNodes = jobNodeService.list(
            new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, rootJobId)
        );
        
        // 2. 收集所有任务组ID（包括嵌套的）
        Set<Long> tempJobIds = new HashSet<>();
        tempJobIds.add(rootJobId);
        
        for (JobNode node : rootNodes) {
            if (DYNAMIC_GROUP.equalsIgnoreCase(node.getNodeType())) {
                Long childJobId = node.getJobId();
                if (childJobId != null) {
                    tempJobIds.add(childJobId);
                }
            }
        }
        
        // 3. 如果发现嵌套的任务组，批量查询所有相关节点
        if (tempJobIds.size() > 1) {
            List<JobNode> allNodes = jobNodeService.list(
                new LambdaQueryWrapper<JobNode>().in(JobNode::getJobParentId, tempJobIds)
            );
            
            // 4. 构建 parentId -> nodes 的映射
            Map<Long, List<JobNode>> nodesByParent = allNodes.stream()
                .collect(Collectors.groupingBy(JobNode::getJobParentId));
            
            // 5. 递归收集所有任务组ID（使用内存数据）
            collectJobIdsRecursive(rootJobId, nodesByParent, allJobIds);
        } else {
            allJobIds.addAll(tempJobIds);
        }
    }
    
    /**
     * 递归收集任务组ID（使用内存数据，不查询数据库）
     */
    private void collectJobIdsRecursive(Long jobId, Map<Long, List<JobNode>> nodesByParent, Set<Long> allJobIds) {
        if (!allJobIds.add(jobId)) {
            return; // 已经收集过，避免重复
        }
        
        List<JobNode> nodes = nodesByParent.getOrDefault(jobId, Collections.emptyList());
        for (JobNode node : nodes) {
            if (DYNAMIC_GROUP.equalsIgnoreCase(node.getNodeType())) {
                Long childJobId = node.getJobId();
                if (childJobId != null) {
                    collectJobIdsRecursive(childJobId, nodesByParent, allJobIds);
                }
            }
        }
    }
    
    /**
     * 递归构建节点和边的VO（使用内存数据，不再查询数据库）
     * 
     * @param jobId 当前任务组ID
     * @param nodesByParent 按父ID分组的节点映射
     * @param edgesByParent 按父ID分组的边映射
     * @param jobInfoMap JobInfo映射
     * @param nodeVos 节点VO列表（输出参数）
     * @param edgeVos 边VO列表（输出参数）
     * @param randomId 随机ID前缀
     */
    private void buildNodeVosRecursive(Long jobId, Map<Long, List<JobNode>> nodesByParent,
                                      Map<Long, List<JobEdge>> edgesByParent,
                                      Map<Long, JobInfo> jobInfoMap,
                                      List<JobNodeVo> nodeVos, List<JobEdgeVo> edgeVos,
                                      String randomId) {
        // 获取当前任务组的节点和边
        List<JobNode> jobNodeList = nodesByParent.getOrDefault(jobId, Collections.emptyList());
        List<JobEdge> jobEdgeList = edgesByParent.getOrDefault(jobId, Collections.emptyList());
        
        // 处理节点
        for (JobNode node : jobNodeList) {
            JobNodeVo jobNodeVo = BeanUtil.copyProperties(node, JobNodeVo.class, "id");
            JobInfo jobInfo = jobInfoMap.get(node.getJobId());
            if (jobInfo == null) {
                continue; // 跳过无效的节点
            }
            
            jobNodeVo.setJobName(jobInfo.getJobDesc());
            jobNodeVo.setId(randomId + node.getId());
            jobNodeVo.setIsPause(jobInfo.getIsPause());
            // 修复bug：节点运行状态处理
            // -1 = 未运行（白色背景）
            // 0 = 失败（红色背景）
            // 1 = 成功（绿色背景）
            // 2 = 运行中（蓝色背景）
            // 如果 triggerStatus 是 null，转换为 -1（兼容历史数据）
            Integer triggerStatus = node.getTriggerStatus();
            jobNodeVo.setTriggerStatus(triggerStatus != null ? triggerStatus : -1);
            
            if (DYNAMIC_GROUP.equalsIgnoreCase(node.getNodeType())) {
                String children = node.getChildren();
                List<String> childIds = new ArrayList<>();
                
                // 如果children字段不为空，使用它
                if (StringUtils.isNotBlank(children)) {
                    childIds = JSONUtil.parseArray(children).toList(String.class);
                } else {
                    // 如果children字段为空，从nodesByParent中查找子节点
                    // 任务组节点的子节点的job_parent_id应该等于任务组节点的jobId
                    List<JobNode> childNodes = nodesByParent.getOrDefault(node.getJobId(), Collections.emptyList());
                    for (JobNode childNode : childNodes) {
                        // 排除任务组节点本身（避免循环）
                        if (!DYNAMIC_GROUP.equalsIgnoreCase(childNode.getNodeType())) {
                            childIds.add(String.valueOf(childNode.getId()));
                        }
                    }
                    
                }
                
                // 修复：即使childIds为空，也要设置children字段（为空数组），以便前端能够识别任务组节点
                List<String> newChildIds = new ArrayList<>();
                for (String childId : childIds) {
                    newChildIds.add(randomId + childId);
                }
                Map<String, Object> propertiesMap = JSONUtil.toBean(node.getProperties(), Map.class);
                if (propertiesMap == null) {
                    propertiesMap = new HashMap<>();
                }
                propertiesMap.put("children", JSONUtil.toJsonStr(newChildIds));
                jobNodeVo.setProperties(JSONUtil.toJsonStr(propertiesMap));
                jobNodeVo.setChildren(JSONUtil.toJsonStr(newChildIds));
                
                // 递归处理子任务组（不再查询数据库）
                if (!childIds.isEmpty()) {
                    buildNodeVosRecursive(node.getJobId(), nodesByParent, edgesByParent, 
                                          jobInfoMap, nodeVos, edgeVos, randomId);
                }
            }
            nodeVos.add(jobNodeVo);
        }
        
        // 处理边
        for (JobEdge jobEdge : jobEdgeList) {
            JobEdgeVo jobEdgeVo = BeanUtil.copyProperties(jobEdge, JobEdgeVo.class, "id");
            jobEdgeVo.setId(randomId + jobEdge.getId());
            jobEdgeVo.setFromNodeId(randomId + jobEdge.getFromNodeId());
            jobEdgeVo.setEndNodeId(randomId + jobEdge.getEndNodeId());
            // 复制锚点信息
            jobEdgeVo.setStartPoint(jobEdge.getStartPoint());
            jobEdgeVo.setEndPoint(jobEdge.getEndPoint());
            jobEdgeVo.setProperties(jobEdge.getProperties());
            edgeVos.add(jobEdgeVo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobEdge saveJobEdge(JobEdgeForm formData) {
        // 检查连线是否已存在
        JobEdge existingEdge = jobEdgeService.getOne(
            new LambdaQueryWrapper<JobEdge>()
                .eq(JobEdge::getJobParentId, formData.getJobParentId())
                .eq(JobEdge::getFromNodeId, formData.getFromNodeId())
                .eq(JobEdge::getEndNodeId, formData.getEndNodeId())
                .eq(JobEdge::getIsDeleted, 0)
        );

        if (existingEdge != null) {
            // 如果连线已存在，返回现有连线
            return existingEdge;
        }

        // 创建新的连线
        JobEdge jobEdge = new JobEdge();
        jobEdge.setJobParentId(formData.getJobParentId());
        jobEdge.setFromNodeId(formData.getFromNodeId());
        jobEdge.setEndNodeId(formData.getEndNodeId());
        jobEdge.setStartPoint(formData.getStartPoint());
        jobEdge.setEndPoint(formData.getEndPoint());
        jobEdge.setProperties(formData.getProperties());
        jobEdge.setPointsList(formData.getPointsList());

        // 保存到数据库
        jobEdgeService.save(jobEdge);

        return jobEdge;
    }
}


