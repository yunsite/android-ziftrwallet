package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.fragment.OWFragment;
import com.ziftr.android.onewallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWTags;
import com.ziftr.android.onewallet.util.OWUtils;

/**
 * The OWMainActivity starts this fragment. This fragment is 
 * associated with list view of user wallets and a bar at the bottom of the list
 * view which opens a dialog to add rows to the list view.  
 */
public class OWAccountsFragment extends OWFragment {

	/** The view container for this fragment. */
	private View rootView;

	/** The context for this fragment. */
	private FragmentActivity mContext;

	/** The list view which shows a link to open each of the wallets/currency types. */
	private ListView currencyListView;
	/** The data set that the currencyListView uses. */
	private List<OWCurrencyListItem> userWallets;

	/** The wallet manager. This fragment works with */
	private OWWalletManager walletManager;

	/** 
	 * Placeholder for later, doesn't do anything other than 
	 * what parent method does right now.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) { 
		super.onSaveInstanceState(outState);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("ziftrWALLET", true, false);
	}
	
	/**
	 * To create the view for this fragment, we start with the 
	 * basic fragment_home view which just has a few buttons that
	 * open up different coin-type wallets.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		this.hideWalletHeader();
		
		this.rootView = inflater.inflate(R.layout.section_accounts_layout, container, false);

		this.walletManager = this.getOWMainActivity().getWalletManager();

		// Initialize the list of user wallets that they can open
		this.initializeCurrencyListView();

		// Return the view which was inflated
		return rootView;
	}

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we save a reference to the activity.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mContext = (FragmentActivity) activity;
	}

	/**
	 * A messy method which is just used to not have huge if else
	 * blocks elsewhere. Gets the item that we will add to the currency
	 * list when the user adds a new currency.
	 * 
	 * @param id - The type of coin to get a new {@link OWCurrencyListItem} for. 
	 * @return as above
	 */
	private OWCurrencyListItem getItemForCoinType(OWCoin id) {
		// TODO need to get market values from some sort of an API

		// TODO see if we can get the constructor of OWCurrencyListItem 
		// to take very few things
		int resId = R.layout.accounts_currency_list_single_item;
		if (id == OWCoin.BTC) {
			return new OWCurrencyListItem(OWCoin.BTC, OWFiat.USD, 
					"620.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.BTC_TEST) {
			String balance = OWUtils.bitcoinValueToFriendlyString(
					this.walletManager.getWalletBalance(id, OWSQLiteOpenHelper.BalanceType.AVAILABLE));
			return new OWCurrencyListItem(OWCoin.BTC_TEST, OWFiat.USD, 
					"0.00", balance, "0.00", resId);
		} else if (id == OWCoin.LTC) {
			return new OWCurrencyListItem(OWCoin.LTC, OWFiat.USD, 
					"6.70", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.LTC_TEST) {
			return new OWCurrencyListItem(OWCoin.LTC_TEST, OWFiat.USD, 
					"0.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.PPC) {
			return new OWCurrencyListItem(OWCoin.PPC, OWFiat.USD, 
					"1.40", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.PPC_TEST) {
			return new OWCurrencyListItem(OWCoin.PPC_TEST, OWFiat.USD, 
					"0.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.DOGE) {
			return new OWCurrencyListItem(OWCoin.DOGE, OWFiat.USD, 
					"0.0023", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.DOGE_TEST) {
			return new OWCurrencyListItem(OWCoin.DOGE_TEST, OWFiat.USD, 
					"0.00", "0.00000000", "0.00", resId);
		}  

		// Should never get here
		return null;
	}


	/**
	 * Notifies the list view adapter that the data set has changed
	 * to initialize a redraw. 
	 * 
	 * May save this for later to refresh when deleting a wallet from the list
	 * of wallets on this accounts page. 
	 */
	@SuppressWarnings("unchecked")
	public void refreshListOfUserWallets() {
		((ArrayAdapter<OWCurrencyListItem>) 
				this.currencyListView.getAdapter()).notifyDataSetChanged();
	}

	/**
	 * Sets up the data set, the adapter, and the onclick for 
	 * the list view of currencies that the user currently has a 
	 * wallet for.
	 */
	private void initializeCurrencyListView() {
		// Get the values from the manager and initialize the list view from them
		this.userWallets = new ArrayList<OWCurrencyListItem>();
		for (OWCoin type : this.walletManager.readAllActivatedTypes()) {
			this.userWallets.add(this.getItemForCoinType(type));
		}
		// The bar at the bottom
		this.userWallets.add(new OWCurrencyListItem(
				null, null, null, null, null, R.layout.accounts_currency_list_add_new));

		this.currencyListView = (ListView) 
				this.rootView.findViewById(R.id.listOfUserWallets);
		this.currencyListView.setAdapter(
				new OWCurrencyListAdapter(this.mContext, this.userWallets));

		this.currencyListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				if (currencyListView.getAdapter().getItemViewType(
						position) == OWCurrencyListAdapter.footerType) {
					// If we have clicked the add new currency bar then
					// make the choose new currency dialog (as long as there are new
					// currencies to add

					// The minus one is because the list contains the add new 
					// currency bar
					if (userWallets.size()-1 < OWCoin.values().length) {
						// TODO may want to change the list to just be
						// walletManager.getAllUsersWalletTypes(); but since
						// that wouldn't include temporarily added wallets we will
						// do this for now.
						List<OWCoin> usersCurWallets = new ArrayList<OWCoin>();
						for (OWCurrencyListItem newItem : userWallets) {
							usersCurWallets.add(newItem.getCoinId());
						}
						getOWMainActivity().openAddCurrency(usersCurWallets);
					}

					// Return because in this case we don't need to 
					// actually start a wallet.  
					return;
				}


				OWCurrencyListItem item = (OWCurrencyListItem) 
						parent.getItemAtPosition(position);
				// If we are using the test net network then we make sure the
				// user has a passphrase and 
				if (!getOWMainActivity().isShowingDialog()) {
					getOWMainActivity().openWalletView(item.getCoinId());
				}
			}
		});
		
		//Long click to deactivate wallet
		this.currencyListView.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				//ignore if long click add new currency bar
				if (currencyListView.getAdapter().getItemViewType(
						position) == OWCurrencyListAdapter.footerType){
					return false;
				}
				OWCurrencyListItem item = (OWCurrencyListItem) parent.getItemAtPosition(position);
				Bundle b = new Bundle();
				b.putString(OWCoin.TYPE_KEY, item.getCoinId().toString());
				b.putInt("ITEM_LOCATION", position);
				getOWMainActivity().alertConfirmation(OWRequestCodes.DEACTIVATE_WALLET, "Are you sure you want to de-activate this wallet?", 
						OWTags.DEACTIVATE_WALLET, b);
				return false;
			}
			
		});

	}
	
	public void removeFromView(int pos){
		this.userWallets.remove(pos);
		this.refreshListOfUserWallets();
	}

}
