<script setup lang="ts">
// ================== 1. 依赖与组件引入 ==================
import LogicFlow from "@logicflow/core";
import {
  Menu,
  DndPanel,
  DynamicGroup,
  SelectionSelect,
  MiniMap,
} from "@logicflow/extension";
import "@logicflow/core/lib/style/index.css";
import "@logicflow/extension/lib/style/index.css";
import { Close, FolderOpened, Document } from "@element-plus/icons-vue";
import { nextTick, onMounted, reactive, ref, watch, onBeforeUnmount } from "vue";
import { useJobInfoStoreHook, useNavbarStoreHook, usePageStoreHook } from "@/store";
import { ElMessage } from "element-plus";
import { debounce, throttle, PerformanceMonitor } from "@/utils/performance";

// LogicFlow 自定义节点组件
import CustomJava from "./node/CustomJava";
import CustomPython from "./node/CustomPython";
import CustomShell from "./node/CustomShell";
import CustomPhp from "./node/CustomPhp";
import CustomNodejs from "./node/CustomNodejs";
import CustomApi from "./node/CustomAPI";
import CustomBean from "./node/CustomBean";
import CustomSql from "./node/CustomSql";
import CustomPowerShell from "./node/CustomPowerShell";
import CustomRect from "./node/CustomRect";
import CustomGroup from "./node/CustomGroup.ts";

// API 和工具类
import JobInfoAPI from "@/api/job-info.ts";
import JobLogAPI from "@/api/job-log.ts";
import Snowflake from "@/utils/snowflake.ts";
import {
  generateNode,
  generateEdge,
  clearData,
  convertContent,
  GLUE_NODE_TYPE_MAP,
  PLATFORM_HEIGHT_LIMITS,
  WEBSOCKET_CONFIG,
  DYNAMIC_CUSTOM_GROUP,
  clearHighlight as clearHighlightUtil,
  updateEdgeStyleForTaskGroup as updateEdgeStyleForTaskGroupUtil,
  layoutNodes as layoutNodesUtil,
  clearCanvas as clearCanvasUtil,
  selectNodes as selectNodesUtil,
  selectElements,
  getNodeColor,
  canvasOperations,
} from "@/utils/logicflow";

// 组件
import Log from "@/components/Log/EnhancedLog.vue";
import EditJobNode from "@/views/side/operation/edit-job-node.vue";

// ================== 2. 类型定义与接口声明 ==================

/**
 * 日志标签页接口定义
 */
interface LogTab {
  id: string;
  jobId: number | null;
  randomId: string;
  label: string;
  isRunning: boolean;
}

/**
 * 任务组状态接口定义
 */
interface JobState {
  fromLineNum: number;
  pullFailCount: number;
  logRun: any;
  ws: WebSocket | null;
  randomId: string;
}

/**
 * 表单数据接口定义
 */
interface FormData {
  executorTimeout: number;
  nodes?: string;
  edges?: string;
  glueType?: string;
  executorHandler?: string;
}

/**
 * WebSocket消息接口定义
 */
interface WebSocketMessage {
  jobId: number;
  randomId: string;
  status: number;
  result?: string;
}

// 扩展Window接口以支持refreshTreeData
declare global {
  interface Window {
    refreshTreeData?: () => void;
  }
}

// ================== 3. 响应式数据声明 ==================

/** 当前页面标签列表 */
const pageTaps = ref<any[]>([]);

/** 当前选中的任务组ID */
const jobId = ref<number | null>(null);

/** 日志组件引用 */
const logger = ref<any>(null);

/** 日志标签页列表 */
const logTabs = ref<LogTab[]>([]);

/** 当前激活的日志标签页ID */
const activeLogTab = ref<string | null>(null);

/** 存储每个任务组对应的LogicFlow实例 */
const lfInstances = ref<Record<number, any>>({});

/** 存储DOM引用 */
const lfRefs = ref<Record<number, HTMLElement>>({});

/** 拖拽状态 */
const isDragging = ref(false);

/** 平台区域高度（vh单位） */
const platformHeight = ref(PLATFORM_HEIGHT_LIMITS.DEFAULT);

/** 主容器引用 */
const mainContainerRef = ref<HTMLElement>();

/** 日志组件引用映射 */
const loggerRefs = ref<Record<string, any>>({});

// ================== 4. 全局变量声明 ==================

/** 节点高亮相关变量 */
let highlightedElement: any = null;
let highlightedType: string | null = null;
let originalStyle: any = null;

/** LogicFlow 主实例引用 */
const lf = ref<any>(null);

/** 雪花算法实例 */
const snowflake = new Snowflake(31, 31, true, new Date());

/** 随机ID */
const randomId = ref("");

/** 运行时间记录 */
const runTime = ref<any[]>([]);

/** 全局日志获取定时器（向后兼容） */
let logRun: any = null;

/** 全局日志行号计数（向后兼容） */
const fromLineNum = ref(0);

/** 全局日志获取失败计数（向后兼容） */
const pullFailCount = ref(0);

/** 任务组状态管理 */
const jobStates = ref<Map<number, JobState>>(new Map());

// ================== 5. 表单与对话框状态 ==================

/** 表单数据 */
const formData = reactive<FormData>({
  executorTimeout: 600000,
});

/** 任务节点编辑对话框可见性 */
const jobNodeVisible = ref(false);

/** 当前编辑的节点任务ID */
const nodeJobId = ref<number | null>(null);

/** 当前正在编辑的节点ID */
const currentEditingNodeId = ref<string | null>(null);

/** 任务选择对话框可见性 */
const jobDialog = ref(false);

/** 任务类型单选值 */
const jobRadio = ref(0);

/** 任务信息列表 */
const jobInfoList = ref<any[]>([]);

/** 可选任务信息列表 */
const selectJobInfoList = ref<any[]>([]);

/** 选中的任务ID */
const jobSelectId = ref<number | undefined>(undefined);

/** 任务节点编辑ID */
const jobNodeEditId = ref<string | undefined>(undefined);

/** 当前选中的节点 */
const selectNode = ref<any>(null);

// ================== 6. 工具函数定义 ==================

/**
 * 获取或创建任务组状态
 * @param jobId 任务组ID
 * @returns 任务组状态
 */
const getJobState = (jobId: number): JobState => {
  if (!jobStates.value.has(jobId)) {
    jobStates.value.set(jobId, {
      fromLineNum: 0,
      pullFailCount: 0,
      logRun: null,
      ws: null,
      randomId: "",
    });
  }
  return jobStates.value.get(jobId)!;
};

/**
 * 清理任务组状态
 * @param jobId 任务组ID
 */
const clearJobState = (jobId: number): void => {
  const state = jobStates.value.get(jobId);
  if (state) {
    // 清理定时器
    if (state.logRun) {
      window.clearInterval(state.logRun);
      state.logRun = null;
    }
    // 关闭WebSocket连接
    if (state.ws) {
      state.ws.close();
      state.ws = null;
    }
    // 移除状态
    jobStates.value.delete(jobId);
  }
};

/**
 * 获取指定标签页的日志组件引用
 * @param tabId 标签页ID
 * @returns 日志组件引用
 */
const getLoggerRef = (tabId: string) => {
  return loggerRefs.value[tabId];
};

/**
 * 刷新任务树数据
 */
const refreshTreeData = (): void => {
  if (typeof window.refreshTreeData === "function") {
    window.refreshTreeData();
  }
};

// ================== 7. Watch监听器管理 ==================

/**
 * 监听页面标签变化，实时同步 pageTaps
 * 当页面状态发生变化时，自动更新当前页面的标签列表
 */
watch(
  () => usePageStoreHook().pages,
  (pages: any) => {
    pageTaps.value = pages;
  },
  {
    immediate: true,
    deep: true,
  }
);

/**
 * 监听当前页面变化，处理任务组切换逻辑
 * 当用户切换任务组时，自动加载对应的LogicFlow实例和数据
 */
watch(
  () => usePageStoreHook().getCurrentPage(),
  (pageId: any) => {
    if (pageId !== 0) {
      jobId.value = pageId;
      // 确保实例存在后再操作
      if (lfInstances.value[pageId]) {
        selectJobCompNode(pageId);
        // 切换任务组时更新边的样式以反映该任务组的运行状态
        setTimeout(() => {
          updateEdgeStyleForTaskGroupUtil(
            pageId,
            lfInstances.value[pageId],
            usePageStoreHook().getCurrentPageRunStatus
          );
        }, 100);
      } else {
        // 如果实例不存在，需要先创建实例
        selectPage(pageId);
      }
    } else {
      // 当pageId为0时，不执行clearData，显示空白画布
      console.log("当前没有选中任何任务组");
    }
  },
  {
    immediate: true,
    deep: true,
  }
);

/**
 * 监听任务信息变化，处理任务新增和修改
 * 当任务信息发生变化时，自动处理新增或修改逻辑
 */
watch(
  () => useJobInfoStoreHook().getJobInfo(),
  async (jobInfo: any) => {
    if (jobInfo.jobId) {
      // 修改任务
    } else {
      // 新增任务
      await addJobNode(jobInfo);
      await selectPage(jobInfo.parentId);
    }
  }
);

/**
 * 监听导航栏动作，处理各种操作命令
 * 根据不同的操作类型执行相应的功能
 */
