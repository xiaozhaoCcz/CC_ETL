package com.cc.job.admin.task.thread;

import com.cc.job.admin.task.complete.XxlJobCompleter;
import com.cc.job.admin.config.XxlJobAdminConfig;
import com.cc.job.admin.task.validation.DataQualityValidationRunner;
import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.xo.model.entity.JobValidation;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.admin.task.utils.I18nUtil;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * job lose-monitor instance
 *
 * @author xuxueli 2015-9-1 18:05:56
 */
public class JobCompleteHelper {
	private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);

	private static JobCompleteHelper instance = new JobCompleteHelper();

	public static JobCompleteHelper getInstance() {
		return instance;
	}

	// ---------------------- monitor ----------------------

	private ThreadPoolExecutor callbackThreadPool = null;
	private Thread monitorThread;
	private volatile boolean toStop = false;

	public void start() {
		// for callback
		callbackThreadPool = new ThreadPoolExecutor(
				2,
				20,
				30L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(3000),
				new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, "xxl-job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode());
					}
				},
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						r.run();
						logger.warn(">>>>>>>>>>> xxl-job, callback too fast, match threadpool rejected handler(run now).");
					}
				});


		// for monitor
		monitorThread = new Thread(new Runnable() {

			@Override
			public void run() {

				// wait for JobTriggerPoolHelper-init
				try {
					TimeUnit.MILLISECONDS.sleep(50);
				} catch (InterruptedException e) {
					if (!toStop) {
						logger.error(e.getMessage(), e);
					}
				}

				// monitor
				while (!toStop) {
					try {
						// 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
						Date losedTime = DateUtil.addMinutes(new Date(), -10);
						List<Long> losedJobIds = XxlJobAdminConfig.getAdminConfig().getJobLogMapper().findLostJobIds(losedTime);

						if (losedJobIds != null && losedJobIds.size() > 0) {
							for (Long logId : losedJobIds) {

								JobLog jobLog = new JobLog();
								jobLog.setId(logId);

								jobLog.setHandleTime(LocalDateTime.now());
								jobLog.setHandleCode(ReturnT.FAIL_CODE);
								jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail"));

								XxlJobCompleter.updateHandleInfoAndFinish(jobLog);
							}

						}
					} catch (Exception e) {
						if (!toStop) {
							logger.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
						}
					}

					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (Exception e) {
						if (!toStop) {
							logger.error(e.getMessage(), e);
						}
					}

				}

				logger.info(">>>>>>>>>>> xxl-job, JobLosedMonitorHelper stop");

			}
		});
		monitorThread.setDaemon(true);
		monitorThread.setName("xxl-job, admin JobLosedMonitorHelper");
		monitorThread.start();
	}

	public void toStop() {
		toStop = true;

		// stop registryOrRemoveThreadPool
		callbackThreadPool.shutdownNow();

		// stop monitorThread (interrupt and wait)
		monitorThread.interrupt();
		try {
			monitorThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}


	// ---------------------- helper ----------------------

	public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {

		callbackThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				for (HandleCallbackParam handleCallbackParam : callbackParamList) {
					ReturnT<String> callbackResult = callback(handleCallbackParam);
					logger.debug(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
							(callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"), handleCallbackParam, callbackResult);
				}
			}
		});

		return ReturnT.SUCCESS;
	}

	private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
		// valid log item
		if (handleCallbackParam.getLogId() == -1) {
			// 构建任务组数据请求，包含执行结果
			Map<String, Object> requestData = new HashMap<>();
			requestData.put("key", handleCallbackParam.getJobId() + ":" + handleCallbackParam.getRandomId());
			requestData.put("value", handleCallbackParam.getHandleCode() == ReturnT.SUCCESS_CODE);
			// 添加执行结果
			if (handleCallbackParam.getExecuteResult() != null) {
				requestData.put("executeResult", handleCallbackParam.getExecuteResult());
			}
			XxlJobRemotingUtil.postBody(handleCallbackParam.getAddress() + "api/addJobGroupData", "", 10, requestData, String.class);
			return ReturnT.SUCCESS;
		}
		JobLog log = XxlJobAdminConfig.getAdminConfig().getJobLogMapper().selectById(handleCallbackParam.getLogId());
		if (log != null && StringUtils.isNotBlank(log.getExecutorParam())) {
			logger.info(">>>>>>>executorParam:{}", log.getExecutorParam());
		}
		if (log == null) {
			return new ReturnT<>(ReturnT.FAIL_CODE, "log item not found.");
		}
		if (log.getHandleCode() > 0) {
			return new ReturnT<>(ReturnT.FAIL_CODE, "log repeate callback.");
		}

		// handle msg
		StringBuffer handleMsg = new StringBuffer();
		if (log.getHandleMsg() != null) {
			handleMsg.append(log.getHandleMsg()).append("<br>");
		}
		if (handleCallbackParam.getHandleMsg() != null) {
			handleMsg.append(handleCallbackParam.getHandleMsg());
		}

		int handleCode = handleCallbackParam.getHandleCode();
		// 同步后数据质量校验：任务成功且配置了校验时执行，不通过则改为失败
		if (handleCode == ReturnT.SUCCESS_CODE && XxlJobAdminConfig.getAdminConfig().getJobValidationMapper() != null) {
			LambdaQueryWrapper<JobValidation> q = new LambdaQueryWrapper<>();
			q.eq(JobValidation::getJobId, log.getJobId()).eq(JobValidation::getIsDeleted, 0).last("LIMIT 1");
			JobValidation validation = XxlJobAdminConfig.getAdminConfig().getJobValidationMapper().selectOne(q);
			if (validation != null && validation.getJdbcDatasourceId() != null) {
				JobJdbcDatasource ds = XxlJobAdminConfig.getAdminConfig().getJobJdbcDatasourceMapper().selectById(validation.getJdbcDatasourceId());
				String validationError = DataQualityValidationRunner.runValidation(validation, ds);
				if (validationError != null) {
					handleCode = ReturnT.FAIL_CODE;
					handleMsg.append("<br>[数据质量校验不通过] ").append(validationError);
				}
			}
		}

		// success, save log
		log.setHandleTime(LocalDateTime.now());
		log.setHandleCode(handleCode);
		log.setHandleMsg(handleMsg.toString());
		XxlJobCompleter.updateHandleInfoAndFinish(log);

		// 生命周期 Webhook：任务成功/失败时通知外部
		com.cc.job.admin.task.lifecycle.LifecycleWebhookSender sender = XxlJobAdminConfig.getAdminConfig().getLifecycleWebhookSender();
		if (sender != null) {
			String event = handleCallbackParam.getHandleCode() == ReturnT.SUCCESS_CODE ? "success" : "fail";
			sender.send(log, event);
		}

		return ReturnT.SUCCESS;
	}

}
