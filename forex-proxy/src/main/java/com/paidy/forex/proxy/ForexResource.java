package com.paidy.forex.proxy;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Interface for the REST resource representing forex
 */
public interface ForexResource 
{
	
	/**
	 * Gets one or more rates 
	 * @param currencyPairs list of two 3 char currency pairs e.g. NZDJPY" 
	 * @return
	 */
	@Path("rates")
	@GET
    @Produces({MediaType.TEXT_PLAIN + "; charset=UTF-8", MediaType.APPLICATION_JSON + "; charset=UTF-8"})
	public Response rates(@QueryParam("pair") String ... currencyPairs ) throws IOException, WebApplicationException;

}
