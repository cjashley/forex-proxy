package com.paidy.forex.proxy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.paidy.forex.proxy.OneFrame.OneFrameCurrencyPairsException;
import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.OneFrame.RatesStreamThread;
import com.paidy.forex.proxy.RateSupplier.RateWrapper;

/**
 * Manages the rate suppliers used for getting specific rates. 
 * 
 * @author chris
 *
 */
public class RateSupplier implements Consumer<OneFrameRate>
{
	final Logger log = Logger.getLogger(RateSupplier.class.getName()); 

	private final OneFrame oneFrame;
	private final Predicate<OneFrameRate> isRateStaleFunc;
	private final BiPredicate<String, Instant> isReadStaleFunc;
	private final Config config;

	private Timer fetchOptimiseTimer = new Timer();
	private final FetchOptimiseTask fetchOptimiseTask; // might need, helpful for debugging

	static class IsRateStale implements Predicate<OneFrameRate>
	{

		private Config config;
		IsRateStale(Config config)
		{
			this.config = config;
		}
		
		@Override
		public boolean test(OneFrameRate rate) 
		{
			return rate.time_stamp.plus(config.rateStaleDuration).isBefore(Instant.now());
		}
		
	}
	
	static class IsReadStale implements BiPredicate<String,Instant>
	{

		private Config config;
		IsReadStale(Config config)
		{
			this.config = config;
		}

		@Override
		public boolean test(String currencyPair, Instant readTimestamp) {
			return readTimestamp.plus(config.readStaleDuration).isBefore(Instant.now());
		}
		
	}
	
	static public class Config
	{
		// TODO all config to be read from properties/web config
		public Duration rateStaleDuration = Duration.ofMinutes(1);
		public Duration readStaleDuration = Duration.ofSeconds(30);
		public long fetchOptimiseSechedulPeriod = 500;
		public long fetchOptimiseInitialDelay = 2000;
		public int numOfRatesPerSupplier = 10;
		public int countDownToRunOptimization = 60 * 1000 / (int) fetchOptimiseSechedulPeriod;  // 1 min / schedule period
		
		
	}

	public RateSupplier(OneFrame oneFrame,Predicate<OneFrameRate> isRateStaleFunc, BiPredicate<String, Instant> isReadStaleFunc, Config config)
	{
		this.oneFrame = oneFrame;
		this.isRateStaleFunc = isRateStaleFunc;
		this.isReadStaleFunc = isReadStaleFunc;
		this.config = config;
		this.fetchOptimiseTask = scheduleFetchOptimiseTask(new FetchOptimiseTask());
	}

	private FetchOptimiseTask scheduleFetchOptimiseTask(FetchOptimiseTask fetchOptimiseTask) 
	{
		// TODO think about t.cancel() if rate supplier is stopped  
		// TODO would like to sleep the timer task when there are zero rates requested
		fetchOptimiseTimer.scheduleAtFixedRate(fetchOptimiseTask, /* delay */ config.fetchOptimiseInitialDelay, config.fetchOptimiseSechedulPeriod); 
		return fetchOptimiseTask;
	}

	/**
	 * Maintains optimal rate streaming for a number of rateSuppliers.
	 * 
	 *  Its work is split into two:
	 *  <li> a quick phase done often which starts to stream new rates
	 *  <li> a slow phase done less often which re-organizes rateSuppliers as rate reads go stale 
	 */
	class FetchOptimiseTask extends TimerTask 
	{
		final Logger log = Logger.getLogger(FetchOptimiseTask.class.getName()); 


		private final RateStreamBuckets rateStreamBuckets = new RateStreamBuckets();

		private int countDownToRunOptimization  = config.countDownToRunOptimization;; // Decremented each call of run() until zero, when we run the heavier process



		class RateStreamBuckets
		{
			final Logger log = Logger.getLogger(RateStreamBuckets.class.getName()); 

			@SuppressWarnings("unchecked")
			private ArrayList<RatesStreamThread> [] buckets = bucketsInit();


