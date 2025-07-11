<template>
  <div class="app-container">
    <!-- task_group表单弹窗 -->
    <el-dialog
      v-model="taskGroupVisible.visible"
      :title="taskGroupVisible.title"
      width="500px"
      @close="handleCloseDialog"
    >
      <el-form ref="dataFormRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="AppName" prop="appName">
          <el-input v-model="formData.appName" placeholder="请输入AppName" />
        </el-form-item>
        <el-form-item label="执行器名称" prop="title">
          <el-input v-model="formData.title" placeholder="请输入执行器名称" />
        </el-form-item>
        <el-form-item label="注册方式" prop="addressType">
          <el-radio-group v-model="formData.addressType">
            <el-radio border :value="0">自动注册</el-radio>
            <el-radio border :value="1">手动录入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="机器地址" prop="addressList">
          <el-input
            v-model="formData.addressList"
            type="textarea"
            placeholder="执行器地址列表，多地址逗号分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleSubmit()">确定</el-button>
          <el-button @click="handleCloseDialog()">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import JobGroupAPI from "@/api/task/job-group";

const emit = defineEmits(["close", "handleResetQuery"]);

const props = defineProps({
  formData: {
    type: Object,
    default: null,
  },
  taskGroupVisible: {
    type: Object,
    default: null,
  },
});

const dataFormRef = ref(ElForm);

watch(
  () => props.taskGroupVisible,
  () => {}
);

// task_group表单校验规则
const rules = reactive({
  appName: [{ required: true, message: "请输入AppName", trigger: "blur" }],
  title: [{ required: true, message: "请输入执行器名称", trigger: "blur" }],
  addressType: [{ required: true, message: "请选择执行器地址类型", trigger: "blur" }],
  addressList: [
    { required: true, message: "请输入执行器地址列表，多地址逗号分隔", trigger: "blur" },
  ],
});

/** 提交task_group表单 */
function handleSubmit() {
  dataFormRef.value.validate((valid: any) => {
    if (valid) {
      const id = props.formData.id;
      if (id) {
        JobGroupAPI.update(id, props.formData)
          .then(() => {
            ElMessage.success("修改成功");
            handleCloseDialog();
            emit("handleResetQuery");
          })
          .finally(() => {});
      } else {
        JobGroupAPI.add(props.formData)
          .then(() => {
            ElMessage.success("新增成功");
            handleCloseDialog();
            emit("handleResetQuery");
          })
          .finally(() => {});
      }
    }
  });
}

/** 关闭task_group弹窗 */
function handleCloseDialog() {
  emit("close");
  dataFormRef.value.resetFields();
  dataFormRef.value.clearValidate();
}
</script>
