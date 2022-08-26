package com.paidy.forex.proxy;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class LoggingFilter implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        LOG.log(Level.INFO, requestContext.getEntity().toString());
    }
}
