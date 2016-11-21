package me.libin.guice;

import com.google.inject.Singleton;

@Singleton
public class C extends B {
	public C() {
		System.err.println("ccc");
	}
}
