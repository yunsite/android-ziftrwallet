package com.ziftr.android.ziftrwallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.spongycastle.crypto.CryptoException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWDefaultCoins;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.crypto.ZWTransactionOutput;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZLog;

/**
 * This class gives the app access to a database that will persist in different
 * sessions of the app being opened. 
 * 
 * To get an instance of this class call ZWSQLiteOpenHelper.getInstance(Context).
 * When finished accessing the database, call ZWSQLiteOpenHelper.closeInstance().
 * 
 * TODO add a long click to list items to delete coin types
 * TODO how does the encrypting of the private keys work into this?
 */
public class ZWSQLiteOpenHelper extends SQLiteOpenHelper {

	/** The current database version, may change in later versions of app. */
	public static final int DATABASE_VERSION = 3;

	
	/**
	 * <p>It's possible to calculate a wallets balance from multiple points of view. This enum specifies which
	 * of the two methods the getWallatBalance() should use.</p>
	 *
	 * <p>Consider a real-world example: you buy a snack costing $5 but you only have a $10 bill. At the start you have
	 * $10 viewed from every possible angle. After you order the snack you hand over your $10 bill. From the
	 * perspective of your wallet you have zero dollars (AVAILABLE). But you know in a few seconds the shopkeeper
	 * will give you back $5 change so most people in practice would say they have $5 (ESTIMATED).</p>
	 */
	public enum BalanceType {
		/**
		 * Balance calculated assuming all pending transactions are in fact included into the best chain by miners.
		 * This includes the value of immature coinbase transactions.
		 */
		ESTIMATED,

		/**
		 * Balance that can be safely used to create new spends. This is whatever the default coin selector would
		 * make available, which by default means all confirmed transaction outputs minus pending spend transactions.
		 */
		AVAILABLE
	}

	/** The helper for doing all things related to the receiving addresses table. */
	private ZWReceivingAddressesTable receivingAddressesTable;

	/** The helper for doing all things related to the sending addresses table. */
	private ZWAddressesTable sendingAddressesTable;

	private ZWTransactionOutputsTable transactionOutputsTable;
	
	/** The table to keep track of coin activated/deactivated/unactivated status. */
	private ZWCoinTable coinTable;

	/** The table to keep track of transactions. */
	private ZWTransactionTable transactionsTable;
	
	/** Table with market values of currencies */
	private ZWExchangeTable exchangeTable;

	///////////////////////////////////////////////////////
	//////////  Boiler plate SQLite Table Stuff ///////////
	///////////////////////////////////////////////////////

