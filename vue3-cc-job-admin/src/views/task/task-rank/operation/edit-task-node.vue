<template>
  <el-drawer v-model="drawVisible" @close="cancelClick">
    <template #default>
      <el-form
        :model="formData"
        label-width="auto"
        style="max-width: 600px"
        :rules="rules"
      >
        <el-form-item label="执行器" prop="jobGroup">
          <el-select
            v-model="formData.jobGroup"
            filterable
            placeholder="Select"
            style="width: 200px"
            :disabled="formData.jobType == 2"
          >
            <el-option
              v-for="item in taskGroupList"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="author">
          <el-input v-model="formData.author" type="text" autocomplete="off" />
        </el-form-item>
        <el-form-item label="任务描述" prop="jobDesc">
          <el-input v-model="formData.jobDesc" type="text" autocomplete="off" />
        </el-form-item>
        <el-form-item label="报警邮件">
          <el-input
            v-model="formData.alarmEmail"
            type="text"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="运行模式" prop="glueType">
          <el-select
            v-model="formData.glueType"
            filterable
            placeholder="Select"
            style="width: 210px"
            @change="handleChangeGlueType"
            :disabled="formData.jobType == 2"
          >
            <el-option
              v-for="item in glueTypeList"
              :key="item.type"
              :label="item.title"
              :value="item.type"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="GLUE IDE"
          v-if="!['BEAN', 'API', 'SQL'].includes(formData.glueType)"
        >
          <el-button type="success" @click="glueClick">GLUE IDE</el-button>
        </el-form-item>
        <el-form-item
          label="JobHandler"
          prop="executorHandler"
          v-if="formData.glueType == 'BEAN'"
        >
          <el-input
            v-model="formData.executorHandler"
            type="text"
            autocomplete="off"
            :disabled="formData.jobType == 2"
          />
        </el-form-item>
        <el-form-item
          label="请求类型"
          prop="reqType"
          v-if="formData.glueType == 'API'"
        >
          <el-select v-model="formData.reqType" filterable style="width: 210px">
            <el-option key="GET" label="GET" value="GET" />
            <el-option key="POST" label="POST" value="POST" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据库" v-if="formData.glueType == 'SQL'">
          <el-select
            v-model="formData.jdbcDatasourceId"
            filterable
            style="width: 210px"
          >
            <el-option
              v-for="item in jdbcDatasourceList"
              :key="item.id"
              :label="item.databaseName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="请求地址"
          prop="reqUrl"
          v-if="formData.glueType === 'API'"
        >
          <el-input
            v-model="formData.reqUrl"
            type="textarea"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item
          label="请求头"
          prop="reqHeader"
          v-if="formData.glueType === 'API'"
        >
          <EditTable
            :list="
              formData.reqHeader == null ? [] : JSON.parse(formData.reqHeader)
            "
            @handleTableData="handleTableData"
            style="width: 720px"
          />
        </el-form-item>
        <el-form-item
          label="body"
          v-if="formData.glueType === 'API' && formData.reqType === 'POST'"
        >
          <el-input
            v-model="formData.reqBody"
            type="textarea"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="任务参数" v-if="formData.glueType !== 'API'">
          <el-input
            v-model="formData.executorParam"
            type="textarea"
            autocomplete="off"
            :disabled="formData.jobType == 2"
          />
        </el-form-item>
        <!--        <el-form-item label="调度过期策略" prop="misfireStrategy">-->
        <!--          <el-select-->
        <!--            v-model="formData.misfireStrategy"-->
        <!--            filterable-->
        <!--          >-->
        <!--            <el-option-->
        <!--              v-for="item in misfireStrategyList"-->
        <!--              :key="item.type"-->
        <!--              :label="item.title"-->
        <!--              :value="item.type"-->
        <!--            />-->
        <!--          </el-select>-->
        <!--        </el-form-item>-->
        <el-form-item label="任务超时时间">
          <el-input
            v-model="formData.executorTimeout"
            type="textarea"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="任务失败策略" prop="executorBlockStrategy">
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
        <el-form-item label="任务重试次数">
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
        <el-button @click="cancelClick">取消</el-button>
        <el-button type="primary" @click="confirmClick">确认</el-button>
      </div>
    </template>
  </el-drawer>

  <CodeEditor
    :glueTaskId="props.nodeTaskId"
    :glueVisible="glueVisible"
    :code="code"
    :nowDate="new Date()"
    @close="closeGlue"
  ></CodeEditor>
