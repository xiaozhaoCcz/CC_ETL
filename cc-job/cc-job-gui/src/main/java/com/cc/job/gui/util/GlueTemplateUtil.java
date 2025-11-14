package com.cc.job.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * GLUE 模板工具类
 * 用于加载 GLUE 模式的默认代码模板
 */
public class GlueTemplateUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(GlueTemplateUtil.class);
    
    private static final String TEMPLATE_DIR = "/templates/";
    private static final String TEMPLATE_SUFFIX = ".template";
    
    /**
     * 根据 GLUE 类型获取默认代码模板
     * @param glueType GLUE 类型（如 GLUE_GROOVY, GLUE_SHELL 等）
     * @return 默认代码模板，如果模板不存在则返回空字符串
     */
    public static String getDefaultTemplate(String glueType) {
        if (glueType == null || glueType.trim().isEmpty()) {
            return "";
        }
        
        // 只处理 GLUE 类型的模板
        if (!glueType.startsWith("GLUE_")) {
            return "";
        }
        
        String templatePath = TEMPLATE_DIR + glueType + TEMPLATE_SUFFIX;
        
        try (InputStream inputStream = GlueTemplateUtil.class.getResourceAsStream(templatePath)) {
            if (inputStream == null) {
                logger.warn("GLUE 模板文件不存在: {}", templatePath);
                return "";
            }
            
            byte[] bytes = inputStream.readAllBytes();
            String template = new String(bytes, StandardCharsets.UTF_8);
            logger.debug("成功加载 GLUE 模板: {}", glueType);
            return template;
            
        } catch (IOException e) {
            logger.error("读取 GLUE 模板文件失败: {}", templatePath, e);
            return "";
        }
    }
    
    /**
     * 检查指定 GLUE 类型是否有模板文件
     * @param glueType GLUE 类型
     * @return 是否存在模板文件
     */
    public static boolean hasTemplate(String glueType) {
        if (glueType == null || !glueType.startsWith("GLUE_")) {
            return false;
        }
        
        String templatePath = TEMPLATE_DIR + glueType + TEMPLATE_SUFFIX;
        InputStream inputStream = GlueTemplateUtil.class.getResourceAsStream(templatePath);
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
            return true;
        }
        return false;
    }
}

