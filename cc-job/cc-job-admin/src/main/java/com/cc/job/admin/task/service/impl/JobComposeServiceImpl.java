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
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.vo.JobEdgeVo;
import com.cc.job.xo.model.vo.JobNodeVo;
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
        for (LfNode node : nodeList) {
            Map<String, Object> properties = JSONUtil.toBean(node.getProperties(), Map.class);
            Long jobId = Long.parseLong(String.valueOf(properties.get(JOB_ID)));
            JobInfo jobInfo1 = jobInfoService.getById(jobId);
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
        }

        // 添加任务组边
        List<JobEdge> jobEdgeList = new ArrayList<>();
        for (LfEdge edge : edgeList) {
            Long sourceJobId = nodeIdMap.get(edge.getSourceNodeId());
            Long targetJobId = nodeIdMap.get(edge.getTargetNodeId());
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

        jobEdgeService.saveBatch(jobEdgeList);

        List<JobNode> nodeFromDbList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobInfo.getId()));
        nodeFromDbList.forEach(item -> {
            item.setNodeInDegree(jobEdgeList.stream().filter(v -> v.getEndNodeId().equals(item.getId())).count());
            item.setNodeOutDegree(jobEdgeList.stream().filter(v -> v.getFromNodeId().equals(item.getId())).count());
        });
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
                JobNode jobNode = nodeFromDb.stream().filter(n -> n.getId().equals(Long.parseLong(node.getId()))).findFirst().orElse(null);
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

        jobEdgeService.remove(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, jobInfo.getId()));

        List<JobEdge> jobEdgeList = new ArrayList<>();
        for (LfEdge edge : edgeList) {
            Long sourceJobId = nodeIdMap.get(edge.getSourceNodeId());
            Long targetJobId = nodeIdMap.get(edge.getTargetNodeId());
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
        nodeFromDbList2.forEach(item -> {
            item.setNodeInDegree(jobEdgeList.stream().filter(v -> v.getEndNodeId().equals(item.getId())).count());
            item.setNodeOutDegree(jobEdgeList.stream().filter(v -> v.getFromNodeId().equals(item.getId())).count());
        });
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
        Map<String, List<JobNodeVo>> groupNodeMap = nodeVos.stream().collect(Collectors.groupingBy(JobNodeVo::getNodeType));
        List<JobNodeVo> dynamicGroupNodes = groupNodeMap.get(DYNAMIC_GROUP);
        List<String> nodeIds = new ArrayList<>();
        if (dynamicGroupNodes != null) {
            dynamicGroupNodes.forEach(node -> {
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                nodeIds.addAll(childIds);
            });
        }
        List<JobNodeVo> nodeList = nodeVos.stream().filter(n -> !nodeIds.contains(n.getId())).toList();
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
        double[] styleArr = getMaxWidthHeight(nodeVos);
        //！！！设置节点的位置，一定要除2，前端真的巨难
        jobNodeVo.setNodePositionX(styleArr[3] + (styleArr[1] - styleArr[3]) / 2);
        jobNodeVo.setNodePositionY(styleArr[0] + (styleArr[2] - styleArr[0]) / 2);
        properties.put("height", styleArr[2] - styleArr[0] + 10);
        properties.put("width", styleArr[1] - styleArr[3] + 10);
        jobNodeVo.setProperties(JSONUtil.toJsonStr(properties));

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
        JobNode jobNode = new JobNode();
        jobNode.setJobId(jobInfo.getId());
        jobNode.setJobParentId(formData.getParentId());
        jobNode.setNodeType(NODE_TYPE_MAP.get(formData.getGlueType()));
        jobNode.setNodePositionX(formData.getNodePositionX()==null?(double)0:formData.getNodePositionX());
        jobNode.setNodePositionY(formData.getNodePositionY()==null?(double)0:formData.getNodePositionY());
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
            for (Object object : objects) {
                Map<String, Object> objectMap = (Map<String, Object>) object;
                Object id = objectMap.get("id");
                Object properties = objectMap.get("properties");
                Object x = objectMap.get( "x");
                Object y = objectMap.get("y");

                Map<String, Object> propertiesMap = (Map<String, Object>)properties;
                Object width = propertiesMap.get("width");
                Object height = propertiesMap.get("height");
                Object jobId = propertiesMap.get("jobId");
                Object glueType = objectMap.get("type");
                // 添加新的节点
                JobInfo jobInfo = jobInfoService.getById(String.valueOf(jobId));
                jobInfo.setJobDesc(jobInfo.getJobDesc()+"_copy");
                jobInfo.setParentId(Long.parseLong(String.valueOf(parentId)));
                jobInfo.setIsNode("Y");
                jobInfo.setIsPause(0);
                jobInfo.setId(null);
                jobInfoService.save(jobInfo);
                JobNode jobNode = new JobNode();
                jobNode.setJobId(jobInfo.getId());
                jobNode.setJobParentId(Long.parseLong(String.valueOf(parentId)));
                jobNode.setNodeType(String.valueOf(glueType));
                jobNode.setNodePositionX(x==null?(double)0:(Double.parseDouble(String.valueOf(x))+50));
                jobNode.setNodePositionY(x==null?(double)0:(Double.parseDouble(String.valueOf(y))+50));
                Map<String,Object> propertieMap = new HashMap<>();
                propertieMap.put(JOB_ID, jobInfo.getId());
                propertieMap.put("width",width);
                propertieMap.put("height",height);
                jobNode.setProperties(JSONUtil.toJsonStr(propertieMap));
                jobNodeService.save(jobNode);
                nodeIds.put(Long.parseLong(String.valueOf(id)),jobNode.getId());
                jobNodes.add(jobNode);

            }


            Object[] objects2 = ((ArrayList<?>) edges).toArray();;
            List<JobEdge> jobEdges = new ArrayList<>();
            for (Object object : objects2) {
                Map<String, Object> objectMap = (Map<String, Object>) object;
                Object sourceNodeId = objectMap.get("sourceNodeId");
                Object targetNodeId = objectMap.get("targetNodeId");
                JobEdge jobEdge = jobEdgeService.getOne(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getFromNodeId,Long.parseLong(String.valueOf(sourceNodeId))).eq(JobEdge::getEndNodeId,Long.parseLong(String.valueOf(targetNodeId))));
                Long fromNodeId = jobEdge.getFromNodeId();
                Long endNodeId = jobEdge.getEndNodeId();
                JobEdge newjobEdge = new JobEdge();
                newjobEdge.setFromNodeId(nodeIds.get(fromNodeId));
                newjobEdge.setEndNodeId(nodeIds.get(endNodeId));
                newjobEdge.setJobParentId(Long.parseLong(String.valueOf(parentId)));
                jobEdgeService.save(newjobEdge);
                jobEdges.add(newjobEdge);

            }
            result.put("nodes",jobNodes);
            result.put("edges",jobEdges);
            result.put("nodeIds",nodeIds);
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

    public void getJobCompose(Long id, List<JobNodeVo> nodeVos, List<JobEdgeVo> edgeVos, String randomId) {
        List<JobNode> jobNodeList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, id));
        List<JobEdge> jobEdgeList = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, id));

        List<Long> jobIds = jobNodeList.stream().map(JobNode::getJobId).toList();
        if(jobIds.isEmpty()){
            return;
        }
        List<JobInfo> jobInfos = jobInfoService.listByIds(jobIds);
        Map<Long, JobInfo> jobInfoMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getId, n -> n));

        jobNodeList.forEach(node -> {
            JobNodeVo jobNodeVo = BeanUtil.copyProperties(node, JobNodeVo.class, "id");
            JobInfo jobInfo = jobInfoMap.get(node.getJobId());
            jobNodeVo.setJobName(jobInfo.getJobDesc());
            jobNodeVo.setId(randomId + node.getId());
            jobNodeVo.setIsPause(jobInfo.getIsPause());
            // 显式传递节点运行状态，避免序列化遗漏
            jobNodeVo.setTriggerStatus(node.getTriggerStatus());
            if (DYNAMIC_GROUP.equalsIgnoreCase(node.getNodeType())) {
                String children = node.getChildren();
                List<String> childIds = JSONUtil.parseArray(children).toList(String.class);
                List<String> newChildIds = new ArrayList<>();
                for (String childId : childIds) {
                    newChildIds.add(randomId + childId);
                }
                Map<String, Object> propertiesMap = JSONUtil.toBean(node.getProperties(), Map.class);
                propertiesMap.put("children", JSONUtil.toJsonStr(newChildIds));
                jobNodeVo.setProperties(JSONUtil.toJsonStr(propertiesMap));
                jobNodeVo.setChildren(JSONUtil.toJsonStr(newChildIds));
                getJobCompose(node.getJobId(), nodeVos, edgeVos, randomId);
            }
            nodeVos.add(jobNodeVo);
        });
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
}


