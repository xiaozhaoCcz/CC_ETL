<template>
  <div class="app-container">
    <div class="tool-list">
      <div>任务编排</div>
      <span class="task_title">{{ taskTitle }}</span>
      <div class="btn_right_list">
        <el-button
          v-if="triggerOneVisible"
          type="warning"
          :icon="Loading"
          circle
          @click="stopTrigger"
        />
        <el-button
          v-else
          type="success"
          :icon="ArrowRight"
          circle
          @click="triggerOne"
        />
        <el-button
          type="info"
          :icon="Folder"
          circle
          @click="handleOpenDialog"
        />
      </div>
    </div>
    <div class="job-platform">
      <div class="job-group-tree">
        <el-input v-model="filterJobCompText" placeholder="Filter keyword" />
        <el-tree
          ref="treeTaskSetRef"
          class="filter-tree"
          :data="jobCompList"
          :props="defaultProps"
          default-expand-all
          :filter-node-method="filterJobCompNode"
          @node-click="selectJobCompNode"
        />
      </div>
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
import { ArrowRight, Folder, Loading } from "@element-plus/icons-vue";
import JobInfoAPI from "@/api/task/job-info";
import { ref } from "vue";
import Snowflake from "@/utils/snowflake";
import { onBeforeRouteLeave } from "vue-router";

LogicFlow.use(Control); // 控制面板
LogicFlow.use(DndPanel); // 拖拽面板

