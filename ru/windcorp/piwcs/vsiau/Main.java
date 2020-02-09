package ru.windcorp.piwcs.vsiau;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
	
	public static final String NAME = "PIWCS Modpack VSIaU";
	public static final String SHORT_NAME = "PIWCS_VSIaU";
	public static final String VERSION = "1.0";
	
	private static final List<Action> ACTIONS = new ArrayList<>();
	
	private static Action action = null;

	public static void main(String[] args) throws Exception {
		registerActions();
		
		if (args.length == 1) {
			ACTIONS.stream()
				.filter(a -> a.getName().equalsIgnoreCase(args[0]))
				.findAny()
				.ifPresent(Main::setAction);
		}
		
		if (action == null && args.length != 0) {
			printUsage();
			System.exit(0);
		}
		
		if (action == null) {
			System.out.println("Using GUI");
			Gui.init();
		}
		
		if (action == null) System.exit(0);
		
		printHeader();
		
		try {
			System.out.println("Running action " + action.getName());
			action.run();
			System.out.println();
			System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.print("An unrecoverable error has occurred, terminating");
			System.in.read();
		}
	}
	
	private static void printUsage() {
		printHeader();
		System.out.print("Operates in working directory");
		System.out.print("Usage: ");
		
		boolean first = false;
		for (Action a : ACTIONS) {
			System.out.print("\"" + a.getName() + "\"");
			
			if (first) {
				first = false;
			} else {
				System.out.print(" or ");
			}
		}
		
		System.out.println();
	}

	private static void registerActions() {
		ACTIONS.add(new ActionInstall());
		ACTIONS.add(new ActionUpdate());
	}
	
	public static List<Action> getActions() {
		return ACTIONS;
	}

	public static void setAction(Action action) {
		System.out.println("Using action " + action.getName());
		Main.action = action;
	}
	
	public static void printHeader() {
		System.out.println("PIWCS Modpack Very Simple Installer and Updater version " + VERSION);
		System.out.println();
		System.out.println("Copyright 2020 Javapony (kvadropups@gmail.com)");
		System.out.println(NAME + " is licensed under GNU GPL v3-or-later. This is free software, and you are welcome to redistribute it");
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY. For details please refer to the license.");
		System.out.println();
	}

}
