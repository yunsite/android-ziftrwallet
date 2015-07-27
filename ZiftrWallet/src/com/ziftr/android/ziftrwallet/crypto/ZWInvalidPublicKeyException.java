/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.crypto;

public class ZWInvalidPublicKeyException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;
	
	public ZWInvalidPublicKeyException(String s) {
		super(s);
	}
	
}
