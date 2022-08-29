package com.paidy.forex.proxy;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
/* https://github.com/valid4j/http-matchers */
import static org.valid4j.matchers.http.HttpResponseMatchers.hasEntity;
import static org.valid4j.matchers.http.HttpResponseMatchers.hasHeader;
import static org.valid4j.matchers.http.HttpResponseMatchers.hasStatus;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ForexResourceTest {

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
	public void testRatesBadRequest()
	{
		Builder builder = target.path("api/rates").queryParam("pair","XXXXXXX").queryParam("pair","YYYZZZ").request("application/json");
		Response response = builder.get();
		System.out.println("Response="+builder.get(String.class));
		// Verify the response
		assertThat(response, hasEntity(equalTo("[{\"ask\":0.3,\"bid\":0.1,\"currencyPair\":\"ABCXWZ\",\"from\":\"ABC\",\"price\":0.2,\"timestamp\":\"2022-08-28T14:09:38.109160Z\",\"to\":\"XWZ\"}]")));
		assertThat(response, hasStatus(Status.OK));
		assertThat(response, hasHeader("Content-Type", equalTo("application/json")));
	}
	
	/**
	 * 
	 */
	@Test
	public void testRates()
	{
		Builder builder = target.path("api/rates").queryParam("pair","JPYNZD").queryParam("pair","JPYUSD").request("application/json");
		Response response = builder.get();
		System.out.println("Response="+builder.get(String.class));
//		// Verify the response
		assertThat(response, hasEntity(equalTo("[{\"ask\":0.3,\"bid\":0.1,\"currencyPair\":\"ABCXWZ\",\"from\":\"ABC\",\"price\":0.2,\"timestamp\":\"2022-08-28T14:09:38.109160Z\",\"to\":\"XWZ\"}]")));
		assertThat(response, hasStatus(Status.OK));
		assertThat(response, hasHeader("Content-Type", equalTo("application/json")));
	}
	
	/**
	 * 
	 */
	@Test
	public void testInvalidRate()
	{
		Builder builder = target.path("rate").queryParam("pair","XXX").request("application/json");
		Response response = builder.get();
		assertThat(response, hasEntity(equalTo("[{\"ask\":0.3,\"bid\":0.1,\"currencyPair\":\"ABCXWZ\",\"from\":\"ABC\",\"price\":0.2,\"timestamp\":\"2022-08-28T14:09:38.109160Z\",\"to\":\"XWZ\"}]")));

		String responseMsg = builder.get(String.class);
		System.out.println(response.getStatus()+" Response="+responseMsg);
		assertEquals("Got RateResource!", responseMsg);
	}
	
	/**
	 * 
	 */
	@Test
	public void testBuckets()
	{
		Builder builder = target.path("dev/buckets").request("text/plain");
		Response response = builder.get();
		System.out.println("Response="+builder.get(String.class));
//		// Verify the response
		assertThat(response, hasStatus(Status.OK));
		assertThat(response, hasHeader("Content-Type", equalTo("text/plain")));
	}
	
	/**
	 * 
	 */
	@Test
	public void tesConf()
	{
		Builder builder = target.path("dev/conf").request(MediaType.APPLICATION_JSON);
		Response response = builder.get();
		System.out.println("Response="+builder.get(String.class));
//		// Verify the response
		assertThat(response, hasStatus(Status.OK));
		assertThat(response, hasHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON)));
	}
}
