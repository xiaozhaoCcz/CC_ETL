<template>
  <div class="app-container">
    <div class="search-container">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="执行器" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="全部"
            clearable
            class="!w-[200px]"
          >
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="全部"
            clearable
            class="!w-[100px]"
          >
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务描述" prop="keywords">
          <el-input
            v-model="queryParams.keywords"
            placeholder="请输入任务描述"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="JobHandler" prop="keywords">
          <el-input
            v-model="queryParams.keywords"
            placeholder="请输入JobHandler"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="责任人" prop="keywords" style="width: 200px">
          <el-input
            v-model="queryParams.keywords"
            placeholder="请输入责任人"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <template #icon><Search /></template>
            搜索
          </el-button>
          <el-button @click="handleResetQuery">
            <template #icon><Refresh /></template>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="never" class="table-container">
      <template #header>
        <!-- v-hasPerm="['task:taskInfo:add']" -->
        <el-button type="success" @click="handleOpenDialog()">
          <template #icon><Plus /></template>
          新增
        </el-button>
        <el-button
          type="danger"
          :disabled="removeIds.length === 0"
          @click="handleDelete()"
        >
          <template #icon><Delete /></template>
          删除
        </el-button>
      </template>

      <el-table
        ref="dataTableRef"
        v-loading="loading"
        :data="pageData"
        highlight-current-row
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column type="index" width="50" label="序号" />
        <el-table-column
          key="jobDesc"
          label="任务描述"
          prop="jobDesc"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="scheduleType"
          label="调度类型"
          prop="scheduleType"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="scheduleConf"
          label="调度配置，值含义取决于调度类型"
          prop="scheduleConf"
          min-width="150"
          align="center"
        />

        <el-table-column
          key="glueType"
          label="运行模式"
          prop="glueType"
          min-width="150"
          align="center"
        />

        <el-table-column
          key="executorHandler"
          label="执行器任务handler"
          prop="executorHandler"
          min-width="150"
          align="center"
        />

        <el-table-column
          key="author"
          label="负责人"
          prop="author"
          min-width="150"
          align="center"
        />

        <el-table-column
          key="triggerStatus"
          label="状态"
          prop="triggerStatus"
          min-width="150"
          align="center"
        >
          <template #default="scope">
            <el-tag type="info" v-if="scope.row.triggerStatus === 0">停止</el-tag>
            <el-tag type="success" v-if="scope.row.triggerStatus === 1">运行</el-tag>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="220">
          <template #default="scope">
            <el-dropdown>
              <el-button type="primary">
                操作<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="executeOne(scope.row.id)"
                    >执行一次</el-dropdown-item
                  >
                  <el-dropdown-item>查询日志</el-dropdown-item>
                  <el-dropdown-item>注册节点</el-dropdown-item>
                  <el-dropdown-item disabled>下次执行时间</el-dropdown-item>
                  <el-dropdown-item
                    divided
                    @click="startTask(scope.row.id)"
                    v-if="scope.row.triggerStatus === 0"
                    >启动</el-dropdown-item
                  >
                  <el-dropdown-item divided @click="stopTask(scope.row.id)" v-else
                    >停止</el-dropdown-item
                  >

                  <el-dropdown-item @click="handleOpenDialog(scope.row.id)"
                    >编辑</el-dropdown-item
                  >
                  <el-dropdown-item>删除</el-dropdown-item>
                  <el-dropdown-item>复制</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="handleQuery()"
      />
    </el-card>

    <ExecuteOne
      :executeOneVal="executeOneVal"
      :taskId="taskId"
      @close="closeExecuteOne"
    ></ExecuteOne>

    <EditTaskInfo
      :taskInfoVisible="taskInfoVisible"
      :formData="formData"
      @close="handleCloseDialog"
      @handleResetQuery="handleResetQuery"
    ></EditTaskInfo>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "TaskInfo",
  inheritAttrs: false,
});

import TaskInfoAPI, {
  TaskInfoPageVO,
  TaskInfoForm,
  TaskInfoPageQuery,
} from "@/api/task/task-info";
import ExecuteOne from "./operation/executeone.vue";
import EditTaskInfo from "./operation/edit-task-info.vue";

const queryFormRef = ref(ElForm);

const loading = ref(false);
const removeIds = ref<number[]>([]);
const total = ref(0);

const queryParams = reactive<TaskInfoPageQuery>({
  pageNum: 1,
  pageSize: 10,
});

// task_info表格数据
const pageData = ref<TaskInfoPageVO[]>([]);

const taskInfoVisible = reactive({
  title: "",
  visible: false,
});

// task_info表单数据
const formData = reactive<TaskInfoForm>({});
const executeOneVal = ref(false);
const taskId = ref();

function executeOne(id?: number) {
  executeOneVal.value = true;
  taskId.value = id;
}

function closeExecuteOne() {
  taskId.value = undefined;
  executeOneVal.value = false;
}

function startTask(id: number) {
  const taskObj = pageData.value.filter((v) => v.id == id)[0];
  TaskInfoAPI.startTask(id).then(() => {
    taskObj.triggerStatus = 1;
  });
}

function stopTask(id: number) {
  const taskObj = pageData.value.filter((v) => v.id == id)[0];
  TaskInfoAPI.stopTask(id).then(() => {
    taskObj.triggerStatus = 0;
  });
}

/** 查询task_info */
function handleQuery() {
  loading.value = true;
  TaskInfoAPI.getPage(queryParams)
    .then((data) => {
      pageData.value = data.list;
      total.value = data.total;
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleCloseDialog() {
  const keys = Object.keys(formData);
  let obj: { [name: string]: string } = {};
  keys.forEach((item) => {
    obj[item] = "";
  });
  Object.assign(formData, obj);
  taskInfoVisible.visible = false;
}

/** 重置task_info查询 */
function handleResetQuery() {
  queryFormRef.value!.resetFields();
  queryParams.pageNum = 1;
  handleQuery();
}

/** 行复选框选中记录选中ID集合 */
function handleSelectionChange(selection: any) {
  removeIds.value = selection.map((item: any) => item.id);
}

/** 打开task_info弹窗 */
function handleOpenDialog(id?: number) {
  taskInfoVisible.visible = true;
  if (id) {
    taskInfoVisible.title = "修改taskInfo";
    TaskInfoAPI.getFormData(id).then((data) => {
      Object.assign(formData, data);
    });
  } else {
    taskInfoVisible.title = "新增taskInfo";
  }
}

/** 删除task_info */
function handleDelete(id?: number) {
  const ids = [id || removeIds.value].join(",");
  if (!ids) {
    ElMessage.warning("请勾选删除项");
    return;
  }

  ElMessageBox.confirm("确认删除已选中的数据项?", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(
    () => {
      loading.value = true;
      TaskInfoAPI.deleteByIds(ids)
        .then(() => {
          ElMessage.success("删除成功");
          handleResetQuery();
        })
        .finally(() => (loading.value = false));
    },
    () => {
      ElMessage.info("已取消删除");
    }
  );
}

onMounted(() => {
  handleQuery();
});
</script>
<style lang="scss" scoped>
// :deep(.example-showcase .el-dropdown-link) {
//   cursor: pointer;
//   color: var(--el-color-primary);
//   display: flex;
//   align-items: center;
// }
</style>
