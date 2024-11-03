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
        <el-table-column
          key="id"
          label="任务id"
          prop="id"
          min-width="150"
          align="center"
        />
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
                  <el-dropdown-item>执行一次</el-dropdown-item>
                  <el-dropdown-item>查询日志</el-dropdown-item>
                  <el-dropdown-item>注册节点</el-dropdown-item>
                  <el-dropdown-item disabled>下次执行时间</el-dropdown-item>
                  <el-dropdown-item divided>启动</el-dropdown-item>
                  <el-dropdown-item>编辑</el-dropdown-item>
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

    <!-- task_info表单弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.title"
      width="500px"
      @close="handleCloseDialog"
    >
      <el-form ref="dataFormRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="" prop="id">
          <el-input v-model="formData.id" placeholder="" />
        </el-form-item>
        <el-form-item label="执行器主键ID" prop="jobGroup">
          <el-input v-model="formData.jobGroup" placeholder="执行器主键ID" />
        </el-form-item>
        <el-form-item label="" prop="jobDesc">
          <el-input v-model="formData.jobDesc" placeholder="" />
        </el-form-item>
        <el-form-item label="" prop="addTime">
          <el-date-picker
            v-model="formData.addTime"
            type="datetime"
            placeholder=""
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="" prop="updateTime">
          <el-date-picker
            v-model="formData.updateTime"
            type="datetime"
            placeholder=""
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="formData.author" placeholder="作者" />
        </el-form-item>
        <el-form-item label="报警邮件" prop="alarmEmail">
          <el-input v-model="formData.alarmEmail" placeholder="报警邮件" />
        </el-form-item>
        <el-form-item label="调度类型" prop="scheduleType">
          <el-input v-model="formData.scheduleType" placeholder="调度类型" />
        </el-form-item>
        <el-form-item label="调度配置，值含义取决于调度类型" prop="scheduleConf">
          <el-input
            v-model="formData.scheduleConf"
            placeholder="调度配置，值含义取决于调度类型"
          />
        </el-form-item>
        <el-form-item label="调度过期策略" prop="misfireStrategy">
          <el-input v-model="formData.misfireStrategy" placeholder="调度过期策略" />
        </el-form-item>
        <el-form-item label="执行器路由策略" prop="executorRouteStrategy">
          <el-input
            v-model="formData.executorRouteStrategy"
            placeholder="执行器路由策略"
          />
        </el-form-item>
        <el-form-item label="执行器任务handler" prop="executorHandler">
          <el-input v-model="formData.executorHandler" placeholder="执行器任务handler" />
        </el-form-item>
        <el-form-item label="执行器任务参数" prop="executorParam">
          <el-input v-model="formData.executorParam" placeholder="执行器任务参数" />
        </el-form-item>
        <el-form-item label="阻塞处理策略" prop="executorBlockStrategy">
          <el-input v-model="formData.executorBlockStrategy" placeholder="阻塞处理策略" />
        </el-form-item>
        <el-form-item label="任务执行超时时间，单位秒" prop="executorTimeout">
          <el-input
            v-model="formData.executorTimeout"
            placeholder="任务执行超时时间，单位秒"
          />
        </el-form-item>
        <el-form-item label="失败重试次数" prop="executorFailRetryCount">
          <el-input
            v-model="formData.executorFailRetryCount"
            placeholder="失败重试次数"
          />
        </el-form-item>
        <el-form-item label="GLUE类型" prop="glueType">
          <el-input v-model="formData.glueType" placeholder="GLUE类型" />
        </el-form-item>
        <el-form-item label="GLUE源代码" prop="glueSource">
          <el-input v-model="formData.glueSource" placeholder="GLUE源代码" />
        </el-form-item>
        <el-form-item label="GLUE备注" prop="glueRemark">
          <el-input v-model="formData.glueRemark" placeholder="GLUE备注" />
        </el-form-item>
        <el-form-item label="GLUE更新时间" prop="glueUpdatetime">
          <el-date-picker
            v-model="formData.glueUpdatetime"
            type="datetime"
            placeholder="GLUE更新时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="子任务ID，多个逗号分隔" prop="childJobid">
          <el-input v-model="formData.childJobid" placeholder="子任务ID，多个逗号分隔" />
        </el-form-item>
        <el-form-item label="调度状态：0-停止，1-运行" prop="triggerStatus">
          <el-input
            v-model="formData.triggerStatus"
            placeholder="调度状态：0-停止，1-运行"
          />
        </el-form-item>
        <el-form-item label="上次调度时间" prop="triggerLastTime">
          <el-input v-model="formData.triggerLastTime" placeholder="上次调度时间" />
        </el-form-item>
        <el-form-item label="下次调度时间" prop="triggerNextTime">
          <el-input v-model="formData.triggerNextTime" placeholder="下次调度时间" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleSubmit()">确定</el-button>
          <el-button @click="handleCloseDialog()">取消</el-button>
        </div>
      </template>
    </el-dialog>
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

