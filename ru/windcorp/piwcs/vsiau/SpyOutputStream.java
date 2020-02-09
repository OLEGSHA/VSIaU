package ru.windcorp.piwcs.vsiau;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class SpyOutputStream extends OutputStream {
	
	private final PrintStream original;
	private final Gui gui;
	
	private final StringBuilder buffer = new StringBuilder();

	public SpyOutputStream(PrintStream out, Gui gui) {
		original = out;
		this.gui = gui;
	}

	@Override
	public void write(int b) throws IOException {
		char c = (char) b;
		
		original.write(c);
		buffer.append(c);
		
		if (c == '\n') {
			flush();
		}
	}
	
	@Override
	public void flush() throws IOException {
		gui.log(buffer.toString());
		buffer.setLength(0);
	}

}
