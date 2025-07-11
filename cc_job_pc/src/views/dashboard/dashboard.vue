<script setup lang="ts">
import NavBar from "@/views/navbar/navbar.vue";
import Side from "@/views/side/side.vue";
import Main from "@/views/main/main.vue";
import { ref, onBeforeUnmount, computed } from "vue";
import { useNavbarStoreHook } from "@/store";
import { ArrowRight } from "@element-plus/icons-vue";

// 左右拖拽相关的状态
const isDraggingHorizontal = ref(false);
const sideWidth = ref(280); // 调整为280px，更符合专业设计

// 添加容器引用
const dashboardRef = ref<HTMLElement>();

// 获取侧边栏显示状态
const sideVisible = computed(() => useNavbarStoreHook().getSideVisible());

// 显示侧边栏
function showSidebar() {
  useNavbarStoreHook().setSideVisible(true);
}

// 水平拖拽处理函数
const handleHorizontalMouseDown = (e: MouseEvent) => {
  isDraggingHorizontal.value = true;
  document.addEventListener("mousemove", handleHorizontalMouseMove);
  document.addEventListener("mouseup", handleHorizontalMouseUp);
  e.preventDefault();
};

const handleHorizontalMouseMove = (e: MouseEvent) => {
  if (!isDraggingHorizontal.value || !dashboardRef.value) return;

  // 获取整个 dashboard 容器的边界信息
  const containerRect = dashboardRef.value.getBoundingClientRect();

  // 计算鼠标相对于容器左边的位置
  const relativeX = e.clientX - containerRect.left;

  // 限制最小和最大宽度，调整为更合理的范围
  if (relativeX >= 240 && relativeX <= 450) {
    sideWidth.value = relativeX;
  }
};

const handleHorizontalMouseUp = () => {
  isDraggingHorizontal.value = false;
  document.removeEventListener("mousemove", handleHorizontalMouseMove);
  document.removeEventListener("mouseup", handleHorizontalMouseUp);
};

// 组件卸载时清理事件监听器
onBeforeUnmount(() => {
  document.removeEventListener("mousemove", handleHorizontalMouseMove);
  document.removeEventListener("mouseup", handleHorizontalMouseUp);
});
</script>

<template>
  <div class="dashboard-container" ref="dashboardRef">
    <div class="nav-bar">
      <NavBar />
    </div>
    <div class="content">
      <!-- 侧边栏 -->
      <div v-show="sideVisible" class="side" :style="{ width: `${sideWidth}px` }">
        <Side />
      </div>

      <!-- 可拖拽的垂直分隔线 -->
      <div
        v-show="sideVisible"
        class="vertical-resize-handle"
        @mousedown="handleHorizontalMouseDown"
        :class="{ dragging: isDraggingHorizontal }"
      >
        <div class="resize-indicator"></div>
      </div>

      <!-- 显示侧边栏按钮 -->
      <div v-show="!sideVisible" class="show-sidebar-btn" @click="showSidebar">
        <el-icon size="18">
          <ArrowRight />
        </el-icon>
        <span>任务组</span>
      </div>

      <div class="main">
        <Main />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);

  .nav-bar {
    flex-shrink: 0;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    z-index: 100;
  }

  .content {
    flex: 1;
    display: flex;
    min-height: 0;
    overflow: hidden;
    position: relative;

    .side {
      flex-shrink: 0;
      overflow: auto;
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(10px);
      border-right: 1px solid rgba(0, 0, 0, 0.06);
      box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.02);
        border-radius: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(0, 0, 0, 0.1);
        border-radius: 3px;

        &:hover {
          background: rgba(0, 0, 0, 0.2);
        }
      }
    }

    /* 可拖拽的垂直分隔线 */
    .vertical-resize-handle {
      width: 8px;
      height: 100%;
      background: linear-gradient(
        90deg,
        rgba(0, 0, 0, 0.02) 0%,
        rgba(0, 0, 0, 0.04) 50%,
        rgba(0, 0, 0, 0.02) 100%
      );
      cursor: col-resize;
      position: relative;
      transition: all 0.2s ease;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;

      .resize-indicator {
        width: 2px;
        height: 40px;
        background: rgba(0, 0, 0, 0.1);
        border-radius: 1px;
        transition: all 0.2s ease;
      }

      &:hover {
        background: linear-gradient(
          90deg,
          rgba(64, 158, 255, 0.1) 0%,
          rgba(64, 158, 255, 0.2) 50%,
          rgba(64, 158, 255, 0.1) 100%
        );

        .resize-indicator {
          background: #409eff;
          height: 60px;
        }
      }

      &.dragging {
        background: linear-gradient(
          90deg,
          rgba(64, 158, 255, 0.2) 0%,
          rgba(64, 158, 255, 0.3) 50%,
          rgba(64, 158, 255, 0.2) 100%
        );

        .resize-indicator {
          background: #409eff;
          height: 80px;
        }
      }
    }

    /* 显示侧边栏按钮 */
    .show-sidebar-btn {
      position: fixed;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
      color: white;
      padding: 16px 10px;
      border-radius: 0 12px 12px 0;
      cursor: pointer;
      z-index: 1000;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      box-shadow: 4px 0 16px rgba(64, 158, 255, 0.3);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      border: 1px solid rgba(255, 255, 255, 0.2);

      &:hover {
        background: linear-gradient(135deg, #337ecc 0%, #2d6da3 100%);
        transform: translateY(-50%) translateX(4px);
        box-shadow: 6px 0 20px rgba(64, 158, 255, 0.4);
      }

      &:active {
        transform: translateY(-50%) translateX(2px);
      }

      span {
        font-size: 11px;
        font-weight: 500;
        writing-mode: vertical-rl;
        text-orientation: mixed;
        letter-spacing: 1px;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
      }
    }

    .main {
      flex: 1;
      min-width: 0;
      overflow: auto;
      background: rgba(255, 255, 255, 0.7);
      backdrop-filter: blur(10px);
      border-radius: 8px 0 0 0;
      margin: 8px 8px 8px 0;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.3);

      &::-webkit-scrollbar {
        width: 8px;
        height: 8px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.02);
        border-radius: 4px;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(0, 0, 0, 0.1);
        border-radius: 4px;

        &:hover {
          background: rgba(0, 0, 0, 0.2);
        }
      }

      &::-webkit-scrollbar-corner {
        background: transparent;
      }
    }
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-container {
    .content {
      .side {
        min-width: 240px;
        max-width: 320px;
      }

      .vertical-resize-handle {
        width: 6px;
      }

      .show-sidebar-btn {
        padding: 12px 8px;

        span {
          font-size: 10px;
        }
      }
    }
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .dashboard-container {
    background: linear-gradient(135deg, #1a1a1a 0%, #2d3748 100%);

    .nav-bar {
      background: rgba(26, 26, 26, 0.95);
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .content {
      .side {
        background: rgba(26, 26, 26, 0.9);
        border-right: 1px solid rgba(255, 255, 255, 0.1);
      }

      .main {
        background: rgba(26, 26, 26, 0.7);
        border: 1px solid rgba(255, 255, 255, 0.1);
      }
    }
  }
}
</style>
