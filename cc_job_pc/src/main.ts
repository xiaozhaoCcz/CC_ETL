import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";

const app = createApp(App);

import router from "./router/index";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import { InstallCodeMirror } from "codemirror-editor-vue3";

app.use(router);
app.use(ElementPlus);
app.use(InstallCodeMirror);

app.mount("#app");
