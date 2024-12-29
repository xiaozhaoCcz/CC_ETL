<template>
  <div class="app-container">
    <div class="task_rank_top">
      <div class="btn_left_list">
        <el-button
          v-if="!showTaskVisible"
          type="primary"
          @click="showTaskVisible = true"
        >
          展示任务
        </el-button>
        <el-button v-else type="info" @click="showTaskVisible = false">
          隐藏任务
        </el-button>

      </div>
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
    <div class="task_rank">
      <div class="task_set_tree">
        <el-input v-model="filterTaskSetText" placeholder="Filter keyword" />

        <el-tree
          ref="treeTaskSetRef"
          class="filter-tree"
          :data="taskSetList"
          :props="defaultProps"
          default-expand-all
          :filter-node-method="filterTaskSetNode"
          @node-click="selectTaskSetNode"
        />
      </div>
      <div v-if="showTaskVisible" class="task_info_tree">
        <el-input v-model="filterTaskInfoText" placeholder="Filter keyword" />

        <el-tree
          ref="taskInfoTreeRef"
          class="filter-tree"
          :data="taskInfoList"
          :props="defaultProps"
          default-expand-all
          :filter-node-method="filterTaskInfoNode"
        >
          <template #default="{ node, data }">
            <span @dblclick="handleDblClick(node)">
              {{ node.label }}
            </span>
            <div style="margin-left: 5px">
              <el-tag
                v-if="data.jobType == 2"
                type="primary"
                round
                size="small"
              >
                任务组
              </el-tag>
            </div>
          </template>
        </el-tree>
      </div>
      <div class="vue_flow_platform">
        <VueFlow
          :nodes="nodes"
          :edges="edges"
          @connect="onConnect"
          @edge-update="onEdgeUpdate"
          @edge-update-start="onEdgeUpdateStart"
          @edge-update-end="onEdgeUpdateEnd"
        >
          <MiniMap />
          <Background />
        </VueFlow>
      </div>
    </div>

    <EditTaskRank
      :taskRankVisible="taskRankVisible"
      :formData="formData"
      @close="handleCloseDialog"
    />

    <EditTaskNode
      :taskNodeVisible="taskNodeVisible"
      :nodeTaskId="nodeTaskId"
      :nowDate="nowDate"
      @close="closeDraw"
    />
  </div>
</template>
<script setup lang="ts">
/* these are necessary styles for vue flow */
import "@vue-flow/core/dist/style.css";
/* this contains the default theme, these are optional styles */
import "@vue-flow/core/dist/theme-default.css";
import { ref, onMounted } from "vue";
import { VueFlow, useVueFlow, MarkerType } from "@vue-flow/core";
import { Background } from "@vue-flow/background";
import { MiniMap } from "@vue-flow/minimap";
import { Folder, ArrowRight, Loading } from "@element-plus/icons-vue";
import TaskInfoAPI, { TaskInfoForm } from "@/api/task/task-info";
import Snowflake from "@/utils/snowflake";
const {
  updateEdge,
  onNodesChange,
  onNodeDragStop,
  onEdgesChange,
  onNodeDoubleClick,
} = useVueFlow();
import EditTaskRank from "./operation/edit-task-rank.vue";
import EditTaskNode from "./operation/edit-task-node.vue";

const showTaskVisible = ref(true);
const taskRankVisible = reactive({
  title: "",
  visible: false,
});
const triggerOneVisible = ref(false);
const taskRankId = ref(null);
const formData = reactive<TaskInfoForm>({
  executorTimeout: 600000,
});
const g_position = ref([140, 140]);
const taskTitle = ref("");

const taskSetList = ref([
  {
    id: 1,
    label: "默认分组",
    children: [],
  },
]);
const taskInfoList = ref([
  {
    id: 1,
    label: "默认分组",
    jobType: -1,
    children: [],
  },
]);

const nodes = ref([]);
const edges = ref([]);

/** 打开task_info弹窗 */
function handleOpenDialog() {
  taskRankVisible.visible = true;
  if (taskRankId.value) {
    taskRankVisible.title = "修改任务组";
    TaskInfoAPI.getFormData(taskRankId.value).then((data) => {
      Object.assign(formData, data);
      formData.nodes = JSON.stringify(nodes.value);
      formData.edges = JSON.stringify(edges.value);
    });
  } else {
    formData.nodes = JSON.stringify(nodes.value);
    formData.edges = JSON.stringify(edges.value);
    taskRankVisible.title = "新增任务组";
  }
}

function handleCloseDialog() {
  const keys = Object.keys(formData);
  let obj: { [name: string]: string } = {};
  keys.forEach((item) => {
    obj[item] = "";
  });
  Object.assign(formData, obj);
  taskRankVisible.visible = false;
}

