package com.paidy.forex.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.paidy.forex.proxy.OneFrame.RatesStreamThread;

public class OneFrame {

	final static Logger LOG = Logger.getLogger(OneFrame.class.getName());

	{
		LOG.setLevel(Level.ALL);
	}
	private static final String TOKEN = "10dc303535874aeccc86a8251e6992f5";
	public static final String ONE_FRAME_URI = "http://localhost:8080/";

	private AtomicInteger requestCount = new AtomicInteger();  // we only get 1,000 per day with a docker instant (may be restarted)

	public static class OneFrameException extends Exception
	{
		OneFrameException(String message)
		{
			super(message);
		}

		OneFrameException(String message, Throwable e)
		{
			super(message,e);
		}
	}

	public static class OneFrameRate
	{
		public final String from;
		public final String to;
		public final double bid;
		public final double ask;
		public final double price;
		public final Instant timestamp; //  "2022-08-25T13:40:52.559Z" 'Z' for Zulu and specifies the Etc/UTC timezone (which has the timezone offset of +00:00 hours).

		public OneFrameRate(JsonObject obj) {
			this.from  = obj.getString("from");
			this.to    = obj.getString("to");
			this.bid   = obj.getJsonNumber("bid").doubleValue();
			this.ask   = obj.getJsonNumber("ask").doubleValue();
			this.price = obj.getJsonNumber("price").doubleValue();
			this.timestamp = Instant.parse(obj.getString("time_stamp"));
		}

		public OneFrameRate(String from,String to, double bid, double ask, double price, Instant timestamp) 
		{
			this.from = from;
			this.to = to;
			this.bid = bid;
			this.ask = ask;
			this.price = price;
			this.timestamp = timestamp;
		}
		
