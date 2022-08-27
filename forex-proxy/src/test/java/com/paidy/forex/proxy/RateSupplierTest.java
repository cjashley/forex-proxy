package com.paidy.forex.proxy;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
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
		RateSupplier rateSupplier = new RateSupplier(oneFrame, (Predicate<OneFrameRate>)null, (BiPredicate<String,Instant>)null, new Params());

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
		BiPredicate<String,Instant> isReadStaleFunc  = (s,i) -> { return true;}; // always read stale, i.e. no streaming is ever done

		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStale_true, isReadStaleFunc, new Params());

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
		List<String> isReadStaleFor = new ArrayList<String>();

		Predicate<OneFrameRate> isRateStaleFunc = t -> isRateStale.get();
		BiPredicate<String,Instant> isReadStaleFunc  = (s,i) -> { return isReadStaleFor.contains(s); };
		Params params = new Params();
		params.fetchOptimiseInitialDelay = 100;
		params.fetchOptimiseSechedulPeriod = 100;

		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStaleFunc, isReadStaleFunc, params);

		Assert.assertTrue(rateSupplier.getRatesStreamCurrecnyPairs().isEmpty());

		OneFrameRate firstRate = rateSupplier.getRate("GBPJPY");
		Assert.assertEquals("GBPJPY", firstRate.getCurrencyPair());

		// Test stream is providing a new rate
		OneFrameRate updatedGBPJPY = fetchUpdatedRate(rateSupplier, firstRate);
		Assert.assertNotNull(updatedGBPJPY);
		Assert.assertFalse(rateSupplier.getRatesStreamCurrecnyPairs().isEmpty());
		Assert.assertArrayEquals(new String[] {"GBPJPY"}, rateSupplier.getRatesStreamCurrecnyPairs().get());

		// Test add a second rate request, supply should drop the first stream for a 
		// new one with the two rates 
		{
			OneFrameRate secondRate = rateSupplier.getRate("NZDJPY");
			Assert.assertEquals("NZDJPY", secondRate.getCurrencyPair());

			OneFrameRate updatedNZDJPY = fetchUpdatedRate(rateSupplier, secondRate);
			Assert.assertNotNull(updatedNZDJPY);

			// Test that the first rate is still being streamed and updated
			OneFrameRate updatedNZDJPYagain = fetchUpdatedRate(rateSupplier, updatedGBPJPY);
			Assert.assertNotNull(updatedNZDJPYagain);

			// Test streaming is happening for both ccyPairs
			{
				List<String> expected = List.of("GBPJPY","NZDJPY");
				List<String> actual = Arrays.asList(rateSupplier.getRatesStreamCurrecnyPairs().get());
				Assert.assertTrue(expected.containsAll(actual));
			}

			// Test making all rates stale one by one and causing the streaming to completely stop
			{
				isReadStaleFor.add("GBPJPY");
				{
					List<String> expected = List.of("NZDJPY"); // NOT stale
					List<String> actual = rateSupplier.readNotStaleCurrencyPairs();
					Assert.assertArrayEquals(expected.toArray(), actual.toArray());
				}

				Thread.currentThread().sleep(params.fetchOptimiseSechedulPeriod*2); // Give time for the stream to be reduced

				// Test streaming stopped for GBPJPY
				{
					List<String> expected = List.of("NZDJPY");
					List<String> actual = Arrays.asList(rateSupplier.getRatesStreamCurrecnyPairs().get());
					Assert.assertArrayEquals(expected.toArray(), actual.toArray());
				}	 

				isReadStaleFor.add("NZDJPY");
				{
					List<String> expected = Collections.emptyList(); // NOT stale
					List<String> actual = rateSupplier.readNotStaleCurrencyPairs();
					Assert.assertArrayEquals(expected.toArray(), actual.toArray());
				}
				
				Thread.currentThread().sleep(params.fetchOptimiseSechedulPeriod*2); // Give time for the stream to be reduced

				// Test streaming stopped for both GBPJPY and NZDJPY
				Assert.assertTrue("streaming pairs "+rateSupplier.getRatesStreamCurrecnyPairs(),rateSupplier.getRatesStreamCurrecnyPairs().isEmpty());

			}

		}

	}

	private OneFrameRate fetchUpdatedRate(RateSupplier rateSupplier, OneFrameRate rate)
			throws OneFrameException, InvalidCurrencyPairException, StaleRateException, InterruptedException {
		OneFrameRate updatedRate = null; 
		for (int i=0; i < 5; i++)
		{
			OneFrameRate rereadRate = rateSupplier.getRate("GBPJPY");
			Thread.currentThread().sleep(1000); // OneFrame will not respond quickly with a new rate
			if (!rate.equals(rereadRate))
			{ updatedRate = rereadRate; break; }
		}
		return updatedRate;
	}
}
