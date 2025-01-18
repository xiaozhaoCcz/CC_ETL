<template>
  <div class="app-container">
    <div class="tool-list">
      <div>任务编排</div>
      <span class="task_title">{{ taskTitle }}</span>
      <div class="btn_right_list">
        <el-tooltip
          v-if="triggerOneVisible"
          class="box-item"
          effect="dark"
          content="停止运行"
          placement="top"
        >
          <el-button
            type="warning"
            :icon="Loading"
            circle
            @click="stopTrigger"
          />
        </el-tooltip>
        <el-tooltip
          v-else
          class="box-item"
          effect="dark"
          content="开始运行"
          placement="top"
        >
          <el-button
            type="success"
            :icon="ArrowRight"
            circle
            @click="triggerOne"
          />
        </el-tooltip>
        <el-tooltip
          class="box-item"
          effect="dark"
          content="保存"
          placement="top"
        >
          <el-button
            type="info"
            :icon="Folder"
            circle
            @click="handleOpenDialog"
          />
        </el-tooltip>
        <el-tooltip
          class="box-item"
          effect="dark"
          content="清除画布"
          placement="top"
        >
          <el-button
            type="danger"
            :icon="CircleClose"
            circle
            @click="clearGraph"
          />
        </el-tooltip>
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
      <div class="logic-flow" ref="lfRef" />
    </div>

    <EditJobComp
      :taskRankVisible="jobComposeVisible"
      :formData="formData"
      @close="handleCloseDialog"
    />

    <EditJobNode
      :taskNodeVisible="jobNodeVisible"
      :nodeTaskId="nodeJobId"
      :nowDate="nowDate"
      @close="closeDraw"
    />

    <el-dialog v-model="jobDialog" style="width: 400px" title="选择任务">
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
  MiniMap,
} from "@logicflow/extension";
import "@logicflow/core/lib/style/index.css";
import "@logicflow/extension/lib/style/index.css";
import EditJobComp from "@/views/task/job-platform/operation/edit-job-compose.vue";
import EditJobNode from "@/views/task/job-platform/operation/edit-job-node.vue";
import {
  ArrowRight,
  CircleClose,
  Folder,
  Loading,
} from "@element-plus/icons-vue";
import JobInfoAPI from "@/api/task/job-info";
import { ref } from "vue";
import Snowflake from "@/utils/snowflake";
import { onBeforeRouteLeave } from "vue-router";

LogicFlow.use(Control); // 控制面板
LogicFlow.use(DndPanel); // 拖拽面板
LogicFlow.use(MiniMap);

