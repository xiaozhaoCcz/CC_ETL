package com.cc.job.executor.sample.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.cc.job.datax.DataxJobExecutorApplication;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

@SpringBootTest(classes = DataxJobExecutorApplication.class)
public class XxlJobExecutorExampleBootApplicationTests {

	// datax测试
	@Test
	public void test() throws InterruptedException, IOException {
		String json = """
                {
                    "job": {
                        "setting": {
                            "speed": {
                                "byte":10485760
                            },
                            "errorLimit": {
                                "record": 0,
                                "percentage": 0.02
                            }
                        },
                        "content": [
                            {
                                "reader": {
                                    "name": "streamreader",
                                    "parameter": {
                                        "column" : [
                                            {
                                                "value": "DataX",
                                                "type": "string"
                                            },
                                            {
                                                "value": 19890604,
                                                "type": "long"
                                            },
                                            {
                                                "value": "1989-06-04 00:00:00",
                                                "type": "date"
                                            },
                                            {
                                                "value": true,
                                                "type": "bool"
                                            },
                                            {
                                                "value": "test",
                                                "type": "bytes"
                                            }
                                        ],
                                        "sliceRecordCount": 100000
                                    }
                                },
                                "writer": {
                                    "name": "streamwriter",
                                    "parameter": {
                                        "print": false,
                                        "encoding": "UTF-8"
                                    }
                                }
                            }
                        ]
                    }
                }""";

		String temJsonFile = generateTemJsonFile(json);
		ProcessBuilder processBuilder = new ProcessBuilder("python", "/Users/zhaowenpeng/Downloads/datax/bin/datax.py", temJsonFile);
		processBuilder.inheritIO();
		Process process = processBuilder.start();
		process.waitFor();
	}

	private String generateTemJsonFile(String jobJson) {
		String tmpFilePath;
		String dataXHomePath = "/Users/zhaowenpeng/Downloads/datax/";
		String jsonPath = "./";
		if (StringUtils.isNotEmpty(dataXHomePath)) {
			jsonPath = dataXHomePath + "jsons";
		}
		if (!FileUtil.exist(jsonPath)) {
			FileUtil.mkdir(jsonPath);
		}
		tmpFilePath = jsonPath + "jobTmp-" + IdUtil.simpleUUID() + ".conf";
		// 根据json写入到临时本地文件
		try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
			writer.println(jobJson);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			 e.printStackTrace();
		}
		return tmpFilePath;
	}


	@Test
	public void test2(){
//		DataXParams dataXParams = new DataXParams();
//		dataXParams.setName("mysqlreader");
//		dataXParams.setIp("localhost");
//		dataXParams.setPort(3306);
//		dataXParams.setDbName("testDbName");
//		dataXParams.setColumns(new ArrayList<>(){{
//			add("id");
//			add("username");
//		}});
//		dataXParams.setUsername("root");
//		dataXParams.setPassword("root");
//		dataXParams.setSourceType("MYSQL");
//		dataXParams.setTableName("yanhuo");
//		dataXParams.setOtherParams(new HashMap<>(){{
//			put("where","id>10");
//		}});
//		MysqlReader mysqlReader = new MysqlReader();
//		JSONObject entries = mysqlReader.buildJson(dataXParams);
//		System.out.println(entries.toString());
	}
}