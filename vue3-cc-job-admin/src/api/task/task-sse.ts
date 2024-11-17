import request from "@/utils/request";

const SSE_BASE_URL = "/api/v1/sse";

const TASK_SSE_API = {
  createConnect(id: number) {
    return request({
      url: `${SSE_BASE_URL}/createConnect/${id}`,
      method: "get",
    });
  },

  closeConnect(id: number) {
    return request({
      url: `${SSE_BASE_URL}/closeConnect/${id}`,
      method: "get",
    });
  },
}

export default TASK_SSE_API;
