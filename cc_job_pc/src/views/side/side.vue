<script setup lang="ts">
import { Close, Search } from "@element-plus/icons-vue";
import { ref, computed, provide, watch, onMounted, reactive } from "vue";

import EnhancedTree from "@/components/EnhancedTree/EnhancedTree.vue";
import ContextMenu from "@/components/ContextMenu/ContextMenu.vue";
import JobPartAPI from "@/api/job-part.ts";
import { useNavbarStoreHook, usePageStoreHook } from "@/store";
import EditJobNode from "@/views/side/operation/edit-job-node.vue";
import { ElMessage, ElMessageBox } from "element-plus";
import EditJobGroup from "@/views/side/operation/edit-job-group.vue";
import JobInfoAPI from "@/api/job-info.ts";
import { useJobInfoStoreHook } from "@/store/modules/jobInfo";

const treeData = ref<any[]>([]);

const searchKeyword = ref("");
const showContextMenu = ref(false);
const contextMenuX = ref(0);
const contextMenuY = ref(0);
const selectedNode = ref<TreeNode | null>(null);
const jobNodeVisible = ref(false);
const node = ref<TreeNode | null>(null);
const nodeJobId = ref<number | null>(null);
const addJobGroupVisible = ref(false);
const jobPartName = ref("");
const jobPartId = ref<number | null>(null);
const jobGroupVisible = reactive({
  title: "新增任务组",
  visible: false,
});
const formData = reactive<any>({
  executorTimeout: 600000,
});

// 创建全局展开状态并提供给所有组件
const expandedNodes = ref({});
provide("expandedNodes", expandedNodes);

// 创建全局选中状态并提供给所有组件
const globalSelectedNodeId = ref(null);
provide("globalSelectedNodeId", globalSelectedNodeId);

const processedTreeData = computed(() => {
  const keyword = searchKeyword.value.toLowerCase();
  const clonedData = JSON.parse(JSON.stringify(treeData.value));

  // 当有搜索关键词时
  if (keyword) {
    const nodesToExpand = new Set();

    const filterAndMark = (nodes, parentPath = []) => {
      return nodes.filter((node) => {
        const path = [...parentPath, node.id];
        let hasMatch = false;

        if (node.children) {
          const children = filterAndMark(node.children, path);
          node.children = children;
          hasMatch = children.length > 0;
        }

        const isSelfMatch = node.label.toLowerCase().includes(keyword);
        if (isSelfMatch || hasMatch) {
          path.forEach((id) => nodesToExpand.add(id));
          return true;
        }
        return false;
      });
    };

    const filteredData = filterAndMark(clonedData);

    // 临时设置展开状态（不修改原始展开状态）
    const applyExpansion = (nodes) => {
      return nodes.map((node) => ({
        ...node,
        expanded: nodesToExpand.has(node.id),
        children: node.children ? applyExpansion(node.children) : null,
      }));
    };

    return applyExpansion(filteredData);
  }

  // 无搜索关键词时应用用户原始展开状态
  const applyUserExpansion = (nodes) => {
    return nodes.map((node) => ({
      ...node,
      expanded: expandedNodes.value[node.id] || false,
      children: node.children ? applyUserExpansion(node.children) : null,
    }));
  };

  return applyUserExpansion(clonedData);
});

// 添加类型定义
interface TreeNode {
  id: number;
  label: string;
  type: number;
  children?: TreeNode[];
  ext1?: string;
}

interface ContextMenuEvent {
  event: MouseEvent;
  node: TreeNode;
}

interface ContextMenuRef {
  menuElement: HTMLElement;
}

const contextMenuRef = ref<ContextMenuRef | null>(null);

watch(showContextMenu, (newVal) => {
  if (newVal) {
    // 添加事件监听使用捕获阶段
    document.addEventListener("click", handleGlobalClick, true);
    document.addEventListener("contextmenu", handleGlobalClick, true);
  } else {
    document.removeEventListener("click", handleGlobalClick, true);
    document.removeEventListener("contextmenu", handleGlobalClick, true);
  }
});

