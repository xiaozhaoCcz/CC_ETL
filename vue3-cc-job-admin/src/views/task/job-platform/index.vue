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

    <el-dialog v-model="jobDialog">
      <el-radio-group v-model="jobRadio" @change="changeJobRadio">
        <el-radio :value="0" size="large">单任务</el-radio>
        <el-radio :value="1" size="large">任务组</el-radio>
      </el-radio-group>
      <el-select
        v-model="jobSelectId"
        placeholder="选择任务"
        size="large"
        style="width: 240px"
      >
        <el-option
          v-for="item in selectJobInfoList"
          :key="item.id"
          :label="item.jobDesc"
          :value="item.id"
        />
      </el-select>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelDialog">取消</el-button>
          <el-button type="primary" @click="confirmDialog">确认</el-button>
        </div>
      </template>
    </el-dialog>
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
    properties: {
      isCollapsed: false,
      isRestrict: true,
      autoResize: true,
    },
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
const nodes = ref([]);
const edges = ref([]);
const jobInfoList = ref([]);
const selectJobInfoList = ref([]);
const jobRadio = ref(0);
const jobDialog = ref(false);
const jobSelectId = ref(undefined);
const jobNodeEditId = ref(undefined);
const menuConfig = {
  nodeMenu: [
    {
      text: "删除",
      callback(node) {
        lf.value.deleteNode(node.id);
      },
    },
    {
      text: "编辑",
      callback(node: any) {
        jobDialog.value = true;
        jobNodeEditId.value = node.id;
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

function cancelDialog() {
  jobSelectId.value = undefined;
  jobDialog.value = false;
}
function confirmDialog() {
  const _node = lf.value!.getNodeModelById(jobNodeEditId.value);
  const _jobInfo = jobInfoList.value.find((e) => e.id === jobSelectId.value);
  console.log(_node);
  if (_jobInfo.jobType === 2) {
    //新增任务组
    JobInfoAPI.getJobCompose(jobSelectId.value).then((res: any) => {
      console.log(res);
      const jobNode = res.jobNode;
      const newNodes = res.nodes;
      const newEdges = res.edges;
      newNodes.forEach((node: any) => {
        lf.value.graphModel.addNode(generateNode(node));
      });

      newNodes.forEach((n: any) => {
        const node = lf.value.getNodeModelById(n.id);
        if (n.nodeType === "dynamic-group") {
          JSON.parse(n.children).forEach((id: any) => node.addChild(id));
        }
      });

      newEdges.forEach((e: any) => {
        let generateEdge1 = generateEdge(e);
        console.log(generateEdge1);
        lf.value.graphModel.addEdge(generateEdge(e));
      });

      JSON.parse(jobNode.children).forEach((id: any) => {
        _node.addChild(id);
      });
    });
  }
  _node.setProperty("jobId", jobSelectId.value);
  _node.updateText(_jobInfo.jobDesc);

  cancelDialog();
}

function generateNode(node: any) {
  return {
    id: node.id,
    text: node.jobName,
    type: node.nodeType,
    x: node.nodePositionX,
    y: node.nodePositionY,
    properties: JSON.parse(node.properties),
    children: node.children != null ? JSON.parse(node.children) : [],
  };
}

function generateEdge(edge: any) {
  return {
    sourceNodeId: edge.fromNodeId,
    targetNodeId: edge.endNodeId,
    type: "polyline",
  };
}

function changeJobRadio(val: number) {
  selectJobInfoList.value = jobInfoList.value.filter((e) =>
    val === 0 ? e.jobType === 0 : e.jobType !== 0
  );
}

/** 打开task_info弹窗 */
function handleOpenDialog() {
  jobComposeVisible.visible = true;
  const nodes = lf.value!.getGraphRawData().nodes;
  const edges = lf.value!.getGraphRawData().edges;
  console.log(111, nodes);
  if (jobCompId.value) {
    jobComposeVisible.title = "修改任务组";
    JobInfoAPI.getFormData(jobCompId.value).then((data) => {
      Object.assign(formData, data);
      formData.nodes = JSON.stringify(nodes);
      formData.edges = JSON.stringify(edges);
    });
  } else {
    formData.nodes = JSON.stringify(nodes);
    formData.edges = JSON.stringify(edges);
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

function getJobInfoList() {
  JobInfoAPI.getList().then((data: any) => {
    jobInfoList.value = data;
    selectJobInfoList.value = data.filter((e) => e.jobType === 0);
  });
}

onMounted(() => {
  getJobInfoList();

  lf.value = new LogicFlow({
    container: lfRef.value,
    grid: true,
    multipleSelectKey: "alt",
    autoExpand: false,
    allowResize: true,
    allowRotate: true,
    keyboard: {
      enabled: true,
    },
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
