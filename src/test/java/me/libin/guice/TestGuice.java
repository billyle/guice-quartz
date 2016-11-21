package me.libin.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class TestGuice {

	@Inject
	A a;

	public static void main(String[] args) {
		Stage stage = Stage.PRODUCTION;
		stage = Stage.DEVELOPMENT;
		Injector inj = Guice.createInjector(stage, new AbstractModule() {
			protected void configure() {
				// bindListener(typeMatcher, listener);
				bind(B.class).to(C.class);
			}
		});
		TestGuice t = inj.getInstance(TestGuice.class);
		System.err.println(t);
	}
}
