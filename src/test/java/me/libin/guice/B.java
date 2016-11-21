package me.libin.guice;

import com.google.inject.Singleton;

@Singleton
public class B {
	public B() {
		System.err.println("bbb");
	}
}
