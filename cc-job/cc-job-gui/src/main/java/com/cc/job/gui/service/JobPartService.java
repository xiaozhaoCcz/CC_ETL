package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.JobPartVo;
import com.cc.job.gui.model.JobComposeData;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class JobPartService extends  BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobPartService.class);

    /**
     * 获取树形数据
     * @return 树形数据列表
     * @throws IOException 网络异常
     */
    public List<JobPartVo> getTree() throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobParts/getTree";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }

            String responseBody = response.body().string();
            logger.debug("API 响应: {}", responseBody);

            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<List<JobPartVo>>>(){}.getType();
            Result<List<JobPartVo>> result = apiUtil.getGson().fromJson(responseBody, resultType);

            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }

    /**
     * 获取子节点数据
     * @param id 父节点ID
     * @param type 类型
     * @return 子节点数据
     * @throws IOException 网络异常
     */
    public Object getChildren(Long id, Integer type) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobParts/getChildren/" + id + "/" + type;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }

            String responseBody = response.body().string();
            logger.debug("API 响应: {}", responseBody);

            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<Object>>() {
            }.getType();
            Result<Object> result = apiUtil.getGson().fromJson(responseBody, resultType);

            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 保存分区数据
     * @param jobPartName 分区名称
     * @return 是否保存成功
     * @throws IOException 网络异常
     */
    public boolean saveJobPart(String jobPartName) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobParts/saveJobPart";
        
        // 构建请求参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("jobPartName", jobPartName);
        requestMap.put("sort", 0); // 默认排序为0
        
        String jsonBody = apiUtil.getGson().toJson(requestMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.debug("saveJobPart API 响应: {}", responseBody);
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 获取任务组合数据（节点和边）
     * @param jobId 任务组ID
     * @return 任务组合数据
     * @throws IOException 网络异常
     */
    public JobComposeData getJobCompose(Long jobId) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/getJobCompose";
        
        // 构建请求参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", jobId);
        requestMap.put("type", 0);  // 添加type参数，0表示普通加载
        // 注意：不要传递x和y参数，否则会导致后端调整节点位置
        
        String jsonBody = apiUtil.getGson().toJson(requestMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.debug("getJobCompose API 响应: {}", responseBody);
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<Map<String, Object>>>(){}.getType();
            Result<Map<String, Object>> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (Result.isSuccess(result)) {
                Map<String, Object> data = result.getData();
                return parseJobComposeData(data);
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 解析任务组合数据
     */
    private JobComposeData parseJobComposeData(Map<String, Object> data) {
        JobComposeData composeData = new JobComposeData();
        
        // 解析 jobNode（根任务组的聚合信息，含 children）
        Object jobNodeObj = data.get("jobNode");
        if (jobNodeObj instanceof Map) {
            Map<?, ?> jnMap = (Map<?, ?>) jobNodeObj;
            JobComposeData.NodeData jobNode = new JobComposeData.NodeData();
            jobNode.setId(String.valueOf(jnMap.get("id")));
            jobNode.setType(String.valueOf(jnMap.get("nodeType")));
            jobNode.setJobName(String.valueOf(jnMap.get("jobName")));
            if (jnMap.get("jobId") != null) {
                try {
                    jobNode.setJobId(((Number) jnMap.get("jobId")).longValue());
                } catch (Exception ignored) {}
            }
            if (jnMap.get("nodePositionX") != null) {
                jobNode.setX(((Number) jnMap.get("nodePositionX")).doubleValue());
            }
            if (jnMap.get("nodePositionY") != null) {
                jobNode.setY(((Number) jnMap.get("nodePositionY")).doubleValue());
            }
            // properties 可能是JSON字符串
            Map<String, Object> jnProps = new HashMap<>();
            Object jnPropsObj = jnMap.get("properties");
            if (jnPropsObj instanceof String) {
                try {
                    jnProps = apiUtil.getGson().fromJson(
                        (String) jnPropsObj,
                        new TypeToken<Map<String, Object>>(){}.getType()
                    );
                } catch (Exception e) {
                    logger.error("解析 jobNode.properties 失败: {}", e.getMessage(), e);
                }
            } else if (jnPropsObj instanceof Map) {
                //noinspection unchecked
                jnProps = (Map<String, Object>) jnPropsObj;
            }
            // 补充 children 字段
            Object jnChildren = jnMap.get("children");
            if (jnChildren != null) {
                jnProps.put("children", jnChildren);
            }
            jobNode.setProperties(jnProps);
            composeData.setJobNode(jobNode);
        }
        
        // 解析节点
        Object nodesObj = data.get("nodes");
        if (nodesObj instanceof List) {
            List<JobComposeData.NodeData> nodeList = new ArrayList<>();
            List<?> nodes = (List<?>) nodesObj;
            
            for (Object nodeObj : nodes) {
                if (nodeObj instanceof Map) {
                    Map<?, ?> nodeMap = (Map<?, ?>) nodeObj;
                    JobComposeData.NodeData node = new JobComposeData.NodeData();
                    
                    node.setId(String.valueOf(nodeMap.get("id")));
                    // ⭐ 修复：正确处理 nodeType，避免 null 被转换为字符串 "null"
                    Object nodeTypeObj = nodeMap.get("nodeType");
                    if (nodeTypeObj != null && !"null".equals(String.valueOf(nodeTypeObj))) {
                        node.setType(String.valueOf(nodeTypeObj));
                    } else {
                        node.setType(null);  // 设置为 null 而不是字符串 "null"
                    }
                    node.setJobName(String.valueOf(nodeMap.get("jobName")));  // 节点显示名称
                    
                    // ⭐ 关键修复：解析 jobId 字段
                    if (nodeMap.get("jobId") != null) {
                        try {
                            node.setJobId(((Number) nodeMap.get("jobId")).longValue());
                        } catch (Exception e) {
                            logger.error("解析节点 jobId 失败: {}", e.getMessage(), e);
                        }
                    }
                    
                    // ⭐ 新增：解析 jobParentId 字段
                    if (nodeMap.get("jobParentId") != null) {
                        try {
                            node.setJobParentId(((Number) nodeMap.get("jobParentId")).longValue());
                        } catch (Exception e) {
                            logger.error("解析节点 jobParentId 失败: {}", e.getMessage(), e);
                        }
                    }
                    
                    // 解析节点运行状态
                    Object triggerObj = nodeMap.get("triggerStatus");
                    if (triggerObj != null) {
                        try {
                            if (triggerObj instanceof Number number) {
                                node.setTriggerStatus(number.intValue());
                            } else if (triggerObj instanceof String str && !str.isBlank()) {
                                node.setTriggerStatus(Integer.parseInt(str.trim()));
                            } else {
                                node.setTriggerStatus(null);
                            }
                            if (node.getTriggerStatus() != null) {
                                logger.debug("✅ 解析节点运行状态: {} -> {}", node.getId(), node.getTriggerStatus());
                            } else {
                                logger.debug("ℹ️ 节点运行状态为空: {}", node.getId());
                            }
                        } catch (Exception e) {
                            logger.error("解析节点 triggerStatus 失败: {}, 错误: {}", triggerObj, e.getMessage(), e);
                            node.setTriggerStatus(null);
                        }
                    }
                    
                    // 解析坐标 - 后端字段是nodePositionX和nodePositionY
                    if (nodeMap.get("nodePositionX") != null) {
                        node.setX(((Number) nodeMap.get("nodePositionX")).doubleValue());
                    }
                    if (nodeMap.get("nodePositionY") != null) {
                        node.setY(((Number) nodeMap.get("nodePositionY")).doubleValue());
                    }
                    
                    // 解析属性 - properties可能是JSON字符串或Map对象
                    Object propsObj = nodeMap.get("properties");
                    Map<String, Object> propsMap = null;
                    if (propsObj instanceof String) {
                        // 如果是字符串，需要解析为Map
                        try {
                            propsMap = apiUtil.getGson().fromJson(
                                (String) propsObj, 
                                new TypeToken<Map<String, Object>>(){}.getType()
                            );
                        } catch (Exception e) {
                            logger.error("解析节点属性失败: {}", e.getMessage(), e);
                            propsMap = new HashMap<>();
                        }
                    } else if (propsObj instanceof Map) {
                        propsMap = (Map<String, Object>) propsObj;
                    } else {
                        propsMap = new HashMap<>();
                    }
                    
                    // ⭐ 关键修复：如果节点有children字段，将其添加到properties中
                    // 因为任务组节点的children信息在JobNodeVo的children字段中，不在properties中
                    Object childrenObj = nodeMap.get("children");
                    if (childrenObj != null && propsMap != null) {
                        propsMap.put("children", childrenObj);
                        logger.debug("节点 {} 的children字段: {}", node.getId(), childrenObj);
                    }
                    
                    // ⭐ 新增：解析嵌套的子节点列表 childrenNodes（用于任务组节点）
                    Object childrenNodesObj = nodeMap.get("childrenNodes");
                    if (childrenNodesObj instanceof List) {
                        List<JobComposeData.NodeData> childrenNodesList = new ArrayList<>();
                        List<?> childrenNodesList0 = (List<?>) childrenNodesObj;
                        for (Object childNodeObj : childrenNodesList0) {
                            if (childNodeObj instanceof Map) {
                                Map<?, ?> childNodeMap = (Map<?, ?>) childNodeObj;
                                // 递归解析子节点（使用相同的逻辑）
                                JobComposeData.NodeData childNode = parseNodeData(childNodeMap);
                                if (childNode != null) {
                                    childrenNodesList.add(childNode);
                                }
                            }
                        }
                        node.setChildrenNodes(childrenNodesList);
                        logger.debug("✅ 节点 {} 包含 {} 个子节点", node.getId(), childrenNodesList.size());
                    }
                    
                    node.setProperties(propsMap);
                    
                    nodeList.add(node);
                }
            }
            composeData.setNodes(nodeList);
        }
        
        // 解析边
        Object edgesObj = data.get("edges");
        if (edgesObj instanceof List) {
            List<JobComposeData.EdgeData> edgeList = new ArrayList<>();
            List<?> edges = (List<?>) edgesObj;
            
            for (Object edgeObj : edges) {
                if (edgeObj instanceof Map) {
                    Map<?, ?> edgeMap = (Map<?, ?>) edgeObj;
                    JobComposeData.EdgeData edge = new JobComposeData.EdgeData();
                    
                    edge.setId(String.valueOf(edgeMap.get("id")));
                    // 后端字段是fromNodeId和endNodeId，不是sourceNodeId和targetNodeId
                    edge.setSourceNodeId(String.valueOf(edgeMap.get("fromNodeId")));
                    edge.setTargetNodeId(String.valueOf(edgeMap.get("endNodeId")));
                    // 后端字段是startPoint和endPoint，不是sourceAnchor和targetAnchor
                    edge.setSourceAnchor(String.valueOf(edgeMap.get("startPoint")));
                    edge.setTargetAnchor(String.valueOf(edgeMap.get("endPoint")));
                    edge.setType(String.valueOf(edgeMap.get("type")));
                    
                    // 解析属性 - properties可能是JSON字符串或Map对象
                    Object propsObj = edgeMap.get("properties");
                    if (propsObj instanceof String) {
                        // 如果是字符串，需要解析为Map
                        try {
                            Map<String, Object> propsMap = apiUtil.getGson().fromJson(
                                (String) propsObj, 
                                new TypeToken<Map<String, Object>>(){}.getType()
                            );
                            edge.setProperties(propsMap);
                        } catch (Exception e) {
                            logger.error("解析边属性失败: {}", e.getMessage(), e);
                        }
                    } else if (propsObj instanceof Map) {
                        edge.setProperties((Map<String, Object>) propsObj);
                    }
                    
                    edgeList.add(edge);
                }
            }
            composeData.setEdges(edgeList);
        }
        
        return composeData;
    }
    
    /**
     * ⭐ 新增：递归解析节点数据（用于解析嵌套的任务组节点）
     * 
     * @param nodeMap 节点数据Map
     * @return 解析后的NodeData对象
     */
    private JobComposeData.NodeData parseNodeData(Map<?, ?> nodeMap) {
        JobComposeData.NodeData node = new JobComposeData.NodeData();
        
        try {
            node.setId(String.valueOf(nodeMap.get("id")));
            
            // 解析 nodeType
            Object nodeTypeObj = nodeMap.get("nodeType");
            if (nodeTypeObj != null && !"null".equals(String.valueOf(nodeTypeObj))) {
                node.setType(String.valueOf(nodeTypeObj));
            } else {
                node.setType(null);
            }
            
            node.setJobName(String.valueOf(nodeMap.get("jobName")));
            
            // 解析 jobId
            if (nodeMap.get("jobId") != null) {
                try {
                    node.setJobId(((Number) nodeMap.get("jobId")).longValue());
                } catch (Exception e) {
                    logger.error("解析节点 jobId 失败: {}", e.getMessage(), e);
                }
            }
            
            // 解析 jobParentId
            if (nodeMap.get("jobParentId") != null) {
                try {
                    node.setJobParentId(((Number) nodeMap.get("jobParentId")).longValue());
                } catch (Exception e) {
                    logger.error("解析节点 jobParentId 失败: {}", e.getMessage(), e);
                }
            }
            
            // 解析节点运行状态
            Object triggerObj = nodeMap.get("triggerStatus");
            if (triggerObj != null) {
                try {
                    if (triggerObj instanceof Number number) {
                        node.setTriggerStatus(number.intValue());
                    } else if (triggerObj instanceof String str && !str.isBlank()) {
                        node.setTriggerStatus(Integer.parseInt(str.trim()));
                    } else {
                        node.setTriggerStatus(null);
                    }
                } catch (Exception e) {
                    logger.error("解析节点 triggerStatus 失败: {}", e.getMessage(), e);
                    node.setTriggerStatus(null);
                }
            }
            
            // 解析坐标
            if (nodeMap.get("nodePositionX") != null) {
                node.setX(((Number) nodeMap.get("nodePositionX")).doubleValue());
            }
            if (nodeMap.get("nodePositionY") != null) {
                node.setY(((Number) nodeMap.get("nodePositionY")).doubleValue());
            }
            
            // 解析属性
            Object propsObj = nodeMap.get("properties");
            Map<String, Object> propsMap = null;
            if (propsObj instanceof String) {
                try {
                    propsMap = apiUtil.getGson().fromJson(
                        (String) propsObj, 
                        new TypeToken<Map<String, Object>>(){}.getType()
                    );
                } catch (Exception e) {
                    logger.error("解析节点属性失败: {}", e.getMessage(), e);
                    propsMap = new HashMap<>();
                }
            } else if (propsObj instanceof Map) {
                propsMap = (Map<String, Object>) propsObj;
            } else {
                propsMap = new HashMap<>();
            }
            
            // 将 children 字段添加到 properties 中
            Object childrenObj = nodeMap.get("children");
            if (childrenObj != null && propsMap != null) {
                propsMap.put("children", childrenObj);
            }
            
            // 递归解析子节点列表
            Object childrenNodesObj = nodeMap.get("childrenNodes");
            if (childrenNodesObj instanceof List) {
                List<JobComposeData.NodeData> childrenNodesList = new ArrayList<>();
                List<?> childrenNodesList0 = (List<?>) childrenNodesObj;
                for (Object childNodeObj : childrenNodesList0) {
                    if (childNodeObj instanceof Map) {
                        Map<?, ?> childNodeMap = (Map<?, ?>) childNodeObj;
                        JobComposeData.NodeData childNode = parseNodeData(childNodeMap);  // 递归调用
                        if (childNode != null) {
                            childrenNodesList.add(childNode);
                        }
                    }
                }
                node.setChildrenNodes(childrenNodesList);
            }
            
            node.setProperties(propsMap);
            return node;
            
        } catch (Exception e) {
            logger.error("解析节点数据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除任务分区
     * 使用 admin 服务提供的 /api/v1/jobParts/deleteJobPart/{id} 接口
     */
    public boolean deleteJobPart(Long partId) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobParts/deleteJobPart/" + partId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("删除任务分区失败: " + response);
            }

            String responseBody = response.body().string();
            logger.debug("deleteJobPart API 响应: {}", responseBody);

            Type resultType = new TypeToken<Result<Void>>() {
            }.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            return Result.isSuccess(result);
        }
    }

    /**
     * 删除任务组
     * 对应 admin 服务的 DELETE /api/v1/jobInfos/{id}
     */
    public boolean deleteJobInfo(Long jobInfoId) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/" + jobInfoId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("删除任务组失败: " + response);
            }

            String responseBody = response.body().string();
            logger.debug("deleteJobInfo API 响应: {}", responseBody);

            Type resultType = new TypeToken<Result<Void>>() {
            }.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            return Result.isSuccess(result);
        }
    }

    /**
     * 删除任务节点
     * 对应 admin 服务的 GET /api/v1/jobInfos/deleteJobNode/{nodeId}
     */
    public boolean deleteJobNode(Long nodeId) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobInfos/deleteJobNode/" + nodeId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("删除任务节点失败: " + response);
            }

            String responseBody = response.body().string();
            logger.debug("deleteJobNode API 响应: {}", responseBody);

            Type resultType = new TypeToken<Result<Void>>() {
            }.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            return Result.isSuccess(result);
        }
    }
    
    /**
     * 导出分区数据
     * @param partId 分区ID
     * @return 导出的字节数组
     * @throws IOException 网络异常
     */
    public byte[] exportData(Long partId) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobParts/exportData/" + partId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("导出分区数据失败: " + response);
            }

            return response.body().bytes();
        }
    }
    
    /**
     * 更新分区名称
     * @param partId 分区ID
     * @param partName 新分区名称
     * @return 是否更新成功
     * @throws IOException 网络异常
     */
    public boolean updateJobPart(Long partId, String partName) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobParts/updateJobPart";
        
        // 构建请求参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", partId);
        requestMap.put("jobPartName", partName);
        
        String jsonBody = apiUtil.getGson().toJson(requestMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("更新分区失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.debug("updateJobPart API 响应: {}", responseBody);
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }
}
