package com.paidy.forex.proxy;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

import com.paidy.forex.proxy.RatesStore.Rate;

public class RatesStore {

	public enum Singleton
	{
		INSTANCE;

		RatesStore rates;

		Singleton()
		{
			rates = new RatesStore();
		}
	}

	public static class CurrencyPair
	{
		final String currencyPair;

		CurrencyPair(String currencyPair) 
		{
			if (currencyPair.length() != 6) throw new IllegalArgumentException("Currency pair len "+currencyPair.length()+"!= 6 i.e. two 3 char pairs e.g. NZDJPY");
			this.currencyPair = currencyPair;
		}

		String getFrom() { return currencyPair.substring(0,3);}
		String getTo() { return currencyPair.substring(3); }

		@Override
		public String toString() 
		{
			return currencyPair;
		}

		@Override
		public boolean equals( Object b)
		{
			return currencyPair.equals(b);
		}

		@Override
		public int hashCode()
		{
			return currencyPair.hashCode();
		}
	}

	static class Rate
	{
		final double bid;
		final double ask;
		final double price;
		final Instant timestamp;

		Rate(double bid, double price, double ask, Instant timeStamp)
		{
			this.bid = bid;
			this.ask = ask;
			this.price = price;
			this.timestamp = timeStamp;
		}
		
		boolean isStale(Duration duration, Instant now)
		{
			Duration d = Duration.between(timestamp, now);
			return d.compareTo(duration) >= 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			
			if (obj instanceof Rate)
			{
				Rate rate = (Rate) obj;
				
				return (    bid == rate.bid 
						&&  ask == rate.ask
						&&  price == rate.price
				        &&  timestamp.equals(rate.timestamp));
			}
			
			return false;
		}
		
		@Override
		public int hashCode() {
			
			 throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			return sb.append("[").append(bid)
					.append(',').append(ask)
					.append(',').append(price)
					.append(',').append(timestamp)
					.append(']').toString();
		}
		
		public static Rate randomRate()
		{
			double bid = Math.random();
			double ask = bid+Math.random()/100.0; 
			double price = (ask-bid) /2;
			return new Rate(bid,ask,price,Instant.now());
		}
	}



	HashMap<CurrencyPair,Rate> rates = new HashMap<>();

	/**
	 * 
	 * @param pair to find rate for
	 * @return rate matching pair or null if not found 
	 */
	Rate findRate(CurrencyPair pair)
	{
		return rates.get(pair);
	}
	
	/**
	 * 
	 * @param pair
	 * @param rate
	 * @return previous mapping of rate or null if none
	 */
	Rate addRate(CurrencyPair pair, Rate rate)
	{
		return rates.put(pair, rate);
	}

	/**
	 *  Clears the rate store of all rates
	 */
	void clear()
	{
		rates.clear();
	}

}
