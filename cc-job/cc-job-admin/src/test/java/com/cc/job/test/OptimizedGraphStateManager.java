package com.cc.job.test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 高性能图节点状态管理器
 * 支持非连续节点ID，内存优化，并行计算
 */
public class OptimizedGraphStateManager {

    // 状态常量
    public static final byte NORMAL = 0;
    public static final byte START = 1;
    public static final byte STOP = 2;
    public static final byte BLOCKED = -1;

    // 核心数据结构
    private final Map<Integer, NodeInfo> nodes;          // 节点ID到节点信息的映射
    private final Map<Integer, Set<Integer>> predecessors; // 前驱邻接表
    private final Map<Integer, Set<Integer>> successors;   // 后继邻接表

    // 并行计算支持
    private final ExecutorService executor;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // 节点信息类
    private static class NodeInfo {
        int id;
        byte state;
        int startRefCount;  // 被开始节点依赖的计数
        int stopRefCount;   // 被停止节点依赖的计数

        NodeInfo(int id) {
            this.id = id;
            this.state = NORMAL;
            this.startRefCount = 0;
            this.stopRefCount = 0;
        }
    }

    // 数据库适配器接口
    public interface DatabaseAdapter {
        // 批量加载节点状态
        Map<Integer, Byte> loadNodeStates(Set<Integer> nodeIds);

        // 批量保存节点状态
        void saveNodeStates(Map<Integer, Byte> states);

        // 加载图结构
        GraphStructure loadGraphStructure();
    }

    // 图结构数据类
    public static class GraphStructure {
        public final Set<Integer> nodeIds;
        public final Map<Integer, Set<Integer>> predecessors;
        public final Map<Integer, Set<Integer>> successors;

        public GraphStructure(Set<Integer> nodeIds,
                              Map<Integer, Set<Integer>> predecessors,
                              Map<Integer, Set<Integer>> successors) {
            this.nodeIds = nodeIds;
            this.predecessors = predecessors;
            this.successors = successors;
        }
    }

    /**
     * 构造函数
     * @param dbAdapter 数据库适配器
     * @param threadPoolSize 线程池大小
     */
    public OptimizedGraphStateManager(DatabaseAdapter dbAdapter, int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);

        // 从数据库加载图结构
        GraphStructure structure = dbAdapter.loadGraphStructure();

        // 初始化数据结构
        this.nodes = new ConcurrentHashMap<>(structure.nodeIds.size());
        this.predecessors = new ConcurrentHashMap<>();
        this.successors = new ConcurrentHashMap<>();

        // 创建节点
        for (Integer nodeId : structure.nodeIds) {
            nodes.put(nodeId, new NodeInfo(nodeId));
        }

        // 构建邻接表
        this.predecessors.putAll(structure.predecessors);
        this.successors.putAll(structure.successors);

