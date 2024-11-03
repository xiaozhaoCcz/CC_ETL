<template>
  <div class="app-container">
    <el-dialog
      v-model="props.editTaskInfoVal"
      title="更新任务"
      width="650"
      :before-close="handleClose"
    >
      <template #footer>
        <el-form :model="taskInfoForm" class="demo-ruleForm">
          <!-- 基础配置 -->
          <el-row>
            <el-row style="width: 100%">
              <span style="color: #8e8e8e; font-size: 14px">基础配置</span>
              <el-divider style="margin: 8px" />
            </el-row>
            <el-row justify="space-around" style="width: 100%">
              <el-col :span="12">
                <el-form-item label="执行器" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="任务描述" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row justify="space-around" style="width: 100%">
              <el-col :span="12">
                <el-form-item label="执行器" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="任务描述" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-row>
          <!-- ----------- -->

          <!-- 调度配置 -->
          <el-row>
            <el-row style="width: 100%">
              <span style="color: #8e8e8e; font-size: 14px">调度配置</span>
              <el-divider style="margin: 8px" />
            </el-row>
            <el-row justify="space-around" style="width: 100%">
              <el-col :span="12">
                <el-form-item label="调度类型" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Cron" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-row>
          <!-- ----------- -->

          <!-- 任务配置 -->
          <el-row>
            <el-row style="width: 100%">
              <span style="color: #8e8e8e; font-size: 14px">任务配置</span>
              <el-divider style="margin: 8px" />
            </el-row>
            <el-row justify="space-around" style="width: 100%">
              <el-col :span="12">
                <el-form-item label="运行模式" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="JobHandler" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row justify="space-around" style="width: 100%">
              <el-col :span="24">
                <el-form-item label="任务参数" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-row>
          <!-- ----------- -->

          <!-- 高级配置 -->
          <el-row>
            <el-row style="width: 100%">
              <span style="color: #8e8e8e; font-size: 14px">高级配置</span>
              <el-divider style="margin: 8px" />
            </el-row>
            <el-row style="width: 100%">
              <el-col :span="12">
                <el-form-item label="路由策略" prop="pass">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                    style="width: 200px; margin-left: 28px"
                  />
                </el-form-item>
                <el-form-item label="调度过期策略" prop="pass">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                    style="width: 200px"
                  />
                </el-form-item>
                <el-form-item label="调度过期策略" prop="pass">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                    style="width: 200px"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="子任务id" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <!-- <el-row justify="space-around" style="width: 100%">
              <el-col :span="12">
                <el-form-item label="调度过期策略" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="阻塞处理策略" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row justify="space-around" style="width: 100%">
              <el-col :span="12">
                <el-form-item label="任务超时时间" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="失败重试次数" prop="pass" width="200px">
                  <el-input
                    v-model="taskInfoForm.executorParam"
                    type="text"
                    autocomplete="off"
                  />
                </el-form-item>
              </el-col>
            </el-row> -->
          </el-row>
          <el-form-item>
            <el-button type="primary" @click="submitForm()"> 保存 </el-button>
            <el-button @click="resetForm()">取消</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
const emit = defineEmits(["close"]);

const props = defineProps({
  taskId: {
    type: Number,
    default: 0,
  },
  editTaskInfoVal: {
    type: Boolean,
    default: false,
  },
});

watch(
  () => props.editTaskInfoVal,
  () => {}
);

const taskInfoForm = reactive({});

const submitForm = () => {};

const resetForm = () => {
  emit("close");
};
</script>
