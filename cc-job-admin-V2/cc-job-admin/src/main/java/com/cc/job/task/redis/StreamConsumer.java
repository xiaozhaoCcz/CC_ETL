package com.cc.job.task.redis;

import cn.hutool.core.lang.Pair;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 如果条件允许可以换成mq队列
 */
@Service
public class StreamConsumer {

    public static final String TASK_SET_STREAM = "TASK_SET_STREAM";

    private static final List<Pair<Long,Boolean>> callbackRes = Collections.synchronizedList(new ArrayList<>());

    public  static  List<Pair<Long,Boolean>> getCallbackRes() {
        return callbackRes;
    }

    public static void removeCallbackRes(Long jobId) {
        if (jobId != null) {
            callbackRes.removeIf(pair -> jobId.equals(pair.getKey()));
        }
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostConstruct
    public void start() {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                StreamMessageListenerContainer.create(redisTemplate.getConnectionFactory(), options);

        listenerContainer.receive(StreamOffset.create(TASK_SET_STREAM, ReadOffset.lastConsumed()), message -> {
            Map<String, String> value = message.getValue();
            value.forEach((k,v)->{
                callbackRes.add(new Pair<>(Long.valueOf(k),Boolean.valueOf(v)));
            });
        });

        listenerContainer.start();
    }
}
