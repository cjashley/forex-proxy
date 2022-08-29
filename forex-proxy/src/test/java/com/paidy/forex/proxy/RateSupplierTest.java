package com.paidy.forex.proxy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.paidy.forex.proxy.OneFrame.OneFrameCurrencyPairsException;
import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.RateSupplier.Config;
import com.paidy.forex.proxy.RateSupplier.FetchOptimiseTask;
import com.paidy.forex.proxy.RateSupplier.IsRateStale;
import com.paidy.forex.proxy.RateSupplier.IsReadStale;
import com.paidy.forex.proxy.RateSupplier.Config;

class RateSupplierTest {

	private OneFrame oneFrame = new OneFrame();

	@Test
	void currencyInvalidTest() throws OneFrameException, OneFrameCurrencyPairsException, StaleRateException 
	{
		RateSupplier rateSupplier = new RateSupplier(oneFrame,null,null,new Config());

		try
		{
			rateSupplier.getRate("XXXYYY");
			Assert.fail();
		}
		catch(InvalidCurrencyPairException e)
		{
			Assert.assertEquals("XXXYYY", e.getCurrencyPair());
		}

	}

	@Test
	public void oneCurrencyAlwayReadStaleTest() throws OneFrameException,  StaleRateException, InvalidCurrencyPairException 
	{
		Predicate<OneFrameRate> isRateStale_true = t-> true; // always  state
		BiPredicate<String,Instant> isReadStaleFunc  = (s,i) -> { return true;}; // always read stale, i.e. no streaming is ever done
		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStale_true, isReadStaleFunc, new Config());

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
	 * Test that the streaming for a currency starts up and stops with just one bucket, simplest case
	 * @throws InterruptedException 
	 * @throws InvalidCurrencyPairException 
	 */
	@Test
	public void onebucketTest() throws OneFrameException, StaleRateException, InterruptedException, InvalidCurrencyPairException 
	{
		AtomicBoolean isRateStale = new AtomicBoolean(false);
		List<String> isReadStaleFor = new ArrayList<String>();

		Predicate<OneFrameRate> isRateStaleFunc = t -> isRateStale.get();
		BiPredicate<String,Instant> isReadStaleFunc  = (s,i) -> { return isReadStaleFor.contains(s); };
		Config config = new Config();
		config.fetchOptimiseInitialDelay = 100;
		config.fetchOptimiseSechedulPeriod = 100;
		config.countDownToRunOptimization = 0;
		config.numOfRatesPerSupplier = 1;  // start off SIMPLE 

		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStaleFunc, isReadStaleFunc, config);

		try
		{
			// Confirm buckets are empty at start
			{
				String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"empty");
				String[] expected = new String[] { "1" };
				Assert.assertArrayEquals(expected, actual);
			}

			OneFrameRate firstRate = rateSupplier.getRate("GBPJPY");
			Assert.assertEquals("GBPJPY", firstRate.getCurrencyPair());

			// Test stream is providing a new rate
			OneFrameRate updatedGBPJPY = Helper.fetchUpdatedRate(rateSupplier, firstRate);
			Assert.assertNotNull(updatedGBPJPY);
			rateSupplier.getRateStreamsBucketReport();