        // 加载节点状态
        Map<Integer, Byte> states = dbAdapter.loadNodeStates(structure.nodeIds);
        if (!states.isEmpty()) {
            batchInitializeStates(states);
        }
    }

    /**
     * 批量初始化节点状态
     */
    private void batchInitializeStates(Map<Integer, Byte> states) {
        lock.writeLock().lock();
        try {
            // 首先设置所有节点状态
            for (Map.Entry<Integer, Byte> entry : states.entrySet()) {
                NodeInfo node = nodes.get(entry.getKey());
                if (node != null) {
                    node.state = entry.getValue();
                }
            }

            // 并行计算引用计数
            List<Future<?>> futures = new ArrayList<>();

            for (NodeInfo node : nodes.values()) {
                if (node.state == START || node.state == STOP) {
                    Future<?> future = executor.submit(() -> {
                        updateRefCountsForNode(node, true);
                    });
                    futures.add(future);
                }
            }

            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException("初始化引用计数失败", e);
                }
            }

            // 更新阻塞状态
            updateBlockedStates();

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 更新节点的引用计数
     * @param node 节点
     * @param increment true表示增加计数，false表示减少计数
     */
    private void updateRefCountsForNode(NodeInfo node, boolean increment) {
        int delta = increment ? 1 : -1;

        if (node.state == START) {
            // 更新所有前驱节点的startRefCount
            Set<Integer> preds = predecessors.get(node.id);
            if (preds != null) {
                for (int predId : preds) {
                    NodeInfo pred = nodes.get(predId);
                    if (pred != null) {
                        synchronized (pred) {
                            pred.startRefCount = Math.max(0, pred.startRefCount + delta);
                        }
                    }
                }
            }
        } else if (node.state == STOP) {
            // 更新所有后继节点的stopRefCount
            Set<Integer> succs = successors.get(node.id);
            if (succs != null) {
                for (int succId : succs) {
                    NodeInfo succ = nodes.get(succId);
                    if (succ != null) {
                        synchronized (succ) {
                            succ.stopRefCount = Math.max(0, succ.stopRefCount + delta);
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新阻塞状态
     */
    private void updateBlockedStates() {
        for (NodeInfo node : nodes.values()) {
            if (node.state == NORMAL || node.state == BLOCKED) {
                if (node.startRefCount > 0 || node.stopRefCount > 0) {
                    node.state = BLOCKED;
                } else {
                    node.state = NORMAL;
                }
            }
        }
    }

    /**
     * 设置单个节点状态
     * @param nodeId 节点ID
     * @param newState 新状态
     * @param dbAdapter 数据库适配器（可为null）
     */
    public void setNodeState(int nodeId, byte newState, DatabaseAdapter dbAdapter) {
        lock.writeLock().lock();
        try {
            NodeInfo node = nodes.get(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("节点不存在: " + nodeId);
            }

            byte oldState = node.state;
            if (oldState == newState) {
                return;
            }

            // 移除旧状态的影响
            if (oldState == START || oldState == STOP) {
                updateRefCountsForNode(node, false);
            }

            // 设置新状态
            node.state = newState;

            // 应用新状态的影响
            if (newState == START || newState == STOP) {
                updateRefCountsForNode(node, true);
            }

            // 更新阻塞状态
            updateBlockedStates();

            // 异步保存到数据库
            if (dbAdapter != null) {
                executor.submit(() -> {
                    saveNodeToDatabase(nodeId, newState, dbAdapter);
                });
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量设置节点状态
     * @param stateChanges 状态变更映射
     * @param dbAdapter 数据库适配器（可为null）
     */
    public void batchSetNodeStates(Map<Integer, Byte> stateChanges, DatabaseAdapter dbAdapter) {
        if (stateChanges.isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try {
            // 收集所有受影响节点
            Set<Integer> affectedNodes = new HashSet<>();

            // 第一步：处理所有节点状态变更
            for (Map.Entry<Integer, Byte> entry : stateChanges.entrySet()) {
                int nodeId = entry.getKey();
                byte newState = entry.getValue();

                NodeInfo node = nodes.get(nodeId);
                if (node == null) {
                    continue;
                }

                byte oldState = node.state;
                if (oldState == newState) {
                    continue;
                }

                // 记录受影响的节点
                if (oldState == START) {
                    affectedNodes.addAll(getAllPredecessors(nodeId));
                } else if (oldState == STOP) {
                    affectedNodes.addAll(getAllSuccessors(nodeId));
                }

                if (newState == START) {
                    affectedNodes.addAll(getAllPredecessors(nodeId));
                } else if (newState == STOP) {
                    affectedNodes.addAll(getAllSuccessors(nodeId));
                }

                // 设置新状态
                node.state = newState;
            }

            // 第二步：并行重新计算引用计数
            recalculateRefCountsParallel(affectedNodes);

            // 第三步：更新阻塞状态
            updateBlockedStates();

            // 第四步：异步批量保存到数据库
            if (dbAdapter != null) {
                executor.submit(() -> {
                    batchSaveToDatabase(stateChanges, dbAdapter);
                });
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 并行重新计算引用计数
     * @param affectedNodes 受影响节点集合
     */
    private void recalculateRefCountsParallel(Set<Integer> affectedNodes) {
        // 重置受影响节点的引用计数
        for (int nodeId : affectedNodes) {
            NodeInfo node = nodes.get(nodeId);
            if (node != null) {
                node.startRefCount = 0;
                node.stopRefCount = 0;
            }
        }

        // 并行重新计算
        List<Future<?>> futures = new ArrayList<>();

        for (NodeInfo node : nodes.values()) {
            if (node.state == START || node.state == STOP) {
                Future<?> future = executor.submit(() -> {
                    updateRefCountsForNodeIncremental(node, affectedNodes);
                });
                futures.add(future);
            }
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("并行计算引用计数失败", e);
            }
        }
    }

    /**
     * 增量更新引用计数
     * @param node 节点
     * @param affectedNodes 受影响节点集合
     */
    private void updateRefCountsForNodeIncremental(NodeInfo node, Set<Integer> affectedNodes) {
        if (node.state == START) {
            Set<Integer> allPreds = getAllPredecessors(node.id);
            for (int predId : allPreds) {
                if (affectedNodes.contains(predId)) {
                    NodeInfo pred = nodes.get(predId);
                    if (pred != null) {
                        synchronized (pred) {
                            pred.startRefCount++;
                        }
                    }
                }
            }
        } else if (node.state == STOP) {
            Set<Integer> allSuccs = getAllSuccessors(node.id);
            for (int succId : allSuccs) {
                if (affectedNodes.contains(succId)) {
                    NodeInfo succ = nodes.get(succId);
                    if (succ != null) {
                        synchronized (succ) {
                            succ.stopRefCount++;
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取节点的所有前驱（BFS遍历）
     */
    private Set<Integer> getAllPredecessors(int nodeId) {
        Set<Integer> result = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(nodeId);
        visited.add(nodeId);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            Set<Integer> preds = predecessors.get(current);

            if (preds != null) {
                for (int pred : preds) {
                    if (visited.add(pred)) {
                        result.add(pred);
                        queue.add(pred);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 获取节点的所有后继（BFS遍历）
     */
    private Set<Integer> getAllSuccessors(int nodeId) {
        Set<Integer> result = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(nodeId);
        visited.add(nodeId);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            Set<Integer> succs = successors.get(current);

            if (succs != null) {
                for (int succ : succs) {
                    if (visited.add(succ)) {
                        result.add(succ);
                        queue.add(succ);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 获取节点状态
     */
    public byte getNodeState(int nodeId) {
        lock.readLock().lock();
        try {
            NodeInfo node = nodes.get(nodeId);
            return node != null ? node.state : NORMAL;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 保存节点到数据库
     */
    private void saveNodeToDatabase(int nodeId, byte state, DatabaseAdapter dbAdapter) {
        try {
            Map<Integer, Byte> states = new HashMap<>();
            states.put(nodeId, state);
            dbAdapter.saveNodeStates(states);
        } catch (Exception e) {
            System.err.println("保存节点状态失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存到数据库
     */
    private void batchSaveToDatabase(Map<Integer, Byte> states, DatabaseAdapter dbAdapter) {
        try {
            dbAdapter.saveNodeStates(states);
        } catch (Exception e) {
            System.err.println("批量保存节点状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有节点ID
     */
    public Set<Integer> getAllNodeIds() {
        lock.readLock().lock();
        try {
            return new HashSet<>(nodes.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取节点统计信息
     */
    public Map<String, Integer> getNodeStatistics() {
        lock.readLock().lock();
        try {
            Map<String, Integer> stats = new HashMap<>();
            int normalCount = 0;
            int startCount = 0;
            int stopCount = 0;
            int blockedCount = 0;

            for (NodeInfo node : nodes.values()) {
                switch (node.state) {
                    case NORMAL: normalCount++; break;
                    case START: startCount++; break;
                    case STOP: stopCount++; break;
                    case BLOCKED: blockedCount++; break;
                }
            }

            stats.put("totalNodes", nodes.size());
            stats.put("normalNodes", normalCount);
            stats.put("startNodes", startCount);
            stats.put("stopNodes", stopCount);
            stats.put("blockedNodes", blockedCount);

            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 简单的内存数据库适配器示例
     */
    public static class MemoryDatabaseAdapter implements DatabaseAdapter {
        private final Map<Integer, Byte> nodeStates = new ConcurrentHashMap<>();
        private final GraphStructure structure;

        public MemoryDatabaseAdapter(GraphStructure structure) {
            this.structure = structure;
        }

        @Override
        public Map<Integer, Byte> loadNodeStates(Set<Integer> nodeIds) {
            Map<Integer, Byte> result = new HashMap<>();
            for (Integer nodeId : nodeIds) {
                Byte state = nodeStates.get(nodeId);
                if (state != null) {
                    result.put(nodeId, state);
                }
            }
            return result;
        }

        @Override
        public void saveNodeStates(Map<Integer, Byte> states) {
            nodeStates.putAll(states);
        }

        @Override
        public GraphStructure loadGraphStructure() {
            return structure;
        }
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        // 创建图结构
        Set<Integer> nodeIds = new HashSet<>(Arrays.asList(1001, 1002, 1003, 1004, 1005));

        Map<Integer, Set<Integer>> preds = new HashMap<>();
        Map<Integer, Set<Integer>> succs = new HashMap<>();

        // 设置节点关系
        preds.put(1001, new HashSet<>());
        succs.put(1001, new HashSet<>(Arrays.asList(1002, 1003)));

        preds.put(1002, new HashSet<>(Arrays.asList(1001)));
        succs.put(1002, new HashSet<>(Arrays.asList(1004)));

        preds.put(1003, new HashSet<>(Arrays.asList(1001)));
        succs.put(1003, new HashSet<>(Arrays.asList(1004)));

        preds.put(1004, new HashSet<>(Arrays.asList(1002, 1003)));
        succs.put(1004, new HashSet<>(Arrays.asList(1005)));

        preds.put(1005, new HashSet<>(Arrays.asList(1004)));
        succs.put(1005, new HashSet<>());

        GraphStructure structure = new GraphStructure(nodeIds, preds, succs);

        // 创建数据库适配器
        DatabaseAdapter dbAdapter = new MemoryDatabaseAdapter(structure);

        // 创建状态管理器
        OptimizedGraphStateManager manager = new OptimizedGraphStateManager(dbAdapter, 4);

        // 设置节点状态
        manager.setNodeState(1001, START, dbAdapter);
        manager.setNodeState(1005, STOP, dbAdapter);

        // 查询状态
        System.out.println("节点1001状态: " + manager.getNodeState(1001));
        System.out.println("节点1005状态: " + manager.getNodeState(1005));

        // 批量设置状态
        Map<Integer, Byte> batchChanges = new HashMap<>();
        batchChanges.put(1001, NORMAL); // 从开始节点改为普通
        batchChanges.put(1002, START);  // 设置为开始节点

        manager.batchSetNodeStates(batchChanges, dbAdapter);

        // 查询所有节点状态
        for (int nodeId : nodeIds) {
            System.out.println("节点" + nodeId + "状态: " + manager.getNodeState(nodeId));
        }

        // 获取统计信息
        Map<String, Integer> stats = manager.getNodeStatistics();
        System.out.println("统计信息: " + stats);

        // 关闭管理器
        manager.shutdown();
    }
}