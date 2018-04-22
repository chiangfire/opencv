package org.opencv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
//import org.opencv.core.Core;

/**
 * @author JIANG
 */
public class Loader {

	private static Path libraryPath = null;
	protected static final String PLATFORM;
	protected static final String OS_NAME;
	protected static final String WINDOWS_NAME = "windows";
	protected static final String OPENCV_PREFIX = "opencv";
	protected static final String FORWARD_SLASH = "/";
	protected static final String NATIVE_LIBRARY_NAME = "opencv_java.dll";//Core.NATIVE_LIBRARY_NAME;
	protected static final Pattern FILE_DIRECTORY_PATTERN = Pattern.compile("^(" + OPENCV_PREFIX + ")[1-9]\\d{16,}$");

	static {
		String jvmName = System.getProperty("java.vm.name", "").toLowerCase();
		String osName = System.getProperty("os.name", "").toLowerCase();
		String osArch = System.getProperty("os.arch", "").toLowerCase();
		String abiType = System.getProperty("sun.arch.abi", "").toLowerCase();
		String libPath = System.getProperty("sun.boot.library.path", "").toLowerCase();
		if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
			osName = "android";
		} else if (jvmName.startsWith("robovm") && osName.startsWith("darwin")) {
			osName = "ios";
			osArch = "arm";
		} else if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
			osName = "macosx";
		} else {
			int spaceIndex = osName.indexOf(' ');
			if (spaceIndex > 0) {
				osName = osName.substring(0, spaceIndex);
			}
		}
		if (osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586") || osArch.equals("i686")) {
			osArch = "x86";
		} else if (osArch.equals("amd64") || osArch.equals("x86-64") || osArch.equals("x64")) {
			osArch = "x86_64";
		} else if (osArch.startsWith("aarch64") || osArch.startsWith("armv8") || osArch.startsWith("arm64")) {
			osArch = "arm64";
		} else if ((osArch.startsWith("arm"))
				&& ((abiType.equals("gnueabihf")) || (libPath.contains("openjdk-armhf")))) {
			osArch = "armhf";
		} else if (osArch.startsWith("arm")) {
			osArch = "arm";
		}
		OS_NAME = osName;
		PLATFORM = osName + "-" + osArch;
	}

	public static final synchronized void load() {
		if (libraryPath == null) {
			System.load(initLibrary().normalize().toString());
		}
	}

	/**
	 * 初始化 OpenCV 本地依赖
	 */
	private static Path initLibrary() {
		String location = String.join("",FORWARD_SLASH,PLATFORM,FORWARD_SLASH,NATIVE_LIBRARY_NAME);
		try (InputStream inputStream = Loader.class.getResourceAsStream(location);) {
			if (inputStream == null) {
				throw new RuntimeException(String.join("", PLATFORM, " 未找到 OpenCV 依赖。"));
			}
			// windows 一般都是开发开发环境，依赖如果已经存在不做任何操作
			if (WINDOWS_NAME.equals(OS_NAME.toLowerCase())) {
				File tempFile = new File(System.getProperty("java.io.tmpdir"));
				if (tempFile.exists()) {
					List<File> files = Arrays.stream(tempFile.listFiles())
							                 .filter(file -> (file.isDirectory() && FILE_DIRECTORY_PATTERN.matcher(file.getName()).find()))
							                 .collect(Collectors.toList());
					
					List<File> dllFils = files.stream().flatMap(timeFile -> Arrays.stream(timeFile.listFiles())
									                   .filter(fromFile -> PLATFORM.equals(fromFile.getName())))
							                  .flatMap(platFile -> Arrays.stream(platFile.listFiles())
									                   .filter(dllFile -> dllFile.getName().equals(NATIVE_LIBRARY_NAME)))
							                  .collect(Collectors.toList());
					if (!dllFils.isEmpty()) {
						return dllFils.get(0).toPath();
					}
				}
			}
			Path tempDir = Files.createTempDirectory(OPENCV_PREFIX);

			// 删除依赖
			/*Arrays.stream(tempDir.getParent().toFile().listFiles())
				  .filter(file -> (file.isDirectory() && FILE_DIRECTORY_PATTERN.matcher(file.getName()).find()))
				  .map(File::toPath).forEach(Loader::delete);*/

			// JVM关闭时删除文件夹
			/*Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					delete(tempDir);
				}
			});*/

			libraryPath = tempDir.resolve(String.join(FORWARD_SLASH,".",location)).normalize();
			Files.createDirectories(libraryPath.getParent());
			Files.copy(inputStream, libraryPath);
			return libraryPath;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 删除文件
	 * 
	 * @param path
	 */
	protected static void delete(Path path) {
		if (!Files.exists(path)) {
			return;
		}
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
					Files.deleteIfExists(dir);
					return super.postVisitDirectory(dir, e);
				}

				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					Files.deleteIfExists(file);
					return super.visitFile(file, attrs);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		load();
	}
}