const lf = ref(null);
const lfRef = ref(null);
const patternItems = [
  {
    type: "circle",
    label: "添加任务",
    text: "Circle",
    icon: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAAH6ji2bAAAABGdBTUEAALGPC/xhBQAAAnBJREFUOBGdVL1rU1EcPfdGBddmaZLiEhdx1MHZQXApraCzQ7GKLgoRBxMfcRELuihWKcXFRcEWF8HBf0DdDCKYRZpnl7p0svLe9Zzbd29eQhTbC8nv+9zf130AT63jvooOGS8Vf9Nt5zxba7sXQwODfkWpkbjTQfCGUd9gIp3uuPP8bZ946g56dYQvnBg+b1HB8VIQmMFrazKcKSvFW2dQTxJnJdQ77urmXWOMBCmXM2Rke4S7UAW+/8ywwFoewmBps2tu7mbTdp8VMOkIRAkKfrVawalJTtIliclFbaOBqa0M2xImHeVIfd/nKAfVq/LGnPss5Kh00VEdSzfwnBXPUpmykNss4lUI9C1ga+8PNrBD5YeqRY2Zz8PhjooIbfJXjowvQJBqkmEkVnktWhwu2SM7SMx7Cj0N9IC0oQXRo8xwAGzQms+xrB/nNSUWVveI48ayrFGyC2+E2C+aWrZHXvOuz+CiV6iycWe1Rd1Q6+QUG07nb5SbPrL4426d+9E1axKjY3AoRrlEeSQo2Eu0T6BWAAr6COhTcWjRaYfKG5csnvytvUr/WY4rrPMB53Uo7jZRjXaG6/CFfNMaXEu75nG47X+oepU7PKJvvzGDY1YLSKHJrK7vFUwXKkaxwhCW3u+sDFMVrIju54RYYbFKpALZAo7sB6wcKyyrd+aBMryMT2gPyD6GsQoRFkGHr14TthZni9ck0z+Pnmee460mHXbRAypKNy3nuMdrWgVKj8YVV8E7PSzp1BZ9SJnJAsXdryw/h5ctboUVi4AFiCd+lQaYMw5z3LGTBKjLQOeUF35k89f58Vv/tGh+l+PE/wG0rgfIUbZK5AAAAABJRU5ErkJggg==",
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
const triggerOneVisible = ref(false);
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
const filterJobCompText = ref("");
const jobCompList = ref<any>([
  {
    id: 1,
    label: "默认分组",
    children: [],
  },
]);
const defaultProps = {
  children: "children",
  label: "label",
};
const taskTitle = ref("");

onBeforeRouteLeave((to, from, next) => {
  if (triggerOneVisible.value) {
    alert("有任务正在运行，请先停止任务～");
  } else {
    next();
  }
});

function filterJobCompNode(value: string, data: any) {
  if (!value) return true;
  return data.label.includes(value);
}

async function selectJobCompNode(node: any) {
  if (triggerOneVisible.value) {
    ElMessage.warning("有任务正在运行，请先停止任务～");
    return;
  }
  lf.value.graphModel.clearData();

  jobCompId.value = node.id;
  await JobInfoAPI.getJobCompose(node.id, 0).then((res) => {
    const jobNode = res.jobNode;
    taskTitle.value = jobNode.jobName;
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
  });
}

function cancelDialog() {
  jobSelectId.value = undefined;
  jobDialog.value = false;
}

async function confirmDialog() {
  const _node = lf.value!.getNodeModelById(jobNodeEditId.value);
  const _jobInfo = jobInfoList.value.find((e) => e.id === jobSelectId.value);

  const graphModel = lf.value.graphModel;
  if (_jobInfo.jobType === 2) {
    //新增任务组
    let data = {} as any;
    await JobInfoAPI.getJobCompose(jobSelectId.value, 1).then(
      (res) => (data = res)
    );
    const jobNode = data.jobNode;
    const newNodes = data.nodes;
    const newEdges = data.edges;
    newNodes.push(jobNode);
    newNodes.forEach((node: any) => {
      graphModel.addNode(generateNode(node));
    });
    newNodes.forEach((n: any) => {
      const node = lf.value.getNodeModelById(n.id);
      if (n.nodeType === "dynamic-group") {
        JSON.parse(n.children).forEach((id: any) => node.addChild(id));
      }
    });

    newEdges.forEach((e: any) => {
      graphModel.addEdge(generateEdge(e));
    });

    const oNodes = lf.value!.getGraphRawData().nodes;
    const oEdges = lf.value!.getGraphRawData().edges;
    //删除之前的节点
    const delNodes = lf.value!.getGraphRawData().nodes;
    const delEdges = lf.value!.getGraphRawData().edges;

    delNodes.forEach((n: any) => {
      graphModel.deleteNode(n.id);
    });
    delEdges.forEach((e: any) => {
      graphModel.deleteEdgeById(e.id);
    });

    oNodes.forEach((n: any) => {
      graphModel.addNode(n);
    });
    oEdges.forEach((e: any) => {
      graphModel.addEdge(e);
    });

    graphModel.moveNodes([jobNode.id], _node.x - 500, _node.y - 230);

    graphModel.deleteNode(_node.id);
  } else {
    _node.setProperty("jobId", jobSelectId.value);
    _node.updateText(_jobInfo.jobDesc);
  }
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

const snowflake = new Snowflake(31, 31, true, new Date());
const randomId = ref("");

function triggerOne() {
  if (jobCompId.value == null) {
    ElMessage.warning("请选择任务组～");
    return;
  }

  randomId.value = snowflake.nextId(1) as string;

  const _nodes = lf.value!.getGraphRawData().nodes;

  _nodes.forEach((node: any) => {
    const _node = lf.value!.getNodeModelById(node.id);
    _node.setProperty("randomId", randomId.value);
  });

  const jobId = jobCompId.value;
  const jobInfoTriggerDto = {} as any;
  jobInfoTriggerDto.id = jobId;
  jobInfoTriggerDto.executorParam = jobId + ":" + randomId.value;
  JobInfoAPI.triggerJob(jobInfoTriggerDto)
    .then((data) => {
      ElMessage.success("执行任务成功");
      connectWs(jobId + ":" + randomId.value);
      triggerOneVisible.value = true;
      updateEdgeStyle();
    })
    .catch((e) => {
      triggerOneVisible.value = true;
      ElMessage.error(e);
    })
    .finally(() => {});
}

function stopTrigger() {
  if (jobCompId.value == null) {
    ElMessage.warning("请选择任务组～");
    return;
  }
  JobInfoAPI.stopTaskSet(jobCompId.value, randomId.value).then(() => {
    triggerOneVisible.value = false;
    updateEdgeStyle();
  });
}

/** 打开task_info弹窗 */
function handleOpenDialog() {
  jobComposeVisible.visible = true;
  const nodes = lf.value!.getGraphRawData().nodes;
  const edges = lf.value!.getGraphRawData().edges;
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

function getJobCompList() {
  JobInfoAPI.getList(2).then((data: any) => {
    data.forEach((item: any) => {
      const obj = {} as any;
      obj.id = item.id;
      obj.label = item.jobDesc;
      jobCompList.value[0].children.push(obj);
    });
  });
}

function updateEdgeStyle() {
  const { edges } = lf.value.getGraphRawData() ?? {};
  if (triggerOneVisible.value) {
    edges?.forEach(({ id }) => {
      lf.value.openEdgeAnimation(id);
    });
    return;
  }
  edges?.forEach(({ id }) => {
    lf.value.closeEdgeAnimation(id);
  });
}

//--------------------------------------------------ws------------------
const ws = ref();
const message = ref();
const reconnectAttempts = ref(0);
const maxReconnectAttempts = 3; // 自定义最大重试次数

const connectWs = (id: string) => {
  ws.value = new WebSocket("ws://localhost:8989/ws/" + id);
  ws.value.onopen = () => {
    reconnectAttempts.value = 0;
    console.log("连接成功");
  };
  ws.value.onclose = () => {
    console.log("连接断开");
    reconnectAttempts.value++;
    if (reconnectAttempts.value <= maxReconnectAttempts) {
      console.log("进行重连");
      connectWs(id);
    } else {
      console.log("连接关闭");
    }
  };
  ws.value.onmessage = (e: any) => {
    const _message = JSON.parse(e.data);
    message.value = _message;
    console.log("接收到消息", _message);

    if (
      _message.jobId == jobCompId.value &&
      _message.randomId == randomId.value
    ) {
      // 关闭任务
      setTimeout(() => {
        triggerOneVisible.value = false;
        updateEdgeStyle();
      }, 1000);
    }

    // 接收到消息后，需要做出相应的操作，比如更新节点或边
    const nodes = lf.value!.getGraphRawData().nodes;
    const node = nodes.find(
      (node: any) =>
        node.properties.jobId == _message.jobId &&
        node.properties.randomId == _message.randomId
    );
    //
    const color = getNodeColor(_message.status);
    if (node) {
      const _node = lf.value!.getNodeModelById(node.id);
      if (_node.type === "dynamic-group") {
        _node.setStyle("stroke", color);
      } else {
        _node.setStyle("fill", color);
      }
    }
  };
};

const getNodeColor = (status: number) => {
  switch (status) {
    case 0:
      return "#CC0000";
    case 1:
      return "#66FF99";
    case 2:
      return "#FFFF33";
    default:
      return "#000";
  }
};

onMounted(() => {
  getJobCompList();
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
.tool-list {
  margin-bottom: 2px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .task_title {
    font-weight: bold;
    font-size: 18px;
  }

  .task_title:before {
    content: "任务组：";
    color: #5174fd;
  }

  .btn_right_list {
    background: #fff;
    padding: 5px 10px;
    border-radius: 8px;
  }
}

.job-platform {
  width: 100%;
  display: flex;
  justify-content: left;

  .job-group-tree {
    width: 20%;
    background: #fff;
    padding: 10px;
    overflow-y: auto;
  }

  .logic-flow {
    width: 100%;
    height: 80vh;
  }
}
</style>
