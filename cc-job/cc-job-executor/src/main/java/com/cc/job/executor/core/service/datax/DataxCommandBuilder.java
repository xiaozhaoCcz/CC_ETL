package com.cc.job.executor.core.service.datax;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.model.datax.DataxColumn;
import com.cc.job.xo.model.entity.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.cc.job.xo.constant.DataxConstant.*;

/**
 * DataX 命令构建器
 * 
 * <p>负责构建 DataX 执行命令
 *
 * @author cc-job-team
 */
@Component
public class DataxCommandBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(DataxCommandBuilder.class);
    
    /**
     * 构建 DataX 命令
     * 
     * @param dataxPy DataX Python脚本路径
     * @param jsonFile JSON配置文件路径
     * @param jobInfo 任务信息
     * @return 命令数组
     */
    public String[] buildCommand(String pythonPath,String dataxPy, String jsonFile, JobInfo jobInfo) {
        logger.debug("[DataxCommandBuilder] 构建DataX命令 - jobId: {}", jobInfo.getId());
        
        List<String> cmdList = new ArrayList<>();
        cmdList.add(pythonPath);
        cmdList.add(dataxPy);
        cmdList.add(jsonFile);
        
        // 如果是增量同步，添加参数
        if (jobInfo.getIncrementType() == ExecutorConstants.DataxType.INCREMENTAL) {
            addIncrementalParams(cmdList, jobInfo);
        }
        
        String[] command = cmdList.toArray(new String[0]);
        logger.debug("[DataxCommandBuilder] 命令构建完成 - 参数数量: {}", command.length);
        
        return command;
    }
    
    /**
     * 添加增量同步参数
     * <p>若配置了自定义参数模板（incrementParamTemplate），则按模板中的 %s 顺序用 incrementContent 各列的 columnValue 替换；
     * 否则按原逻辑拼 -DcolumnParam=columnValue。
     */
    private void addIncrementalParams(List<String> cmdList, JobInfo jobInfo) {
        cmdList.add(PARAM);
        
        JSONArray jsonArray = JSONUtil.parseArray(jobInfo.getIncrementContent());
        List<DataxColumn> columns = jsonArray.toList(DataxColumn.class);
        
        String paramString;
        String template = jobInfo.getIncrementParamTemplate();
        if (StringUtils.isNotBlank(template)) {
            // 自定义模板：按 %s 顺序替换为各列值
            paramString = buildParamsFromTemplate(template, columns);
        } else {
            // 默认：-DcolumnParam=columnValue
            paramString = buildDefaultParams(columns);
        }
        
        cmdList.add(paramString);
        logger.debug("[DataxCommandBuilder] 增量参数: {}", paramString);
    }
    
    /**
     * 按自定义模板替换 %s，顺序对应 columns 的 columnValue（时间类型转为秒）
     */
    private String buildParamsFromTemplate(String template, List<DataxColumn> columns) {
        String result = template;
        for (DataxColumn column : columns) {
            String value;
            if (column.getColumnType() != null && column.getColumnType() == 1) {
                long seconds = Long.parseLong(column.getColumnValue()) / 1000;
                value = String.valueOf(seconds);
            } else {
                value = column.getColumnValue() != null ? column.getColumnValue() : "";
            }
            result = result.replaceFirst("%s", Matcher.quoteReplacement(value));
        }
        return result;
    }
    
    private String buildDefaultParams(List<DataxColumn> columns) {
        StringBuilder paramBuilder = new StringBuilder();
        for (DataxColumn column : columns) {
            paramBuilder.append(DASH)
                    .append(column.getColumnParam())
                    .append(EQUALS)
                    .append(SINGLE_QUOTE);
            if (column.getColumnType() != null && column.getColumnType() == 1) {
                long seconds = Long.parseLong(column.getColumnValue()) / 1000;
                paramBuilder.append(seconds);
            } else {
                paramBuilder.append(column.getColumnValue());
            }
            paramBuilder.append(SINGLE_QUOTE)
                    .append(SPACE);
        }
        return paramBuilder.toString();
    }
}

