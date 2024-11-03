<template>
  <div class="app-container">
    <div class="search-container">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
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
        <el-button
            v-hasPerm="['task:taskLog:add']"
            type="success"
            @click="handleOpenDialog()"
        >
          <template #icon><Plus /></template>
          新增
        </el-button>
        <el-button
            v-hasPerm="['task:taskLog:delete']"
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
                    label=""
                    prop="id"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="jobGroup"
                    label="执行器主键ID"
                    prop="jobGroup"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="jobId"
                    label="任务，主键ID"
                    prop="jobId"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="executorAddress"
                    label="执行器地址，本次执行的地址"
                    prop="executorAddress"
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
                    key="executorParam"
                    label="执行器任务参数"
                    prop="executorParam"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="executorShardingParam"
                    label="执行器任务分片参数，格式如 1/2"
                    prop="executorShardingParam"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="executorFailRetryCount"
                    label="失败重试次数"
                    prop="executorFailRetryCount"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="triggerTime"
                    label="调度-时间"
                    prop="triggerTime"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="triggerCode"
                    label="调度-结果"
                    prop="triggerCode"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="triggerMsg"
                    label="调度-日志"
                    prop="triggerMsg"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="handleTime"
                    label="执行-时间"
                    prop="handleTime"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="handleCode"
                    label="执行-状态"
                    prop="handleCode"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="handleMsg"
                    label="执行-日志"
                    prop="handleMsg"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="alarmStatus"
                    label="告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败"
                    prop="alarmStatus"
                    min-width="150"
                    align="center"
                />
        <el-table-column fixed="right" label="操作" width="220">
          <template #default="scope">
            <el-button
                v-hasPerm="['task:taskLog:edit']"
                type="primary"
                size="small"
                link
                @click="handleOpenDialog(scope.row.id)"
            >
              <template #icon><Edit /></template>
              编辑
            </el-button>
            <el-button
                v-hasPerm="['task:taskLog:delete']"
                type="danger"
                size="small"
                link
                @click="handleDelete(scope.row.id)"
            >
              <template #icon><Delete /></template>
              删除
            </el-button>
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

    <!-- task_log表单弹窗 -->
    <el-dialog
        v-model="dialog.visible"
        :title="dialog.title"
        width="500px"
        @close="handleCloseDialog"
    >
      <el-form ref="dataFormRef" :model="formData" :rules="rules" label-width="100px">
                <el-form-item label="" prop="id">
                      <el-input
                          v-model="formData.id"
                          placeholder=""
                      />
                </el-form-item>
                <el-form-item label="执行器主键ID" prop="jobGroup">
                      <el-input
                          v-model="formData.jobGroup"
                          placeholder="执行器主键ID"
                      />
                </el-form-item>
                <el-form-item label="任务，主键ID" prop="jobId">
                      <el-input
                          v-model="formData.jobId"
                          placeholder="任务，主键ID"
                      />
                </el-form-item>
                <el-form-item label="执行器地址，本次执行的地址" prop="executorAddress">
                      <el-input
                          v-model="formData.executorAddress"
                          placeholder="执行器地址，本次执行的地址"
                      />
                </el-form-item>
                <el-form-item label="执行器任务handler" prop="executorHandler">
                      <el-input
                          v-model="formData.executorHandler"
                          placeholder="执行器任务handler"
                      />
                </el-form-item>
                <el-form-item label="执行器任务参数" prop="executorParam">
                      <el-input
                          v-model="formData.executorParam"
                          placeholder="执行器任务参数"
                      />
                </el-form-item>
                <el-form-item label="执行器任务分片参数，格式如 1/2" prop="executorShardingParam">
                      <el-input
                          v-model="formData.executorShardingParam"
                          placeholder="执行器任务分片参数，格式如 1/2"
                      />
                </el-form-item>
                <el-form-item label="失败重试次数" prop="executorFailRetryCount">
                      <el-input
                          v-model="formData.executorFailRetryCount"
                          placeholder="失败重试次数"
                      />
                </el-form-item>
                <el-form-item label="调度-时间" prop="triggerTime">
                      <el-date-picker
                          v-model="formData.triggerTime"
                          type="datetime"
                          placeholder="调度-时间"
                          value-format="YYYY-MM-DD HH:mm:ss"
                      />
                </el-form-item>
                <el-form-item label="调度-结果" prop="triggerCode">
                      <el-input
                          v-model="formData.triggerCode"
                          placeholder="调度-结果"
                      />
                </el-form-item>
                <el-form-item label="调度-日志" prop="triggerMsg">
                      <el-input
                          v-model="formData.triggerMsg"
                          placeholder="调度-日志"
                      />
                </el-form-item>
                <el-form-item label="执行-时间" prop="handleTime">
                      <el-date-picker
                          v-model="formData.handleTime"
                          type="datetime"
                          placeholder="执行-时间"
                          value-format="YYYY-MM-DD HH:mm:ss"
                      />
                </el-form-item>
                <el-form-item label="执行-状态" prop="handleCode">
                      <el-input
                          v-model="formData.handleCode"
                          placeholder="执行-状态"
                      />
                </el-form-item>
                <el-form-item label="执行-日志" prop="handleMsg">
                      <el-input
                          v-model="formData.handleMsg"
                          placeholder="执行-日志"
                      />
                </el-form-item>
                <el-form-item label="告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败" prop="alarmStatus">
                      <el-input
                          v-model="formData.alarmStatus"
                          placeholder="告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败"
                      />
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
    name: "TaskLog",
    inheritAttrs: false,
  });

  import TaskLogAPI, { TaskLogPageVO, TaskLogForm, TaskLogPageQuery } from "@/api/task/task-log";

  const queryFormRef = ref(ElForm);
  const dataFormRef = ref(ElForm);

  const loading = ref(false);
  const removeIds = ref<number[]>([]);
  const total = ref(0);

  const queryParams = reactive<TaskLogPageQuery>({
    pageNum: 1,
    pageSize: 10,
  });

  // task_log表格数据
  const pageData = ref<TaskLogPageVO[]>([]);

  // 弹窗
  const dialog = reactive({
    title: "",
    visible: false,
  });

  // task_log表单数据
  const formData = reactive<TaskLogForm>({});

  // task_log表单校验规则
  const rules = reactive({
                      executorAddress: [{ required: true, message: "请输入执行器地址，本次执行的地址", trigger: "blur" }],
                      executorHandler: [{ required: true, message: "请输入执行器任务handler", trigger: "blur" }],
                      executorParam: [{ required: true, message: "请输入执行器任务参数", trigger: "blur" }],
                      executorShardingParam: [{ required: true, message: "请输入执行器任务分片参数，格式如 1/2", trigger: "blur" }],
                      triggerTime: [{ required: true, message: "请输入调度-时间", trigger: "blur" }],
                      triggerMsg: [{ required: true, message: "请输入调度-日志", trigger: "blur" }],
                      handleTime: [{ required: true, message: "请输入执行-时间", trigger: "blur" }],
                      handleMsg: [{ required: true, message: "请输入执行-日志", trigger: "blur" }],
  });

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
    dialog.visible = true;
    if (id) {
      dialog.title = "修改task_log";
            TaskLogAPI.getFormData(id).then((data) => {
        Object.assign(formData, data);
      });
    } else {
      dialog.title = "新增task_log";
    }
  }

  /** 提交task_log表单 */
  function handleSubmit() {
    dataFormRef.value.validate((valid: any) => {
      if (valid) {
        loading.value = true;
        const id = formData.id;
        if (id) {
                TaskLogAPI.update(id, formData)
              .then(() => {
                ElMessage.success("修改成功");
                handleCloseDialog();
                handleResetQuery();
              })
              .finally(() => (loading.value = false));
        } else {
                TaskLogAPI.add(formData)
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

  /** 关闭task_log弹窗 */
  function handleCloseDialog() {
    dialog.visible = false;
    dataFormRef.value.resetFields();
    dataFormRef.value.clearValidate();
    formData.id = undefined;
  }

  /** 删除task_log */
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
                TaskLogAPI.deleteByIds(ids)
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
