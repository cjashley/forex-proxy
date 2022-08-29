package com.paidy.forex.proxy;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
/* https://github.com/valid4j/http-matchers */
import static org.valid4j.matchers.http.HttpResponseMatchers.hasEntity;
import static org.valid4j.matchers.http.HttpResponseMatchers.hasHeader;
import static org.valid4j.matchers.http.HttpResponseMatchers.hasStatus;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.paidy.forex.proxy.OneFrame.OneFrameCurrencyPairsException;
import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;

public class Forex10000Test {

	private HttpServer server;
	private WebTarget target;

	/**
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		// start the server
		server = Main.startServer();
		// create the client
		Client c = ClientBuilder.newClient();

		// uncomment the following line if you want to enable
		// support for JSON in the client (you also have to uncomment
		// dependency on jersey-media-json module in pom.xml and Main.startServer())
		// --
		// c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

		target = c.target(Main.BASE_URI);
	}

	@After
	public void tearDown() throws Exception {
		server.shutdownNow();
	}

	/**
	 * 
	 */
	@Test
	public void test10000RatesBadRequest()
	{
		for (int count=0; count<10000; count++)
		{
			Builder builder = target.path("api/rates").queryParam("pair","JPYNZD").request("application/json");
			String jsonStr = builder.get(String.class);

			StringReader stringReader = new StringReader(jsonStr);
			try (JsonReader jsonReader = Json.createReader(stringReader)) 
			{
				JsonStructure read = jsonReader.read(); // OneFrame returns data or error
				JsonArray array = read.asJsonArray();

				List<OneFrameRate> rates = new ArrayList<OneFrameRate>(array.size());

				for (int i=0; i<array.size();i++)
				{
					OneFrameRate rate = new OneFrameRate(array.getJsonObject(i));
					rates.add(rate);
					Assert.assertEquals("JPYNZD", rate.getCurrencyPair());
					System.out.println(count+" "+rate.getCurrencyPair()+" price="+rate.price+" timestamp="+rate.time_stamp);
				}
			}
		}
	}


}
