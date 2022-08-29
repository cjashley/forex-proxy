package com.paidy.forex.proxy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.paidy.forex.proxy.OneFrame.OneFrameRate;

/**
 * Root resource (exposed at "rate" path)
 * 
 * 
 */
@Path("/")
public class ForexResourceService implements ForexResource {

	@Inject
	private RateSupplierService rateSupplierService;

	@Path("api/rates")
	@GET
	@Produces({MediaType.TEXT_PLAIN + "; charset=UTF-8", MediaType.APPLICATION_JSON + "; charset=UTF-8"})
	@Override
	public Response rates(@QueryParam("pair") String ...curencyPairs ) throws IOException, WebApplicationException
	{
		try
		{
			System.out.println("params "+ Arrays.asList(curencyPairs)+" to process");

			// Do our best to validate ccyPairs, fail fast, without wasting limited calls to OneFrame		
			for(String c : curencyPairs) { if (!CurrencyPair.validate.test(c)) throw new InvalidCurrencyPairException(c); }

			List<OneFrameRate> rates = new ArrayList<OneFrameRate>();
			for(String currencyPair : curencyPairs)
			{
				OneFrameRate rate = rateSupplierService.getRateSupplier().getRate(currencyPair);
				rates.add(rate);
			}

			return Response
					.status(Response.Status.OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(rates)
					.build();

		}
		catch (Throwable e)
		{
			// * 200 - OK <--- 
			// * 400 - Bad Request  
			// * 500 - Internal Server Error
			// TODO figure out why jax-rs standard exception handlers are not working and neither are attempts to use ExceptionMappers
			// Work around to construct own responses manually 

			return Response
					.status(Response.Status.OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST.getReasonPhrase(),e.getClass().getSimpleName(),e.getMessage()))
					.build();
		}
	}


	@Path("dev/buckets")
	@GET
	@Produces({MediaType.TEXT_PLAIN + "; charset=UTF-8", MediaType.APPLICATION_JSON + "; charset=UTF-8"})
	public Response bucketsReport() throws IOException, WebApplicationException
	{

		try
		{
			String[] report = rateSupplierService.getRateSupplier().getRateStreamsBucketReport();
			return Response
					.status(Response.Status.OK)
					.entity(String.join("\n", report))
					.type(MediaType.TEXT_PLAIN)
					.build();
		}
		catch (Throwable e)
		{
			// * 200 - OK <--- 
			// * 400 - Bad Request  
			// * 500 - Internal Server Error
			// TODO figure out why jax-rs standard exception handlers are not working and neither are attempts to use ExceptionMappers
			// Work around to construct own responses manually 

			return Response
					.status(Response.Status.OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST.getReasonPhrase(),e.getClass().getSimpleName(),e.getMessage()))
					.build();
		}
	}
	
	@Path("dev/conf")
	@GET
	@Produces({MediaType.TEXT_PLAIN + "; charset=UTF-8", MediaType.APPLICATION_JSON + "; charset=UTF-8"})
	public Response conf() throws IOException, WebApplicationException
	{
		try
		{
			return Response
					.status(Response.Status.OK)
					.entity(rateSupplierService.getConfig())
					.type(MediaType.APPLICATION_JSON)
					.build();
		}
		catch (Throwable e)
		{
			// * 200 - OK <--- 
			// * 400 - Bad Request  
			// * 500 - Internal Server Error
			// TODO figure out why jax-rs standard exception handlers are not working and neither are attempts to use ExceptionMappers
			// Work around to construct own responses manually 

			return Response
					.status(Response.Status.OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(),Response.Status.BAD_REQUEST.getReasonPhrase(),e.getClass().getSimpleName(),e.getMessage()))
					.build();
		}
	}
	

	@Path("test/numbers")
	@GET
	public Response streamExample(){
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				Writer writer = new BufferedWriter(new OutputStreamWriter(out));
				for (int i = 0; i < 10000000 ; i++){
					writer.write(i + " ");
				}
				writer.flush();
			}
		};
		return Response.ok(stream).build();
	}

}