</template>
<script setup lang="ts">
import EditTable from "@/components/EditTable/EditTable.vue";
import TaskGroupAPI from "@/api/task/task-group";
import TaskInfoAPI from "@/api/task/task-info";
import { getThemeCode } from "@/utils/theme";
import CodeEditor from "@/components/CodeEdit/index.vue";
import JobJdbcDatasourceAPI from "@/api/task/job-jdbc-datasource";

const props = defineProps({
  taskNodeVisible: {
    type: Boolean,
    default: false,
  },
  nodeTaskId: {
    type: Number,
    default: -1,
  },
  nowDate: {
    type: Date,
    default: null,
  },
});
const emit = defineEmits(["close"]);
const drawVisible = ref(false);
const formData = reactive({});
const taskGroupList = ref([]);
const rules = reactive({
  jobGroup: [{ required: true }],
  author: [{ required: true }],
  jobDesc: [{ required: true }],
  glueType: [{ required: true }],
  executorHandler: [{ required: true }],
  reqType: [{ required: true }],
  reqUrl: [{ required: true }],
  misfireStrategy: [{ required: true }],
  executorBlockStrategy: [{ required: true }],
});
const jdbcDatasourceList = ref([]);

const glueTypeList = [
  {
    type: "BEAN",
    title: "BEAN",
  },
  {
    type: "API",
    title: "API",
  },
  {
    type: "SQL",
    title: "SQL",
  },
  {
    type: "GLUE_GROOVY",
    title: "GLUE(Java)",
  },
  {
    type: "GLUE_SHELL",
    title: "GLUE(Shell)",
  },
  {
    type: "GLUE_PYTHON",
    title: "GLUE(Python)",
  },
  {
    type: "GLUE_PHP",
    title: "GLUE(PHP)",
  },
  {
    type: "GLUE_NODEJS",
    title: "GLUE(Nodejs)",
  },
  {
    type: "GLUE_POWERSHELL",
    title: "GLUE(PowerShell)",
  },
];
// const misfireStrategyList = [
//   {
//     type: "DO_NOTHING",
//     title: "忽略",
//   },
//   {
//     type: "FIRE_ONCE_NOW",
//     title: "立即执行一次",
//   },
// ];
const blockStrategyList = [
  {
    type: "SERIAL_EXECUTION",
    title: "单机串行",
  },
  {
    type: "DO_NOTHING",
    title: "忽略",
  },
];

watch(
  () => props.taskNodeVisible,
  (val) => {
    drawVisible.value = val;
  }
);

watch(
  () => props.nowDate,
  () => {
    if (props.nodeTaskId) {
      getTaskInfo();
    }
  }
);

const glueVisible = ref(false);
const code = ref("");

function glueClick() {
  glueVisible.value = true;
  const glueType = formData.glueType;
  TaskInfoAPI.getFormData(props.nodeTaskId).then((data) => {
    if (data.glueSource == null || data.glueSource.length <= 0) {
      code.value = getThemeCode(glueType);
    } else {
      code.value = data.glueSource;
    }
  });
}

function handleChangeGlueType(data: any) {
  formData.executorHandler = "";
  formData.reqUrl = "";
  formData.reqHeader = null;
  formData.reqBody = "";
  formData.executorParam = "";
}

function closeGlue() {
  glueVisible.value = false;
}

function handleTableData(val) {
  formData.reqHeader = JSON.stringify(val);
}

async function fetchJdbcDatasource() {
  const data = await JobJdbcDatasourceAPI.getJdbcDatasourceList();
  jdbcDatasourceList.value = data as any;
}

function cancelClick() {
  emit("close");
}
function confirmClick() {
  if (!props.nodeTaskId) {
    return;
  }
  if (formData.glueType == "SQL") {
    formData.executorHandler = "runJobJdbcXxlJob";
  }
  if (formData.glueType == "API") {
    formData.executorHandler = "runApiHandler";
  }

  formData.misfireStrategy = "DO_NOTHING";
  formData.scheduleType = "NONE";
  TaskInfoAPI.update(props.nodeTaskId, formData)
    .then(() => {
      ElMessage.success("修改成功");
    })
    .finally(() => {});
}

async function fetchTaskGroupList() {
  const data = await TaskGroupAPI.getAllTaskGroupList();
  taskGroupList.value = data as any;
}

async function getTaskInfo() {
  if (props.nodeTaskId) {
    TaskInfoAPI.getFormData(props.nodeTaskId).then((data) => {
      Object.assign(formData, data);
    });
  }
}
onMounted(() => {
  fetchTaskGroupList();
  fetchJdbcDatasource();
});
</script>
<style scoped lang="scss"></style>
