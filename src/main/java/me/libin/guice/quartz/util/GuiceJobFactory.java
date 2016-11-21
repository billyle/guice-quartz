package me.libin.guice.quartz.util;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * 作者:李斌 E-mail:libin02@17guagua.com<br/>
 * 创建时间：2016-1-18 下午10:01:15<br/>
 * 说明:自定义调度任务工厂，从guice中获取Job对象的实例
 */
@Singleton
public final class GuiceJobFactory implements JobFactory {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private final Injector injector;

	@Inject
	public GuiceJobFactory(final Injector injector) {
		this.injector = injector;
	}

	public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
		JobDetail jobDetail = triggerFiredBundle.getJobDetail();
		Class<? extends Job> jobClass = jobDetail.getJobClass();
		try {
			return (Job) injector.getInstance(jobClass);
		} catch (Exception e) {
			logger.error("创建任务调度Job对象失败", e);
			throw new UnsupportedOperationException(e);
		}
	}
}