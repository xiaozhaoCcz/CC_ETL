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

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class JobComposeServiceImpl implements JobComposeService {

    final JobInfoService jobInfoService;

    final JobNodeService jobNodeService;

    final JobEdgeService jobEdgeService;

    @Data
    public static class LfNode{
        private String id;
        private String text;
        private String type;
        private Double x;
        private Double y;
        private String properties;
        private String children;
    }

    @Data
    public static class LfEdge{
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveJobCompose(JobInfoForm formData) {
        JobInfo jobInfo = jobInfoService.baseSaveTaskInfo(formData);
        if (StringUtils.isBlank(formData.getNodes())) {
            throw new BusinessException("任务节点不能为空");
        }

        jobInfo.setJobType(2);
        jobInfoService.save(jobInfo);
        jobInfo.setExecutorParam(String.valueOf(jobInfo.getId()));
        jobInfoService.updateById(jobInfo);

        // 添加任务组
        List<LfNode> lfNodes = JSONUtil.parseArray(formData.getNodes()).toList(LfNode.class);
        List<LfEdge> lfEdges = JSONUtil.parseArray(formData.getEdges()).toList(LfEdge.class);

        Map<String, List<LfNode>> groupNodeMap = lfNodes.stream().collect(Collectors.groupingBy(LfNode::getType));
        List<LfNode> dynamicGroupNodes = groupNodeMap.get("dynamic-group");
        List<String> nodeIds = new ArrayList<>();
        if(dynamicGroupNodes!=null){
            dynamicGroupNodes.forEach(node->{
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                nodeIds.addAll(childIds);
            });
        }

        List<LfNode> nodeList = lfNodes.stream().filter(n->!nodeIds.contains(n.getId())).toList();
        List<String> firstNodes = nodeList.stream().map(LfNode::getId).toList();
        List<LfEdge> edgeList = lfEdges.stream().filter(e->firstNodes.contains(e.getSourceNodeId())||firstNodes.contains(e.getTargetNodeId())).toList();

        operateToSaveJobCompose(jobInfo,nodeList,edgeList,lfNodes,lfEdges);
         return true;
    }


    private List<Long> operateToSaveJobCompose(JobInfo jobInfo, List<LfNode> nodeList, List<LfEdge> edgeList,List<LfNode> lfNodes,List<LfEdge> lfEdges) {
        Map<String,Long> nodeIdMap = new HashMap<>();
        for (LfNode node : nodeList) {
            Map<String,Object> properties= JSONUtil.toBean(node.getProperties(), Map.class);
            Long jobId = Long.parseLong(String.valueOf(properties.get("jobId"))) ;
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
            Map<String,Object> propertiesMap = JSONUtil.toBean(node.properties, Map.class);
            propertiesMap.put("jobId",copyJobInfo.getId());

            if("dynamic-group".equalsIgnoreCase(node.getType())){
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                List<LfNode> childNodes = lfNodes.stream().filter(n -> childIds.contains(n.getId())).toList();
                List<LfEdge> childEdges = lfEdges.stream().filter(e -> childIds.contains(e.getSourceNodeId())||childIds.contains(e.targetNodeId)).toList();
                List<Long> childJobIds = operateToSaveJobCompose(copyJobInfo, childNodes, childEdges,lfNodes,lfEdges);
                propertiesMap.put("children",JSONUtil.toJsonStr(childJobIds));
                jobNode.setChildren(JSONUtil.toJsonStr(childJobIds));
                copyJobInfo.setJobType(2);
                copyJobInfo.setExecutorParam(String.valueOf(copyJobInfo.getId()));
                jobInfoService.updateById(copyJobInfo);
            }
            jobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
            jobNodeService.save(jobNode);
            nodeIdMap.put(node.getId(),jobNode.getId());
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
        JobInfo jobInfo = jobInfoService.baseUpdateTaskInfo(id, formData);
        if (StringUtils.isBlank(formData.getNodes())) {
            throw new BusinessException("任务节点不能为空");
        }
        jobInfoService.updateById(jobInfo);

        // 添加任务组
        List<LfNode> lfNodes = JSONUtil.parseArray(formData.getNodes()).toList(LfNode.class);
        List<LfEdge> lfEdges = JSONUtil.parseArray(formData.getEdges()).toList(LfEdge.class);

        Map<String, List<LfNode>> groupNodeMap = lfNodes.stream().collect(Collectors.groupingBy(LfNode::getType));
        List<LfNode> dynamicGroupNodes = groupNodeMap.get("dynamic-group");
        List<String> nodeIds = new ArrayList<>();
        if(dynamicGroupNodes!=null) {
            dynamicGroupNodes.forEach(node -> {
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                nodeIds.addAll(childIds);
            });
        }

        List<LfNode> nodeList = lfNodes.stream().filter(n->!nodeIds.contains(n.getId())).toList();
        List<String> firstNodes = nodeList.stream().map(LfNode::getId).toList();
        List<LfEdge> edgeList = lfEdges.stream().filter(e->firstNodes.contains(e.getSourceNodeId())||firstNodes.contains(e.getTargetNodeId())).toList();
        operateToUpdateJobCompose(jobInfo,nodeList,edgeList,lfNodes,lfEdges);
        return true;
    }


    private List<Long> operateToUpdateJobCompose(JobInfo jobInfo, List<LfNode> nodeList, List<LfEdge> edgeList,List<LfNode> lfNodes,List<LfEdge> lfEdges){
        Map<String,Long> nodeIdMap = new HashMap<>();
        List<JobNode> nodeFromDb = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, jobInfo.getId()));
        List<JobNode> updateNodes = new ArrayList<>();

        for (LfNode node : nodeList) {
            Map<String, Object> properties = JSONUtil.toBean(node.getProperties(), Map.class);
            Long jobId = Long.parseLong(String.valueOf(properties.get("jobId")));
            JobInfo jobInfo1 = jobInfoService.getById(jobId);
            if(node.getId().contains("-")){
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
                Map<String,Object> propertiesMap = JSONUtil.toBean(node.properties, Map.class);
                propertiesMap.put("jobId",copyJobInfo.getId());

                if("dynamic-group".equalsIgnoreCase(node.getType())){
                    List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                    List<LfNode> childNodes = lfNodes.stream().filter(n -> childIds.contains(n.getId())).toList();
                    List<LfEdge> childEdges = lfEdges.stream().filter(e -> childIds.contains(e.getSourceNodeId())||childIds.contains(e.targetNodeId)).toList();
                    List<Long> childJobIds = operateToSaveJobCompose(copyJobInfo, childNodes, childEdges,lfNodes,lfEdges);
                    propertiesMap.put("children",JSONUtil.toJsonStr(childJobIds));
                    jobNode.setChildren(JSONUtil.toJsonStr(childJobIds));
                    copyJobInfo.setJobType(2);
                    copyJobInfo.setExecutorParam(String.valueOf(copyJobInfo.getId()));
                    jobInfoService.updateById(copyJobInfo);
                }
                jobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
                jobNodeService.save(jobNode);
                nodeIdMap.put(node.getId(),jobNode.getId());
            }else {
                JobNode jobNode = nodeFromDb.stream().filter(n -> n.getJobId().equals(jobId)).findFirst().orElse(null);
                if("dynamic-group".equalsIgnoreCase(node.getType())){
                    List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                    List<LfNode> childNodes = lfNodes.stream().filter(n -> childIds.contains(n.getId())).toList();
                    List<LfEdge> childEdges = lfEdges.stream().filter(e -> childIds.contains(e.getSourceNodeId())||childIds.contains(e.targetNodeId)).toList();
                    List<Long> childJobIds = operateToUpdateJobCompose(jobInfo1, childNodes, childEdges,lfNodes,lfEdges);
                    jobNode.setChildren(JSONUtil.toJsonStr(childJobIds));
                }
                updateNodes.add(jobNode);
                nodeIdMap.put(node.getId(),jobNode.getId());
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
                if ( "dynamic-group".equalsIgnoreCase(delNodeDb.getNodeType())){
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
    public Map<String, Object> getJobCompose(Long id,Integer type) {
        Map<String, Object> res =new HashMap<>();
        List<JobNodeVo> nodeVos = new ArrayList<>();
        List<JobEdgeVo> edgeVos = new ArrayList<>();
        String randomId = type==0?"":UUID.fastUUID()+":";
        getJobCompose(id,nodeVos,edgeVos,randomId);
        //创建一个父亲节点
        Map<String, List<JobNodeVo>> groupNodeMap = nodeVos.stream().collect(Collectors.groupingBy(JobNodeVo::getNodeType));
        List<JobNodeVo> dynamicGroupNodes = groupNodeMap.get("dynamic-group");
        List<String> nodeIds = new ArrayList<>();
        if(dynamicGroupNodes!=null){
            dynamicGroupNodes.forEach(node->{
                List<String> childIds = JSONUtil.parseArray(node.getChildren()).toList(String.class);
                nodeIds.addAll(childIds);
            });
        }
        List<JobNodeVo> nodeList = nodeVos.stream().filter(n->!nodeIds.contains(n.getId())).toList();
        List<String> firstNodes = nodeList.stream().map(JobNodeVo::getId).toList();

        JobInfo jobInfo = jobInfoService.getById(id);
        JobNodeVo jobNodeVo = new JobNodeVo();
        jobNodeVo.setId(randomId+"dynamic-group");
        jobNodeVo.setJobId(jobInfo.getId());
        jobNodeVo.setChildren(JSONUtil.toJsonStr(firstNodes));
        jobNodeVo.setNodeType("dynamic-group");
        jobNodeVo.setJobName(jobInfo.getJobDesc());
        Map<String,Object> properties = new HashMap<>();
        properties.put("jobId",String.valueOf(jobInfo.getId()));
        properties.put("children",JSONUtil.toJsonStr(firstNodes));
        properties.put("isRestrict",true);
        properties.put("autoResize",true);
        //计算最大高度和最大宽度
        double[] styleArr = getMaxWidthHeight(nodeVos);
        jobNodeVo.setNodePositionX(styleArr[3]+(styleArr[1]-styleArr[3])/2);
        jobNodeVo.setNodePositionY(styleArr[0]+(styleArr[2]-styleArr[0])/2);
        properties.put("height",styleArr[2]-styleArr[0]);
        properties.put("width",styleArr[1]-styleArr[3]);
        jobNodeVo.setProperties(JSONUtil.toJsonStr(properties));

        res.put("jobNode",jobNodeVo);
        res.put("nodes",nodeVos);
        res.put("edges",edgeVos);
        return res;
    }

    private double[] getMaxWidthHeight(List<JobNodeVo> nodeVos) {
        double top = Double.MAX_VALUE;
        double bottom = 0;
        double left = Double.MAX_VALUE;
        double right = 0;

        for (JobNodeVo nodeVo : nodeVos) {
            String properties = nodeVo.getProperties();
            Map<String,Object> propertiesMap = JSONUtil.toBean(properties, Map.class);
            double width = Double.parseDouble(String.valueOf(propertiesMap.get("width")));
            double height = Double.parseDouble(String.valueOf(propertiesMap.get("height")));
            if (nodeVo.getNodePositionY()-height/2 < top) {
                top = nodeVo.getNodePositionY()-height/2<0?0:nodeVo.getNodePositionY()-height/2;
            }
            if (nodeVo.getNodePositionY()+height > bottom) {
                bottom = nodeVo.getNodePositionY()+height;
            }
            if (nodeVo.getNodePositionX()-width/2 < left) {
                left = nodeVo.getNodePositionX()-width/2<0?0:nodeVo.getNodePositionX()-width/2;
            }
            if (nodeVo.getNodePositionX()+width > right) {
                right = nodeVo.getNodePositionX()+width;
            }
        }
        return new double[]{top,right,bottom,left};
    }

    public void getJobCompose(Long id,List<JobNodeVo> nodeVos,List<JobEdgeVo> edgeVos,String randomId) {
        List<JobNode> jobNodeList = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, id));
        List<JobEdge> jobEdgeList = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, id));

        List<Long> jobIds = jobNodeList.stream().map(JobNode::getJobId).toList();
        List<JobInfo> jobInfos = jobInfoService.listByIds(jobIds);
        Map<Long, String> jobInfoMap = jobInfos.stream().collect(Collectors.toMap(JobInfo::getId, JobInfo::getJobDesc));

        jobNodeList.forEach(node -> {
            JobNodeVo jobNodeVo = BeanUtil.copyProperties(node, JobNodeVo.class,"id");
            String jobName = jobInfoMap.get(node.getJobId());
            jobNodeVo.setJobName(jobName);
            jobNodeVo.setId(randomId+node.getId());
            if ("dynamic-group".equalsIgnoreCase(node.getNodeType())) {
                String children = node.getChildren();
                List<String> childIds = JSONUtil.parseArray(children).toList(String.class);
                List<String> newChildIds = new ArrayList<>();
                for (String childId : childIds) {
                    newChildIds.add( randomId+childId);
                }
                jobNodeVo.setChildren(JSONUtil.toJsonStr(newChildIds));
                getJobCompose(node.getJobId(),nodeVos,edgeVos,randomId);
            }
            nodeVos.add(jobNodeVo);
        });
        for (JobEdge jobEdge : jobEdgeList) {
            JobEdgeVo jobEdgeVo = BeanUtil.copyProperties(jobEdge, JobEdgeVo.class,"id");
            jobEdgeVo.setId(randomId+jobEdge.getId());
            jobEdgeVo.setFromNodeId(randomId+jobEdge.getFromNodeId());
            jobEdgeVo.setEndNodeId(randomId+jobEdge.getEndNodeId());
            edgeVos.add(jobEdgeVo);
        }
    }
}


