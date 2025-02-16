<template>
  <div class="app-container">
    <div class="search-container">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="数据源名称" prop="datasourceName">
          <el-input
            v-model="queryParams.datasourceName"
            placeholder="请输入数据源名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="数据源" prop="datasource">
          <el-select
            v-model="queryParams.datasource"
            placeholder="全部"
            clearable
            class="!w-[100px]"
          >
            <el-option
              v-for="item in datasourceList"
              :label="item.type"
              :value="item.type"
              :key="item.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据库名" prop="databaseName">
          <el-input
            v-model="queryParams.databaseName"
            placeholder="请输入数据库名"
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
          key="datasourceName"
          label="数据源名称"
          prop="datasourceName"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="datasource"
          label="数据源"
          prop="datasource"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="databaseName"
          label="数据库名"
          prop="databaseName"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="jdbcUsername"
          label="用户名"
          prop="jdbcUsername"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="jdbcUrl"
          label="jdbc url"
          prop="jdbcUrl"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="createTime"
          label="创建时间"
          prop="createTime"
          min-width="150"
          align="center"
        />
        <el-table-column
          key="updateTime"
          label="更新时间"
          prop="updateTime"
          min-width="150"
          align="center"
        />
        <el-table-column fixed="right" label="操作" width="220">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleOpenDialog(scope.row.id)"
            >
              <template #icon><Edit /></template>
              编辑
            </el-button>
            <el-button
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

    <!-- jdbc数据源配置表单弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.title"
      width="500px"
      @close="handleCloseDialog"
    >
      <el-form
        ref="dataFormRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="数据源名称" prop="datasourceName">
          <el-input
            v-model="formData.datasourceName"
            placeholder="数据源名称"
          />
        </el-form-item>
        <el-form-item label="数据源" prop="datasource">
          <el-select
            v-model="formData.datasource"
            filterable
            placeholder="数据源"
            style="width: 210px"
          >
            <el-option
              v-for="item in datasourceList"
              :key="item.type"
              :label="item.title"
              :value="item.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据库名" prop="databaseName">
          <el-input v-model="formData.databaseName" placeholder="数据库名" />
        </el-form-item>
        <el-form-item label="SchemaName" prop="schemaName">
          <el-input v-model="formData.schemaName" placeholder="SchemaName" />
        </el-form-item>
        <el-form-item label="用户名" prop="jdbcUsername">
          <el-input v-model="formData.jdbcUsername" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="jdbcPassword">
          <el-input
            type="password"
            v-model="formData.jdbcPassword"
            placeholder="密码"
          />
        </el-form-item>
        <el-form-item label="address" prop="address">
          <el-input v-model="formData.ip" placeholder="address" />
        </el-form-item>
        <el-form-item label="port" prop="port">
          <el-input v-model="formData.port" placeholder="port" />
        </el-form-item>
        <el-form-item label="备注" prop="comments">
          <el-input
            v-model="formData.comments"
            type="textarea"
            placeholder="备注"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="isConnect">测试连接</el-button>
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
  name: "JobJdbcDatasource",
  inheritAttrs: false,
});

import JobJdbcDatasourceAPI, {
  JobJdbcDatasourcePageVO,
  JobJdbcDatasourceForm,
  JobJdbcDatasourcePageQuery,
} from "@/api/task/job-jdbc-datasource";

const queryFormRef = ref(ElForm);
const dataFormRef = ref(ElForm);

const loading = ref(false);
const removeIds = ref<number[]>([]);
const total = ref(0);

const queryParams = reactive<JobJdbcDatasourcePageQuery>({
  pageNum: 1,
  pageSize: 10,
});

// jdbc数据源配置表格数据
const pageData = ref<JobJdbcDatasourcePageVO[]>([]);

// 弹窗
const dialog = reactive({
  title: "",
  visible: false,
});

const datasourceList = [
  {
    type: "MYSQL",
    title: "MYSQL",
  },
  { type: "ORACLE", title: "ORACLE" },
];

// jdbc数据源配置表单数据
const formData = reactive<JobJdbcDatasourceForm>({});

