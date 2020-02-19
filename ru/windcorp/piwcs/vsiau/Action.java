package ru.windcorp.piwcs.vsiau;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Action {

	private final String name;

	protected Action(String name) {
		this.name = name;
	}
	
	public abstract void run() throws IOException, AbortException;
	
	public String getName() {
		return name;
	}
	
	protected static Path download(String path) throws IOException {
		URL source = new URL("http://windcorp.ru/" + path);

		Path destination = Files.createTempFile(Main.SHORT_NAME + "__", null);
		destination.toFile().deleteOnExit();

		URLConnection connection = source.openConnection();
		
		try (
				ReadableByteChannel sourceChannel =
						Channels.newChannel(connection.getInputStream());
		) {
		
			try (
					FileOutputStream destStream =
							new FileOutputStream(destination.toFile());
					FileChannel destChannel =
							destStream.getChannel();
			) {
				
				final long step = 1024 * 1024 * 8; // Mebibyte
				final long size = connection.getContentLengthLong();
				
				System.out.println("Downloading " + source + " to " + destination + "...");
				
				long position = 0;
				while (true) {
					long transferred = destChannel.transferFrom(sourceChannel, position, step);
					
					if (transferred == 0) break;
					else position += transferred;
					
					System.out.printf("  %-2d / %s MiB%n",
							(position - 1) / step + 1,
							(size >= 0) ? Long.toString((size - 1) / step + 1) : "?");
				}
				
				System.out.printf("Download complete (%.2f MiB)%n", size / (float) step);
			
			}
		}
		
		return destination;
	}

}
