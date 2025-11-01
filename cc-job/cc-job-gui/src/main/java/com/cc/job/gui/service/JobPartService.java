package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.JobPartVo;
import com.cc.job.gui.model.JobComposeData;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class JobPartService extends  BaseService {

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
            System.out.println("API 响应: " + responseBody);

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
            System.out.println("API 响应: " + responseBody);

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
            System.out.println("saveJobPart API 响应: " + responseBody);
            
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
            System.out.println("getJobCompose API 响应: " + responseBody);
            
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
                    node.setType(String.valueOf(nodeMap.get("nodeType")));  // 后端字段是nodeType
                    node.setJobName(String.valueOf(nodeMap.get("jobName")));  // 节点显示名称
                    
                    // ⭐ 关键修复：解析 jobId 字段
                    if (nodeMap.get("jobId") != null) {
                        try {
                            node.setJobId(((Number) nodeMap.get("jobId")).longValue());
                        } catch (Exception e) {
                            System.err.println("解析节点 jobId 失败: " + e.getMessage());
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
                    if (propsObj instanceof String) {
                        // 如果是字符串，需要解析为Map
                        try {
                            Map<String, Object> propsMap = apiUtil.getGson().fromJson(
                                (String) propsObj, 
                                new TypeToken<Map<String, Object>>(){}.getType()
                            );
                            node.setProperties(propsMap);
                        } catch (Exception e) {
                            System.err.println("解析节点属性失败: " + e.getMessage());
                        }
                    } else if (propsObj instanceof Map) {
                        node.setProperties((Map<String, Object>) propsObj);
                    }
                    
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
                            System.err.println("解析边属性失败: " + e.getMessage());
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
}
