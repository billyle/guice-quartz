package me.libin.guice.quartz.task;

import com.google.inject.Singleton;

import me.libin.guice.quartz.annotation.Scheduled;

@Singleton
public class TestTask {

	@Scheduled(cron = "*/4 * * * * ?")
	@Scheduled(cron = "*/2 * * * * ?")
	public void exec() {
		System.err.println("I'm running!");
	}
}
