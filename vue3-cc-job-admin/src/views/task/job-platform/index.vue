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
            type="primary"
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
        <el-tooltip
          class="box-item"
          effect="dark"
          content="运行日志"
          placement="top"
        >
          <el-button
            type="info"
            :icon="Document"
            circle
            @click="getJobTriggerLog"
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
          style="height: 800px; overflow-y: scroll"
          @node-click="selectJobCompNode"
        />
      </div>
      <div ref="lfRef" class="logic-flow" />
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

    <Dialog v-if="diaLogVisible" :z-index="1001" @close="closeDiaLog">
      <template #header>运行日志</template>
      <div>
        <Codemirror
          v-model:value="execLog"
          :options="cmOptions"
          :height="logHeight"
          :KeepCursorInEnd="true"
          @change="change"
        />
      </div>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
// TODO 需要优化代码结构
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
  Document,
} from "@element-plus/icons-vue";
import CustomJava from "./node/CustomJava";
import CustomPython from "./node/CustomPython";
import CustomShell from "./node/CustomShell";
import CustomPhp from "./node/CustomPhp";
import CustomNodejs from "./node/CustomNodejs";
import CustomApi from "./node/CustomAPI";
import CustomBean from "./node/CustomBean";
import CustomSql from "./node/CustomSql";
import CustomPowerShell from "./node/CustomPowerShell";

import JobInfoAPI from "@/api/task/job-info";
import { ref } from "vue";
import Snowflake from "@/utils/snowflake";
import { onBeforeRouteLeave } from "vue-router";
import CustomGroup from "@/components/CustomGroup/CustomGroup";
import router from "@/router";
import Dialog from "@/components/Dialog/Dialog.vue";
import JobLogAPI from "@/api/task/job-log";
import CustomRect, { CustomRectModel, CustomRectView } from "./node/CustomRect";

LogicFlow.use(Control); // 控制面板
LogicFlow.use(DndPanel); // 拖拽面板
LogicFlow.use(MiniMap);

