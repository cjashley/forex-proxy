package com.paidy.forex.proxy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CurrencyPairTest {

	@Test
	void testValidate() 
	{
		assertFalse(CurrencyPair.validate.test(null));
		assertFalse(CurrencyPair.validate.test("ToShort"));
		assertFalse(CurrencyPair.validate.test("ToLonggggggg"));
		assertFalse(CurrencyPair.validate.test("123456"));
		assertTrue(CurrencyPair.validate.test("GBPNZD"));
		assertTrue(CurrencyPair.validate.test("NZDGBP"));
		assertTrue(CurrencyPair.validate.test("JPYGBP"));
	}

}
