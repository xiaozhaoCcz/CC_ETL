<!-- Tree.vue -->
<template>
  <div class="tree">
    <div v-for="node in processedNodes" :key="node.id" class="tree-node">
      <div
        class="node-content"
        :class="{
          'node-selected': selectedNodeId === node.id,
          'node-hover': hoveredNodeId === node.id,
          'node-disabled': [2, 3].includes(node.type),
        }"
        @contextmenu.prevent="handleContextMenu($event, node)"
        @mouseenter="hoveredNodeId = node.id"
        @mouseleave="hoveredNodeId = null"
        @click="selectNode(node)"
        v-if="![2, 3].includes(node.type)"
      >
        <!-- 缩进容器 -->
        <div class="indent-container" :style="{ width: node.level * 24 + 'px' }"></div>

        <!-- 展开/折叠图标 -->
        <span class="node-toggle" @click.stop="toggleExpand(node)">
          <svg
            v-if="node.children"
            class="toggle-icon"
            :class="{ expanded: node.expanded }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="6,9 12,15 18,9"></polyline>
          </svg>
          <span class="toggle-placeholder" v-else></span>
        </span>

        <!-- 节点图标和标签 -->
        <span class="node-label">
          <div class="icon-container">
            <svg
              v-if="node.type === 0"
              class="node-icon folder-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M3 7v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-5l-2-2H5a2 2 0 0 0-2 2z"
              ></path>
            </svg>
            <svg
              v-if="node.type === 1"
              class="node-icon file-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14,2 14,8 20,8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
              <polyline points="10,9 9,9 8,9"></polyline>
            </svg>
            <svg
              v-if="node.type === 4"
              class="node-icon tools-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <!-- 齿轮图标 -->
              <circle cx="12" cy="12" r="3"></circle>
              <path
                d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33h.09A1.65 1.65 0 0 0 11 3.09V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v.09a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"
              ></path>
            </svg>
            <svg
              v-if="node.type === 5"
              class="node-icon database-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <!-- 链条图标 -->
              <path
                d="M10 13a5 5 0 0 1 7.07 0l1.41 1.41a5 5 0 0 1-7.07 7.07l-1.41-1.41"
              ></path>
              <path
                d="M14 11a5 5 0 0 0-7.07 0l-1.41 1.41a5 5 0 0 0 7.07 7.07l1.41-1.41"
              ></path>
            </svg>
          </div>
          <span class="node-text">{{ node.label }}</span>
        </span>
      </div>

      <!-- 为type为2和3的节点创建不触发右键菜单的版本 -->
      <div
        class="node-content node-disabled"
        :class="{ 'node-hover': hoveredNodeId === node.id }"
        @mouseenter="hoveredNodeId = node.id"
        @mouseleave="hoveredNodeId = null"
        @click="selectNode(node)"
        v-else
      >
        <!-- 缩进容器 -->
        <div class="indent-container" :style="{ width: node.level * 24 + 'px' }"></div>

        <!-- 展开/折叠图标 -->
        <span class="node-toggle" @click.stop="toggleExpand(node)">
          <svg
            v-if="node.children"
            class="toggle-icon"
            :class="{ expanded: node.expanded }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="6,9 12,15 18,9"></polyline>
          </svg>
          <span class="toggle-placeholder" v-else></span>
        </span>

        <!-- 节点图标和标签 -->
        <span class="node-label">
          <div class="icon-container">
            <svg
              v-if="node.type === 2"
              class="node-icon menu-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="3" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
            <svg
              v-if="node.type === 3"
              class="node-icon arrow-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="9,18 15,12 9,6"></polyline>
            </svg>
          </div>
          <span class="node-text">{{ node.label }}</span>
        </span>
      </div>
      <EnhancedTree
        v-if="node.expanded && node.children"
        :nodes="node.children"
        :search-keyword="searchKeyword"
        :level="node.level + 1"
        @node-contextmenu="$emit('node-contextmenu', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, inject, ref, watch } from "vue";
