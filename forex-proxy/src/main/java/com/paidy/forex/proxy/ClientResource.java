package com.paidy.forex.proxy;
/**
 * A resource which use managed client injected by {@link org.glassfish.jersey.server.Uri @Uri annotation} to query
 * external resources and resource from {@link StandardResource}.
 *
 * @author Miroslav Fuksa
 *
 */

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.Uri;

// https://github.com/eclipse-ee4j/jersey/blob/2.36/examples/managed-client-simple-webapp/src/main/java/org/glassfish/jersey/examples/managedclientsimple/resources/ClientResource.java


@Path("client")
public class ClientResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("clientrate")
    public Response rate(@Uri("http://localhost:8080/rates?pair=USDJPY") WebTarget webTarget) {
    	System.out.println("rate URI "+webTarget.getUri());
        final Response response = webTarget.request().header("token", "10dc303535874aeccc86a8251e6992f5").get();
        return Response.fromResponse(response).build();
    }

    /**
     * Make request to external web site using injected client. The response from the injected client is then
     * returned as a response from this resource method.
     *
     * @param webTarget Injected web target.
     * @return Response.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("glassfish")
    public Response glassfish(@Uri("http://glassfish.java.net/") WebTarget webTarget) {
        final Response response = webTarget.request().get();
        return Response.fromResponse(response).build();
    }

    
    
    /**
     * Query {@link StandardResource} and return result based on the results from methods of the {@link StandardResource}.
     *
     * @param dogWebTarget Injected client.
     * @param catWebTarget Injected client.
     * @param elefantWebTarget Injected client.
     * @return String entity.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("animals")
    public String animals(@Uri("resource/dog") WebTarget dogWebTarget,
                          @Uri("resource/cat") WebTarget catWebTarget,
                          @Uri("resource/elefant") WebTarget elefantWebTarget) {

        final String dog = dogWebTarget.request().get(String.class);
        final String cat = catWebTarget.request().get(String.class);
        final String elefant = elefantWebTarget.request().get(String.class);
        return "Queried animals: " + dog + " and " + cat + " and " + elefant;
    }

    /**
     * Query {@link StandardResource} using a injected client. The client injection is using a template parameter {@code id}
     * which is filled by JAX-RS implementation using a path parameter of this resource method.
     *
     * @param webTarget Injected client.
     * @param id Path parameter.
     * @return String entity.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("car/{id}")
    public String car(@Uri("resource/car/{id}") WebTarget webTarget, @PathParam("id") String id) {
        final Response response = webTarget.request().get();
        return "Response from resource/car/" + id + ": " + response.readEntity(String.class);
    }
}
