<template>
  <el-form
    ref="formRef"
    :model="writerForm"
    label-width="auto"
    class="writer-form"
  >
    <el-form-item label="数据源" prop="jdbcDatasourceId">
      <el-select
        v-model="writerForm.jdbcDatasourceId"
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
    <el-form-item label="数据表" v-if="writerForm.jdbcDatasourceId">
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
        v-model="writerForm.sql"
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
    <el-form-item label="表字段" v-if="writerForm.tableName">
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
    <el-form-item>
      <el-button type="info" @click="pre">上一步</el-button>
      <el-button type="primary" @click="next">下一步</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import JobJdbcDatasourceAPI from "@/api/task/job-jdbc-datasource";
import JobDataXAPI from "@/api/task/job-datax";

const writerForm = ref({});
const jdbcDatasourceList = ref([]);
const tableList = ref([]);
const columnList = ref([]);

const emit = defineEmits(["pre", "next"]);

const props = defineProps({
  preData: Object,
});

watch(
  () => props.preData,
  async (data) => {
    if (data.jdbcDatasourceId) {
      writerForm.value = data;
      await getTables(data.jdbcDatasourceId);
      await getColumns(data.jdbcDatasourceId);
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
    console.log(val);
    getColumns(writerForm.value.jdbcDatasourceId);
  }
);

function next() {
  emit("next", writerForm.value);
}

function pre() {
  emit("pre", writerForm.value);
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
  if (writerForm.value.sql != null) {
    params.sql = writerForm.value.sql;
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

<style scoped lang="scss">
.writer-form {
  padding: 20px;
  background: #fff;
}
</style>
