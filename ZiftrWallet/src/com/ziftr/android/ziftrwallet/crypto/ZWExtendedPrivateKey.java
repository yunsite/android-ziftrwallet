package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.spongycastle.math.ec.ECPoint;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.Base58;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;

public class ZWExtendedPrivateKey extends ZWPrivateKey {

	private static byte[] MASTER_HMAC_KEY = "Bitcoin seed".getBytes();

	public static final byte[] HD_VERSION_MAIN_PRIV = new byte[] {(byte)0x04, (byte)0x88, (byte)0xAD, (byte)0xE4};
	public static final byte[] HD_VERSION_TEST_PRIV = new byte[] {(byte)0x04, (byte)0x35, (byte)0x83, (byte)0x94};

	protected ZWHdPath path;
	protected ZWHdData data;

	/** 
	 * Constructs a master private key from the seed.
	 */
	public ZWExtendedPrivateKey(byte[] seed) {
		super(false);
		if (seed == null) {
			throw new ZWHdWalletException("Null seed is not valid. ");
		}
		byte[] result = CryptoUtils.Hmac(MASTER_HMAC_KEY, seed);
		this.priv = new BigInteger(1, CryptoUtils.left(result));
		this.path = new ZWHdPath("m");
		this.data = new ZWHdData(CryptoUtils.right(result));
	}
	
	public ZWExtendedPrivateKey(String path, String xprv) throws ZWAddressFormatException {
		this(new ZWHdPath(path), xprv);
	}

	/**
	 * Deserialize from an extended private key.
	 */
	protected ZWExtendedPrivateKey(ZWHdPath path, String xprv) throws ZWAddressFormatException {
		super(false);
		byte[] decoded = Base58.decodeChecked(xprv);
		byte[] version = Arrays.copyOfRange(decoded, 0, 4);
		CryptoUtils.checkPrivateVersionBytes(version);
		this.data = new ZWHdData(xprv);
		if (path == null) {
			throw new ZWHdWalletException("Keys should be combined with their relative path.");
		}
		this.path = path;
		this.priv = new BigInteger(1, Arrays.copyOfRange(decoded, 46, 78));
	}
	
	public ZWExtendedPrivateKey(String path, BigInteger priv, ZWHdData data) {
		this(new ZWHdPath(path), priv, data);
	}

	/**
	 * Make an extended key from the private key and the extension data. 
	 */
	protected ZWExtendedPrivateKey(ZWHdPath path, BigInteger priv, ZWHdData data) {
		super(priv);
		if (path == null) {
			throw new ZWHdWalletException("Keys should be combined with their relative path.");
		}
		this.path = path;
		this.data = data;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	public ZWExtendedPrivateKey deriveChild(ZWHdChildNumber index) throws ZWHdWalletException {
		byte[] hmacData = new byte[37];
		if (index.isHardened()) {
			byte[] privBytes = this.getPrivKeyBytes();
			if (privBytes == null || privBytes.length != 32)
				throw new ZWHdWalletException("Could not load private key bytes for key derivation");

			hmacData[0] = 0;
			System.arraycopy(privBytes, 0, hmacData, 1, 32);
		} else {
			if (!this.getPub().isPubKeyCanonical())
				throw new ZWHdWalletException("Non-canonical public key!");
			if (!this.getPub().isCompressed())
				throw new ZWHdWalletException("HD Wallets do not support uncompressed keys!");

			byte[] pkBytes = this.getPub().getPubKeyBytes();
			System.arraycopy(pkBytes, 0, hmacData, 0, 33);
		}

		System.arraycopy(index.serialize(), 0, hmacData, 33, 4);
		byte[] result = CryptoUtils.Hmac(this.data.chainCode, hmacData);

		// This addition modulo the curve order is what makes it possible to generate child public keys without
		// knowing the private key (for non-hardened children). Essentially
		//     priv_child = (priv_parent + one_way(pub_parent)) mod CURVE_ORDER
		// This means the points generated will be the same (adding here means ECC addition):
		//     priv_child * G = priv_parent * G + one_way(pub_parent) * G
		// which is the same as
		//     pub_child = pub_parent + one_way(pub_parent) * G
		// TODO In case parse256(I_left) � n or ki = 0, the resulting key is invalid, proceed with the next value
		BigInteger left = new BigInteger(1, CryptoUtils.left(result));
		CryptoUtils.checkLessThanCurveOrder(left);
		BigInteger newPriv = left.add(this.priv).mod(ZWCurveParameters.CURVE_ORDER);
		CryptoUtils.checkNonZero(newPriv);

		byte[] childChainCode = CryptoUtils.right(result);
		ZWHdFingerPrint parentFingerPrint = new ZWHdFingerPrint(this.getPub().getPubKeyHash());
		ZWHdData childData = new ZWHdData((byte)(this.data.depth + 1), parentFingerPrint, index, childChainCode);

		return new ZWExtendedPrivateKey(this.path.slash(index), newPriv, childData);
	}
	
	public Object deriveChild(String path) throws ZWHdWalletException {
		return this.deriveChild(new ZWHdPath(path));
	}

	public Object deriveChild(ZWHdPath path) throws ZWHdWalletException {
		if (path == null)
			throw new ZWHdWalletException("Cannot derive null path");
		if (!path.derivedFromPrivateKey())
			throw new ZWHdWalletException("Cannot derive this path, it is relative to a public key");
		
		ZWExtendedPrivateKey p = this;
		for (ZWHdChildNumber ci : path.getRelativeToPrv()) {
			p = p.deriveChild(ci);
		}
		
		String xprv = p.xprv(true);
		
		if (path.resolvesToPublicKey()) {
			ZWExtendedPublicKey pub = (ZWExtendedPublicKey) p.calculatePublicKey(true);
			for (ZWHdChildNumber ci : path.getRelativeToPub()) {
				pub = pub.deriveChild(ci);
			}
			return pub;
		}
		
		return p;
	}
	
	
	
	public String xprv(boolean mainnet) {
		return Base58.encodeChecked(this.serialize(mainnet));
	}

	
	/**
	 * Gets the serialized version of this extended private key.
	 * Note: returned bytes do NOT include checksum (for converting to base58)
	 * @param mainnet
	 * @return
	 */
	public byte[] serialize(boolean mainnet) {
		ByteBuffer b = ByteBuffer.allocate(78);
		b.put(mainnet ? HD_VERSION_MAIN_PRIV : HD_VERSION_TEST_PRIV); //version bytes
		b.put(this.data.serialize()); //data contains (in order), depth, fingerprint, child number, chain code
		b.put((byte)0);
		b.put(this.getPrivKeyBytes());
		CryptoUtils.checkHd(b.remaining() == 0); 
		return b.array();
	}

	/**
	 * CPU costly operation!
	 */
	@Override
	public ZWPublicKey calculatePublicKey(boolean compressed) {
		if (!compressed) {
			throw new UnsupportedOperationException("HD wallets only support compressd public keys.");
		}
		ECPoint point = ZWCurveParameters.CURVE.getG().multiply(this.priv);
		return new ZWExtendedPublicKey(this.path.toPublicPath(true), point.getEncoded(compressed), this.data);
	}
	
	/**
	 * @return the path
	 */
	public ZWHdPath getPath() {
		return path;
	}

	/**
	 * @return the data
	 */
	public ZWHdData getData() {
		return data;
	}
	
}
