package ru.windcorp.piwcs.vsiau;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ActionInstall extends Action {
	
	public ActionInstall() {
		super("Install");
	}

	@Override
	public void run() throws IOException {
		checkDirectories();
		Path zipFile = downloadZipFile();
		unpackZipFile(zipFile);
	}

	private static void checkDirectories() throws IOException {
		System.out.println("Checking installation directory...");
		
		checkDirectory(getConfigPath());
		checkDirectory(getModsPath());
		checkDirectory(getModsVersionPath());
	}
	
	private static void checkDirectory(Path dir) throws IOException {
		if (Files.notExists(dir)) {
			System.out.println(dir + " does not exist, creating one");
			Files.createDirectory(dir);
		} else {
			if (Files.list(dir)
					.filter(((Predicate<Path>) Files::isDirectory).negate())
					.findAny()
					.isPresent()
			) {
				throw new IOException(dir + " is not empty. Please clear it manually");
			}
		}
	}

	private static Path getConfigPath() {
		return Paths.get("config");
	}
	
	private static Path getModsPath() {
		return Paths.get("mods");
	}
	
	private static Path getModsVersionPath() {
		return getModsPath().resolve("1.7.10");
	}

	private static Path downloadZipFile() throws IOException {
		System.out.println("Downloading modpack...");
		return download("pages/piwcs/latest/");
	}

	private static void unpackZipFile(Path zipFile) throws IOException {
		System.out.println("Unpacking modpack...");
		try (ZipFile zip = new ZipFile(zipFile.toFile())) {
		
			for (ZipEntry e : Collections.list(zip.entries())) {
				install(e, zip);
			}
		
		}
	}

	private static void install(ZipEntry e, ZipFile zip) throws IOException {
		if (e.getName().endsWith("/")) {
			// Is a directory
			return;
		}
		
		System.out.println("Unpacking " + e.getName());
		
		Path path = Paths.get(e.getName());
		
		if (path.getNameCount() > 1 && path.getName(0).toString().startsWith("PIWCS"))
			path = path.subpath(1, path.getNameCount());
		
		if (path.getNameCount() > 1)
			Files.createDirectories(path.subpath(0, path.getNameCount() - 1));
		
		try (
				InputStream inputStream = zip.getInputStream(e);
				ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
				FileOutputStream outputStream = new FileOutputStream(path.toFile());
				FileChannel outputChannel = outputStream.getChannel();
		) {
			outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
		}
	}

}
