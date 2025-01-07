<template>
  <div class="app-container">
    <div class="search-container">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="执行器" prop="status">
          <el-select
            v-model="queryParams.jobGroup"
            placeholder="全部"
            clearable
            class="!w-[200px]"
          >
            <el-option
              :label="item.title"
              :value="item.id"
              v-for="item in taskGroupList"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="任务状态" prop="status">
          <el-select
            v-model="queryParams.logStatus"
            placeholder="全部"
            clearable
            class="!w-[100px]"
          >
            <el-option label="全部" :value="0" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="2" />
            <el-option label="进行中" :value="3" />
          </el-select>
        </el-form-item>

        <el-form-item label="调度时间">
          <el-date-picker
            v-model="queryParams.filterTime"
            type="datetimerange"
            :shortcuts="shortcuts"
            range-separator="To"
            start-placeholder="Start date"
            end-placeholder="End date"
            value-format="YYYY-MM-DDTHH:mm:ss.000Z"
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
          <el-button type="danger" @click="handleDelete">
            <template #icon><Delete /></template>
            清理
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="never" class="table-container">
      <el-table
        ref="dataTableRef"
        v-loading="loading"
        :data="pageData"
        highlight-current-row
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="index" width="120" label="序号" align="center" />
        <el-table-column
          key="jobId"
          label="任务"
          prop="jobId"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="triggerTime"
          label="调度时间"
          prop="triggerTime"
          min-width="200"
          align="center"
        >
          <template #default="{ row }">
            <span>{{ dayjs(row.triggerTime).format("YYYY-MM-DD HH:mm:ss") }}</span>
          </template>
        </el-table-column>
        <el-table-column
          key="triggerCode"
          label="调度结果"
          prop="triggerCode"
          min-width="150"
          align="center"
        >
          <template #default="{ row }">
            <el-tag type="success" v-if="row.triggerCode === 200">成功</el-tag>
            <el-tag type="danger" v-if="row.triggerCode === 500">失败</el-tag>
            <span v-else></span>
          </template>
        </el-table-column>

        <el-table-column label="调度备注" min-width="150" align="center">
          <template #default="{ row }">
            <el-link type="primary" @click="lookTriggerLog(row)">查看</el-link>
          </template>
        </el-table-column>

        <el-table-column
          key="handleTime"
          label="执行时间"
          prop="handleTime"
          min-width="200"
          align="center"
        >
          <template #default="{ row }">
            <span>{{ dayjs(row.handleTime).format("YYYY-MM-DD HH:mm:ss") }}</span>
          </template>
        </el-table-column>
        <el-table-column
          key="handleCode"
          label="执行结果"
          prop="handleCode"
          min-width="150"
          align="center"
        >
          <template #default="{ row }">
            <el-tag type="success" v-if="row.triggerCode === 200">成功</el-tag>
            <el-tag type="danger" v-if="row.triggerCode === 500">失败</el-tag>
            <span v-else></span>
          </template>
        </el-table-column>

        <el-table-column
          key="handleMsg"
          label="执行备注"
          prop="handleMsg"
          min-width="150"
          align="center"
        >
          <template #default="{ row }">
            <span v-if="row.handleMsg == null || row.handleMsg == ''">无</span>
            <el-link v-else type="primary" @click="lookHandleMsg(row.handleMsg)"
              >查看</el-link
            >
          </template>
        </el-table-column>

        <el-table-column fixed="right" label="操作" width="220">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleOpenDialog(scope.row.id)"
            >
              <template #icon><Edit /></template>
              执行日志
            </el-button>
          </template>
        </el-table-column>
        <el-table-column
          key="jobType"
          label="任务类型"
          prop="jobType"
          min-width="150"
          align="center"
        >
          <template #default="{ row }">
            <el-tag type="warning" effect="dark" v-if="row.jobType == 2">任务组</el-tag>
            <el-tag type="primary"  effect="dark" v-if="row.jobType == 0">任务</el-tag>
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

    <TaskTriggerLog
      :triggerLogVisable="triggerLogVisable"
      :triggerMsg="triggerMsg"
      @close="closeTriggerMsg"
    ></TaskTriggerLog>

    <TaskExecuteLog
      :taskLogId="taskLogId"
      :executeLogVisable="executeLogVisable"
      @close="closeExecuteLog"
    ></TaskExecuteLog>

    <el-dialog v-model="handleMsgVisable" title="调度备注" width="500">
      {{ handleMsg }}
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "TaskLog",
  inheritAttrs: false,
});

import TaskLogAPI, { TaskLogPageVO } from "@/api/task/task-log";
import TaskTriggerLog from "./operstion/task-trigger-log.vue";
import TaskExecuteLog from "./operstion/task-execute-log.vue";
import JobGroupAPI from "@/api/task/job-group";
import dayjs from "dayjs";
import { useRoute } from "vue-router";
const route = useRoute();

const queryFormRef = ref(ElForm);

const loading = ref(false);
const removeIds = ref<number[]>([]);
const total = ref(0);

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  logStatus: 0,
});

// task_log表格数据
const pageData = ref<TaskLogPageVO[]>([]);

const triggerLogVisable = ref(false);
const triggerMsg = ref("");
const taskGroupList = ref([]);
const taskLogId = ref();
const executeLogVisable = ref(false);
const handleMsgVisable = ref(false);
const handleMsg = ref("");

function lookTriggerLog(o: any) {
  triggerLogVisable.value = true;
  triggerMsg.value = o.triggerMsg;
}

function closeTriggerMsg() {
  triggerLogVisable.value = false;
  triggerMsg.value = "";
}

function closeExecuteLog() {
  taskLogId.value = undefined;
  executeLogVisable.value = false;
}

function lookHandleMsg(msg: string) {
  handleMsgVisable.value = true;
  handleMsg.value = msg;
}

const shortcuts = [
  {
    text: "上一个小时",
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setHours(start.getHours() - 1);
      return [start, end];
    },
  },
  {
    text: "上一天",
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setDate(start.getDate() - 1);
      return [start, end];
    },
  },
  {
    text: "上周",
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setDate(start.getDate() - 7);
      return [start, end];
    },
  },
  {
    text: "上个月",
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setMonth(start.getMonth() - 1);
      return [start, end];
    },
  },
];

/** 查询task_log */
function handleQuery() {
  loading.value = true;
  TaskLogAPI.getPage(queryParams)
    .then((data) => {
      pageData.value = data.list;
      total.value = data.total;
    })
    .finally(() => {
      loading.value = false;
    });
}

/** 重置task_log查询 */
function handleResetQuery() {
  queryFormRef.value!.resetFields();
  queryParams.pageNum = 1;
  handleQuery();
}

/** 行复选框选中记录选中ID集合 */
function handleSelectionChange(selection: any) {
  removeIds.value = selection.map((item: any) => item.id);
}

/** 打开task_log弹窗 */
function handleOpenDialog(id?: number) {
  taskLogId.value = id;
  executeLogVisable.value = true;
}

/** 删除task_log */
function handleDelete() {
  ElMessageBox.confirm("确认删除已选中的数据项?", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(
    () => {
      loading.value = true;
      TaskLogAPI.deleteTaskLogs(queryParams)
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

async function fetchTaskGroupList() {
  const data = await JobGroupAPI.getAllJobGroupList();
  taskGroupList.value = data as any;
}

onMounted(() => {
  if (route.query.id != null) {
    queryParams.jobId = route.query.id;
  }
  handleQuery();
  fetchTaskGroupList();
});
</script>