watch(
  () => useNavbarStoreHook().getAction(),
  (actionObj: any) => {
    if (actionObj.actionName == "add") {
      addJobGroupVisible.value = true;
    } else if (actionObj.actionName == "open") {
      console.log("open");
      // 创建隐藏的文件输入元素
      const fileInput = document.createElement("input");
      fileInput.type = "file";

      // 设置可选属性（多选/目录/文件类型）
      fileInput.multiple = true; // 允许多选
      // fileInput.webkitdirectory = true; // 允许选择目录（Chrome特有）
      fileInput.accept = ".cetl"; // 文件类型过滤

      // 添加变化事件监听
      fileInput.addEventListener("change", (event) => {
        const target = event.target as HTMLInputElement;
        const files = target.files;
        console.log("已选择文件:", files);
        // 这里添加文件处理逻辑
        if (files) {
          for (const file of files) {
            console.log(file);
            const formData = new FormData();
            formData.append("file", file);
            JobPartAPI.importData(formData).then(() => {
              ElMessage.success("导入数据成功");

              //刷新树形数据
              refreshTreeData();
            });
          }
        }
      });

      // 触发文件选择对话框
      fileInput.click();
    }
  },
  {
    immediate: true,
    deep: true,
  }
);

function handleContextMenu({ event, node }: ContextMenuEvent) {
  console.log(event.clientX, event.clientY);
  showContextMenu.value = true;
  contextMenuX.value = event.clientX;
  contextMenuY.value = event.clientY;
  selectedNode.value = node;
}

function handleAction(action: string, _node: TreeNode) {
  if (_node.type === 1 && action == "addJobInfo") {
    //新增节点
    jobNodeVisible.value = true;
    node.value = _node;
    // 重置nodeJobId为null，确保新增时不会显示之前编辑的数据
    nodeJobId.value = null;
  } else if (_node.type === 0 && action == "addJobGroup") {
    // 新增任务组
    jobGroupVisible.visible = true;
    formData.jobPartId = _node.id;
  } else if ([0, 1, 4, 5].includes(_node.type) && action == "edit") {
    if (_node.type === 0) {
      // edit jobPart
      addJobGroupVisible.value = true;
      jobPartName.value = _node.label;
      jobPartId.value = _node.id;
    } else if (_node.type === 1) {
      // edit jobGroup
      jobGroupVisible.title = "编辑任务组";
      jobGroupVisible.visible = true;
      JobInfoAPI.getFormData(_node.id).then((data) => {
        Object.assign(formData, data);
        // 设置nodes 为空
        formData.nodes = "";
        formData.edges = "";
      });
    } else {
      jobNodeVisible.value = true;
      nodeJobId.value = Number.parseInt(_node.ext1 || "0");
    }
  } else if (action == "delete") {
    deleteMessageNotice(_node.id, _node.type);
  } else if (action === "export") {
    JobPartAPI.exportData(_node.id).then((data) => {
      const fileName = getFileNameFromHeaders(data.headers) || "encryptedData.cetl";

      // 创建Blob并保存文件
      const blob = new Blob([data.data], { type: "application/octet-stream" });
      saveFile(blob, fileName);
    });
  } else if (action === "refresh") {
    refreshTreeData();
  } else if (action === "location") {
    //定位
    console.log("location", _node);
    usePageStoreHook().clearLoactionObject();
    //找到父亲节点
    const grandParentNode = findGrandParent(_node.id);
    if (grandParentNode) {
      console.log("祖父节点:", grandParentNode);
    }
    usePageStoreHook().addPage(grandParentNode);
    usePageStoreHook().setLoactionObject(_node);
  }
}

