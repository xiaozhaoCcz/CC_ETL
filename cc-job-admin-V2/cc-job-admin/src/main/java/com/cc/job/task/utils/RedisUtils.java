package com.cc.job.task.utils;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Map;


public class RedisUtils {

    private RedisTemplate redisTemplate;

    public RedisUtils(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RecordId sendMessage(String streamKey, Map<?,?> map) {
        RecordId recordId = redisTemplate.opsForStream().add(MapRecord.create(streamKey, map));
        return recordId;
    }
}
