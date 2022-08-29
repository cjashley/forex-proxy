package com.paidy.forex.proxy;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.paidy.forex.proxy.OneFrame.OneFrameRate;
import com.paidy.forex.proxy.RateSupplier.Config;

@Singleton
public class RateSupplierService 
{

	private final OneFrame oneFrame = new OneFrame();
	private final RateSupplier rateSupplier;
	private final Config config = new Config(); // TODO all the config here needs to be read from web config files

	public RateSupplierService() 
	{
		Predicate<OneFrameRate> isRateStale = new RateSupplier.IsRateStale(config);
		BiPredicate<String,Instant> isReadStaleFunc  = new RateSupplier.IsReadStale(config);

		rateSupplier = new RateSupplier(oneFrame, isRateStale, isReadStaleFunc, config);
	}

	public RateSupplier getRateSupplier() 
	{
		return rateSupplier;
	}
	
	public static final class RateSupplierBinder extends AbstractBinder
	{
	  @Override
	  protected void configure()
	  {
	    bind(RateSupplierService.class).to(RateSupplierService.class).in(Singleton.class);
	  }
	}

	public Config getConfig()
	{
		return config;
	}
}
