import { createRouter, createWebHashHistory } from "vue-router";

export const routes = [
  {
    path: "/",
    name: "/",
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        component: () => import(/* webpackChunkName: "dashboard" */ "@/views/dashboard/dashboard.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "Dashboard",
        meta: {
          title: "dashboard",
          icon: "homepage",
          affix: true,
          keepAlive: true,
          // 添加预加载配置
          preload: true,
        },
      },
    ]
  },
];

const router = createRouter({
  scrollBehavior: () => ({ left: 0, top: 0 }),
  history: createWebHashHistory(),
  routes,
});

// 路由守卫优化
router.beforeEach((to, from, next) => {
  // 添加路由切换性能监控
  const startTime = performance.now();

  next();

  // 记录路由切换时间
  setTimeout(() => {
    const endTime = performance.now();
    console.log(`路由切换耗时: ${endTime - startTime}ms`);
  }, 0);
});

// 路由后置守卫 - 预加载优化
router.afterEach((to) => {
  // 预加载相关路由
  if (to.meta.preload) {
    const nextRoute = routes[0].children?.find(route => route.name !== to.name);
    if (nextRoute) {
      // 预加载下一个可能访问的路由
      import(/* webpackChunkName: "dashboard" */ "@/views/dashboard/dashboard.vue");
    }
  }
});

export default router;
