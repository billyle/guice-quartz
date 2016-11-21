package me.libin.guice.quartz.internal.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 作者:李斌 E-mail:libin02@17guagua.com<br/>
 * 创建时间：2016年11月17日 下午11:40:45<br/>
 * 说明:类扫描器
 */
public class ClassScanner {
	private static Logger logger = LoggerFactory.getLogger(ClassScanner.class);

	public static Set<Class<?>> scanPackage(String[] packageNames) {
		if (packageNames == null || packageNames.length == 0) {
			return Collections.emptySet();
		}
		Set<Class<?>> rs = new HashSet<>();
		for (String packageName : packageNames) {
			rs.addAll(scanPackage(packageName));
		}
		return rs;
	}

	/**
	 * 扫描包下面的类
	 * 
	 * @param packageName
	 */
	public static Set<Class<?>> scanPackage(String packageName) {
		if (packageName == null || packageName.isEmpty()) {
			return Collections.emptySet();
		}
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		// 是否循环搜索包
		boolean recursive = true;
		// 存放扫描到的类
		Set<Class<?>> classes = new LinkedHashSet<>();
		// 将包名转换为文件路径
		String packageDirName = packageName.replace('.', '/');

		try {
			Enumeration<URL> resources = classLoader.getResources(packageDirName);
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				String protocol = url.getProtocol();
				if ("file".equals(protocol)) {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					// 以文件的方式扫描整个包下的文件 并添加到集合中
					scanDirectory(classLoader, packageName, filePath, recursive, classes);
				} else if ("jar".equals(protocol)) {
					//
					scanJar(classLoader, packageName, url, recursive, classes);
				}
			}
			return classes;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void scanJar(ClassLoader classLoader, String packageName, URL url, final boolean recursive,
			Set<Class<?>> classes) throws IOException {
		String splashPath = packageName.replace('.', '/');
		String filePath = getRootPath(url);
		List<String> names = null;
		if (filePath.endsWith(".jar")) {
			logger.debug("{} 是一个JAR包", filePath);
			names = getClassNameFromJar(filePath, splashPath);
		}

		for (String name : names) {
			if (name.endsWith(".class")) {
				String className = name.replaceAll("/", ".").substring(0, name.length() - 6);
				logger.debug("load class -> {}", className);
				try {
					// 添加到集合中去
					classes.add(classLoader.loadClass(className));
				} catch (ClassNotFoundException e) {
					logger.error("添加用户自定义视图类错误 找不到此类的.class文件,class={}", className);
				} catch (NoClassDefFoundError e) {
					logger.error("扫包加载类出错，class={},e={}", className, e.getMessage());
				}
			}
		}
	}

	private static List<String> getClassNameFromJar(String jarPath, String splashedPackageName) throws IOException {
		JarInputStream jarIn = new JarInputStream(new FileInputStream(jarPath));
		JarEntry entry;
		List<String> nameList = new ArrayList<>();
		while (null != (entry = jarIn.getNextJarEntry())) {
			String name = entry.getName();
			if (name.startsWith(splashedPackageName) && name.endsWith(".class")) {
				nameList.add(name);
			}
		}
		jarIn.close();
		logger.debug("从JAR包中读取类:{},共{}个", jarPath, nameList.size());
		return nameList;
	}

	/**
	 * "file:/home/whf/cn/fh" -> "/home/whf/cn/fh"
	 * "jar:file:/home/whf/foo.jar!cn/fh" -> "/home/whf/foo.jar"
	 */
	public static String getRootPath(URL url) {
		String fileUrl = url.getFile();
		int pos = fileUrl.indexOf('!');
		if (-1 == pos) {
			return fileUrl;
		}
		return fileUrl.substring(5, pos);
	}

	/**
	 * 以文件的形式来获取包下的所有Class
	 * 
	 * @param packageName
	 *            包名
	 * @param packagePath
	 *            包的物理路径
	 * @param recursive
	 *            是否递归扫描
	 * @param classes
	 *            类集合
	 */
	private static void scanDirectory(ClassLoader classLoader, String packageName, String packagePath,
			final boolean recursive, Set<Class<?>> classes) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			System.out.println("用户定义包名 " + packageName + " 下没有任何文件");
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		File[] dirAndClassFiles = dir.listFiles(new FileFilter() {
			// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
			public boolean accept(File file) {
				return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
			}
		});
		// 循环所有文件
		for (File file : dirAndClassFiles) {
			// 如果是目录 则递归继续扫描
			if (file.isDirectory()) {
				scanDirectory(classLoader, packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
						classes);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0, file.getName().length() - 6);
				try {
					// 添加到集合中去
					classes.add(classLoader.loadClass(packageName + '.' + className));
				} catch (ClassNotFoundException e) {
					logger.error("添加用户自定义视图类错误 找不到此类的.class文件", e);
				}
			}
		}
	}
}
