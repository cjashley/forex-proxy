package com.paidy.forex.proxy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.OneFrame.RatesStreamThread;

public class RateSupplier implements Consumer<OneFrameRate> {


	private static final int FETCH_OPTIMISE_PERIOD = /* period */ 500;

	ConcurrentHashMap<String,RateWrapper> rates = new ConcurrentHashMap<>(); // 
	final Logger log = Logger.getLogger(RateSupplier.class.getName()); 

	final OneFrame oneFrame;

	final Predicate<OneFrameRate> isRateStaleFunc;

	final BiPredicate<String, Instant> isReadStaleFunc;

	private Params params;

	private FetchOptimiseTask fetchOptimiseTask;


	public static class RateWrapper
	{
		volatile private OneFrameRate rate;
		volatile private Instant readTimestamp = Instant.now();

		public RateWrapper(OneFrameRate rate) 
		{
			this.rate = rate;
		}

		/**
		 *  Gets rate, and timestamps the get
		 *  when isReadStaleFunc is true this rate will no longer be fetched
		 *  
		 * @return rate
		 */
		public OneFrameRate getRate()
		{
			readTimestamp = Instant.now();
			return rate;
		}

		public Instant getReadTimestamp() 
		{
			return readTimestamp;
		}

		public void setRate(OneFrameRate rate) 
		{
			this.rate = rate;
		}

	}
	
	static class Params
	{
		long fetchOptimiseSechedulPeriod = 500;
		long fetchOptimiseInitialDelay = 2000;
	}

	class FetchOptimiseTask extends TimerTask 
		{
			final Logger log = Logger.getLogger(FetchOptimiseTask.class.getName()); 
	
			private OneFrame.RatesStreamThread ratesStream; 
	
			public void run()
			{
				// Objective is to build a list of active currency pairs. 
				// Then change the ONeFream streamer if necessary
	
				List<String> toStart = readNotStaleCurrencyPairs();
	
				if (toStart.isEmpty())
				{
					if (ratesStream != null)
					{
						System.out.println("ask OneFrame to stop streaming []");
	
						ratesStream.safeStop();
						ratesStream = null;
					}
				}
				else // one or more cyyPairs to fetch
				{
					boolean allPresent = false;
	
					Optional<String[]> current = getRatesStreamCurrencyPairs();
					if (!current.isEmpty())
					{
						if (toStart.size() != current.get().length)
							allPresent = false;
						else
						{
							allPresent = Arrays.asList(current.get()).containsAll(toStart);
						}
					}
	
					if (!allPresent)
					{
						String[] currencyPairs = toStart.toArray(new String[0]);
	
						// replace current stream or start new stream
						try {
							System.out.println("ask OneFrame to stream "+toStart);
	
							ratesStream = oneFrame.streamRates(RateSupplier.this, currencyPairs);
							
						} catch (OneFrameException e) {
	
							log.log(Level.WARNING,e.getMessage(),e);
						}
					}
				}
	
	//			System.out.println("Task executed at time: " + new Date());
	
			}
	
			public Optional<String[]> getRatesStreamCurrencyPairs() 
			{
					RatesStreamThread currentRatesStream = ratesStream; // take a copy to test for null, as maybe changed to null by Timer thread half way
					
					return (currentRatesStream == null || !currentRatesStream.isRunning()) ? Optional.empty() : Optional.of(currentRatesStream.getCurrencyPairs());
			}
		}

	RateSupplier(OneFrame oneFrame,Predicate<OneFrameRate> isRateStaleFunc, BiPredicate<String, Instant> isReadStaleFunc2, Params params)
	{
		this.oneFrame = oneFrame;
		this.isRateStaleFunc = isRateStaleFunc;
		this.isReadStaleFunc = isReadStaleFunc2;
		this.params = params;

		scheduleFetchOptimiseTask();
	}

	List<String> readNotStaleCurrencyPairs()
	{
		List<String> toStart = new ArrayList<String>();

		Enumeration<RateWrapper> enumeration = rates.elements();
		while(enumeration.hasMoreElements())
		{
			RateWrapper rateWrapper = enumeration.nextElement();

			if (!isReadStaleFunc.test(rateWrapper.rate.getCurrencyPair(),rateWrapper.getReadTimestamp()))
				toStart.add(rateWrapper.getRate().getCurrencyPair());
		}
		
		return toStart;
	}

	private void scheduleFetchOptimiseTask() 
	{
		Timer t = new Timer();
		fetchOptimiseTask = new FetchOptimiseTask();
		t.scheduleAtFixedRate(fetchOptimiseTask, /* delay */ params.fetchOptimiseInitialDelay, params.fetchOptimiseSechedulPeriod); 

		// TODO think about t.cancel() if rate supplier is stopped  
		// TODO would like to sleep the timer task when there are zero rates requested
	}



	/**
	 * Gets a rate. In the background, once an initial rate is fetched a stream fetch is started
	 * 
	 * @param ccyPair
	 * @return rate fetched, note the rate may be stale
	 * @throws OneFrameException 
	 * @throws InvalidCurrencyPairException 
	 * @throws StaleRateException 
	 */
	public OneFrameRate getRate(String ccyPair) throws OneFrameException, InvalidCurrencyPairException, StaleRateException
	{
		RateWrapper rateWrapper = rates.get(ccyPair);

		if (rateWrapper != null)
		{
			OneFrameRate rate = rateWrapper.getRate();
			if (isRateStaleFunc.test(rate)) 
			{
				// Since the rates.get will have read stamped the rate, the next FetchOptimise run will get the rate
				try {
					Thread.sleep(FETCH_OPTIMISE_PERIOD*2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				RateWrapper maybeNewRate = rates.get(ccyPair);

				if (maybeNewRate != null)
				{
					if (!isRateStaleFunc.test(maybeNewRate.getRate()))
					{
						return maybeNewRate.getRate();
					}
				}
				throw new StaleRateException(ccyPair,"No fresh rate was fetched");
			}

			return rate;
		}


		// get an initial rate through get, which also checks if ccyPair is valid, assumption is ccyPair will still be valid for streaming
		// which is started by the background scheduled FetchOptimiseTask
		List<OneFrameRate> initialList = oneFrame.getRate(ccyPair);
		OneFrameRate initalRate = initialList.get(0);
		
		rates.put(ccyPair, new RateWrapper(initalRate));

		return initalRate;
	}

	@Override
	public void accept(OneFrameRate rate) 
	{
		String ccyPair = rate.getCurrencyPair();
		 
		RateWrapper existingWrapper = rates.get(ccyPair);

		if (existingWrapper != null)
		{
			existingWrapper.setRate(rate);
		}
		else
		{
			log.log(Level.WARNING,"Rate received for "+ccyPair+" but not in rates mpa, adding anyway");
			rates.put(ccyPair, new RateWrapper(rate));
		}
	}

	public Optional<String[]> getRatesStreamCurrecnyPairs() {

		return fetchOptimiseTask.getRatesStreamCurrencyPairs();
		
	}

}
