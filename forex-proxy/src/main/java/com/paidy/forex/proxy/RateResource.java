package com.paidy.forex.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.Uri;

@Path("rate")
public class RateResource {

	
	@GET
	 @Produces(MediaType.APPLICATION_JSON)
    public String getIt() {
		
		Client client = ClientBuilder.newClient();
	    WebTarget webTarget = client.target("http://localhost:8080/rates?pair=USDJPY");
	    final Response response = webTarget.request().header("token", "10dc303535874aeccc86a8251e6992f5").get();
	    return response.readEntity(String.class);
	    
//	    [{"from":"NZD","to":"JPY","bid":0.6118225421857174,"ask":0.8243869101616611,"price":0.71810472617368925,"time_stamp":"2022-08-25T09:18:41.693Z"},
//	     {"from":"JPY","to":"NZD","bid":0.8435259660697864,"ask":0.4175532166907524,"price":0.6305395913802694, "time_stamp":"2022-08-25T09:18:41.693Z"}]
	    		
	}

}
