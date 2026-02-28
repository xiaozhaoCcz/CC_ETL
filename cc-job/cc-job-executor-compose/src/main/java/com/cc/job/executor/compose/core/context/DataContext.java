package com.cc.job.executor.compose.core.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据上下文 - 任务组执行期间的数据共享容器
 *
 * 特性：
 * 1. 线程安全：使用ConcurrentHashMap保证并发访问安全
 * 2. 层次结构：支持父子上下文，子上下文可以访问父上下文的数据
 * 3. 类型转换：提供类型安全的数据访问方法
 * 4. 作用域隔离：foreach等场景可以创建子上下文，避免数据污染
 *
 * @author cc-job-team
 */
public class DataContext {

    /** 执行批次ID，唯一标识一次任务组执行 */
    private final String executionBatchId;

    /** 数据存储 - 键值对形式 */
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    /** 父上下文（支持上下文嵌套） */
    private final DataContext parentContext;

    /** 元数据：记录每个数据项的来源节点ID */
    private final Map<String, Long> dataSourceMap = new ConcurrentHashMap<>();

    /** 元数据：记录每个数据项的创建时间 */
    private final Map<String, Long> dataTimestampMap = new ConcurrentHashMap<>();

    /** 元数据：记录每个数据项的数据来源类型 */
    private final Map<String, DataSourceType> dataSourceTypeMap = new ConcurrentHashMap<>();

    /**
     * 构造根上下文
     */
    public DataContext(String executionBatchId) {
        this.executionBatchId = executionBatchId;
        this.parentContext = null;
    }

    /**
     * 构造子上下文
     */
    private DataContext(String executionBatchId, DataContext parent) {
        this.executionBatchId = executionBatchId;
        this.parentContext = parent;
    }

    /**
     * 创建子上下文
     *
     * 使用场景：foreach循环中为每个迭代创建独立的上下文
     */
    public DataContext createChildContext() {
        return new DataContext(this.executionBatchId, this);
    }

    /**
     * 存储数据
     *
     * @param key 数据键
     * @param value 数据值
     * @param sourceNodeId 数据来源节点ID
     */
    public void put(String key, Object value, Long sourceNodeId) {
        put(key, value, sourceNodeId, DataSourceType.CURRENT_RUNNING);
    }

    /**
     * 存储数据（完整版，包含数据来源类型）
     *
     * @param key 数据键
     * @param value 数据值
     * @param sourceNodeId 数据来源节点ID
     * @param sourceType 数据来源类型
     */
    public void put(String key, Object value, Long sourceNodeId, DataSourceType sourceType) {
        data.put(key, value);
        dataSourceMap.put(key, sourceNodeId);
        dataTimestampMap.put(key, System.currentTimeMillis());
        dataSourceTypeMap.put(key, sourceType);
    }

    /**
     * 存储数据（简化版）
     */
    public void put(String key, Object value) {
        put(key, value, null, DataSourceType.CURRENT_RUNNING);
    }

    /**
     * 从数据库加载数据并存储（自动标记为DATABASE来源）
     *
     * @param key 数据键
     * @param value 数据值
     * @param sourceNodeId 数据来源节点ID
     */
    public void putFromDatabase(String key, Object value, Long sourceNodeId) {
        put(key, value, sourceNodeId, DataSourceType.DATABASE);
    }

    /**
     * 从数据库加载数据并存储（简化版）
     *
     * @param key 数据键
     * @param value 数据值
     */
    public void putFromDatabase(String key, Object value) {
        putFromDatabase(key, value, null);
    }

    /**
     * 获取数据
     *
     * 查找顺序：当前上下文 → 父上下文 → 祖父上下文 → ...
     */
    public Object get(String key) {
        // 先在当前上下文查找
        if (data.containsKey(key)) {
            return data.get(key);
        }

        // 如果没找到，尝试从父上下文获取
        if (parentContext != null) {
            return parentContext.get(key);
        }

        return null;
    }

    /**
     * 获取数据（类型安全）
     *
     * @param key 数据键
     * @param clazz 期望的数据类型
     * @return 类型转换后的数据
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }

        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        // 尝试类型转换
        return convertType(value, clazz);
    }

    /**
     * 获取字符串类型数据
     */
    public String getString(String key) {
        return get(key, String.class);
    }

    /**
     * 获取整数类型数据
     */
    public Integer getInteger(String key) {
        return get(key, Integer.class);
    }

    /**
     * 获取布尔类型数据
     */
    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }

    /**
     * 获取列表类型数据
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        Object value = get(key);
        if (value instanceof List) {
            return (List<T>) value;
        }
        return null;
    }

    /**
     * 获取Map类型数据
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String key) {
        Object value = get(key);
        if (value instanceof Map) {
            return (Map<K, V>) value;
        }
        return null;
    }

    /**
     * 检查是否包含某个键
     */
    public boolean containsKey(String key) {
        if (data.containsKey(key)) {
            return true;
        }
        if (parentContext != null) {
            return parentContext.containsKey(key);
        }
        return false;
    }

    /**
     * 移除数据
     */
    public void remove(String key) {
        data.remove(key);
        dataSourceMap.remove(key);
        dataTimestampMap.remove(key);
        dataSourceTypeMap.remove(key);
    }

    /**
     * 清空当前上下文（不影响父上下文）
     */
    public void clear() {
        data.clear();
        dataSourceMap.clear();
        dataTimestampMap.clear();
        dataSourceTypeMap.clear();
    }

    /**
     * 获取所有数据键
     */
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>(data.keySet());
        if (parentContext != null) {
            keys.addAll(parentContext.keySet());
        }
        return keys;
    }

    /**
     * 获取所有变量（包括父上下文）
     */
    public Map<String, Object> getAllVariables() {
        Map<String, Object> allData = new HashMap<>();

        // 先添加父上下文的数据
        if (parentContext != null) {
            allData.putAll(parentContext.getAllVariables());
        }

        // 再添加当前上下文的数据（会覆盖父上下文的同名键）
        allData.putAll(data);

        return allData;
    }

    /**
     * 获取数据来源节点ID
     */
    public Long getDataSource(String key) {
        if (dataSourceMap.containsKey(key)) {
            return dataSourceMap.get(key);
        }
        if (parentContext != null) {
            return parentContext.getDataSource(key);
        }
        return null;
    }

    /**
     * 获取数据来源类型
     *
     * @param key 数据键
     * @return 数据来源类型，如果不存在则返回null
     */
    public DataSourceType getDataSourceType(String key) {
        if (dataSourceTypeMap.containsKey(key)) {
            return dataSourceTypeMap.get(key);
        }
        if (parentContext != null) {
            return parentContext.getDataSourceType(key);
        }
        return null;
    }

    /**
     * 类型转换工具
     */
    @SuppressWarnings("unchecked")
    private <T> T convertType(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        // String类型转换
        if (targetType == String.class) {
            return (T) value.toString();
        }

        // 数值类型转换
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }
            return (T) Integer.valueOf(value.toString());
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return (T) Long.valueOf(((Number) value).longValue());
            }
            return (T) Long.valueOf(value.toString());
        }

        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            }
            return (T) Double.valueOf(value.toString());
        }

        // 布尔类型转换
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            }
            return (T) Boolean.valueOf(value.toString());
        }

        throw new ClassCastException(
                String.format("无法将 %s 转换为 %s",
                        value.getClass().getName(), targetType.getName()));
    }

    @Override
    public String toString() {
        return String.format("DataContext[batchId=%s, size=%d, parent=%s]",
                executionBatchId, data.size(), parentContext != null ? "yes" : "no");
    }
}
