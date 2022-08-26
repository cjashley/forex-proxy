package com.paidy.forex.proxy;

import javax.ws.rs.WebApplicationException;

// TODO example WEB exception to make use when needed
// http://www.mastertheboss.com/jboss-frameworks/resteasy/how-to-handle-exceptions-in-jax-rs-applications/
// https://stackoverflow.com/questions/38971609/error-handling-in-rest-api-with-jax-rs
public class MyException extends WebApplicationException {
    public MyException() {
        super();
    }
    public MyException(String message) {
        super("MyException Error: " +message);
    }
}