package me.libin.guice.quartz;

import com.google.inject.Guice;

import me.libin.guice.quartz.module.QuartzTaskModule;

public class TestGuiceQuartz {

	public static void main(String[] args) {
		Guice.createInjector(new QuartzTaskModule());
		System.err.println("启动");
		try {
			Thread.sleep(100000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
