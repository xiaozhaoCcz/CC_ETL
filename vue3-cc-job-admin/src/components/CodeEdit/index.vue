<template>
  <el-dialog
    v-model="_glueVisible"
    title="GLUE IDE"
    width="850px"
    :before-close="handleCloseDialog"
  >
    <div class="editor-container">
      <MonacoEditor
        v-model:value="_code"
      />
    </div>
    <template #footer>
      <div class="footer">
        <div>
          <span class="m_title">备注</span>
          <el-input
            v-model="_input"
            placeholder="备注"
            style="width: 200px;"
          />
        </div>
        <div>
          <el-select
            v-model="glueId"
            filterable
            placeholder="Select"
            @change="handleGlueChange"
            style="width: 200px;"
          >
            <el-option
              v-for="item in glueList"
              :key="item.id"
              :label="item.glueRemark"
              :value="item.id"
            />
          </el-select>
        </div>
        <div>
          <el-button type="primary" @click="submitForm()">保存</el-button>
          <el-button @click="handleCloseDialog()">取消</el-button>
        </div>
      </div>

    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import MonacoEditor from './MonacoEditor.vue'
import TaskInfoAPI from "@/api/task/task-info";
const emit = defineEmits(["close"]);
const props = defineProps({
  glueVisible: {
    type: Boolean,
    default: false
  },
  code:{
     type:String,
     default:'',
  },
  glueTaskId:{
    type:Number,
    default:null,
  },
  nowDate:{
    type:Date,
    default:null,
  }
});

const _glueVisible = ref(false)
const _input =ref('')
const _code = ref('');
const taskId = ref(null)
const glueList = ref([{}]);
const glueId = ref(null);

watch(()=>props.glueVisible,(val)=>{
  _glueVisible.value = val;
})

watch(()=>props.code,(val)=>{
  _code.value = val;
})

watch(()=>props.glueTaskId,(val)=>{
  taskId.value = val;
  if(val) {
    getGlueList(val)
  }
})

watch(()=>props.nowDate,()=>{})

function handleCloseDialog(){
    emit('close')
}

function handleGlueChange(val:any){
  console.log(val);
  if(val!=null){
     _code.value = glueList.value.find((item)=>item.id==val).glueSource;
  }
}

function getGlueList(){
  TaskInfoAPI.getGlueList(taskId.value).then((data)=>{
    glueList.value = data;
  })
}

function submitForm(){
  if(_input.value==''||_input.value.trim().length<=0){
    ElMessage.warning("请输入备注～")
    return;
  }
  const obj = {};
  obj.taskId = taskId.value;
  obj.glueRemark = _input.value
  obj.glueSource = _code.value;
  TaskInfoAPI.saveGlueSource(obj).then(data=>{
    _input.value = ''
    ElMessage.success("保存成功")
  })
}

</script>
<style lang="scss" scoped>
.footer{
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.m_title::after {
  content: "*";
  color: red;
  font-size: 16px;
  margin-right: 5px
}
</style>
