

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * 作者:李斌 E-mail:libin02@17guagua.com<br/>
 * 创建时间：2016-1-19 下午04:01:55<br/>
 * 说明:初始化调度任务
 */
@Singleton
public class InitQuartz  {
	private Logger logger = LoggerFactory.getLogger(getClass());
	@Inject
	private Injector injector;

	private static volatile boolean isTimerServer = false;


	public boolean isTimeServer() {
		return isTimerServer;
	}

	public void afterInjection() {
		if (!isTimerServer) {
			logger.error("不是任务服务器");
			return;
		}
		logger.error("是任务服务器");

		try {
			// 配置调度器线程数量，就这一个懒得写配置文件了
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			// !!! 设置成为Guice特殊处理过的JobFactory类
			scheduler.setJobFactory(injector.getInstance(GuiceJobFactory.class));
			// 大厅任务
			// JobDetail jobHall =
			// JobBuilder.newJob(HallUpdateJob.class).withIdentity("首页大厅推荐更新分数任务",
			// "大厅组").build();
			CronTrigger cronTriggerHall = TriggerBuilder.newTrigger().withIdentity("首页大厅推荐更新分数任务", "大厅组")
					.withSchedule(CronScheduleBuilder.cronSchedule("*/5 * * * * ?")).build();// 每五秒执行一次
			// scheduler.scheduleJob(jobHall, cronTriggerHall);
			Trigger trigger = cronTriggerHall;
			scheduler.scheduleJob(trigger);
			
			

			// 启动调度器
			scheduler.start();
			logger.error("------- 定时任务开始 -----------------");
		} catch (SchedulerException e) {
			logger.error("------- 定时任务启动失败 -------------", e);
		}
	}
}
