package com.cc.job.test;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.CcJobApplication;
import com.cc.job.admin.task.command.JdbcCommand;
import com.cc.job.admin.task.service.*;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.vo.JobPartVo;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest(classes = CcJobApplication.class,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CcJobApplicationTest {

    @Autowired
    private JobJdbcDatasourceService jobJdbcDatasourceService;

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private JobEdgeService jobEdgeService;

    @Resource
    private JobNodeService jobNodeService;

    @Test
    public void test1() {
        JdbcCommand jdbcCommand = new JdbcCommand("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@//127.0.0.1:1521/helowin", "system", "root", "DT");
        Connection con = null;
        try {
            con = jdbcCommand.getConnection();
            System.out.println(con);
        } catch (Exception e) {
            throw new BusinessException(e);
        } finally {
            JdbcCommand.close(con);
        }
    }


    @Test
    public void test2() {
        // copy jobgroup
        JobInfo soureJobInfo = jobInfoService.getById(789);
        System.out.println(soureJobInfo);
        List<JobInfo> childrenJobInfo = jobInfoService.list(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getParentId, 789));
        System.out.println(childrenJobInfo);

        List<JobNode> jobNodes = jobNodeService.list(new LambdaQueryWrapper<JobNode>().eq(JobNode::getJobParentId, 789));
        Map<Long, JobNode> jobNodeMap = jobNodes.stream().collect(Collectors.toMap(JobNode::getJobId, n -> n));


        // 复制jobNode
        List<JobEdge> jobEdges = jobEdgeService.list(new LambdaQueryWrapper<JobEdge>().eq(JobEdge::getJobParentId, 789));
        System.out.println(jobEdges);

        try {
            for (int i = 2; i < 1000; i++) {
                //copy 100 jobgroup
                JobInfo jobInfo = new JobInfo();
                BeanUtils.copyProperties(soureJobInfo, jobInfo, "id");
                jobInfo.setJobDesc("demojob" + (i + 1));
                jobInfoService.save(jobInfo);
                jobInfo.setExecutorParam(String.valueOf(jobInfo.getId()));
                jobInfoService.updateById(jobInfo);

                //copy jobchildrens
                List<Pair<Long, Long>> childrenIds = new ArrayList<>();
                for (JobInfo info : childrenJobInfo) {
                    JobInfo children = new JobInfo();
                    BeanUtils.copyProperties(info, children, "id");
                    children.setParentId(jobInfo.getId());
                    jobInfoService.save(children);
                    childrenIds.add(new Pair<>(info.getId(), children.getId()));
                }


                List<Pair<Long, Long>> nodeIds = new ArrayList<>();
                //copy nodes
                for (Pair<Long, Long> childrenId : childrenIds) {
                    Long jobId = childrenId.getKey();
                    JobNode jobNode = jobNodeMap.get(jobId);
                    Map<String, Object> propertiesMap = JSONUtil.toBean(jobNode.getProperties(), Map.class);
                    JobNode newJobNode = new JobNode();
                    BeanUtils.copyProperties(jobNode, newJobNode, "id");
                    newJobNode.setJobParentId(jobInfo.getId());
                    newJobNode.setJobId(childrenId.getValue());
                    propertiesMap.put("jobId", childrenId.getValue());
                    newJobNode.setProperties(JSONUtil.toJsonStr(propertiesMap));
                    jobNodeService.save(newJobNode);
                    nodeIds.add(new Pair<>(jobNode.getId(), newJobNode.getId()));
                }

                for (JobEdge jobEdge : jobEdges) {
                    JobEdge newJobEdge = new JobEdge();
                    BeanUtils.copyProperties(jobEdge, newJobEdge, "id");
                    newJobEdge.setJobParentId(jobInfo.getId());
                    Long fromNodeId = jobEdge.getFromNodeId();
                    Long endNodeId = jobEdge.getEndNodeId();

                    Pair<Long, Long> fromPair = nodeIds.stream().filter(v -> v.getKey().equals(fromNodeId)).findFirst().orElseThrow();
                    Pair<Long, Long> endPair = nodeIds.stream().filter(v -> v.getKey().equals(endNodeId)).findFirst().orElseThrow();

                    newJobEdge.setFromNodeId(fromPair.getValue());
                    newJobEdge.setEndNodeId(endPair.getValue());
                    jobEdgeService.save(newJobEdge);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test3() {
        JobInfo jobInfo = jobInfoService.getById(33);
        for (int i = 0; i < 10000; i++) {
            JobInfo jobInfo1 = new JobInfo();
            BeanUtils.copyProperties(jobInfo, jobInfo1, "id");
            jobInfo1.setScheduleConf("0 * * * * ? *");
            jobInfoService.save(jobInfo1);
        }
    }

    @Test
    public void test4() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        Map<Integer,Integer> ma = new HashMap<>();
        System.out.println(ma.entrySet());
    }

    @Autowired
    JobPartService jobPartService;

    private static final String SECRET_KEY = "1234567890abcdef"; // 256-bit key
    private static final String ALGORITHM = "AES";
    @Test
    public void test5() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        //byte[] encryptedData = Files.readAllBytes(Paths.get("/Users/zhaowenpeng/Downloads/encryptedData (3).cetl"));

        File file = new File("/Users/zhaowenpeng/Downloads/encryptedData (3).cetl");
        FileInputStream fis = new FileInputStream(file);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        // 3. 执行解密
        byte[] decryptedBytes = cipher.doFinal(fis.readAllBytes());
        String json = new String(decryptedBytes, "UTF-8");
        System.out.println(json);
    }
}




