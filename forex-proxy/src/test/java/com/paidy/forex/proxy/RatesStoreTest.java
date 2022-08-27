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
			new CurrencyPair("ABC");
		}
		catch (IllegalArgumentException e) {}
		
		CurrencyPair pair = new CurrencyPair("JPYNZD");
		
		Assert.assertEquals("JPY", pair.getFrom());
		Assert.assertEquals("NZD", pair.getTo());

	}
	
	@Test
	public void rateSingletonTest()
	{
		final CurrencyPair JPYNZD = new CurrencyPair("JPYNZD");
		assertNull(Singleton.INSTANCE.rates.findRate(JPYNZD));
		
		Rate rateJPYNZD = Rate.randomRate();
		Singleton.INSTANCE.rates.addRate(JPYNZD,rateJPYNZD);
		Rate found = Singleton.INSTANCE.rates.findRate(JPYNZD);
		
		assertTrue(rateJPYNZD+" = "+found, rateJPYNZD.equals(found));		
		assertEquals(rateJPYNZD, found);
	}
	
	
}