const lf = ref<any>(null);
const lfRef = ref<any>(null);
const patternItems = [
  {
    type: "custom-rect",
    label: "添加任务",
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
const runTime = ref([]);
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
        if (DynamicCustomGroup == node.type) {
          ElMessage.warning("暂不支持任务组选择");
          return;
        }
        jobDialog.value = true;
        jobNodeEditId.value = node.id;
      },
    },
    {
      text: "编辑节点",
      callback(node: { properties: { jobId: number | null } }) {
        if (
          node.properties.jobId === null ||
          node.properties.jobId === undefined
        ) {
          ElMessage.warning("请选择任务或任务组");
          return;
        }
        if (DynamicCustomGroup == node.type) {
          ElMessage.warning("暂不支持任务组编辑");
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
        if (DynamicCustomGroup == node.type) {
          ElMessage.warning("暂不支持任务组复制");
          return;
        }
        lf.value.graphModel.cloneNode(node.id);
      },
    },
    {
      text: "属性",
      callback(node: any) {
        console.log(node);
        let startTime = "";
        let endTime = "";

        if (runTime.value.length > 0) {
          const n = runTime.value.find((v) => v[0] == node.id) as any;
          startTime = n[1];
          endTime = n[2];
        }
        alert(`
          节点id：${node.id}
          节点任务开始时间：${startTime}
          节点任务结束时间：${endTime}
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
const DynamicCustomGroup = "CustomGroup";
const diaLogVisible = ref(false);
const execLog = ref("");
const cmOptions = {
  mode: "log",
  theme: "default",
};
const logHeight = ref("100vh");
const fromLineNum = ref(0);
let logRun: any = null;
const pullFailCount = ref(0);

onBeforeRouteLeave((to, from, next) => {
  if (triggerOneVisible.value) {
    alert("有任务正在运行，请先停止任务～");
  } else {
    next();
  }
});

function change(msg: any, cm: any) {
  const scrollInfo = cm.getScrollInfo();
  cm.scrollTo(scrollInfo.left, scrollInfo.height);
}

function closeDiaLog() {
  diaLogVisible.value = false;
  fromLineNum.value = 0;
  execLog.value = "";
  pullFailCount.value = 0;
  logRunStop(logRun);
}

function convertContent(str: string) {
  return str
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, "'")
    .replace(/&#39;/g, "'")
    .replace(/&quot;/g, '"');
}

function run(id: number) {
  logRun = setInterval(() => {
    getExecuteTaskLog(id);
  }, 2000);
}

/**
 * 任务日志停止运行
 * @param content
 */
function logRunStop(content: string) {
  if (logRun != null) {
    window.clearInterval(logRun);
    logRun = null;
    execLog.value += convertContent(content);
  }
}

/**
 * 获取任务日志
 * @param id
 */
function getExecuteTaskLog(id: number) {
  if (pullFailCount.value++ > 20) {
    logRunStop("日志加载完成.....");
    return;
  }

  JobLogAPI.logDetailCat(id, fromLineNum.value).then((data: any) => {
    if (data.code == 200) {
      if (!data.content) {
        console.log("pullLog fail");
        return;
      }
      if (fromLineNum.value != data.content.fromLineNum) {
        console.log("pullLog fromLineNum not match");
        return;
      }
      if (fromLineNum.value > data.content.toLineNum) {
        console.log("pullLog already line-end");

        // valid end
        if (data.content.end) {
          logRunStop("[Rolling Log Finish]");
          return;
        }
        return;
      }

      // append content
      fromLineNum.value = data.content.toLineNum + 1;

      execLog.value += convertContent(data.content.logContent);

      pullFailCount.value = 0;
    } else {
      ElMessage.error("pullLog fail:" + data.msg);
    }
  });
}
//-------------------------------------------------------log------------------------------

/**
 * 清除画布
 */
async function clearGraph() {
  jobSelectId.value = undefined;
  jobCompId.value = null;
  await clearData();
}

const GLUE_NODE_TYPE_MAP: Record<string, string> = {
  SQL: "custom-sql",
  API: "custom-api",
  BEAN: "custom-bean",
  GLUE_GROOVY: "custom-java",
  GLUE_SHELL: "custom-shell",
  GLUE_PYTHON: "custom-python",
  GLUE_PHP: "custom-php",
  GLUE_NODEJS: "custom-nodejs",
  GLUE_POWERSHELL: "custom-powershell",
};
/**
 * 关闭节点编辑
 */
function updateNodeTypeByGlueType(
  jobId: number,
  jobDesc: string,
  glueType: string
) {
  console.log("updateNodeTypeByGlueType", jobId, glueType);
  const nodes = lf.value.getGraphRawData().nodes;
  const edges = lf.value.getGraphRawData().edges;
  const _node = nodes.find((n) => n.properties.jobId === jobId);
  if (_node) {
    const nodeType =
      GLUE_NODE_TYPE_MAP[glueType] ||
      (_node.nodeType === DynamicCustomGroup ? DynamicCustomGroup : "rect");
    const graphModel = lf.value.graphModel;
    // 创建新的节点对象，并指定新的 type
    const newNode = {
      ..._node, // 保留原有节点的属性
    };
    newNode.type = nodeType;
    //newNode.isPause = isPause;
    //保留原来的线
    const _edges = edges.filter(
      (e) => e.sourceNodeId === _node.id || e.targetNodeId === _node.id
    );
    console.log(">>>>", _edges);
    // 删除节点
    graphModel.deleteNode(_node.id);
    // 重新添加节点
    lf.value.addNode(newNode);
    if (_edges.length > 0) {
      _edges.forEach((edge) => {
        lf.value.addEdge(edge);
      });
    }
    // 更新节点属性
    const node = lf.value.getNodeModelById(newNode.id);
    node.updateText(jobDesc);
    // 更新节点样式
    //const styleKey = node.type === DynamicCustomGroup ? "stroke" : "fill";
    // node.setStyle(styleKey, node.isPause ? "#CCCCCC" : "#FFFFFF");
  }
}

function closeDraw(jobId: number, jobDesc: string, glueType: string) {
  updateNodeTypeByGlueType(jobId, jobDesc, glueType);
  jobNodeVisible.value = false; // 这行是控制编辑弹窗的关闭
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

/**
 * 运行日志
 */
function getJobTriggerLog() {
  if (triggerOneVisible.value) {
    ElMessage.warning("有任务正在运行，请先停止任务～");
    return;
  }
  if (jobCompId.value === null) {
    ElMessage.warning("请选择任务~");
    return;
  }
  router.push({
    path: "/job/job-log",
    query: { id: jobCompId.value },
  });
}

/**
 * 选择任务组
 * @param node
 */
async function selectJobCompNode(node: any) {
  if (triggerOneVisible.value) {
    ElMessage.warning("有任务正在运行，请先停止任务～");
    return;
  }
  const graphModel = lf.value.graphModel;
  jobCompId.value = node.id;

  await clearData();

  let data = {} as any;
  const formMap = {
    id: node.id,
    type: 0,
    x: 0,
    y: 0,
  };

  await JobInfoAPI.getJobCompose(formMap).then((res) => {
    data = res;
  });

  const jobNode = data.jobNode;
  taskTitle.value = jobNode.jobName;
  const newNodes = data.nodes;
  const newEdges = data.edges;
  addJobNodes(newNodes, graphModel, newEdges);
}

function cancelDialog() {
  jobSelectId.value = undefined;
  jobDialog.value = false;
}

/**
 * 校验边
 */
function validateEdge() {
  const nodes = lf.value.getGraphRawData().nodes;
  const nodesIds = [] as any;
  nodes.forEach((node: any) => {
    if (node.type === DynamicCustomGroup) {
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
 * 增加节点
 * @param newNodes 新的节点
 * @param graphModel 画布模型
 * @param newEdges 新的边
 */
function addJobNodes(newNodes: any[], graphModel: any, newEdges: any[]) {
  // 添加新节点
  newNodes.forEach((node) => {
    graphModel.addNode(generateNode(node));
  });
  // 重新设置任务组的孩子节点
  newNodes.forEach((n) => {
    const node = lf.value.getNodeModelById(n.id);
    node.setStyle("stroke", n.isPause == 1 ? "#0031ff" : "#000");
    if (n.nodeType === DynamicCustomGroup) {
      JSON.parse(n.children).forEach((id: any) => node.addChild(id));
    }
  });
  // 添加新边
  newEdges.forEach((e) => {
    graphModel.addEdge(generateEdge(e));
  });
}

/**
 * 确认选择任务
 */
async function confirmDialog() {
  const _node = lf.value.getNodeModelById(jobNodeEditId.value);

  const _jobInfo = jobInfoList.value.find(
    (e: any) => e.id === jobSelectId.value
  ) as any;

  const graphModel = lf.value.graphModel;
  if (_jobInfo.jobType === 2) {
    graphModel.deleteNode(_node.id);
    //新增任务组
    let data = {} as any;

    const formMap = {
      id: jobSelectId.value,
      type: 1,
      x: _node.x,
      y: _node.y,
    };
    await JobInfoAPI.getJobCompose(formMap).then((res) => (data = res));
    const newNodes = data.nodes;
    const newEdges = data.edges;

    addJobNodes(newNodes, graphModel, newEdges);
  } else {
    _node.setProperty("jobId", jobSelectId.value);
    _node.updateText(_jobInfo.jobDesc);
    console.log(_jobInfo.glueType);
    updateNodeTypeByGlueType(
      jobSelectId.value,
      _jobInfo.jobDesc,
      _jobInfo.glueType
    );
  }
  cancelDialog();
}

/**
 * 产生节点
 * @param node
 */
function generateNode(node: any) {
  console.log("generateNode", node, node.nodeType);
  const properties = JSON.parse(node.properties);
  // 类型判断逻辑
  const nodeType =
    node.nodeType ||
    (node.nodeType === DynamicCustomGroup ? DynamicCustomGroup : "rect");
  if (node.nodeType === DynamicCustomGroup) {
    properties.children = JSON.parse(properties.children);
  }

  return {
    id: node.id,
    text: node.jobName,
    type: nodeType,
    x: node.nodePositionX,
    y: node.nodePositionY,
    properties: properties,
    children: node.children != null ? JSON.parse(node.children) : node.children,
  };
}

/**
 * 产生边
 * @param edge
 */
function generateEdge(edge: any) {
  return {
    sourceNodeId: edge.fromNodeId,
    targetNodeId: edge.endNodeId,
    type: "bezier",
  };
}

function changeJobRadio(val: string | number | boolean | undefined) {
  selectJobInfoList.value = jobInfoList.value.filter((e: any) =>
    val === 0 ? e.jobType === 0 : e.jobType !== 0
  );
}

const snowflake = new Snowflake(31, 31, true, new Date());
const randomId = ref("");

/**
 * 任务执行一次
 */
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
    .then((data: any) => {
      if (data) {
        diaLogVisible.value = true;
        run(data);
      }
    })
    .catch((e) => {
      triggerOneVisible.value = false;
      ElMessage.error(e);
      execLog.value += "读取任务日志失败...";
    })
    .finally(() => {});
  ElMessage.success("执行任务成功");
  connectWs(jobId + ":" + randomId.value);
  triggerOneVisible.value = true;
  updateEdgeStyle();
}

function selectElements() {
  const elements = lf.value.graphModel.getSelectElements(true);
}

/**
 * 停止任务
 */
function stopTrigger() {
  if (jobCompId.value == null) {
    ElMessage.warning("请选择任务组～");
    return;
  }
  JobInfoAPI.stopJobCompose(jobCompId.value, randomId.value).then(() => {
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

/**
 * 更新边的状态
 */
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
  ws.value = new WebSocket(import.meta.env.VITE_APP_WS_ENDPOINT + id);
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
      _message.status == 5
    ) {
      // 关闭任务
      setTimeout(() => {
        triggerOneVisible.value = false;
        updateEdgeStyle();
      }, 1000);
    } else if (
      _message.jobId == jobCompId.value &&
      _message.randomId == randomId.value &&
      _message.status == 9
    ) {
      runTime.value = JSON.parse(_message.result);
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
      const style = _node.type === DynamicCustomGroup ? "stroke" : "fill";
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
  let len = array.length;
  let sum = 0;
  for (let i = 0; i < len; i++) {
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
  //注册自定义矩形节点
  lf.value.register(CustomJava);
  lf.value.register(CustomPython);
  lf.value.register(CustomShell);
  lf.value.register(CustomPhp);
  lf.value.register(CustomNodejs);
  lf.value.register(CustomApi);
  lf.value.register(CustomBean);
  lf.value.register(CustomSql);
  lf.value.register(CustomPowerShell);
  lf.value.register(CustomRect);

  lf.value.register(CustomGroup);
  lf.value.setDefaultEdgeType("bezier");
  lf.value.render({
    nodes: nodes.value,
    edges: edges.value,
  });

  lf.value.on("node:mouseenter", ({ data }) => {
    const node = lf.value.getNodeModelById(data.id);
    node.buttonGroupOpacity = 1;
  });

  lf.value.on("node:mouseleave", ({ data }) => {
    const node = lf.value.getNodeModelById(data.id);
    node.buttonGroupOpacity = 0;
  });

  // 状态切换事件
  lf.value.on("custom:node-toggle-status", ({ nodeId }) => {
    const node = lf.value.getNodeModelById(nodeId);
    node.isPause = !node.isPause;

    if (node.properties.jobId == null) {
      ElMessage.warning("请选择任务～");
      return;
    }

    JobInfoAPI.pauseJob(node.properties.jobId, node.isPause ? 1 : 0);

    // 更新节点样式
    const styleKey = node.type === DynamicCustomGroup ? "stroke" : "fill";
    node.setStyle(styleKey, node.isPause ? "#f0f0f0" : "#FFFFFF");
  });

  //复制节点事件
  lf.value.on("custom:node-copy", ({ nodeId }) => {
    const node = lf.value.getNodeModelById(nodeId);
    if (DynamicCustomGroup == node.type) {
      ElMessage.warning("暂不支持任务组复制");
      return;
    }
    lf.value.graphModel.cloneNode(nodeId);
  });

  //编辑节点事件
  lf.value.on("custom:node-edit", ({ nodeId }) => {
    const node = lf.value.getNodeModelById(nodeId);
    if (node.properties.jobId === null || node.properties.jobId === undefined) {
      ElMessage.warning("请选择任务或任务组");
      return;
    }
    jobNodeVisible.value = true;
    nodeJobId.value = node.properties.jobId;
    nowDate.value = new Date();
  });

  //选择任务事件
  lf.value.on("custom:node-task-edit", ({ nodeId }) => {
    jobDialog.value = true;
    jobNodeEditId.value = nodeId;
  });

  lf.value.on("custom:node-delete", ({ nodeId }) => {
    lf.value.deleteNode(nodeId);
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
  }

  .logic-flow {
    width: 100%;
    height: 80vh;
  }
}
</style>
