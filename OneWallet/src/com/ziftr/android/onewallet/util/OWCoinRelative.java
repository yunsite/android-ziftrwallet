package com.ziftr.android.onewallet.util;

public interface OWCoinRelative {
	
	/**
	 * Get the OWCoin.Type for the actual subclassing fragment.
	 * 
	 * @return OWCoin.Type.BTC for Bitcoin, OWCoin.Type.LTC for Litecoin, etc.
	 */
	public abstract OWCoin.Type getCoinId();
	
}