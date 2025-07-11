package com.cc.job.admin.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.admin.task.service.JobEdgeService;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.service.JobPartService;
import com.cc.job.xo.mapper.JobPartMapper;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.entity.JobPart;
import com.cc.job.xo.model.vo.JobPartVo;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

        List<JobInfo> jobInfoList = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getJobType, 2).eq(JobInfo::getIsNode, "N"));

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

    // TODO 后修修改
    @Override
    public byte[] exportData(Long id) {
        List<JobPartVo> treeList = this.getTree();
        JobPartVo jobPartVo = treeList.stream().filter(e -> e.getId().equals(id) ).findFirst().orElseThrow();
        Gson gson = new Gson();
        System.out.println(gson.toJson(jobPartVo));
        String json = gson.toJson(jobPartVo);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importData(MultipartFile file) {
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
            throw new RuntimeException(e);
        }
        System.out.println(json);
        Gson gson = new Gson();
        JobPartVo jobPartVo = gson.fromJson(json, JobPartVo.class);
        //添加数据
        //1.往jabpart中插入数据
        JobPart jobPart = this.getById(jobPartVo.getId());
        JobPart copyJobPart = BeanUtil.copyProperties(jobPart, JobPart.class, "id");
        this.save(copyJobPart);
        //2.往jobinfo中插入数据
        List<JobInfo> jobInfoList = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getJobPartId, jobPartVo.getId()));
        Map<Long, JobInfo> jobInfoMap = new HashMap<>();
        Map<Long,Long> jobParentIdMap = new HashMap<>();
        getAllJobInfoMap(jobInfoList, jobInfoMap,jobParentIdMap);

        Map<Long,Long> jobParentIdMap2 = new HashMap<>();

        List<JobInfo> copyJobInfoList = new ArrayList<>();
        jobInfoMap.values().forEach(jobInfo -> {
            JobInfo copyJobInfo = BeanUtil.copyProperties(jobInfo, JobInfo.class, "id");
            if("N".equalsIgnoreCase(copyJobInfo.getIsNode())&&copyJobInfo.getJobType()==2){
                copyJobInfo.setJobPartId(Integer.parseInt(String.valueOf(copyJobPart.getId())));
            }
            jobInfoService.save(copyJobInfo);
            copyJobInfo.setExecutorParam(String.valueOf(copyJobInfo.getId()));
            jobInfoService.updateById(copyJobInfo);
            copyJobInfoList.add(copyJobInfo);
            //新id与原id的比较
            jobParentIdMap2.put(copyJobInfo.getId(),jobInfo.getId());
        });

        for (JobInfo jobInfo : copyJobInfoList) {
            if(jobInfo.getParentId()==0){
                continue;
            }
            Long preJobId = jobParentIdMap2.get(jobInfo.getId());
            JobInfo preJobInfo = jobInfoMap.get(preJobId);
            Long parentId = preJobInfo.getParentId();
            Long newParentId = 0L;
            for (Map.Entry<Long, Long> entry : jobParentIdMap2.entrySet()) {
                if(entry.getValue().equals(parentId)){
                    newParentId = entry.getKey();
                    break;
                }
            }
            jobInfo.setParentId(newParentId);
            jobInfoService.updateById(jobInfo);
        }
        //3.往jobnode中插入数据
        List<Long> ids = jobInfoMap.keySet().stream().toList();
        List<JobNode> jobNodes = jobNodeService.list(new LambdaQueryWrapper<JobNode>().in(JobNode::getJobId, ids));

        Map<Long,Long> jobNodeIdMap = new HashMap<>();
        List<JobNode> copyJobNodeList = new ArrayList<>();
        for (JobNode jobNode : jobNodes) {
            //TODO 保存子节点
            for (Map.Entry<Long, Long> entry : jobParentIdMap2.entrySet()) {
                if(entry.getValue().equals(jobNode.getJobId())){
                    jobNode.setJobId(entry.getKey());
                    JobNode jobNode1 = BeanUtil.copyProperties(jobNode, JobNode.class, "id");
                    jobNodeService.save(jobNode1);
                    copyJobNodeList.add(jobNode1);
                    jobNodeIdMap.put(jobNode.getId(),jobNode1.getId());
                    break;
                }
            }
        }

        for (JobNode jobNode : copyJobNodeList) {
            Map<String, Object> propertiesMap = JSONUtil.toBean(jobNode.getProperties(), Map.class);
            if(jobNode.getNodeType().equalsIgnoreCase("CustomGroup")){
                //修改孩子节点
                String children = jobNode.getChildren();
                List<Long> newIds = new ArrayList<>();
                if(children!=null){
                    long[] childrenArr = Arrays.stream(children.substring(1, children.length() - 1).split(","))
                            .map(String::trim)
                            .mapToLong(Long::parseLong)
                            .toArray();
                    for (long id : childrenArr) {
                        Long nid = jobNodeIdMap.get(id);
                        newIds.add(nid);
                    }
                }
                jobNode.setChildren(JSONUtil.toJsonStr(newIds));
                propertiesMap.put("children", JSONUtil.toJsonStr(newIds));
            }
            propertiesMap.put("jobId",jobNode.getJobId());
            jobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
            for (Map.Entry<Long, Long> entry : jobParentIdMap2.entrySet()) {
                if(entry.getValue().equals(jobNode.getJobParentId())){
                    jobNode.setJobParentId(entry.getKey());
                    break;
                }
            }
            jobNodeService.updateById(jobNode);
        }
        //4.往jobedge中插入数据
        List<JobEdge> jobEdgeList = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().in(JobEdge::getJobParentId, ids));
        for (JobEdge jobEdge : jobEdgeList) {
            Long fromNodeId = jobEdge.getFromNodeId();
            Long endNodeId = jobEdge.getEndNodeId();
            JobEdge copyJobEdge = BeanUtil.copyProperties(jobEdge, JobEdge.class, "id");
            Long fromId = jobNodeIdMap.get(fromNodeId);
            Long endId = jobNodeIdMap.get(endNodeId);
            for (Map.Entry<Long, Long> entry : jobParentIdMap2.entrySet()) {
                if(entry.getValue().equals(jobEdge.getJobParentId())){
                    copyJobEdge.setJobParentId(entry.getKey());
                    break;
                }
            }
            copyJobEdge.setFromNodeId(fromId);
            copyJobEdge.setEndNodeId(endId);
            jobEdgeService.save(copyJobEdge);
        }
    }


    private void  getAllJobInfoMap(List<JobInfo> jobInfoList,Map<Long, JobInfo> jobInfoMap,Map<Long,Long> jobParentIdMap) {
        if(jobInfoList==null){
            return;
        }
        for (JobInfo jobInfo : jobInfoList) {
            jobInfoMap.put(jobInfo.getId(), jobInfo);
            jobParentIdMap.put(jobInfo.getId(), jobInfo.getParentId());
            if(jobInfo.getJobType()==2){
                List<JobInfo> childJobInfoList = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, jobInfo.getId()));
                getAllJobInfoMap(childJobInfoList, jobInfoMap,jobParentIdMap);
            }
        }
    }
}
