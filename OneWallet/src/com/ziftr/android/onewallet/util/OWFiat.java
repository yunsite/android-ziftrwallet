package com.ziftr.android.onewallet.util;

import java.math.BigDecimal;

public class OWFiat {
	public enum Type implements OWCurrency {
		USD("$", 2),
		EUR(String.valueOf((char) 0x80), 2);
		
		/** This is the symbol for the currency. "$" for USD, etc. */
		private String symbol;
		
		/** For most currencies this is 2, as non-whole amounts are displayed as 0.XX */
		private int numberOfDigitsOfPrecision;
		
		private Type(String symbol, int numberOfDigitsOfPrecision) {
			this.symbol = symbol;
			this.numberOfDigitsOfPrecision = numberOfDigitsOfPrecision;
		}
		
		/**
		 * @return the symbol
		 */
		public String getSymbol() {
			return symbol;
		}

		/**
		 * @return the numberOfDigitsOfPrecision
		 */
		@Override
		public int getNumberOfDigitsOfPrecision() {
			return numberOfDigitsOfPrecision;
		}
	}
	
	/**
	 * A convenience method to format a BigDecimal. In Standard use,
	 * one can just use standard BigDecimal methods and use this
	 * right before formatting for displaying a fiat value. 
	 * 
	 * @param toFormat - The BigDecimal to format.
	 * @return as above
	 */
	public static BigDecimal formatFiatAmount(OWFiat.Type type, BigDecimal toFormat) {
		return OWUtils.formatToNDecimalPlaces(
				type.getNumberOfDigitsOfPrecision(), toFormat);
	}
	
}