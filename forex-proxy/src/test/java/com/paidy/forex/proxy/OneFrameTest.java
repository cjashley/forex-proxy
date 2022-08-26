package com.paidy.forex.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.OneFrame.RatesStreamThread;
import com.paidy.forex.proxy.RatesStore.CurrencyPair;

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

						if(t.getPair().contentEquals("GBPJPY")) GBPJPYCount.getAndIncrement();
						if(t.getPair().contentEquals("JPYNZD")) JPYNZDCount.getAndIncrement();
						System.out.println("CONSUME "+t);
					}
			
				},"GBPJPY","JPYNZD","NZDGBP","USDJPY","USDNZD","USDGBP","GBPUSD","NZDUSD","AUDUSD","AUDJPY");
		
		
		assertTrue(thread.isRunning());

		for(int i=0; i< 10; i ++)
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
	public void testOneFrame() throws OneFrameException
	{
		OneFrame of = new OneFrame();
		String ccyPairs [] ={ "JPYNZD", "GBPJPY" };
		System.out.println("OneFrameRate getRate "+Arrays.asList(ccyPairs));
		List<OneFrameRate> rates = of.getRate(ccyPairs);

		int ccyPair = 0;
		for( OneFrameRate rate : rates)
		{
			CurrencyPair pair = new RatesStore.CurrencyPair(ccyPairs[ccyPair++]);
			System.out.println(pair+" OneFrameRate ="+rate);
			assertEquals(pair.getFrom(), rate.from);
			assertEquals(pair.getTo(), rate.to);
		}
		
		assertEquals(ccyPairs.length, rates.size());
	}
}
