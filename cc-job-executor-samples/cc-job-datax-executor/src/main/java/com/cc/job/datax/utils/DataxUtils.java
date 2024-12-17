package com.cc.job.datax.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class DataxUtils {

    public static String generateTemJsonFile(String jsonPath,String jobJson) {
        String tmpFilePath;
        jsonPath = jsonPath + "jsons";
        if (!FileUtil.exist(jsonPath)) {
            FileUtil.mkdir(jsonPath);
        }
        tmpFilePath = jsonPath + "jobTmp-" + IdUtil.simpleUUID() + ".conf";
        // 根据json写入到临时本地文件
        try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
            writer.println(jobJson);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return tmpFilePath;
	}


    public static void deleteTemJsonFile(String tmpFilePath) {
        if (FileUtil.exist(tmpFilePath)) {
            FileUtil.del(new File(tmpFilePath));
        }
    }
}