		public String getPair() { return from+to; }

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			return sb.append("[").append(from)
					.append(',').append(to)
					.append(',').append(bid)
					.append(',').append(ask)
					.append(',').append(price)
					.append(',').append(timestamp)
					.append(']').toString();
		}
	}

	public static final int MAX_REQUESTS = 1000;  // per docker session 



	public class RatesStreamThread implements Runnable {

		private Thread worker;
		private final AtomicBoolean running = new AtomicBoolean(true); // it is considered running is true through init phase,
		private final AtomicInteger consumeCount = new AtomicInteger(0); 

		private Response response;
		private Consumer<? super OneFrameRate> consumer;
		private String[] currencyPairs;

		public RatesStreamThread(String [] currencyPairs, Response response, Consumer<? super OneFrameRate> consumer) 
		{
			this.currencyPairs = currencyPairs;
			this.response = response;
			this.consumer = consumer;
		}

		public void start() {
			worker = new Thread(this,String.join(",", currencyPairs));
			worker.start();
		}

		public void safeStop() {
			running.set(false);
		}

		public void run() { 
			System.out.println("run()"); System.out.flush();

			running.set(true);
			System.out.println("run() "+running.get()); System.out.flush();

			try ( InputStream is = (InputStream)response.readEntity(InputStream.class) )
			{
				try (Scanner s = new Scanner(is).useDelimiter(String.join("|","\\[\\{","\\}\\,\\{", "\\}\\]"))) // [{ or },{ or }]
				{
					System.out.println("while(running,get() "+running.get()); System.out.flush();

					while (running.get()) {

						String line = s.next();
						if (!line.isBlank())  // line will be empty in between each data sequence [{,,,}][{,,}] 
						{
							line = "{"+ line + "}"; 

							System.out.println(line); System.out.flush();

							// LOG.log(Level.INFO,line);

							StringReader stringReader = new StringReader(line);
							try (JsonReader jsonReader = Json.createReader(stringReader)) 
							{
								OneFrameRate rate = new OneFrameRate(jsonReader.readObject());
								//								System.out.println("OneFrameRate "+rate); System.out.flush();
								consumer.accept(rate);
								consumeCount.getAndIncrement();
							}
						}
					}
				}

			} catch (IOException e1) 
			{
				System.out.println("input srtream error "+e1);
			}
			finally
			{
				response.close();
			}

		}

		public boolean isRunning() {
			return running.get();
		}

		public int getConsumeCount() {
			return consumeCount.get();
		}

		public String getName() {
			return worker.getName();
		}

	} 

	/**
	 * 
	 * @param consumer
	 * @param currencyPairs
	 * @return started streamThread 
	 * @throws OneFrameException
	 */
	public void streamRates(@QueryParam("pair") String ... currencyPairs) throws OneFrameException
	{
		//		curl --no-buffer -s -H "token: 10dc303535874aeccc86a8251e6992f5" 'localhost:8080/streaming/rates?pair=USDJPY'
		//
		//		[{"from":"USD","to":"JPY","bid":0.0020265114170565646,"ask":0.9851988475245471,"price":0.4936126794708018323,"time_stamp":"2022-08-25T01:00:40.422Z"}][{"from":"USD","to":"JPY","bid":0.3007248195549609,"ask":0.6341746179647224,"price":0.46744971875984165,"time_stamp":"2022-08-25T01:00:39.42Z"}]

		final Client client = ClientBuilder.newClient();
		WebTarget webTarget = client.target(ONE_FRAME_URI+ "streaming/rates");
		for(String ccyPair : currencyPairs)
		{
			webTarget = webTarget.queryParam("pair", ccyPair);
		}

		final Response response = webTarget.request().header("token", TOKEN).get();

		LOG.log(Level.INFO,"streamRates "+ response.getStatus()+" "+webTarget.getUri());

		if (response.getStatus() != 200)
			throw new OneFrameException(response.getStatus()+" "+response.getStatusInfo()+" from OneFrame get "+webTarget.getUri().toString());

		try ( InputStream is = (InputStream)response.readEntity(InputStream.class) )
		{
			try (Scanner s = new Scanner(is).useDelimiter(String.join("|","\\[\\{","\\{\\,\\}", "\\}\\]"))) // [{ or },{ or }]
			{
				String line;

				while ((line = s.next()) != null)
				{
					line = "{"+line+"}"; // put the curly brackets back on prior to json parsing

					//					LOG.log(Level.INFO,line);
					System.out.println(line);
					System.out.flush();

					StringReader stringReader = new StringReader(line);
					try (JsonReader jsonReader = Json.createReader(stringReader)) 
					{
						OneFrameRate rate = new OneFrameRate(jsonReader.readObject());
						//						System.out.println("OneFrameRate "+rate);
					}
				}

			}
		} catch (IOException e1) 
		{
			throw new OneFrameException("input srtream error ",e1);
		}
		finally
		{
			response.close();
		}

	}

	public RatesStreamThread streamRates(Consumer<? super OneFrameRate> consumer, @QueryParam("pair") String ... currencyPairs) throws OneFrameException
	{
		//		curl --no-buffer -s -H "token: 10dc303535874aeccc86a8251e6992f5" 'localhost:8080/streaming/rates?pair=USDJPY'
		//
		//		[{"from":"USD","to":"JPY","bid":0.0020265114170565646,"ask":0.9851988475245471,"price":0.4936126794708018323,"time_stamp":"2022-08-25T01:00:40.422Z"}][{"from":"USD","to":"JPY","bid":0.3007248195549609,"ask":0.6341746179647224,"price":0.46744971875984165,"time_stamp":"2022-08-25T01:00:39.42Z"}]

		final Client client = ClientBuilder.newClient();
		WebTarget webTarget = client.target(ONE_FRAME_URI+ "streaming/rates");
		for(String ccyPair : currencyPairs)
		{
			webTarget = webTarget.queryParam("pair", ccyPair);
		}

		final Response response = webTarget.request().header("token", TOKEN).get();

		LOG.log(Level.INFO,"streamRates "+ response.getStatus()+" "+webTarget.getUri());

		if (response.getStatus() != 200)
			throw new OneFrameException(response.getStatus()+" "+response.getStatusInfo()+" from OneFrame get "+webTarget.getUri().toString());


		RatesStreamThread streamThread = new RatesStreamThread(currencyPairs,response,consumer);

		streamThread.start();

		return streamThread;
	}
	/*

  StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));

                for (org.neo4j.graphdb.Path path : paths) {
                    writer.write(path.toString() + "\n");
                }
                writer.flush();
            }
        };

        return Response.ok(stream).build();
	 */


	public List<OneFrameRate> getRate(@QueryParam("pair") String ... currencyPairs) throws OneFrameException
	{
		Client client = null;

		try
		{
			client = ClientBuilder.newClient(); // TODO this is a heavy weight object

			WebTarget webTarget = client.target(ONE_FRAME_URI+ "rates");
			for(String ccyPair : currencyPairs)
			{
				webTarget = webTarget.queryParam("pair", ccyPair);
			}

			final Response response = webTarget.request().header("token", TOKEN).get();

			//See Response.Status.OK;
			if (response.getStatus() == 200) 
			{
				requestCount.getAndIncrement();

				String jsonStr = response.readEntity(String.class);
				StringReader stringReader = new StringReader(jsonStr);
				try (JsonReader jsonReader = Json.createReader(stringReader)) 
				{
					JsonStructure read = jsonReader.read(); // OneFrame returns data or error
					switch (read.getValueType())
					{
					case OBJECT:
						//  handling {"error":"No currency pair provided"}
						//  handling {"error":"Quota reached"}

						LOG.log(Level.INFO,"request "+webTarget.getUri().toString());
						LOG.log(Level.INFO,"response jsonStr="+jsonStr);


						JsonObject errorObj = read.asJsonObject();

						String errorMsg = errorObj.getString("error");

						throw new OneFrameException(errorMsg);


					case ARRAY:
						//	    [{"from":"NZD","to":"JPY","bid":0.6118225421857174,"ask":0.8243869101616611,"price":0.71810472617368925,"time_stamp":"2022-08-25T09:18:41.693Z"},
						//	     {"from":"JPY","to":"NZD","bid":0.8435259660697864,"ask":0.4175532166907524,"price":0.6305395913802694, "time_stamp":"2022-08-25T09:18:41.693Z"}]
						JsonArray array = read.asJsonArray();
						LOG.log(Level.INFO,"JSON array="+array);

						List<OneFrameRate> rates = new ArrayList<OneFrameRate>(array.size());

						for (int i=0; i<array.size();i++)
						{
							rates.add(new OneFrameRate(array.getJsonObject(i)));
						}

						return rates;

					default:
						LOG.log(Level.INFO,"request "+webTarget.getUri().toString());
						LOG.log(Level.INFO,"response jsonStr="+jsonStr);
						throw new OneFrameException("OneFrame returned unexpected valueType:"+read.getValueType());
					}
				}
			}
			throw new OneFrameException(response.getStatus()+" "+response.getStatusInfo()+" from OneFrame get "+webTarget.getUri().toString());
		}
		finally
		{
			client.close();
		}


	}

}

