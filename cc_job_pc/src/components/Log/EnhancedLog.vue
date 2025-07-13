<template>
  <div class="enhanced-log-container" ref="containerRef">
    <!-- 日志头部 -->
    <div class="log-header">
      <div class="log-stats">
        <span class="stat-item">
          <span class="stat-label">总日志:</span>
          <span class="stat-value">{{ stats.total }}</span>
        </span>
        <span class="stat-item">
          <span class="stat-label">错误:</span>
          <span class="stat-value error-count">{{ stats.error }}</span>
        </span>
        <span class="stat-item">
          <span class="stat-label">警告:</span>
          <span class="stat-value warning-count">{{ stats.warning }}</span>
        </span>
        <span class="stat-item">
          <span class="stat-label">信息:</span>
          <span class="stat-value info-count">{{ stats.info }}</span>
        </span>
      </div>

      <div class="log-controls">
        <button
          class="control-btn"
          @click="clearLogs"
          title="清空日志"
          :disabled="logs.length === 0"
        >
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

        <button class="control-btn" @click="toggleSearch" title="搜索日志">
          <span class="btn-icon">🔍</span>
        </button>

        <button
          class="control-btn"
          @click="exportLogs"
          title="导出日志"
          :disabled="logs.length === 0"
        >
          <span class="btn-icon">📥</span>
        </button>
        <button v-if="!isAtTop" class="control-btn" @click="scrollToTop" title="回到顶部">
          <span class="btn-icon">⏫</span>
        </button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div v-if="showSearch" class="search-bar">
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="搜索日志内容..."
        class="search-input"
        @input="handleSearch"
      />
      <div class="search-options">
        <label class="search-option">
          <input type="checkbox" v-model="caseSensitive" />
          区分大小写
        </label>
        <span class="search-result-count"> 找到 {{ searchResults.length }} 条结果 </span>
      </div>
    </div>

    <!-- 日志内容 -->
    <div class="log-content" ref="contentRef" @scroll="handleScroll">
      <div
        v-for="log in displayLogs"
        :key="log.id"
        class="log-item"
        :class="[log.level, { 'search-highlight': isSearchResult(log.id) }]"
        @click="handleLogClick(log)"
      >
        <div class="log-timestamp">{{ log.time }}</div>
        <div class="log-level-badge" :class="log.level">
          {{ getLevelText(log.level) }}
        </div>
        <div class="log-message" :title="log.message">
          {{ log.message }}
        </div>
        <div v-if="log.metadata" class="log-metadata">
          <span class="metadata-item" v-if="log.jobId"> 任务ID: {{ log.jobId }} </span>
          <span class="metadata-item" v-if="log.randomId">
            随机ID: {{ log.randomId }}
          </span>
        </div>
      </div>

      <div v-if="displayLogs.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">
          {{ searchKeyword ? "未找到匹配的日志" : "暂无日志信息" }}
        </div>
      </div>
    </div>

    <!-- 性能指示器 -->
    <div v-if="showPerformanceIndicator" class="performance-indicator">
      <span class="performance-text">
        日志处理性能: {{ performanceMetrics.avgProcessingTime }}ms
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from "vue";
import { ElMessage } from "element-plus";
import { LogManager, LogLevel, type LogItem, type LogStats } from "@/utils/logManager";

// Props
interface Props {
  logManager?: LogManager;
  showPerformanceIndicator?: boolean;
  maxDisplayLogs?: number;
}

const props = withDefaults(defineProps<Props>(), {
  showPerformanceIndicator: false,
  maxDisplayLogs: 1000,
});

// Emits
const emit = defineEmits<{
  logClick: [log: LogItem];
  logClear: [];
}>();

// 响应式数据
const containerRef = ref<HTMLElement>();
const contentRef = ref<HTMLElement>();
const isAutoScroll = ref(true);
const showSearch = ref(false);
const searchKeyword = ref("");
const caseSensitive = ref(false);
const searchResults = ref<LogItem[]>([]);
const performanceMetrics = ref({
  avgProcessingTime: 0,
  totalProcessed: 0,
});
const isAtTop = ref(true);

// 日志管理器
const logManager = props.logManager || new LogManager({
  maxLogs: 10000,
  enablePerformance: props.showPerformanceIndicator,
});

// 计算属性
const logs = computed(() => logManager.getLogs().value);
const stats = computed(() => logManager.getStats());

