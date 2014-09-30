package com.ziftr.android.ziftrwallet.crypto;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;

import com.ziftr.android.ziftrwallet.fragment.accounts.OWSearchableListItem;
import com.ziftr.android.ziftrwallet.fragment.accounts.OWWalletTransactionListAdapter;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;

/**
 * This class is just a data holder for the {@link OWWalletTransactionListAdapter}.
 * 
 * TODO do we need to add any of the functionality from Bitcoinj's TransactionOutput or TransactionInput?
 * 
 * TODO delete the id field, just use hash
 */
public class OWTransaction implements OWSearchableListItem {

	// Database stuff

	
	/** 
	 * The identifier of this transaciton in the network. 
	 * This should be given after creation, in an update.
	 */
	private OWSha256Hash sha256Hash;

	/** 
	 * The amount that the transaction caused the balance of this wallet to change by. 
	 * If this is a received transaction, then the fee was not paid by 
	 * us, and so does not affect the wallet balance.
	 * This should be be put in when created.
	 */
	private BigInteger txAmount;

	/** 
	 * The fee paid to the miners for this tx. May or may not have been paid by user.
	 * If this is a sending transaction, user paid. If it is a reciving transaction, 
	 * then the sender paid. 
	 * This should be be put in when created.
	 */
	private BigInteger txFee;

	/** 
	 * The title of the transaction. This is given in the send/recieve
	 * section when the user makes/receivers a transaction.
	 * Default create has no note, calls other create with empty string.
	 */
	private String txNote;

	/** 
	 * The time this transaction was broadcasted to the network.
	 * Doesn't need to be included in create method, can be just calculated. 
	 */
	private long txTime;
	
	/** 
	 * The last known number of confirmations for this tx. May be out of date.
	 * Default create has no num confs, calls other create with -1 confs. 
	 * 0 confs means has been seen by netowrk. -1 confs means is local only.  
	 */
	private long numConfirmations;
	
	/** 
	 * The list of addresses that were used in this transaction. 
	 * Should always be included in all creates.
	 */
	private List<String> displayAddresses;
	
	// Viewing stuff

	/** The currency's title. e.g. "Bitcoin". */
	private OWCoin coinId;

	// TODO figure out what really needs to be in this constructor.
	public OWTransaction(OWCoin coinId,
			String txNote, 
			long txTime, 
			BigInteger txAmount) {
		this.setCoinId(coinId);
		this.setTxNote(txNote);
		this.setTxTime(txTime);
		this.setTxAmount(txAmount);
	}
	
	/**
	 * @return the coinId
	 */
	public OWCoin getCoinId() {
		return coinId;
	}

	/**
	 * @param coinId the coinId to set
	 */
	public void setCoinId(OWCoin coinId) {
		this.coinId = coinId;
	}


	/**
	 * @return the txNote
	 */
	public String getTxNote() {
		return txNote;
	}

	/**
	 * @param txNote the txNote to set
	 */
	public void setTxNote(String txNote) {
		this.txNote = txNote;
	}


	/**
	 * @return the sha256Hash
	 */
	public OWSha256Hash getSha256Hash() {
		return sha256Hash;
	}

	/**
	 * @param sha256Hash the sha256Hash to set
	 */
	public void setSha256Hash(OWSha256Hash sha256Hash) {
		this.sha256Hash = sha256Hash;
	}

	/**
	 * @return the txFee
	 */
	public BigInteger getTxFee() {
		return txFee;
	}

	/**
	 * @param txFee the txFee to set
	 */
	public void setTxFee(BigInteger txFee) {
		this.txFee = txFee;
	}

	/**
	 * @return the txTime
	 */
	public long getTxTime() {
		return txTime;
	}

	/**
	 * @param txTime the txTime to set
	 */
	public void setTxTime(long txTime) {
		this.txTime = txTime;
	}
	
	/**
	 * @return the txAmount
	 */
	public BigInteger getTxAmount() {
		return this.txAmount;
	}

	/**
	 * @param txAmount the txAmount to set
	 */
	public void setTxAmount(BigInteger txAmount) {
		this.txAmount = txAmount;
	}

	
	/**
	 * @return True if this transaction is seen by the network.
	 */
	public Boolean isBroadcasted() {
		return this.numConfirmations > -1;
	}

	/**
	 * @return True if this transaction type is pending
	 */
	public Boolean isPending() {
		// TODO should this be -1 or 0?
		ZLog.log("numConfs: " + this.numConfirmations);
		return -1 <= this.numConfirmations && 
				this.numConfirmations < this.coinId.getNumRecommendedConfirmations();
	}
	
	/**
	 * @return the numConfirmations
	 */
	public long getNumConfirmations() {
		return numConfirmations;
	}

	/**
	 * @param numConfirmations the numConfirmations to set
	 */
	public void setNumConfirmations(long numConfirmations) {
		this.numConfirmations = numConfirmations;
	}

	/**
	 * @return the displayAddress
	 */
	public List<String> getDisplayAddresses() {
		return displayAddresses;
	}

	/**
	 * @param displayAddresses the displayAddress to set
	 */
	public void setDisplayAddresses(List<String> displayAddresses) {
		this.displayAddresses = displayAddresses;
	}
	
	public void addDisplayAddress(String address) {
		if (this.displayAddresses == null) {
			this.displayAddresses = new ArrayList<String>();
		}
		this.displayAddresses.add(address);
	}
	
	public String getAddressAsCommaListString() {
		if (this.displayAddresses == null || this.displayAddresses.size() == 0) {
			return "";
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(",");
			for (String address : this.getDisplayAddresses()) {
				sb.append(address).append(",");
			}
			return sb.toString();
		}
	}

	
	@SuppressLint("DefaultLocale")
	@Override
	public boolean matches(CharSequence constraint, OWSearchableListItem nextItem) {
		// TODO This isn't quite right because the next item in this list doesn't necessarily 
		// also meet the search criteria...
		
		/**
		OWTransaction owNextTransaction = (OWTransaction) nextItem;
		if (owNextTransaction != null && owNextTransaction.isDivider()) {
			// The pending bar shouldn't show if it the only bar
			return false;
		} 
		**/
		//else if (this.isDivider()) {
		//	return true;
		//} 
		//else {
			return this.getTxNote().toLowerCase(Locale.ENGLISH).contains(constraint.toString().toLowerCase());
		//}
	}
	
}