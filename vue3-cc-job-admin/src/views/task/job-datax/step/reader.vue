<template>
  <el-form
    ref="formRef"
    :model="readerForm"
    label-width="auto"
    class="reader-form"
  >
    <el-form-item label="数据源" prop="jdbcDatasourceId">
      <el-select
        v-model="readerForm.jdbcDatasourceId"
        filterable
        placeholder="Select"
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
    <el-form-item label="数据表" v-if="readerForm.jdbcDatasourceId">
      <el-radio-group v-model="readerForm.tableName">
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
        v-model="readerForm.sql"
        type="textarea"
        rows="6"
        autocomplete="off"
      />
    </el-form-item>
    <el-form-item label="解析">
      <el-button
        type="success"
        @click="getColumns(readerForm.jdbcDatasourceId)"
      >
        sql解析
      </el-button>
    </el-form-item>
    <el-form-item label="表字段" v-if="readerForm.tableName">
      <el-checkbox-group v-model="readerForm.columns">
        <el-checkbox
          v-for="item in columnList"
          border
          :label="item"
          :value="item"
          style="margin-top: 5px"
        />
      </el-checkbox-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="next">下一步</el-button>
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import JobJdbcDatasourceAPI from "@/api/task/job-jdbc-datasource";
import JobDataXAPI from "@/api/task/job-datax";

const readerForm = ref({});
const jdbcDatasourceList = ref([]);
const tableList = ref([]);
const columnList = ref([]);

const emit = defineEmits(["next"]);

const props = defineProps({
  preData: Object,
});

watch(
  () => props.preData,
  async (data) => {
    if (data.jdbcDatasourceId) {
      console.log("data", data);
      readerForm.value = data;
      await getTables(data.jdbcDatasourceId);
      await getColumns(data.jdbcDatasourceId);
    }
  },
  { immediate: true, deep: true }
);

watch(
  () => readerForm.value.jdbcDatasourceId,
  (val) => {
    console.log(val);
    getTables(val);
  }
);

watch(
  () => readerForm.value.tableName,
  (val) => {
    console.log(val);
    getColumns(readerForm.value.jdbcDatasourceId);
  }
);

function next() {
  emit("next", readerForm.value);
}

async function getTables(id: number) {
  await JobDataXAPI.getTables(id).then((data) => {
    console.log("11122233");
    tableList.value = data;
  });
}

async function getColumns(id: number) {
  const params = {} as any;
  if (readerForm.value.tableName != null) {
    params.tableName = readerForm.value.tableName;
  }
  if (readerForm.value.sql != null) {
    params.sql = readerForm.value.sql;
  }
  await JobDataXAPI.getColumns(id, params).then((data) => {
    columnList.value = data;
  });
}

async function fetchJdbcDatasource() {
  const data = await JobJdbcDatasourceAPI.getJdbcDatasourceList();
  jdbcDatasourceList.value = data as any;
}

onMounted(() => {
  fetchJdbcDatasource();
});
</script>
<style lang="scss" scoped>
.reader-form {
  padding: 20px;
  background: #fff;
}
</style>