			// Confirm buckets has GBPJPT
			{
				String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"GBPJPY");
				String[] expected = new String[] { "1 [GBPJPY]" };
				Assert.assertArrayEquals(expected, actual);
			}

			// Test add a second rate request, supply should drop the first stream for a 
			// new one with the two rates 
			{
				OneFrameRate secondRate = rateSupplier.getRate("NZDJPY");
				Assert.assertEquals("NZDJPY", secondRate.getCurrencyPair());

				OneFrameRate updatedNZDJPY = Helper.fetchUpdatedRate(rateSupplier, secondRate);
				Assert.assertNotNull(updatedNZDJPY);

				// Test that the first rate is still being streamed and updated
				OneFrameRate updatedNZDJPYagain = Helper.fetchUpdatedRate(rateSupplier, updatedGBPJPY);
				Assert.assertNotNull(updatedNZDJPYagain);

				// Confirm buckets have both GBPJPT NZDJPY
				{
					String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"GBPJPY NZDJPY");
					String[] expected = new String[] { "1 [GBPJPY],[NZDJPY]" };
					Assert.assertArrayEquals(expected, actual);
				}
				// Test making all rates stale one by one and causing the streaming to completely stop
				{
					isReadStaleFor.add("GBPJPY");
					Thread.sleep(config.fetchOptimiseSechedulPeriod*2); // Give time for the stream to be reduced

					// Confirm buckets have only NZDJPY i.e. GBPY streaming stopped
					{
						String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"stopped GBPJPY");
						String[] expected = new String[] { "1 [NZDJPY]" };
						Assert.assertArrayEquals(expected, actual);
					}

					isReadStaleFor.add("NZDJPY");
					Thread.sleep(config.fetchOptimiseSechedulPeriod*2); // Give time for the stream to be reduced

					// Confirm buckets are empty
					{
						String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"stopped all");
						String[] expected = new String[] { "1" };
						Assert.assertArrayEquals(expected, actual);
					}
				}
			}
		}
		finally
		{
			Helper.print(rateSupplier.currencyPairsWithNonStaleReads(), "finally toKeepOrStart report");
			Helper.print(rateSupplier.getRateStreamsBucketReport(),"finally bucket report");
		}
	}



	/**
	 * Test that the streaming for a currency starts up and stops 
	 * @throws InterruptedException 
	 * @throws InvalidCurrencyPairException 
	 */
	@Test
	public void twobucketsTest() throws OneFrameException, StaleRateException, InterruptedException, InvalidCurrencyPairException 
	{
		AtomicBoolean isRateStale = new AtomicBoolean(false);
		List<String> isReadStaleFor = new ArrayList<String>();

		Predicate<OneFrameRate> isRateStaleFunc = t -> isRateStale.get();
		BiPredicate<String,Instant> isReadStaleFunc  = (s,i) -> { return isReadStaleFor.contains(s); };
		Config config = new Config();
		config.fetchOptimiseInitialDelay = 100;
		config.fetchOptimiseSechedulPeriod = 100;
		config.countDownToRunOptimization = 0;
		config.numOfRatesPerSupplier = 2;  // use two buckets, 

		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStaleFunc, isReadStaleFunc, config);

		FetchOptimiseTask optimiserTask = rateSupplier.unscheduleFetchOptmiseTask();
		try
		{
			// Confirm buckets are empty at start
			{
				String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"empty");
				String[] expected = new String[] { "1" , "2" };
				Assert.assertArrayEquals(expected, actual);
			}

			OneFrameRate firstRate = rateSupplier.getRate("GBPJPY");
			Assert.assertEquals("GBPJPY", firstRate.getCurrencyPair());
			Helper.print(rateSupplier.getRateStreamsBucketReport(),"before run(1)");

			// Test stream is providing a new rate
			optimiserTask.run();
			OneFrameRate updatedGBPJPY = Helper.fetchUpdatedRate(rateSupplier, firstRate);
			Assert.assertNotNull(updatedGBPJPY);
			rateSupplier.getRateStreamsBucketReport();

			// Confirm buckets has GBPJPT
			{
				String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"GBPJPY");
				String[] expected = new String[] { "1 [GBPJPY]" , "2" };
				Assert.assertArrayEquals(expected, actual);
			}

			// Test add a second rate request, supply should drop the first stream for a 
			// new one with the two rates 
			{
				OneFrameRate secondRate = rateSupplier.getRate("NZDJPY");
				Assert.assertEquals("NZDJPY", secondRate.getCurrencyPair());
				optimiserTask.run();

				OneFrameRate updatedNZDJPY = Helper.fetchUpdatedRate(rateSupplier, secondRate);
				Assert.assertNotNull(updatedNZDJPY);

				// Test that the first rate is still being streamed and updated
				OneFrameRate updatedNZDJPYagain = Helper.fetchUpdatedRate(rateSupplier, updatedGBPJPY);
				Assert.assertNotNull(updatedNZDJPYagain);

				// Confirm buckets have both GBPJPT NZDJPY
				{
					String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"GBPJPY NZDJPY");
					String[] expected = new String[] { "1 [GBPJPY],[NZDJPY]", "2" };
					Assert.assertArrayEquals(expected, actual);
				}

				isReadStaleFor.add("NZDJPY");					
				isReadStaleFor.add("GBPJPY");
				optimiserTask.run();

				// Confirm buckets are empty
				{
					String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"stopped all");
					String[] expected = new String[] { "1"  , "2" };
					Assert.assertArrayEquals(expected, actual);
				}

				isReadStaleFor.remove("NZDJPY");					
				isReadStaleFor.remove("GBPJPY");
				Assert.assertArrayEquals(new String [] {"GBPJPY","NZDJPY"}, Helper.print(rateSupplier.currencyPairsWithNonStaleReads(), "toStart").toArray(new String[0]));
				optimiserTask.run(); // the stream are to be increased again

				// Confirm buckets streaming with just one stream but with two pairs
				{
					String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"started both");
					String[] expected = new String[] { "1"  , "2 [GBPJPY,NZDJPY]" };
					Assert.assertArrayEquals(expected, actual);
				}

				// Test making all rates stale one by one and causing the streaming to completely stop
				{
					isReadStaleFor.add("GBPJPY");
					optimiserTask.run();  // Give time for the stream to be reduced

					// Confirm buckets have only NZDJPY i.e. GBPY streaming stopped
					{
						String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"stopped GBPJPY");
						String[] expected = new String[] { "1 [NZDJPY]" , "2" };
						Assert.assertArrayEquals(expected, actual);
					}

					isReadStaleFor.add("NZDJPY");
					optimiserTask.run(); // the stream to be reduced

					// Confirm buckets are empty
					{
						String[] actual = Helper.print(rateSupplier.getRateStreamsBucketReport(),"stopped all");
						String[] expected = new String[] { "1"  , "2" };
						Assert.assertArrayEquals(expected, actual);
					}
				}
			}
		}
		finally
		{
			Helper.print(rateSupplier.currencyPairsWithNonStaleReads(), "finally toKeepOrStart report");
			Helper.print(rateSupplier.getRateStreamsBucketReport(),"finally bucket report");
		}
	}

	@Test
	public void majorCcyTest() throws OneFrameException, InvalidCurrencyPairException, StaleRateException
	{
		/* 18 major currency pairs
		USD/CAD	EUR/JPY
		EUR/USD	EUR/CHF
		USD/CHF	EUR/GBP
		GBP/USD	AUD/CAD
		NZD/USD	GBP/CHF
		AUD/USD	GBP/JPY
		USD/JPY	CHF/JPY
		EUR/CAD	AUD/JPY
		EUR/AUD	AUD/NZD
		http://localhost:8081/api/rates?pair=USDCAD&pair=EURJPY&pair=EURUSD&pair=EURCHF&pair=USDCHF&pair=EURGBP&pair=USDCAD&pair=EURJPY&pair=EURUSD&pair=EURCHF&pair=USDCHF&pair=EURGBP // DUPLICATE TEST

		http://localhost:8081/api/rates?pair=GBPUSD&pair=AUDCAD&pair=NZDUSD&pair=GBPCHF&pair=AUDUSD&pair=GBPJPY&pair=USDCAD&pair=USDJPY&pair=CHFJPY&pair=EURCAD&pair=AUDJPY&pair=AUDNZD
		 */
		List<String> batch1 = List.of("USDCAD","EURJPY","EURUSD","EURCHF","USDCHF","EURGBP");
		List<String> batch2 = List.of("GBPUSD","AUDCAD","NZDUSD","GBPCHF","AUDUSD","GBPJPY");
		List<String> batch3 = List.of("USDJPY","CHFJPY","EURCAD","AUDJPY","EURAUD","AUDNZD");

		List<List<String>> batches = List.of(batch1,batch2,batch3);
		List<String> all = new ArrayList<>();
		all.addAll(batch1);
		all.addAll(batch2);
		all.addAll(batch3);
		Collections.sort(all);

		AtomicBoolean isRateStale = new AtomicBoolean(false);
		List<String> isReadStaleFor = new ArrayList<String>();

		Predicate<OneFrameRate> isRateStaleFunc = t -> isRateStale.get();
		BiPredicate<String,Instant> isReadStaleFunc  = (s,i) -> { return isReadStaleFor.contains(s); };
		Config config = new Config();
		config.fetchOptimiseInitialDelay = 100;
		config.fetchOptimiseSechedulPeriod = 100;
		config.countDownToRunOptimization = 0;
		config.numOfRatesPerSupplier = 9;  // use two buckets, 

		RateSupplier rateSupplier = new RateSupplier(oneFrame, isRateStaleFunc, isReadStaleFunc, config);

		FetchOptimiseTask optimiserTask = rateSupplier.unscheduleFetchOptmiseTask();

		int iBatch = 0;
		for(List<String> batch : batches)
		{
			for(String ccyPair : batch)
			{
				OneFrameRate rate = rateSupplier.getRate(ccyPair);
				Assert.assertEquals(ccyPair, rate.getCurrencyPair());
			}

			optimiserTask.run(); // the stream to be reduced
			Helper.print(rateSupplier.getRateStreamsBucketReport(),"batch"+(++iBatch));
		}

		List<String> activePairs = rateSupplier.currencyPairsWithNonStaleReads();
		Collections.sort(activePairs);
		Assert.assertArrayEquals(all.toArray(new String[0]), activePairs.toArray(new String[0]));
	}

	@Test void isRateStaleTest()
	{
		Config config = new Config();

		IsRateStale func = new IsRateStale(config);

		OneFrameRate rate = new OneFrameRate("ABC", "XYZ", 0.0, 0.0, 0.0, Instant.now());
		Assert.assertFalse(func.test(rate));
		rate = new OneFrameRate("ABC", "XYZ", 0.0, 0.0, 0.0, Instant.now().minus(config.rateStaleDuration).minusSeconds(1));
		Assert.assertTrue(func.test(rate));

	}

	@Test void isReadStaleTest()
	{
		Config config = new Config();

		IsReadStale func = new IsReadStale(config);

		Instant readTimestamp = Instant.now();
		String currencyPair = "anything";
		Assert.assertFalse(func.test(currencyPair, readTimestamp));
		readTimestamp = Instant.now().minus(config.readStaleDuration);
		Assert.assertTrue(func.test(currencyPair, readTimestamp));
	}


	public static class Helper
	{
		public static OneFrameRate fetchUpdatedRate(RateSupplier rateSupplier, OneFrameRate rate)
				throws OneFrameException,  StaleRateException, InterruptedException, InvalidCurrencyPairException {
			OneFrameRate updatedRate = null; 
			for (int i=0; i < 5; i++)
			{
				OneFrameRate rereadRate = rateSupplier.getRate("GBPJPY");
				Thread.sleep(1000); // OneFrame will not respond quickly with a new rate
				if (!rate.equals(rereadRate))
				{ updatedRate = rereadRate; break; }
			}
			return updatedRate;
		}


		public static String[] print(String[] rateStreamsBucketReport, String ... prefixPostfix ) 
		{
			Arrays.stream(rateStreamsBucketReport).forEach(s -> { System.out.println(((prefixPostfix.length >= 1) ? prefixPostfix[0]+" " : "") + s +	((prefixPostfix.length >= 2) ? " "+prefixPostfix[1] : "")); });
			return rateStreamsBucketReport;
		}

		public static Collection<?> print(Collection<?> collection, String ... prefixPostfix ) 
		{
			collection.forEach(s -> { System.out.println(((prefixPostfix.length >= 1) ? prefixPostfix[0]+" " : "") + s +	((prefixPostfix.length >= 2) ? " "+prefixPostfix[1] : "")); });
			return collection;
		}
	}

}