const queryFormRef = ref(ElForm);
const dataFormRef = ref(ElForm);

const loading = ref(false);
const removeIds = ref<number[]>([]);
const total = ref(0);

const queryParams = reactive<TaskInfoPageQuery>({
  pageNum: 1,
  pageSize: 10,
});

// task_info表格数据
const pageData = ref<TaskInfoPageVO[]>([]);

// 弹窗
const dialog = reactive({
  title: "",
  visible: false,
});

// task_info表单数据
const formData = reactive<TaskInfoForm>({});

// task_info表单校验规则
const rules = reactive({
  addTime: [{ required: true, message: "请输入", trigger: "blur" }],
  updateTime: [{ required: true, message: "请输入", trigger: "blur" }],
  author: [{ required: true, message: "请输入作者", trigger: "blur" }],
  alarmEmail: [{ required: true, message: "请输入报警邮件", trigger: "blur" }],
  scheduleConf: [
    { required: true, message: "请输入调度配置，值含义取决于调度类型", trigger: "blur" },
  ],
  executorRouteStrategy: [
    { required: true, message: "请输入执行器路由策略", trigger: "blur" },
  ],
  executorHandler: [
    { required: true, message: "请输入执行器任务handler", trigger: "blur" },
  ],
  executorParam: [{ required: true, message: "请输入执行器任务参数", trigger: "blur" }],
  executorBlockStrategy: [
    { required: true, message: "请输入阻塞处理策略", trigger: "blur" },
  ],
  glueSource: [{ required: true, message: "请输入GLUE源代码", trigger: "blur" }],
  glueRemark: [{ required: true, message: "请输入GLUE备注", trigger: "blur" }],
  glueUpdatetime: [{ required: true, message: "请输入GLUE更新时间", trigger: "blur" }],
  childJobid: [
    { required: true, message: "请输入子任务ID，多个逗号分隔", trigger: "blur" },
  ],
});

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
  dialog.visible = true;
  if (id) {
    dialog.title = "修改task_info";
    TaskInfoAPI.getFormData(id).then((data) => {
      Object.assign(formData, data);
    });
  } else {
    dialog.title = "新增task_info";
  }
}

/** 提交task_info表单 */
function handleSubmit() {
  dataFormRef.value.validate((valid: any) => {
    if (valid) {
      loading.value = true;
      const id = formData.id;
      if (id) {
        TaskInfoAPI.update(id, formData)
          .then(() => {
            ElMessage.success("修改成功");
            handleCloseDialog();
            handleResetQuery();
          })
          .finally(() => (loading.value = false));
      } else {
        TaskInfoAPI.add(formData)
          .then(() => {
            ElMessage.success("新增成功");
            handleCloseDialog();
            handleResetQuery();
          })
          .finally(() => (loading.value = false));
      }
    }
  });
}

/** 关闭task_info弹窗 */
function handleCloseDialog() {
  dialog.visible = false;
  dataFormRef.value.resetFields();
  dataFormRef.value.clearValidate();
  formData.id = undefined;
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
