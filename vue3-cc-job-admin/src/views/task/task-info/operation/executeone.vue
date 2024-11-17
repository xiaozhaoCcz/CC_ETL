<template>
  <div class="app-container">
    <el-dialog
      v-model="props.executeOneVal"
      title="执行一次"
      width="500"
      :before-close="handleClose"
    >
      <template #footer>
        <el-form
          style="max-width: 600px"
          :model="taskInfoTriggerDto"
          status-icon
          label-width="auto"
          class="demo-ruleForm"
        >
          <el-form-item label="任务参数" prop="pass">
            <el-input
              v-model="taskInfoTriggerDto.executorParam"
              type="textarea"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="机器地址" prop="checkPass">
            <el-input
              v-model="taskInfoTriggerDto.addressList"
              type="textarea"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitForm()">保存</el-button>
            <el-button @click="resetForm()">取消</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import TaskInfoAPI from "@/api/task/task-info";

const props = defineProps({
  taskId: {
    type: Number,
    default: 0,
  },
  executeOneVal: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["close"]);

const taskInfoTriggerDto = reactive({
  id: 0,
  executorParam: "",
  addressList: "",
});

watch(
  () => props.executeOneVal,
  () => {}
);

watch(
  () => props.taskId,
  (val) => {
    taskInfoTriggerDto.id = val;
  }
);

const submitForm = () => {
  // TODO: submit form data
  console.log(taskInfoTriggerDto);
  TaskInfoAPI.triggerJob(taskInfoTriggerDto)
    .then((data) => {
      ElMessage.success("执行任务成功");
    })
    .catch((e) => {
      ElMessage.error(e);
    })
    .finally(() => {
      resetForm();
    });
};

const resetForm = () => {
  taskInfoTriggerDto.id = 0;
  taskInfoTriggerDto.executorParam = "";
  taskInfoTriggerDto.addressList = "";
  props.executeOneVal = false;
  emit("close");
};

const handleClose = (done: () => void) => {
  resetForm();
};
</script>