function generateNode(val: any) {
  console.log(val);
  g_position.value[0] = g_position.value[0] + 10;
  g_position.value[1] = g_position.value[1] + 10;
  return {
    id: "node:" + Date.now().toString(),
    data: {
      taskId: val.id,
      label: val.label,
      randomId: "",
    },
    position: { x: g_position.value[0], y: g_position.value[0] },
  };
}

function generateEdge(val: any) {
  return {
    id: val.source + "-" + val.target,
    source: val.source,
    target: val.target,
    updatable: true,
    // animated: true,
    // style: { stroke: "#10b981" },
    markerEnd: MarkerType.ArrowClosed,
  };
}

const handleDblClick = (node) => {
  if (triggerOneVisible.value) {
    ElMessage.warning("有任务正在运行，请先停止任务～");
    return;
  }
  // 在这里处理双击事件
  if (node.data != undefined && node.data) {
    console.log("双击了节点:", node.data);
    // 往nodes添加节点
    nodes.value.push(generateNode(node.data));
    console.log(nodes.value);
  }
};

function onConnect(params) {
  edges.value.push(generateEdge(params));
  console.log("edge updated", params, edges);
}

function onEdgeUpdate({ edge, connection }) {
  updateEdge(edge, connection);
  console.log("end update", edge, connection);
}

function onEdgeUpdateStart(edge) {
  console.log("start update", edge);
}

function onEdgeUpdateEnd(edge) {
  console.log("end update", edge);
}

onNodesChange(async (changes) => {
  for (const change of changes) {
    if (change.type === "remove") {
      //removeNodes(change.id);
      removeNode(change.id);
      removeEdge(change.id);
      console.log("nodes", nodes, "edges", edges);
    }
  }
});

onNodeDragStop((event) => {
  console.log("Node drag stopped", event);
  const { id, position } = event.node;
  const node = nodes.value.find((node) => node.id === id);
  if (node) {
    node.position = position;
  }
});

onEdgesChange(async (changes) => {
  for (const change of changes) {
    if (change.type === "remove") {
      edges.value = edges.value.filter((edge) => edge.id !== change.id);
      console.log("Removed " + change.id, edges.value);
    }
  }
});

const nodeTaskId = ref(null);
const taskNodeVisible = ref(false);
const nowDate = ref(null);

onNodeDoubleClick(async (changes) => {
  if (triggerOneVisible.value) {
    ElMessage.warning("任务正在运行，请先停止任务～");
    return;
  }
  taskNodeVisible.value = true;
  nodeTaskId.value = changes.node.data.taskId;
  nowDate.value = new Date();
});

function closeDraw() {
  nodeTaskId.value = null;
  taskNodeVisible.value = false;
}

function removeNode(id) {
  nodes.value = nodes.value.filter((node) => node.id !== id);
}

function removeEdge(id) {
  edges.value = edges.value.filter((edge) => edge.target !== id);
}

const filterTaskSetText = ref("");

const filterTaskInfoText = ref("");
const taskInfoTreeRef = ref<InstanceType<typeof ElTree>>();
const treeTaskSetRef = ref<InstanceType<typeof ElTree>>();
const defaultProps = {
  children: "children",
  label: "label",
};

watch(filterTaskInfoText, (val) => {
  taskInfoTreeRef.value!.filter(val);
});

watch(filterTaskSetText, (val) => {
  treeTaskSetRef.value!.filter(val);
});

const snowflake = new Snowflake(31, 31, true);
const randomId = ref("");

