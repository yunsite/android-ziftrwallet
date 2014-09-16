package com.ziftr.android.onewallet.fragment.accounts;

import android.view.View;

import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.fragment.OWFragment;
import com.ziftr.android.onewallet.util.OWCoin;

// TODO refactor to OWWalletManagerFragment
public abstract class OWWalletUserFragment extends OWFragment {

	/**
	 * Gives the globally accessible wallet manager. All coin-network related
	 * things should be done through this class as it chooses whether to use
	 * (temporary) bitcoinj or to use the SQLite tables and API (future). 
	 * 
	 * @return the wallet manager
	 */
	protected OWWalletManager getWalletManager() {
		return this.getOWMainActivity().getWalletManager();
	}

	protected OWCoin getCurSelectedCoinType() {
		return this.getOWMainActivity().getCurSelectedCoinType();
	}

	protected void showWalletHeader() {
		this.getOWMainActivity().getWalletHeaderBar().setVisibility(View.VISIBLE);
	}
	
}