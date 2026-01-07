package com.cc.job.gui.service;

import com.cc.job.gui.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数自动补全服务
 * 
 * <p>提供任务描述和属性的自动补全功能
 * 
 * @author cc-job-team
 * @since 2026-01-06
 */
public class ParameterAutocompleteService {
    
    private static final Logger logger = LoggerFactory.getLogger(ParameterAutocompleteService.class);
    
    private final JobInfoService jobInfoService;
    private final JobPartService jobPartService;
    private final ConfigManager configManager;
    
    // 默认属性列表
    private static final List<String> DEFAULT_ATTRIBUTES = Arrays.asList(
        "result", "value", "list", "data", "output", "response"
    );
    
    public ParameterAutocompleteService() {
        this.jobInfoService = new JobInfoService();
        this.jobPartService = new JobPartService();
        this.configManager = ConfigManager.getInstance();
    }
    
    /**
     * 获取任务描述建议列表
     * 
     * @param prefix 前缀（用于过滤）
     * @param taskGroupId 任务组ID（可选，如果提供则从任务组中获取节点）
     * @return 任务描述列表
     */
    public List<String> getJobDescSuggestions(String prefix, Long taskGroupId) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // 如果提供了任务组ID，优先从任务组中获取节点
            if (taskGroupId != null) {
                suggestions.addAll(getJobDescsFromTaskGroup(taskGroupId, prefix));
            }
            
            // 从任务列表中获取（作为补充）
            suggestions.addAll(getJobDescsFromJobList(prefix));
            
            // 去重并排序
            return suggestions.stream()
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("获取任务描述建议失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 从任务组中获取任务描述
     */
    private List<String> getJobDescsFromTaskGroup(Long taskGroupId, String prefix) {
        try {
            com.cc.job.gui.model.JobComposeData composeData = jobPartService.getJobCompose(taskGroupId);
            if (composeData == null || composeData.getNodes() == null) {
                return new ArrayList<>();
            }
            
            return composeData.getNodes().stream()
                    .map(com.cc.job.gui.model.JobComposeData.NodeData::getJobName)
                    .filter(jobDesc -> jobDesc != null && !jobDesc.isEmpty())
                    .filter(jobDesc -> prefix == null || prefix.isEmpty() || 
                            jobDesc.toLowerCase().contains(prefix.toLowerCase()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.debug("从任务组获取任务描述失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 从任务列表中获取任务描述
     */
    private List<String> getJobDescsFromJobList(String prefix) {
        try {
            // 获取前100个任务（用于补全，不需要太多）
            com.cc.job.xo.model.query.JobInfoQuery query = new com.cc.job.xo.model.query.JobInfoQuery();
            query.setPageNum(1);
            query.setPageSize(100);
            
            if (prefix != null && !prefix.isEmpty()) {
                query.setJobDesc(prefix);
            }
            
            com.cc.job.xo.common.result.PageResult<com.cc.job.xo.model.vo.JobInfoVO> page = 
                    jobInfoService.getJobInfoPage(query);
            
            if (page != null && page.getData() != null && page.getData().getList() != null) {
                return page.getData().getList().stream()
                        .map(com.cc.job.xo.model.vo.JobInfoVO::getJobDesc)
                        .filter(Objects::nonNull)
                        .filter(jobDesc -> prefix == null || prefix.isEmpty() || 
                                jobDesc.toLowerCase().contains(prefix.toLowerCase()))
                        .distinct()
                        .collect(Collectors.toList());
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.debug("从任务列表获取任务描述失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取属性建议列表
     * 
     * @param prefix 前缀（用于过滤）
     * @param jobDesc 任务描述（可选，用于动态获取该任务的属性）
     * @param taskGroupId 任务组ID（可选，用于从任务组中获取属性）
     * @return 属性列表
     */
    public List<String> getAttributeSuggestions(String prefix, String jobDesc, Long taskGroupId) {
        List<String> suggestions = new ArrayList<>();
        
        // 1. 从配置文件获取默认属性
        suggestions.addAll(getAttributesFromConfig(prefix));
        
        // 2. 从任务组中动态获取属性
        if (taskGroupId != null) {
            suggestions.addAll(getAttributesFromTaskGroup(taskGroupId, jobDesc, prefix));
        }
        
        // 去重并排序
        return suggestions.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 从配置文件获取属性
     */
    private List<String> getAttributesFromConfig(String prefix) {
        try {
            // 先尝试从资源文件读取
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream input = getClass().getClassLoader()
                    .getResourceAsStream("parameter-attributes.properties")) {
                if (input != null) {
                    props.load(input);
                }
            }
            
            // 从配置文件读取属性列表
            String attributesStr = props.getProperty("parameter.attributes", 
                    configManager.getProperty("parameter.attributes", 
                            String.join(",", DEFAULT_ATTRIBUTES)));
            
            List<String> attributes = Arrays.asList(attributesStr.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            
            // 如果有前缀，进行过滤
            if (prefix != null && !prefix.isEmpty()) {
                return attributes.stream()
                        .filter(attr -> attr.toLowerCase().startsWith(prefix.toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            return attributes;
            
        } catch (Exception e) {
            logger.debug("从配置获取属性失败，使用默认属性: {}", e.getMessage());
            // 返回默认属性
            if (prefix != null && !prefix.isEmpty()) {
                return DEFAULT_ATTRIBUTES.stream()
                        .filter(attr -> attr.toLowerCase().startsWith(prefix.toLowerCase()))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>(DEFAULT_ATTRIBUTES);
        }
    }
    
    /**
     * 从任务组中动态获取属性
     * 这里可以根据实际需求实现，比如从节点的执行结果中提取属性
     */
    private List<String> getAttributesFromTaskGroup(Long taskGroupId, String jobDesc, String prefix) {
        // TODO: 实现从任务组中动态获取属性的逻辑
        // 可以分析节点的执行结果，提取可用的属性
        return new ArrayList<>();
    }
    
    /**
     * 过滤建议列表
     * 
     * @param suggestions 原始建议列表
     * @param prefix 前缀
     * @return 过滤后的列表
     */
    public List<String> filterSuggestions(List<String> suggestions, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return suggestions;
        }
        
        String lowerPrefix = prefix.toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().contains(lowerPrefix))
                .collect(Collectors.toList());
    }
}

