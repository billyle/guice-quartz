

import com.google.inject.AbstractModule;

public class QuartzTaskModule extends AbstractModule {
	protected void configure() {
		// 初始化调度任务
		bind(InitQuartz.class).asEagerSingleton();
	}
}