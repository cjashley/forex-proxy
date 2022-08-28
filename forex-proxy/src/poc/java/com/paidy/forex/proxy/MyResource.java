package com.paidy.forex.proxy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.server.Uri;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Got it!";
    }
    
//    @GET
//    @Produces(MediaType.TEXT_HTML)
//    @Path("rate")
//    public Response rate(@Uri("/rates?pair=USDJPY") WebTarget webTarget) {
//    	System.out.println("rate URI "+webTarget.getUri());
//        final Response response = webTarget.request().header("token", "10dc303535874aeccc86a8251e6992f5").get();
//        return Response.fromResponse(response).build();
//    }
   
}
