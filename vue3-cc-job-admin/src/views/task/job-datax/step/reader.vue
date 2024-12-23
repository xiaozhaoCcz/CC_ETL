<template>
  <el-form
    ref="formRef"
    :model="readerForm"
    label-width="auto"
    class="reader-form"
  >
    <el-form-item label="数据源" prop="datasource">
      <el-select
        v-model="readerForm.ds"
        filterable
        placeholder="选择数据源"
        style="width: 210px"
      >
        <el-option v-for="item in datasourceList" :label="item" :value="item" />
      </el-select>
    </el-form-item>
    <el-form-item label="数据库" prop="jdbcDatasourceId">
      <el-select
        v-model="readerForm.jdbcDatasourceId"
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
        v-model="readerForm.querySql"
        type="textarea"
        :rows="6"
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
    <el-form-item label="表字段" v-if="columnList.length > 0">
      <el-checkbox
        v-model="checkAll"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      >
        全选
      </el-checkbox>
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
    <el-form-item label="增量备份">
      <el-radio-group v-model="readerForm.incrType">
        <el-radio :value="0" style="margin-top: 5px">全量</el-radio>
        <el-radio :value="1" style="margin-top: 5px">增量</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="自增序列" v-if="readerForm.incrType == 1">
      <el-select
        v-model="readerForm.incrColumnType"
        filterable
        style="width: 210px"
      >
        <el-option label="主键自增" :value="0" />
        <el-option label="时间自增" :value="1" />
      </el-select>
    </el-form-item>
    <el-form-item
      label="时间数据列"
      v-if="readerForm.incrType == 1 && readerForm.incrColumnType == 1"
    >
      <el-input
        v-model="readerForm.incrColumnName"
        style="width: 240px"
        placeholder="Please input"
      />
    </el-form-item>
    <el-form-item label="默认参数" v-if="readerForm.incrType == 1">
      <el-input
        v-model="readerForm.incrParam"
        style="width: 240px"
        placeholder="Please input"
      />
    </el-form-item>
    <el-form-item label="默认自增数据" v-if="readerForm.incrType == 1">
      <el-input
        v-model="readerForm.incrId"
        style="width: 240px"
        placeholder="Please input"
        v-if="readerForm.incrColumnType == 0"
      />
      <el-date-picker
        v-else
        v-model="readerForm.incrTime"
        type="datetime"
        placeholder="Select date and time"
        value-format="x"
      />
    </el-form-item>
    <el-form-item
      label="时间格式"
      v-if="readerForm.incrType == 1 && readerForm.incrColumnType == 1"
    >
      <el-select
        v-model="readerForm.timeFormat"
        filterable
        style="width: 210px"
      >
        <el-option label="YYYY/MM/DD hh:mm:ss" value="yyyy/MM/dd hh:mm:ss" />
        <el-option label="YYYY-MM-DD hh:mm:ss" value="yyyy-MM-dd hh:mm:ss" />
        <el-option label="timestamp" value="timestamp" />
      </el-select>
    </el-form-item>
    <el-form-item label="增量参数">
      <IncrEditTable
        :list="
          readerForm.incrContent == null
            ? []
            : JSON.parse(readerForm.incrContent)
        "
        @handleTableData="handleTableData"
      ></IncrEditTable>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="next">下一步</el-button>
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import JobJdbcDatasourceAPI from "@/api/task/job-jdbc-datasource";
import JobDataXAPI from "@/api/task/job-datax";
import IncrEditTable from "@/views/task/job-datax/componects/IncrEditTable.vue";
import EditTable from "@/components/EditTable/EditTable.vue";

const readerForm = ref({
  incrType: 0,
  incrId: 0,
});
const jdbcDatasourceList = ref([]);
const tableList = ref([]);
const columnList = ref([]);

const emit = defineEmits(["next"]);

const props = defineProps({
  preData: Object,
});

const datasourceList = ["MYSQL", "ORACLE"];
const checkAll = ref(false);
const isIndeterminate = ref(true);

watch(
  () => readerForm.value.ds,
  (val) => {
    fetchJdbcDatasource(val);
  }
);

watch(
  () => props.preData,
  async (data) => {
    if (data.jdbcDatasourceId) {
      readerForm.value = data;
      await getTables(data.jdbcDatasourceId);
      await getColumns(data.jdbcDatasourceId);
      await fetchJdbcDatasource(data.datasource.datasource);
    }
  },
  { immediate: true, deep: true, once: true }
);

watch(
  () => readerForm.value.jdbcDatasourceId,
  (val) => {
    getTables(val);
  }
);

watch(
  () => readerForm.value.tableName,
  (val) => {
    readerForm.value.querySql = "";
    getColumns(readerForm.value.jdbcDatasourceId);
  }
);

function handleTableData(val) {
  readerForm.value.incrContent = JSON.stringify(val);
}

function handleCheckAllChange(val: boolean) {
  readerForm.value.columns = val ? columnList.value : [];
  isIndeterminate.value = false;
}

function next() {
  if (
    readerForm.value.columns == null ||
    readerForm.value.columns.length <= 0
  ) {
    if (readerForm.value.querySql.trim() == "") {
      ElMessage.warning("请选择要同步的数据列");
      return;
    }
  }

  readerForm.value.datasource = jdbcDatasourceList.value.find(
    (v) => v.id === readerForm.value.jdbcDatasourceId
  );
  emit("next", readerForm.value);
}

async function getTables(id: number) {
  tableList.value = [];
  await JobDataXAPI.getTables(id).then((data) => {
    tableList.value = data;
  });
}

async function getColumns(id: number) {
  const params = {} as any;
  if (readerForm.value.tableName != null) {
    params.tableName = readerForm.value.tableName;
  }
  if (readerForm.value.querySql != null) {
    params.querySql = readerForm.value.querySql;
  }
  columnList.value = [];
  await JobDataXAPI.getColumns(id, params).then((data) => {
    columnList.value = data;
  });
}

async function fetchJdbcDatasource(ds: string) {
  const data = await JobJdbcDatasourceAPI.getJdbcDatasourceList();
  jdbcDatasourceList.value = data.filter((v) => v.datasource == ds);
}
</script>
<style lang="scss" scoped>
.reader-form {
  padding: 20px;
  background: #fff;
}
</style>
