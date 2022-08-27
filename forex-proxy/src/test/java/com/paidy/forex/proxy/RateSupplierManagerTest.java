package com.paidy.forex.proxy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.paidy.forex.proxy.OneFrame.OneFrameException;

class RateSupplierManagerTest {

	private OneFrame oneFrame = new OneFrame();

	@Test
	void currencyInvalidTest() throws OneFrameException, InvalidCurrencyPairException, StaleRateException 
	{
		RateSupplierManager manager = new RateSupplierManager(oneFrame);
		
		try
		{
		manager.getRate("XXXYYY");
		Assert.fail();
		}
		catch(InvalidCurrencyPairException e)
		{
			Assert.assertArrayEquals(new String[] {"XXXYYY"}, e.getCurrencyPairs());
		}
		
	}

}
