package com.paidy.forex.proxy;

import javax.ws.rs.BadRequestException;

public class InvalidCurrencyPairException extends BadRequestException {

	private String currencyPair;

	public InvalidCurrencyPairException(String currencyPair) {
		super(currencyPair);
		this.currencyPair = currencyPair;
	}

	/**
	 * One of these was invalid but don't know which
	 */
	public String getCurrencyPair() {
		return currencyPair;
	}
	
}
