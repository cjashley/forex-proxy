package com.paidy.forex.proxy;


import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.junit.Assert; 
import org.junit.Test;

import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.RateSupplier.Params;

public class RateSupplierTest {

	OneFrame oneFrame = new OneFrame();

	@Test
	public void currencyInvalidTest() throws OneFrameException, InvalidCurrencyPairException, StaleRateException 
	{
		RateSupplier rateSupplier = new RateSupplier(oneFrame, (Predicate<OneFrameRate>)null, (Predicate<Instant>)null, new Params());
	
		try {
			rateSupplier.getRate("XXYZZZ");
		Assert.fail();
		} catch (InvalidCurrencyPairException e) 
		{
			Assert.assertArrayEquals(new String[] {"XXYZZZ"},e.getCurrencyPairs());
		}
	}

	@Test
	public void oneCurrencyAlwayReadStaleTest() throws OneFrameException, InvalidCurrencyPairException, StaleRateException 
	{
		Predicate<OneFrameRate> isRateStale_true = t-> true; // always  state
		Predicate<Instant> isReadStale_true = i -> true; // always read stale, i.e. no streaming is ever done
		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStale_true, isReadStale_true, new Params());
	
		OneFrameRate rate = rateSupplier.getRate("GBPJPY");
		Assert.assertEquals("GBPJPY", rate.getCurrencyPair());
		
		// get the rate again, it will be stale, because of our always stale function
		try {
			rateSupplier.getRate("GBPJPY");
		Assert.fail();
		} catch (StaleRateException e) 
		{
			Assert.assertEquals("GBPJPY",e.getCurrencyPair());
		}
	}
	
	/**
	 * Test that the streaming for a currency starts up and stops 
	 * @throws InterruptedException 
	 */
	@Test
	public void oneCurrencyStreamingStartStopTest() throws OneFrameException, InvalidCurrencyPairException, StaleRateException, InterruptedException 
	{
		AtomicBoolean isRateStale = new AtomicBoolean(false);
		AtomicBoolean isReadStale = new AtomicBoolean(false);
		
		Predicate<OneFrameRate> isRateStaleFunc = t -> isRateStale.get();
		Predicate<Instant> isReadStaleFunc      = i -> isReadStale.get(); 
		Params params = new Params();
		params.fetchOptimiseInitialDelay = 100;
		params.fetchOptimiseSechedulPeriod = 100;
		
		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStaleFunc, isReadStaleFunc, params);
	
		OneFrameRate rate = rateSupplier.getRate("GBPJPY");
		Assert.assertEquals("GBPJPY", rate.getCurrencyPair());
		
		OneFrameRate updatedRate = null; 
		for (int i=0; i < 5; i++)
		{
			OneFrameRate rereadRate = rateSupplier.getRate("GBPJPY");
			Thread.currentThread().sleep(1000); // OneFrame will not respond quickly with a new rate
			if (!rate.equals(rereadRate))
			{ updatedRate = rereadRate; break; }
		}

		Assert.assertNotNull(updatedRate);
		
		
	}
}
