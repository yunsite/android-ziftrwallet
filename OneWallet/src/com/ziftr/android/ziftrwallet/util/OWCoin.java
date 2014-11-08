package com.ziftr.android.ziftrwallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.exceptions.OWAddressFormatException;

/**
 * This class holds the Type enum. Each member of the enum can be thought of as an identifier for 
 * that cryptocurrency type. Abstract classes can define an abstract getCoinId() method and 
 * then subclasses can be made that implement this method, which is very useful for making classes 
 * applicable to many types of cryptocurrencies. 
 * 
 * TODO somehow incorporate a list of address prefixes for each coin type. 
 * e.g. bitcoin address start with a 1, bitcoin private keys start with 
 * 
 * Four leading prefixes are needed, currently, for each coin type:
 *     1. pubKeyHashPrefix
 *     2. scriptHashPrefix
 *     3. privKeyPrefix
 */
public class OWCoin implements OWCurrency {

	/** When using bundles, this can be used to store a specific coin type. */
	public static final String TYPE_KEY = "OWCOIN_TYPE_KEY";

	public static OWCoin BTC = new OWCoin("10000", "BTC", "Bitcoin", "btc", "main", "bitcoin", 8, R.drawable.logo_bitcoin,
			(byte) 0, (byte) 5, (byte) 128, 6, 600, "Bitcoin Signed Message:\n");
	public static OWCoin LTC = new OWCoin("100000", "LTC", "Litecoin", "ltc", "main", "litecoin", 8, R.drawable.logo_litecoin,
			(byte) 48, (byte) 5, (byte) 176, 12, 150, "Litecoin Signed Message:\n");

	public static OWCoin PPC = new OWCoin("1000000", "PPC", "Peercoin", "ppc", "main", "peercoin", 8, R.drawable.logo_peercoin,
			(byte) 55, (byte) 117, (byte) 183, 6, 600, "PPCoin Signed Message:\n");
	public static final OWCoin DOGE = new OWCoin("100000000", "DOGE", "Dogecoin", "doge", "main", "dogecoin", 8, R.drawable.logo_dogecoin,
			(byte) 30, (byte) 22, (byte) 158, 6, 60, "Dogecoin Signed Message:\n");

	public static OWCoin BTC_TEST = new OWCoin("10000", "BTC_TEST", "Bitcoin Testnet", "btc", "testnet3", "bitcoin", 8, R.drawable.logo_bitcoin,
			// (byte) 0, (byte) 5, (byte) 128, 6);
			(byte) 111, (byte) 196, (byte) 239, 6, 600, "Bitcoin Signed Message:\n");
	public static OWCoin LTC_TEST = new OWCoin("100000", "LTC_TEST", "Litecoin Testnet", "ltc", "testnet", "litecoin", 8, R.drawable.logo_litecoin,
			(byte) 111, (byte) 196, (byte) 239, 12, 150, "Litecoin Signed Message:\n");
	public static OWCoin PPC_TEST = new OWCoin("0.0000", "PPC_TEST", "Peercoin Testnet", "ppc", "test", "peercoin", 8, R.drawable.logo_peercoin,
			(byte) 111, (byte) 196, (byte) 239, 6, 600, "PPCoin Signed Message:\n");
	public static OWCoin DOGE_TEST = new OWCoin("100000000", "DOGE_TEST", "Dogecoin Testnet", "doge", "test", "dogecoin", 8, R.drawable.logo_dogecoin,
			(byte) 113, (byte) 196, (byte) 241, 6, 60, "Dogecoin Signed Message:\n");

	public static final OWCoin[] TYPES = new OWCoin[] {BTC, LTC, PPC, DOGE, BTC_TEST, LTC_TEST, PPC_TEST, DOGE_TEST};

	public static final OWCoin[] values() {
		return TYPES;
	}

	public static OWCoin valueOf(String coinStr) {
		for (OWCoin coin : OWCoin.values()) {
			if (coin.toString().equals(coinStr)) {
				return coin;
			}
		}
		return null;
	}

	private String defaultFeePerKb;
	private String shortTitle;
	private String longTitle;
	private String type;
	private String chain;
	private String scheme; 
	private int numberOfDigitsOfPrecision; 
	private int logoResId;
	private byte pubKeyHashPrefix; 
	private byte scriptHashPrefix;  
	private byte privKeyPrefix; // TODO assume its pubKeyHashPrefix + 128
	private int numRecommendedConfirmations; 
	private int secondsPerAverageBlockSolve;  
	private String signingMessageMagic;

	private OWCoin(String defaultFeePerKb, String shortTitle, String longTitle, String type, String chain, 
			String scheme, int numberOfDigitsOfPrecision, int logoResId, byte pubKeyHashPrefix, byte scriptHashPrefix, 
			byte privKeyPrefix, int numRecommendedConfirmations, int secondsPerAverageBlockSolve, String signingMessageMagic) {
		this.defaultFeePerKb = defaultFeePerKb;
		this.shortTitle = shortTitle;
		this.longTitle = longTitle;
		this.type = type;
		this.chain = chain;
		this.scheme = scheme;
		this.numberOfDigitsOfPrecision = numberOfDigitsOfPrecision;
		this.logoResId = logoResId;
		this.pubKeyHashPrefix = pubKeyHashPrefix;
		this.scriptHashPrefix = scriptHashPrefix;
		this.privKeyPrefix = privKeyPrefix;
		this.numRecommendedConfirmations = numRecommendedConfirmations;
		this.secondsPerAverageBlockSolve = secondsPerAverageBlockSolve;
		this.signingMessageMagic = signingMessageMagic;
	}



