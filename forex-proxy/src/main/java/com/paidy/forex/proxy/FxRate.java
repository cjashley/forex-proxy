package com.paidy.forex.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.Uri;

@Path("fxrate")
public class FxRate {

	
	@GET
	 @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Got rate!";
    }

}
