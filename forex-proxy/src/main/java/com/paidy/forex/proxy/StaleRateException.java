package com.paidy.forex.proxy;

public class StaleRateException extends Exception {

	private String currencyPair;

	public StaleRateException(String currencyPair, String msg) {
		super(msg);
		this.currencyPair = currencyPair;
	}

	public String getCurrencyPair() {
		return currencyPair;
	}
}