watch(
  () => useNavbarStoreHook().getAction(),
  (actionObj: any) => {
    if (!actionObj?.actionName) return;

    const actionName = actionObj.actionName;
    const currentPageId = actionObj.pageId || usePageStoreHook().getCurrentPage();
    const currentLf = lfInstances.value[currentPageId] || lf.value;

    switch (actionName) {
      case "save":
        saveOrUpdateJob();
        break;
      case "start-current-job":
        if (currentPageId) {
          triggerOne();
        }
        break;
      case "stop-current-job":
        if (currentPageId) {
          stopTrigger();
        }
        break;
      case "undo":
        canvasOperations.undo(currentLf);
        break;
      case "redo":
        canvasOperations.redo(currentLf);
        break;
      case "fit":
        canvasOperations.fit(currentLf);
        break;
      case "zoom-in":
        canvasOperations.zoomIn(currentLf);
        break;
      case "zoom-out":
        canvasOperations.zoomOut(currentLf);
        break;
      case "clear":
        try {
          clearCanvasUtil(currentLf);
          ElMessage.success("画布已清空");
        } catch (error: any) {
          ElMessage.error(error.message || "清除画布失败");
        }
        break;
      case "layout-horizontal":
        layoutNodesUtil("horizontal", currentLf);
        break;
      case "layout-vertical":
        layoutNodesUtil("vertical", currentLf);
        break;
      case "select":
        selectNodesUtil(currentLf);
        break;
      default:
        console.warn(`未知的操作类型: ${actionName}`);
    }
  },
  {
    immediate: true,
    deep: true,
  }
);

/**
 * 监听定位节点信息，处理节点高亮定位
 * 当需要定位到特定节点时，自动高亮显示该节点
 */
watch(
  () => usePageStoreHook().getLoactionObject(),
  (locationObj: any) => {
    if (!locationObj || Object.keys(locationObj).length === 0) return;
    // 获取当前LogicFlow实例
    const currentPageId = usePageStoreHook().getCurrentPage();
    const currentLf = lfInstances.value[currentPageId];

    if (!currentLf || !locationObj.id) return;

    try {
      // 先清除之前的高亮
      clearHighlightUtil(highlightedElement, highlightedType, originalStyle);

      if (locationObj.type === 4) {
        // 定位到节点
        const nodeElement = currentLf.getNodeModelById(locationObj.id);
        if (nodeElement) {
          // 记录原始样式
          originalStyle = {
            ...nodeElement.getData().properties?.style,
            fill: nodeElement.style?.fill,
          };
          // 修改节点的边框颜色
          nodeElement.setStyle("fill", "#0B57D0");
          highlightedElement = nodeElement;
          highlightedType = "node";
          document.addEventListener(
            "mousedown",
            () => clearHighlightUtil(highlightedElement, highlightedType, originalStyle),
            true
          );
        } else {
          console.warn(`未找到节点: ${locationObj.id}`);
        }
      } else {
        // 定位到边
        const edgeElement = currentLf.getEdgeModelById(locationObj.id);
        if (edgeElement) {
          // 记录原始样式
          originalStyle = {
            ...edgeElement.getData().properties?.style,
            stroke: edgeElement.style?.stroke,
          };
          // 设置边的颜色
          edgeElement.setStyle("stroke", "#0B57D0");
          highlightedElement = edgeElement;
          highlightedType = "edge";
          document.addEventListener(
            "mousedown",
            () => clearHighlightUtil(highlightedElement, highlightedType, originalStyle),
            true
          );
        } else {
          console.warn(`未找到边: ${locationObj.id}`);
        }
      }
    } catch (error) {
      console.error("定位节点时出错:", error);
    }
  },
  {
    immediate: true,
    deep: true,
  }
);

/**
 * 监听删除节点的功能
 */
watch(
  () => useJobInfoStoreHook().getNodeToDelete(),
  (nodeId) => {
    if (nodeId) {
      JobInfoAPI.deleteJobNode(nodeId)
        .then(() => {
          ElMessage.success("删除节点成功");
          // 遍历所有 LogicFlow 实例，删除节点
          Object.values(lfInstances.value).forEach((lfInstance: any) => {
            if (lfInstance && lfInstance.getNodeModelById(nodeId)) {
              lfInstance.deleteNode(nodeId);
              // 如果有边也一并删掉
              const edges = lfInstance.getGraphRawData().edges;
              edges.forEach((edge: any) => {
                if (
                  edge.sourceNodeId === String(nodeId) ||
                  edge.targetNodeId === String(nodeId)
                ) {
                  lfInstance.deleteEdge(edge.id);
                }
              });
            }
          });
          useJobInfoStoreHook().clearNodeToDelete();
          refreshTreeData();
        })
        .catch(() => {
          ElMessage.error("删除节点失败");
        });
    }
  }
);

watch(
  () => useJobInfoStoreHook().getNodeToEdit(),
  (node: any) => {
    if (!node) return;
    // 遍历所有 LogicFlow 实例，找到对应 jobId 的节点并刷新
    Object.values(lfInstances.value).forEach((lfInstance: any) => {
      if (!lfInstance) return;
      const nodes = lfInstance.getGraphRawData().nodes;
      const _node = nodes.find((n: any) => n.properties.jobId == node.jobId);
      if (_node) {
        updateNodeTypeByGlueType(node.jobId, node.jobDesc, node.glueType, lfInstance);
      }
    });
    setTimeout(() => {
      useJobInfoStoreHook().clearNodeToEdit();
    }, 1);
  },
  {
    deep: true,
  }
);

//============================watch管理====================

// ================== 8. 拖拽功能管理 ==================

/**
 * 鼠标按下事件处理，开始拖拽
 * @param e 鼠标事件
 */
const handleMouseDown = (e: MouseEvent): void => {
  isDragging.value = true;
  document.addEventListener("mousemove", handleMouseMove);
  document.addEventListener("mouseup", handleMouseUp);
  e.preventDefault();
};

/**
 * 鼠标移动事件处理，更新分隔线位置
 * @param e 鼠标事件
 */
const handleMouseMove = (e: MouseEvent): void => {
  throttledMouseMove(e);
};

/**
 * 鼠标松开事件处理，结束拖拽
 */
const handleMouseUp = (): void => {
  isDragging.value = false;
  document.removeEventListener("mousemove", handleMouseMove);
  document.removeEventListener("mouseup", handleMouseUp);
};

/**
 * 组件卸载时清理事件监听器、WebSocket连接和日志管理器
 */
onBeforeUnmount(() => {
  document.removeEventListener("mousemove", handleMouseMove);
  document.removeEventListener("mouseup", handleMouseUp);

  // 清理所有WebSocket连接
  clearAllWsConnections();

  // 清理所有日志管理器
  LogManagerFactory.destroyAll();
});

// ================== 8. 页面管理 ==================

/**
 * 选择任务组页面并初始化 LogicFlow 实例
 * @param id 任务组ID
 */
async function selectPage(id: number): Promise<void> {
  performanceMonitor.startTimer("selectPage");

  try {
    // 先更新状态
    usePageStoreHook().setCurrentPage(id);
    jobId.value = id;

    // 等待DOM更新
    await nextTick();

    // 确保所有容器正确设置可见性
    Object.keys(lfRefs.value).forEach((pageId) => {
      const pageIdNum = parseInt(pageId);
      if (lfRefs.value[pageIdNum]) {
        // 显式设置样式
        lfRefs.value[pageIdNum].style.display = pageIdNum === id ? "block" : "none";
      }
    });

    // 再等待DOM更新
    await nextTick();
    await new Promise((resolve) => setTimeout(resolve, 100));

    // 确保容器已准备好
    if (!lfRefs.value[id]) {
      setTimeout(() => selectPage(id), 300);
      return;
    }

    // 检查是否已经有该任务组的实例
    if (!lfInstances.value[id]) {
      // 初始化LogicFlow实例
      lfInstances.value[id] = initLogicFlowInstance(id);
      lf.value = lfInstances.value[id];

      if (lf.value) {
        // 加载数据
        await selectJobCompNode(id);
      }
    } else {
      lf.value = lfInstances.value[id];

      // 强制激活画布并刷新
      lfInstances.value[id].resize();
    }

    // 切换任务组时，确保边动画状态正确
    setTimeout(() => {
      updateEdgeStyleForTaskGroupUtil(
        id,
        lfInstances.value[id],
        usePageStoreHook().getCurrentPageRunStatus
      );
    }, 200);
  } catch (err) {
    console.error(`处理任务组 ${id} 切换时出错:`, err);
  } finally {
    const duration = performanceMonitor.endTimer("selectPage");
    console.log(`页面切换耗时: ${duration}ms`);
  }
}

/**
 * 关闭指定任务组页面，并清理相关状态
 * @param id 任务组ID
 */
function closePage(id: number): void {
  // 清理 LogicFlow 实例
  if (lfInstances.value[id]) {
    try {
      // 销毁 LogicFlow 实例
      lfInstances.value[id].destroy();
    } catch (e) {
      console.warn(`销毁LogicFlow实例时出错:`, e);
    }
    // 删除实例引用
    delete lfInstances.value[id];
  }

  // 清理 DOM 引用（虽然 setLfRef 也会清理，但这里主动清理更安全）
  if (lfRefs.value[id]) {
    delete lfRefs.value[id];
  }

  // 清理相关的日志标签页
  logTabs.value = logTabs.value.filter((tab) => tab.jobId !== id);
  if (
    activeLogTab.value &&
    logTabs.value.find((tab) => tab.id === activeLogTab.value)?.jobId === id
  ) {
    activeLogTab.value = null;
  }

  // 清理任务组的状态（WebSocket连接、定时器等）
  clearJobState(id);

  // 移除页面状态
  usePageStoreHook().removePage(id);
  if (id === usePageStoreHook().getCurrentPage()) {
    usePageStoreHook().getLastPage();
  }
}

