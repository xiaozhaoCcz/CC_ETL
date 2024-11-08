<template>
  <div class="app-container">
    <el-dialog
      v-model="props.editTaskInfoVal"
      title="更新任务"
      width="850px"
      :before-close="handleClose"
    >
      <div class="info_form">
        <div class="child_form">
          <div style="color: #8e8e8e; font-size: 14px">基础配置</div>
          <el-divider style="margin: 8px" />
          <div class="child_main">
            <div class="m_left">
              <div class="c_cont">
                <span>执行器*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
              <div class="c_cont">
                <span>负责人*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
            <div class="m_right">
              <div class="c_cont">
                <span>任务描述*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
              <div class="c_cont">
                <span>报警邮件</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="child_form">
          <div style="color: #8e8e8e; font-size: 14px">调度配置</div>
          <el-divider style="margin: 8px" />
          <div class="child_main">
            <div class="m_left">
              <div class="c_cont">
                <span>调度类型*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
            <div class="m_right">
              <div class="c_cont">
                <span>CRON*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="child_form">
          <div style="color: #8e8e8e; font-size: 14px">任务配置</div>
          <el-divider style="margin: 8px" />
          <div class="child_main">
            <div class="m_left">
              <div class="c_cont">
                <span>运行模式</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
            <div class="m_right">
              <div class="c_cont">
                <span>JobHandler*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="child_form">
          <div class="child_main">
            <div class="m_left">
              <div class="c_cont_param">
                <span>任务参数*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="child_form">
          <div style="color: #8e8e8e; font-size: 14px">高级配置</div>
          <el-divider style="margin: 8px" />
          <div class="child_main">
            <div class="m_left">
              <div class="c_cont">
                <span>路由策略*</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
              <div class="c_cont">
                <span>调度过期策略</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
              <div class="c_cont">
                <span>任务超时时间</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
            <div class="m_right">
              <div class="c_cont">
                <span>子任务id</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
              <div class="c_cont">
                <span>阻塞处理策略</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
              <div class="c_cont">
                <span>失败重试次数</span>
                <el-input
                  v-model="taskInfoForm.taskName"
                  type="text"
                  autocomplete="off"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="submitForm()"> 保存 </el-button>
        <el-button @click="resetForm()">取消</el-button>
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
<style lang="scss" scoped>
.info_form {
  width: 100%;
  .child_form {
    width: 100%;
    .child_main {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin: 0 50px;

      .c_cont {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;
        span {
          display: inline-block;
          width: 140px;
          font-size: 14px;
          text-align: right;
          margin-right: 10px;
        }
      }
      .c_cont_param {
        display: flex;
        align-items: center;
        width: 710px;
        margin-left: 10px;
        span {
          display: inline-block;
          width: 100px;
          font-size: 14px;
          text-align: right;
          margin-right: 10px;
        }
      }
    }
  }
}
</style>
