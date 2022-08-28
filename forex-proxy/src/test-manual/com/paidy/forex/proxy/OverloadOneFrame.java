package com.paidy.forex.proxy;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.paidy.forex.proxy.OneFrame.OneFrameCurrencyPairsException;
import com.paidy.forex.proxy.OneFrame.OneFrameException;
import com.paidy.forex.proxy.OneFrame.OneFrameRate;

public class OverloadOneFrame {

	
	@Test
	public void overloadOneFrame() throws OneFrameCurrencyPairsException
	{
		OneFrame oneFrame = new OneFrame();
		try {
		for(int i=0; i<OneFrame.MAX_REQUESTS+10; i++)
		{
				List<OneFrameRate> rate = oneFrame.getRates("NZDJPY");
		}
		} catch (OneFrameException e) {
			assertEquals("Quota reached",e.getMessage());
		}  
	}
}
