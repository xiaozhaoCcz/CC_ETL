package com.cc.job.gui.manager;

import com.cc.job.gui.model.ProcessNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 节点搜索管理器 - 负责高级搜索功能
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class NodeSearchManager {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeSearchManager.class);
    
    private final Consumer<String> loggerCallback;
    
    public NodeSearchManager(Consumer<String> loggerCallback) {
        this.loggerCallback = loggerCallback;
    }
    
    /**
     * 搜索结果
     */
    public static class SearchResult {
        private ProcessNode node;
        private double matchScore; // 匹配分数（0-1）
        private List<String> matchReasons; // 匹配原因
        
        public SearchResult(ProcessNode node, double matchScore, List<String> matchReasons) {
            this.node = node;
            this.matchScore = matchScore;
            this.matchReasons = matchReasons;
        }
        
        public ProcessNode getNode() {
            return node;
        }
        
        public double getMatchScore() {
            return matchScore;
        }
        
        public List<String> getMatchReasons() {
            return matchReasons;
        }
    }
    
    /**
     * 搜索条件
     */
    public static class SearchCriteria {
        private String keyword; // 关键字（模糊搜索）
        private String nodeType; // 节点类型
        private ProcessNode.NodeStatus nodeStatus; // 节点状态
        private List<String> tags; // 标签列表
        private String remarkKeyword; // 备注关键字（在备注中搜索）
        
        public String getKeyword() {
            return keyword;
        }
        
        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
        
        public String getNodeType() {
            return nodeType;
        }
        
        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }
        
        public ProcessNode.NodeStatus getNodeStatus() {
            return nodeStatus;
        }
        
        public void setNodeStatus(ProcessNode.NodeStatus nodeStatus) {
            this.nodeStatus = nodeStatus;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        public String getRemarkKeyword() {
            return remarkKeyword;
        }
        
        public void setRemarkKeyword(String remarkKeyword) {
            this.remarkKeyword = remarkKeyword;
        }
    }
    
    /**
     * 执行搜索
     *
     * @param nodes 节点列表
     * @param criteria 搜索条件
     * @return 搜索结果列表（按匹配分数排序）
     */
    public List<SearchResult> search(List<ProcessNode> nodes, SearchCriteria criteria) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SearchResult> results = new ArrayList<>();
        
        for (ProcessNode node : nodes) {
            double score = 0.0;
            List<String> reasons = new ArrayList<>();
            
            // 关键字搜索（节点名称）
            if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
                String nodeName = node.getJobHandlerName();
                if (nodeName != null && nodeName.toLowerCase().contains(criteria.getKeyword().toLowerCase())) {
                    score += 0.4;
                    reasons.add("名称匹配: " + criteria.getKeyword());
                }
            }
            
            // 节点类型搜索
            if (criteria.getNodeType() != null && !criteria.getNodeType().trim().isEmpty()) {
                if (criteria.getNodeType().equalsIgnoreCase(node.getType())) {
                    score += 0.2;
                    reasons.add("类型匹配: " + criteria.getNodeType());
                }
            }
            
            // 节点状态搜索
            if (criteria.getNodeStatus() != null) {
                if (node.getStatus() == criteria.getNodeStatus()) {
                    score += 0.2;
                    reasons.add("状态匹配: " + criteria.getNodeStatus());
                }
            }
            
            // 标签搜索
            if (criteria.getTags() != null && !criteria.getTags().isEmpty()) {
                List<String> nodeTags = node.getTags();
                for (String searchTag : criteria.getTags()) {
                    if (nodeTags.contains(searchTag)) {
                        score += 0.1;
                        reasons.add("标签匹配: " + searchTag);
                    }
                }
            }
            
            // 备注搜索
            if (criteria.getRemarkKeyword() != null && !criteria.getRemarkKeyword().trim().isEmpty()) {
                String remark = node.getRemark();
                if (remark != null && remark.toLowerCase().contains(criteria.getRemarkKeyword().toLowerCase())) {
                    score += 0.1;
                    reasons.add("备注匹配: " + criteria.getRemarkKeyword());
                }
            }
            
            // 如果匹配分数大于0，添加到结果列表
            if (score > 0) {
                results.add(new SearchResult(node, score, reasons));
            }
        }
        
        // 按匹配分数排序（降序）
        results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        
        return results;
    }
    
    /**
     * 模糊搜索（仅按名称）
     *
     * @param nodes 节点列表
     * @param keyword 关键字
     * @return 匹配的节点列表
     */
    public List<ProcessNode> fuzzySearch(List<ProcessNode> nodes, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return nodes.stream()
                .filter(node -> {
                    String nodeName = node.getJobHandlerName();
                    return nodeName != null && nodeName.toLowerCase().contains(lowerKeyword);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 按类型搜索
     *
     * @param nodes 节点列表
     * @param nodeType 节点类型
     * @return 匹配的节点列表
     */
    public List<ProcessNode> searchByType(List<ProcessNode> nodes, String nodeType) {
        if (nodeType == null || nodeType.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return nodes.stream()
                .filter(node -> nodeType.equalsIgnoreCase(node.getType()))
                .collect(Collectors.toList());
    }
    
    /**
     * 按状态搜索
     *
     * @param nodes 节点列表
     * @param status 节点状态
     * @return 匹配的节点列表
     */
    public List<ProcessNode> searchByStatus(List<ProcessNode> nodes, ProcessNode.NodeStatus status) {
        if (status == null) {
            return new ArrayList<>();
        }
        
        return nodes.stream()
                .filter(node -> node.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * 按标签搜索
     *
     * @param nodes 节点列表
     * @param tag 标签
     * @return 匹配的节点列表
     */
    public List<ProcessNode> searchByTag(List<ProcessNode> nodes, String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return nodes.stream()
                .filter(node -> {
                    List<String> tags = node.getTags();
                    return tags != null && tags.contains(tag);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 在备注中搜索
     *
     * @param nodes 节点列表
     * @param keyword 关键字
     * @return 匹配的节点列表
     */
    public List<ProcessNode> searchInRemark(List<ProcessNode> nodes, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return nodes.stream()
                .filter(node -> {
                    String remark = node.getRemark();
                    return remark != null && remark.toLowerCase().contains(lowerKeyword);
                })
                .collect(Collectors.toList());
    }
    
    private void log(String message) {
        if (loggerCallback != null) {
            loggerCallback.accept(message);
        }
        logger.info(message);
    }
}