			private ArrayList<RatesStreamThread>[] bucketsInit() 
			{
				@SuppressWarnings("unchecked")
				ArrayList<RatesStreamThread>[] buckets = new ArrayList[config.numOfRatesPerSupplier];

				for(int i=0; i<buckets.length; i++)
				{
					buckets[i] = new ArrayList<RatesStreamThread>();
				}
				return buckets;
			}

			public void add(RatesStreamThread ratesStream)
			{
				buckets[ratesStream.getCurrencyPairs().length-1].add(ratesStream);
			}

			/**
			 * 
			 * @param toStartOrKeepGoing from which all 'keepGoing' currency pairs are removed
			 */
			public void removeKeepGoingFrom(List<String> toStartOrKeepGoing) 
			{
				Collections.sort(toStartOrKeepGoing); // now we can fast find.

				for(int bucketIndex=0; bucketIndex<buckets.length; bucketIndex++)
				{
					int ccyParisPerRateStream = bucketIndex+1;
					int locations [] = new int[ccyParisPerRateStream];  // locations matching length of rateStream ccyParis in this bucket

					for(int rateStreamsIndex = buckets[bucketIndex].size()-1; rateStreamsIndex >=0; rateStreamsIndex--)
					{
						RatesStreamThread ratesStream = buckets[bucketIndex].get(rateStreamsIndex);
						int locationsIndex = 0;
						for( String ccyPair : ratesStream.getCurrencyPairs())
						{
							locations[locationsIndex] = Collections.binarySearch(toStartOrKeepGoing, ccyPair);

							if (locations[locationsIndex] < 0)
							{
								ratesStream.safeStop(); // Stop this fetching rates
								buckets[bucketIndex].remove(rateStreamsIndex); // safe since backwards iterating
								break; // give up on this rate stream as it needed removing
							}
							locationsIndex++;
						}

						if (locationsIndex == locations.length)
						{
							// all this rateStream's ccyParis are to be kept going
							// remove ccyPairs from toStartOrKeepGoing list
							Arrays.sort(locations); // in acceding order

							// remove from list in descending order to keep found location valid
							for(int foundIndex=locations.length-1; foundIndex >=0 ;foundIndex--)
							{
								toStartOrKeepGoing.remove(locations[foundIndex]); // safe since backwards iterating
							}
						}
					}
				}
			}

			public void arrageRateSuppliersFor(List<String> toStart) 
			{
				if (toStart.isEmpty()) return; 

				// Strategy is to fill up the steamers using those with the most free slots first
				// NOTE buckets.length-1 don't use the last bucket as it's streamers are full
				for(int bucketIndex=0; bucketIndex<buckets.length-1; bucketIndex++)
				{
//					int ccyParisPerRateStream = bucketIndex+1;

					for(int rateStreamsIndex = buckets[bucketIndex].size()-1; rateStreamsIndex >=0; rateStreamsIndex--)
					{
						RatesStreamThread ratesStream = buckets[bucketIndex].remove(rateStreamsIndex);

						ratesStream.safeStop();

						String[] toRestart = ratesStream.getCurrencyPairs();

						arrangeRestartDrainingToStart(toStart, toRestart);

						if (toStart.isEmpty()) return; // Finished
					}

				}

				// Create more streamers until the toStart list is empty 
				while(!toStart.isEmpty())
				{
					arrangeStartDrainingToStart(toStart);
				}					
			}

			void arrangeStartDrainingToStart(List<String> toStart) 
			{
				String[] ccyPairs = toStart.stream().limit(config.numOfRatesPerSupplier).toArray(String[]::new);
				Arrays.stream(ccyPairs).forEach(c -> toStart.remove(c));

				try {
					// the new streamRates is added into the appropriate bucket
					buckets[ccyPairs.length-1].add(oneFrame.streamRates(RateSupplier.this, ccyPairs));

				} catch (OneFrameException e) {
					log.log(Level.WARNING,e.getMessage());

					for(String ccyPairWithError : ccyPairs)
					{
						RateWrapper wrapper = rates.get(ccyPairWithError);
						if (wrapper != null) wrapper.setcurrentException(e);
					}
				}
			}

