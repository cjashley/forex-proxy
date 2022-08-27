package com.paidy.forex.proxy;

@Deprecated
public class CurrencyPair
{
	final String currencyPair;

	CurrencyPair(String currencyPair) 
	{
		if (currencyPair.length() != 6) throw new IllegalArgumentException("Currency pair len "+currencyPair.length()+"!= 6 i.e. two 3 char pairs e.g. NZDJPY");
		this.currencyPair = currencyPair;
	}

	String getFrom() { return currencyPair.substring(0,3);}
	String getTo() { return currencyPair.substring(3); }

	@Override
	public String toString() 
	{
		return currencyPair;
	}

	@Override
	public boolean equals( Object b)
	{
		return currencyPair.equals(b);
	}

	@Override
	public int hashCode()
	{
		return currencyPair.hashCode();
	}
}