// ================== 10. 日志管理 ==================

// 导入日志管理工具
import { LogManagerFactory } from "@/utils/logManager";

// 日志管理器映射
const logManagers = ref<Record<string, any>>({});

/**
 * 获取或创建日志管理器
 * @param jobId 任务组ID
 * @returns 日志管理器实例
 */
const getLogManager = (jobId: number): any => {
  const key = `job_${jobId}`;
  if (!logManagers.value[key]) {
    logManagers.value[key] = LogManagerFactory.getInstance(key, {
      maxLogs: 10000,
      enablePerformance: true,
      flushInterval: 100,
    });
  }
  return logManagers.value[key];
};

/**
 * 重置日志状态
 * @param specificJobId 可选，指定要重置的任务组ID，不提供则重置当前激活的日志标签页
 */
function logReset(specificJobId?: number | null): void {
  console.log(
    "重置日志状态",
    specificJobId ? `指定任务组: ${specificJobId}` : "当前激活日志"
  );

  if (specificJobId) {
    // 重置指定任务组的状态
    const state = getJobState(specificJobId);
    state.fromLineNum = 0;
    state.pullFailCount = 0;

    // 重置对应的日志管理器
    const logManager = getLogManager(specificJobId);
    logManager.reset();

    // 如果指定了任务组ID，只重置该任务组对应的日志组件
    const tabId = `${specificJobId}`;
    const loggerRef = getLoggerRef(tabId);
    if (loggerRef) {
      loggerRef.reset();
    } else {
      console.warn(`未找到任务组 ${specificJobId} 的日志组件`);
    }
  } else {
    // 重置全局计数器 - 这些是全局状态，只在需要时重置
    fromLineNum.value = 0;
    pullFailCount.value = 0;

    // 清除定时器 - 定时器是全局的，需要在任务停止或切换时清除
    if (logRun != null) {
      window.clearInterval(logRun);
      logRun = null;
    }

    // 否则重置当前激活的日志标签页
    if (activeLogTab.value) {
      const loggerRef = getLoggerRef(activeLogTab.value);
      if (loggerRef) {
        loggerRef.reset();
      } else {
        console.warn(`未找到标签页 ${activeLogTab.value} 的日志组件`);
      }
    }
  }
}

/**
 * 启动指定任务的日志获取
 * @param id 执行日志ID
 */
function run(id: number): void {
  // 保存当前执行的日志ID，这是后端返回的，与任务组ID不同
  const currentExecuteLogId = id;
  const currentJobId = jobId.value;

  if (!currentJobId) {
    return;
  }

  // 获取或创建该任务组的状态
  const state = getJobState(currentJobId);

  // 重置该任务组的日志计数
  state.fromLineNum = 0;
  state.pullFailCount = 0;

  // 清除该任务组之前的定时器（如果存在）
  if (state.logRun) {
    window.clearInterval(state.logRun);
  }

  // 为该任务组创建独立的日志获取定时器
  state.logRun = setInterval(() => {
    getExecuteTaskLog(currentExecuteLogId, currentJobId);
  }, 2000);
}

/**
 * 切换日志标签页
 * @param tabId 标签页ID
 */
const switchLogTab = (tabId: string): void => {
  activeLogTab.value = tabId;
};

/**
 * 关闭日志标签页
 * @param tabId 标签页ID
 */
const closeLogTab = (tabId: string): void => {
  const index = logTabs.value.findIndex((tab) => tab.id === tabId);
  if (index > -1) {
    logTabs.value.splice(index, 1);

    // 如果关闭的是当前激活的标签页，切换到其他标签页
    if (activeLogTab.value === tabId) {
      if (logTabs.value.length > 0) {
        activeLogTab.value = logTabs.value[logTabs.value.length - 1].id;
      } else {
        activeLogTab.value = null;
      }
    }

    // 清理对应的日志组件引用
    delete loggerRefs.value[tabId];
  }
};

/**
 * 获取任务执行日志
 * @param id 执行日志ID
 * @param targetJobId 可选的目标任务组ID
 */
function getExecuteTaskLog(id: number, targetJobId?: number): void {
  // 如果提供了目标任务组ID，使用该任务组的状态；否则使用全局状态（向后兼容）
  const currentJobId = targetJobId || jobId.value;
  let currentFromLineNum: number;
  let currentPullFailCount: number;

  if (targetJobId) {
    const state = getJobState(targetJobId);
    currentFromLineNum = state.fromLineNum;
    currentPullFailCount = state.pullFailCount;
    state.pullFailCount++;
  } else {
    currentFromLineNum = fromLineNum.value;
    currentPullFailCount = pullFailCount.value++;
  }

  if (currentPullFailCount > 20) {
    logRunStop("日志加载完成.....", targetJobId);
    return;
  }

  JobLogAPI.logDetailCat(id, currentFromLineNum).then((data: any) => {
    if (data.code == 200) {
      if (!data.content) {
        return;
      }
      if (currentFromLineNum != data.content.fromLineNum) {
        return;
      }
      if (currentFromLineNum > data.content.toLineNum) {
        // valid end
        if (data.content.end) {
          logRunStop("[Rolling Log Finish]", targetJobId);
          return;
        }
        return;
      }

      // append content
      const newFromLineNum = data.content.toLineNum + 1;

      // 更新对应任务组的状态
      if (targetJobId) {
        const state = getJobState(targetJobId);
        state.fromLineNum = newFromLineNum;
      } else {
        fromLineNum.value = newFromLineNum;
      }

      // 使用正确的任务ID作为标签页的唯一标识
      const currentTabId = `${currentJobId}`;

      // 获取对应的日志管理器
      if (currentJobId) {
        const logManager = getLogManager(currentJobId);
        logManager.addLogsFromText(convertContent(data.content.logContent), currentJobId);
      }

      // 获取对应标签页的日志组件
      const loggerRef = getLoggerRef(currentTabId);
      if (loggerRef) {
        loggerRef.addLogsFromText(convertContent(data.content.logContent));
      }

      // 重置失败计数
      if (targetJobId) {
        const state = getJobState(targetJobId);
        state.pullFailCount = 0;
      } else {
        pullFailCount.value = 0;
      }
    } else {
      ElMessage.error("pullLog fail:" + data.msg);
    }
  });
}

/**
 * 停止任务日志获取
 * @param content 结束内容
 * @param targetJobId 可选的目标任务组ID
 */
function logRunStop(content: string, targetJobId?: number): void {
  const currentJobId = targetJobId || jobId.value;

  if (targetJobId) {
    // 停止特定任务组的日志获取
    const state = jobStates.value.get(targetJobId);
    if (state && state.logRun) {
      window.clearInterval(state.logRun);
      state.logRun = null;
    }

    // 日志停止意味着任务完成，立即更新任务组状态
    usePageStoreHook().updatePageRunStatus(targetJobId, false);
    updateEdgeStyleForTaskGroupUtil(
      targetJobId,
      lfInstances.value[targetJobId],
      usePageStoreHook().getCurrentPageRunStatus
    );

    // 同时更新对应的日志标签页状态
    const tabId = `${targetJobId}`;
    const tab = logTabs.value.find((t) => t.id === tabId);
    if (tab) {
      tab.isRunning = false;
    }
  } else {
    // 停止全局日志获取（向后兼容）
    if (logRun != null) {
      window.clearInterval(logRun);
      logRun = null;

      // 更新当前任务组状态
      if (currentJobId) {
        usePageStoreHook().updatePageRunStatus(currentJobId, false);

        updateEdgeStyleForTaskGroupUtil(
          currentJobId,
          lfInstances.value[currentJobId],
          usePageStoreHook().getCurrentPageRunStatus
        );

        // 同时更新对应的日志标签页状态
        const tabId = `${currentJobId}`;
        const tab = logTabs.value.find((t) => t.id === tabId);
        if (tab) {
          tab.isRunning = false;
        }
      }
    }
  }

  // 使用正确的任务ID作为标签页的唯一标识
  const currentTabId = `${currentJobId}`;

  // 获取对应的日志管理器并添加结束日志
  if (currentJobId) {
    const logManager = getLogManager(currentJobId);
    logManager.addLogsFromText(convertContent(content), currentJobId);
  }

  // 获取对应标签页的日志组件
  const loggerRef = getLoggerRef(currentTabId);

  if (loggerRef) {
    loggerRef.addLogsFromText(convertContent(content));
  } else {
    console.error(`日志结束但未找到任务组 ${currentJobId} 的日志组件，无法添加结束日志`);
  }
}
//===========================log===============================

// ================== 11. LogicFlow 核心功能 ==================
/**
 * 初始化LogicFlow实例
 * @param pageId 页面ID
 * @returns LogicFlow实例
 */
