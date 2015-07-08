/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.spongycastle.crypto.CryptoException;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWEncryptedData;
import com.ziftr.android.ziftrwallet.crypto.ZWHdPath;
import com.ziftr.android.ziftrwallet.crypto.ZWHdWalletException;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWPrivateKey;
import com.ziftr.android.ziftrwallet.crypto.ZWPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWReceivingAddress;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWReceivingAddressesTable extends ZWAddressesTable<ZWReceivingAddress> {

	/** The postfix that assists in making the names for the users addresses table. */
	private static final String TABLE_NAME_BASE = "_receiving_addresses";

	/** The private key encoded as a string using WIF. */
	public static final String COLUMN_PRIV_KEY = "priv_key";

	/** The public key, encoded as a string. */
	public static final String COLUMN_PUB_KEY = "pub_key";

	/** whether a receiving address is hidden from the user or not (for change addresses) */
	public static final String COLUMN_HIDDEN = "hidden";

	/** whether an address has spent money or not */
	public static final String COLUMN_SPENT_FROM = "spent_From";

	/** Each key generated has an index relative to its parent. */
	public static final String COLUMN_HD_INDEX = "hd_counter";
	public static final String COLUMN_HD_ACCOUNT = "account";

	public static int VISIBLE_TO_USER = 0;
	public static int HIDDEN_FROM_USER = 1;

	public static int UNSPENT_FROM = 0;
	public static int SPENT_FROM = 1;

	public static enum EncryptionStatus{
		SUCCESS,
		ALREADY_ENCRYPTED, //first address was already encrypted
		ERROR //non-first address was already encrypted
	};

	@Override
	protected String getTableName(ZWCoin coin) {
		return coin.getSymbol() + TABLE_NAME_BASE;
	}


	@Override
	protected void createBaseTable(ZWCoin coin, SQLiteDatabase database) {

		String createSql = "CREATE TABLE IF NOT EXISTS " + getTableName(coin) + 
				" (" + COLUMN_PRIV_KEY + " TEXT UNQIUE NOT NULL, " + 
				COLUMN_PUB_KEY + " TEXT UNIQUE NOT NULL, " + 
				COLUMN_ADDRESS + " TEXT UNIQUE NOT NULL)";

		database.execSQL(createSql);
	}

	@Override
	protected void createTableColumns(ZWCoin coin, SQLiteDatabase database) {

		//create columns that all address tables use
		super.createTableColumns(coin, database);

		addColumn(coin, COLUMN_SPENT_FROM, "INTEGER", database);
		addColumn(coin, COLUMN_HIDDEN, "INTEGER", database);
		addColumn(coin, COLUMN_HD_INDEX, "INTEGER", database);
		addColumn(coin, COLUMN_HD_ACCOUNT, "INTEGER", database);

		// "A unique constraint is satisfied if and only if no two rows in a table 
		// have the same values and have non-null values in the unique columns."
		// Hence, we are okay to add this here. The standard private keys that were
		// derived without HD wallets will have null in both the COLUMN_HD_INDEX and
		// the COLUMN_HD_ACCOUNT column, but non-null in COLUMN_HIDDEN.
		try {
			String rawQuery = "CREATE UNIQUE INDEX unique_account_change_counter on ";
			rawQuery += this.getTableName(coin);
			rawQuery += "(";
			rawQuery += COLUMN_HD_ACCOUNT + ", ";
			rawQuery += COLUMN_HIDDEN + ", ";
			rawQuery += COLUMN_HD_INDEX;
			rawQuery += ");";
			database.rawQuery(rawQuery, null);
		} catch(Exception e) {
			// Okay if this throws, just means the unique index already exists
		}
	}

	@Override
	protected ZWReceivingAddress cursorToAddress(ZWCoin coinId, Cursor c) throws ZWAddressFormatException {
		String dataInPrivColumn = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));

		// If a constructor is used that doesn't pass the pubKeyBytes it recalculates them
		// here and that is pretty slow. Hence, whenever loading in addresses from the db, 
		// use a constructor where you can provide the pubKeyBytes.
		byte[] pubKeyBytes = ZiftrUtils.hexStringToBytes(c.getString(c.getColumnIndex(COLUMN_PUB_KEY)));
		ZWPublicKey pubkey = new ZWPublicKey(pubKeyBytes);

		// Hidden column is used even in non-HD address rows
		int hidden = c.getInt(c.getColumnIndex(COLUMN_HIDDEN));
		ZWReceivingAddress addrRet = null;

		if (dataInPrivColumn == null || dataInPrivColumn.isEmpty()) {
			int account = c.getInt(c.getColumnIndex(COLUMN_HD_ACCOUNT));
			String path = "[m/44'/" + coinId.getHdId() + "'/" + account + "']/" + hidden + "/" + c.getInt(c.getColumnIndex(COLUMN_HD_INDEX));
			addrRet = new ZWReceivingAddress(coinId, pubkey, new ZWHdPath(path));
		} else if (dataInPrivColumn.charAt(0) == ZWKeyCrypter.NO_ENCRYPTION) {
			byte[] privBytes = ZiftrUtils.hexStringToBytes(dataInPrivColumn.substring(1));
			ZWPrivateKey newKey = new ZWPrivateKey(new BigInteger(1, privBytes), pubkey);
			addrRet = new ZWReceivingAddress(coinId, newKey, hidden != 0);
		} else {
			char encryptionId = dataInPrivColumn.charAt(0);
			String privDataWithoutEncryptionPrefix = dataInPrivColumn.substring(1);
			addrRet = new ZWReceivingAddress(coinId, pubkey, 
					new ZWEncryptedData(encryptionId, privDataWithoutEncryptionPrefix), hidden != 0);
		}

		addrRet.setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		addrRet.setSpentFrom(c.getInt(c.getColumnIndex(COLUMN_SPENT_FROM)) != 0);

		// All addresses ahve these columns
		addrRet.setLabel(c.getString(c.getColumnIndex(COLUMN_LABEL)));
		addrRet.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		addrRet.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));

		return addrRet;
	}

	//called in wrapper with transaction begin and transaction end to ensure atomicity
	protected EncryptionStatus recryptAllAddresses(ZWCoin coin, ZWKeyCrypter oldCrypter, ZWKeyCrypter newCrypter, SQLiteDatabase db) throws CryptoException {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(COLUMN_PRIV_KEY);
		sb.append(" FROM ");
		sb.append(getTableName(coin));
		sb.append(";");
		Cursor c = db.rawQuery(sb.toString(), null);

		List<String> oldPrivKeysInDb = new ArrayList<String>();
		//read private keys
		if (c.moveToFirst()) {
			do {
				String val = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
				if (val != null && !val.isEmpty()) {
					oldPrivKeysInDb.add(val);
				}
			} while (c.moveToNext());
		}
		c.close();

		// Decrypt private keys
		for (int i = 0; i < oldPrivKeysInDb.size(); i++){
			String oldPrivKeyValInDb = oldPrivKeysInDb.get(i);
			char encId = oldPrivKeyValInDb.charAt(0);
			String data = oldPrivKeyValInDb.substring(1);
			ZWPrivateKey key = null;

			boolean haveOldCrypter = oldCrypter != null;
			boolean dataIsEncrypted = oldPrivKeyValInDb.charAt(0) != ZWKeyCrypter.NO_ENCRYPTION;

			if (haveOldCrypter && dataIsEncrypted) {
				key = ZWPrivateKey.decrypt(new ZWEncryptedData(encId, data), oldCrypter);
			} else if (!haveOldCrypter && !dataIsEncrypted) {
				byte[] privBytes = ZiftrUtils.hexStringToBytes(data);
				key = new ZWPrivateKey(new BigInteger(1, privBytes));
			} else if (i == 0 && !haveOldCrypter && dataIsEncrypted) {
				// This can happen if the user upgrades from an only wallet and the password hash is erased.
				// The app will think that the user doesn't have a password, but the data in the database is still
				// encrypted. 
				// TODO In this case, we should make sure that the user has entered the right password and attempt 
				// to recrypt
				ZLog.log("ERROR tried to encrypt already encrypted first key possible recovery by entering old password.");
				return EncryptionStatus.ALREADY_ENCRYPTED;
			} else {
				//shouldn't happen
				ZLog.log("ERROR tried to encrypt already non-first encrypted keys, inconsistently encrypted keys uh oh");
				return EncryptionStatus.ERROR;
			}

			ContentValues cv = new ContentValues();
			cv.put(COLUMN_PRIV_KEY, CryptoUtils.encryptAndPrefixHex(newCrypter, key.getPrivHex()));
			StringBuilder where = new StringBuilder();
			where.append(COLUMN_PRIV_KEY).append(" = ").append(DatabaseUtils.sqlEscapeString(oldPrivKeyValInDb));

			// TODO this would be a good candidate to use compiled a compiled statement, as
			// only the values in the statement change each time through the loop.
			int numUpdated = db.update(getTableName(coin), cv, where.toString(), null);

			if (numUpdated != 1) {
				throw new CryptoException("Tables were not updated properly");
			}
		}
		return EncryptionStatus.SUCCESS;
	}

	//	protected boolean attemptDecryptAllAddresses(ZWCoin coin, ZWKeyCrypter crypter, SQLiteDatabase db){
	//		StringBuilder sb = new StringBuilder();
	//		sb.append("SELECT ");
	//		sb.append(COLUMN_PRIV_KEY);
	//		sb.append(",");
	//		sb.append(COLUMN_PUB_KEY);
	//		sb.append(" FROM ");
	//		sb.append(getTableName(coin));
	//		sb.append(";");
	//		Cursor c = db.rawQuery(sb.toString(), null);
	//		//read private keys
	//		if (c.moveToFirst()) {
	//			do {
	//				String encryptedPrivKeyString = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
	//				String storedPubKeyString = c.getString(c.getColumnIndex(COLUMN_PUB_KEY));
	//				if (encryptedPrivKeyString.charAt(0) != ZWKeyCrypter.PBE_AES_ENCRYPTION){
	//					ZLog.log("Error tried to decrypt already decrypted key...");
	//				} else {
	//					try {
	//						// Create public address from decrypted and see if it matches the stored one in the database
	//						// as a safety check
	//						ZWEncryptedData encryptedPrivKey = new ZWEncryptedData(encryptedPrivKeyString.substring(1));
	//						ZWPrivateKey key = new ZWPrivateKey(encryptedPrivKey, null, crypter);
	//						key = key.decrypt();
	//						if (!storedPubKeyString.equals(ZiftrUtils.bytesToHexString(key.getPub().getPubKeyBytes()))) {
	//							return false;
	//						}
	//					} catch(ZWKeyCrypterException e){
	//						ZLog.log("Crypter error " + e);
	//						return false;
	//					}
	//
	//				}
	//			} while (c.moveToNext());
	//		}
	//		return true;
	//	}

	protected List<String> getHiddenAddresses(ZWCoin coin, SQLiteDatabase db, boolean includeSpentFrom){

		String sql = "SELECT " + COLUMN_ADDRESS + " FROM " + getTableName(coin) + " WHERE " + COLUMN_HIDDEN + " = " + HIDDEN_FROM_USER;
		if(!includeSpentFrom) {
			sql += " AND " + COLUMN_SPENT_FROM + " = " + UNSPENT_FROM;
		}

		Cursor cursor = db.rawQuery(sql, null);
		return this.readAddressList(coin, cursor);
	}

	protected List<ZWReceivingAddress> getAllVisibleAddresses(ZWCoin coin, SQLiteDatabase db) {
		ZWAddressFilter<ZWReceivingAddress> filter = new ZWAddressFilter<ZWReceivingAddress>() {
			@Override public boolean include(ZWReceivingAddress address) { return true; }
			@Override public String where() { return COLUMN_HIDDEN + " = " + VISIBLE_TO_USER; }
		};
		return this.getAddresses(coin, filter, db);
	}

	@Override
	protected ContentValues addressToContentValues(ZWReceivingAddress address, boolean forInsert) {
		ContentValues values = super.addressToContentValues(address, forInsert);

		String privData = "";
		Integer index = null;
		Integer account = null;

		switch (address.getKeyType()) {
		case EXTENDED_UNENCRYPTED:
			ZWHdPath p = address.getExtendedPriv().getPath();

			privData = "";
			index = p.getBip44AddressIndex();
			account = p.getBip44Account();
			break;
		case DERIVABLE_PATH:
			ZWHdPath path = address.getPath();

			privData = "";
			index = path.getBip44AddressIndex();
			account = path.getBip44Account();
			break;
		case ENCRYPTED:
			ZWEncryptedData data = address.getEncryptedKey();

			privData = data.toStringWithEncryptionId();
			index = null;
			account = null;
			break;
		case STANDARD_UNENCRYPTED:
			ZWPrivateKey key = address.getStandardKey();

			privData = "" + ZWKeyCrypter.NO_ENCRYPTION + key.getPrivHex();
			index = null;
			account = null;
			break;
		}

		if (privData.isEmpty() == (index == null || account == null)) {
			throw new ZWHdWalletException("Data for insertion not available. ");
		}

		values.put(COLUMN_CREATION_TIMESTAMP, address.getCreationTimeSeconds());
		values.put(COLUMN_PRIV_KEY, privData);
		values.put(COLUMN_PUB_KEY, address.getPublicKey().getPubHex());
		values.put(COLUMN_SPENT_FROM, address.isSpentFrom());
		if (forInsert || !privData.isEmpty()) {
			values.put(COLUMN_HIDDEN, address.isHidden() ? 1 : 0);
		}
		if (forInsert) {
			// Do not update these, they cannot be changed after insertion
			values.put(COLUMN_HD_INDEX, index);
			values.put(COLUMN_HD_ACCOUNT, account);
		}

		return values;
	}

	protected int nextUnusedIndex(ZWCoin coin, boolean change, int account, SQLiteDatabase db) {
		String[] selectionArgs = new String[] {
				String.valueOf(change ? HIDDEN_FROM_USER : VISIBLE_TO_USER),
				String.valueOf(account)
		};
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT MAX(").append(COLUMN_HD_INDEX).append(") FROM ").append(getTableName(coin));
		sb.append(" WHERE ").append(COLUMN_HIDDEN).append(" = ?");
		sb.append(" AND ").append(COLUMN_HD_ACCOUNT).append(" = ?");
		Cursor c = db.rawQuery(sb.toString(), selectionArgs);
		return !c.moveToFirst() || c.getCount() == 0 ? 0 : c.getInt(0) + 1;
	}

}