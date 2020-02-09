package ru.windcorp.piwcs.vsiau;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ActionUpdate extends Action {
	
	private static class Program {
		private abstract static class Command {
			private static class Extract extends Command {
				public Extract() {
					super("Extract", 2, "Extracting %2$s");
				}
				
				@Override
				public void run(String[] args, ZipFile zip) throws IOException {
					ZipEntry source = zip.getEntry(args[0]);
					Path dest = Paths.get(args[1]);
					
					if (source == null)
						throw new IOException("Malformed update program: \"" + source + "\" not found in ZIP file");
					
					if (dest.getNameCount() > 1)
						Files.createDirectories(dest.subpath(0, dest.getNameCount() - 1));
					
					try (
							InputStream inputStream = zip.getInputStream(source);
							ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
							FileOutputStream outputStream = new FileOutputStream(dest.toFile());
							FileChannel outputChannel = outputStream.getChannel();
					) {
						outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
					}
				}
			}
			
			private static class Delete extends Command {
				public Delete() {
					super("Delete", 1, "Deleting %2$s");
				}
				
				@Override
				public void run(String[] args, ZipFile zip) throws IOException {
					for (
							Path path = Paths.get(args[1]);
							path.getNameCount() != 0;
							path = path.subpath(0, path.getNameCount() - 1)
					) {
						
						if (Files.notExists(path)) {
							continue;
						} else if (Files.isRegularFile(path)) {
							// Go forth; delete silently
						} else if (Files.isDirectory(path)) {
							if (Files.list(path).findAny().isPresent()) {
								break;
							} else {
								System.out.println("  also deleting empty directory " + path);
							}
						} else {
							System.out.println("  not deleting: " + path + " does not denote a file or a directory");
							break;
						}
						
						Files.delete(path);
						
					}
				}
			}
			
			private final String name;
			private final int argCount;
			private final String description;
			
			protected Command(String name, int argCount, String description) {
				this.name = name;
				this.argCount = argCount;
				this.description = description;
			}
			
			public abstract void run(String[] args, ZipFile zip) throws IOException;
			
			@SuppressWarnings("unused")
			public String getName() {
				return name;
			}
			
			public int getArgCount() {
				return argCount;
			}
			
			public String getDescription(String[] args) {
				return String.format(description, (Object[]) args);
			}
			
			public void register() {
				COMMANDS.put(name, this);
			}
		}
		
		private static class CommandInvocation {
			private final Command command;
			private final String[] args;
			
			public CommandInvocation(Command command, String[] args) {
				this.command = command;
				this.args = args;
			}

			public void run(ZipFile zip) throws IOException {
				System.out.println(command.getDescription(args));
				command.run(args, zip);
			}
		}
		
		private static final Map<String, Command> COMMANDS = new HashMap<>();
		
		static {
			new Command.Extract().register();
			new Command.Delete().register();
		}
		
		private final List<CommandInvocation> instructions = new ArrayList<>();
		private final String expectedVersion;
		
		public Program(Reader source) throws IOException {
			expectedVersion = readHeader(source);
			
			StringBuilder commandName = new StringBuilder();
			StringBuilder[] args = new StringBuilder[
					COMMANDS.values().stream().mapToInt(Command::getArgCount).max().getAsInt()
			];
			
			for (int i = 0; i < args.length; ++i) {
				args[i] = new StringBuilder();
			}
			
			int element = -1;
			
			while (true) {
				
				int cOrEOF = source.read();
				if (cOrEOF < 0) break;
				
				char c = (char) cOrEOF;
				switch (c) {
				
				case ';':
					element++;
					
					if (element >= args.length) {
						throw new IOException("Malformed update program: excessive arguments");
					}
					
					args[element].setLength(0);
					if (source.read() != ' ') {
						throw new IOException("Malformed update program: unexpected char or EOF after a semicolon, expected ' '");
					}
					
					break;
					
				case '\n':
					instructions.add(compile(commandName, args, element + 1));
					commandName.setLength(0);
					element = -1;
					break;
					
				default:
					if (element == -1) {
						commandName.append(c);
					} else {
						args[element].append(c);
					}
					break;
					
				}
				
			}
			
			instructions.add(compile(commandName, args, element + 1));
		}
		
		private static final char SYNTAX_VERSION = '0';
		
		private static String readHeader(Reader source) throws IOException {
			int syntaxVersion = source.read();
			if (syntaxVersion != SYNTAX_VERSION) {
				throw new IOException("This updater cannot apply the update because the updater is outdated. "
						+ "Reinstall from scratch or get the newest updater. "
						+ "Required syntax version: " + ((char) syntaxVersion));
			}
			
			if (source.read() != '\n') {
				throw new IOException("Malformed update program: unexpected char or EOF after syntax version, expected '\\n'");
			}
			
			StringBuilder sb = new StringBuilder();
			while (true) {
				int c = source.read();
				if (c == '\n') {
					break;
				} else {
					sb.append((char) c);
				}
			}
			
			return sb.toString();
		}

		private static CommandInvocation compile(StringBuilder commandName, StringBuilder[] args, int argCount) throws IOException {
			Command command = COMMANDS.get(commandName.toString());
			
			if (command == null) {
				throw new IOException("Malformed update program: unknown command \"" + commandName + "\"");
			}
			
			if (argCount != command.getArgCount()) {
				throw new IOException("Malformed update program: command " + commandName + " requires " + command.getArgCount() + " arguments but " + argCount + " provided");
			}
			
			String[] strArgs = new String[argCount];
			for (int i = 0; i < strArgs.length; ++i) strArgs[i] = args[i].toString();
			
			return new CommandInvocation(command, strArgs);
		}

		public void run(ZipFile zip) throws IOException {
			System.out.println("Applying update...");
			for (CommandInvocation inv : instructions) {
				inv.run(zip);
			}
		}
		
		public String getExpectedVersion() {
			return expectedVersion;
		}
	}
	
	public ActionUpdate() {
		super("Update");
	}

	@Override
	public void run() throws IOException {
		Path zipFile = downloadZipFile();
		
		try (ZipFile zip = unpackZipFile(zipFile)) {
			Program program = readProgram(zip);
			checkDirectories(program.getExpectedVersion());
			program.run(zip);

			updateMarker(program.getExpectedVersion());
		}
	}

	private static Path downloadZipFile() throws IOException {
		System.out.println("Downloading patch...");
		return download("pages/piwcs/latest_patch/");
	}

	private static ZipFile unpackZipFile(Path zipFile) throws IOException {
		System.out.println("Unpacking patch...");
		ZipFile zip = new ZipFile(zipFile.toFile());
		
		return zip;
	}

	private static Program readProgram(ZipFile zip) throws IOException {
		ZipEntry entry = zip.getEntry("program");
		
		if (entry == null) {
			throw new IOException("Malformed update package: update program not found");
		}
		
		try (
				InputStream inputStream = zip.getInputStream(entry);
				Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		) {
			return new Program(reader);
		}
	}

	private static void checkDirectories(String expected) throws IOException {
		System.out.println("Checking installation directory...");
		
		Path markerPath = Paths.get("mods", "1.7.10");
		
		Pattern regex = Pattern.compile("PIWCS \\d+\\.\\d+\\.\\d+\\.txt");
		
		List<String> markers = Files.list(markerPath)
			.map(Path::getFileName)
			.map(Path::toString)
			.filter(regex.asPredicate())
			.map(s -> s.substring("PIWCS ".length(), s.length() - ".txt".length()))
			.collect(Collectors.toCollection(ArrayList::new));
		
		if (markers.isEmpty()) {
			throw new IOException("PIWCS modpack not installed: file \"PIWCS <version>.txt\" not found");
		}
		
		markers.sort(Comparator.reverseOrder());
		String marker = markers.get(0);
		
		if (markers.size() > 1)
			System.out.println("Found modpack version markers " + markers + ", assuming " + marker);
		else
			System.out.println("Found modpack version marker " + marker);
		
		if (!marker.equals(expected)) {
			throw new IOException("Expected version " + expected + " but found version " + marker + ". Please reinstall from scratch.");
		}
	}
	
	private static  void updateMarker(String version) throws IOException {
		Files.write(
				Paths.get("mods", "1.7.10", "PIWCS " + version + ".txt"),
				("Модпак PIWCS. Обновлено автоматически при помощи " + Main.NAME + " " + Main.VERSION + ".").getBytes(StandardCharsets.UTF_8)
		);
	}

}