function initLogicFlowInstance(pageId: number): any {
  const container = lfRefs.value[pageId];
  if (!container) {
    console.error(`[流程4] 错误：任务组 ${pageId} 的容器元素不存在`);
    return null;
  }

  // 如果已存在实例，检查其容器是否仍然有效
  if (lfInstances.value[pageId]) {
    const existingInstance = lfInstances.value[pageId];
    // 检查实例的容器是否还存在且可用
    if (existingInstance.container && existingInstance.container.parentNode) {
      return existingInstance;
    } else {
      // 清理无效实例
      try {
        existingInstance.destroy();
      } catch (e) {
        console.warn(`清理无效实例时出错:`, e);
      }
      delete lfInstances.value[pageId];
    }
  }

  // 确保元素可见
  const originalDisplay = container.style.display;
  container.style.display = "block";

  // 创建LogicFlow实例

  const newLf = new LogicFlow({
    container: container,
    background: {
      background: "#ECECEC", // 设置画布背景色
    },
    grid: {
      visible: false,
    },
    multipleSelectKey: "alt",
    autoExpand: false,
    allowResize: true,
    allowRotate: true,
    keyboard: {
      enabled: true,
    },
    plugins: [DynamicGroup, DndPanel, SelectionSelect, Menu, MiniMap],
    pluginsOptions: {
      miniMap: {
        width: 120,
        height: 120,
        showEdge: true,
        isShowHeader: false,
        isShowCloseIcon: false,
      },
    },
  });

  // 初始化逻辑

  (newLf.extension.menu as any).setMenuConfig(menuConfig);
  newLf.register(CustomJava);
  newLf.register(CustomPython);
  newLf.register(CustomShell);
  newLf.register(CustomPhp);
  newLf.register(CustomNodejs);
  newLf.register(CustomApi);
  newLf.register(CustomBean);
  newLf.register(CustomSql);
  newLf.register(CustomPowerShell);
  newLf.register(CustomRect);
  newLf.register(CustomGroup);
  newLf.setDefaultEdgeType("bezier");

  // 渲染空画布

  newLf.render({
    nodes: [],
    edges: [],
  });

  // 绑定事件
  bindEvents(newLf);

  // 恢复元素原始显示状态
  container.style.display = originalDisplay;

  // 显示MiniMap (必须在render之后)

  (newLf.extension.miniMap as any).show();

  // 保存实例并返回
  lfInstances.value[pageId] = newLf;

  const { eventCenter } = newLf.graphModel;

  eventCenter.on("selection:selected", () => {
    selectElements(lfInstances, lf);
  });
  return newLf;
}

/**
 * 为LogicFlow实例绑定事件处理
 * @param lfInstance LogicFlow实例
 */
function bindEvents(lfInstance: any): void {
  // 节点鼠标事件
  lfInstance.on("node:mouseenter", ({ data }: any) => {
    const node = lfInstance.getNodeModelById(data.id);
    node.buttonGroupOpacity = 1;
  });

  lfInstance.on("node:mouseleave", ({ data }: any) => {
    const node = lfInstance.getNodeModelById(data.id);
    node.buttonGroupOpacity = 0;
  });

  // 复制原有所有事件绑定
  lfInstance.on("custom:node-toggle-status", ({ nodeId }: any) => {
    const node = lfInstance.getNodeModelById(nodeId);
    node.isPause = !node.isPause;

    if (node.properties.jobId == null) {
      ElMessage.warning("请选择任务～");
      return;
    }

    JobInfoAPI.pauseJob(node.properties.jobId, node.isPause ? 1 : 0);

    // 更新节点样式
    const styleKey = node.type === DYNAMIC_CUSTOM_GROUP ? "stroke" : "fill";
    node.setStyle(styleKey, node.isPause ? "#409EEE" : "#FFFFFF");
  });

  lfInstance.on("custom:node-copy", async ({ nodeId }: any) => {
    const node = lfInstance.getNodeModelById(nodeId);
    if (DYNAMIC_CUSTOM_GROUP == node.type) {
      ElMessage.warning("暂不支持任务组复制");
      return;
    }

    // 获取原节点的数据
    const originalNodeData = node.getData();

    const originalJobId = originalNodeData.properties?.jobId;
    if (!originalJobId) {
      ElMessage.warning("原节点没有关联的任务，无法复制");
      return;
    }

    try {
      // 获取原任务的完整数据
      const originalJobData = await JobInfoAPI.getFormData(originalJobId);

      // 创建新任务的数据，移除ID相关字段，添加_copy后缀
      const newJobData = {
        ...originalJobData,
        id: undefined, // 移除id，让后端自动生成新的
        jobDesc: (originalJobData.jobDesc || "") + "_copy",
        addTime: undefined,
        updateTime: undefined,
        triggerLastTime: undefined,
        triggerNextTime: undefined,
        nodePositionX: originalJobData.nodePositionX + 50,
        nodePositionY: originalJobData.nodePositionY + 50,
      };

      // 调用后端API创建新任务

      const jobNode = await JobInfoAPI.saveJobNode(newJobData);

      // 检查后端返回的数据是否有效
      if (!jobNode) {
        console.error(`❌ 后端返回的数据为空`);
        ElMessage.error(
          "复制节点失败: 后端创建任务失败，未返回新任务数据。请检查后端API实现。"
        );
        return;
      }

      // 创建新的独立属性，使用新的jobId
      const newProperties = {
        ...JSON.parse(JSON.stringify(originalNodeData.properties || {})), // 深拷贝原属性
        jobId: jobNode.jobId, // 使用新任务的ID
        glueType: newJobData.glueType, // 确保glueType正确
      };

      // 创建新节点，位置稍微偏移
      const newNode = {
        type: originalNodeData.type,
        x: jobNode.nodePositionX,
        y: jobNode.nodePositionY,
        text: newJobData.jobDesc,
        properties: newProperties,
      };

      console.log(
        `🆔 新节点独立jobId: ${newProperties.jobId}, glueType: ${newProperties.glueType}`
      );

      // 添加新节点
      lfInstance.addNode(newNode);

      //刷新任务树
      refreshTreeData();

      ElMessage.success("节点复制成功，已创建独立的后端任务");
    } catch (error: any) {
      console.error(`❌ 复制节点失败:`, error);
      ElMessage.error("复制节点失败: " + (error.message || "未知错误"));
    }
  });

  lfInstance.on("custom:node-edit", ({ nodeId }: any) => {
    const node = lfInstance.getNodeModelById(nodeId);
    if (node.properties.jobId === null || node.properties.jobId === undefined) {
      ElMessage.warning("请选择任务或任务组");
      return;
    }
    jobNodeVisible.value = true;
    nodeJobId.value = node.properties.jobId;
    selectNode.value = node;
    // 保存当前正在编辑的节点ID，用于精确更新
    currentEditingNodeId.value = nodeId;
  });

  lfInstance.on("custom:node-task-edit", ({ nodeId }: any) => {
    jobDialog.value = true;
    jobNodeEditId.value = nodeId;
  });

  lfInstance.on("custom:node-prop", ({ nodeId }: any) => {
    const node = lfInstance.getNodeModelById(nodeId);
    let startTime = "";
    let endTime = "";

    if (runTime.value.length > 0) {
      const n = runTime.value.find((v) => v[0] == node.id) as any;
      startTime = n ? n[1] : "";
      endTime = n ? n[2] : "";
    }
    alert(`
      节点id：${node.id}
      节点类型：${node.type}
      任务状态：${node.isPause ? "暂停" : "正常"}
      X坐标：${node.x}
      Y坐标：${node.y}
      开始时间：${startTime}
      结束时间：${endTime}
    `);
  });

  lfInstance.on("custom:node-delete", ({ nodeId }: any) => {
    lfInstance.deleteNode(nodeId);
  });
}

/**
 * 设置LogicFlow容器的DOM引用
 * @param pageId 页面ID
 * @param el DOM元素
 */
function setLfRef(pageId: number, el: HTMLElement | null): void {
  if (el) {
    lfRefs.value[pageId] = el;
    // 只记录引用，不立即初始化
    // 选择当前页面时会通过selectPage调用initLogicFlowInstance
  } else if (lfRefs.value[pageId]) {
    delete lfRefs.value[pageId];
  }
}

