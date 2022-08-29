package com.paidy.forex.proxy;

import java.io.Serializable;

public class ErrorResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	public final int statusCode;
	public final String reasonPhrase;
	public final String exception;
	public final String message;
	

	public ErrorResponse(int statusCode, String reasonPhrase, String exception, String message) {
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
		this.exception = exception;
		this.message = message;
	}

}
