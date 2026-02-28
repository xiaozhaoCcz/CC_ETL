package com.cc.job.gui.service;

import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Locale;
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
    
    /**
     * 建议项数据类（包含名称和类型）
     */
    public static class SuggestionItem {
        private String name;
        private String type;
        
        public SuggestionItem(String name, String type) {
            this.name = name;
            this.type = type != null ? type : "Bean";
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
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
     * 获取任务描述建议列表（返回字符串列表，保持向后兼容）
     * 
     * @param prefix 前缀（用于过滤）
     * @param taskGroupId 任务组ID（可选，如果提供则从任务组中获取节点）
     * @return 任务描述列表
     */
    public List<String> getJobDescSuggestions(String prefix, Long taskGroupId) {
        List<SuggestionItem> items = getJobDescSuggestionItems(prefix, taskGroupId);
        return items.stream()
                .map(SuggestionItem::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取任务描述建议项列表（包含类型信息）
     * 
     * @param prefix 前缀（用于过滤）
     * @param taskGroupId 任务组ID（可选，如果提供则从任务组中获取节点）
     * @return 建议项列表
     */
    public List<SuggestionItem> getJobDescSuggestionItems(String prefix, Long taskGroupId) {
        List<SuggestionItem> suggestions = new ArrayList<>();
        
        try {
            // 如果提供了任务组ID，优先从任务组中获取节点
            if (taskGroupId != null) {
                suggestions.addAll(getJobDescsFromTaskGroupWithType(taskGroupId, prefix));
            }
            
            // 从任务列表中获取（作为补充）
            //suggestions.addAll(getJobDescsFromJobList(prefix));
            
            // 去重并排序
            return suggestions.stream()
                    .collect(Collectors.toMap(
                            SuggestionItem::getName,
                            item -> item,
                            (existing, replacement) -> existing))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(SuggestionItem::getName))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("获取任务描述建议失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 从任务组中获取任务描述（返回字符串列表，保持向后兼容）
     */
    private List<String> getJobDescsFromTaskGroup(Long taskGroupId, String prefix) {
        List<SuggestionItem> items = getJobDescsFromTaskGroupWithType(taskGroupId, prefix);
        return items.stream()
                .map(SuggestionItem::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * 从任务组中获取任务描述（包含类型信息）
     */
    private List<SuggestionItem> getJobDescsFromTaskGroupWithType(Long taskGroupId, String prefix) {
        try {
            JobComposeData composeData = jobPartService.getJobCompose(taskGroupId);
            if (composeData == null || composeData.getNodes() == null) {
                return new ArrayList<>();
            }
            
            return composeData.getNodes().stream()
                    .filter(node -> node.getJobName() != null && !node.getJobName().isEmpty())
                    .filter(node -> prefix == null || prefix.isEmpty() || 
                            node.getJobName().toLowerCase().contains(prefix.toLowerCase()))
                    .map(node -> {
                        String nodeType = normalizeNodeType(node.getType(), node.getProperties());
                        return new SuggestionItem(node.getJobName(), nodeType);
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.debug("从任务组获取任务描述失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 规范化节点类型（参考 CanvasDataLoader.mapNodeType 方法）
     */
    private String normalizeNodeType(String rawType, Map<String, Object> properties) {
        String candidate = rawType;
        if ((candidate == null || candidate.isBlank()) && properties != null) {
            Object glueType = properties.get("glueType");
            if (glueType instanceof String) {
                candidate = (String) glueType;
            }
        }
        if (candidate == null || candidate.isBlank()) {
            return "Bean";
        }
        
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "bean", "custom-bean" -> "Bean";
            case "api", "custom-api" -> "API";
            case "sql", "custom-sql" -> "SQL";
            case "java", "glue(java)", "custom-java" -> "Java";
            case "shell", "glue(shell)", "custom-shell" -> "Shell";
            case "python", "glue(python)", "custom-python" -> "Python";
            case "php", "glue(php)", "custom-php" -> "PHP";
            case "node", "nodejs", "glue(nodejs)", "custom-nodejs" -> "Node";
            case "powershell", "ps", "glue(powershell)", "custom-powershell" -> "PS";
            case "csharp", "c#", "glue(csharp)", "custom-csharp" -> "C#";
            default -> "Bean";
        };
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
     * @param attributePath 属性路径（如 "data" 或 "data.range"），用于链式调用
     * @return 属性列表
     */
    public List<String> getAttributeSuggestions(String prefix, String jobDesc, Long taskGroupId, String attributePath) {
        List<String> suggestions = new ArrayList<>();
        
        // 1. 获取节点类型
        String nodeType = getNodeType(jobDesc, taskGroupId);
        
        // 2. 如果提供了属性路径，获取嵌套属性
        if (attributePath != null && !attributePath.isEmpty()) {
            suggestions.addAll(getNestedAttributeSuggestions(attributePath, nodeType, prefix));
        } else {
            // 3. 根据节点类型从配置文件获取第一级属性
            suggestions.addAll(getAttributesFromConfigByType(prefix, nodeType));
            
            // 4. 从任务组中动态获取属性
            if (taskGroupId != null) {
                suggestions.addAll(getAttributesFromTaskGroup(taskGroupId, jobDesc, prefix));
            }
        }
        
        // 去重并排序
        return suggestions.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 获取属性建议（兼容旧方法，用于第一级属性补全）
     */
    public List<String> getAttributeSuggestions(String prefix, String jobDesc, Long taskGroupId) {
        return getAttributeSuggestions(prefix, jobDesc, taskGroupId, "");
    }
    
    /**
     * 获取节点类型
     */
    private String getNodeType(String jobDesc, Long taskGroupId) {
        if (jobDesc == null || jobDesc.isEmpty()) {
            return "bean"; // 默认返回bean类型
        }
        
        try {
            // 尝试从任务组中获取节点信息
            if (taskGroupId != null) {
                JobComposeData composeData = jobPartService.getJobCompose(taskGroupId);
                if (composeData != null && composeData.getNodes() != null) {
                    for (JobComposeData.NodeData node : composeData.getNodes()) {
                        if (jobDesc.equals(node.getJobName())) {
                            // 获取节点类型
                            String normalizedType = normalizeNodeType(node.getType(), node.getProperties());
                            // 转换为配置文件中的key格式
                            return mapNodeTypeToConfigKey(normalizedType);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("获取节点类型失败: {}", e.getMessage());
        }
        
        return "bean"; // 默认返回bean类型
    }
    
    /**
     * 将节点类型映射到配置文件中的key
     */
    private String mapNodeTypeToConfigKey(String nodeType) {
        if (nodeType == null) {
            return "bean";
        }
        
        String lowerType = nodeType.toLowerCase();
        // 映射到配置文件中的key
        return switch (lowerType) {
            case "sql" -> "sql";
            case "api" -> "api";
            case "bean" -> "bean";
            case "java", "shell", "python", "php", "node", "ps", "c#" -> "glue";
            default -> "bean";
        };
    }
    
    /**
     * 获取嵌套属性建议
     * 
     * @param attributePath 属性路径（如 "data" 或 "data.range"）
     * @param nodeType 节点类型
     * @param prefix 前缀（用于过滤）
     * @return 属性列表
     */
    private List<String> getNestedAttributeSuggestions(String attributePath, String nodeType, String prefix) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // 解析路径，获取最后一个属性名
            String[] pathParts = attributePath.split("\\.");
            String lastAttribute = pathParts[pathParts.length - 1];
            
            // 从配置文件读取嵌套属性（使用 UTF-8 编码避免乱码）
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream input = getClass().getClassLoader()
                    .getResourceAsStream("parameter-attributes.properties")) {
                if (input != null) {
                    // 使用 UTF-8 编码读取，避免中文乱码
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
                        props.load(reader);
                    }
                }
            }
            
            // 根据最后一个属性名和节点类型，查找对应的嵌套属性配置
            // 例如：data -> parameter.attributes.data.list 或 parameter.attributes.data.map
            // 例如：response -> parameter.attributes.response
            
            // 1. 尝试直接匹配：parameter.attributes.{lastAttribute}
            String directKey = "parameter.attributes." + lastAttribute;
            String directAttrs = props.getProperty(directKey);
            if (directAttrs != null && !directAttrs.isEmpty()) {
                suggestions.addAll(parseAttributes(directAttrs));
            }
            
            // 2. 根据节点类型尝试：parameter.attributes.{lastAttribute}.{nodeType}
            String typeKey = "parameter.attributes." + lastAttribute + "." + nodeType.toLowerCase();
            String typeAttrs = props.getProperty(typeKey);
            if (typeAttrs != null && !typeAttrs.isEmpty()) {
                suggestions.addAll(parseAttributes(typeAttrs));
            }
            
            // 3. 尝试通用类型：parameter.attributes.{lastAttribute}.list 或 .map
            // 对于 data 属性，根据节点类型推断是 List 还是 Map
            if ("data".equals(lastAttribute)) {
                // SQL 节点的 data 通常是 List
                if ("sql".equals(nodeType)) {
                    String listKey = "parameter.attributes.data.list";
                    String listAttrs = props.getProperty(listKey);
                    if (listAttrs != null && !listAttrs.isEmpty()) {
                        suggestions.addAll(parseAttributes(listAttrs));
                    }
                }
            }
            
            // 4. 如果没有找到，使用默认的嵌套属性
            if (suggestions.isEmpty()) {
                // 根据常见属性名提供默认建议
                switch (lastAttribute) {
                    case "data":
                        // data 对象的方法（List 或 Map）
                        suggestions.addAll(java.util.Arrays.asList("get", "size", "first", "last", "isEmpty", "range"));
                        break;
                    case "response":
                        // response 对象的属性
                        suggestions.addAll(java.util.Arrays.asList("body", "rawBody", "headers", "statusCode", "contentType"));
                        break;
                    case "request":
                        // request 对象的属性
                        suggestions.addAll(java.util.Arrays.asList("url", "method", "headers", "body"));
                        break;
                }
            }
            
            // 根据前缀过滤
            if (prefix != null && !prefix.isEmpty()) {
                String lowerPrefix = prefix.toLowerCase();
                suggestions = suggestions.stream()
                        .filter(attr -> attr.toLowerCase().contains(lowerPrefix))
                        .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            logger.debug("获取嵌套属性建议失败: {}", e.getMessage());
        }
        
        return suggestions;
    }
    
    /**
     * 根据节点类型从配置文件获取属性
     */
    private List<String> getAttributesFromConfigByType(String prefix, String nodeType) {
        try {
            // 先尝试从资源文件读取（使用 UTF-8 编码避免乱码）
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream input = getClass().getClassLoader()
                    .getResourceAsStream("parameter-attributes.properties")) {
                if (input != null) {
                    // 使用 UTF-8 编码读取，避免中文乱码
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
                        props.load(reader);
                    }
                }
            }
            
            List<String> attributes = new ArrayList<>();
            
            // 1. 添加通用属性
            String commonAttrs = props.getProperty("parameter.attributes.common", 
                    configManager.getProperty("parameter.attributes.common", 
                            "code,message,success,duration,timestamp"));
            attributes.addAll(parseAttributes(commonAttrs));
            
            // 2. 根据节点类型添加特定属性
            String typeKey = "parameter.attributes." + nodeType.toLowerCase();
            String typeAttrs = props.getProperty(typeKey, 
                    configManager.getProperty(typeKey, ""));
            if (!typeAttrs.isEmpty()) {
                attributes.addAll(parseAttributes(typeAttrs));
            }
            
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
     * 解析属性字符串
     */
    private List<String> parseAttributes(String attributesStr) {
        if (attributesStr == null || attributesStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(attributesStr.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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