LogicFlow.use(DndPanel); // 拖拽面板
LogicFlow.use(MiniMap);
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
        if (DYNAMIC_CUSTOM_GROUP == node.type) {
          ElMessage.warning("暂不支持任务组选择");
          return;
        }

        jobDialog.value = true;
        jobNodeEditId.value = node.id;
      },
    },
    {
      text: "编辑节点",
      callback(node: any) {
        if (node.properties.jobId === null || node.properties.jobId === undefined) {
          ElMessage.warning("请选择任务或任务组");
          return;
        }
        if (DYNAMIC_CUSTOM_GROUP == node.type) {
          ElMessage.warning("暂不支持任务组编辑");
          return;
        }
        jobNodeVisible.value = true;
        nodeJobId.value = node.properties.jobId;
        currentEditingNodeId.value = node.id; // 保存当前正在编辑的节点ID

        selectNode.value = node;
      },
    },
    {
      text: "复制",
      async callback(node: any) {
        if (DYNAMIC_CUSTOM_GROUP == node.type) {
          ElMessage.warning("暂不支持任务组复制");
          return;
        }

        // 获取当前LogicFlow实例
        const currentPageId = usePageStoreHook().getCurrentPage();
        const currentLf = lfInstances.value[currentPageId] || lf.value;

        if (!currentLf) {
          ElMessage.error("找不到对应的LogicFlow实例");
          return;
        }

        // 获取节点数据
        const nodeModel = currentLf.getNodeModelById(node.id);
        const originalNodeData = nodeModel.getData();

        const originalJobId = originalNodeData.properties?.jobId;
        if (!originalJobId) {
          ElMessage.warning("原节点没有关联的任务，无法复制");
          return;
        }

        try {
          // 获取原任务的完整数据
          const originalJobData = await JobInfoAPI.getFormData(originalJobId);

          // 创建新任务的数据，移除ID相关字段，添加_copy后缀
          const newJobData = {
            ...originalJobData,
            id: undefined, // 移除id，让后端自动生成新的
            jobDesc: (originalJobData.jobDesc || "") + "_copy",
            addTime: undefined,
            updateTime: undefined,
            triggerLastTime: undefined,
            triggerNextTime: undefined,
          };

          // 调用后端API创建新任务

          const newJobID = await JobInfoAPI.add(newJobData);

          // 检查后端返回的数据是否有效
          if (!newJobID) {
            console.error(`❌ 菜单复制节点 - 后端返回的数据为空`);
            ElMessage.error(
              "复制节点失败: 后端创建任务失败，未返回新任务数据。请检查后端API实现。"
            );
            return;
          }

          // 创建新的独立属性，使用新的jobId
          const newProperties = {
            ...JSON.parse(JSON.stringify(originalNodeData.properties || {})), // 深拷贝原属性
            jobId: newJobID, // 使用新任务的ID
            glueType: newJobData.glueType, // 确保glueType正确
          };

          // 创建新节点
          const newNode = {
            type: originalNodeData.type,
            x: originalNodeData.x + 50,
            y: originalNodeData.y + 50,
            text: newJobData.jobDesc,
            properties: newProperties,
          };

          console.log(
            `🆔 新节点独立jobId: ${newProperties.jobId}, glueType: ${newProperties.glueType}`
          );

          // 添加新节点
          currentLf.addNode(newNode);

          ElMessage.success("节点复制成功，已创建独立的后端任务");
        } catch (error) {
          console.error(`❌ 菜单复制节点失败:`, error);
          ElMessage.error("复制节点失败: " + (error.message || "未知错误"));
        }
      },
    },
    {
      text: "属性",
      callback(node: any) {
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

function getJobInfoList() {
  JobInfoAPI.getList().then((data: any) => {
    jobInfoList.value = data;
    selectJobInfoList.value = data.filter((e: any) => e.jobType === 0);
  });
}

function cancelDialog() {
  jobSelectId.value = undefined;
  jobDialog.value = false;
}

/**
 * 确认选择任务
 */
async function confirmDialog() {
  const _node = lf.value.getNodeModelById(jobNodeEditId.value);

  const _jobInfo = jobInfoList.value.find((e: any) => e.id === jobSelectId.value) as any;

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
    // 后台更新
    JobInfoAPI.updateJobNode(jobSelectId.value, _node.id).then((id) => {
      _node.setProperty("jobId", id);
      _node.updateText(_jobInfo.jobDesc);
      updateNodeTypeByGlueType(id, _jobInfo.jobDesc, _jobInfo.glueType);
    });
  }
  cancelDialog();
}

function changeJobRadio(val: string | number | boolean | undefined) {
  selectJobInfoList.value = jobInfoList.value.filter((e: any) =>
    val === 0 ? e.jobType === 0 : e.jobType !== 0
  );
}

/**
 * 根据glueType更新节点类型
 * 优先使用当前编辑的节点ID进行精确更新，确保只更新正在编辑的特定节点
 */
function updateNodeTypeByGlueType(
  jobId: number,
  jobDesc: string,
  glueType: string,
  lfInstance?: any
) {
  // 获取当前任务组对应的LogicFlow实例
  const currentPageId = usePageStoreHook().getCurrentPage();
  const currentLf = lfInstance || lfInstances.value[currentPageId] || lf.value;

  if (!currentLf) {
    console.error("找不到对应的LogicFlow实例");
    return;
  }

  const nodes = currentLf.getGraphRawData().nodes;
  const edges = currentLf.getGraphRawData().edges;

  let targetNode = null;

  // 只用 jobId 匹配
  targetNode = nodes.find((n: any) => n.properties.jobId == jobId);

  if (!targetNode) {
    console.error(`❌ 无法找到要更新的节点 - jobId: ${jobId}`);
    ElMessage.error("无法定位要更新的节点，请重新操作");
    return;
  }

  const nodeType =
    GLUE_NODE_TYPE_MAP[glueType] ||
    (targetNode.nodeType === DYNAMIC_CUSTOM_GROUP ? DYNAMIC_CUSTOM_GROUP : "rect");
  const graphModel = currentLf.graphModel;

  // 创建新的节点对象，并指定新的 type
  const newNode = {
    ...targetNode, // 保留原有节点的属性
  };
  newNode.type = nodeType;

  // 更新节点的properties，确保glueType也被更新
  newNode.properties = {
    ...newNode.properties,
    glueType: glueType,
  };

  // 保留原来的连线
  const relatedEdges = edges.filter(
    (e: any) => e.sourceNodeId === targetNode.id || e.targetNodeId === targetNode.id
  );

  // 删除原节点
  graphModel.deleteNode(targetNode.id);

  // 重新添加节点
  currentLf.addNode(newNode);

  // 恢复连线
  if (relatedEdges.length > 0) {
    relatedEdges.forEach((edge: any) => {
      currentLf.addEdge(edge);
    });
  }

  // 更新节点文本
  const newNodeModel = currentLf.getNodeModelById(newNode.id);
  if (newNodeModel) {
    newNodeModel.updateText(jobDesc);
    console.log(
      `✅ 节点精确更新完成 - 节点ID: ${newNode.id}, 类型: ${nodeType}, 文本: ${jobDesc}, glueType: ${glueType}`
    );
  }

  // 清除当前编辑节点ID，避免影响后续操作
  currentEditingNodeId.value = null;
}

function closeEditJobNode(
  jobId: number,
  jobDesc: string,
  glueType: string,
  type: number
) {
  if (type == 1) {
    updateNodeTypeByGlueType(jobId, jobDesc, glueType);
  }

  // 清理编辑状态
  jobNodeVisible.value = false;
  currentEditingNodeId.value = null;
  selectNode.value = null; // 清理选中的节点
  console.log(
    `📝 编辑对话框关闭 - jobId: ${jobId}, 操作类型: ${type === 1 ? "保存" : "取消"}`
  );
}

async function saveOrUpdateJob() {
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

  if (jobId.value) {
    await JobInfoAPI.getFormData(jobId.value).then((data) => {
      Object.assign(formData, data);
      formData.nodes = JSON.stringify(nodes);
      formData.edges = JSON.stringify(edges);
    });

    formData.glueType = "BEAN";
    formData.executorHandler = "runJobGroupXxlJob";

    await JobInfoAPI.updateJobCompose(jobId.value, formData)
      .then(() => {
        ElMessage.success("修改成功");
      })
      .finally(() => {});
    window.refreshTreeData();
  }
}

async function addJobNode(jobInfo: any) {
  jobInfo.executorRouteStrategy = "FIRST";
  await JobInfoAPI.saveJobNode(jobInfo).then((data: any) => {
    const nodeType = GLUE_NODE_TYPE_MAP[jobInfo.glueType];
    const node = {
      id: data.id,
      jobName: jobInfo.jobDesc,
      nodeType: nodeType,
      nodePositionX: data.nodePositionX,
      nodePositionY: data.nodePositionY,
      properties: data.properties,
      children: null,
    };
    lf.value.graphModel.addNode(generateNode(node));
  });
}

// ================== 12. 任务执行和操作处理 ==================

/**
 * 任务执行一次
 * 启动当前任务组的执行流程
 */
async function triggerOne(): Promise<void> {
  if (jobId.value == null) {
    ElMessage.warning("请选择任务组～");
    return;
  }

  // 获取当前任务组对应的LogicFlow实例
  const currentJobId = jobId.value;
  const currentLf = lfInstances.value[currentJobId] || lf.value;

  if (!currentLf) {
    ElMessage.error("找不到对应的LogicFlow实例");
    return;
  }

  // 获取或创建任务组状态
  const state = getJobState(currentJobId);

  // 在开始新任务前，先清理旧的连接和定时器

  // 清理旧的WebSocket连接
  if (state.ws) {
    state.ws.close();
    state.ws = null;
  }

  // 清理旧的日志定时器
  if (state.logRun) {
    window.clearInterval(state.logRun);
    state.logRun = null;
  }

  // 生成新的randomId
  randomId.value = snowflake.nextId(1) as string;

  const _nodes = currentLf.getGraphRawData().nodes;

  _nodes.forEach((node: any) => {
    const _node = currentLf.getNodeModelById(node.id);
    _node.setProperty("randomId", randomId.value);

    if (_node.type === DYNAMIC_CUSTOM_GROUP) {
      _node.setStyle("stroke", "#000");
    } else {
      _node.setStyle("fill", "#fff");
    }
    console.log(
      `🔄 重置节点 ${node.id} - jobId: ${node.properties?.jobId}, 新randomId: ${randomId.value}, 颜色: 白色`
    );
  });

  // TODO 查找暂停中的任务并修改任务状态
  const jobIds = _nodes.map((node: any) => node.properties.jobId);

  const pauseJobIds = await JobInfoAPI.pauseJobs(jobIds);

  _nodes
    .filter((node: any) => pauseJobIds.includes(node.properties.jobId))
    .forEach((node: any) => {
      const _node = currentLf.getNodeModelById(node.id);
      // 修改节点状态
      _node.isPause = true;
      _node.setStyle("fill", "#409EEE");
      // 触发节点重新渲染以更新图标
      _node.setAttributes();
    });

  // 保存当前任务组的randomId到状态中
  state.randomId = randomId.value;

  // 创建或更新日志标签页
  const currentPage = pageTaps.value.find((page) => page.id === jobId.value);
  // 只使用任务ID作为标签页的唯一标识，而不是任务ID加随机ID
  const tabId = `${jobId.value}`;

  // 检查是否已存在该任务组的标签页
  const existingTab = logTabs.value.find((tab) => tab.id === tabId);
  if (!existingTab) {
    // 如果不存在，创建新标签页

    logTabs.value.push({
      id: tabId,
      jobId: jobId.value,
      randomId: randomId.value,
      label: currentPage?.label || `任务组${jobId.value}`,
      isRunning: true,
    });
  } else {
    // 如果已存在，更新随机ID和运行状态
    existingTab.randomId = randomId.value;
    existingTab.isRunning = true;
  }

  // 重置该任务组的日志状态 - 使用任务组独立的状态
  state.fromLineNum = 0;
  state.pullFailCount = 0;

  // 重置该任务组对应的日志组件
  logReset(jobId.value);

  // 设置为当前激活的标签页
  activeLogTab.value = tabId;

  const jobInfoTriggerDto = {} as any;
  jobInfoTriggerDto.id = jobId.value;
  jobInfoTriggerDto.executorParam = randomId.value;

  JobInfoAPI.triggerJob(jobInfoTriggerDto)
    .then((data: any) => {
      if (data) {
        run(data);
        ElMessage.success("执行任务成功");
        // 为当前任务组创建独立的WebSocket连接
        connectWs(jobId.value + ":" + randomId.value, jobId.value);
        // 更新当前任务组的运行状态
        usePageStoreHook().updatePageRunStatus(jobId.value!, true);
        updateEdgeStyleForTaskGroupUtil(
          jobId.value,
          lfInstances.value[jobId.value],
          usePageStoreHook().getCurrentPageRunStatus
        );
      }
    })
    .catch((e: any) => {
      // 更新当前任务组的运行状态为停止
      usePageStoreHook().updatePageRunStatus(jobId.value!, false);
      ElMessage.error(e);
      // 获取当前激活标签页对应的日志组件
      const loggerRef = getLoggerRef(activeLogTab.value!);
      loggerRef?.addLogsFromText("读取任务日志失败...");

      // 任务启动失败时，更新日志标签页状态
      const tab = logTabs.value.find((t) => t.id === tabId);
      if (tab) {
        tab.isRunning = false;
      }
    })
    .finally(() => {});
}

/**
 * 停止任务
 * 停止当前任务组的执行
 */
function stopTrigger(): void {
  if (jobId.value == null) {
    ElMessage.warning("请选择任务组～");
    return;
  }

  const currentJobId = jobId.value;
  const state = getJobState(currentJobId);

  JobInfoAPI.stopJobCompose(currentJobId, state.randomId || randomId.value)
    .then(() => {
      // 更新当前任务组的运行状态
      usePageStoreHook().updatePageRunStatus(currentJobId, false);
      updateEdgeStyleForTaskGroupUtil(
        currentJobId,
        lfInstances.value[currentJobId],
        usePageStoreHook().getCurrentPageRunStatus
      );

      // 停止对应任务组的日志获取
      if (state.logRun) {
        window.clearInterval(state.logRun);
        state.logRun = null;
      }

      // 关闭对应任务组的WebSocket连接
      disconnectWs(currentJobId,state.randomId || randomId.value,);

      // 更新对应的日志标签页状态
      const tabId = `${currentJobId}`;
      const tab = logTabs.value.find((t) => t.id === tabId);
      if (tab) {
        tab.isRunning = false;
      }

      ElMessage.success("任务已停止");
    })
    .catch((error: any) => {
      console.error("停止任务失败:", error);
      ElMessage.error("停止任务失败");
    });
}

/**
 * 选择任务组
 * @param id 任务组ID
 */
async function selectJobCompNode(id: number): Promise<void> {
  // 只有在首次加载时调用，避免重复加载
  const currentLf = lfInstances.value[id];
  if (!currentLf) {
    console.error(`[流程5] 错误：找不到任务组 ${id} 的LogicFlow实例`);
    return;
  }

  // 检查是否已有数据
  const currentData = currentLf.getGraphRawData();
  console.log(
    `[流程5] 当前画布数据: 节点=${currentData.nodes.length}, 边=${currentData.edges.length}`
  );

  if (currentData.nodes.length > 0) {
    return;
  }

  // 原有的数据加载逻辑
  const graphModel = currentLf.graphModel;

  // 清空现有数据

  await clearData(currentLf);

  // 获取任务组数据

  const formMap = {
    id: id,
    type: 0,
    x: 0,
    y: 0,
  };
  const data = (await JobInfoAPI.getJobCompose(formMap)) as any;

  console.log(
    `[流程5] 获取到数据: 节点=${data.nodes?.length || 0}, 边=${data.edges?.length || 0}`
  );
  const newNodes = data.nodes;
  const newEdges = data.edges;

  // 添加节点和边

  addJobNodes(newNodes, graphModel, newEdges, currentLf);
}

/**
 * 增加节点
 * @param newNodes 新的节点
 * @param graphModel 画布模型
 * @param newEdges 新的边
 * @param lfInstance LogicFlow实例
 */
function addJobNodes(
  newNodes: any[],
  graphModel: any,
  newEdges: any[],
  lfInstance: any = null
): void {
  // 允许指定实例或使用当前活跃实例
  const instance = lfInstance || lf.value;
  if (!instance) return;

  // 添加节点
  newNodes.forEach((node: any) => {
    graphModel.addNode(generateNode(node));
  });

  // 设置节点样式和子节点
  newNodes.forEach((n: any) => {
    const node = instance.getNodeModelById(n.id);
    if (node) {
      node.isPause = n.isPause == 1;
      node.setStyle("fill", n.isPause == 1 ? "#409EEE" : "#fff");
      // 触发节点重新渲染以更新图标
      node.setAttributes();
      if (n.nodeType === DYNAMIC_CUSTOM_GROUP && n.children) {
        JSON.parse(n.children).forEach((id: string) => node.addChild(id));
      }
    }
  });

  // 添加边
  newEdges.forEach((e: any) => {
    graphModel.addEdge(generateEdge(e));
  });
}

// ================== 13. WebSocket连接管理 ==================

// 导入WebSocket管理工具
import { WebSocketMessageHandler } from "@/utils/websocketHandler";
import { webSocketPool } from "@/utils/websocket";

// 创建WebSocket消息处理器实例
const wsMessageHandler = new WebSocketMessageHandler(
  lfInstances.value,
  usePageStoreHook,
  logTabs,
  runTime,
  jobId
);

/**
 * 连接WebSocket
 * @param id 连接ID
 * @param targetJobId 目标任务组ID
 */
const connectWs = (id: string, targetJobId?: number): void => {
  try {
    // 使用WebSocket连接池管理连接
    const connection = webSocketPool.getConnection(id, targetJobId, {
      onMessage: (message: WebSocketMessage) => {
        // 使用消息处理器处理WebSocket消息
        wsMessageHandler.handleMessage(message);
      },
      onError: (event: Event) => {
        console.error("WebSocket连接错误:", event);
        ElMessage.error("WebSocket连接失败，请检查网络连接");
      },
      onReconnect: (attempt: number) => {
        console.log(`WebSocket重连尝试 ${attempt}`);
      },
      onReconnectFailed: () => {
        ElMessage.error("WebSocket重连失败，请刷新页面重试");
      },
    });

    // 连接WebSocket
    connection.connect();

    console.log(
      `WebSocket连接已建立: ${id}${targetJobId ? ` (任务组: ${targetJobId})` : ""}`
    );
  } catch (error) {
    console.error("WebSocket连接失败:", error);
    ElMessage.error("WebSocket连接失败");
  }
};

/**
 * 关闭WebSocket连接
 * @param id 连接ID
 * @param targetJobId 目标任务组ID
 */
const disconnectWs = (id: number, targetJobId?: string): void => {
  try {
    webSocketPool.closeConnection(id, targetJobId);
    console.log(
      `WebSocket连接已关闭: ${id}${targetJobId ? ` (任务组: ${targetJobId})` : ""}`
    );
  } catch (error) {
    console.error("关闭WebSocket连接失败:", error);
  }
};

/**
 * 获取WebSocket连接状态
 * @param id 连接ID
 * @param targetJobId 目标任务组ID
 * @returns 连接状态
 */
const getWsConnectionStatus = (id: string, targetJobId?: number): boolean => {
  return webSocketPool.hasConnection(id, targetJobId);
};

/**
 * 清理所有WebSocket连接
 */
const clearAllWsConnections = (): void => {
  webSocketPool.closeAllConnections();
  console.log("所有WebSocket连接已清理");
};

onMounted(() => {
  getJobInfoList();
  logger.value?.reset();

  // 确保DOM已渲染完成
  nextTick(() => {
    // 初始化一个空画布实例，不加载任务组数据

    // 如果已有初始任务组，则显示该任务组
    const currentPageId = usePageStoreHook().getCurrentPage();
    if (currentPageId) {
      // 使用selectPage来处理初始任务组的选择和数据加载
      selectPage(currentPageId);
    }
  });
});
// ================== 14. 组件生命周期 ==================

// 性能监控实例
const performanceMonitor = new PerformanceMonitor();

// 节流优化的鼠标移动处理
const throttledMouseMove = throttle((e: MouseEvent) => {
  if (!isDragging.value || !mainContainerRef.value) return;

  // 获取 main-container 的边界信息
  const containerRect = mainContainerRef.value.getBoundingClientRect();
  const containerHeight = containerRect.height;

  // 计算鼠标相对于容器顶部的位置
  const relativeY = e.clientY - containerRect.top;
  const newHeight = (relativeY / containerHeight) * 100;

  // 限制最小和最大高度
  if (
    newHeight >= PLATFORM_HEIGHT_LIMITS.MIN &&
    newHeight <= PLATFORM_HEIGHT_LIMITS.MAX
  ) {
    platformHeight.value = newHeight;
  }
}, 16); // 约60fps
</script>

<template>
  <div class="main-container" ref="mainContainerRef">
    <div class="platform" :style="{ height: `${platformHeight}vh` }">
      <div class="p-tap">
        <ul>
          <li
            v-for="(item, index) in pageTaps"
            :key="index"
            :class="{ active: item.id == jobId }"
          >
            <span @click="selectPage(item.id)">
              <el-icon class="tab-icon"><FolderOpened /></el-icon>
              {{ item.label }}
            </span>
            <el-icon @click="closePage(item.id)" class="close-icon">
              <Close />
            </el-icon>
          </li>
        </ul>
      </div>
      <template v-for="page in pageTaps" :key="page.id">
        <div
          :ref="(el) => setLfRef(page.id, el)"
          class="logic-flow"
          v-show="page.id === usePageStoreHook().getCurrentPage()"
          :style="{
            width: '100%',
            height: '100%',
          }"
        ></div>
      </template>
    </div>

    <EditJobNode
      :job-node-visible="jobNodeVisible"
      :node-job-id="nodeJobId"
      :now-date="new Date()"
      :node="selectNode"
      @close="closeEditJobNode"
    />

    <el-dialog v-model="jobDialog" style="width: 400px" title="选择任务" append-to-body>
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

    <!-- 可拖拽的分隔线 -->
    <div
      class="resize-handle"
      @mousedown="handleMouseDown"
      :class="{ dragging: isDragging }"
    ></div>

    <div class="log" :style="{ height: `${100 - platformHeight}vh` }">
      <div class="log-t">
        <span>日志监控</span>
        <el-icon>
          <Close />
        </el-icon>
      </div>
      <!-- 任务组标签页 -->
      <div class="log-tabs" v-if="logTabs.length > 0">
        <div
          v-for="tab in logTabs"
          :key="tab.id"
          class="log-tab-item"
          :class="{ active: activeLogTab === tab.id }"
          @click="switchLogTab(tab.id)"
        >
          <el-icon class="tab-icon"><Document /></el-icon>
          <span>{{ tab.label }}</span>
          <span v-if="tab.isRunning" class="running-indicator">
            <svg
              width="10"
              height="10"
              viewBox="0 0 10 10"
              style="vertical-align: middle"
            >
              <circle cx="5" cy="5" r="4" fill="#10b981" />
            </svg>
          </span>
          <el-icon class="close-tab-icon" @click.stop="closeLogTab(tab.id)">
            <Close />
          </el-icon>
        </div>
      </div>
      <!-- 日志内容区域 -->
      <div class="log-content">
        <div v-if="logTabs.length === 0" class="no-logs">
          <p>暂无运行日志</p>
          <p style="font-size: 0.75rem; margin-top: 0.5rem; opacity: 0.7">
            启动任务后将在此显示实时日志信息
          </p>
        </div>
        <div v-else class="log-panels">
          <div
            v-for="tab in logTabs"
            :key="tab.id"
            v-show="activeLogTab === tab.id"
            class="log-panel"
          >
            <Log
              :ref="
                (el) => {
                  if (el) loggerRefs[tab.id] = el;
                }
              "
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 全局变量 */
.main-container {
  --primary-color: #2563eb;
  --primary-hover: #1d4ed8;
  --primary-light: #e0e7ff;
  --secondary-color: #64748b;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
  --background-light: #f8fafc;
  --background-white: #ffffff;
  --border-light: #e5e7eb;
  --border-medium: #cbd5e1;
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-muted: #94a3b8;
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
  --radius-sm: 0.375rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;

  min-width: 85%;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--background-light);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue",
    Arial, sans-serif;
  overflow-x: hidden !important;
}