	/**
	 * As the name describes, this gets the default fee per kb in satoshis for the
	 * coin. It might be useful to use BigDecimal() or BigInteger for the parsing
	 * as they are better to store monetary values and they will do the parsing
	 * of this string right in the constructor. 
	 * 
	 * @return as above
	 */
	public String getDefaultFeePerKb() {
		return this.defaultFeePerKb;
	}

	/**
	 * @return the shortTitle
	 */
	public String getShortTitle() {
		return shortTitle;
	}

	/**
	 * @return the longTitle
	 */
	public String getLongTitle() {
		return longTitle;
	}


	/**
	 * Gives the title of the coin. e.g. "Bitcoin" for OWCoin.BTC, etc. 
	 * 
	 * @return as above
	 */


	/**
	 * gets which type of coin this is for sending to api/database, eg "btc"
	 * @return a String used to identify a coin
	 */
	public String getType() {
		return type;
	}


	/**
	 * gets which chain this coin is on eg testnet3 vs main
	 * @return the chain this coin is using as a String
	 */
	public String getChain() {
		return chain;
	}

	/**
	 * @return the scheme
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * This is an okay place to get this information for now, but will likely 
	 * want to get this info somewhere else eventually. This gets the number of 
	 * decimal places of precision, just in case coins have different precision
	 * amounts. For exmaple, for Bitcoin this returns 8 because 1/10^8 is the 
	 * smallest unit of bitcoin.
	 * 
	 * @return as above
	 */
	public int getNumberOfDigitsOfPrecision() {
		return this.numberOfDigitsOfPrecision;
	}

	/**
	 * @return the logoResId
	 */
	public int getLogoResId() {
		return logoResId;
	}

	
	/**
	 * @return the pubKeyHashPrefix
	 */
	public byte getPubKeyHashPrefix() {
		return pubKeyHashPrefix;
	}

	/**
	 * @return the scriptHashPrefix
	 */
	public byte getScriptHashPrefix() {
		return scriptHashPrefix;
	}

	/**
	 * @return the uncompressedPrivKeyPrefix
	 */
	public byte getPrivKeyPrefix() {
		return privKeyPrefix;
	}

	/**
	 * @return the numRecommendedConfirmations
	 */
	public int getNumRecommendedConfirmations() {
		return numRecommendedConfirmations;
	}

	public byte[] getAcceptableAddressCodes() {
		return new byte[] {getPubKeyHashPrefix(), getScriptHashPrefix()};
	}

	/**
	 * @return the secondsPerAverageBlockSolve
	 */
	public int getSecondsPerAverageBlockSolve() {
		return secondsPerAverageBlockSolve;
	}

	@Override
	public String toString() {
		return this.getShortTitle();
	}

	
	@Override
	public String getFormattedAmount(BigDecimal amount) {
		
		BigDecimal coins = ZiftrUtils.formatToNDecimalPlaces(this.getNumberOfDigitsOfPrecision(), amount);
		
		coins = coins.stripTrailingZeros();
		if(coins.scale() < 2) {
			//make sure and always show 2 decimal places (even trailing zeros) like the QT wallets do
			coins = coins.setScale(2);
		}
		
		return coins.toPlainString();
	}

	
	@Override
	public String getFormattedAmount(BigInteger atmoicUnits) {
		
		BigDecimal toFormatAsDecimal = this.getAmount(atmoicUnits);
		BigDecimal coins = ZiftrUtils.formatToNDecimalPlaces(this.getNumberOfDigitsOfPrecision(), toFormatAsDecimal);
		
		return this.getFormattedAmount(coins);
	}

	/**
	 * This is a placeholder for a spot to determine if a given address
	 * is valid.
	 * 
	 * @param address - The address to verify the validity of.
	 * @return as above
	 * 
	 * TODO use the checksum to make sure addresses are valid
	 */
	public boolean addressIsValid(String address) {
		if (address == null || address.isEmpty()) {
			return false;
		}

		try {
			byte[] decodedBytes = Base58.decode(address);
			for (byte b : this.getAcceptableAddressCodes()) {
				if (decodedBytes[0] == b) {
					return true;
				}
			}
			return false;
		} catch(OWAddressFormatException afe) {
			return false;
		}
	}

	/**
	 * @return the signingMessageMagic
	 */
	public String getSigningMessageMagic() {
		return signingMessageMagic;
	}
	
	public void updateCoin(String defaultFee, byte pubKeyPrefix, byte scriptHashPrefix, byte privKeyPrefix, int confirmationsNeeded, int blockGenTime){
		this.defaultFeePerKb = defaultFee;
		this.pubKeyHashPrefix = pubKeyPrefix;
		this.scriptHashPrefix = scriptHashPrefix;
		this.privKeyPrefix = privKeyPrefix;
		this.numRecommendedConfirmations = confirmationsNeeded;
		this.secondsPerAverageBlockSolve = secondsPerAverageBlockSolve;
	}

	
	
	@Override
	public BigDecimal getAmount(BigInteger atomicUnits) {
		return new BigDecimal(atomicUnits, this.getNumberOfDigitsOfPrecision());
	}

	
	@Override
	public BigInteger getAtomicUnits(BigDecimal amount) {
		int precision = -1*this.getNumberOfDigitsOfPrecision();
		
		//note, this constructor is weird to me, but RTFM, it's for making small numbers and makes the scale negative
		BigDecimal multiplier = new BigDecimal(BigInteger.ONE, precision); 
		return amount.multiply(multiplier).toBigInteger();
	}
	
	
	

	
	
	
	
	
	
	
	
}