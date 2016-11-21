package me.libin.guice.quartz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import me.libin.guice.quartz.annotation.Scheduled;
import me.libin.guice.quartz.internal.util.ClassScanner;
import me.libin.guice.quartz.internal.util.EmptyUtil;
import me.libin.guice.quartz.internal.util.Stopwatch;
import me.libin.guice.quartz.util.GuiceJobFactory;

/**
 * 作者:李斌 E-mail:libin02@17guagua.com<br/>
 * 创建时间：2016-1-19 下午04:01:55<br/>
 * 说明：调度任务管理器
 */
@Singleton
public class GuiceQuartzManager {
	private Logger logger = LoggerFactory.getLogger(getClass());
	@Inject
	private GuiceJobFactory guiceJobFactory;
	@Inject
	private Injector injector;

	private final Stopwatch stopwatch = new Stopwatch();

	public static final String PROPERTIES_FILE = "me.libin.guice.quartz.properties";
	public static final String TASK_SWITCH = "me.libin.guice.quartz.isRun";
	public static final String TASK_PACKAGES = "me.libin.guice.quartz.scanPackages";

	private Properties properties = null;

	private void loadConfig() {
		String requestedFile = System.getProperty(PROPERTIES_FILE);
		String propFileName = requestedFile != null ? requestedFile : "quartz-guice.properties";
		File propFile = new File(propFileName);
		Properties props = new Properties();
		InputStream in = null;
		try {
			if (propFile.exists()) {
				try {
					in = new BufferedInputStream(new FileInputStream(propFileName));
					props.load(in);
				} catch (IOException ioe) {
					throw new RuntimeException("Properties file: '" + propFileName + "' could not be read.", ioe);
				}
			} else if (requestedFile != null) {
				in = Thread.currentThread().getContextClassLoader().getResourceAsStream(requestedFile);
				if (in == null) {
					throw new RuntimeException("Properties file: '" + requestedFile + "' could not be found.");
				}
				in = new BufferedInputStream(in);
				try {
					props.load(in);
				} catch (IOException ioe) {
					throw new RuntimeException("Properties file: '" + requestedFile + "' could not be read.", ioe);
				}

			} else {
				ClassLoader cl = getClass().getClassLoader();
				if (cl == null)
					cl = findClassloader();
				if (cl == null)
					throw new RuntimeException("Unable to find a class loader on the current thread or class.");
				in = cl.getResourceAsStream("quartz-guice.properties");
				if (in == null) {
					in = cl.getResourceAsStream("/quartz-guice.properties");
				}
				if (in == null) {
					throw new RuntimeException("Default quartz-guice.properties not found in class path");
				}
				try {
					props.load(in);
				} catch (IOException ioe) {
					throw new RuntimeException("Resource properties file: 'org/quartz/quartz-guice.properties' "
							+ "could not be read from the classpath.", ioe);
				}
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
		}
		properties = overrideWithSysProps(props);
	}

	private Properties overrideWithSysProps(Properties props) {
		Properties sysProps = null;
		try {
			sysProps = System.getProperties();
		} catch (AccessControlException e) {
			logger.warn("Skipping overriding quartz properties with System properties "
					+ "during initialization because of an AccessControlException.  "
					+ "This is likely due to not having read/write access for "
					+ "java.util.PropertyPermission as required by java.lang.System.getProperties().  "
					+ "To resolve this warning, either add this permission to your policy file or "
					+ "use a non-default version of initialize().", e);
		}

		if (sysProps != null) {
			props.putAll(sysProps);
		}
		return props;
	}

	private ClassLoader findClassloader() {
		// work-around set context loader for windows-service started jvms
		// (QUARTZ-748)
		if (Thread.currentThread().getContextClassLoader() == null && getClass().getClassLoader() != null) {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		}
		return Thread.currentThread().getContextClassLoader();
	}

	public void init() {
		loadConfig();
		String isRun = properties.getProperty(TASK_SWITCH, "no");
		if (!"yes".equalsIgnoreCase(isRun)) {
			logger.debug("调度任务没有打开，不初始化任务");
			return;
		}
		String scanPackages = properties.getProperty(TASK_PACKAGES);
		if (null == scanPackages || scanPackages.isEmpty()) {
			logger.debug("没有扫描的包，不初始化任务");
			return;
		}
		stopwatch.reset();
		String[] packageNames = scanPackages.split(",");
		Set<Class<?>> classes = ClassScanner.scanPackage(packageNames);
		stopwatch.resetAndLog("扫描class完成");

		Set<Class<?>> jobClasses = classes.parallelStream().filter(clazz -> {
			Method[] methods = clazz.getDeclaredMethods();
			for (Method method : methods) {
				Scheduled[] scheduleds = method.getAnnotationsByType(Scheduled.class);
				if (null != scheduleds && scheduleds.length > 0) {
					return true;
				}
			}
			return false;
		}).collect(Collectors.toSet());
		stopwatch.resetAndLog("过滤class完成");
		if (null == jobClasses || jobClasses.isEmpty()) {
			logger.debug("没有需要调度的方法，不初始化任务");
			return;
		}

		ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
		jobClasses.stream().forEach(clazz -> {
			Object target = injector.getInstance(clazz);
			Method[] methods = clazz.getDeclaredMethods();
			for (Method method : methods) {
				Scheduled[] scheduleds = method.getAnnotationsByType(Scheduled.class);
				if (!EmptyUtil.isEmpty(scheduleds)) {
					registrar.registJob(target, method, scheduleds);
				}
			}
		});
		stopwatch.resetAndLog("解析调度任务完成");

		if (!registrar.hasJobs()) {
			logger.debug("没有需要调度的方法，不初始化任务");
			return;
		}

		try {
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			// !!! 设置成为Guice特殊处理过的JobFactory类
			scheduler.setJobFactory(guiceJobFactory);

			for (JobDetail jobDetail : registrar.getJobDetails()) {
				scheduler.addJob(jobDetail, true);
			}

			for (Trigger tirgger : registrar.getTriggers()) {
				scheduler.scheduleJob(tirgger);
			}
			// 启动调度器
			scheduler.start();
			stopwatch.resetAndLog("调度完成！！！");
			logger.error("------- 定时任务开始 -----------------");
		} catch (SchedulerException e) {
			logger.error("------- 定时任务启动失败 -------------", e);
		}
	}
}
