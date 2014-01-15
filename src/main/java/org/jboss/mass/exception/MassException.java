package org.jboss.mass.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings("serial")
public class MassException extends Exception {
	
	private String message;
	private String stacktrace;
	
	public MassException(String message, Exception e) {
		super(message);
		this.message = message;
		setStackTrace(e);

	}
	
	public MassException(Exception e) {
		setStackTrace(e);
	}
	
	public String getTrace() {
		return stacktrace;
	}
	
	private void setStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		stacktrace = sw.toString();
	}

}
