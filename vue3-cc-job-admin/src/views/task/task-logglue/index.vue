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
            v-hasPerm="['task:taskLogglue:add']"
            type="success"
            @click="handleOpenDialog()"
        >
          <template #icon><Plus /></template>
          新增
        </el-button>
        <el-button
            v-hasPerm="['task:taskLogglue:delete']"
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
                    key="jobId"
                    label="任务，主键ID"
                    prop="jobId"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="glueType"
                    label="GLUE类型"
                    prop="glueType"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="glueSource"
                    label="GLUE源代码"
                    prop="glueSource"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="glueRemark"
                    label="GLUE备注"
                    prop="glueRemark"
                    min-width="150"
                    align="center"
                />
                <el-table-column
                    key="addTime"
                    label=""
                    prop="addTime"
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
                v-hasPerm="['task:taskLogglue:edit']"
                type="primary"
                size="small"
                link
                @click="handleOpenDialog(scope.row.id)"
            >
              <template #icon><Edit /></template>
              编辑
            </el-button>
            <el-button
                v-hasPerm="['task:taskLogglue:delete']"
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

    <!-- task_logglue表单弹窗 -->
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
                <el-form-item label="任务，主键ID" prop="jobId">
                      <el-input
                          v-model="formData.jobId"
                          placeholder="任务，主键ID"
                      />
                </el-form-item>
                <el-form-item label="GLUE类型" prop="glueType">
                      <el-input
                          v-model="formData.glueType"
                          placeholder="GLUE类型"
                      />
                </el-form-item>
                <el-form-item label="GLUE源代码" prop="glueSource">
                      <el-input
                          v-model="formData.glueSource"
                          placeholder="GLUE源代码"
                      />
                </el-form-item>
                <el-form-item label="GLUE备注" prop="glueRemark">
                      <el-input
                          v-model="formData.glueRemark"
                          placeholder="GLUE备注"
                      />
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
    name: "TaskLogglue",
    inheritAttrs: false,
  });

  import TaskLogglueAPI, { TaskLoggluePageVO, TaskLogglueForm, TaskLoggluePageQuery } from "@/api/task/task-logglue";

  const queryFormRef = ref(ElForm);
  const dataFormRef = ref(ElForm);

  const loading = ref(false);
  const removeIds = ref<number[]>([]);
  const total = ref(0);

  const queryParams = reactive<TaskLoggluePageQuery>({
    pageNum: 1,
    pageSize: 10,
  });

  // task_logglue表格数据
  const pageData = ref<TaskLoggluePageVO[]>([]);

  // 弹窗
  const dialog = reactive({
    title: "",
    visible: false,
  });

  // task_logglue表单数据
  const formData = reactive<TaskLogglueForm>({});

  // task_logglue表单校验规则
  const rules = reactive({
                      glueType: [{ required: true, message: "请输入GLUE类型", trigger: "blur" }],
                      glueSource: [{ required: true, message: "请输入GLUE源代码", trigger: "blur" }],
                      addTime: [{ required: true, message: "请输入", trigger: "blur" }],
                      updateTime: [{ required: true, message: "请输入", trigger: "blur" }],
  });

  /** 查询task_logglue */
  function handleQuery() {
    loading.value = true;
          TaskLogglueAPI.getPage(queryParams)
        .then((data) => {
          pageData.value = data.list;
          total.value = data.total;
        })
        .finally(() => {
          loading.value = false;
        });
  }

  /** 重置task_logglue查询 */
  function handleResetQuery() {
    queryFormRef.value!.resetFields();
    queryParams.pageNum = 1;
    handleQuery();
  }

  /** 行复选框选中记录选中ID集合 */
  function handleSelectionChange(selection: any) {
    removeIds.value = selection.map((item: any) => item.id);
  }

  /** 打开task_logglue弹窗 */
  function handleOpenDialog(id?: number) {
    dialog.visible = true;
    if (id) {
      dialog.title = "修改task_logglue";
            TaskLogglueAPI.getFormData(id).then((data) => {
        Object.assign(formData, data);
      });
    } else {
      dialog.title = "新增task_logglue";
    }
  }

  /** 提交task_logglue表单 */
  function handleSubmit() {
    dataFormRef.value.validate((valid: any) => {
      if (valid) {
        loading.value = true;
        const id = formData.id;
        if (id) {
                TaskLogglueAPI.update(id, formData)
              .then(() => {
                ElMessage.success("修改成功");
                handleCloseDialog();
                handleResetQuery();
              })
              .finally(() => (loading.value = false));
        } else {
                TaskLogglueAPI.add(formData)
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

  /** 关闭task_logglue弹窗 */
  function handleCloseDialog() {
    dialog.visible = false;
    dataFormRef.value.resetFields();
    dataFormRef.value.clearValidate();
    formData.id = undefined;
  }

  /** 删除task_logglue */
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
                TaskLogglueAPI.deleteByIds(ids)
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
