package com.paidy.forex.proxy;

import java.util.Currency;
import java.util.function.Predicate;

public class CurrencyPair
{
	final String currencyPair;

	@Deprecated
	CurrencyPair(String currencyPair) 
	{
		if (currencyPair.length() != 6) throw new IllegalArgumentException("Currency pair len "+currencyPair.length()+"!= 6 i.e. two 3 char pairs e.g. NZDJPY");
		this.currencyPair = currencyPair;
	}

	String from() { return currencyPair.substring(0,3);}
	String to() { return currencyPair.substring(3); }

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

	public static Predicate<String> validate = new Predicate<String>() 
	{
		@Override
		public boolean test(String currencyPair) 
		{
			try {
			Currency.getInstance(getFrom(currencyPair));
			Currency.getInstance(getTo(currencyPair));
			} catch (IllegalArgumentException | NullPointerException e  )
			{
				return false;
			}
			return true;
		} 
	};
	
	public static String getFrom(String currencyPair) 
	{ 
		if (currencyPair == null) throw new NullPointerException();
		if (currencyPair.length() != 6) throw new IllegalArgumentException(currencyPair);

		return currencyPair.substring(0,3);
	}
	public static String getTo(String currencyPair) 
	{ 
		if (currencyPair == null) throw new NullPointerException();
		if (currencyPair.length() != 6) throw new IllegalArgumentException(currencyPair);

		return currencyPair.substring(3); 
	}

}