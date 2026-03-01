package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.vo.JobPartVo;
import com.cc.job.gui.model.JobComposeData;
import com.google.gson.reflect.TypeToken;
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
        TypeToken<List<JobPartVo>> typeToken = new TypeToken<List<JobPartVo>>(){};
        Result<List<JobPartVo>> result = httpClient.get("/api/v1/jobParts/getTree", typeToken);
        return httpClient.extractData(result, "获取树形数据失败");
    }

    /**
     * 获取子节点数据
     * @param id 父节点ID
     * @param type 类型
     * @return 子节点数据
     * @throws IOException 网络异常
     */
    public Object getChildren(Long id, Integer type) throws IOException {
        String path = "/api/v1/jobParts/getChildren/" + id + "/" + type;
        Result<Object> result = httpClient.get(path, Object.class);
        return httpClient.extractDataOrNull(result, "获取子节点数据失败");
    }
    
    /**
     * 保存分区数据
     * @param jobPartName 分区名称
     * @return 是否保存成功
     * @throws IOException 网络异常
     */
    public boolean saveJobPart(String jobPartName) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("jobPartName", jobPartName);
        requestMap.put("sort", 0); // 默认排序为0
        return httpClient.postForBoolean("/api/v1/jobParts/saveJobPart", requestMap);
    }
    
    /**
     * 获取任务组合数据（节点和边）
     * @param jobId 任务组ID
     * @return 任务组合数据
     * @throws IOException 网络异常
     */
    public JobComposeData getJobCompose(Long jobId) throws IOException {
        // 构建请求参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", jobId);
        requestMap.put("type", 0);  // 添加type参数，0表示普通加载
        // 注意：不要传递x和y参数，否则会导致后端调整节点位置
        
        TypeToken<Map<String, Object>> typeToken = new TypeToken<Map<String, Object>>(){};
        Result<Map<String, Object>> result = httpClient.post("/api/v1/jobInfos/getJobCompose", requestMap, typeToken);
        
        Map<String, Object> data = httpClient.extractData(result, "获取任务组合数据失败");
        return parseJobComposeData(data);
    }

    private JobComposeData parseJobComposeData(Map<String, Object> data) {
        JobComposeData composeData = new JobComposeData();

//        Object jobNodeObj = data.get("jobNode");
//        if (jobNodeObj instanceof Map) {
//            Map<?, ?> jnMap = (Map<?, ?>) jobNodeObj;
//            JobComposeData.NodeData jobNode = new JobComposeData.NodeData();
//            jobNode.setId(String.valueOf(jnMap.get("id")));
//            jobNode.setType(String.valueOf(jnMap.get("nodeType")));
//            jobNode.setJobName(String.valueOf(jnMap.get("jobName")));
//            if (jnMap.get("jobId") != null) {
//                try {
//                    jobNode.setJobId(((Number) jnMap.get("jobId")).longValue());
//                } catch (Exception ignored) {}
//            }
//            if (jnMap.get("nodePositionX") != null) {
//                jobNode.setX(((Number) jnMap.get("nodePositionX")).doubleValue());
//            }
//            if (jnMap.get("nodePositionY") != null) {
//                jobNode.setY(((Number) jnMap.get("nodePositionY")).doubleValue());
//            }
//            Map<String, Object> jnProps = new HashMap<>();
//            Object jnPropsObj = jnMap.get("properties");
//            if (jnPropsObj instanceof String) {
//                try {
//                    jnProps = apiUtil.getGson().fromJson(
//                            (String) jnPropsObj,
//                            new TypeToken<Map<String, Object>>(){}.getType()
//                    );
//                } catch (Exception e) {
//                    logger.error("解析 jobNode.properties 失败: {}", e.getMessage(), e);
//                }
//            } else if (jnPropsObj instanceof Map) {
//                //noinspection unchecked
//                jnProps = (Map<String, Object>) jnPropsObj;
//            }
//            Object jnChildren = jnMap.get("children");
//            if (jnChildren != null) {
//                jnProps.put("children", jnChildren);
//            }
//            jobNode.setProperties(jnProps);
//            composeData.setJobNode(jobNode);
//        }

        Object nodesObj = data.get("nodes");
        if (nodesObj instanceof List) {
            List<JobComposeData.NodeData> nodeList = new ArrayList<>();
            List<?> nodes = (List<?>) nodesObj;

            for (Object nodeObj : nodes) {
                if (!(nodeObj instanceof Map nodeMap)) {
                    continue;
                }
                JobComposeData.NodeData node = new JobComposeData.NodeData();
                node.setId(String.valueOf(nodeMap.get("id")));
                Object nodeTypeObj = nodeMap.get("nodeType");
                if (nodeTypeObj != null && !"null".equals(String.valueOf(nodeTypeObj))) {
                    node.setType(String.valueOf(nodeTypeObj));
                } else {
                    node.setType(null);
                }
                node.setJobName(String.valueOf(nodeMap.get("jobName")));

                if (nodeMap.get("jobId") != null) {
                    try {
                        node.setJobId(((Number) nodeMap.get("jobId")).longValue());
                    } catch (Exception e) {
                        logger.error("解析节点 jobId 失败: {}", e.getMessage(), e);
                    }
                }

                if (nodeMap.get("jobParentId") != null) {
                    try {
                        node.setJobParentId(((Number) nodeMap.get("jobParentId")).longValue());
                    } catch (Exception e) {
                        logger.error("解析节点 jobParentId 失败: {}", e.getMessage(), e);
                    }
                }

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
                        logger.error("解析节点 triggerStatus 失败: {}, 错误: {}", triggerObj, e.getMessage(), e);
                        node.setTriggerStatus(null);
                    }
                }

                if (nodeMap.get("nodePositionX") != null) {
                    node.setX(((Number) nodeMap.get("nodePositionX")).doubleValue());
                }
                if (nodeMap.get("nodePositionY") != null) {
                    node.setY(((Number) nodeMap.get("nodePositionY")).doubleValue());
                }

                Object propsObj = nodeMap.get("properties");
                Map<String, Object> propsMap;
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

                Object childrenObj = nodeMap.get("children");
                if (childrenObj != null) {
                    propsMap.put("children", childrenObj);
                }

                Object childrenNodesObj = nodeMap.get("childrenNodes");
                if (childrenNodesObj instanceof List childrenNodesList0) {
                    List<JobComposeData.NodeData> childrenNodesList = new ArrayList<>();
                    for (Object childNodeObj : childrenNodesList0) {
                        if (childNodeObj instanceof Map childNodeMap) {
                            JobComposeData.NodeData childNode = parseNodeData(childNodeMap);
                            if (childNode != null) {
                                childrenNodesList.add(childNode);
                            }
                        }
                    }
                    node.setChildrenNodes(childrenNodesList);
                }

                node.setProperties(propsMap);
                nodeList.add(node);
            }
            composeData.setNodes(nodeList);
        }

        Object edgesObj = data.get("edges");
        if (edgesObj instanceof List) {
            List<JobComposeData.EdgeData> edgeList = new ArrayList<>();
            List<?> edges = (List<?>) edgesObj;

            for (Object edgeObj : edges) {
                if (!(edgeObj instanceof Map edgeMap)) {
                    continue;
                }
                JobComposeData.EdgeData edge = new JobComposeData.EdgeData();
                edge.setId(String.valueOf(edgeMap.get("id")));
                edge.setSourceNodeId(String.valueOf(edgeMap.get("fromNodeId")));
                edge.setTargetNodeId(String.valueOf(edgeMap.get("endNodeId")));
                edge.setSourceAnchor(String.valueOf(edgeMap.get("startPoint")));
                edge.setTargetAnchor(String.valueOf(edgeMap.get("endPoint")));
                edge.setType(String.valueOf(edgeMap.get("type")));

                Object propsObj = edgeMap.get("properties");
                if (propsObj instanceof String) {
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
            composeData.setEdges(edgeList);
        }

        return composeData;
    }

    /**
     * 递归解析节点数据（用于嵌套任务组）
     */
    private JobComposeData.NodeData parseNodeData(Map<?, ?> nodeMap) {
        JobComposeData.NodeData node = new JobComposeData.NodeData();

        try {
            node.setId(String.valueOf(nodeMap.get("id")));

            Object nodeTypeObj = nodeMap.get("nodeType");
            if (nodeTypeObj != null && !"null".equals(String.valueOf(nodeTypeObj))) {
                node.setType(String.valueOf(nodeTypeObj));
            } else {
                node.setType(null);
            }

            node.setJobName(String.valueOf(nodeMap.get("jobName")));

            if (nodeMap.get("jobId") != null) {
                try {
                    node.setJobId(((Number) nodeMap.get("jobId")).longValue());
                } catch (Exception e) {
                    logger.error("解析节点 jobId 失败: {}", e.getMessage(), e);
                }
            }

            if (nodeMap.get("jobParentId") != null) {
                try {
                    node.setJobParentId(((Number) nodeMap.get("jobParentId")).longValue());
                } catch (Exception e) {
                    logger.error("解析节点 jobParentId 失败: {}", e.getMessage(), e);
                }
            }

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

            if (nodeMap.get("nodePositionX") != null) {
                node.setX(((Number) nodeMap.get("nodePositionX")).doubleValue());
            }
            if (nodeMap.get("nodePositionY") != null) {
                node.setY(((Number) nodeMap.get("nodePositionY")).doubleValue());
            }

            Object propsObj = nodeMap.get("properties");
            Map<String, Object> propsMap;
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

            Object childrenObj = nodeMap.get("children");
            if (childrenObj != null) {
                propsMap.put("children", childrenObj);
            }

            Object childrenNodesObj = nodeMap.get("childrenNodes");
            if (childrenNodesObj instanceof List childrenNodesList0) {
                List<JobComposeData.NodeData> childrenNodesList = new ArrayList<>();
                for (Object childNodeObj : childrenNodesList0) {
                    if (childNodeObj instanceof Map childNodeMap) {
                        JobComposeData.NodeData childNode = parseNodeData(childNodeMap);
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
        Result<Void> result = httpClient.get("/api/v1/jobParts/deleteJobPart/" + partId, Void.class);
        return Result.isSuccess(result);
    }

    /**
     * 删除任务组
     * 对应 admin 服务的 DELETE /api/v1/jobInfos/{id}
     */
    public boolean deleteJobInfo(Long jobInfoId) throws IOException {
        return httpClient.deleteForBoolean("/api/v1/jobInfos/" + jobInfoId);
    }

    /**
     * 删除任务节点
     * 对应 admin 服务的 GET /api/v1/jobInfos/deleteJobNode/{nodeId}
     */
    public boolean deleteJobNode(Long nodeId) throws IOException {
        Result<Void> result = httpClient.get("/api/v1/jobInfos/deleteJobNode/" + nodeId, Void.class);
        return Result.isSuccess(result);
    }
    
    /**
     * 导出分区数据
     * @param partId 分区ID
     * @return 导出的字节数组
     * @throws IOException 网络异常
     */
    public byte[] exportData(Long partId) throws IOException {
        // 这个方法需要直接返回字节数组，不能使用通用的工具类
        // 保留原有实现，但需要添加import
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(httpClient.buildUrl("/api/v1/jobParts/exportData/" + partId))
                .get()
                .build();

        try (okhttp3.Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("导出分区数据失败: " + response);
            }
            return response.body().bytes();
        }
    }

    /**
     * 导出任务组数据（.cel 格式）
     * @param jobId 任务组ID
     * @return 导出的字节数组
     * @throws IOException 网络异常
     */
    public byte[] exportTaskGroupData(Long jobId) throws IOException {
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(httpClient.buildUrl("/api/v1/jobParts/exportTaskGroup/" + jobId))
                .get()
                .build();
        try (okhttp3.Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("导出任务组数据失败: " + response);
            }
            return response.body() != null ? response.body().bytes() : new byte[0];
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
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", partId);
        requestMap.put("jobPartName", partName);
        return httpClient.postForBoolean("/api/v1/jobParts/updateJobPart", requestMap);
    }
    
    /**
     * 导入分区数据
     * @param file 导入的文件
     * @return 是否导入成功
     * @throws IOException 网络异常
     */
    public boolean importData(java.io.File file) throws IOException {
        // 这个方法需要multipart/form-data，不能使用通用的工具类
        // 保留原有实现，但需要添加import
        String url = httpClient.buildUrl("/api/v1/jobParts/importData");
        
        // 读取文件内容
        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
        
        // 构建multipart请求
        okhttp3.MultipartBody.Builder builder = new okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM);
        
        okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(
            fileBytes, 
            okhttp3.MediaType.parse("application/octet-stream")
        );
        
        builder.addFormDataPart("file", file.getName(), fileBody);
        okhttp3.RequestBody requestBody = builder.build();
        
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        
        try (okhttp3.Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("导入分区数据失败: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            
            // 解析 JSON 响应
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            return Result.isSuccess(result);
        }
    }

    /**
     * 导入任务组到指定分区
     * @param partitionId 分区ID
     * @param file .cel 文件
     * @return 是否导入成功
     * @throws IOException 网络异常
     */
    public boolean importTaskGroup(Long partitionId, java.io.File file) throws IOException {
        String url = httpClient.buildUrl("/api/v1/jobParts/importTaskGroup?partitionId=" + partitionId);

        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
        okhttp3.MultipartBody.Builder builder = new okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM);
        okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(
                fileBytes,
                okhttp3.MediaType.parse("application/octet-stream")
        );
        builder.addFormDataPart("file", file.getName(), fileBody);
        okhttp3.RequestBody requestBody = builder.build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (okhttp3.Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("导入任务组失败: " + response.code() + " - " + errorBody);
            }
            String responseBody = response.body() != null ? response.body().string() : "";
            Type resultType = new TypeToken<Result<Void>>(){}.getType();
            Result<Void> result = apiUtil.getGson().fromJson(responseBody, resultType);
            return Result.isSuccess(result);
        }
    }

    /**
     * 从 GUI 保存的快照 JSON 解析为 JobComposeData（用于版本回滚）
     * 格式：nodes 为 [{id, type, x, y, properties}], edges 为 [{sourceNodeId, targetNodeId, startPoint, endPoint, properties}]
     */
    public JobComposeData parseComposeFromGuiSnapshot(String nodesJson, String edgesJson) {
        JobComposeData composeData = new JobComposeData();
        if (nodesJson != null && !nodesJson.isEmpty()) {
            try {
                List<Map<String, Object>> nodeMaps = apiUtil.getGson().fromJson(
                    nodesJson, new TypeToken<List<Map<String, Object>>>(){}.getType());
                if (nodeMaps != null) {
                    List<JobComposeData.NodeData> nodeList = new ArrayList<>();
                    for (Map<String, Object> m : nodeMaps) {
                        JobComposeData.NodeData node = parseNodeFromGuiSnapshot(m);
                        if (node != null) nodeList.add(node);
                    }
                    composeData.setNodes(nodeList);
                }
            } catch (Exception e) {
                logger.error("解析快照节点 JSON 失败: {}", e.getMessage(), e);
            }
        }
        if (edgesJson != null && !edgesJson.isEmpty()) {
            try {
                List<Map<String, Object>> edgeMaps = apiUtil.getGson().fromJson(
                    edgesJson, new TypeToken<List<Map<String, Object>>>(){}.getType());
                if (edgeMaps != null) {
                    List<JobComposeData.EdgeData> edgeList = new ArrayList<>();
                    for (Map<String, Object> m : edgeMaps) {
                        JobComposeData.EdgeData edge = new JobComposeData.EdgeData();
                        edge.setId(String.valueOf(m.get("sourceNodeId")) + "->" + m.get("targetNodeId"));
                        edge.setSourceNodeId(String.valueOf(m.get("sourceNodeId")));
                        edge.setTargetNodeId(String.valueOf(m.get("targetNodeId")));
                        edge.setSourceAnchor(m.get("startPoint") != null ? String.valueOf(m.get("startPoint")) : "right");
                        edge.setTargetAnchor(m.get("endPoint") != null ? String.valueOf(m.get("endPoint")) : "left");
                        Object props = m.get("properties");
                        if (props instanceof String) {
                            try {
                                edge.setProperties(apiUtil.getGson().fromJson((String) props, new TypeToken<Map<String, Object>>(){}.getType()));
                            } catch (Exception ignored) {}
                        } else if (props instanceof Map) {
                            edge.setProperties((Map<String, Object>) props);
                        }
                        edgeList.add(edge);
                    }
                    composeData.setEdges(edgeList);
                }
            } catch (Exception e) {
                logger.error("解析快照边 JSON 失败: {}", e.getMessage(), e);
            }
        }
        return composeData;
    }

    private JobComposeData.NodeData parseNodeFromGuiSnapshot(Map<String, Object> m) {
        try {
            JobComposeData.NodeData node = new JobComposeData.NodeData();
            node.setId(String.valueOf(m.get("id")));
            node.setType(m.get("type") != null ? String.valueOf(m.get("type")) : null);
            if (m.get("x") != null) node.setX(((Number) m.get("x")).doubleValue());
            if (m.get("y") != null) node.setY(((Number) m.get("y")).doubleValue());
            Object props = m.get("properties");
            Map<String, Object> propsMap = null;
            if (props instanceof String) {
                propsMap = apiUtil.getGson().fromJson((String) props, new TypeToken<Map<String, Object>>(){}.getType());
                node.setProperties(propsMap);
            } else if (props instanceof Map) {
                propsMap = (Map<String, Object>) props;
                node.setProperties(propsMap);
            }
            if (propsMap != null && propsMap.get("jobId") != null) {
                try {
                    node.setJobId(((Number) propsMap.get("jobId")).longValue());
                } catch (Exception ignored) {}
            }
            return node;
        } catch (Exception e) {
            logger.error("解析快照节点项失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
