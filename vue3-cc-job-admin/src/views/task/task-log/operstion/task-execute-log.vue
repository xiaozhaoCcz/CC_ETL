<template>
  <div class="app-container">
    <el-dialog
      v-model="props.executeLogVisable"
      title="调度备注"
      :before-close="handleClose"
      width="850px"
    >
      <Codemirror
        v-model:value="execLog"
        :options="cmOptions"
        height="800"
        :KeepCursorInEnd="true"
      ></Codemirror>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import JobLogAPI from "@/api/task/job-log";

const emit = defineEmits(["close"]);
const props = defineProps({
  taskLogId: {
    type: Number,
    default: 0,
  },
  executeLogVisable: {
    type: Boolean,
    default: false,
  },
});

const fromLineNum = ref(0);
const execLog = ref("");
const pullFailCount = ref(0);
let logRun: any = null;
const logContainer = ref();

const cmOptions = {
  mode: "log",
  theme: "default",
};

watch(
  () => props.executeLogVisable,
  () => {}
);

watch(
  () => props.taskLogId,
  (id) => {
    if (id) {
      run(id);
    }
  }
);

watch(
  () => execLog,
  () => {
    // 使用nextTick确保DOM更新完成
    nextTick(() => {
      logContainer.value.scrollTop = logContainer.value.scrollHeight;
    });
  }
);

function convertContent(str: string) {
  return str.replace( /&amp;/g,'&')
    .replace(/&lt;/g,'<')
    .replace( /&gt;/g,'>')
    .replace( /&quot;/g,"'")
    .replace(/&#39;/g,"'")
    .replace(/&quot;/g, '"')
}

function run(id: number) {
  logRun = setInterval(() => {
    getExecuteTaskLog(id);
  }, 2000);
}

function logRunStop(content: string) {
  if (logRun != null) {
    window.clearInterval(logRun);
    logRun = null;
    execLog.value += convertContent(content);
  }
}

function getExecuteTaskLog(id: number) {
  if (pullFailCount.value++ > 20) {
    logRunStop("日志加载完成.....");
    return;
  }

  JobLogAPI.logDetailCat(id, fromLineNum.value).then((data: any) => {
    if (data.code == 200) {
      if (!data.content) {
        console.log("pullLog fail");
        return;
      }
      if (fromLineNum.value != data.content.fromLineNum) {
        console.log("pullLog fromLineNum not match");
        return;
      }
      if (fromLineNum.value > data.content.toLineNum) {
        console.log("pullLog already line-end");

        // valid end
        if (data.content.end) {
          logRunStop("[Rolling Log Finish]");
          return;
        }
        return;
      }

      // append content
      fromLineNum.value = data.content.toLineNum + 1;

      execLog.value += convertContent(data.content.logContent);

      pullFailCount.value = 0;
    } else {
      console.log("pullLog fail:" + data.msg);
    }
  });
}

function handleClose() {
  fromLineNum.value = 0;
  execLog.value = "";
  pullFailCount.value = 0;
  logRunStop(logRun);
  emit("close");
}
</script>
