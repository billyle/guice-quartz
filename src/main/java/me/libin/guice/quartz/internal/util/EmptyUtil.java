package me.libin.guice.quartz.internal.util;

import java.util.Collection;
import java.util.Map;

public class EmptyUtil {
	private EmptyUtil() {
	}

	public static boolean isEmpty(String str) {
		return null == str || str.isEmpty();
	}

	public static boolean isEmpty(Collection<?> collection) {
		return null == collection || collection.isEmpty();
	}

	public static boolean isEmpty(Map<?, ?> map) {
		return null == map || map.isEmpty();
	}

	public static boolean isEmpty(Object[] array) {
		return null == array || array.length == 0;
	}
}
