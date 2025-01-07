<template>
  <div class="app-container">
    <div class="logic-flow" ref="lfRef"></div>
  </div>
</template>

<script setup lang="ts">
import LogicFlow from "@logicflow/core";
import {
  Control,
  Menu,
  DndPanel,
  DynamicGroup,
  SelectionSelect,
} from "@logicflow/extension";
import "@logicflow/core/lib/style/index.css";
import "@logicflow/extension/lib/style/index.css";
LogicFlow.use(Control); // 控制面板
LogicFlow.use(Menu); // 右键菜单
LogicFlow.use(DndPanel); // 拖拽面板

const lf = ref(null);
const lfRef = ref(null);
const patternItems = [
  {
    type: "dynamic-group",
    label: "内置动态分组",
    text: "DynamicGroup",
    icon: "https://cdn.jsdelivr.net/gh/Logic-Flow/static@latest/docs/examples/extension/group/group.png",
  },
  {
    type: "circle",
    label: "圆形",
    text: "Circle",
    icon: "https://cdn.jsdelivr.net/gh/Logic-Flow/static@latest/docs/examples/extension/group/circle.png",
  },
  {
    type: "rect",
    label: "矩形",
    text: "Rect",
    icon: "https://cdn.jsdelivr.net/gh/Logic-Flow/static@latest/docs/examples/extension/group/rect.png",
  },
  {
    label: "选区",
    icon: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACYAAAAmCAYAAACoPemuAAAAAXNSR0IArs4c6QAAAUdJREFUWEftV9ENwiAQPXAB7Q7GMWzX0TiDdgZjHcd2DNMd1AHsYWgCuTbFlFw/aANfDfCOxzt4PQQE2kSgvCAS881MVGwyxTZF/egHUw3kn9O21P10XCisXsfdRfevr3UqVnA22Pdhm5nvoZh0nK7nTKUOIgBSOhkbyCgxO64wp8TkCtpNKYCSLpwUtaLx+uMLIqYw17tBlKVRLLnXZ0C0incUk2iVNv0abzFCtqnmK0ZS5XuI+/OT2/MCHGJcAlx89DFfBf/ahQlGfcp3gaEzpoTcD/mcv12EdPg7BhsSMZdPTZFKGoP63KhUcglw8fOzi9ahSXNJ7qvM2LjjqouQDn+wt9JVCPqmbnKD5RLg4ud3K7k75uLnfytBSoVfqGxp7fA5/UqSrtLaYDgVbHwlkXfnqJ94sA9e7q3i4qOP+SoYFVuMYj+QKUM2eJOYrwAAAABJRU5ErkJggg==",
    callback: () => {
      lf.value.extension.selectionSelect.openSelectionSelect();
      lf.value.once("selection:selected", () => {
        lf.value.extension.selectionSelect.closeSelectionSelect();
      });
    },
  },
];

const nodes = ref([
  {
    type: "dynamic-group",
    x: 400,
    y: 400,
    children: ["rect_2", "rect_3"],
  },
  {
    id: "rect_2",
    type: "circle",
    x: 400,
    y: 400,
  },
  {
    id: "rect_3",
    type: "circle",
    x: 600,
    y: 400,
  },
]);

const edges = ref([]);

onMounted(() => {
  lf.value = new LogicFlow({
    container: lfRef.value,
    grid: true,
    plugins: [DynamicGroup, DndPanel, SelectionSelect],
  });
  lf.value.extension.dndPanel.setPatternItems(patternItems);
  lf.value.render({
    nodes: nodes.value,
    edges: edges.value,
  });
});
</script>

<style scoped lang="scss">
.logic-flow {
  width: 100%;
  height: 80vh;
}
</style>
