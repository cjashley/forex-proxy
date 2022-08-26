package com.paidy.forex.proxy;

import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.paidy.forex.proxy.OneFrame.OneFrameException;

public class OneFrame {

   
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

	final static Logger LOG = Logger.getLogger(OneFrame.class.getName());
	public static final int MAX_REQUESTS = 1000;  // per docker session 

	public List<OneFrameRate> getRate(@QueryParam("pair") String ... currencyPairs) throws OneFrameException
	{

		Client client = ClientBuilder.newClient();
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

}
