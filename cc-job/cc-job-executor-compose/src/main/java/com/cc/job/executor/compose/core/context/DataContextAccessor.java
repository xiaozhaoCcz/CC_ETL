package com.cc.job.executor.compose.core.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 数据上下文访问器
 * 
 * <p>为GLUE脚本（特别是Java GLUE/Groovy）提供访问执行上下文数据的API
 * 
 * <p>使用示例（Groovy脚本）：
 * <pre>
 * def userId = dataContext.get('task1.userId')
 * def result = dataContext.get('task1.result')
 * def data = dataContext.getObject('task1.data')
 * </pre>
 * 
 * @author cc-job-team
 * @since 2026-01-06
 */
public class DataContextAccessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DataContextAccessor.class);
    
    private final DataContext dataContext;
    private final Map<String, Long> jobNameMap;
    
    public DataContextAccessor(DataContext dataContext, Map<String, Long> jobNameMap) {
        this.dataContext = dataContext;
        this.jobNameMap = jobNameMap;
    }
    
    /**
     * 获取数据（返回字符串）
     * 
     * @param key 数据键，格式：jobName.attr 或 jobName.attr.subAttr
     * @return 数据值（字符串），如果不存在则返回null
     */
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        Object value = getObject(key);
        if (value == null) {
            return null;
        }
        
        return value.toString();
    }
    
    /**
     * 获取数据（返回对象）
     * 
     * @param key 数据键，格式：jobName.attr 或 jobName.attr.subAttr
     * @return 数据值（对象），如果不存在则返回null
     */
    public Object getObject(String key) {
        if (key == null || key.isEmpty() || dataContext == null) {
            return null;
        }
        
        // 解析键：jobName.attr1.attr2
        String[] parts = key.split("\\.", 2);
        if (parts.length < 2) {
            logger.warn("[DataContextAccessor] 键格式错误，需要至少包含 jobName.attr: {}", key);
            return null;
        }
        
        String jobName = parts[0];
        String attrPath = parts[1];
        
        // 规范化 jobName
        String normalizedJobName = normalizeJobName(jobName);
        
        // 构建数据键：jobName.attrPath
        String dataKey = normalizedJobName + "." + attrPath;
        
        // 从上下文获取值
        Object value = dataContext.get(dataKey);
        
        // 如果直接获取失败，尝试其他可能的键名
        if (value == null) {
            // 尝试 jobName.result（如果 attrPath 是 result）
            if ("result".equals(attrPath)) {
                value = dataContext.get(normalizedJobName + ".result");
            }
            // 尝试 jobName.value（如果 attrPath 是 value）
            if (value == null && "value".equals(attrPath)) {
                value = dataContext.get(normalizedJobName + ".value");
            }
        }
        
        // 如果值存在，可能需要进一步解析嵌套属性
        if (value != null && attrPath.contains(".")) {
            value = resolveNestedAttribute(value, attrPath);
        }
        
        // 如果值存在，可能需要解析数组索引
        if (value != null && attrPath.contains("[")) {
            value = resolveArrayIndex(value, attrPath);
        }
        
        return value;
    }
    
    /**
     * 获取数据（返回指定类型）
     * 
     * @param key 数据键
     * @param clazz 目标类型
     * @return 数据值（指定类型），如果不存在或类型不匹配则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = getObject(key);
        if (value == null) {
            return null;
        }
        
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        
        // 尝试类型转换
        try {
            if (clazz == String.class) {
                return (T) value.toString();
            } else if (clazz == Integer.class && value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            } else if (clazz == Long.class && value instanceof Number) {
                return (T) Long.valueOf(((Number) value).longValue());
            } else if (clazz == Double.class && value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            } else if (clazz == Boolean.class) {
                return (T) Boolean.valueOf(value.toString());
            }
        } catch (Exception e) {
            logger.warn("[DataContextAccessor] 类型转换失败: key={}, targetType={}", key, clazz.getName(), e);
        }
        
        return null;
    }
    
    /**
     * 检查数据是否存在
     * 
     * @param key 数据键
     * @return 如果存在则返回true
     */
    public boolean contains(String key) {
        return getObject(key) != null;
    }
    
    /**
     * 获取所有可用的键
     * 
     * @return 键集合
     */
    public java.util.Set<String> keySet() {
        if (dataContext == null) {
            return java.util.Collections.emptySet();
        }
        return dataContext.keySet();
    }
    
    /**
     * 解析嵌套属性
     */
    private Object resolveNestedAttribute(Object value, String attrPath) {
        if (value == null || attrPath == null) {
            return value;
        }
        
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            
            String[] pathParts = attrPath.split("\\.");
            Object current = map;
            
            for (String part : pathParts) {
                if (part.contains("[")) {
                    current = resolveArrayIndex(current, part);
                } else if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMap = (Map<String, Object>) current;
                    current = currentMap.get(part);
                } else {
                    return null;
                }
                
                if (current == null) {
                    return null;
                }
            }
            
            return current;
        }
        
        return value;
    }
    
    /**
     * 解析数组索引
     */
    private Object resolveArrayIndex(Object value, String attrPath) {
        if (value == null || attrPath == null) {
            return value;
        }
        
        int bracketIndex = attrPath.indexOf('[');
        if (bracketIndex < 0) {
            return value;
        }
        
        int closeBracketIndex = attrPath.indexOf(']', bracketIndex);
        if (closeBracketIndex < 0) {
            return value;
        }
        
        String indexStr = attrPath.substring(bracketIndex + 1, closeBracketIndex);
        try {
            int index = Integer.parseInt(indexStr);
            
            if (value instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> list = (java.util.List<Object>) value;
                if (index >= 0 && index < list.size()) {
                    return list.get(index);
                }
            } else if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                if (index >= 0 && index < array.length) {
                    return array[index];
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("[DataContextAccessor] 无效的数组索引: {}", indexStr);
        }
        
        return null;
    }
    
    /**
     * 规范化任务名称
     */
    private String normalizeJobName(String jobName) {
        if (jobName == null || jobName.isEmpty()) {
            return "";
        }
        
        String normalized = jobName.trim();
        normalized = normalized.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        normalized = normalized.replaceAll("_{2,}", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        
        return normalized;
    }
}

