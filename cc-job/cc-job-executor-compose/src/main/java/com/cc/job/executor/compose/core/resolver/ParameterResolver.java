package com.cc.job.executor.compose.core.resolver;

import cn.hutool.json.JSONUtil;
import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.context.DataSourceType;
import com.cc.job.xo.model.result.NodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数解析器
 * 
 * <p>负责解析和替换参数中的变量引用，支持 #{jobName}.attr 语法
 * 
 * <p>支持的语法：
 * <ul>
 *   <li>#{jobName}.attr - 获取 jobName 任务的 attr 属性</li>
 *   <li>#{jobName}.data.userId - 支持嵌套属性访问</li>
 *   <li>#{jobName}.list[0] - 支持数组/列表索引访问</li>
 *   <li>#{jobName}.result - 获取整个结果（如果结果是简单类型）</li>
 * </ul>
 * 
 * @author cc-job-team
 */
@Component
public class ParameterResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(ParameterResolver.class);
    
    /** 变量匹配模式：匹配 #{jobName}.attr 或 #{jobName}.attr.subAttr 等，支持方法调用 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("#\\{([^}]+)\\}(\\.[^\\s}()]+(?:\\([^)]*\\))?)+");
    
    /**
     * 解析并替换模板字符串中的所有变量
     * 
     * @param template 模板字符串，可能包含 #{jobName}.attr 变量
     * @param context 数据上下文
     * @param jobNameMap jobName 到 jobId 的映射（用于查找任务）
     * @return 替换后的字符串
     */
    public String resolve(String template, DataContext context, Map<String, Long> jobNameMap) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        
        if (context == null) {
            logger.warn("[ParameterResolver] 数据上下文为空，无法解析变量");
            return template;
        }
        
        // 提取所有变量
        Set<String> variables = extractVariables(template);
        if (variables.isEmpty()) {
            return template;
        }
        
        // 替换每个变量
        String result = template;
        for (String variable : variables) {
            String value = getValueFromContext(variable, context, jobNameMap);
            if (value != null) {
                // variable 格式：jobName.attr，需要替换 #{jobName}.attr
                String[] parts = variable.split("\\.", 2);
                if (parts.length >= 2) {
                    String jobName = parts[0];
                    String attrPath = parts[1];
                    String fullExpression = "#{" + jobName + "}." + attrPath;
                    result = result.replace(fullExpression, value);
                    logger.debug("[ParameterResolver] 替换变量: #{{{}}} = {}", variable, value);
                }
            } else {
                logger.warn("[ParameterResolver] 变量 #{{{}}} 未找到值，保持原值", variable);
            }
        }
        
        return result;
    }
    
    /**
     * 提取模板字符串中的所有变量
     * 
     * @param template 模板字符串
     * @return 变量集合（不包含 # 符号）
     */
    public Set<String> extractVariables(String template) {
        Set<String> variables = new LinkedHashSet<>();
        
        if (template == null || template.isEmpty()) {
            return variables;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            // 移除开头的 #{ 和结尾的 }，保留 jobName.attr 部分
            // fullMatch 格式：#{jobName}.attr
            // 需要提取：jobName.attr
            String jobName = matcher.group(1);  // 任务名称
            String attrPath = matcher.group(2); // 属性路径（包含开头的 .）
            String variable = jobName + attrPath;  // 组合为 jobName.attr
            variables.add(variable);
        }
        
        return variables;
    }
    
    /**
     * 从数据上下文中获取变量的值
     * 
     * @param variable 变量名（格式：jobName.attr 或 jobName.attr.subAttr）
     * @param context 数据上下文
     * @param jobNameMap jobName 到 jobId 的映射
     * @return 变量的值（转换为字符串），如果不存在则返回 null
     */
    public String getValueFromContext(String variable, DataContext context, Map<String, Long> jobNameMap) {
        if (variable == null || variable.isEmpty()) {
            return null;
        }
        
        // 解析变量：jobName.attr1.attr2 或 jobName.list[0]
        String[] parts = variable.split("\\.", 2);
        if (parts.length < 2) {
            logger.warn("[ParameterResolver] 变量格式错误，需要至少包含 jobName.attr: {}", variable);
            return null;
        }
        
        String jobName = parts[0];
        String attrPath = parts[1];
        
        // 规范化 jobName（去除特殊字符，转换为小写）
        String normalizedJobName = normalizeJobName(jobName);
        
        // 检查是否包含方法调用
        boolean hasMethodCall = attrPath.contains("(");
        
        // 如果包含方法调用，需要先解析到方法调用的位置
        if (hasMethodCall) {
            // 找到第一个方法调用的位置
            int methodIndex = attrPath.indexOf('(');
            String beforeMethod = attrPath.substring(0, methodIndex);
            String methodPart = attrPath.substring(methodIndex);
            
            // 先解析方法调用之前的部分
            String baseKey = normalizedJobName + "." + beforeMethod;
            Object baseValue = context.get(baseKey);
            
            // 如果直接获取失败，尝试其他可能的键名
            if (baseValue == null) {
                // 尝试 jobName.result（如果 beforeMethod 是 result）
                if ("result".equals(beforeMethod)) {
                    baseValue = context.get(normalizedJobName + ".result");
                }
                // 尝试 jobName.value（如果 beforeMethod 是 value）
                if (baseValue == null && "value".equals(beforeMethod)) {
                    baseValue = context.get(normalizedJobName + ".value");
                }
                // 尝试 jobName.data（如果 beforeMethod 是 data）
                if (baseValue == null && "data".equals(beforeMethod)) {
                    baseValue = context.get(normalizedJobName + ".data");
                }
            }
            
            // 如果baseValue是NodeResult，需要进一步解析
            if (baseValue != null) {
                baseValue = resolveNodeResultAttribute(baseValue, beforeMethod);
            }
            
            // 如果值存在，可能需要进一步解析嵌套属性（在方法调用之前）
            if (baseValue != null && beforeMethod.contains(".")) {
                baseValue = resolveNestedAttribute(baseValue, beforeMethod);
            }
            
            // 执行方法调用
            if (baseValue != null) {
                return convertToString(executeMethod(baseValue, methodPart));
            }
        } else {
            // 没有方法调用，使用原有逻辑
            // 构建数据键：jobName.attrPath
            String dataKey = normalizedJobName + "." + attrPath;
            
            // 从上下文获取值
            Object value = context.get(dataKey);
            
            // 如果直接获取失败，尝试其他可能的键名
            if (value == null) {
                // 尝试 jobName.result（如果 attrPath 是 result）
                if ("result".equals(attrPath)) {
                    value = context.get(normalizedJobName + ".result");
                }
                // 尝试 jobName.value（如果 attrPath 是 value）
                if (value == null && "value".equals(attrPath)) {
                    value = context.get(normalizedJobName + ".value");
                }
                // 尝试直接获取jobName（可能是NodeResult对象）
                if (value == null) {
                    value = context.get(normalizedJobName);
                    if (value != null) {
                        value = resolveNodeResultAttribute(value, attrPath);
                    }
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
            
            // 转换为字符串
            if (value != null) {
                // 记录数据来源类型（用于调试和追踪）
                DataSourceType sourceType = context.getDataSourceType(dataKey);
                if (sourceType != null) {
                    if (sourceType == DataSourceType.DATABASE) {
                        logger.debug("[ParameterResolver] 从数据库获取变量值: {} (来源: 历史数据)", variable);
                    } else {
                        logger.debug("[ParameterResolver] 从当前执行上下文获取变量值: {} (来源: 当前运行)", variable);
                    }
                }
                return convertToString(value);
            }
        }
        
        logger.debug("[ParameterResolver] 未找到变量值: {}", variable);
        return null;
    }
    
    /**
     * 从NodeResult对象中解析属性
     */
    private Object resolveNodeResultAttribute(Object value, String attrPath) {
        if (value == null || attrPath == null) {
            return value;
        }
        
        // 检查是否是NodeResult类型
        if (value instanceof NodeResult) {
            NodeResult nodeResult = (NodeResult) value;
            
            // 根据属性路径返回相应的值
            switch (attrPath) {
                case "code":
                    return nodeResult.getCode();
                case "message":
                    return nodeResult.getMessage();
                case "success":
                    return nodeResult.getSuccess();
                case "duration":
                    return nodeResult.getDuration();
                case "timestamp":
                    return nodeResult.getTimestamp();
                case "data":
                    return nodeResult.getData();
                case "sqlResult":
                    return nodeResult.getSqlResult();
                case "apiResult":
                    return nodeResult.getApiResult();
                case "beanResult":
                    return nodeResult.getBeanResult();
                case "glueResult":
                    return nodeResult.getGlueResult();
                default:
                    // 尝试从data中获取
                    if (nodeResult.getData() != null && nodeResult.getData() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) nodeResult.getData();
                        return dataMap.get(attrPath);
                    }
            }
        }
        
        return value;
    }
    
    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * 解析嵌套属性（如 data.userId）
     * 
     * @param value 当前值
     * @param attrPath 属性路径（如 data.userId）
     * @return 解析后的值
     */
    private Object resolveNestedAttribute(Object value, String attrPath) {
        if (value == null || attrPath == null) {
            return value;
        }
        
        // 如果值是 Map，尝试获取嵌套属性
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            
            // 分割路径
            String[] pathParts = attrPath.split("\\.");
            Object current = map;
            
            for (String part : pathParts) {
                // 处理数组索引（如 list[0]）
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
     * 解析数组索引（如 list[0]）
     * 
     * @param value 当前值
     * @param attrPath 属性路径（可能包含 [index]）
     * @return 解析后的值
     */
    private Object resolveArrayIndex(Object value, String attrPath) {
        if (value == null || attrPath == null) {
            return value;
        }
        
        // 检查是否包含数组索引
        int bracketIndex = attrPath.indexOf('[');
        if (bracketIndex < 0) {
            return value;
        }
        
        // 提取索引
        int closeBracketIndex = attrPath.indexOf(']', bracketIndex);
        if (closeBracketIndex < 0) {
            return value;
        }
        
        String indexStr = attrPath.substring(bracketIndex + 1, closeBracketIndex);
        try {
            int index = Integer.parseInt(indexStr);
            
            // 如果值是 List，获取指定索引的元素
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                if (index >= 0 && index < list.size()) {
                    return list.get(index);
                }
            }
            // 如果值是数组，获取指定索引的元素
            else if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                if (index >= 0 && index < array.length) {
                    return array[index];
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("[ParameterResolver] 无效的数组索引: {}", indexStr);
        }
        
        return null;
    }
    
    /**
     * 规范化任务名称
     * 
     * <p>将任务名称转换为标准格式，去除特殊字符，统一大小写
     * 
     * @param jobName 原始任务名称
     * @return 规范化后的任务名称
     */
    private String normalizeJobName(String jobName) {
        if (jobName == null || jobName.isEmpty()) {
            return "";
        }
        
        // 去除前后空格
        String normalized = jobName.trim();
        
        // 将特殊字符替换为下划线（保留中文字符、字母、数字）
        normalized = normalized.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        
        // 去除连续的下划线
        normalized = normalized.replaceAll("_{2,}", "_");
        
        // 去除开头和结尾的下划线
        normalized = normalized.replaceAll("^_+|_+$", "");
        
        return normalized;
    }
    
    /**
     * 将值转换为字符串
     * 
     * <p>对于简单类型（字符串、数字、布尔值），直接返回字符串表示
     * <p>对于复杂对象（Map、List），转换为JSON字符串并转义，确保可以安全地嵌入到脚本中
     * 
     * @param value 值
     * @return 字符串表示
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        
        // 如果是字符串，直接返回
        if (value instanceof String) {
            return (String) value;
        }
        
        // 如果是基本类型（数字、布尔值），直接转换为字符串
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        
        // 如果是 Map 或 List，转换为 JSON 字符串
        if (value instanceof Map || value instanceof List) {
            try {
                // 使用 JSONUtil 进行序列化
                String json = JSONUtil.toJsonStr(value);
                // 对于脚本中的使用，需要转义单引号和双引号
                // 但这里先返回JSON字符串，由调用方决定是否需要进一步转义
                return json;
            } catch (Exception e) {
                logger.warn("[ParameterResolver] 转换对象为JSON字符串失败: {}", e.getMessage());
                // 降级：使用toString()
                return value.toString();
            }
        }
        
        // 其他类型直接转换为字符串
        return value.toString();
    }
    
    /**
     * 将值转换为字符串（用于脚本嵌入）
     * 
     * <p>对于复杂对象，会进行JSON序列化并转义，确保可以安全地嵌入到脚本中
     * 
     * @param value 值
     * @param forScript 是否用于脚本（如果是，会对JSON进行转义）
     * @return 字符串表示
     */
    public String convertToStringForScript(Object value, boolean forScript) {
        String str = convertToString(value);
        if (str == null) {
            return null;
        }
        
        // 如果是用于脚本，且是JSON格式（以{或[开头），需要转义
        if (forScript && (str.startsWith("{") || str.startsWith("["))) {
            // 转义单引号和双引号，确保可以安全地嵌入到脚本字符串中
            // 对于Python/PHP等使用单引号的脚本，需要转义单引号
            // 对于Shell等，需要转义双引号和特殊字符
            return escapeForScript(str);
        }
        
        return str;
    }
    
    /**
     * 执行方法调用
     * 
     * @param value 目标对象
     * @param methodCall 方法调用字符串（如 get(0), range(0, 10), size()）
     * @return 方法返回值
     */
    private Object executeMethod(Object value, String methodCall) {
        if (value == null || methodCall == null || methodCall.isEmpty()) {
            return value;
        }
        
        // 解析方法名和参数
        int openParen = methodCall.indexOf('(');
        int closeParen = methodCall.lastIndexOf(')');
        
        if (openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            return value;
        }
        
        String methodName = methodCall.substring(0, openParen);
        String paramsStr = methodCall.substring(openParen + 1, closeParen).trim();
        
        // 解析参数
        List<Object> params = parseMethodParams(paramsStr);
        
        // 根据方法名执行相应的操作
        return executeMethodByName(value, methodName, params);
    }
    
    /**
     * 解析方法参数
     */
    private List<Object> parseMethodParams(String paramsStr) {
        List<Object> params = new ArrayList<>();
        if (paramsStr == null || paramsStr.isEmpty()) {
            return params;
        }
        
        // 简单解析：按逗号分割，支持整数和字符串
        String[] parts = paramsStr.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            
            // 尝试解析为整数
            try {
                params.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                // 如果不是整数，作为字符串处理（去除引号）
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    params.add(part.substring(1, part.length() - 1));
                } else if (part.startsWith("'") && part.endsWith("'")) {
                    params.add(part.substring(1, part.length() - 1));
                } else {
                    params.add(part);
                }
            }
        }
        
        return params;
    }
    
    /**
     * 根据方法名执行相应的操作
     */
    private Object executeMethodByName(Object value, String methodName, List<Object> params) {
        if (!(value instanceof List)) {
            logger.warn("[ParameterResolver] 方法调用 {} 只能在List类型上执行，当前类型: {}", 
                    methodName, value.getClass().getSimpleName());
            return value;
        }
        
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        
        switch (methodName) {
            case "get":
                if (params.size() == 1 && params.get(0) instanceof Integer) {
                    int index = (Integer) params.get(0);
                    if (index >= 0 && index < list.size()) {
                        return list.get(index);
                    }
                }
                break;
                
            case "range":
                if (params.size() == 2 && params.get(0) instanceof Integer && params.get(1) instanceof Integer) {
                    int start = (Integer) params.get(0);
                    int end = (Integer) params.get(1);
                    if (start >= 0 && end <= list.size() && start <= end) {
                        return new ArrayList<>(list.subList(start, end));
                    }
                }
                break;
                
            case "size":
                return list.size();
                
            case "first":
                if (!list.isEmpty()) {
                    return list.get(0);
                }
                break;
                
            case "last":
                if (!list.isEmpty()) {
                    return list.get(list.size() - 1);
                }
                break;
                
            case "isEmpty":
                return list.isEmpty();
                
            default:
                logger.warn("[ParameterResolver] 不支持的方法: {}", methodName);
        }
        
        return value;
    }
    
    /**
     * 转义字符串，使其可以安全地嵌入到脚本中
     * 
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private String escapeForScript(String str) {
        if (str == null) {
            return null;
        }
        
        // 转义单引号、双引号和反斜杠
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

