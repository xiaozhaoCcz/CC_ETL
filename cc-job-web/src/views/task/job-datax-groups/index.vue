<template>
  <div class="app-container">
    <el-steps :active="active" finish-status="success">
      <el-step title="Step 1" />
      <el-step title="Step 2" />
      <el-step title="Step 3" />
    </el-steps>

    <Readers v-if="active === 0" />
    <Writers v-if="active === 1" />
    <Merges v-if="active === 2" />
    <el-button v-if="active > 0" style="margin-top: 12px" @click="pre">
      上一步
    </el-button>
    <el-button v-if="active <= 2" style="margin-top: 12px" @click="next">
      下一步
    </el-button>
  </div>
</template>

<script setup lang="ts">
import Readers from "./step/readers.vue";
import Writers from "@/views/task/job-datax-groups/step/writers.vue";
import Merges from "@/views/task/job-datax-groups/step/merges.vue";
import JobInfoAPI from "@/api/task/job-info";
import { useDataxStore } from "@/store";
import JobDataXAPI from "@/api/task/job-datax";
const userStore = useDataxStore();

const active = ref(0);

function pre() {
  active.value--;
}

function next() {
  if (
    active.value === 0 &&
    (userStore.dataxGroups.readers.tableList == null ||
      userStore.dataxGroups.readers.tableList.length < 0)
  ) {
    ElMessage.error("请选择读取数据表");
    return;
  }
  if (
    active.value === 1 &&
    (userStore.dataxGroups.writers.tableList == null ||
      userStore.dataxGroups.writers.tableList.length < 0)
  ) {
    ElMessage.error("请选择写入数据表");
    return;
  }

  active.value++;
  if (active.value > 2) {
    submitForm();
  }
}

async function submitForm() {
  JobDataXAPI.batchBuildJson(userStore.dataxGroups).then(() => {
    ElMessage.success("新增成功");
    userStore.clearDataxGroups();
    active.value = 0;
  });
}
</script>
<style scoped lang="scss">
.app-container {
  padding: 20px;
}
</style>
