package com.paidy.forex.proxy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;

public class RateSupplier implements Consumer<OneFrameRate> {


	private static final int FETCH_OPTIMISE_PERIOD = /* period */ 500;

	ConcurrentHashMap<String,RateWrapper> rates = new ConcurrentHashMap<>(); // 
	final Logger log = Logger.getLogger(RateSupplier.class.getName()); 

	final OneFrame oneFrame;

	final Predicate<OneFrameRate> isRateStaleFunc;

	final Predicate<Instant> isReadStaleFunc;

	private Params params;


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

	RateSupplier(OneFrame oneFrame,Predicate<OneFrameRate> isRateStaleFunc, Predicate<Instant> isReadStaleFunc, Params params)
	{
		this.oneFrame = oneFrame;
		this.isRateStaleFunc = isRateStaleFunc;
		this.isReadStaleFunc = isReadStaleFunc;
		this.params = params;

		scheduleFetchOptimiseTask();
	}

	List<String> readStaleCurrencyPairs()
	{
		List<String> toStart = new ArrayList<String>();

		Enumeration<RateWrapper> enumeration = rates.elements();
		while(enumeration.hasMoreElements())
		{
			RateWrapper rateWrapper = enumeration.nextElement();

			if (!isReadStaleFunc.test(rateWrapper.getReadTimestamp()))
				toStart.add(rateWrapper.getRate().getCurrencyPair());
		}
		
		return toStart;
	}

	class FetchOptimiseTask extends TimerTask 
	{
		final Logger log = Logger.getLogger(FetchOptimiseTask.class.getName()); 

		private OneFrame.RatesStreamThread ratesStream; 

		public void run()
		{
			// Objective is to build a list of active currency pairs. 
			// Then change the ONeFream streamer if necessary

			List<String> toStart = readStaleCurrencyPairs();

			if (toStart.isEmpty())
			{
				if (ratesStream != null)
				{
					ratesStream.safeStop();
					ratesStream = null;
				}
			}
			else // one or more cyyPairs to fetch
			{
				boolean allPresent = false;

				if (ratesStream != null)
				{
					allPresent = true;
					for(String ccyPair : toStart)
					{
						if (!ratesStream.isFetching(ccyPair))
						{
							allPresent = false;
							break;
						}
					}

				}

				if (!allPresent)
				{
					String[] currencyPairs = toStart.toArray(new String[0]);

					// replace current stream or start new stream
					try {
						ratesStream = oneFrame.streamRates(RateSupplier.this, currencyPairs);
					} catch (OneFrameException e) {

						log.log(Level.WARNING,e.getMessage(),e);
					}
				}
			}

			System.out.println("Task executed at time: " + new Date());

		}
	};

	private void scheduleFetchOptimiseTask() 
	{
		Timer t = new Timer();
		TimerTask task = new FetchOptimiseTask();
		t.scheduleAtFixedRate(task, /* delay */ params.fetchOptimiseInitialDelay, params.fetchOptimiseSechedulPeriod); 

		// TODO think about t.cancel() if rate supplier is stopped          
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

}