// 找到当前节点的父节点的父节点
const findGrandParent = (nodeId: number) => {
  const findParent = (nodes: any[], targetId: number, parent: any = null): any => {
    for (const node of nodes) {
      if (node.id === targetId) {
        return parent;
      }
      if (node.children) {
        const result = findParent(node.children, targetId, node);
        if (result) return result;
      }
    }
    return null;
  };

  const parent = findParent(treeData.value, nodeId);
  if (parent) {
    const grandParent = findParent(treeData.value, parent.id);
    return grandParent;
  }
  return null;
};

function getFileNameFromHeaders(headers: any) {
  const disposition = headers["content-disposition"];
  if (disposition) {
    const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
    const matches = filenameRegex.exec(disposition);
    if (matches != null && matches[1]) {
      return matches[1].replace(/['"]/g, "");
    }
  }
  return null;
}

function saveFile(blob: Blob, fileName: string) {
  // 创建临时下载链接
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", fileName);

  // 触发下载
  document.body.appendChild(link);
  link.click();

  ElMessage.success("导出成功");
  // 清理
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

function deleteMessageNotice(id: number, type: number) {
  const messageArr = [
    {
      type: 0,
      message: "确认是否删除当前分区并删除当前节点下的所有任务组",
    },
    {
      type: 1,
      message: "确认删除当前任务组下的所有节点和关系",
    },
    {
      type: 4,
      message: "确认删除当前节点",
    },
  ];
  ElMessageBox.confirm(messageArr.find((m) => m.type === type)?.message, "删除", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      if (type == 0) {
        JobPartAPI.deleteJobPart(id).then(() => {
          ElMessage({
            type: "success",
            message: "删除成功",
          });
          // 刷新任务树
          refreshTreeData();
        });
      } else if (type == 1) {
        JobInfoAPI.deleteByIds(id.toString()).then(() => {
          ElMessage({
            type: "success",
            message: "删除成功",
          });
          // 刷新任务树
          refreshTreeData();
        });
      } else if (type == 4 || type == 5) {
        // 得到当前页面
        useJobInfoStoreHook().setNodeToDelete(id);
      }
    })
    .catch(() => {
      ElMessage({
        type: "info",
        message: "取消删除",
      });
    });
}

function closeEditJobNode(id: any, jobDesc: any, glueType: any, type: any) {
  console.log(id, jobDesc, glueType, type);
  jobNodeVisible.value = false;
  // 关闭对话框时重置nodeJobId，确保下次打开时状态正确
  nodeJobId.value = null;
  node.value = null;
}

// 全局点击处理逻辑
const handleGlobalClick = (event: Event) => {
  if (
    contextMenuRef.value?.menuElement &&
    !contextMenuRef.value.menuElement.contains(event.target as Node)
  ) {
    showContextMenu.value = false;
  }
};

function saveJobPart() {
  const jobPart = {
    id: jobPartId.value,
    jobPartName: jobPartName.value,
    sort: 1,
  };
  if (!jobPartId.value) {
    JobPartAPI.saveJobPart(jobPart).then(() => {
      ElMessage.success("添加任务组成功");
      // 刷新任务树
      refreshTreeData();
    });
  } else {
    JobPartAPI.updateJobPart(jobPart).then(() => {
      ElMessage.success("修改任务组成功");
      // 刷新任务树
      refreshTreeData();
    });
  }
  addJobGroupVisible.value = false;
  jobPartName.value = "";
  jobPartId.value = null;
}

function handleCloseDialog() {
  const keys = Object.keys(formData);
  let obj: { [name: string]: string } = {};
  keys.forEach((item) => {
    obj[item] = "";
  });
  Object.assign(formData, obj);
  jobGroupVisible.visible = false;
}

// 添加布局内容区域的引用
const layoutContentRef = ref<HTMLElement | undefined>();

/**
 * 刷新任务树数据
 * 防抖处理，避免短时间内重复调用API
 */
let refreshTimer: any = null;
function refreshTreeData() {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
  }

  refreshTimer = setTimeout(() => {
    console.log("刷新任务树数据");
    JobPartAPI.getTree().then((data: any) => {
      treeData.value = data;
    });
  }, 300); // 300ms 防抖时间
}

// 让其他组件可以直接调用刷新树
if (typeof window !== "undefined") {
  window.refreshTreeData = refreshTreeData;
}

// 监听任务信息变更事件
watch(
  () => useJobInfoStoreHook().getTreeRefreshState(),
  (needRefresh) => {
    if (needRefresh) {
      refreshTreeData();
    }
  }
);

// 添加侧边栏隐藏功能
function hideSidebar() {
  useNavbarStoreHook().setSideVisible(false);
}

onMounted(() => {
  // 初始加载任务树数据
  refreshTreeData();
});
</script>

<template>
  <div class="side-container">
    <div class="side-tasks">
      <div class="t-title">
        <div class="side-t">
          <span>任务组</span>
          <el-icon @click="hideSidebar" style="cursor: pointer">
            <Close />
          </el-icon>
        </div>
        <div class="t-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索节点"
            :suffix-icon="Search"
            size="small"
          />
        </div>
      </div>
      <div class="t-list">
        <EnhancedTree
          :nodes="processedTreeData"
          :search-keyword="searchKeyword"
          @node-contextmenu="handleContextMenu"
        />

        <ContextMenu
          v-if="showContextMenu"
          ref="contextMenuRef"
          :x="contextMenuX"
          :y="contextMenuY"
          :node="selectedNode"
          @handle-action="handleAction"
          @close="showContextMenu = false"
        />
      </div>
    </div>
    <!--    <div class="side-layout">-->
    <!--      <div class="side-t">-->
    <!--        <span>布局</span>-->
    <!--        <el-icon>-->
    <!--          <Close />-->
    <!--        </el-icon>-->
    <!--      </div>-->
    <!--      <div class="layout-content" ref="layoutContentRef">-->
    <!--        &lt;!&ndash; miniMap 将被移动到这里 &ndash;&gt;-->
    <!--      </div>-->
    <!--    </div>-->
  </div>

  <EditJobNode
    :job-node-visible="jobNodeVisible"
    :node="node || undefined"
    :node-job-id="nodeJobId || undefined"
    :now-date="new Date()"
    @close="closeEditJobNode"
  />

  <EditJobGroup
    :job-group-visible="jobGroupVisible"
    :formData="formData"
    @close="handleCloseDialog"
  ></EditJobGroup>

  <el-dialog
    v-model="addJobGroupVisible"
    title="任务组名称"
    width="300"
    draggable
    append-to-body
  >
    <el-input v-model="jobPartName" placeholder="Please input" />
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="addJobGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="saveJobPart"> 确认 </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.side-container {
  min-width: 120px;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;

  .side-tasks {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 0 5px;

    .t-title {
      border-bottom: 1px solid #cccccc;
      flex-shrink: 0;

      .t-search {
        display: flex;
        margin: 10px 0;
        width: 90%;
      }
    }

    .t-list {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
    }
  }

  .side-layout {
    border-top: 1px solid #cccccc;
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    .layout-content {
      flex: 1;
      min-height: 200px;
      padding: 8px;
      background-color: #f9f9f9;
      overflow: hidden;
      position: relative; /* 添加相对定位 */
      display: block; /* 确保显示为块级元素 */
      // 重写 miniMap 的样式
      :deep(.lf-mini-map) {
        width: 100% !important;
        height: 150px !important;
        border: 1px solid #e43939 !important;
        border-radius: 4px !important;
        background: rgb(29, 21, 21) !important;
        box-shadow: none !important;

        .lf-mini-map-header {
          background: #f0f0f0 !important;
          border-bottom: 1px solid #ddd !important;
          padding: 4px 8px !important;
          font-size: 12px !important;
        }

        .lf-mini-map-graph {
          background: white !important;
        }
      }
    }
  }

  .side-t {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #cccccc;
    padding: 8px;
    flex-shrink: 0;

    span {
      font-weight: bolder;
    }
  }
}
</style>