const lf = ref<any>(null);
const lfRef = ref<any>(null);
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
const jobSelectId = ref<any>(undefined);
const jobNodeEditId = ref(undefined);
const nodeJobId = ref<number | undefined>(undefined);
const jobNodeVisible = ref(false);
const nowDate = ref<Date | undefined>(undefined);
const menuConfig = {
  nodeMenu: [
    {
      text: "删除",
      callback(node: { id: string }) {
        lf.value.deleteNode(node.id);
      },
    },
    {
      text: "选择任务",
      callback(node: { id: string }) {
        jobDialog.value = true;
        jobNodeEditId.value = node.id;
      },
    },
    {
      text: "编辑节点",
      callback(node: { properties: { jobId: number | null } }) {
        if (triggerOneVisible.value) {
          ElMessage.warning("任务正在运行，请先停止任务～");
          return;
        }
        if (
          node.properties.jobId === null ||
          node.properties.jobId === undefined
        ) {
          ElMessage.warning("请选择任务或任务组");
          return;
        }
        jobNodeVisible.value = true;
        nodeJobId.value = node.properties.jobId;
        nowDate.value = new Date();
      },
    },
    {
      text: "复制",
      callback(node: any) {
        if (node.type === "dynamic-group") {
          alert("暂时还不支持任务组复制~");
          return;
        } else {
          lf.value.graphModel.cloneNode(node.id);
        }
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
      text: "删除",
      callback(edge: { id: string }) {
        lf.value.graphModel.deleteEdgeById(edge.id);
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
  // edgeMenu: false, // 删除默认的边右键菜单
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

async function clearGraph() {
  jobSelectId.value = undefined;
  jobCompId.value = null;
  await clearData();
}

function closeDraw() {
  nodeJobId.value = null;
  jobNodeVisible.value = false;
}

function filterJobCompNode(value: string, data: any) {
  if (!value) return true;
  return data.label.includes(value);
}

/**
 * ！！！！清除画布不能使用graphModel.clearData();，前端坑是真的多，坑死我了
 */
async function clearData() {
  const nodes = lf.value.getGraphRawData().nodes;
  const edges = lf.value.getGraphRawData().edges;
  const graphModel = lf.value.graphModel;
  nodes.forEach((node: any) => {
    const _node = graphModel.getNodeModelById(node.id);
    if (_node) {
      graphModel.deleteNode(_node.id);
    }
  });

  edges.forEach((edge: any) => {
    const _edge = graphModel.getEdgeModelById(edge.id);
    if (_edge) {
      graphModel.deleteEdgeById(_edge.id);
    }
  });
}

async function selectJobCompNode(node: any) {
  if (triggerOneVisible.value) {
    ElMessage.warning("有任务正在运行，请先停止任务～");
    return;
  }
  const graphModel = lf.value.graphModel;
  jobCompId.value = node.id;

  await clearData();

  let data = {} as any;
  await JobInfoAPI.getJobCompose(node.id, 0).then((res) => {
    data = res;
  });

  const jobNode = data.jobNode;
  taskTitle.value = jobNode.jobName;
  const newNodes = data.nodes;
  const newEdges = data.edges;
  await addJobNodes(newNodes, graphModel, newEdges);
}

function cancelDialog() {
  jobSelectId.value = undefined;
  jobDialog.value = false;
}

function validateEdge() {
  const nodes = lf.value.getGraphRawData().nodes;
  const nodesIds = [] as any;
  nodes.forEach((node: any) => {
    if (node.type === "dynamic-group") {
      const children = [] as any;
      children.push(...node.children);
      nodesIds.push(children);
    }
  });
  const edges = lf.value.getGraphRawData().edges;
  const errorEdges = [] as any;
  edges.forEach((e: any) => {
    nodesIds.forEach((children: any) => {
      if (
        (children.includes(e.sourceNodeId) &&
          !children.includes(e.targetNodeId)) ||
        (children.includes(e.targetNodeId) &&
          !children.includes(e.sourceNodeId))
      ) {
        errorEdges.push(e);
      }
    });
  });
  errorEdges.forEach((e: any) => {
    const _edge = lf.value.getEdgeModelById(e.id);
    _edge.style.stroke = "red";
  });
  return;
}

/**
 * ！！！！离谱一段逻辑，我也不知道为什么这样写才能成功。不然就出现各种奇怪的bug，折磨死我了，前端真的太难了！！！！
 * @param newNodes
 * @param graphModel
 * @param newEdges
 */
async function addJobNodes(newNodes: any[], graphModel: any, newEdges: any[]) {
  // 添加新节点
  newNodes.forEach((node) => {
    graphModel.addNode(generateNode(node));
  });
  // 重新设置任务组的孩子节点
  newNodes.forEach((n) => {
    const node = lf.value.getNodeModelById(n.id);
    if (n.nodeType === "dynamic-group") {
      JSON.parse(n.children).forEach((id: any) => node.addChild(id));
    }
  });
  // 添加新边
  newEdges.forEach((e) => {
    graphModel.addEdge(generateEdge(e));
  });

  // 获取当前图数据
  const nodes = lf.value.getGraphRawData().nodes;
  const edges = lf.value.getGraphRawData().edges;

  await clearData();

  // 重新添加节点和边
  nodes.forEach((n: any) => {
    graphModel.addNode(n);
  });
  edges.forEach((e: any) => {
    graphModel.addEdge(e);
  });
}

async function confirmDialog() {
  const _node = lf.value.getNodeModelById(jobNodeEditId.value);
  const _jobInfo = jobInfoList.value.find(
    (e: any) => e.id === jobSelectId.value
  ) as any;

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

    await addJobNodes(newNodes, graphModel, newEdges);

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

function changeJobRadio(val: string | number | boolean | undefined) {
  selectJobInfoList.value = jobInfoList.value.filter((e: any) =>
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
  jobInfoTriggerDto.executorParam = randomId.value;
  JobInfoAPI.triggerJob(jobInfoTriggerDto)
    .then(() => {
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

function selectElements() {
  const elements = lf.value.graphModel.getSelectElements(true);
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
async function handleOpenDialog() {
  const nodes = lf.value!.getGraphRawData().nodes;
  const edges = lf.value!.getGraphRawData().edges;

  const valObj = {
    nodes: JSON.stringify(nodes),
    edges: JSON.stringify(edges),
  };

  let flag = true;
  await JobInfoAPI.validateJobComposeEdge(valObj).then((data: any) => {
    flag = data;
  });

  if (!flag) {
    ElMessage.error("不同组的节点不能连接~");
    return;
  }

  jobComposeVisible.visible = true;
  // 对任务边进行校验
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
    selectJobInfoList.value = data.filter((e: any) => e.jobType === 0);
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
const reconnectAttempts = ref(0);
const maxReconnectAttempts = 3; // 自定义最大重试次数

const connectWs = (id: string) => {
  // TODO 后端做多节点部署时，需要修改
  ws.value = new WebSocket("ws://175.178.249.190/ccjob-ws/" + id);
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
    if (
      _message.jobId == jobCompId.value &&
      _message.randomId == randomId.value &&
      _message.status != 2
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
      const style = _node.type === "dynamic-group" ? "stroke" : "fill";
      _node.setStyle(style, color);
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

function avg(array: any) {
  var len = array.length;
  var sum = 0;
  for (var i = 0; i < len; i++) {
    sum += array[i];
  }
  return sum / len;
}

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

  lf.value.extension.control.addItem({
    key: "horizontal-alignment",
    iconClass: "Minus",
    title: "横向对齐",
    text: "横向对齐",
    onClick: (lf: any, ev: any) => {
      const elements = lf.graphModel.getSelectElements(true);
      const arrY = elements.nodes.map((n: any) => n.y);
      const avgY = avg(arrY);
      elements.nodes.forEach((node: any) => {
        const _node = lf.getNodeModelById(node.id);
        _node.moveTo(node.x, avgY);
      });
    },
  });
  lf.value.extension.control.addItem({
    key: "vertical-alignment",
    iconClass: "XX",
    title: "纵向对齐",
    text: "纵向对齐",
    onClick: (lf: any, ev: any) => {
      const elements = lf.graphModel.getSelectElements(true);
      const arrX = elements.nodes.map((n: any) => n.x);
      const avgX = avg(arrX);
      elements.nodes.forEach((node: any) => {
        const _node = lf.getNodeModelById(node.id);
        _node.moveTo(avgX, node.y);
      });
    },
  });

  lf.value.extension.dndPanel.setPatternItems(patternItems);
  lf.value.extension.menu.setMenuConfig(menuConfig);
  lf.value.render({
    nodes: nodes.value,
    edges: edges.value,
  });
  //！！一定要在render下面才能显示
  lf.value.extension.miniMap.show();

  const { eventCenter } = lf.value.graphModel;
  eventCenter.on("graph:updated", () => {
    validateEdge();
  });
  eventCenter.on("selection:selected", () => {
    selectElements();
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
