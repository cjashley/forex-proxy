package com.paidy.forex.proxy;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.paidy.forex.proxy.Main;
import com.paidy.forex.proxy.OneFrame;
import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.RatesStore.CurrencyPair;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

public class ResourceTest {

	private HttpServer server;
	private WebTarget target;

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
	 * Test to see that the message "Got it!" is sent in the response.
	 */
	@Test
	public void testGetIt() {
		
		String responseMsg = target.path("myresource").request().get(String.class);
		System.out.println("Response="+responseMsg);
		
		assertEquals("Got it!", responseMsg);
	}

	@Test
	public void testRateResource()
	{
		String responseMsg = target.path("rate").queryParam("pair","USDJPY").queryParam("pair","GBPJPY").request("application/json").get(String.class);
		System.out.println("Response="+responseMsg);
		assertEquals("Got RateResource!", responseMsg);
	}
	
}
