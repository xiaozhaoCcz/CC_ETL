package com.cc.job.xo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.job.xo.model.entity.JobSseBroadcast;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SSE 多实例广播队列表 Mapper
 *
 * @author cc-job
 */
@Mapper
public interface JobSseBroadcastMapper extends BaseMapper<JobSseBroadcast> {

    /**
     * 查询待处理记录（未处理且未过期）
     *
     * @param createdAfter 只查该时间之后创建的记录，避免积压
     * @param limit        最大条数
     * @return 待处理记录列表
     */
    List<JobSseBroadcast> selectPending(@Param("createdAfter") LocalDateTime createdAfter, @Param("limit") int limit);

    /**
     * 将指定记录标记为已处理
     *
     * @param id 主键
     * @return 更新行数
     */
    int markProcessed(@Param("id") Long id);

    /**
     * 删除已处理且早于指定时间的记录
     *
     * @param before 删除 created_at 早于该时间的已处理记录
     * @return 删除行数
     */
    int deleteProcessedBefore(@Param("before") LocalDateTime before);
}
