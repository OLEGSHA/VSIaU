package ru.windcorp.piwcs.vsiau;

public class AbortException extends Exception {

	private static final long serialVersionUID = -5428727885162568682L;

	public AbortException(String message) {
		super(message);
	}

	public AbortException(String message, Throwable cause) {
		super(message, cause);
	}

}
