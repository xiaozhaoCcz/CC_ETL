<template>
  <div
    class="dialog-wrapper"
    :style="{
      left: position.left + 'px',
      top: position.top + 'px',
      width: size.width + 'px',
      height: size.height + 'px',
      zIndex: zIndex,
    }"
  >
    <div class="dialog-header" @mousedown="startDrag">
      <slot name="header">默认标题</slot>
      <span class="close" @click="$emit('close')">×</span>
    </div>

    <div class="dialog-content">
      <slot></slot>
    </div>

    <div class="resize-handle" @mousedown="startResize"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";

const props = defineProps({
  zIndex: {
    type: Number,
    default: 1000,
  },
  minWidth: {
    type: Number,
    default: 600,
  },
  minHeight: {
    type: Number,
    default: 600,
  },
});

const emit = defineEmits(["close"]);

const position = ref({ left: 0, top: 0 });
const size = ref({ width: props.minWidth, height: props.minHeight });
const isDragging = ref(false);
const isResizing = ref(false);
const startX = ref(0);
const startY = ref(0);
const initialPos = ref({ left: 0, top: 0 });
const initialSize = ref({ width: 0, height: 0 });

onMounted(() => {
  // 初始居中显示
  position.value.left = (window.innerWidth - size.value.width) ;
  position.value.top = (window.innerHeight - size.value.height)/2 ;
});

const startDrag = (e) => {
  isDragging.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;
  initialPos.value = { ...position.value };
  document.addEventListener("mousemove", handleDrag);
  document.addEventListener("mouseup", stopDrag);
};

const handleDrag = (e) => {
  if (!isDragging.value) return;

  const dx = e.clientX - startX.value;
  const dy = e.clientY - startY.value;

  // 计算新位置并限制在窗口范围内
  let newLeft = initialPos.value.left + dx;
  let newTop = initialPos.value.top + dy;

  newLeft = Math.max(
    0,
    Math.min(newLeft, window.innerWidth - size.value.width)
  );
  newTop = Math.max(
    0,
    Math.min(newTop, window.innerHeight - size.value.height)
  );

  position.value.left = newLeft;
  position.value.top = newTop;
};

const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener("mousemove", handleDrag);
  document.removeEventListener("mouseup", stopDrag);
};

const startResize = (e) => {
  isResizing.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;
  initialSize.value = { ...size.value };
  initialPos.value = { ...position.value };
  document.addEventListener("mousemove", handleResize);
  document.addEventListener("mouseup", stopResize);
};

const handleResize = (e) => {
  if (!isResizing.value) return;

  const dx = e.clientX - startX.value;
  const dy = e.clientY - startY.value;

  // 计算新尺寸并限制最小值和窗口范围
  let newWidth = initialSize.value.width + dx;
  let newHeight = initialSize.value.height + dy;

  newWidth = Math.max(props.minWidth, newWidth);
  newHeight = Math.max(props.minHeight, newHeight);

  // 防止超出窗口右侧
  const maxWidth = window.innerWidth - position.value.left;
  newWidth = Math.min(newWidth, maxWidth);

  // 防止超出窗口底部
  const maxHeight = window.innerHeight - position.value.top;
  newHeight = Math.min(newHeight, maxHeight);

  size.value.width = newWidth;
  size.value.height = newHeight;
};

const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener("mousemove", handleResize);
  document.removeEventListener("mouseup", stopResize);
};

// 组件卸载时清除事件监听
onBeforeUnmount(() => {
  document.removeEventListener("mousemove", handleDrag);
  document.removeEventListener("mouseup", stopDrag);
  document.removeEventListener("mousemove", handleResize);
  document.removeEventListener("mouseup", stopResize);
});
</script>

<style scoped>
.dialog-wrapper {
  position: fixed;
  background: white;
  border: 1px solid #ccc;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.dialog-header {
  padding: 12px;
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
  cursor: move;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-content {
  flex: 1;
  padding: 15px;
  overflow: auto;
}

.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 12px;
  height: 12px;
  background: #fff;
  cursor: nwse-resize;

}

.close {
  cursor: pointer;
  padding: 0 5px;
  font-size: 20px;
}

.close:hover {
  color: #666;
}
</style>
