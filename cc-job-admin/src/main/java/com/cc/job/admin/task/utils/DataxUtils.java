package com.cc.job.admin.task.utils;

import java.util.List;

import static com.cc.job.xo.constant.DataxConstant.SPLIT;

public class DataxUtils {

    public static List<String> split(String str){
        return split(str,SPLIT);
    }

    public static List<String> split(String str, String sp){
        String[] split = str.split(sp);
        return List.of(split);
    }
}
