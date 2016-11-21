package me.libin.guice.quartz.internal.util;

import java.util.Set;

import me.libin.guice.quartz.internal.util.ClassScanner;

public class TestScanner {

	public static void main(String[] args) throws Exception {
		// ClasspathPackageScanner scan = new ClasspathPackageScanner("org",
		// "/Users/binli/.m2/");
		// Set<String> names = scan.getFullyQualifiedClassNameList();
		// for (String name : names) {
		// System.out.println(name);
		// }

		System.err.println("-----");

		Set<Class<?>> names2 = ClassScanner.scanPackage("org");
		for (Class<?> name : names2) {
			System.out.println(name);
		}
	}

}
