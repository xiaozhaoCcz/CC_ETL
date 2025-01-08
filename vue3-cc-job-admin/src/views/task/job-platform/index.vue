<template>
  <div class="app-container">
    <div class="tool-list">
      <el-button type="info" :icon="Folder" circle @click="handleOpenDialog" />
    </div>
    <div class="job-platform">
      <div class="job-group-tree"></div>
      <div class="logic-flow" ref="lfRef"></div>
    </div>

    <EditJobComp
      :taskRankVisible="jobComposeVisible"
      :formData="formData"
      @close="handleCloseDialog"
    />
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
import EditJobComp from "@/views/task/job-platform/operation/edit-job-compose.vue";
import { Folder } from "@element-plus/icons-vue";
import JobInfoAPI from "@/api/task/job-info";
LogicFlow.use(Control); // 控制面板
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
    id: "circle_2",
    type: "circle",
    x: 522,
    y: 170,
    text: {
      value: "circle_2",
      x: 522,
      y: 170,
      editable: false,
      draggable: true,
    },
    properties: {
      jobId: "123",
    },
  },
  {
    id: "dynamic-group_1",
    type: "dynamic-group",
    x: 382,
    y: 189,
    // children: ["rect_3"],
    text: "dynamic-group_1",
    resizable: true,
    properties: {
      // resizable: true,
      collapsible: true,
      width: 420,
      height: 250,
      radius: 5,
      isCollapsed: true,
    },
  },
  {
    id: "dynamic-group_2",
    type: "dynamic-group",
    x: 382,
    y: 393,
    // children: ["rect_3"],
    text: "dynamic-group_2",
    resizable: true,
    properties: {
      width: 420,
      height: 250,
      radius: 5,
      collapsible: false,
      isCollapsed: false,
    },
  },
]);
const edges = ref([]);
const menuConfig = {
  nodeMenu: [
    {
      text: "删除",
      callback(node) {
        lf.value.deleteNode(node.id);
      },
    },
    {
      text: "分享",
      callback() {
        alert("分享成功！");
      },
    },
    {
      text: "属性",
      callback(node: any) {
        alert(`
          节点id：${node.id}
          节点类型：${node.type}
          节点坐标：(x: ${node.x}, y: ${node.y})`);
      },
    },
  ],
  edgeMenu: [
    {
      text: "属性",
      callback(edge: any) {
        alert(`
          边id：${edge.id}
          边类型：${edge.type}
          边坐标：(x: ${edge.x}, y: ${edge.y})
          源节点id：${edge.sourceNodeId}
          目标节点id：${edge.targetNodeId}`);
      },
    },
  ],
  graphMenu: [
    {
      text: "分享",
      callback() {
        alert("分享成功！");
      },
    },
  ],
  edgeMenu: false, // 删除默认的边右键菜单
  graphMenu: [], // 覆盖默认的边右键菜单，与false表现一样
};
const jobComposeVisible = reactive({
  title: "",
  visible: false,
});
const formData = reactive<any>({
  executorTimeout: 600000,
});
const jobCompId = ref(null);

/** 打开task_info弹窗 */
function handleOpenDialog() {
  jobComposeVisible.visible = true;
  if (jobCompId.value) {
    jobComposeVisible.title = "修改任务组";
    JobInfoAPI.getFormData(jobCompId.value).then((data) => {
      Object.assign(formData, data);
      formData.nodes = JSON.stringify(nodes.value);
      formData.edges = JSON.stringify(edges.value);
    });
  } else {
    formData.nodes = JSON.stringify(nodes.value);
    formData.edges = JSON.stringify(edges.value);
    jobComposeVisible.title = "新增任务组";
  }
}

function handleCloseDialog() {
  const keys = Object.keys(formData);
  let obj: { [name: string]: string } = {};
  keys.forEach((item) => {
    obj[item] = "";
  });
  Object.assign(formData, obj);
  jobComposeVisible.visible = false;
}

onMounted(() => {
  lf.value = new LogicFlow({
    container: lfRef.value,
    grid: true,
    multipleSelectKey: "alt",
    autoExpand: false,
    allowResize: true,
    allowRotate: true,
    plugins: [DynamicGroup, DndPanel, SelectionSelect, Menu],
  });
  lf.value.extension.dndPanel.setPatternItems(patternItems);
  lf.value.extension.menu.setMenuConfig(menuConfig);
  lf.value.render({
    nodes: nodes.value,
    edges: edges.value,
  });
});
</script>

<style scoped lang="scss">
.job-platform {
  .logic-flow {
    width: 100%;
    height: 80vh;
  }
}
</style>
