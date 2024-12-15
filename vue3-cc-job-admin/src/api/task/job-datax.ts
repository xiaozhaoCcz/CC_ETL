import request from "@/utils/request";

const JOB_DATAX_BASE_URL = "/api/v1/datax";

const JobDataXAPI = {
  getTables(id: number) {
    return request({
      url: `${JOB_DATAX_BASE_URL}/getTables/${id}`,
      method: "get",
    });
  },

  getColumns(id: number, data: any) {
    return request({
      url: `${JOB_DATAX_BASE_URL}/getColumns/${id}`,
      method: "post",
      data: data,
    });
  },

  getJson(data: any) {
    return request({
      url: `${JOB_DATAX_BASE_URL}/getJson`,
      method: "post",
      data: data,
    });
  },
};

export default JobDataXAPI;