function triggerOne() {
  if (taskRankId.value == null) {
    ElMessage.warning("请选择任务组～");
    return;
  }

  randomId.value = snowflake.nextId(1);

  nodes.value.forEach((node) => {
    node.data.randomId = randomId.value;
  });

  const taskId = taskRankId.value;
  const taskInfoTriggerDto = {};
  taskInfoTriggerDto.id = taskId;
  taskInfoTriggerDto.executorParam = taskId + ":" + randomId.value;
  TaskInfoAPI.triggerJob(taskInfoTriggerDto)
    .then((data) => {
      ElMessage.success("执行任务成功");
      connectWs(taskId + ":" + randomId.value);
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
  if (taskRankId.value == null) {
    ElMessage.warning("请选择任务组～");
    return;
  }
  TaskInfoAPI.stopTaskSet(taskRankId.value, randomId.value).then(() => {
    triggerOneVisible.value = false;
    updateEdgeStyle();
  });
}

function filterTaskInfoNode(value: string, data: any) {
  if (!value) return true;
  return data.label.includes(value);
}

function filterTaskSetNode(value: string, data: any) {
  if (!value) return true;
  return data.label.includes(value);
}

function updateEdgeStyle() {
  if (triggerOneVisible.value) {
    const convertEdge = [] as any;
    // 所有边变色
    edges.value.forEach((edge) => {
      // animated: true,
      // style: { stroke: "#10b981" },
      const obj = {} as any;
      Object.assign(obj, edge);
      obj.style = { stroke: "#10b981" };
      obj.animated = true;
      convertEdge.push(obj);
    });
    edges.value = convertEdge;
    return;
  }
  const convertEdge = [] as any;
  edges.value.forEach((edge) => {
    const obj = {} as any;
    Object.assign(obj, edge);
    obj.style = {};
    obj.animated = false;
    convertEdge.push(obj);
  });
  edges.value = convertEdge;
}

function selectTaskSetNode(node) {
  if (triggerOneVisible.value) {
    ElMessage.warning("有任务正在运行，请先停止任务～");
    return;
  }
  g_position.value = [140, 140];
  nodes.value = [];
  edges.value = [];
  taskRankId.value = node.id;
  TaskInfoAPI.getFormData(node.id).then((data) => {
    Object.assign(formData, data);
    taskTitle.value = data.jobDesc;
    const _nodes = JSON.parse(data.nodes);
    const _edges = JSON.parse(data.edges);

    _nodes.forEach((item) => {
      const nodeObj = {} as any;
      nodeObj.id = item.id + "";
      nodeObj.data = {
        taskId: item.taskId,
        label: item.taskName,
      };
      nodeObj.position = { x: item.nodePositionX, y: item.nodePositionY };
      //style: { border: "1px solid green", borderRadius: "8px", width: "140px" },
      // (nodeObj.style = {
      //   border: "1px solid #ccc",
      //   borderRadius: "8px",
      //   width: "140px",
      // }),
      nodes.value.push(nodeObj);
    });

    _edges.forEach((item) => {
      const edgeObj = {} as any;
      edgeObj.id = item.fromNodeId + "-" + item.endNodeId;
      edgeObj.source = item.fromNodeId + "";
      edgeObj.target = item.endNodeId + "";
      edgeObj.updatable = true;
      edgeObj.markerEnd = MarkerType.ArrowClosed;
      edges.value.push(edgeObj);
    });
  });
}

function getTaskInfoList() {
  TaskInfoAPI.getList().then((data) => {
    data.forEach((item) => {
      const obj = {};
      obj.id = item.id;
      obj.label = item.jobDesc;
      obj.jobType = item.jobType;

      taskInfoList.value[0].children.push(obj);
    });
  });
}

function getTaskSetList() {
  TaskInfoAPI.getList(2).then((data) => {
    data.forEach((item) => {
      const obj = {};
      obj.id = item.id;
      obj.label = item.jobDesc;
      taskSetList.value[0].children.push(obj);
    });
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
      _message.taskId == taskRankId.value &&
      _message.randomId == randomId.value
    ) {
      // 关闭任务
      setTimeout(() => {
        triggerOneVisible.value = false;
        updateEdgeStyle();
      }, 1000);
    }

    // 接收到消息后，需要做出相应的操作，比如更新节点或边
    const node = nodes.value.find(
      (node: any) =>
        node.data.taskId == _message.taskId &&
        node.data.randomId == _message.randomId
    );
    //
    const color = getNodeColor(_message.status);
    console.log("node", node, nodes.value, color);
    if (node) {
      node.style = {
        border: "1px solid " + color,
        color: color,
      };
      nodes.value = [...nodes.value]; // 触发Vue反应性更新
    }
  };
};

const getNodeColor = (status: number) => {
  switch (status) {
    case 0:
      return "red";
    case 1:
      return "green";
    case 2:
      return "gold";
    default:
      return "#000";
  }
};

onMounted(() => {
  getTaskInfoList();
  getTaskSetList();
});
</script>

<style lang="scss" scoped>
.task_rank_top {
  margin-bottom: 2px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .task_title{
     font-weight: bold;
    font-size: 18px;
  }

  .task_title:before{
    content: '任务组：';
    color: #5174fd;
  }

  .btn_right_list {
    background: #fff;
    padding: 5px 10px;
    border-radius: 8px;
  }
}

.task_rank {
  width: 100%;
  display: flex;
  justify-content: left;
  height: 80vh;
  .task_set_tree {
    width: 20%;
    background: #fff;
    padding: 10px;
    overflow-y: auto;
  }

  .task_info_tree {
    border-left: 1px solid #f0f0f0;
    width: 20%;
    background: #fff;
    padding: 10px;
    overflow-y: auto;
  }

  .vue_flow_platform {
    background: #fff;
    border: 1px solid #f0f0f0;
    width: 100%;
  }
}
</style>