.platform {
  width: 100%;
  display: flex;
  flex-direction: column;
  background: var(--background-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  margin: 1rem;
  overflow: hidden;

  .p-tap {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    background: var(--background-white);
    border-bottom: 1px solid var(--border-light); /* 恢复分隔线 */
    height: 48px;
    flex-shrink: 0;
    padding: 0 1rem;
    position: relative;

    &::after {
      display: none !important; /* 移除伪元素下划线 */
    }

    ul {
      display: flex;
      list-style: none;
      margin: 0;
      padding: 0;
      gap: 0.25rem;
    }

    li {
      font-size: 0.875rem;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0.5rem 1rem;
      border-radius: var(--radius-md);
      height: 36px;
      cursor: pointer;
      transition: all 0.2s ease;
      position: relative;
      color: var(--text-secondary);
      background: transparent;
      border: 1px solid transparent;
      border-bottom: none !important;

      &:hover {
        background: #f3f6fd;
        color: var(--primary-color);
        transform: translateY(-1px);
      }

      &.active {
        background: var(--primary-light) !important;
        color: var(--primary-color) !important;
        border-color: transparent !important;
        box-shadow: var(--shadow-sm) !important;
        font-weight: 700 !important;
        /* 移除下划线 */
        &::after {
          display: none !important;
        }
      }

      span {
        margin-right: 0.5rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 120px;
        display: flex;
        align-items: center;
        gap: 0.5rem;

        .tab-icon {
          font-style: normal;
          font-size: 1rem;
          opacity: 0.8;
          transition: all 0.2s ease;
        }
      }

      .close-icon {
        font-size: 0.875rem;
        opacity: 0.6;
        transition: all 0.2s ease;
        padding: 0.25rem;
        border-radius: var(--radius-sm);

        &:hover {
          opacity: 1;
          background: rgba(239, 68, 68, 0.1);
          color: var(--danger-color);
          transform: scale(1.1);
        }
      }
    }
  }

  .logic-flow {
    width: 100% !important;
    height: 100% !important;
    min-height: 500px;
    position: relative;
    flex: 1;
    min-height: 0;
    background: var(--background-white);
  }
}

/* 可拖拽的分隔线 */
.resize-handle {
  width: 100%;
  height: 3px;
  background: #e2e2e2;
  cursor: row-resize;
  position: relative;
  transition: all 0.2s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;

  &::before {
    content: "";
    width: 40px;
    height: 4px;
    background: var(--border-medium);
    border-radius: 2px;
    transition: all 0.2s ease;
  }

  &:hover {
    background: linear-gradient(
      180deg,
      var(--primary-color) 0%,
      transparent 50%,
      var(--primary-color) 100%
    );

    &::before {
      background: var(--primary-color);
      width: 60px;
      height: 6px;
    }
  }

  &.dragging {
    background: linear-gradient(
      180deg,
      var(--primary-color) 0%,
      transparent 50%,
      var(--primary-color) 100%
    );

    &::before {
      background: var(--primary-color);
      width: 80px;
      height: 8px;
    }
  }
}

.log {
  background: var(--background-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  margin: 0 1rem 1rem 1rem;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .log-t {
    padding: 0 1.5rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
    height: 56px;
    background: var(--background-white);
    border-bottom: 1px solid var(--border-light) !important; /* 恢复分隔线 */
    position: relative;

    &::after {
      display: none !important;
    }

    span {
      font-weight: 600;
      font-size: 1rem;
      color: var(--text-primary);
      display: flex;
      align-items: center;
      gap: 0.5rem;

      &::before {
        content: "📋";
        font-size: 1.125rem;
      }
    }

    .el-icon {
      font-size: 1.125rem;
      color: var(--text-secondary);
      cursor: pointer;
      padding: 0.5rem;
      border-radius: var(--radius-sm);
      transition: all 0.2s ease;

      &:hover {
        background: var(--background-light);
        color: var(--danger-color);
        transform: scale(1.1);
      }
    }
  }

  .log-tabs {
    display: flex;
    background: var(--background-light);
    flex-shrink: 0;
    height: 48px;
    overflow-x: auto;
    border-bottom: 1px solid var(--border-light) !important; /* 恢复分隔线 */
    padding: 0.2rem 1rem;
    gap: 0.25rem;

    /* 自定义滚动条 */
    &::-webkit-scrollbar {
      height: 4px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--border-medium);
      border-radius: 2px;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: var(--secondary-color);
    }

    .log-tab-item {
      display: flex;
      align-items: center;
      padding: 0 1rem;
      cursor: pointer;
      white-space: nowrap;
      min-width: 140px;
      height: 40px;
      border-radius: var(--radius-md);
      transition: all 0.2s ease;
      position: relative;
      background: transparent;
      border: 1px solid transparent;
      color: var(--text-secondary);
      border-bottom: none !important;
      font-weight: 500;
      .tab-icon {
        margin-right: 0.5rem;
        font-size: 1rem;
        color: var(--primary-color);
      }
      &:hover {
        background: #f3f6fd;
        color: var(--primary-color);
      }
      &.active {
        background: var(--primary-light) !important;
        color: var(--primary-color) !important;
        border-color: transparent !important;
        box-shadow: var(--shadow-sm) !important;
        font-weight: 700 !important;
      }
      span {
        font-size: 0.875rem;
        font-weight: 500;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .running-indicator {
        margin-left: 0.5rem;
        display: flex;
        align-items: center;
      }
      .close-tab-icon {
        margin-left: 0.5rem;
        font-size: 0.875rem;
        opacity: 0.6;
        padding: 0.25rem;
        border-radius: var(--radius-sm);
        transition: all 0.2s ease;
        &:hover {
          opacity: 1;
          background: rgba(239, 68, 68, 0.1);
          color: var(--danger-color);
          transform: scale(1.1);
        }
      }
    }
  }

  .log-content {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    background: var(--background-white);

    .no-logs {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-muted);
      font-size: 0.875rem;
      background: var(--background-light);
      margin: 1rem;
      border-radius: var(--radius-md);
      border: 2px dashed var(--border-medium);
      flex-direction: column;
      padding: 2rem;
      text-align: center;

      p {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin: 0;
        font-weight: 500;

        &::before {
          content: "📝";
          font-size: 1.25rem;
        }

        &:last-child {
          font-size: 0.75rem;
          margin-top: 0.5rem;
          opacity: 0.7;
          font-weight: normal;
          color: var(--text-muted);

          &::before {
            content: "💡";
            font-size: 1rem;
          }
        }
      }
    }

    .log-panels {
      flex: 1;
      min-height: 0;
      position: relative;

      .log-panel {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        display: flex;
        flex-direction: column;
        padding: 1rem;
      }
    }
  }
}

/* 动画效果 */
@keyframes pulse {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.1);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-container {
    .platform {
      margin: 0.5rem;
      border-radius: var(--radius-md);

      .p-tap {
        height: 40px;
        padding: 0 0.5rem;

        li {
          padding: 0.25rem 0.5rem;
          height: 32px;
          font-size: 0.75rem;

          span {
            max-width: 80px;
          }
        }
      }
    }

    .log {
      margin: 0 0.5rem 0.5rem 0.5rem;
      border-radius: var(--radius-md);

      .log-t {
        height: 48px;
        padding: 0 1rem;

        span {
          font-size: 0.875rem;
        }
      }

      .log-tabs {
        height: 40px;
        padding: 0 0.5rem;

        .log-tab-item {
          min-width: 100px;
          height: 32px;
          padding: 0 0.5rem;

          span {
            font-size: 0.75rem;
          }
        }
      }
    }

    .resize-handle {
      height: 6px;

      &::before {
        width: 30px;
        height: 3px;
      }

      &:hover::before {
        width: 40px;
        height: 4px;
      }

      &.dragging::before {
        width: 50px;
        height: 5px;
      }
    }
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .main-container {
    --background-light: #1e293b;
    --background-white: #334155;
    --border-light: #475569;
    --border-medium: #64748b;
    --text-primary: #f1f5f9;
    --text-secondary: #cbd5e1;
    --text-muted: #94a3b8;
    --primary-light: #1e3a8a;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .main-container {
    --primary-color: #0000ff;
    --border-light: #000000;
    --border-medium: #000000;
    --text-primary: #000000;
    --text-secondary: #000000;
  }
}

/* 减少动画模式支持 */
@media (prefers-reduced-motion: reduce) {
  .main-container * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* Element Plus 组件样式优化 */
:deep(.el-dialog) {
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-lg) !important;
  border: none !important;
  overflow: hidden !important;

  .el-dialog__header {
    background: var(--background-light) !important;
    border-bottom: 1px solid var(--border-light) !important;
    padding: 1.5rem !important;
    margin: 0 !important;

    .el-dialog__title {
      font-weight: 600 !important;
      color: var(--text-primary) !important;
      font-size: 1.125rem !important;
    }

    .el-dialog__headerbtn {
      top: 1.5rem !important;
      right: 1.5rem !important;

      .el-dialog__close {
        font-size: 1.25rem !important;
        color: var(--text-secondary) !important;
        transition: all 0.2s ease !important;

        &:hover {
          color: var(--danger-color) !important;
          transform: scale(1.1) !important;
        }
      }
    }
  }

  .el-dialog__body {
    padding: 2rem !important;
    background: var(--background-white) !important;
  }

  .el-dialog__footer {
    background: var(--background-light) !important;
    border-top: 1px solid var(--border-light) !important;
    padding: 1.5rem !important;
    margin: 0 !important;

    .dialog-footer {
      display: flex !important;
      justify-content: flex-end !important;
      gap: 0.75rem !important;
    }
  }
}

:deep(.el-button) {
  border-radius: var(--radius-md) !important;
  font-weight: 500 !important;
  transition: all 0.2s ease !important;
  border: 1px solid transparent !important;

  &:hover {
    transform: translateY(-1px) !important;
    box-shadow: var(--shadow-sm) !important;
  }

  &.el-button--primary {
    background: var(--primary-color) !important;
    border-color: var(--primary-color) !important;

    &:hover {
      background: var(--primary-hover) !important;
      border-color: var(--primary-hover) !important;
    }
  }

  &.el-button--default {
    background: var(--background-white) !important;
    border-color: var(--border-medium) !important;
    color: var(--text-secondary) !important;

    &:hover {
      background: var(--background-light) !important;
      border-color: var(--primary-color) !important;
      color: var(--primary-color) !important;
    }
  }
}

:deep(.el-radio-group) {
  margin-bottom: 1.5rem;

  .el-radio {
    margin-right: 1.5rem;
    margin-bottom: 0.75rem;

    .el-radio__label {
      font-weight: 500;
      color: var(--text-primary);
    }

    .el-radio__input.is-checked .el-radio__inner {
      background: var(--primary-color);
      border-color: var(--primary-color);
    }
  }
}

:deep(.el-select) {
  width: 100%;

  .el-input__wrapper {
    border-radius: var(--radius-md);
    border: 1px solid var(--border-medium);
    transition: all 0.2s ease;

    &:hover {
      border-color: var(--primary-color);
    }

    &.is-focus {
      border-color: var(--primary-color);
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
    }
  }

  .el-input__inner {
    color: var(--text-primary);
    font-weight: 500;
  }
}

:deep(.el-option) {
  font-weight: 500;
  color: var(--text-primary);

  &:hover {
    background: var(--background-light);
  }

  &.selected {
    background: var(--primary-light);
    color: var(--primary-color);
  }
}

/* 滚动条美化 */
:deep(*) {
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: var(--background-light);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-medium);
    border-radius: 3px;
    transition: background 0.2s ease;

    &:hover {
      background: var(--secondary-color);
    }
  }
}

