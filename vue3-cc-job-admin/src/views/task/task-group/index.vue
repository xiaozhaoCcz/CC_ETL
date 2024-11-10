<template>
  <div class="app-container">
    <div class="search-container">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="AppName" prop="appName">
          <el-input
            v-model="queryParams.appName"
            placeholder="请输入AppName"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="执行器名称" prop="title">
          <el-input
            v-model="queryParams.title"
            placeholder="请输入执行器名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <template #icon>
              <Search />
            </template>
            搜索
          </el-button>
          <el-button @click="handleResetQuery">
            <template #icon>
              <Refresh />
            </template>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="never" class="table-container">
      <template #header>
        <el-button type="success" @click="handleOpenDialog()">
          <template #icon>
            <Plus />
          </template>
          新增
        </el-button>
        <el-button
          type="danger"
          :disabled="removeIds.length === 0"
          @click="handleDelete()"
        >
          <template #icon>
            <Delete />
          </template>
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
        <el-table-column type="index" width="120" label="序号" align="center" />
        <el-table-column
          key="appName"
          label="执行器AppName"
          prop="appName"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="title"
          label="执行器名称"
          prop="title"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="addressType"
          label="执行器类型"
          prop="addressType"
          min-width="150"
          align="center"
        >
          <template #default="{ row }">
            {{ row.addressType === 0 ? "自动注册" : "手动录入" }}
          </template>
        </el-table-column>
        <el-table-column
          key="addressList"
          label="OnLine机器地址"
          prop="addressList"
          min-width="150"
          align="center"
        >
          <template #default="{ row }">
            <span v-if="row.addressList === null || row.addressList === ''">
              无
            </span>
            <span v-else>
              <el-link type="primary" @click="findAddressList(row.id)">
                查看
              </el-link>
            </span>
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
              <template #icon>
                <Edit />
              </template>
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              link
              @click="handleDelete(scope.row.id)"
            >
              <template #icon>
                <Delete />
              </template>
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

    <el-dialog
      v-model="addressVisible"
      title="注册节点"
      width="300px"
      @close="handleCloseAddress"
    >
      <ul>
        <li v-for="item in addressList">
          {{ item }}
        </li>
      </ul>
    </el-dialog>

    <EditTaskGroup
      :taskGroupVisible="taskGroupVisible"
      :formData="formData"
      @close="handleCloseDialog"
      @handleResetQuery="handleResetQuery"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "TaskGroup",
  inheritAttrs: false
});

import TaskGroupAPI, {
  TaskGroupPageVO,
  TaskGroupForm,
  TaskGroupPageQuery
} from "@/api/task/task-group";
import EditTaskGroup from "./operation/edit-task-group.vue";

const queryFormRef = ref(ElForm);

const loading = ref(false);
const removeIds = ref<number[]>([]);
const total = ref(0);
const addressList = ref([]);
const addressVisible = ref(false);

const queryParams = reactive<TaskGroupPageQuery>({
  pageNum: 1,
  pageSize: 10
});

// task_group表格数据
const pageData = ref<TaskGroupPageVO[]>([]);

// 弹窗
const taskGroupVisible = reactive({
  title: "",
  visible: false
});

// task_group表单数据
const formData = reactive<TaskGroupForm>({
  addressType: 0
});

function findAddressList(id: number) {
  console.log(id);
  addressVisible.value = true;
  TaskGroupAPI.findAddressList(id).then((data: any) => {
    addressList.value = data;
  });
}

const handleCloseAddress = () => {
  addressVisible.value = false;
  addressList.value = [];
};

/** 打开task_group弹窗 */
function handleOpenDialog(id?: number) {
  taskGroupVisible.visible = true;
  if (id) {
    taskGroupVisible.title = "修改taskGroup";
    TaskGroupAPI.getFormData(id).then((data) => {
      Object.assign(formData, data);
    });
  } else {
    taskGroupVisible.title = "新增taskGroup";
  }
}

/** 查询task_group */
function handleQuery() {
  loading.value = true;
  TaskGroupAPI.getPage(queryParams)
    .then((data) => {
      pageData.value = data.list;
      total.value = data.total;
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleCloseDialog() {
  formData.id = undefined;
  taskGroupVisible.visible = false;
}

/** 重置task_group查询 */
function handleResetQuery() {
  queryFormRef.value!.resetFields();
  queryParams.pageNum = 1;
  handleQuery();
}

/** 行复选框选中记录选中ID集合 */
function handleSelectionChange(selection: any) {
  removeIds.value = selection.map((item: any) => item.id);
}

/** 删除task_group */
function handleDelete(id?: number) {
  const ids = [id || removeIds.value].join(",");
  if (!ids) {
    ElMessage.warning("请勾选删除项");
    return;
  }

  ElMessageBox.confirm("确认删除已选中的数据项?", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(
    () => {
      loading.value = true;
      TaskGroupAPI.deleteByIds(ids)
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
