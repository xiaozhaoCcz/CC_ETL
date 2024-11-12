package com.xxl.job.core.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

public class JobCallBackThread {
    private static Logger logger = LoggerFactory.getLogger(JobCallBackThread.class);


    private static JobCallBackThread instance = new JobCallBackThread();

    private LinkedBlockingQueue<Long> callBackQueue = new LinkedBlockingQueue<Long>();

    public static Vector<Long> vector = new Vector<Long>();

    public static JobCallBackThread getInstance(){
        return instance;
    }

    public static void pushCallBack(Long callback){
        getInstance().callBackQueue.add(callback);
        logger.info("push callback request, logId:{}", callback);
    }

    private Thread callBackThread = null;

    private volatile boolean toStop = false;
    public void start() {
        callBackThread = new Thread(()->{
            while(!toStop){
                try {
                    Long callback = getInstance().callBackQueue.take();
                    if (callback != null) {

                        // callback list param
                        List<Long> callbackParamList = new ArrayList<Long>();
                        int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                        callbackParamList.add(callback);

                        // callback, will retry if error
                        if (callbackParamList!=null && callbackParamList.size()>0) {
                            doCallback(callbackParamList);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });

        logger.info(">>>>>>>>>>>> callBackThread start ");
        callBackThread.start();
    }

    private void doCallback(List<Long> callbackParamList) {

        for (Long aLong : callbackParamList) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            vector.add(aLong);
        }
    }


}
