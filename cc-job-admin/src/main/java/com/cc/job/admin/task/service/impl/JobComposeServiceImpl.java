package com.cc.job.admin.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.cc.job.admin.task.service.JobComposeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.form.JobInfoForm;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class JobComposeServiceImpl implements JobComposeService {

    final JobInfoService jobInfoService;

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
    public boolean saveJobCompose(JobInfoForm formData) {
        JobInfo jobInfo = BeanUtil.copyProperties(formData, JobInfo.class);
        jobInfo.setIsNode("N");
        jobInfo.setJobType(2);
        jobInfoService.save(jobInfo);

        // 添加任务组
        List<LfNode> lfNodes = JSONUtil.parseArray(formData.getNodes()).toList(LfNode.class);
        List<LfEdge> lfEdges = JSONUtil.parseArray(formData.getEdges()).toList(LfEdge.class);

        return operateToSaveJobCompose(jobInfo,lfNodes,lfEdges);
    }

    private boolean operateToSaveJobCompose(JobInfo jobInfo, List<LfNode> lfNodes, List<LfEdge> lfEdges) {
         // 找到第一层级节点
        Map<String, List<LfNode>> lfNodeGroupByTypeMap = lfNodes.stream().collect(Collectors.groupingBy(LfNode::getType));
        //all child
        List<LfNode> groupNodes = lfNodeGroupByTypeMap.get("dynamic-group");
        Set<String> childrenNode = new HashSet<>();
        groupNodes.forEach(lfNode -> {

        });
        return false;
    }
}
