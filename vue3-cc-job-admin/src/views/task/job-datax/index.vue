<template>
  <div class="app-container">
    <el-steps :active="active" finish-status="success">
      <el-step title="Step 1" />
      <el-step title="Step 2" />
      <el-step title="Step 3" />
    </el-steps>

    <Reader v-if="active == 0" :preData="readerForm" @next="nextStep1" />
    <Writer
      v-if="active == 1"
      :preData="writerForm"
      :preFormData="fromData"
      @pre="preStep1"
      @next="nextStep2"
    />
    <Merge
      v-if="active == 2"
      :finalJson="finalJson"
      :preData="writerForm"
      :preFormData="fromData"
      @pre="preStep2"
      @handleReset="handleReset"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import Reader from "./step/reader.vue";
import Writer from "./step/writer.vue";
import Merge from "./step/merge.vue";
import JobDataXAPI from "@/api/task/job-datax";

const active = ref(0);
const readerForm = ref({});
const writerForm = ref({});
const finalJson = ref("");
const fromData = ref({});

function nextStep1(data: any) {
  console.log(data);
  readerForm.value = data;
  active.value++;
}

function preStep1(data: any, data2: any) {
  writerForm.value = data;
  fromData.value = data2;
  active.value--;
}

function preStep2(data: any, data2: any) {
  writerForm.value = data;
  fromData.value = data2;
  active.value--;
}

async function nextStep2(data: any, data2: any) {
  writerForm.value = data;
  fromData.value = data2;
  await buildJson();
  active.value++;
}

function handleReset() {
  active.value = 0;
  readerForm.value = {};
  writerForm.value = {};
  finalJson.value = "";
}

function getIpAndPort(url: string) {
  const match = url.match(/\/\/([^:]+):([^\/]+)/);
  const formData = {
    ip: match[1],
    port: match[2],
  };
  return formData;
}

async function buildJson() {
  console.log(readerForm.value);
  const readerDataXParams = {
    columns: readerForm.value.columns,
    sourceType: readerForm.value.datasource.datasource,
    username: readerForm.value.datasource.jdbcUsername,
    password: readerForm.value.datasource.jdbcPassword,
    dbName: readerForm.value.datasource.databaseName,
    tableName: readerForm.value.tableName,
    ip: getIpAndPort(readerForm.value.datasource.jdbcUrl).ip,
    port: getIpAndPort(readerForm.value.datasource.jdbcUrl).port,
    querySql: readerForm.value.querySql,
    type: 0,
    incrType: readerForm.value.incrType,
    incrContent: readerForm.value.incrContent,
  };
  const writerDataXParams = {
    columns: writerForm.value.columns,
    sourceType: writerForm.value.datasource.datasource,
    username: writerForm.value.datasource.jdbcUsername,
    password: writerForm.value.datasource.jdbcPassword,
    dbName: writerForm.value.datasource.databaseName,
    tableName: writerForm.value.tableName,
    ip: getIpAndPort(writerForm.value.datasource.jdbcUrl).ip,
    port: getIpAndPort(writerForm.value.datasource.jdbcUrl).port,
    type: 1,
    writeMode: writerForm.value.writeMode,
  };
  let readerJson = "";
  let writerJson = "";
  await JobDataXAPI.getJson(readerDataXParams).then((data) => {
    readerJson = data;
  });
  await JobDataXAPI.getJson(writerDataXParams).then((data) => {
    writerJson = data;
  });
  const dataList = [];
  const obj = {
    readerJson: readerJson,
    writerJson: writerJson,
  };
  dataList.push(obj);
  finalJson.value = `{"job":{"content":[ ${getContent(dataList)} ],"setting":{"speed":{"channel":3,"byte":-1},"errorLimit":{"record":0,"percentage":0.02} } }}`;
  console.log("finalJson: ", finalJson.value);
  fromData.value.incrType = readerForm.value.incrType;
  fromData.value.incrId = readerForm.value.incrId;
  fromData.value.incrTime = readerForm.value.incrTime;
  fromData.value.timeFormat = readerForm.value.timeFormat;
  fromData.value.incrParam = readerForm.value.incrParam;
  fromData.value.incrColumnType = readerForm.value.incrColumnType;
  fromData.value.incrColumnName = readerForm.value.incrColumnName;
  fromData.value.jdbcDatasourceId = writerForm.value.datasource.id;
  fromData.value.incrContent = readerForm.value.incrContent;
}

function getContent(data: any) {
  const arr = [] as any;
  data.forEach((item) => {
    arr.push(`{"reader":${item.readerJson},"writer":${item.writerJson}}`);
  });
  return arr.join(",");
}
</script>
