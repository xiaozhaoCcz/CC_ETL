<template>
  <el-dialog
    v-model="drawVisible"
    :title="props.nodeJobId ? '编辑任务' : '新增任务'"
    width="540px"
    @close="cancelClick"
    draggable
    :close-on-click-modal="false"
    class="job-node-dialog compact"
    append-to-body
  >
    <div class="dialog-content">
      <el-form
        :model="formData"
        label-width="120px"
        :rules="rules"
        class="job-form"
        size="default"
      >
        <!-- 基本信息区域 -->
        <div class="form-section">
          <div class="section-header">
            <el-icon><Document /></el-icon>
            <span>基本信息</span>
          </div>
          <div class="form-grid">
            <el-form-item label="执行器" prop="jobGroup" class="form-item">
              <el-select
                v-model="formData.jobGroup"
                filterable
                placeholder="请选择执行器"
                class="form-select"
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
            <el-form-item label="负责人" prop="author" class="form-item">
              <el-input
                v-model="formData.author"
                type="text"
                autocomplete="off"
                placeholder="请输入负责人姓名"
                class="form-input"
              />
            </el-form-item>
            <el-form-item label="任务描述" prop="jobDesc" class="form-item full-width">
              <el-input
                v-model="formData.jobDesc"
                type="text"
                autocomplete="off"
                placeholder="请输入任务描述"
                class="form-input"
              />
            </el-form-item>
            <el-form-item label="报警邮件" class="form-item full-width">
              <el-input
                v-model="formData.alarmEmail"
                type="text"
                autocomplete="off"
                placeholder="请输入报警邮件地址，多个用逗号分隔"
                class="form-input"
              />
            </el-form-item>
          </div>
        </div>

        <!-- 执行配置区域 -->
        <div class="form-section">
          <div class="section-header">
            <el-icon><Setting /></el-icon>
            <span>执行配置</span>
          </div>
          <div class="form-grid">
            <el-form-item label="运行模式" prop="glueType" class="form-item">
              <el-select
                v-model="formData.glueType"
                filterable
                placeholder="请选择运行模式"
                class="form-select"
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
              class="form-item"
            >
              <el-button
                type="primary"
                @click="glueClick"
                class="glue-button"
                :icon="Edit"
              >
                GLUE IDE
              </el-button>
            </el-form-item>

            <el-form-item
              label="JobHandler"
              prop="executorHandler"
              v-if="formData.glueType == 'BEAN'"
              class="form-item full-width"
            >
              <el-input
                v-model="formData.executorHandler"
                type="text"
                autocomplete="off"
                placeholder="请输入JobHandler名称"
                :disabled="formData.jobType == 2"
                class="form-input"
              />
            </el-form-item>

            <el-form-item
              label="请求类型"
              prop="reqType"
              v-if="formData.glueType == 'API'"
              class="form-item"
            >
              <el-select v-model="formData.reqType" filterable class="form-select">
                <el-option key="GET" label="GET" value="GET" />
                <el-option key="POST" label="POST" value="POST" />
              </el-select>
            </el-form-item>

            <el-form-item
              label="数据库"
              v-if="formData.glueType == 'SQL'"
              class="form-item"
            >
              <el-select
                v-model="formData.jdbcDatasourceId"
                filterable
                class="form-select"
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
              class="form-item full-width"
            >
              <el-input
                v-model="formData.reqUrl"
                type="textarea"
                autocomplete="off"
                placeholder="请输入请求地址"
                :rows="3"
                class="form-textarea"
              />
            </el-form-item>

            <el-form-item
              label="请求头"
              prop="reqHeader"
              v-if="formData.glueType === 'API'"
              class="form-item full-width"
            >
              <EditTable
                :list="formData.reqHeader == null ? [] : JSON.parse(formData.reqHeader)"
                @handleTableData="handleTableData"
                class="edit-table"
              />
            </el-form-item>

            <el-form-item
              label="请求体"
              v-if="formData.glueType === 'API' && formData.reqType === 'POST'"
              class="form-item full-width"
            >
              <el-input
                v-model="formData.reqBody"
                type="textarea"
                autocomplete="off"
                placeholder="请输入请求体内容"
                :rows="4"
                class="form-textarea"
              />
            </el-form-item>

            <el-form-item
              :label="formData.glueType == 'SQL' ? 'SQL语句' : '任务参数'"
              v-if="formData.glueType !== 'API'"
              class="form-item full-width"
            >
              <el-input
                v-model="formData.executorParam"
                type="textarea"
                autocomplete="off"
                :placeholder="
                  formData.glueType == 'SQL' ? '请输入SQL语句' : '请输入任务参数'
                "
                :disabled="formData.jobType == 2"
                :rows="4"
                class="form-textarea"
              />
            </el-form-item>
          </div>
        </div>

        <!-- 高级配置区域 -->
        <div class="form-section">
          <div class="section-header">
            <el-icon><Tools /></el-icon>
            <span>高级配置</span>
          </div>
          <div class="form-grid">
            <el-form-item label="任务超时时间" class="form-item">
              <el-input
                v-model="formData.executorTimeout"
                type="text"
                autocomplete="off"
                placeholder="单位：秒"
                class="form-input"
              />
            </el-form-item>
            <el-form-item
              label="任务失败策略"
              prop="executorBlockStrategy"
              class="form-item"
            >
              <el-select
                v-model="formData.executorBlockStrategy"
                filterable
                placeholder="请选择失败策略"
                class="form-select"
              >
                <el-option
                  v-for="item in blockStrategyList"
                  :key="item.type"
                  :label="item.title"
                  :value="item.type"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="任务重试次数" class="form-item">
              <el-input-number
                v-model="formData.executorFailRetryCount"
                :min="0"
                :max="10"
                class="form-number"
              />
            </el-form-item>
          </div>
        </div>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="cancelClick" class="cancel-btn">取消</el-button>
        <el-button type="primary" @click="confirmClick" class="confirm-btn">
          {{ props.nodeJobId ? "保存" : "创建" }}
        </el-button>
      </div>
    </template>
  </el-dialog>

  <CodeEditor
    :glueTaskId="getGlueTaskId()"
    :glueVisible="glueVisible"
    :code="code"
    :nowDate="new Date()"
    @close="closeGlue"
  ></CodeEditor>
