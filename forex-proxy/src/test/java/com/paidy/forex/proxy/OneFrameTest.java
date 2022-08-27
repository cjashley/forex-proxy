package com.paidy.forex.proxy;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.OneFrame.RatesStreamThread;

/**
 * Integration test of interface to OneFrame (note service needs to be running) 
 * 
 * @author chris
 */
public class OneFrameTest {

	@BeforeClass
	public static void before()
	{
		Logger.getLogger("").setLevel(Level.ALL);
	}
	
	@Test 
	public void testStreaming() throws OneFrameException, InterruptedException
	{
		final AtomicInteger GBPJPYCount = new AtomicInteger(0); 
		final AtomicInteger JPYNZDCount = new AtomicInteger(0); 

		OneFrame of = new OneFrame();
		RatesStreamThread thread = of.streamRates(new Consumer<OneFrameRate>()
				{

					@Override
					public void accept(OneFrameRate t) {

						if(t.getCurrencyPair().contentEquals("GBPJPY")) GBPJPYCount.getAndIncrement();
						if(t.getCurrencyPair().contentEquals("JPYNZD")) JPYNZDCount.getAndIncrement();
						System.out.println("CONSUME "+t);
					}
			
				},"GBPJPY","JPYNZD","NZDGBP","USDJPY","USDNZD","USDGBP","GBPUSD","NZDUSD","AUDUSD","AUDJPY");
		
		
		assertTrue(thread.isRunning());

		for(int i=0; i< 2; i ++)
		{
			Thread.currentThread().sleep(1000);
		System.out.println(thread.getName()+"running "+thread.isRunning()+" consumeCount:"+thread.getConsumeCount());
		}
		thread.safeStop();
		
		// Check that both request are being consumed
		assertTrue(GBPJPYCount.get() > 0);
		assertTrue(JPYNZDCount.get() > 0);

		for(int i=0; i< 2; i ++)
		{
			Thread.currentThread().sleep(1000);
			System.out.println(thread.getName()+"running "+thread.isRunning()+" consumeCount:"+thread.getConsumeCount());
		}
		assertFalse(thread.isRunning());
		

	}
	
	@Test
	public void testStreamingInvalidccyPair() throws OneFrameException, InterruptedException
	{
		final AtomicBoolean fail = new AtomicBoolean(false); 

		OneFrame of = new OneFrame();
		RatesStreamThread thread = of.streamRates(new Consumer<OneFrameRate>()
				{

					@Override
					public void accept(OneFrameRate t) {

						fail.set(true);
					}
			
				},"XXXQ");
		
		
		assertTrue(thread.isRunning());
		Thread.currentThread().sleep(10);
		thread.safeStop();

		assertTrue(thread.getLastException() instanceof InvalidCurrencyPairException);
		assertFalse(thread.isRunning());
		assertFalse("Consumer should not be called",fail.get());

	}
	@Test
	public void testGetRates() throws OneFrameException, InvalidCurrencyPairException
	{
		OneFrame of = new OneFrame();
		String ccyPairs [] ={ "JPYNZD", "GBPJPY" };
		System.out.println("OneFrameRate getRate "+Arrays.asList(ccyPairs));
		List<OneFrameRate> rates = of.getRate(ccyPairs);

		int ccyPair = 0;
		for( OneFrameRate rate : rates)
		{
			CurrencyPair pair = new CurrencyPair(ccyPairs[ccyPair++]);
			System.out.println(pair+" OneFrameRate ="+rate);
			assertEquals(pair.getFrom(), rate.from);
			assertEquals(pair.getTo(), rate.to);
		}
		
		assertEquals(ccyPairs.length, rates.size());
	}
	
	@Test
	public void testGetRateInvalid() throws OneFrameException
	{
		// {"error":"Invalid Currency Pair"}
		
		OneFrame of = new OneFrame();
		String ccyPairs [] ={ "XXXQ" };
		System.out.println("OneFrameRate getRate "+Arrays.asList(ccyPairs));

		try {
		List<OneFrameRate> rates = of.getRate(ccyPairs);
		Assert.fail("expect invalid ccy pair exception");
		}
		catch(InvalidCurrencyPairException e)
		{
			assertArrayEquals(ccyPairs,e.getCurrencyPairs());
		}
	}
}
