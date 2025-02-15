<template>
  <div class="readers-container">
    <el-form
      ref="formRef"
      :model="userStore.dataxGroups.readers"
      label-width="auto"
      class="reader-form"
    >
      <el-form-item label="数据源" prop="datasource">
        <el-select
          v-model="userStore.dataxGroups.readers.ds"
          filterable
          placeholder="选择数据源"
          style="width: 210px"
        >
          <el-option
            v-for="item in datasourceList"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="数据库" prop="jdbcDatasourceId">
        <el-select
          v-model="userStore.dataxGroups.readers.jdbcDatasourceId"
          filterable
          placeholder="选择数据库"
          style="width: 210px"
        >
          <el-option
            v-for="item in jdbcDatasourceList"
            :key="item.id"
            :label="`${item.datasourceName}:${item.databaseName}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="数据表" prop="dataTables">
        <el-table
          :data="tableList"
          style="width: 300px; height: 600px; overflow-y: hidden"
          :border="true"
          @selection-change="handleDataTables"
        >
          <el-table-column type="selection" width="60" />
          <el-table-column label="表名字" width="240" align="center">
            <template #default="scope">{{ scope.row }}</template>
          </el-table-column>
        </el-table>
      </el-form-item>
    </el-form>
  </div>
</template>
<script setup lang="ts">
import JobJdbcDatasourceAPI from "@/api/task/job-jdbc-datasource";
import JobDataXAPI from "@/api/task/job-datax";
import { useDataxStore } from "@/store/modules/datax";

const userStore = useDataxStore();

// const readerForm = ref({});
const jdbcDatasourceList = ref([]);
const datasourceList = ["MYSQL", "ORACLE"];
const tableList = ref([]);

watch(
  () => userStore.dataxGroups.readers.ds,
  (val) => {
    fetchJdbcDatasource(val);
  },{
    immediate:true
  }
);
watch(
  () => userStore.dataxGroups.readers.jdbcDatasourceId,
  (val) => {
    fetchDataTables(val);
  },{
    immediate:true
  }
);

async function fetchJdbcDatasource(ds: string) {
  const data = await JobJdbcDatasourceAPI.getJdbcDatasourceList();
  jdbcDatasourceList.value = data.filter((v) => v.datasource == ds);
}

const handleDataTables = (val: any) => {
  userStore.dataxGroups.readers.tableList = val;
};

async function fetchDataTables(id: string) {
  if (id == undefined) {
    return;
  }
  tableList.value = [];
  await JobDataXAPI.getTables(id).then((data) => {
    tableList.value = data;
  });
}
</script>
<style scoped lang="scss">
.readers-container {
  max-width: 800px;
  background: #fff;
  margin: 0 auto;
  padding: 30px;
  border-radius: 8px;
}
</style>
