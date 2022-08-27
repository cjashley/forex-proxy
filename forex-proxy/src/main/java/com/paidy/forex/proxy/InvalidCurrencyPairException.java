package com.paidy.forex.proxy;

public class InvalidCurrencyPairException extends Exception {

	private String[] currencyPairs;

	public InvalidCurrencyPairException(String[] currencyPairs) {
		super(String.join(",", currencyPairs));
		this.currencyPairs = currencyPairs;
	}

	/**
	 * One of these was invalid but don't know which
	 */
	public String[] getCurrencyPairs() {
		return currencyPairs;
	}
	
}