</template>

<script setup lang="ts">
import EditTable from "@/components/EditTable/EditTable.vue";
import JobGroupAPI from "@/api/job-group";
import JobInfoAPI from "@/api/job-info";
import { getThemeCode } from "@/utils/theme";
import CodeEditor from "@/components/CodeEdit/index.vue";
import JobJdbcDatasourceAPI from "@/api/job-jdbc-datasource";
import { ref, reactive, watch, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { useJobInfoStore, usePageStoreHook } from "@/store";
import { Document, Setting, Tools, Edit } from "@element-plus/icons-vue";

// 定义类型接口
interface TaskGroup {
  id: number;
  title: string;
}

interface JdbcDatasource {
  id: number;
  databaseName: string;
}

interface GlueType {
  type: string;
  title: string;
}

interface BlockStrategy {
  type: string;
  title: string;
}

const props = defineProps({
  jobNodeVisible: {
    type: Boolean,
    default: false,
  },
  nodeJobId: {
    type: Number,
    default: null,
  },
  node: {
    type: Object,
    default: null,
  },
  nowDate: {
    type: Date,
    default: null,
  },
});

const emit = defineEmits(["close"]);
const drawVisible = ref(false);
const formData = ref<any>({});
const taskGroupList = ref<TaskGroup[]>([]);
const jdbcDatasourceList = ref<JdbcDatasource[]>([]);

const rules = reactive({
  jobGroup: [{ required: true, message: "请选择执行器", trigger: "change" }],
  author: [{ required: true, message: "请输入负责人", trigger: "blur" }],
  jobDesc: [{ required: true, message: "请输入任务描述", trigger: "blur" }],
  glueType: [{ required: true, message: "请选择运行模式", trigger: "change" }],
  executorHandler: [{ required: true, message: "请输入JobHandler", trigger: "blur" }],
  reqType: [{ required: true, message: "请选择请求类型", trigger: "change" }],
  reqUrl: [{ required: true, message: "请输入请求地址", trigger: "blur" }],
  misfireStrategy: [{ required: true, message: "请选择调度过期策略", trigger: "change" }],
  executorBlockStrategy: [
    { required: true, message: "请选择任务失败策略", trigger: "change" },
  ],
});

const glueTypeList: GlueType[] = [
  { type: "BEAN", title: "BEAN" },
  { type: "API", title: "API" },
  { type: "SQL", title: "SQL" },
  { type: "GLUE_GROOVY", title: "GLUE(Java)" },
  { type: "GLUE_SHELL", title: "GLUE(Shell)" },
  { type: "GLUE_PYTHON", title: "GLUE(Python)" },
  { type: "GLUE_PHP", title: "GLUE(PHP)" },
  { type: "GLUE_NODEJS", title: "GLUE(Nodejs)" },
  { type: "GLUE_POWERSHELL", title: "GLUE(PowerShell)" },
];

const blockStrategyList: BlockStrategy[] = [
  { type: "SERIAL_EXECUTION", title: "单机串行" },
  { type: "DO_NOTHING", title: "忽略" },
];

watch(
  () => props.jobNodeVisible,
  (val) => {
    drawVisible.value = val;
  }
);

watch(
  () => props.nowDate,
  () => {
    if (props.nodeJobId) {
      getTaskInfo();
    } else {
      drawVisible.value = props.jobNodeVisible;
    }
  }
);

const glueVisible = ref(false);
const code = ref("");

// 获取GLUE任务ID，确保不为null
function getGlueTaskId(): number | undefined {
  // 如果是编辑模式且有nodeJobId，返回nodeJobId
  if (props.nodeJobId && props.nodeJobId > 0) {
    return props.nodeJobId;
  }

  // 如果是新增模式，返回undefined，让CodeEditor组件处理
  return undefined;
}

function glueClick() {
  glueVisible.value = true;
  const glueType = formData.value.glueType;

  // 只有在编辑模式下才获取现有代码
  if (props.nodeJobId && props.nodeJobId > 0) {
    JobInfoAPI.getFormData(props.nodeJobId)
      .then((data) => {
        if (data.glueSource == null || data.glueSource.length <= 0) {
          code.value = getThemeCode(glueType);
        } else {
          code.value = data.glueSource;
        }
      })
      .catch((error) => {
        console.error("获取GLUE代码失败:", error);
        code.value = getThemeCode(glueType);
      });
  } else {
    // 新增模式下直接使用默认代码模板
    code.value = getThemeCode(glueType);
  }
}

function handleChangeGlueType() {
  formData.value.executorHandler = "";
  formData.value.reqUrl = "";
  formData.value.reqHeader = null;
  formData.value.reqBody = "";
  formData.value.executorParam = "";
}

function closeGlue(code: string) {
  if (code !== "" && code.length > 0) {
    formData.value.glueSource = code;
  }
  glueVisible.value = false;
}

function handleTableData(val: any) {
  formData.value.reqHeader = JSON.stringify(val);
}

async function fetchJdbcDatasource() {
  const response = await JobJdbcDatasourceAPI.getJdbcDatasourceList();
  jdbcDatasourceList.value = (response as any).data || [];
}

function cancelClick() {
  emit("close", props.nodeJobId, formData.value.jobDesc, formData.value.glueType, 0);
}

function confirmClick() {
  if (formData.value.glueType == "SQL") {
    formData.value.executorHandler = "runJobJdbcXxlJob";
  }
  if (formData.value.glueType == "API") {
    formData.value.executorHandler = "runApiHandler";
  }
  formData.value.misfireStrategy = "DO_NOTHING";
  formData.value.scheduleType = "NONE";
  // 将glueType保存到properties中
  formData.value.properties = {
    ...formData.value.properties,
    glueType: formData.value.glueType,
  };

  console.log("确认编辑 - nodeJobId:", props.nodeJobId, "node:", props.node);

  if (props.nodeJobId && props.node) {
    // 编辑现有任务
    console.log("编辑现有任务，nodeJobId:", props.nodeJobId);
    JobInfoAPI.update(props.nodeJobId, formData.value)
      .then(() => {
        ElMessage.success("修改成功");
        drawVisible.value = false;
        // 触发树刷新事件
        useJobInfoStore().triggerTreeRefresh();
        emit(
          "close",
          props.nodeJobId,
          formData.value.jobDesc,
          formData.value.glueType,
          1
        ); // 传递任务ID而不是节点ID
      })
      .finally(() => {});
  } else if (props.nodeJobId) {
    // 只有nodeJobId，没有node，仍然是编辑现有任务
    console.log("编辑现有任务（无node对象），nodeJobId:", props.nodeJobId);
    JobInfoAPI.update(props.nodeJobId, formData.value)
      .then(() => {
        ElMessage.success("修改成功");
        drawVisible.value = false;
        // 触发树刷新事件
        useJobInfoStore().triggerTreeRefresh();
        emit(
          "close",
          props.nodeJobId,
          formData.value.jobDesc,
          formData.value.glueType,
          1
        );
      })
      .finally(() => {});
  } else if (props.node) {
    // 新增任务
    console.log("新增任务，node:", props.node);
    formData.value.jobId = props.nodeJobId;
    formData.value.parentId = props.node.id;
    console.log("新增任务数据:", formData.value);
    useJobInfoStore().setJobInfo(formData.value);
    // 触发树刷新事件
    useJobInfoStore().triggerTreeRefresh();
    formData.value = {};
    drawVisible.value = false;
    usePageStoreHook().addPage(props.node);
  } else {
    console.error("无法确定操作类型：nodeJobId和node都为空");
    ElMessage.error("无法确定操作类型，请重新尝试");
  }
}

async function fetchTaskGroupList() {
  const response = await JobGroupAPI.getAllJobGroupList();
  taskGroupList.value = (response as any) || [];
}

async function getTaskInfo() {
  if (props.nodeJobId) {
    JobInfoAPI.getFormData(props.nodeJobId).then((data) => {
      Object.assign(formData.value, data);
    });
  }
}

onMounted(() => {
  fetchTaskGroupList();
  fetchJdbcDatasource();
});
</script>

<style scoped lang="scss">
.job-node-dialog.compact {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 8px 8px 0 0;
    padding: 10px 16px;
    .el-dialog__title {
      font-size: 14px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }
    .el-dialog__headerbtn .el-dialog__close {
      color: white;
      font-size: 15px;
      &:hover {
        color: #f0f0f0;
      }
    }
  }
  :deep(.el-dialog__body) {
    padding: 0;
  }
  :deep(.el-dialog__footer) {
    border-top: 1px solid #f4f4f4;
    padding: 8px 16px;
  }
}
.dialog-content {
  padding: 10px 14px;
  max-height: 56vh;
  overflow-y: auto;
}
.job-form {
  text-align: left;
  .el-form-item {
    justify-content: flex-start;
    align-items: flex-start;
    .el-form-item__label {
      text-align: left;
      justify-content: flex-start;
      align-items: flex-start;
      padding-left: 0;
    }
    .el-form-item__content {
      text-align: left;
      justify-content: flex-start;
      align-items: flex-start;
    }
  }
  .form-section {
    margin-bottom: 12px;
  }
  .section-header {
    display: flex;
    align-items: center;
    margin-bottom: 6px;
    padding-bottom: 3px;
    border-bottom: 1px solid #f4f4f4;
    .el-icon {
      margin-right: 5px;
      color: #409eff;
      font-size: 14px;
    }
    span {
      font-size: 12.5px;
      font-weight: 600;
      color: #333;
    }
  }
  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 10px;
    .form-item {
      margin-bottom: 6px;
      &.full-width {
        grid-column: 1 / -1;
      }
      :deep(.el-form-item__label) {
        font-weight: 500;
        color: #888;
        font-size: 12.5px;
        padding-right: 2px;
        .el-form-item__required {
          color: #ffb300;
          margin-right: 2px;
          font-size: 12px;
        }
      }
      :deep(.el-form-item__content) {
        .form-input,
        .form-select,
        .form-textarea,
        .form-number {
          width: 100%;
          font-size: 12.5px;
        }
        .form-textarea {
          :deep(.el-textarea__inner) {
            border-radius: 3px;
            border: 1px solid #e0e0e0;
            min-height: 28px;
            font-size: 12.5px;
            padding: 3px 7px;
            color: #333;
            background: #fafbfc;
            transition: all 0.2s;
            &::placeholder {
              color: #c2c2c2;
              font-weight: 400;
              font-size: 12px;
              opacity: 0.9;
            }
            &:focus {
              border-color: #409eff;
              box-shadow: 0 0 0 1px #e3f0fc;
              background: #fff;
            }
          }
        }
        .form-input,
        .form-select {
          :deep(.el-input__wrapper) {
            border-radius: 3px;
            border: 1px solid #e0e0e0;
            min-height: 24px;
            font-size: 12.5px;
            padding: 0 7px;
            background: #fafbfc;
            &.is-focus {
              border-color: #409eff;
              box-shadow: 0 0 0 1px #e3f0fc;
            }
            &:hover {
              border-color: #bfc6d1;
            }
          }
          :deep(.el-input__inner) {
            color: #333;
            &::placeholder {
              color: #c2c2c2;
              font-weight: 400;
            }
          }
        }
        .form-number {
          :deep(.el-input-number__decrease),
          :deep(.el-input-number__increase) {
            border-radius: 3px 0 0 3px;
          }
          :deep(.el-input__wrapper) {
            border-radius: 0 3px 3px 0;
          }
        }
      }
    }
  }
}
.glue-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 3px;
  padding: 5px 12px;
  font-weight: 500;
  font-size: 12.5px;
  transition: all 0.3s;
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.13);
  }
}
.edit-table {
  border: 1px solid #e4e7ed;
  border-radius: 3px;
  overflow: hidden;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  .cancel-btn,
  .confirm-btn {
    border-radius: 3px;
    padding: 5px 14px;
    font-weight: 500;
    font-size: 12.5px;
    transition: all 0.3s;
  }
  .cancel-btn:hover {
    background: #f5f7fa;
    border-color: #c0c4cc;
  }
  .confirm-btn {
    background: linear-gradient(135deg, #409eff 0%, #36a3f7 100%);
    border: none;
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.13);
    }
  }
}
@media (max-width: 768px) {
  .job-node-dialog.compact :deep(.el-dialog) {
    width: 99% !important;
    margin: 2vh auto;
  }
  .dialog-content {
    padding: 6px 2px;
  }
  .job-form .form-grid {
    grid-template-columns: 1fr;
    gap: 6px;
  }
  .dialog-footer {
    flex-direction: column;
    .cancel-btn,
    .confirm-btn {
      width: 100%;
    }
  }
}
.dialog-content::-webkit-scrollbar {
  width: 3px;
}
.dialog-content::-webkit-scrollbar-track {
  background: #f6f6f6;
  border-radius: 2px;
}
.dialog-content::-webkit-scrollbar-thumb {
  background: #d1d1d1;
  border-radius: 2px;
}
</style>
