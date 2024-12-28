<template>
  <el-form
    ref="formRef"
    :model="writerForm"
    label-width="auto"
    class="writer-form"
  >
    <el-form-item label="数据源" prop="datasource">
      <el-select
        v-model="writerForm.ds"
        filterable
        placeholder="选择数据源"
        style="width: 210px"
      >
        <el-option v-for="item in datasourceList" :label="item" :value="item" />
      </el-select>
    </el-form-item>
    <el-form-item label="数据库" prop="jdbcDatasourceId">
      <el-select
        v-model="writerForm.jdbcDatasourceId"
        filterable
        placeholder="选择数据库"
        style="width: 210px"
      >
        <el-option
          v-for="item in jdbcDatasourceList"
          :key="item.id"
          :label="item.databaseName"
          :value="item.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="数据表" v-if="writerForm.jdbcDatasourceId&&writerForm.ds!=='ORACLE'">
      <el-radio-group v-model="writerForm.tableName">
        <el-radio
          v-for="item in tableList"
          border
          :value="item"
          style="margin-top: 5px"
        >
          {{ item }}
        </el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="sql">
      <el-input
        v-model="writerForm.querySql"
        type="textarea"
        rows="6"
        autocomplete="off"
      />
    </el-form-item>
    <el-form-item label="解析">
      <el-button
        type="success"
        @click="getColumns(writerForm.jdbcDatasourceId)"
      >
        sql解析
      </el-button>
    </el-form-item>
    <el-form-item label="表字段">
      <el-checkbox
        v-model="checkAll"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      >
        全选
      </el-checkbox>
      <el-checkbox-group v-model="writerForm.columns">
        <el-checkbox
          v-for="item in columnList"
          border
          :label="item"
          :value="item"
          style="margin-top: 5px"
        />
      </el-checkbox-group>
    </el-form-item>

    <el-form-item label="写入模式">
      <el-radio-group v-model="writerForm.writeMode">
        <el-radio
          v-for="item in writeModeList"
          border
          :value="item"
          style="margin-top: 5px"
        >
          {{ item }}
        </el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="info" @click="pre">上一步</el-button>
      <el-button type="primary" @click="next">下一步</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import JobJdbcDatasourceAPI from "@/api/task/job-jdbc-datasource";
import JobDataXAPI from "@/api/task/job-datax";

const writerForm = ref({
  writeMode: "insert",
});
const jdbcDatasourceList = ref([]);
const tableList = ref([]);
const columnList = ref([]);
const checkAll = ref(false);
const isIndeterminate = ref(true);

const emit = defineEmits(["pre", "next"]);

const props = defineProps({
  preData: Object,
  preFormData: Object,
});

const datasourceList = ["MYSQL", "ORACLE"];

const writeModeList = ["insert", "update", "replace"];

watch(
  () => writerForm.value.ds,
  (val) => {
    console.log(val);
    fetchJdbcDatasource(val);
  }
);

watch(
  () => props.preData,
  async (data) => {
    if (data.jdbcDatasourceId) {
      writerForm.value = data;
      await getTables(data.jdbcDatasourceId);
      await getColumns(data.jdbcDatasourceId);
      await fetchJdbcDatasource(data.datasource.datasource);
    }
  },
  { immediate: true, deep: true }
);

watch(
  () => writerForm.value.jdbcDatasourceId,
  (val) => {
    console.log(val);
    getTables(val);
  }
);

watch(
  () => writerForm.value.tableName,
  (val) => {
    writerForm.value.querySql = "";
    getColumns(writerForm.value.jdbcDatasourceId);
  }
);


function handleCheckAllChange(val: boolean) {
  writerForm.value.columns = val ? columnList.value : [];
  isIndeterminate.value = false;
}

function next() {
  if (
    writerForm.value.columns == null ||
    writerForm.value.columns.length <= 0
  ) {
    ElMessage.warning("请选择要写入的数据列");
    return;
  }
  writerForm.value.datasource = jdbcDatasourceList.value.find(
    (v) => v.id === writerForm.value.jdbcDatasourceId
  );
  emit("next", writerForm.value, props.preFormData);
}

function pre() {
  emit("pre", writerForm.value, props.preFormData);
}

async function getTables(id: number) {
  await JobDataXAPI.getTables(id).then((data) => {
    tableList.value = data;
  });
}

async function getColumns(id: number) {
  const params = {} as any;
  if (writerForm.value.tableName != null) {
    params.tableName = writerForm.value.tableName;
  }
  if (writerForm.value.querySql != null) {
    params.querySql = writerForm.value.querySql;
  }
  await JobDataXAPI.getColumns(id, params).then((data) => {
    columnList.value = data;
    console.log(data);
  });
}

async function fetchJdbcDatasource(ds: string) {
  const data = await JobJdbcDatasourceAPI.getJdbcDatasourceList();
  jdbcDatasourceList.value = data.filter((v) => v.datasource == ds);
}
</script>

<style scoped lang="scss">
.writer-form {
  padding: 20px;
  background: #fff;
}
</style>
