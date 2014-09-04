package com.ziftr.android.onewallet.sqlite;

import java.math.BigInteger;

import android.content.ContentValues;
import android.database.Cursor;

import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.crypto.OWECKey;
import com.ziftr.android.onewallet.exceptions.OWAddressFormatException;
import com.ziftr.android.onewallet.util.Base58;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWCoin.Type;
import com.ziftr.android.onewallet.util.OWUtils;

public class OWReceivingAddressesTable extends OWAddressesTable {

	/** The postfix that assists in making the names for the users addresses table. */
	private static final String TABLE_POSTFIX = "_receiving_addresses";

	/** The private key encoded as a string using WIF. */
	public static final String COLUMN_PRIV_KEY = "priv_key";

	/** The public key, encoded as a string. */
	public static final String COLUMN_PUB_KEY = "pub_key";

	/** This is the timestamp when the address was created. */
	public static final String COLUMN_CREATION_TIMESTAMP = "creation_timestamp";

	@Override
	protected String getTablePostfix() {
		return TABLE_POSTFIX;
	}

	@Override
	protected String getCreateTableString(Type coinId) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(getTableName(coinId)).append(" (");
		sb.append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(COLUMN_PRIV_KEY).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_PUB_KEY).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_ADDRESS).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_NOTE).append(" TEXT, ");
		sb.append(COLUMN_BALANCE).append(" INTEGER, ");
		sb.append(COLUMN_CREATION_TIMESTAMP).append("INTEGER, ");
		sb.append(COLUMN_MODIFIED_TIMESTAMP).append(" INTEGER );");
		return sb.toString();
	}

	@Override
	protected OWAddress cursorToAddress(OWCoin.Type coinId, Cursor c) throws OWAddressFormatException {
		String encodedPrivKey = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
		byte[] wifPrivKeyBytes = Base58.decodeChecked(encodedPrivKey);
		OWECKey newKey = new OWECKey(new BigInteger(1, OWUtils.stripVersionAndChecksum(wifPrivKeyBytes)));
		OWAddress newAddress = new OWAddress(coinId, newKey);

		// Reset all the address' parameters for use elsewhere
		newAddress.setNote(c.getString(c.getColumnIndex(COLUMN_NOTE)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.getKey().setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		return newAddress;
	}

	@Override
	protected ContentValues addressToContentValues(OWAddress address, boolean forInsert) {
		// Can assume here that address.getKey() doesn't return null.

		ContentValues values = new ContentValues();
		// TODO can we do this as a blob?
		values.put(COLUMN_PRIV_KEY, Base58.encode(
				address.getCoinId().getPrivKeyPrefix(), address.getKey().getPrivKeyBytesForAddressEncoding()));

		// Either for insert or for update
		if (forInsert) {
			// For inserting a key into the db, we need the keys
			values.put(COLUMN_PUB_KEY, OWUtils.bytesToHexString(address.getKey().getPubKey()));
			values.put(COLUMN_ADDRESS, Base58.encode(address.getCoinId().getPubKeyHashPrefix(), address.getHash160()));
		} 
		values.put(COLUMN_NOTE, address.getNote());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_CREATION_TIMESTAMP, address.getKey().getCreationTimeSeconds());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());

		// The id will be generated upon insertion.
		return values;
	}

}