package com.cc.job.admin.task.utils;

import com.cc.job.admin.task.enums.DatasourceEnum;

import java.util.List;
import java.util.Map;

import static com.cc.job.xo.constant.DataxConstant.SPLIT;

public class DataxUtils {

    public final static Map<String,String> timeFormatMap = Map.of(
            "YYYY-MM-DD hh:mm:ss","%Y-%m-%d %H:%i:%s",
            "YYYY/MM/DD hh:mm:ss","%Y/%m/%d %H:%i:%s",
            "YYYY-MM-DD","%Y-%m-%d",
            "YYYY/MM/DD","%Y/%m/%d"
    );

    public static List<String> split(String str){
        return split(str,SPLIT);
    }

    public static List<String> split(String str, String sp){
        String[] split = str.split(sp);
        return List.of(split);
    }


    public static String getDateFormat(DatasourceEnum datasource,String timeFormat){
        if(DatasourceEnum.MYSQL==datasource){
            return timeFormatMap.get(timeFormat);
        }
        return "";
    }
}