const displayLogs = computed(() => {
  let filteredLogs = logs.value;

  // 搜索过滤
  if (searchKeyword.value.trim()) {
    filteredLogs = searchResults.value;
  }

  // 限制显示数量
  return filteredLogs.slice(-props.maxDisplayLogs);
});

// 方法
const getLevelText = (level: LogLevel): string => {
  const levelMap = {
    [LogLevel.ERROR]: "ERROR",
    [LogLevel.WARNING]: "WARN",
    [LogLevel.INFO]: "INFO",
    [LogLevel.DEBUG]: "DEBUG",
  };
  return levelMap[level] || "INFO";
};

const clearLogs = () => {
  logManager.clearLogs();
  emit("logClear");
  ElMessage.success("日志已清空");
};

const toggleAutoScroll = () => {
  isAutoScroll.value = !isAutoScroll.value;
  if (isAutoScroll.value) {
    nextTick(() => {
      scrollToBottom();
    });
  }
};

const toggleSearch = () => {
  showSearch.value = !showSearch.value;
  if (!showSearch.value) {
    searchKeyword.value = "";
    searchResults.value = [];
  }
};

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    searchResults.value = logManager.searchLogs(searchKeyword.value, caseSensitive.value);
  } else {
    searchResults.value = [];
  }
};

const isSearchResult = (logId: number): boolean => {
  return searchResults.value.some(log => log.id === logId);
};

const handleLogClick = (log: LogItem) => {
  emit("logClick", log);
};

const scrollToBottom = () => {
  if (contentRef.value) {
    contentRef.value.scrollTop = contentRef.value.scrollHeight;
  }
};

const scrollToTop = () => {
  if (contentRef.value) {
    contentRef.value.scrollTop = 0;
  }
};

const handleScroll = (e: Event) => {
  const container = e.target as HTMLElement;
  const threshold = 10;
  const isNearBottom =
    container.scrollHeight - container.scrollTop - container.clientHeight <= threshold;
  isAutoScroll.value = isNearBottom;
  isAtTop.value = container.scrollTop === 0;
};

const exportLogs = () => {
  try {
    const format = "txt";
    const content = logManager.exportLogs(format);
    const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `logs_${new Date().toISOString().slice(0, 19).replace(/:/g, "-")}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    ElMessage.success("日志导出成功");
  } catch (error) {
    ElMessage.error("日志导出失败");
    console.error("导出日志失败:", error);
  }
};

// 重置方法
const reset = () => {
  logManager.reset();
  isAutoScroll.value = true;
  showSearch.value = false;
  searchKeyword.value = "";
  searchResults.value = [];
};

// 添加日志方法（兼容原有接口）
const addLogsFromText = (text: string, jobId?: number, randomId?: string) => {
  logManager.addLogsFromText(text, jobId, randomId);
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

// 性能监控
if (props.showPerformanceIndicator) {
  watch(
    logs,
    () => {
      // 这里可以添加性能监控逻辑
      performanceMetrics.value.totalProcessed = logs.value.length;
    },
    { deep: true }
  );
}

// 生命周期
onMounted(() => {
  // 初始化时滚动到底部
  nextTick(() => {
    scrollToBottom();
  });
});

onUnmounted(() => {
  // 清理资源
  if (!props.logManager) {
    logManager.destroy();
  }
});

// 暴露方法给父组件
defineExpose({
  addLogsFromText,
  reset,
  clearLogs,
  logManager,
});
</script>

<style scoped>
.enhanced-log-container {
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

.info-count {
  background: rgba(51, 187, 255, 0.3);
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

.control-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.control-btn.active {
  background: rgba(255, 255, 255, 0.4);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 14px;
}

.search-bar {
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e1e5e9;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
}

.search-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.search-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.search-option {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.search-result-count {
  font-size: 12px;
  color: #666;
  font-weight: 500;
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
  cursor: pointer;
}

.log-item:hover {
  background-color: #f8f9fa;
}

.log-item.search-highlight {
  background-color: #fff3cd;
  border-left: 3px solid #ffc107;
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
  overflow: hidden;
  text-overflow: ellipsis;
}

.log-metadata {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  font-size: 11px;
  color: #666;
}

.metadata-item {
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #e9ecef;
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

.performance-indicator {
  padding: 8px 16px;
  background: #e9ecef;
  border-top: 1px solid #dee2e6;
  font-size: 12px;
  color: #6c757d;
  text-align: center;
}

.performance-text {
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

  .search-bar {
    padding: 8px 12px;
  }
}
</style>
