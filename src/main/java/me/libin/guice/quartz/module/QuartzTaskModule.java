package me.libin.guice.quartz.module;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import me.libin.guice.quartz.GuiceQuartzManager;

public class QuartzTaskModule extends AbstractModule {
	protected void configure() {
		bindListener(new AbstractMatcher<TypeLiteral<?>>() {
			TypeLiteral<GuiceQuartzManager> tt = TypeLiteral.get(GuiceQuartzManager.class);

			public boolean matches(TypeLiteral<?> t) {
				return (tt == t) || (tt.getRawType().equals(t.getRawType()));
			}
		}, new TypeListener() {
			public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
				encounter.register(new InjectionListener<I>() {
					public void afterInjection(final I injectee) {
						((GuiceQuartzManager) injectee).init();
					}
				});
			}
		});
		bind(GuiceQuartzManager.class).asEagerSingleton();
	}
}