package com.cc.job.executor.core.service.datax;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.cc.job.executor.infrastructure.constant.ExecutorConstants;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.model.datax.DataxColumn;
import com.cc.job.xo.model.entity.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    public String[] buildCommand(String dataxPy, String jsonFile, JobInfo jobInfo) {
        logger.debug("[DataxCommandBuilder] 构建DataX命令 - jobId: {}", jobInfo.getId());
        
        List<String> cmdList = new ArrayList<>();
        cmdList.add(DataxConstant.PYTHON);
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
     */
    private void addIncrementalParams(List<String> cmdList, JobInfo jobInfo) {
        cmdList.add(PARAM);
        
        StringBuilder paramBuilder = new StringBuilder();
        JSONArray jsonArray = JSONUtil.parseArray(jobInfo.getIncrementContent());
        List<DataxColumn> columns = jsonArray.toList(DataxColumn.class);
        
        for (DataxColumn column : columns) {
            paramBuilder.append(DASH)
                    .append(column.getColumnParam())
                    .append(EQUALS)
                    .append(SINGLE_QUOTE);
            
            // 时间类型需要转换为秒
            if (column.getColumnType() == 1) {
                long seconds = Long.parseLong(column.getColumnValue()) / 1000;
                paramBuilder.append(seconds);
            } else {
                paramBuilder.append(column.getColumnValue());
            }
            
            paramBuilder.append(SINGLE_QUOTE)
                    .append(SPACE);
        }
        
        cmdList.add(paramBuilder.toString());
        logger.debug("[DataxCommandBuilder] 增量参数: {}", paramBuilder);
    }
}

