package me.libin.guice.quartz;

import java.lang.reflect.Method;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import me.libin.guice.quartz.task.TestTask;

public class TestQuartz {
	public static void main(String[] args) throws SchedulerException, NoSuchMethodException, SecurityException {
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		// !!! 设置成为Guice特殊处理过的JobFactory类
		// scheduler.setJobFactory(guiceJobFactory);

		// 启动调度器
		scheduler.start();

		Method method = TestTask.class.getDeclaredMethod("exec");
		JobDataMap newJobDataMap = new JobDataMap();
		newJobDataMap.put("obj", new TestTask());
		newJobDataMap.put("method", method);
		JobDetail jobDetail = JobBuilder.newJob(MethodInvokeJob.class).setJobData(newJobDataMap).storeDurably(true)
				.build();
		Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
				.withSchedule(CronScheduleBuilder.cronSchedule("*/5 * * * * ?")).build();
		scheduler.addJob(jobDetail, true);
		scheduler.scheduleJob(trigger);
	}
}
