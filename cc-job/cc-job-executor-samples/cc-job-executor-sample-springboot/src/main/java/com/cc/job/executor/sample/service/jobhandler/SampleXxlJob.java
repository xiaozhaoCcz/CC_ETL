package com.cc.job.executor.sample.service.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 *      1、任务开发：在Spring Bean实例中，开发Job方法；
 *      2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 *      3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 *      4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
public class SampleXxlJob {
    private static Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);


    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("demoJobHandler1")
    public void demoJobHandler() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler1 start");
        System.out.println(">>>>>>>> demoJobHandler1 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler1 beat at:" + i);
            System.out.println("demoJobHandler1 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
         //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler1 end");
        System.out.println(">>>>>>>> demoJobHandler1 end");
    }


    @XxlJob("demoJobHandler2")
    public void demoJobHandler2() throws Exception {
        XxlJobHelper.log(">>>>>>>> demoJobHandler2 start");
        System.out.println(">>>>>>>> demoJobHandler2 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler2 beat at:" + i);
            System.out.println("demoJobHandler2 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //throw  new RuntimeException();
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler2 end");
        System.out.println(">>>>>>>> demoJobHandler2 end");
    }

    static  int retryCount = 0;

    @XxlJob("demoJobHandler3")
    public void demoJobHandler3() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler3 start");
        System.out.println(">>>>>>>> demoJobHandler3 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler3 beat at:" + i);
            System.out.println("demoJobHandler3 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
//        if(retryCount<2){
//            retryCount++;
            //throw new RuntimeException();
       // }
        XxlJobHelper.log(">>>>>>>> demoJobHandler3 end");
        System.out.println(">>>>>>>> demoJobHandler3 end");
    }

    @XxlJob("demoJobHandler4")
    public void demoJobHandler4() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler4 start");
        System.out.println(">>>>>>>> demoJobHandler1 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler4 beat at:" + i);
            System.out.println("demoJobHandler4 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler4 end");
        System.out.println(">>>>>>>> demoJobHandler4 end");
    }



    @XxlJob("demoJobHandler5")
    public void demoJobHandler5() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler5 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler5 beat at:" + i);
            System.out.println("demoJobHandler5 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler5 end");
    }

    @XxlJob("demoJobHandler6")
    public void demoJobHandler6() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler6 start");
        System.out.println(">>>>>>>> demoJobHandler6 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler6 beat at:" + i);
            System.out.println("demoJobHandler6 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler6 end");
        System.out.println(">>>>>>>> demoJobHandler6 end");
    }

    @XxlJob("demoJobHandler7")
    public void demoJobHandler7() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler7 start");
        System.out.println(">>>>>>>> demoJobHandler7 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler7 beat at:" + i);
            System.out.println("demoJobHandler7 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler7 end");
        System.out.println(">>>>>>>> demoJobHandler7 end");
    }

    @XxlJob("demoJobHandler8")
    public void demoJobHandler8() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler8 start");
        System.out.println(">>>>>>>> demoJobHandler8 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler8 beat at:" + i);
            System.out.println("demoJobHandler8 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler8 end");
        System.out.println(">>>>>>>> demoJobHandler8 end");
    }

    @XxlJob("demoJobHandler9")
    public void demoJobHandler9() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler9 start");
        System.out.println(">>>>>>>> demoJobHandler9 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler9 beat at:" + i);
            System.out.println("demoJobHandler9 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler9 end");
        System.out.println(">>>>>>>> demoJobHandler9 end");
    }

    @XxlJob("demoJobHandler10")
    public void demoJobHandler10() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler10 start");
        System.out.println(">>>>>>>> demoJobHandler10 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler10 beat at:" + i);
            System.out.println("demoJobHandler10 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler10 end");
        System.out.println(">>>>>>>> demoJobHandler10 end");
    }
    @XxlJob("demoJobHandler11")
    public void demoJobHandler11() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler11 start");
        System.out.println(">>>>>>>> demoJobHandler11 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler11 beat at:" + i);
            System.out.println("demoJobHandler11 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler11 end");
        System.out.println(">>>>>>>> demoJobHandler11 end");
    }

    @XxlJob("demoJobHandler12")
    public void demoJobHandler12() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler12 start");
        System.out.println(">>>>>>>> demoJobHandler12 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler12 beat at:" + i);
            System.out.println("demoJobHandler12 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler12 end");
        System.out.println(">>>>>>>> demoJobHandler12 end");
