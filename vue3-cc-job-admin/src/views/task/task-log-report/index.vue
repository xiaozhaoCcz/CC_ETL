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
            v-hasPerm="['task:taskLogReport:add']"
            type="success"
            @click="handleOpenDialog()"
        >
          <template #icon><Plus /></template>
          新增
        </el-button>
        <el-button
            v-hasPerm="['task:taskLogReport:delete']"
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
                    key="triggerDay"
                    label="调度-时间"
                    prop="triggerDay"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="runningCount"
                    label="运行中-日志数量"
                    prop="runningCount"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="sucCount"
                    label="执行成功-日志数量"
                    prop="sucCount"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="failCount"
                    label="执行失败-日志数量"
                    prop="failCount"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="updateTime"
                    label=""
                    prop="updateTime"
                    min-width="150"
                    align="center"
                />
        <el-table-column fixed="right" label="操作" width="220">
          <template #default="scope">
            <el-button
                v-hasPerm="['task:taskLogReport:edit']"
                type="primary"
                size="small"
                link
                @click="handleOpenDialog(scope.row.id)"
            >
              <template #icon><Edit /></template>
              编辑
            </el-button>
            <el-button
                v-hasPerm="['task:taskLogReport:delete']"
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

    <!-- task_log_report表单弹窗 -->
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
                <el-form-item label="调度-时间" prop="triggerDay">
                      <el-date-picker
                          v-model="formData.triggerDay"
                          type="datetime"
                          placeholder="调度-时间"
                          value-format="YYYY-MM-DD HH:mm:ss"
                      />
                </el-form-item>
                <el-form-item label="运行中-日志数量" prop="runningCount">
                      <el-input
                          v-model="formData.runningCount"
                          placeholder="运行中-日志数量"
                      />
                </el-form-item>
                <el-form-item label="执行成功-日志数量" prop="sucCount">
                      <el-input
                          v-model="formData.sucCount"
                          placeholder="执行成功-日志数量"
                      />
                </el-form-item>
                <el-form-item label="执行失败-日志数量" prop="failCount">
                      <el-input
                          v-model="formData.failCount"
                          placeholder="执行失败-日志数量"
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
    name: "TaskLogReport",
    inheritAttrs: false,
  });

  import TaskLogReportAPI, { TaskLogReportPageVO, TaskLogReportForm, TaskLogReportPageQuery } from "@/api/task/task-log-report";

  const queryFormRef = ref(ElForm);
  const dataFormRef = ref(ElForm);

  const loading = ref(false);
  const removeIds = ref<number[]>([]);
  const total = ref(0);

  const queryParams = reactive<TaskLogReportPageQuery>({
    pageNum: 1,
    pageSize: 10,
  });

  // task_log_report表格数据
  const pageData = ref<TaskLogReportPageVO[]>([]);

  // 弹窗
  const dialog = reactive({
    title: "",
    visible: false,
  });

  // task_log_report表单数据
  const formData = reactive<TaskLogReportForm>({});

  // task_log_report表单校验规则
  const rules = reactive({
                      triggerDay: [{ required: true, message: "请输入调度-时间", trigger: "blur" }],
                      updateTime: [{ required: true, message: "请输入", trigger: "blur" }],
  });

  /** 查询task_log_report */
  function handleQuery() {
    loading.value = true;
          TaskLogReportAPI.getPage(queryParams)
        .then((data) => {
          pageData.value = data.list;
          total.value = data.total;
        })
        .finally(() => {
          loading.value = false;
        });
  }

  /** 重置task_log_report查询 */
  function handleResetQuery() {
    queryFormRef.value!.resetFields();
    queryParams.pageNum = 1;
    handleQuery();
  }

  /** 行复选框选中记录选中ID集合 */
  function handleSelectionChange(selection: any) {
    removeIds.value = selection.map((item: any) => item.id);
  }

  /** 打开task_log_report弹窗 */
  function handleOpenDialog(id?: number) {
    dialog.visible = true;
    if (id) {
      dialog.title = "修改task_log_report";
            TaskLogReportAPI.getFormData(id).then((data) => {
        Object.assign(formData, data);
      });
    } else {
      dialog.title = "新增task_log_report";
    }
  }

  /** 提交task_log_report表单 */
  function handleSubmit() {
    dataFormRef.value.validate((valid: any) => {
      if (valid) {
        loading.value = true;
        const id = formData.id;
        if (id) {
                TaskLogReportAPI.update(id, formData)
              .then(() => {
                ElMessage.success("修改成功");
                handleCloseDialog();
                handleResetQuery();
              })
              .finally(() => (loading.value = false));
        } else {
                TaskLogReportAPI.add(formData)
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

  /** 关闭task_log_report弹窗 */
  function handleCloseDialog() {
    dialog.visible = false;
    dataFormRef.value.resetFields();
    dataFormRef.value.clearValidate();
    formData.id = undefined;
  }

  /** 删除task_log_report */
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
                TaskLogReportAPI.deleteByIds(ids)
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
