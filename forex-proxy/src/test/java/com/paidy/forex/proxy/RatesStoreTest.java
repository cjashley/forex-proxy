package com.paidy.forex.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import com.paidy.forex.proxy.*;
import com.paidy.forex.proxy.RatesStore.Rate;
import com.paidy.forex.proxy.RatesStore.Singleton;
public class RatesStoreTest {

	@Test
	public void currencyPairTest()
	{
		try {
			new RatesStore.CurrencyPair("ABC");
		}
		catch (IllegalArgumentException e) {}
		
		RatesStore.CurrencyPair pair = new RatesStore.CurrencyPair("JPYNZD");
		
		Assert.assertEquals("JPY", pair.getFrom());
		Assert.assertEquals("NZD", pair.getTo());

	}
	
	@Test
	public void rateSingletonTest()
	{
		final RatesStore.CurrencyPair JPYNZD = new RatesStore.CurrencyPair("JPYNZD");
		assertNull(Singleton.INSTANCE.rates.findRate(JPYNZD));
		
		Rate rateJPYNZD = randomRate();
		Singleton.INSTANCE.rates.addRate(JPYNZD,rateJPYNZD);
		Rate found = Singleton.INSTANCE.rates.findRate(JPYNZD);
		
		assertTrue(rateJPYNZD+" = "+found, rateJPYNZD.equals(found));		
		assertEquals(rateJPYNZD, found);
	}
	
	Rate randomRate()
	{
		double bid = Math.random();
		double ask = bid+Math.random()/100.0; 
		double price = (ask-bid) /2;
		return new Rate(bid,ask,price,Instant.now());
	}
}