			/**
			 * 
			 * @param toStart list from which extra pairs are drained
			 * @param toRestart currency pairs re-started 
			 */
			void arrangeRestartDrainingToStart(List<String> toStart, String[] toRestart) 
			{
				int numOfSlotsFree = config.numOfRatesPerSupplier - toRestart.length;

				int toStartIndexLast = Math.max(0, toStart.size() - numOfSlotsFree); // prevent using more than we have

				String[] combinedCcyParis = Arrays.copyOf(toRestart, toRestart.length + toStart.size() - toStartIndexLast);

				for(int toStartIndex = toStart.size()-1; toStartIndex >= toStartIndexLast; toStartIndex--)
				{
					// Fill in the blank combined pairs from the to start list
					combinedCcyParis[toRestart.length + toStart.size() - toStartIndex] = toStart.remove(toStartIndex); // Draining with toStart.remove
				}

				try {
					// the new streamRates is added into the appropriate bucket
					buckets[combinedCcyParis.length].add(oneFrame.streamRates(RateSupplier.this, combinedCcyParis));

				} catch (OneFrameException e) {
					log.log(Level.WARNING,e.getMessage());

					for(String ccyPairWithError : combinedCcyParis)
					{
						RateWrapper wrapper = rates.get(ccyPairWithError);
						if (wrapper != null) wrapper.setcurrentException(e);
					}
				}
			}

			/**
			 * 
			 * @return report  eg.
			 * <pre>
			 * 1 [NZDJPY],[NZDUSD],[USDGBP]
			 * 2 [GBPNZD,NZDGPB]
			 * 3 }</pre>
			 * Shows bucket sized 1 had three streams of one
			 * <br>Shows bucket sized 2 had two streams of two
			 * <br>Shows bucket sized 3 had no streams of three

			 */
			public String[] getRateStreamsBucketReport() 
			{
				String[] report = new String[buckets.length];


				for(int bucketIndex=0; bucketIndex<buckets.length; bucketIndex++)
				{
					report[bucketIndex] = Integer.toString(bucketIndex+1);

					String separator = " ";
					for(RatesStreamThread ratesStream : buckets[bucketIndex])
					{
						report[bucketIndex] += separator + "["+ String.join(",", ratesStream.getCurrencyPairs()) + "]";
						separator = ",";
					}
				}

				return report;			
			}

		}

		public void run()
		{
			List<RateWrapper> toStartStreaming = new LinkedList<RateWrapper>();
			// Objective is to build a list of active currency pairs. 
			// Then change the ONeFream streamer if necessary
			int newRateCount = newRates.drainTo(toStartStreaming, config.numOfRatesPerSupplier);


			if (newRateCount > 0) arrageRateSuppliersFor(toStartStreaming);

			if (--countDownToRunOptimization <= 0 )
			{
				countDownToRunOptimization = config.countDownToRunOptimization; // reset
				optimiseRateSuppliers();
			}

		}

		/**
		 * Starting with the simplest approach of adding everything to a new streamRate supplier
		 * @param toStartStreaming
		 */
		private void arrageRateSuppliersFor(List<RateWrapper> toStartStreaming)
		{
			// ensure currencyPairs are unique (just in case)
			Set<String> currencyPairs = toStartStreaming.stream().map( wrapper -> wrapper.rate.getCurrencyPair()).collect(Collectors.toSet());

			try {
				rateStreamBuckets.add( oneFrame.streamRates(RateSupplier.this, currencyPairs.toArray(new String[0])) );


			} catch (OneFrameException e) {
				log.log(Level.WARNING,e.getMessage());
				log.log(Level.FINE,e.getMessage(),e);
				toStartStreaming.addAll(toStartStreaming); // Put rates back on transfer queue

				for(String ccyPairWithError : currencyPairs)
				{
					RateWrapper wrapper = rates.get(ccyPairWithError);
					if (wrapper != null) wrapper.setcurrentException(e);
				}
			}

		}

