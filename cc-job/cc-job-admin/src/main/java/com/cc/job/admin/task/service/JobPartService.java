package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobPart;
import com.cc.job.xo.model.vo.JobPartVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface JobPartService extends IService<JobPart> {

    List<JobPartVo> getTree();

    Object getChildren(Long id, Integer type);

    void delete(Long id);

    byte[] exportData(Long id);

    void importData(MultipartFile file);
}
