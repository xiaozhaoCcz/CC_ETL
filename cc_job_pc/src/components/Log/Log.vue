<template>
  <div class="log-container" ref="containerRef" @scroll="handleScroll">
    <div class="log-header">
      <div class="log-stats">
        <span class="stat-item">
          <span class="stat-label">总日志:</span>
          <span class="stat-value">{{ logs.length }}</span>
        </span>
        <span class="stat-item">
          <span class="stat-label">错误:</span>
          <span class="stat-value error-count">{{ errorCount }}</span>
        </span>
        <span class="stat-item">
          <span class="stat-label">警告:</span>
          <span class="stat-value warning-count">{{ warningCount }}</span>
        </span>
      </div>
      <div class="log-controls">
        <button class="control-btn" @click="clearLogs" title="清空日志">
          <span class="btn-icon">🗑️</span>
        </button>
        <button
          class="control-btn"
          @click="toggleAutoScroll"
          :class="{ active: isAutoScroll }"
          title="自动滚动"
        >
          <span class="btn-icon">📌</span>
        </button>
      </div>
    </div>

    <div class="log-content">
      <div v-for="log in logs" :key="log.id" class="log-item" :class="log.level">
        <div class="log-timestamp">{{ log.time }}</div>
        <div class="log-level-badge" :class="log.level">
          {{ getLevelText(log.level) }}
        </div>
        <div class="log-message">{{ log.message }}</div>
      </div>

      <div v-if="logs.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无日志信息</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted, onMounted, computed } from "vue";

const logs = ref([]);
const containerRef = ref(null);
const isAutoScroll = ref(true);
let logId = 0;
let rawLogBuffer = "";

// 计算属性：统计各类型日志数量
const errorCount = computed(
  () => logs.value.filter((log) => log.level === "error").length
);
const warningCount = computed(
  () => logs.value.filter((log) => log.level === "warning").length
);

// 获取日志级别文本
const getLevelText = (level) => {
  const levelMap = {
    error: "ERROR",
    warning: "WARN",
    info: "INFO",
    debug: "DEBUG",
  };
  return levelMap[level] || "INFO";
};

// 增强的日志添加方法
const addLogsFromText = (text) => {
  // 追加到缓冲区并处理换行
  rawLogBuffer += text.replace(/\r\n/g, "\n"); // 统一换行符

  // 按换行分割并保留未完成行
  const lines = rawLogBuffer.split("\n");
  rawLogBuffer = lines.pop() || ""; // 最后未完成行保留在缓冲区

  // 解析每行日志
  lines.forEach((line) => {
    const level = detectLogLevel(line);
    addLog(line.trim(), level);
  });
};

// 日志级别检测逻辑
const detectLogLevel = (line) => {
  if (
    /Exception|ERROR|error|失败|ERR|code：500|handleCode=500|任务运行状态:false/.test(
      line
    )
  )
    return "error";
  if (/WARN|warning|警告/.test(line)) return "warning";
  if (/INFO|info|信息/.test(line)) return "info";
  return "debug";
};

// 添加日志的公共方法
const addLog = (message, level = "info") => {
  logs.value.push({
    id: logId++,
    time: new Date().toLocaleTimeString("zh-CN", {
      hour12: false,
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    }),
    message,
    level,
  });
};

// 清空日志
const clearLogs = () => {
  logs.value = [];
  logId = 0;
};

// 切换自动滚动
const toggleAutoScroll = () => {
  isAutoScroll.value = !isAutoScroll.value;
  if (isAutoScroll.value) {
    nextTick(() => {
      scrollToBottom();
    });
  }
};

// 滚动到底部
const scrollToBottom = () => {
  const container = containerRef.value?.querySelector(".log-content");
  if (container) {
    container.scrollTop = container.scrollHeight;
  }
};

// 重置状态方法
const reset = () => {
  logs.value = [];
  logId = 0;
  isAutoScroll.value = true;
};

// 监听日志变化自动滚动
watch(
  logs,
  () => {
    if (isAutoScroll.value) {
      nextTick(() => {
        scrollToBottom();
      });
    }
  },
  { deep: true }
);

// 处理滚动事件
const handleScroll = (e) => {
  const container = e.target;
  const threshold = 10; // 滚动到底部的阈值（像素）
  const isNearBottom =
    container.scrollHeight - container.scrollTop - container.clientHeight <= threshold;
  isAutoScroll.value = isNearBottom;
};

// 暴露方法给父组件
defineExpose({ addLogsFromText, reset, clearLogs });
</script>

<style scoped>
.log-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #e1e5e9;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom: 1px solid #e1e5e9;
}

.log-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.stat-label {
  opacity: 0.8;
}

.stat-value {
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.2);
}

.error-count {
  background: rgba(255, 68, 68, 0.3);
}

.warning-count {
  background: rgba(255, 187, 51, 0.3);
}

.log-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 4px;
  padding: 6px 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: white;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.control-btn.active {
  background: rgba(255, 255, 255, 0.4);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.btn-icon {
  font-size: 14px;
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  background: #fafbfc;
  min-height: 0;
}

.log-item {
  display: flex;
  align-items: flex-start;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-family: "SF Mono", "Monaco", "Inconsolata", "Roboto Mono", "Source Code Pro",
    monospace;
  font-size: 13px;
  line-height: 1.4;
  transition: background-color 0.2s ease;
}

.log-item:hover {
  background-color: #f8f9fa;
}

.log-item:last-child {
  border-bottom: none;
}

.log-timestamp {
  color: #6c757d;
  font-size: 11px;
  min-width: 80px;
  margin-right: 12px;
  font-weight: 500;
}

.log-level-badge {
  min-width: 50px;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  text-align: center;
  margin-right: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.log-level-badge.error {
  background: #fee;
  color: #dc3545;
  border: 1px solid #f5c6cb;
}

.log-level-badge.warning {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.log-level-badge.info {
  background: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}

.log-level-badge.debug {
  background: #e2e3e5;
  color: #383d41;
  border: 1px solid #d6d8db;
}

.log-message {
  flex: 1;
  word-break: break-word;
  white-space: pre-wrap;
  color: #2c3e50;
}

.log-item.error .log-message {
  color: #dc3545;
  font-weight: 500;
}

.log-item.warning .log-message {
  color: #856404;
}

.log-item.info .log-message {
  color: #0c5460;
}

.log-item.debug .log-message {
  color: #6c757d;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #6c757d;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
  font-weight: 500;
}

/* 滚动条样式 */
.log-content::-webkit-scrollbar {
  width: 8px;
}

.log-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.log-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.log-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .log-header {
    padding: 8px 12px;
  }

  .log-stats {
    gap: 8px;
  }

  .stat-item {
    font-size: 11px;
  }

  .log-item {
    padding: 6px 12px;
    font-size: 12px;
  }

  .log-timestamp {
    min-width: 70px;
    margin-right: 8px;
  }

  .log-level-badge {
    min-width: 45px;
    margin-right: 8px;
  }
}
</style>
