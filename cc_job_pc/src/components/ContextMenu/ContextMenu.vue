<!-- ContextMenu.vue -->
<template>
  <div
    ref="menuElement"
    class="context-menu"
    :style="{
      left: adjustedX + 'px',
      top: adjustedY + 'px',
    }"
    @click.stop
  >
    <div
      class="menu-item"
      v-if="node && node.type === 0"
      @click="handleAction('addJobGroup')"
    >
      <el-icon class="menu-icon"><Plus /></el-icon>
      <span>新增任务组</span>
    </div>
    <div
      class="menu-item"
      v-if="node && node.type === 1"
      @click="handleAction('addJobInfo')"
    >
      <el-icon class="menu-icon"><Plus /></el-icon>
      <span>新增任务</span>
    </div>

    <div class="menu-separator" v-if="shouldShowSeparator('edit')"></div>

    <div
      class="menu-item"
      v-if="node && ![5].includes(node.type)"
      @click="handleAction('edit')"
    >
      <el-icon class="menu-icon"><Edit /></el-icon>
      <span>编辑</span>
    </div>

    <div
      class="menu-item"
      v-if="node && ![5].includes(node.type)"
      @click="handleAction('delete')"
    >
      <el-icon class="menu-icon"><Delete /></el-icon>
      <span>删除</span>
    </div>
    <!-- 新增刷新按钮，仅type为1或2时显示 -->
    <div
      class="menu-item danger-item"
      v-if="node && (node.type === 0 || node.type === 1)"
      @click="handleAction('refresh')"
    >
      <el-icon class="menu-icon"><ElIconRefresh /></el-icon>
      <span>刷新</span>
    </div>

    <div class="menu-separator" v-if="shouldShowSeparator('export')"></div>

    <div class="menu-item" v-if="node && node.type === 0" @click="handleAction('export')">
      <el-icon class="menu-icon"><Download /></el-icon>
      <span>导出</span>
    </div>

    <div
      class="menu-item"
      v-if="node && [4, 5].includes(node.type)"
      @click="handleAction('location')"
    >
      <el-icon class="menu-icon"><Location /></el-icon>
      <span>定位</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from "vue";
import { Plus, Edit, Delete, Download, Location } from "@element-plus/icons-vue";
import { Refresh as ElIconRefresh } from "@element-plus/icons-vue";

const props = defineProps({
  x: Number,
  y: Number,
  node: Object,
});

const menuElement = ref(null);

defineExpose({ menuElement }); // 暴露DOM元素给父组件

const emit = defineEmits(["close", "handleAction"]);

// 计算属性，确保 node 在模板中可用
const node = computed(() => props.node);

// 智能位置计算
const adjustedX = ref(0);
const adjustedY = ref(0);

// 计算最佳位置
const calculatePosition = async () => {
  await nextTick();

  if (!menuElement.value) return;

  const menuRect = menuElement.value.getBoundingClientRect();
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;

  let x = props.x || 0;
  let y = props.y || 0;

  // 检查右边界
  if (x + menuRect.width > viewportWidth) {
    x = viewportWidth - menuRect.width - 10;
  }

  // 检查下边界
  if (y + menuRect.height > viewportHeight) {
    y = viewportHeight - menuRect.height - 10;
  }

  // 确保不超出左边界和上边界
  x = Math.max(10, x);
  y = Math.max(10, y);

  adjustedX.value = x;
  adjustedY.value = y - 160;
};

// 监听位置变化
onMounted(() => {
  calculatePosition();
});

// 判断是否显示分隔线
const shouldShowSeparator = (action) => {
  if (action === "edit") {
    return props.node?.type === 0 || props.node?.type === 1;
  }
  if (action === "export") {
    return ![5].includes(props.node?.type);
  }
  return false;
};

function handleAction(action) {
  emit("close");
  emit("handleAction", action, props.node);
}
</script>

<style scoped>
.context-menu {
  position: fixed;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 140px;
  padding: 4px 0;
  font-size: 14px;
  color: #606266;
  backdrop-filter: blur(10px);
  animation: contextMenuFadeIn 0.15s ease-out;
}

@keyframes contextMenuFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-5px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  user-select: none;
}

.menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.menu-item.danger-item:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.menu-icon {
  margin-right: 8px;
  font-size: 16px;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.menu-separator {
  height: 1px;
  background: #e4e7ed;
  margin: 4px 0;
}

.menu-item span {
  flex: 1;
  white-space: nowrap;
}
</style>
