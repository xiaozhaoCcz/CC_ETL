<template>
  <el-drawer v-model="drawVisible" @close="cancelClick">
    <template #default>
      <el-form :model="formData" label-width="auto" style="max-width: 600px">
        <el-form-item label="执行器*">
          <el-select
            v-model="formData.jobGroup"
            filterable
            placeholder="Select"
            style="width: 200px"
          >
            <el-option
              v-for="item in taskGroupList"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人*">
          <el-input
            v-model="formData.author"
            type="text"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="任务描述*">
          <el-input
            v-model="formData.jobDesc"
            type="text"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="报警邮件">
          <el-input
            v-model="formData.alarmEmail"
            type="text"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="调度类型*">
          <el-select
            v-model="formData.scheduleType"
            filterable
            placeholder="Select"
            style="width: 210px"
          >
            <el-option
              v-for="item in scheduleTypeList"
              :key="item.type"
              :label="item.title"
              :value="item.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="CRON" v-if="formData.scheduleType=='CRON'">
          <el-input
            v-model="formData.scheduleConf"
            placeholder="cron表达式..."
          >
            <template #append>
              <el-button @click="cronPopover = !cronPopover">
                设置
              </el-button>
            </template>
          </el-input>
          <div v-show="cronPopover" class="cronPopover">
            <noVue3Cron
              :cron-value="formData.scheduleConf"
              i18n="cn"
              @change="changeCron"
              @close="cronPopover = false"
            />
          </div>
        </el-form-item  >
        <el-form-item label="固定秒" v-if="formData.scheduleType=='FIX_RATE'">
          <el-input
            v-model="formData.scheduleConf"
            placeholder="默认秒"
          />
        </el-form-item>
        <el-form-item label="运行模式*">
          <el-select
            v-model="formData.glueType"
            filterable
            placeholder="Select"
            style="width: 210px"
          >
            <el-option
              v-for="item in glueTypeList"
              :key="item.type"
              :label="item.title"
              :value="item.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="JobHandler*">
          <el-input
            v-model="formData.executorHandler"
            type="text"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="请求类型*" v-if="formData.glueType=='API'">
          <el-select
            v-model="formData.reqType"
            filterable
            placeholder="Select"
            style="width: 210px"
          >
            <el-option
              key="GET"
              label="GET"
              value="GET"
            />
            <el-option
              key="POST"
              label="POST"
              value="POST"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="请求地址"  v-if="formData.glueType==='API'">
          <el-input
            v-model="formData.reqUrl"
            type="textarea"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="请求头" v-if="formData.glueType==='API'">
          <EditTable :list="formData.reqHeader==null?[]:JSON.parse(formData.reqHeader)" @handleTableData="handleTableData"  style="width: 720px"/>
        </el-form-item>
        <el-form-item label="body" v-if="formData.glueType==='API'&&formData.reqType==='POST'">
          <el-input
            v-model="formData.reqBody"
            type="textarea"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="任务参数"  v-if="formData.glueType!=='API'">
          <el-input
            v-model="formData.executorParam"
            type="textarea"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="调度过期策略">
          <el-select
            v-model="formData.misfireStrategy"
            filterable
            placeholder="Select"
          >
            <el-option
              v-for="item in misfireStrategyList"
              :key="item.type"
              :label="item.title"
              :value="item.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务超时时间">
          <el-input
            v-model="formData.executorTimeout"
            type="textarea"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="任务失败策略">
          <el-select
            v-model="formData.executorBlockStrategy"
            filterable
            placeholder="Select"
          >
            <el-option
              v-for="item in blockStrategyList"
              :key="item.type"
              :label="item.title"
              :value="item.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务超时时间">
          <el-input
            v-model="formData.executorFailRetryCount"
            type="text"
            autocomplete="off"
          />
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <div style="flex: auto">
        <el-button @click="cancelClick">cancel</el-button>
        <el-button type="primary" @click="confirmClick">confirm</el-button>
      </div>
    </template>
  </el-drawer>
</template>
<script setup lang="ts">

import NoVue3Cron from "@/components/NoVue3Cron/index.vue";
import EditTable from "@/components/EditTable/EditTable.vue";
import TaskGroupAPI from "@/api/task/task-group";
import TaskInfoAPI from "@/api/task/task-info";

const props = defineProps({
  taskNodeVisible: {
    type: Boolean,
    default: false
  },
  nodeTaskId: {
    type: Number,
    default: -1
  },
  nowDate:{
    type: Date,
    default:null
  }
});
const emit = defineEmits(["close"]);
const drawVisible = ref(false);
const formData = reactive({});
const taskGroupList = ref([]);
const scheduleTypeList = [
  {
    type: "CRON",
    title: "CRON"
  },
  { type: "NONE", title: "无" },
  { type: "FIX_RATE", title: "固定速度" }
];
const glueTypeList = [
  {
    type: "BEAN",
    title: "BEAN"
  },
  {
    type: "API",
    title: "API"
  }
];
const misfireStrategyList = [
  {
    type: "DO_NOTHING",
    title: "忽略"
  },
  {
    type: "FIRE_ONCE_NOW",
    title: "立即执行一次"
  }
];
const blockStrategyList = [
  {
    type: "SERIAL_EXECUTION",
    title: "单机串行"
  },
  {
    type: "DO_NOTHING",
    title: "忽略"
  },
];

watch(()=>props.taskNodeVisible,(val)=>{
  drawVisible.value = val;
})

watch(()=>props.nowDate,()=>{
   if(props.nodeTaskId)
   {
     getTaskInfo();
   }
})

function handleTableData(val){
  formData.reqHeader = JSON.stringify(val);
}

function changeCron(cron: string) {
  formData.scheduleConf = cron;
}

function cancelClick() {
    emit("close");
}
function confirmClick() {
    if(!props.nodeTaskId){
      return;
    }
  TaskInfoAPI.update(props.nodeTaskId,formData)
    .then(() => {
      ElMessage.success("修改成功");
    })
    .finally(() => {
    });
}

async function fetchTaskGroupList() {
  const data = await TaskGroupAPI.getAllTaskGroupList();
  taskGroupList.value = data as any;
}

async function  getTaskInfo(){
  if(props.nodeTaskId){
    TaskInfoAPI.getFormData(props.nodeTaskId).then((data) => {
      Object.assign(formData, data);
    });
  }
}
onMounted( () => {
   fetchTaskGroupList();
});
</script>
<style scoped lang="scss">

</style>
