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
      @pre="preStep1"
      @next="nextStep2"
    />
    <Merge v-if="active == 2" />
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import Reader from "./step/reader.vue";
import Writer from "./step/writer.vue";
import Merge from "./step/merge.vue";

const active = ref(2);
const readerForm = ref({});
const writerForm = ref({});

function nextStep1(data: any) {
  console.log(data);
  readerForm.value = data;
  active.value++;
}

function preStep1(data: any) {
  writerForm.value = data;
  active.value--;
}

function nextStep2(data: any) {
  writerForm.value = data;
  active.value++;
}

function buildJson(){
  const readerDataXParams = {
       
  }
  const writerDataXParams = {

  }
}

const next = () => {
  if (active.value++ > 2) active.value = 0;
};
</script>