import { usePageStoreHook } from "@/store/modules/page";

const props = defineProps({
  nodes: Array,
  searchKeyword: String,
  level: {
    type: Number,
    default: 0,
  },
});

// 使用注入的展开状态管理
const expandedNodes = inject("expandedNodes", ref({}));

// 添加一个全局的选中状态管理
const globalSelectedNodeId = inject("globalSelectedNodeId", ref(null));

const emit = defineEmits(["node-contextmenu"]);

// 选中和悬停状态
const selectedNodeId = ref(null);
const hoveredNodeId = ref(null);

const processedNodes = computed(() => {
  return props.nodes.map((node) => ({
    ...node,
    level: props.level,
    // 从全局状态获取展开状态
    expanded: expandedNodes.value[node.id] || false,
  }));
});

function toggleExpand(node) {
  if (node.children) {
    // 更新全局展开状态
    expandedNodes.value[node.id] = !expandedNodes.value[node.id];
  }
}

function handleContextMenu(event, node) {
  console.log("EnhancedTree 右键事件触发", { event, node });
  emit("node-contextmenu", { event, node });
}

function selectNode(node) {
  // 更新全局选中状态，确保只有一个节点被选中
  globalSelectedNodeId.value = node.id;
  selectedNodeId.value = node.id;
  if (node.type === 1) {
    console.log(node);
    usePageStoreHook().addPage(node);
  }
}

// 监听全局选中状态变化
watch(globalSelectedNodeId, (newSelectedId) => {
  selectedNodeId.value = newSelectedId;
});
</script>

<style scoped>
.tree {
  text-align: left;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue",
    Arial, sans-serif;
  font-size: 14px;
  line-height: 1.4;
}

.tree-node {
  position: relative;
}

.node-content {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  cursor: pointer;
  min-height: 32px;
  border-radius: 6px;
  margin: 1px 4px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.node-content:hover {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
}

.node-content.node-hover {
  background-color: #f0f9ff;
  border-color: #b3d8ff;
}

.node-content.node-selected {
  background-color: #e6f7ff;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.node-content.node-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.node-content.node-disabled:hover {
  background-color: #fafafa;
  border-color: #f0f0f0;
}

.indent-container {
  flex-shrink: 0;
  min-width: 0;
}

.node-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  margin-right: 6px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.node-toggle:hover {
  background-color: #e6f7ff;
}

.toggle-icon {
  width: 16px;
  height: 16px;
  color: #666;
  transition: transform 0.2s ease;
}

.toggle-icon.expanded {
  transform: rotate(90deg);
}

.toggle-placeholder {
  width: 20px;
  height: 20px;
}

.node-label {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  gap: 8px;
}

.icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.node-icon {
  width: 16px;
  height: 16px;
  transition: all 0.2s ease;
}

.folder-icon {
  color: #ffa500;
}

.file-icon {
  color: #1890ff;
}

.tools-icon {
  color: #52c41a;
}

.database-icon {
  color: #722ed1;
}

.menu-icon {
  color: #8c8c8c;
}

.arrow-icon {
  color: #8c8c8c;
}

.node-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #262626;
  font-weight: 500;
}

.node-content:hover .node-text {
  color: #1890ff;
}

.node-content.node-selected .node-text {
  color: #1890ff;
  font-weight: 600;
}

/* 添加连接线效果 */
.tree-node:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 20px;
  top: 32px;
  width: 1px;
  height: calc(100% - 32px);
  background-color: #e8e8e8;
  z-index: 0;
}

.tree-node:last-child::after {
  content: "";
  position: absolute;
  left: 20px;
  top: 32px;
  width: 1px;
  height: 16px;
  background-color: #e8e8e8;
  z-index: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .node-content {
    padding: 4px 6px;
    min-height: 28px;
  }

  .node-text {
    font-size: 13px;
  }

  .node-icon {
    width: 14px;
    height: 14px;
  }
}
</style>