	protected ZWSQLiteOpenHelper(Context context, String databasePath) {
		// If the database path is null then an in memory database is used
		super(context, databasePath, null, DATABASE_VERSION);

		this.sendingAddressesTable = new ZWSendingAddressesTable();
		this.receivingAddressesTable = new ZWReceivingAddressesTable();
		this.transactionOutputsTable = new ZWTransactionOutputsTable();
		this.coinTable = new ZWCoinTable();
		this.transactionsTable = new ZWTransactionTable();
		this.exchangeTable = new ZWExchangeTable();
	}

	
	@Override
	public void onOpen(SQLiteDatabase db) {
		// Make table of coin activation statuses
		this.coinTable.create(db);
		
		//if we have any coins in our table load them into the ZWCoin class
		//otherwise load insert the default hardcoded coins into the table and load them into the ZWCoin class
		List<ZWCoin> coins = coinTable.getAllCoins(db);
		if(coins.size() == 0) {
			
			// fill in table with default coin, using UNACTIVATED as the status
			coins = ZWDefaultCoins.getDefaultCoins();
			for (ZWCoin coin : coins) {
				this.coinTable.insertDefault(coin, db);
			}
		}
		
		ZWCoin.loadCoins(coins);
		

		//Make exchange table
		this.exchangeTable.create(db);
	}
	
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		//do nothing, handled in onOpen (so that it's guaranteed to be there)
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion < 2 && newVersion >= 2) {
			//upgrading from version 1 to 2+ requires adding new tables for any active coins
			ZLog.log("Upgrading from local database v1 to v2...");
			List<ZWCoin> coins = this.coinTable.getNotUnactiveCoins(db);
			for(ZWCoin coin : coins) {
				this.receivingAddressesTable.create(coin, db);
				this.sendingAddressesTable.create(coin, db);
				this.transactionsTable.create(coin, db);
				this.transactionOutputsTable.create(coin, db);
			}
		}
	}

	

	//////////////////////////////////////////////////////////////////
	//////////  Interface for receiving addresses table  ///////////
	//////////////////////////////////////////////////////////////////

	private synchronized ZWAddressesTable getTable(boolean receivingNotSending) {
		return receivingNotSending ? this.receivingAddressesTable : this.sendingAddressesTable;
	}

	/**
	 * As part of the C in CRUD, this method adds a receiving (owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the note, balance, and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	protected ZWAddress createChangeAddress(ZWKeyCrypter crypter, ZWCoin coinId) {
		long time = System.currentTimeMillis() / 1000;
		return createReceivingAddress(crypter, coinId, "", 0, time, time, true, false);
	}


	/**
	 * this method creates a receiving (owned by the user) {@link ZWAddress} object and adds it 
	 * to the correct table within our database
	 * 
	 * @param crypter
	 * @param coinId
	 * @param note
	 * @param balance
	 * @param creation
	 * @param modified
	 * @param hidden
	 * @param spentFrom 
	 * @return
	 */
	protected synchronized ZWAddress createReceivingAddress(ZWKeyCrypter crypter, ZWCoin coinId, String note, 
			long balance, long creation, long modified, boolean hidden, boolean spentFrom) {
		ZWAddress address = new ZWAddress(coinId);
		address.getKey().setKeyCrypter(crypter);
		address.setLabel(note);
		address.setLastKnownBalance(balance);
		address.getKey().setCreationTimeSeconds(creation);
		address.setLastTimeModifiedSeconds(modified);
		address.setHidden(hidden);
		address.setSpentFrom(spentFrom);
		this.receivingAddressesTable.insert(address, this.getWritableDatabase());

		return address;
	}

	protected synchronized void changeEncryptionOfReceivingAddresses(ZWKeyCrypter oldCrypter, ZWKeyCrypter newCrypter) {

		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		try {
			ZWReceivingAddressesTable receiveTable = this.receivingAddressesTable;

			for (ZWCoin coin : this.getNotUnactivatedCoins()) {
				receiveTable.recryptAllAddresses(coin, oldCrypter, newCrypter, db);
			}
			db.setTransactionSuccessful();
		} catch (CryptoException e) {
			ZLog.log("Error trying to change the encryption of receiving address private keys. ", e.getMessage());
			e.printStackTrace();
		} catch (Exception oe) {
			ZLog.log("Some other exception: ", oe.getMessage());
			oe.printStackTrace();
		} finally {
			// If setTransactionSuccessful is not called, then this will roll back to not do 
			// any of the changes since there was. If it was called then it finalizes everything.
			db.endTransaction();
		}

	}

	
	/**
	 * As part of the U in CRUD, this method updates the addresses table in 
	 * the database for the given coin type and key and table boolean.
	 * 
	 * TODO is there a way to make sure that the new private key matches the old,
	 * even on encryption state changes?
	 * 
	 * @param address - the address to fully update 
	 */
	public synchronized void updateAddress(ZWAddress address) {
		this.getTable(address.isPersonalAddress()).updateAddress(address, getWritableDatabase());
	}

	/**
	 * As part of the U in CRUD, this method updates the label for the given address.
	 * 
	 * @param coin - The coin type
	 * @param address - The address to update
	 * @param newLabel - the label to change the address to
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table.
	 */
	public synchronized void updateAddressLabel(ZWCoin coin, String address, String newLabel, boolean receivingNotSending) {
		this.getTable(receivingNotSending).updateAddressLabel(coin, address, newLabel, getWritableDatabase());
	}

	/* 
	 * NOTE:
	 * Note that we don't give the method to delete an address, shouldn't ever
	 * delete an address just in case someone sends coins to an old address.
	 */

	/////////////////////////////////////////////////////////////////
	//////////  Interface for creating sending addresses  ///////////
	/////////////////////////////////////////////////////////////////

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the note, balance, and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @throws ZWAddressFormatException 
	 */
	public ZWAddress createSendingAddress(ZWCoin coinId, String addressString) 
			throws ZWAddressFormatException {
		return createSendingAddress(coinId, addressString, "");
	}

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the balance and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param note - The note that the user associates with this address.
	 * @throws ZWAddressFormatException 
	 */
	public ZWAddress createSendingAddress(ZWCoin coinId, String addressString, String note) throws ZWAddressFormatException {
		long time = System.currentTimeMillis() / 1000;
		return createSendingAddress(coinId, addressString, note, 0, time);
	}

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param key - The key to use.
	 * @throws ZWAddressFormatException 
	 */
	public synchronized ZWAddress createSendingAddress(ZWCoin coinId, String addressString, String note, 
			long balance, long modified) throws ZWAddressFormatException {
		ZWAddress address = new ZWAddress(coinId, addressString);
		address.setLabel(note);
		address.setLastKnownBalance(balance);
		//address.getKey().setCreationTimeSeconds(creation);
		address.setLastTimeModifiedSeconds(modified);
		this.sendingAddressesTable.insert(address, this.getWritableDatabase());
		return address;
	}


	///////////////////////////////////////////////////////////
	//////////  Interface for  transactions  ///////////
	///////////////////////////////////////////////////////////

	
	/**
	 * add the transaction to the local database,
	 * does an insert if the transaction is new otherwise updates the existing transaction
	 * @param transaction {@link ZWTransaction} to be inserted/updated in the database
	 */
	public synchronized void addTransaction(ZWTransaction transaction) {
		this.transactionsTable.addTransaction(transaction, getWritableDatabase());
	}


	public synchronized ZWTransaction readTransactionByHash(ZWCoin coinId, String hash) {
		if (hash == null) {
			return null;
		}

		List<String> hashes = new ArrayList<String>();
		hashes.add(hash);

		List<ZWTransaction> readAddresses = 
				this.transactionsTable.readTransactionsByHash(coinId, hashes, this.getReadableDatabase());
		if (readAddresses.size() == 0) {
			return null;
		} else {
			return readAddresses.get(0);
		}
	}

	/**
	 * Gets a list of txs from the database that have the address specified. 
	 * If address is null then it gets all addresses from the database.
	 * Orders them by most recent. 
	 * 
	 * @param coinId
	 * @param address
	 * @param db
	 * @return
	 */
	public synchronized List<ZWTransaction> readTransactionsByAddress(ZWCoin coinId, String address) {
		return transactionsTable.readTransactionsByAddress(coinId, address, getReadableDatabase());
	}

	/**
	 * Gets a list of txs from the database that have the hashes specified. 
	 * If hashes is null then it gets all addresses from the database.
	 * Orders them by most recent. 
	 * 
	 * @param coinId
	 * @param addresses
	 * @param db
	 * @return
	 */
	public synchronized List<ZWTransaction> readTransactionsByHash(ZWCoin coinId, List<String> hashes) {
		return transactionsTable.readTransactionsByHash(coinId, hashes, getReadableDatabase());
	}

	public synchronized List<ZWTransaction> readAllTransactions(ZWCoin coinId) {
		return this.transactionsTable.readTransactions(coinId, null, getReadableDatabase());
	}

	public synchronized void updateTransaction(ZWTransaction tx) {
		try {
			this.transactionsTable.updateTransaction(tx, getWritableDatabase());
		} catch (ZWNoTransactionFoundException t) {
			ZLog.log("no transaction found exception" + t);
		}
	}
	
	public synchronized void updateTransactionNumConfirmations(ZWTransaction tx) {
		this.transactionsTable.updateTransactionNumConfirmations(tx, getReadableDatabase());
	}


	protected synchronized void deleteTransaction(ZWTransaction tx) {
		this.transactionsTable.deleteTransaction(tx, getWritableDatabase());
	}
	
	public synchronized BigInteger getWalletBalance(ZWCoin coin) {
		return ZWPreferencesUtils.getMempoolIsSpendable() ? getWalletBalance(coin, BalanceType.ESTIMATED) : getWalletBalance(coin, BalanceType.AVAILABLE);
	}

	public synchronized BigInteger getWalletBalance(ZWCoin coinId, BalanceType bType) {
		BigInteger balance = BigInteger.ZERO;
		List<ZWTransaction> txs = this.readAllTransactions(coinId);

		for (ZWTransaction tx : txs) {
			if (bType == BalanceType.AVAILABLE) {
				if (!tx.isPending() || tx.getAmount().compareTo(BigInteger.ZERO) == -1) {
					balance = balance.add(tx.getAmount());
				}
			} else if (tx.isBroadcasted()) {
				balance = balance.add(tx.getAmount());
			}
		}
		return balance;
	}

	public synchronized List<ZWTransaction> getPendingTransactions(ZWCoin coinId) {
		return transactionsTable.readPendingTransactions(coinId, getReadableDatabase());
	}

	public synchronized List<ZWTransaction> getConfirmedTransactions(ZWCoin coinId) {
		return transactionsTable.readConfirmedTransactions(coinId, getReadableDatabase());
	}

	public synchronized void updateTransactionNote(ZWTransaction tx) {
		this.transactionsTable.updateTransactionNote(tx, getWritableDatabase());
	}

	public synchronized List<String> getAddressList(ZWCoin coin, boolean receivingAddresses) {
		List<String> addresses;

		if(receivingAddresses) {
			addresses = this.receivingAddressesTable.getAddressesList(coin, getWritableDatabase());
		}
		else {
			addresses = this.sendingAddressesTable.getAddressesList(coin, getWritableDatabase());
		}
		return addresses;
	}
	
	/**
	 * As part of the R in CRUD, this method gets an address from the 
	 * database for the given coin type and table boolean. Returns null if
	 * no such address is found
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param address - The list of 1xyz... (Base58) encoded address in the database.
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 */
	public synchronized ZWAddress getAddress(ZWCoin coin, String address, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).getAddress(coin, address, getReadableDatabase());
	}

	
	/**
	 * As part of the R in CRUD, this method gets an address from the 
	 * database for the given coin type and table boolean.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param addresses - The list of 1xyz... (Base58) encoded address in the database. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 */
	public synchronized List<ZWAddress> getAddresses(ZWCoin coin, List<String> addresses, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).getAddresses(coin, addresses, getReadableDatabase());
	}
	
	
	/**
	 * gets all {@link ZWAddress} from the receiving table for the given coin
	 * @param coin which coin to get addresses for
	 * @param includeSpentFrom should "unsafe" addresses (which have previously been spent from) be included
	 * @return a list of address objects
	 */
	public synchronized List<String> getHiddenAddressList(ZWCoin coin, boolean includeSpentFrom) {
		return this.receivingAddressesTable.getHiddenAddresses(coin, getReadableDatabase(), true);
	}

	/**
	 * As part of the R in CRUD, this method gets all the addresses from the 
	 * database for the given coin type and table boolean.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 * @return List of {@link ZWAddress} objects. See {@link #getAddressList(ZWCoin, boolean)} for list of Address strings.
	 */
	public synchronized List<ZWAddress> getAllVisibleAddresses(ZWCoin coin) {
		return this.receivingAddressesTable.getAllVisibleAddresses(coin, getReadableDatabase());
	}
	
	
	public synchronized List<ZWAddress> getAllSendAddresses(ZWCoin coin) {
		return this.sendingAddressesTable.getAllAddresses(coin, getReadableDatabase());
	}
	

	/**
	 * As part of the R in CRUD, this method gives the number of entries
	 * in the addresses table for the coin type specified and table boolean. 
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table.
	 */
	public synchronized int getNumAddresses(ZWCoin coinId, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).numEntries(coinId, getReadableDatabase());
	}

	/////////////////////////////////////////////////////////////
	//////////  Interface for exchange table  ///////////
	/////////////////////////////////////////////////////////////
	
	public synchronized String getExchangeValue(String from, String to){
		return this.exchangeTable.getExchangeVal(from, to, getReadableDatabase());
	}
	
	public synchronized void upsertExchangeValue(String from, String to, String val){
		this.exchangeTable.upsert(from, to, val, getWritableDatabase());
	}
	
	////////////////////////////////////////////////////////
	//////////  Interface for coin table  ///////////////
	////////////////////////////////////////////////////////

	/***
	public synchronized void updateCoinTWO(ZWCoin coin) {
		this.coinTable.updateCoin(coin, getWritableDatabase());
	}
	***/
	
	
	/**
	public synchronized List<ZWCoin> getEnabledCoins(boolean includeTestnet){
		return this.coinTable.getEnabledCoins(getReadableDatabase());
	}
	**/
	
	
	public synchronized void updateCoin(ZWCoin coin){
		this.coinTable.upsertCoin(coin, getWritableDatabase());
	}
	
	public synchronized List<ZWCoin> getCoins() {
		return this.coinTable.getAllCoins(getReadableDatabase());
	}
	
	public synchronized ZWCoin getCoin(String coinSymbol){
		return this.coinTable.getCoin(coinSymbol, getReadableDatabase());
	}

	/**
	* Call this method to activate a coin type if it is not already activated.
	* If it has not been activated then this method will create the necessary tables 
	* for the coin type given and update the activated statuses table appropriately.
	* 
	* @param coinId
	*/
	public synchronized void activateCoin(ZWCoin coin) {
		// Safety check
		if (isCoinActivated(coin)) {
			return;
		}
		
		this.receivingAddressesTable.create(coin, this.getWritableDatabase());
		this.sendingAddressesTable.create(coin, this.getWritableDatabase());
		this.transactionsTable.create(coin, this.getWritableDatabase());
		this.transactionOutputsTable.create(coin, this.getWritableDatabase());
		
		// Update table to match activated status
		this.coinTable.activateCoin(coin, this.getWritableDatabase());
	}
	
	
	public synchronized boolean isCoinActivated(ZWCoin coin) {
		return this.coinTable.isCoinActivated(coin, getReadableDatabase());
	}
	
	
	public synchronized List<ZWCoin> getActivatedCoins() {
		return this.coinTable.getActiveCoins(getReadableDatabase());
	}
	
	public synchronized List<ZWCoin> getInactiveCoins(boolean includeTestnets) {
		return this.coinTable.getInactiveCoins(getReadableDatabase(), includeTestnets);
	}
	
	
	/**
	* gets a list of every coin that has been, or currently is, activated,
	* basically excluded coins that are "un-activated",
	* the current use case for this is changing passwords, where private keys 
	* that are in deactivated coin's databases must also be updated
	* @return a list of activated and deactivated coins
	*/
	public synchronized List<ZWCoin> getNotUnactivatedCoins() {
		return this.coinTable.getNotUnactiveCoins(getReadableDatabase());
	}
	
	
	public synchronized void deactivateCoin(ZWCoin coin) {
		this.coinTable.deactivateCoin(coin, getWritableDatabase());
	}
	

	/**
	 * Get any outputs that are from the given transaction and index. Most of the time should only return 1
	 * output, but with multisig it's possible there could be multiple output addresses matching a single index
	 * @param transactionId the transaction id these outputs are for
	 * @param outputIndex the index of outputs these outputs are for
	 * @return 
	 */
	public ArrayList<ZWTransactionOutput> getTransactionOutputs(ZWCoin coin, String transactionId, int outputIndex) {
		return this.transactionOutputsTable.getTransactionOutputs(coin, transactionId, outputIndex, this.getReadableDatabase());
	}

	/**
	 * 
	 * @param coin
	 * @param output
	 */
	public void addTransactionOutput(ZWCoin coin, ZWTransactionOutput output) {
		this.transactionOutputsTable.addTransactionOutput(coin, output, this.getWritableDatabase());
	}
	
}



