package me.libin.guice.quartz;

import java.lang.reflect.Method;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisallowConcurrentExecution
public class MethodInvokeJob implements Job {
	private Logger logger = LoggerFactory.getLogger(getClass());

	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		Object object = dataMap.get("obj");
		Method method = (Method) dataMap.get("method");
		logger.debug("trigger=({}),method={}", context.getTrigger().getDescription(), method.getName());
		try {
			method.setAccessible(true);
			method.invoke(object);
		} catch (Exception e) {
			throw new RuntimeException("执行定时任务方法出错，" + method.getName(), e);
		}
	}
}