//        if(count.incrementAndGet()<=3){
//            System.out.println(">>>>>>>>count"+count.get());
//            throw  new RuntimeException();
//        }
        //throw  new RuntimeException("节点12运行失败");
    }

    @XxlJob("demoJobHandler13")
    public void demoJobHandler13() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler13 start");
        System.out.println(">>>>>>>> demoJobHandler13 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler13 beat at:" + i);
            System.out.println("demoJobHandler13 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler13 end");
        System.out.println(">>>>>>>> demoJobHandler13 end");
    }

    @XxlJob("demoJobHandler14")
    public void demoJobHandler14() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler14 start");
        System.out.println(">>>>>>>> demoJobHandler14 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler14 beat at:" + i);
            System.out.println("demoJobHandler14 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler14 end");
        System.out.println(">>>>>>>> demoJobHandler14 end");
    }

    @XxlJob("demoJobHandler15")
    public void demoJobHandler15() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler15 start");
        System.out.println(">>>>>>>> demoJobHandler15 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler15 beat at:" + i);
            System.out.println("demoJobHandler15 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler15 end");
        System.out.println(">>>>>>>> demoJobHandler15 end");
    }

    @XxlJob("demoJobHandler16")
    public void demoJobHandler16() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler16 start");
        System.out.println(">>>>>>>> demoJobHandler16 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler16 beat at:" + i);
            System.out.println("demoJobHandler16beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler16 end");
        System.out.println(">>>>>>>> demoJobHandler16 end");
    }

    @XxlJob("demoJobHandler17")
    public void demoJobHandler17() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler17 start");
        System.out.println(">>>>>>>> demoJobHandler17 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler17 beat at:" + i);
            System.out.println("demoJobHandler17 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler17 end");
        System.out.println(">>>>>>>> demoJobHandler17 end");
    }

    @XxlJob("demoJobHandler18")
    public void demoJobHandler18() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler18 start");
        System.out.println(">>>>>>>> demoJobHandler18 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler18 beat at:" + i);
            System.out.println("demoJobHandler18 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        throw new RuntimeException("节点18运行失败");
        //default success
//        XxlJobHelper.log(">>>>>>>> demoJobHandler18 end");
//        System.out.println(">>>>>>>> demoJobHandler18 end");
    }

    @XxlJob("demoJobHandler19")
    public void demoJobHandler19() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler19 start");
        System.out.println(">>>>>>>> demoJobHandler19 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler19 beat at:" + i);
            System.out.println("demoJobHandler19 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler19 end");
        System.out.println(">>>>>>>> demoJobHandler19 end");
    }

    @XxlJob("demoJobHandler20")
    public void demoJobHandler20() throws Exception {

        XxlJobHelper.log(">>>>>>>> demoJobHandler20 start");
        System.out.println(">>>>>>>> demoJobHandler20 start");

        for (int i = 0; i < 10; i++) {
            XxlJobHelper.log("demoJobHandler20 beat at:" + i);
            System.out.println("demoJobHandler20 beat at:" + i);
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //default success
        XxlJobHelper.log(">>>>>>>> demoJobHandler20 end");
        System.out.println(">>>>>>>> demoJobHandler20 end");
    }



    /**
     * 2、分片广播任务
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 业务逻辑
        for (int i = 0; i < shardTotal; i++) {
            if (i == shardIndex) {
                XxlJobHelper.log("第 {} 片, 命中分片开始处理", i);
            } else {
                XxlJobHelper.log("第 {} 片, 忽略", i);
            }
        }

    }


    /**
     * 3、命令行任务
     */
    @XxlJob("commandJobHandler")
    public void commandJobHandler() throws Exception {
        String command = XxlJobHelper.getJobParam();
        int exitValue = -1;

        BufferedReader bufferedReader = null;
        try {
            // command process
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            //Process process = Runtime.getRuntime().exec(command);

            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));

            // command log
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                XxlJobHelper.log(line);
            }

            // command exit
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            XxlJobHelper.log(e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        if (exitValue == 0) {
            // default success
        } else {
            XxlJobHelper.handleFail("command exit value("+exitValue+") is failed");
        }

    }


    /**
     * 4、跨平台Http任务
     *  参数示例：
     *      "url: http://www.baidu.com\n" +
     *      "method: get\n" +
     *      "data: content\n";
     */
    @XxlJob("httpJobHandler")
    public void httpJobHandler() throws Exception {

        // param parse
        String param = XxlJobHelper.getJobParam();
        if (param==null || param.trim().length()==0) {
            XxlJobHelper.log("param["+ param +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }

        String[] httpParams = param.split("\n");
        String url = null;
        String method = null;
        String data = null;
        for (String httpParam: httpParams) {
            if (httpParam.startsWith("url:")) {
                url = httpParam.substring(httpParam.indexOf("url:") + 4).trim();
            }
            if (httpParam.startsWith("method:")) {
                method = httpParam.substring(httpParam.indexOf("method:") + 7).trim().toUpperCase();
            }
            if (httpParam.startsWith("data:")) {
                data = httpParam.substring(httpParam.indexOf("data:") + 5).trim();
            }
        }

        // param valid
        if (url==null || url.trim().length()==0) {
            XxlJobHelper.log("url["+ url +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }
        if (method==null || !Arrays.asList("GET", "POST").contains(method)) {
            XxlJobHelper.log("method["+ method +"] invalid.");

            XxlJobHelper.handleFail();
            return;
        }
        boolean isPostMethod = method.equals("POST");

        // request
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            // connection
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // connection setting
            connection.setRequestMethod(method);
            connection.setDoOutput(isPostMethod);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(5 * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            // do connection
            connection.connect();

            // data
            if (isPostMethod && data!=null && data.trim().length()>0) {
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(data.getBytes("UTF-8"));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            // valid StatusCode
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                throw new RuntimeException("Http Request StatusCode(" + statusCode + ") Invalid.");
            }

            // result
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String responseMsg = result.toString();

            XxlJobHelper.log(responseMsg);

            return;
        } catch (Exception e) {
            XxlJobHelper.log(e);

            XxlJobHelper.handleFail();
            return;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                XxlJobHelper.log(e2);
            }
        }

    }

    /**
     * 5、生命周期任务示例：任务初始化与销毁时，支持自定义相关逻辑；
     */
    @XxlJob(value = "demoJobHandler21", init = "init", destroy = "destroy")
    public void demoJobHandler21() throws Exception {
        System.out.println("demoJobHandler2 run.....");
    }
    public void init(){
        logger.info("init");
    }
    public void destroy(){
        logger.info("destroy");
    }


}
