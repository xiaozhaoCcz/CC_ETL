import request from "@/utils/request";

const JOB_PART_BASE_URL = "/api/v1/jobParts";

const JobPartAPI ={

    getTree() {
        return request({
            url: `${JOB_PART_BASE_URL}/getTree`,
            method: "get",
        });
    },
    saveJobPart(data: any) {
        return request({
            url: `${JOB_PART_BASE_URL}/saveJobPart`,
            method: "post",
            data: data,
        });
    },
    updateJobPart(data: any) {
        return request({
            url: `${JOB_PART_BASE_URL}/updateJobPart`,
            method: "post",
            data: data,
        });
    },
    deleteJobPart(id: number) {
        return request({
            url: `${JOB_PART_BASE_URL}/deleteJobPart/${id}`,
            method: "get",
        });
    },
    exportData(id: number) {
        return request({
            url: `${JOB_PART_BASE_URL}/exportData/${id}`,
            method: "get",
            responseType: 'arraybuffer' // 重要：接收二进制数据
        });
    },
    importData(formData: any) {
        return request({
            url: `${JOB_PART_BASE_URL}/importData`,
            method: "post",
            data: formData,
            headers:{
                'Content-Type': 'multipart/form-data'
            }
        });
    },
}

export default JobPartAPI;