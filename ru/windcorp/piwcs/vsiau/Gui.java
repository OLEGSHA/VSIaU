package ru.windcorp.piwcs.vsiau;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

public class Gui {
	
	private final JFrame frame;
	private JTextArea textArea;
	
	public static void init() throws Exception {
		chooseAction();
		showLog();
	}

	private static void chooseAction() {
		String[] actions = new String[Main.getActions().size() + 1];
		for (int i = 0; i < actions.length - 1; ++i) {
			actions[i] = Main.getActions().get(i).getName();
		}
		actions[actions.length - 1] = "Cancel";
		
		int result = JOptionPane.showOptionDialog(
				null,
				"Please choose an action",
				Main.NAME,
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				actions,
				actions[0]
		);
		
		if (result < 0 || result >= actions.length - 1) {
			System.exit(0);
		}
		
		Main.setAction(Main.getActions().get(result));
	}
	
	private static void showLog() throws Exception {
		SwingUtilities.invokeAndWait(Gui::new);
	}

	public Gui() {
		frame = new JFrame(Main.NAME + " " + Main.VERSION);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.getContentPane().add(createLogWindow(), BorderLayout.CENTER);
		
		frame.pack();
		frame.setVisible(true);
	}

	private Container createLogWindow() {
		textArea = new JTextArea(25, 100);
		textArea.setFont(Font.decode(Font.MONOSPACED + "-plain-14"));
		textArea.setEditable(false);
		
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		System.setOut(new PrintStream(new SpyOutputStream(System.out, this)));
		System.setErr(new PrintStream(new SpyOutputStream(System.err, this)));
		
		JScrollPane scrollPane = new JScrollPane(
				textArea,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		);
		
		return scrollPane;
	}
	
	public void log(String text) {
		textArea.append(text);
	}

}