// jdbc数据源配置表单校验规则
const rules = reactive({
  databaseName: [
    { required: true, message: "请输入数据库名", trigger: "blur" },
  ],
  jdbcUsername: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  jdbcPassword: [{ required: true, message: "请输入密码", trigger: "blur" }],
  comments: [{ required: true, message: "请输入备注", trigger: "blur" }],
});

/** 查询jdbc数据源配置 */
function handleQuery() {
  loading.value = true;
  JobJdbcDatasourceAPI.getPage(queryParams)
    .then((data) => {
      pageData.value = data.list;
      total.value = data.total;
    })
    .finally(() => {
      loading.value = false;
    });
}

/** 重置jdbc数据源配置查询 */
function handleResetQuery() {
  queryFormRef.value!.resetFields();
  queryParams.pageNum = 1;
  handleQuery();
}

/** 行复选框选中记录选中ID集合 */
function handleSelectionChange(selection: any) {
  removeIds.value = selection.map((item: any) => item.id);
}

/** 打开jdbc数据源配置弹窗 */
function handleOpenDialog(id?: number) {
  dialog.visible = true;
  if (id) {
    dialog.title = "修改jdbc数据源配置";
    JobJdbcDatasourceAPI.getFormData(id).then((data) => {
      Object.assign(formData, data);
      const match = data.jdbcUrl.match(/\/\/([^:]+):([^\/]+)/);
      if (match) {
        formData.ip = match[1]; // IP地址
        formData.port = match[2]; // 端口号
      }
    });
  } else {
    dialog.title = "新增jdbc数据源配置";
  }
}

function getDriver(datasource?: string) {
  let jdbcDriverClass = "";
  if (datasource == "MYSQL") {
    jdbcDriverClass = "com.mysql.cj.jdbc.Driver";
  } else if (datasource == "ORACLE") {
    jdbcDriverClass = "oracle.jdbc.OracleDriver";
  }
  return jdbcDriverClass;
}

function getJdbcUrl(datasource?: string, ip?: string, port?: string) {
  let jdbcUrl = "";
  if (datasource == "MYSQL") {
    port = port == null || port.trim() == "" ? 3306 : port;
    jdbcUrl = `jdbc:mysql://${ip}:${port}/${formData.databaseName}`;
  } else if (datasource == "ORACLE") {
    port = port == null || port.trim() == "" ? 1521 : port;
    jdbcUrl = `jdbc:oracle:thin:@//${ip}:${port}/${formData.databaseName}`;
  }
  return jdbcUrl;
}

async function isConnect() {
  let _isConnect = false;
  formData.jdbcDriverClass = getDriver(formData.datasource);
  formData.jdbcUrl = getJdbcUrl(
    formData.datasource,
    formData.ip,
    formData.port
  );
  await JobJdbcDatasourceAPI.isConnect(formData).then((data: any) => {
    if (data) {
      ElMessage.success("数据库连接成功");
    }
    _isConnect = data;
  });
  return _isConnect;
}

/** 提交jdbc数据源配置表单 */
function handleSubmit() {
  dataFormRef.value.validate(async (valid: any) => {
    if (valid) {
      loading.value = true;
      const id = formData.id;

      const _isConnect = await isConnect();
      if (!_isConnect) {
        ElMessage.error("数据库连接失败");
        return;
      }

      if (id) {
        await JobJdbcDatasourceAPI.update(id, formData)
          .then(() => {
            ElMessage.success("修改成功");
            handleCloseDialog();
            handleResetQuery();
          })
          .finally(() => (loading.value = false));
      } else {
        await JobJdbcDatasourceAPI.add(formData)
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

/** 关闭jdbc数据源配置弹窗 */
function handleCloseDialog() {
  dialog.visible = false;
  dataFormRef.value.resetFields();
  dataFormRef.value.clearValidate();
  formData.id = undefined;
  formData.ip = "";
  formData.port = "";
}

/** 删除jdbc数据源配置 */
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
      JobJdbcDatasourceAPI.deleteByIds(ids)
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
