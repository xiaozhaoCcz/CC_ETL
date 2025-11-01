package com.example.nodefx.util;

/**
 * 雪花算法ID生成器
 * 用于生成唯一的任务执行批次ID
 */
public class SnowflakeIdGenerator {
    
    // 起始时间戳 (2020-01-01)
    private final static long START_TIMESTAMP = 1577836800000L;
    
    // 机器ID所占位数
    private final static long WORKER_ID_BITS = 5L;
    // 数据中心ID所占位数
    private final static long DATACENTER_ID_BITS = 5L;
    // 序列号所占位数
    private final static long SEQUENCE_BITS = 12L;
    
    // 机器ID最大值
    private final static long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 数据中心ID最大值
    private final static long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    // 序列号最大值
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    
    // 机器ID向左移位数
    private final static long WORKER_ID_SHIFT = SEQUENCE_BITS;
    // 数据中心ID向左移位数
    private final static long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // 时间戳向左移位数
    private final static long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    
    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    /**
     * 构造函数
     * @param workerId 工作机器ID (0-31)
     * @param datacenterId 数据中心ID (0-31)
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("worker Id 必须在 0 和 %d 之间", MAX_WORKER_ID)
            );
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("datacenter Id 必须在 0 和 %d 之间", MAX_DATACENTER_ID)
            );
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    /**
     * 默认构造函数，使用默认的机器ID和数据中心ID
     */
    public SnowflakeIdGenerator() {
        this(1, 1);
    }
    
    /**
     * 生成下一个ID
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        
        // 时钟回拨检查
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                String.format("时钟向后移动。拒绝生成ID，时间差: %d 毫秒", 
                lastTimestamp - timestamp)
            );
        }
        
        // 同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号溢出
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        // 组装ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) |
               (datacenterId << DATACENTER_ID_SHIFT) |
               (workerId << WORKER_ID_SHIFT) |
               sequence;
    }
    
    /**
     * 生成字符串格式的ID
     * @return ID字符串
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }
    
    /**
     * 阻塞到下一个毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    /**
     * 获取当前时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
    
    /**
     * 获取单例实例
     */
    private static class SingletonHolder {
        private static final SnowflakeIdGenerator INSTANCE = new SnowflakeIdGenerator();
    }
    
    /**
     * 获取全局单例实例
     */
    public static SnowflakeIdGenerator getInstance() {
        return SingletonHolder.INSTANCE;
    }
}

