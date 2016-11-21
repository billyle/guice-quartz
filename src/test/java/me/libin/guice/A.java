package me.libin.guice;

import com.google.inject.Singleton;

@Singleton
public class A {
	public A() {
		System.err.println("aaa");
	}
}