		/**
		 * A few things can happen here:
		 * <li> rates reads can go stale, i.e. no point in continuing fetching the rate
		 * <li> a rate can start to be read again.
		 * <li> re-balancing of the rate streams may be needed, e.g. too many single or low grouped streams
		 */
		private void optimiseRateSuppliers()
		{

			List<String> toStartOrKeepGoing = currencyPairsWithNonStaleReads();
			// How do we find, matching sets?
			rateStreamBuckets.removeKeepGoingFrom(toStartOrKeepGoing);
			List<String> toStart = toStartOrKeepGoing; // make clear by changing variable name that the list only has toStart ccyPairs now

			rateStreamBuckets.arrageRateSuppliersFor(toStart);
		}

		public List<String> currencyPairsWithNonStaleReads()
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

		public String [] getRateStreamsBucketReport() 
		{
			return rateStreamBuckets.getRateStreamsBucketReport();
		}
		
	}

	private final ConcurrentHashMap<String,RateWrapper> rates = new ConcurrentHashMap<>(); 

	private final BlockingQueue<RateWrapper> newRates = new LinkedTransferQueue<RateWrapper>();

	public static class RateWrapper
	{
		volatile private OneFrameRate rate;
		volatile private Instant readTimestamp = Instant.now();
		volatile private Optional<Throwable> lastException = Optional.empty();
		volatile private Optional<Throwable> currentException = Optional.empty();

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
			if (currentException.isPresent()) 
			{
				lastException = currentException;
				currentException = Optional.empty();
			}
			this.rate = rate;
		}

		/**
		 * 
		 * @return last exception have been connected with this currency pair
		 */
		public Optional<Throwable> getLastException() 
		{
			return lastException;  
		}

		public void setcurrentException(Throwable exception) 
		{
			if (currentException.isPresent())	lastException = currentException;

			this.currentException = Optional.of(exception); 
		}

		public void setReadTimeStamptoEPOC() {
			readTimestamp = Instant.EPOCH;  // i.e. expire rate reading
		}

	}
	/**
	 * Gets a rate. In the background, it uses a rate supplier or creates a new supplier to delegate the rate fetch to.
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
				newRates.add(rateWrapper); // can add as a new rate, its stale and no streamer will be getting it.
				// Since the rates.get will have read stamped the rate, the next FetchOptimise run will get the rate
				try {
					Thread.sleep(config.fetchOptimiseSechedulPeriod*2);
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
		List<OneFrameRate> initialList;
		try {
			initialList = oneFrame.getRates(ccyPair);
		} catch ( OneFrameCurrencyPairsException e) {
			throw new InvalidCurrencyPairException(ccyPair); // adapt to more specific exception
		}
		OneFrameRate initalRate = initialList.get(0);

		rateWrapper = new RateWrapper(initalRate);
		rates.put(ccyPair, rateWrapper);
		newRates.add(rateWrapper);

		return initalRate;
	}

	@Override
	public void accept(OneFrameRate rate) 
	{
		String ccyPair = rate.getCurrencyPair();

		RateWrapper existingWrapper = rates.get(ccyPair);

		if (existingWrapper != null)
		{
			existingWrapper.setRate(rate); // Setting rate clears current exception if there were any
		}
		else
		{
			boolean isValid = CurrencyPair.validate.test(ccyPair);
			String msg = "Rate received for "+ccyPair+" but not in rates map, adding if valid("+isValid+")";
			log.log(Level.WARNING,msg);
			if (CurrencyPair.validate.test(ccyPair)) {
				RateWrapper unexpected = new RateWrapper(rate);
				unexpected.setcurrentException(new RuntimeException(msg));
				unexpected.setReadTimeStamptoEPOC();
				rates.put(ccyPair, unexpected);
			}
		}
	}

	public String[] getRateStreamsBucketReport() {	return fetchOptimiseTask.getRateStreamsBucketReport();	}

	public List<String> currencyPairsWithNonStaleReads() {return fetchOptimiseTask.currencyPairsWithNonStaleReads(); }

	public FetchOptimiseTask unscheduleFetchOptmiseTask()
	{ 
		fetchOptimiseTask.cancel();
		return fetchOptimiseTask;
	}
	
}
