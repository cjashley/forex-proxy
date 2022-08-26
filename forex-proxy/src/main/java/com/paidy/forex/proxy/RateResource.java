package com.paidy.forex.proxy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.server.Uri;

import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.RatesStore.CurrencyPair;
import com.paidy.forex.proxy.RatesStore.Rate;

/**
 * Root resource (exposed at "rate" path)
 */
@Path("/")
public class RateResource {

	@Path("rate")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<OneFrameRate> rate(@QueryParam("pair") String ...ccyPairs ) 
	{
		System.out.println("params "+ Arrays.asList(ccyPairs));

		List<OneFrameRate> rates = new ArrayList<OneFrameRate>();
		for(String cc : ccyPairs)
		{
			rates.add(makeOneFrameRate(new CurrencyPair(cc), Rate.randomRate()));
		}
		return rates;
	}

	OneFrameRate makeOneFrameRate(CurrencyPair ccPair, Rate rate)
	{
		return new OneFrameRate(
				ccPair.getFrom()
				,ccPair.getTo()
				,rate.bid
				,rate.ask
				,rate.price
				,rate.timestamp);
	}


	@Path("numbers")
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
