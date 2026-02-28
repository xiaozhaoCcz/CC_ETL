package com.cc.job.executor.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class DataxUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataxUtils.class);

    private static final String JOB = "job";
    private static final String SETTING = "setting";
    private static final String ERROR_LIMIT = "errorLimit";
    private static final String RECORD = "record";

    /**
     * 规范化 DataX 作业 JSON，确保 job.setting.errorLimit.record 为整数类型。
     * DataX 的 Configuration.getLong() 要求 record 为整数，若为 "0.0" 会解析失败。
     * 解析异常或路径不存在时返回原 JSON，不抛错。
     */
    static String normalizeDataxJobJson(String jobJson) {
        if (jobJson == null || jobJson.isEmpty()) {
            return jobJson;
        }
        try {
            JSONObject root = JSONUtil.parseObj(jobJson);
            JSONObject job = root.getJSONObject(JOB);
            if (job == null) {
                return jobJson;
            }
            JSONObject setting = job.getJSONObject(SETTING);
            if (setting == null) {
                return jobJson;
            }
            JSONObject errorLimit = setting.getJSONObject(ERROR_LIMIT);
            if (errorLimit == null || !errorLimit.containsKey(RECORD)) {
                return jobJson;
            }
            Object recordObj = errorLimit.get(RECORD);
            int recordInt;
            if (recordObj instanceof Number) {
                recordInt = ((Number) recordObj).intValue();
            } else if (recordObj instanceof String) {
                recordInt = (int) Math.round(Double.parseDouble((String) recordObj));
            } else {
                return jobJson;
            }
            errorLimit.set(RECORD, recordInt);
            return root.toString();
        } catch (Exception e) {
            return jobJson;
        }
    }

    public static String generateTemJsonFile(String jsonPath, String jobJson) {
        String normalized = normalizeDataxJobJson(jobJson);
        // 保证 jsonPath 末尾有分隔符，再拼 jsons 子目录
        String normalizedBase = (jsonPath == null || jsonPath.isEmpty())
                ? ""
                : (jsonPath.endsWith(File.separator) ? jsonPath : jsonPath + File.separator);
        String jsonsDir = normalizedBase + "jsons" + File.separator;
        if (!FileUtil.exist(jsonsDir)) {
            FileUtil.mkdir(jsonsDir);
        }
        String tmpFilePath = jsonsDir + "jobTmp-" + IdUtil.simpleUUID() + ".conf";
        try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
            writer.println(normalized);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            logger.error("[DataxUtils] 生成 DataX 临时 JSON 文件失败: {}", tmpFilePath, e);
            throw new RuntimeException("生成 DataX 临时 JSON 文件失败: " + tmpFilePath, e);
        }
        return tmpFilePath;
    }


    public static void deleteTemJsonFile(String tmpFilePath) {
        if (FileUtil.exist(tmpFilePath)) {
            FileUtil.del(new File(tmpFilePath));
        }
    }
}
