package com.cc.job.executor.compose.core.evaluator;

import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.executor.compose.core.resolver.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件表达式评估器
 * 
 * <p>负责评估条件节点的条件表达式，决定是否执行条件节点内的任务
 * 
 * <p>支持的表达式类型：
 * <ul>
 *   <li>SIMPLE：简单表达式，支持比较运算符（>, <, ==, !=, >=, <=）和逻辑运算符（&&, ||）</li>
 *   <li>SCRIPT：脚本表达式，使用 JavaScript 引擎执行</li>
 * </ul>
 * 
 * <p>支持的变量语法：
 * <ul>
 *   <li>${var} - 从 DataContext 获取变量值</li>
 *   <li>#jobName.attr - 从 DataContext 获取任务结果（通过 ParameterResolver）</li>
 * </ul>
 * 
 * @author cc-job-team
 */
@Component
public class ConditionEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConditionEvaluator.class);
    
    private final ParameterResolver parameterResolver;
    private final ScriptEngineManager scriptEngineManager;
    
    /** 变量匹配模式：匹配 ${var} 格式 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    public ConditionEvaluator(ParameterResolver parameterResolver) {
        this.parameterResolver = parameterResolver;
        this.scriptEngineManager = new ScriptEngineManager();
    }
    
    /**
     * 评估条件表达式
     * 
     * @param expression 条件表达式
     * @param expressionType 表达式类型（SIMPLE 或 SCRIPT）
     * @param context 数据上下文
     * @param jobNameMap jobName 到 jobId 的映射（用于参数解析）
     * @return 评估结果，true 表示条件满足，false 表示条件不满足
     */
    public boolean evaluate(String expression, String expressionType, 
                           DataContext context, Map<String, Long> jobNameMap) {
        if (expression == null || expression.trim().isEmpty()) {
            logger.warn("[ConditionEvaluator] 条件表达式为空，默认返回 false");
            return false;
        }
        
        if (context == null) {
            logger.warn("[ConditionEvaluator] 数据上下文为空，无法评估条件表达式");
            return false;
        }
        
        try {
            if ("SIMPLE".equalsIgnoreCase(expressionType)) {
                return evaluateSimpleExpression(expression, context, jobNameMap);
            } else if ("SCRIPT".equalsIgnoreCase(expressionType)) {
                return evaluateScriptExpression(expression, context, jobNameMap);
            } else {
                logger.warn("[ConditionEvaluator] 未知的表达式类型: {}，默认使用简单表达式", expressionType);
                return evaluateSimpleExpression(expression, context, jobNameMap);
            }
        } catch (Exception e) {
            logger.error("[ConditionEvaluator] 评估条件表达式失败: {}", expression, e);
            // 评估失败时，默认返回 false，跳过条件节点内的任务
            return false;
        }
    }
    
    /**
     * 评估简单表达式
     * 
     * <p>支持的操作符：>, <, ==, !=, >=, <=, &&, ||
     * <p>示例：${var} > 100 && ${status} == "success"
     */
    private boolean evaluateSimpleExpression(String expression, DataContext context, 
                                            Map<String, Long> jobNameMap) {
        // 先解析变量（${var} 和 #jobName.attr）
        String resolvedExpression = resolveVariables(expression, context, jobNameMap);
        
        logger.debug("[ConditionEvaluator] 解析后的简单表达式: {}", resolvedExpression);
        
        // 使用 JavaScript 引擎评估简单表达式（更安全，支持基本运算）
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
            Object result = engine.eval(resolvedExpression);
            
            // 将结果转换为布尔值
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof Number) {
                // 数字非零视为 true
                return ((Number) result).doubleValue() != 0;
            } else if (result instanceof String) {
                // 非空字符串视为 true
                return !((String) result).trim().isEmpty();
            } else {
                // 其他类型，非 null 视为 true
                return result != null;
            }
        } catch (ScriptException e) {
            logger.error("[ConditionEvaluator] 评估简单表达式失败: {}", resolvedExpression, e);
            return false;
        }
    }
    
    /**
     * 评估脚本表达式
     * 
     * <p>使用 JavaScript 引擎执行脚本
     * <p>示例：if (${var} > 100 && ${status} == "success") { return true; } else { return false; }
     */
    private boolean evaluateScriptExpression(String expression, DataContext context, 
                                           Map<String, Long> jobNameMap) {
        // 先解析变量（${var} 和 #jobName.attr）
        String resolvedExpression = resolveVariables(expression, context, jobNameMap);
        
        logger.debug("[ConditionEvaluator] 解析后的脚本表达式: {}", resolvedExpression);
        
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
            Object result = engine.eval(resolvedExpression);
            
            // 将结果转换为布尔值
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof Number) {
                return ((Number) result).doubleValue() != 0;
            } else if (result instanceof String) {
                return !((String) result).trim().isEmpty();
            } else {
                return result != null;
            }
        } catch (ScriptException e) {
            logger.error("[ConditionEvaluator] 评估脚本表达式失败: {}", resolvedExpression, e);
            return false;
        }
    }
    
    /**
     * 解析表达式中的变量
     * 
     * <p>支持两种变量格式：
     * <ul>
     *   <li>${var} - 从 DataContext 直接获取变量</li>
     *   <li>#jobName.attr - 通过 ParameterResolver 解析任务结果</li>
     * </ul>
     */
    private String resolveVariables(String expression, DataContext context, Map<String, Long> jobNameMap) {
        String result = expression;
        
        // 1. 解析 ${var} 格式的变量
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            
            String replacement;
            if (value == null) {
                // 如果直接获取失败，尝试通过 ParameterResolver 解析（支持 #jobName.attr 格式）
                if (varName.startsWith("#")) {
                    replacement = parameterResolver.resolve(varName, context, jobNameMap);
                } else {
                    logger.warn("[ConditionEvaluator] 变量 {} 未找到值，使用 null", varName);
                    replacement = "null";
                }
            } else {
                replacement = formatValueForExpression(value);
            }
            
            result = result.replace(matcher.group(0), replacement);
        }
        
        // 2. 解析 #jobName.attr 格式的变量（如果还没有被解析）
        if (result.contains("#")) {
            result = parameterResolver.resolve(result, context, jobNameMap);
        }
        
        return result;
    }
    
    /**
     * 将值格式化为表达式可用的格式
     */
    private String formatValueForExpression(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof String) {
            // 字符串需要加引号并转义
            String str = (String) value;
            str = str.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "\"" + str + "\"";
        }
        
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        
        // 其他类型转换为字符串（可能需要进一步处理）
        return "\"" + value.toString().replace("\"", "\\\"") + "\"";
    }
}

