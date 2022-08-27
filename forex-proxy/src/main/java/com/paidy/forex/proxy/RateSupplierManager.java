package com.paidy.forex.proxy;

import java.util.concurrent.ConcurrentHashMap;

import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.RateSupplier.RateWrapper;

/**
 * Manages the rate suppliers used for getting specific rates. 
 * 
 * @author chris
 *
 */
public class RateSupplierManager {

	private final OneFrame oneFrame;

	public RateSupplierManager(OneFrame oneFrame)
	{
		this.oneFrame = oneFrame;
	}
	
	ConcurrentHashMap<String,RateSupplier> suppliers = new ConcurrentHashMap<>(); // 

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
	
		return null;
	}
}