/* 加载状态优化 */
:deep(.el-loading-mask) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);

  .el-loading-spinner {
    .el-loading-text {
      color: var(--primary-color);
      font-weight: 500;
    }

    .path {
      stroke: var(--primary-color);
    }
  }
}

/* 消息提示优化 */
:deep(.el-message) {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: none;
  backdrop-filter: blur(8px);

  &.el-message--success {
    background: rgba(16, 185, 129, 0.1);
    border-left: 4px solid var(--success-color);
  }

  &.el-message--warning {
    background: rgba(245, 158, 11, 0.1);
    border-left: 4px solid var(--warning-color);
  }

  &.el-message--error {
    background: rgba(239, 68, 68, 0.1);
    border-left: 4px solid var(--danger-color);
  }

  &.el-message--info {
    background: rgba(37, 99, 235, 0.1);
    border-left: 4px solid var(--primary-color);
  }
}

/* 微交互效果 */
.main-container {
  .platform {
    .p-tap {
      li {
        &:hover {
          .tab-icon {
            transform: rotate(5deg);
          }
        }

        &.active {
          .tab-icon {
            animation: bounce 0.6s ease;
          }
        }
      }
    }
  }

  .log {
    .log-tabs {
      .log-tab-item {
        &:hover {
          .running-indicator {
            animation: pulse 1s infinite;
          }
        }
      }
    }
  }
}

@keyframes bounce {
  0%,
  20%,
  53%,
  80%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  40%,
  43% {
    transform: translate3d(0, -8px, 0);
  }
  70% {
    transform: translate3d(0, -4px, 0);
  }
  90% {
    transform: translate3d(0, -2px, 0);
  }
}

/* 工具提示样式 */
:deep(.el-tooltip__popper) {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: none;
  backdrop-filter: blur(8px);
  background: rgba(30, 41, 59, 0.95);
  color: white;
  font-weight: 500;
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
}

/* 焦点状态优化 */
:deep(.el-button:focus),
:deep(.el-select .el-input__wrapper.is-focus) {
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
}

/* 禁用状态优化 */
:deep(.el-button.is-disabled) {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

/* 加载状态优化 */
:deep(.el-loading-spinner) {
  .circular {
    width: 42px;
    height: 42px;
    animation: loading-rotate 2s linear infinite;
  }

  .path {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: 0;
    stroke-width: 2;
    stroke: var(--primary-color);
    stroke-linecap: round;
    animation: loading-dash 1.5s ease-in-out infinite;
  }
}

@keyframes loading-rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes loading-dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}
</style>
