/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.libin.guice.quartz;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import me.libin.guice.quartz.annotation.Scheduled;
import me.libin.guice.quartz.internal.util.Assert;
import me.libin.guice.quartz.internal.util.EmptyUtil;
import me.libin.guice.quartz.internal.util.StringUtils;

public class ScheduledTaskRegistrar {
	private List<JobDetail> jobDetails;

	private List<Trigger> triggers;

	public void registJob(Object target, Method method, Scheduled[] scheduleds) {
		if (null == jobDetails) {
			jobDetails = new ArrayList<>();
		}
		JobDataMap newJobDataMap = new JobDataMap();
		newJobDataMap.put("obj", target);
		newJobDataMap.put("method", method);
		JobDetail jobDetail = JobBuilder.newJob(MethodInvokeJob.class).setJobData(newJobDataMap).storeDurably(true)
				.build();
		jobDetails.add(jobDetail);

		for (Scheduled scheduled : scheduleds) {
			boolean processedSchedule = false;
			String errorMessage = "Exactly one of the 'cron', 'fixedDelay(String)', or 'fixedRate(String)' attributes is required";
			long initialDelay = scheduled.initialDelay();
			String initialDelayString = scheduled.initialDelayString();
			if (StringUtils.hasText(initialDelayString)) {
				Assert.isTrue(initialDelay < 0, "Specify 'initialDelay' or 'initialDelayString', not both");
				try {
					initialDelay = Long.parseLong(initialDelayString);
				} catch (NumberFormatException ex) {
					throw new IllegalArgumentException("Invalid initialDelayString value \"" + initialDelayString
							+ "\" - cannot parse into integer");
				}
			}

			String cron = scheduled.cron();
			if (StringUtils.hasText(cron)) {
				Assert.isTrue(initialDelay == -1, "'initialDelay' not supported for cron triggers");
				processedSchedule = true;
				String zone = scheduled.zone();
				TimeZone timeZone;
				if (StringUtils.hasText(zone)) {
					timeZone = StringUtils.parseTimeZoneString(zone);
				} else {
					timeZone = TimeZone.getDefault();
				}
				addCronTrigger(jobDetail, scheduled.cron(), timeZone);
			}

			// At this point we don't need to differentiate between initial
			// delay set or not anymore
			if (initialDelay < 0) {
				initialDelay = 0;
			}

			// Check fixed delay
			long fixedDelay = scheduled.fixedDelay();
			if (fixedDelay >= 0) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				addFixedDelayTrigger(jobDetail, fixedDelay, initialDelay);
			}
			String fixedDelayString = scheduled.fixedDelayString();
			if (StringUtils.hasText(fixedDelayString)) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				try {
					fixedDelay = Long.parseLong(fixedDelayString);
				} catch (NumberFormatException ex) {
					throw new IllegalArgumentException(
							"Invalid fixedDelayString value \"" + fixedDelayString + "\" - cannot parse into integer");
				}
				addFixedDelayTrigger(jobDetail, fixedDelay, initialDelay);
			}
		}
	}

	public void addCronTrigger(JobDetail jobDetail, String corn, TimeZone zone) {
		CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(corn).inTimeZone(zone);
		Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail).withSchedule(scheduleBuilder)
				.withDescription("corn=" + corn + ",zone" + zone).build();
		addTrigger(trigger);
	}

	public void addFixedDelayTrigger(JobDetail jobDetail, long fixedDelay, long initialDelay) {
		SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
				.withIntervalInMilliseconds(fixedDelay);
		Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail).withSchedule(scheduleBuilder)
				.withDescription("").build();
		addTrigger(trigger);
	}

	private void addTrigger(Trigger trigger) {
		if (null == triggers) {
			triggers = new ArrayList<>();
		}
		triggers.add(trigger);
	}

	public boolean hasJobs() {
		return !EmptyUtil.isEmpty(jobDetails);
	}

	public List<JobDetail> getJobDetails() {
		return jobDetails;
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}
}
