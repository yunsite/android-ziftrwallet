package com.ziftr.android.ziftrwallet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURI;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURIParseException;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.dialog.ZWConfirmationDialog;
import com.ziftr.android.ziftrwallet.dialog.ZWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZWEditAddressLabelDialog;
import com.ziftr.android.ziftrwallet.dialog.ZWSimpleAlertDialog;
import com.ziftr.android.ziftrwallet.dialog.ZWValidatePassphraseDialog;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWConfirmationDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWEditAddressLabelDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWNeutralDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWResetPassphraseDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWSetNameDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWValidatePassphraseDialogHandler;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.fragment.ZWAboutFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWAccountsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWNewCurrencyFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWNewCurrencyListItem;
import com.ziftr.android.ziftrwallet.fragment.ZWReceiveCoinsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWRequestCodes;
import com.ziftr.android.ziftrwallet.fragment.ZWSearchableListAdapter;
import com.ziftr.android.ziftrwallet.fragment.ZWSearchableListItem;
import com.ziftr.android.ziftrwallet.fragment.ZWSecurityFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSendCoinsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSetFiatFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSettingsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWTags;
import com.ziftr.android.ziftrwallet.fragment.ZWTermsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWTransactionDetailsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWWalletFragment;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.network.ZiftrNetworkHandler;
import com.ziftr.android.ziftrwallet.network.ZiftrNetworkManager;
import com.ziftr.android.ziftrwallet.sqlite.ZWSQLiteOpenHelper;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * This is the main activity of the OneWallet application. It handles
 * the menu drawer and the switching between the different fragments 
 * depending on which task the user selects.
 */
public class ZWMainFragmentActivity extends ActionBarActivity 
implements DrawerListener, ZWValidatePassphraseDialogHandler, ZWNeutralDialogHandler, 
ZWResetPassphraseDialogHandler, ZWConfirmationDialogHandler, ZWEditAddressLabelDialogHandler, ZWSetNameDialogHandler, OnClickListener, 
ZiftrNetworkHandler {

	/*
	--- TODO list for the OneWallet project ---

	X 1. Ability to generate new address upon user request
	and be able to turn that into a QR code

	o 2. Get transaction history for all addresses in wallet and
	be able to display them.

	o 3. Making layouts necessary for the wireframe models.

	o 4. Organizing tasks that need to be done and appr. difficulty

	o 5. Learn how to use sqlite and use it to save data about transactions, such
	as the title of the transaction.

	X 6. ZiftrUtils and Zlog for static useful methods.
	ex. ZLog.log("aa", "b"); (get's exception message, as well)
	also autotags comments with class name and shuts itself off at
	launch time for release build.

	X 7. Move all dialog stuff into dialog package and make dialogs
	persistent.

	X 8. Get QR code scanning example working

	X 9. Get a list interface on top right corner. Move over to ActionBar
	instead of custom header layout.

	X 10. Get a reset working for the passphrase.

	X 11. Get Fragment switching working with selection in the drawer layout

	X 12. Put menu icon on the top left part of action bar rather than the right.
	To do this we need to make a custom action bar. Maybe just keep it on the right?
	In general, need to figure out how to make a more customizable action bar, 
	might just have to use custom actionbar layout.

	X 13. make it so that the content is moved over whenever we click the menu 
	button.

	X 14. Turn the EditText field in the passphrase dialog into an xml just like
	the reset passphrase dialog.

	o 15. Make the IO in loading/saving a wallet into an asynchrynous task
	so as to not block the UI. Some lagging can be seen now, such as when you just
	get into the accounts section and you try to rotate the screen horizontally.

	X 16. Make the backgrounds of the grid view in the new currency dialog
	reflect which item is currenly selected.

	X 17. Add a nicer display to the dialogs than the default.

	o 18. Redesign the send coins screen now that I know a little more about 
	android layouts.

	o 19. Make it so that the wallet is actually user passphrase protected. Right
	now it just won't let you in unless you know the passphrase. We want to actually
	encrypt the wallet.

	o 20. Make the �add new currency' bar a footer of list rather than last element.
	Make sure that all list views have headers/footers where appropriate rather
	than just extra views in the xml files. 

	X 21. Don't show a pending transaction bar if there aren't any pending. 

	o 22. Add descriptions to each of the resources (like @string/ resources).

	X 23. Need to remember where we were when navigating back the accounts section

	o 24. Should only need to have passphrase when sending coins.

	X 25. Need to have all the fragments start fragments by routing through
	the activity so that the activity can do more complicated things.

	o 26. BUG: sending 0 testnet coins to valid address throws IllegalStateException (actually might just be bitcoinj)

	TODO Maybe we don't want to set up all the open wallets right from the get go? 
	But we need to get the balance...
	This will be made easier with the API.

	 */

	/** The drawer layout menu. */
	private DrawerLayout menuDrawer;
	
	/** available coins that the user can generate wallets for */
	private List<ZWCoin> availCoins;

	/** Use this key to save which section of the drawer menu is open. */
	private static final String SELECTED_SECTION_KEY = "SELECTED_SECTION_KEY";

	/** Use this key to save whether or not the search bar is visible (vs gone). */
	private static final String SEARCH_BAR_VISIBILITY_KEY = "SEARCH_BAR_VISIBILITY_KEY";

	/** The main view that all of the fragments will be stored in. */
	private View baseFragmentContainer;

	/** A float used in making the container move over when the drawer is moved. */
	private float lastTranslate = 0.0f;

	/** This object is responsible for all the wallets. */
	private ZWWalletManager walletManager;

	/** Boolean determining if a dialog is shown, used to prevent overlapping dialogs */
	private boolean showingDialog = false;

	/** reference to searchBarEditText */
	private EditText searchEditText;

	private ZWCoin selectedCoin;
	private ImageView syncButton; //the button in various fragments users can press to sync their data
	private boolean isSyncing = false;
	
	

	/**
	 * This is an enum to differentiate between the different
	 * sections of the app. Each enum also holds specific information related
	 * to each of fragment types.
	 */
	public enum FragmentType {
		/** The enum to identify the accounts fragment of the app. */
		ACCOUNT_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWAccountsFragment();
			}
		}, 
		/** The enum to identify the terms fragment of the app. */
		TERMS_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWTermsFragment();
			}
		}, 
		/** The enum to identify the settings fragment of the app. */
		SETTINGS_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWSettingsFragment();
			}
		}, 
		/** The enum to identify the about fragment of the app. */
		ABOUT_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWAboutFragment();
			}
		},
		/** The enum to identify the contact fragment of the app. */
		SECURITY_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWSecurityFragment();
			}
		};

		/** The drawer menu view associated with this section of the app. */
		private View drawerMenuView;

		/**
		 * @return a new fragment of the type specified.
		 */
		public abstract Fragment getNewFragment();

		/**
		 * @return the drawerMenuView
		 */
		public View getDrawerMenuView() {
			return drawerMenuView;
		}

		/**
		 * @param drawerMenuView the drawerMenuView to set
		 */
		public void setDrawerMenuView(View drawerMenuView) {
			this.drawerMenuView = drawerMenuView;
		}
	};

	/**
	 * Loads up the views and starts the ZWHomeFragment 
	 * if necessary. 
	 * 
	 * @param savedInstanceState - The stored bundle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (ZWPreferencesUtils.getLogToFile()){
			ZLog.setLogger(ZLog.FILE_LOGGER);
		}
		ZLog.log("\nMain Activity Created  " + (new Date()) + "\n");

		// Everything is held within this main activity layout
		this.setContentView(R.layout.activity_main);

		// Recreate wallet manager
		this.walletManager = ZWWalletManager.getInstance();
		
		// Get the saved cur selected coin type
		this.initializeCoinType(savedInstanceState);
		
		//load available coins from API blockchains
		initAvailableCoins();
		
		// Get passphrase from welcome screen if exists
		this.handleWelcomeActivityResults();

		// Set up header and visibility of header
		this.initializeHeaderViewsVisibility(savedInstanceState);

		// Set up the drawer and the menu button
		this.initializeDrawerLayout();

		// Make sure the base fragment view is initialized
		this.initializeBaseFragmentContainer(savedInstanceState);

		// Make sure the action bar changes with what fragment we are in
		this.initializeActionBar();

		// Hook up the search bar to show the keyboard without messing up the view
		this.initializeSearchBarText();
		
		if (getIntent() != null){
			//Check if loaded from widget
			if (getIntent().hasExtra(ZWWalletWidget.WIDGET_RECEIVE) || getIntent().hasExtra(ZWWalletWidget.WIDGET_SEND)){
				this.setSelectedCoin(ZWCoin.valueOf(ZWPreferencesUtils.getWidgetCoin()));
				if (this.selectedCoin != null && this.getWalletManager().typeIsActivated(this.selectedCoin)) {
					if (getIntent().hasExtra(ZWWalletWidget.WIDGET_SEND)){
						getIntent().removeExtra(ZWWalletWidget.WIDGET_SEND);
						openSendCoinsView(null, null);
					} else{
						getIntent().removeExtra(ZWWalletWidget.WIDGET_RECEIVE);
						openReceiveCoinsView(null);
					}
				}
			//loaded from coin uri
			} else if (getIntent().getAction() == Intent.ACTION_VIEW){
				String data = getIntent().getDataString();
				ZWCoin coin = ZWCoin.valueOf(data.substring(0, data.indexOf(':')));
				this.setSelectedCoin(coin);
				if (this.selectedCoin != null && this.getWalletManager().typeIsActivated(this.selectedCoin)){
					try {
						ZWCoinURI uri = new ZWCoinURI(coin, data);
						openSendCoinsView(uri.getAddress(), uri.getAmount());
					} catch (ZWCoinURIParseException e) {
						e.printStackTrace();
					} catch (ZWAddressFormatException e) {
						this.alertUser("The address is not valid.", "invalid_uri_address_dialog");
					}
				}
			}
		}
		ZiftrNetworkManager.registerNetworkHandler(this);
	}
	public void onNewIntent(Intent intent){
		ZLog.log("waffles");
		super.onNewIntent(intent);
		
	}
	/**
	 * This method handles the results from the welcome activity. Specifically,
	 * if the intent that brought us to this activity contains a passphrase or a bundle
	 * here then we take appropriate action using those values.
	 */
	private void handleWelcomeActivityResults() {
		Bundle info = getIntent().getExtras();
		if (info != null) {
			if (info.getString(ZWPreferencesUtils.BUNDLE_PASSPHRASE_KEY) != null) {
				if (ZWPreferencesUtils.userHasPassphrase()) {
					throw new IllegalArgumentException("Cannot have a passphrase and be setting in onCreate");
				} else {
					// We can only get here if there is no previous passphrase, so we put in null
					changePassphrase(null, info.getString(ZWPreferencesUtils.BUNDLE_PASSPHRASE_KEY));
				}
			}

			if (info.getString(ZWPreferencesUtils.BUNDLE_NAME_KEY) != null) { 
				ZWPreferencesUtils.setUserName(info.getString(ZWPreferencesUtils.BUNDLE_NAME_KEY));
			}
		}
	}

	/**
	 * Create the options menu.
	 * Updates the icon appropriately.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//setup actionbar menu button
		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		menuButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onActionBarMenuIconClicked();		
			}

		});

		// Make sure the icon matches the current open/close state
		this.setActionBarMenuIcon(this.drawerMenuIsOpen());
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Here we save save which part of the app is currently open in
	 * the drawer.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save which part of the app is currently open.
		outState.putString(SELECTED_SECTION_KEY, this.getCurrentlySelectedDrawerMenuOption());
		outState.putInt(SEARCH_BAR_VISIBILITY_KEY, getSearchBar().getVisibility());
		if (this.getSelectedCoin() != null) {
			outState.putString(ZWCoin.TYPE_KEY, this.getSelectedCoin().toString());
		}
	}

	/**
	 * When the back button is pressed from this activity we
	 * go to the accounts fragment if we are somewhere else, and from
	 * the accounts fragment we exit the app. 
	 */
	@Override
	public void onBackPressed() {
		if (drawerMenuIsOpen()){
			this.menuDrawer.closeDrawer(Gravity.LEFT);
			return;
		}
		ZWFragment topFragment = this.getTopDisplayedFragment();
		if (topFragment != null && topFragment.handleBackPress()) {
			return;
		}

		String curSelected = getCurrentlySelectedDrawerMenuOption();
		if (curSelected == null) {
			super.onBackPressed();
		} else if (FragmentType.ACCOUNT_FRAGMENT_TYPE.toString().equals(curSelected)) {
			super.onBackPressed();
			//if we are in set fiat screen, back should go back to settings
		} else if (topFragment.getTag().equals(ZWTags.SET_FIAT)) {
			super.onBackPressed();
		} else {
			//Select accounts in drawer since anywhere you hit back, you will end up in accounts
			selectSingleDrawerMenuOption(findViewById(R.id.menuDrawerAccountsLayout));
			if (!getSupportFragmentManager().popBackStackImmediate(ZWTags.ACCOUNTS_INNER, 0)) {
				ZWMainFragmentActivity.this.showFragmentFromType(
						FragmentType.ACCOUNT_FRAGMENT_TYPE, true);
				//clear back stack
				this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}

	private ZWFragment getTopDisplayedFragment() {
		return (ZWFragment) this.getSupportFragmentManager().findFragmentById(R.id.oneWalletBaseFragmentHolder);
	}

	/**
	 * Here we need to close all the wallets. 
	 */
	@Override
	protected void onDestroy() {
		ZWWalletManager.closeInstance();
		super.onDestroy();
	}

	/**
	 * When the user hits the sync button, we get the top fragment
	 * and refresh it's data.
	 * 
	 * @param v
	 */
	@Override
	public void onClick(View v) {
		if (v == syncButton) {
			ZWFragment top = this.getTopDisplayedFragment();
			if (top != null) {
				top.refreshData();
			}
		}
	}




	/**
	 * Starts a new fragment in the main layout space depending
	 * on which fragment FragmentType is passed in. 
	 * 
	 * @param fragmentType - The identifier for which type of fragment to start.
	 * @param addToBackStack - boolean determining if fragment should be added to backstack
	 */
	public void showFragmentFromType(FragmentType fragmentType, Boolean addToBackStack) {
		// Go through menu options and select the right one, note  
		// that not all menu options can be selected
		if (fragmentType.getDrawerMenuView() != null) {
			this.selectSingleDrawerMenuOption(fragmentType.getDrawerMenuView());
		} else {
			ZLog.log("The drawer menu was null, can't select... shouldn't happen2");
		}

		// The tag for fragments of this fragmentType
		String tag = fragmentType.toString();

		// Make the new fragment of the correct type
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			// If the fragment doesn't exist yet, make a new one
			fragToShow = fragmentType.getNewFragment();
		}

		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder, addToBackStack, fragmentType.toString());
	}

	/**
	 * 
	 * @param fragmentType - 
	 * @param tag - The tag of the new fragment.
	 * @param resId - The view id to show the new fragment in.
	 */
	public void showFragment(Fragment fragToShow, String tag, 
			int resId, boolean addToBackStack, String backStackTag) {
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();

		// TODO add animation to transaciton here
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);

		if (fragToShow.isVisible()) {
			// If the fragment is already visible, no need to do anything
			return;
		}

		transaction.replace(resId, fragToShow, tag);
		if (addToBackStack) {
			transaction.addToBackStack(backStackTag);
		}
		transaction.commit();
	}



	/**
	 * A convenience method to toggle the drawer menu position.
	 * Also sets the icons to a static resource while opening/closing
	 * to avoid extra swapping.
	 */
	@SuppressLint("RtlHardcoded")
	private void toggleDrawerPosition() {
		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);


		if (this.menuDrawer != null) {
			if (this.drawerMenuIsOpen()) {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is closing
				menuButton.setImageResource(R.drawable.menu_white_enabled);
				// If drawer menu is open, close it
				this.menuDrawer.closeDrawer(Gravity.LEFT);
			} else {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is opening
				menuButton.setImageResource(R.drawable.menu_white_disabled);
				// If drawer menu is closed, open it
				this.menuDrawer.openDrawer(Gravity.LEFT);
			}
		}

	}

	/**
	 * Should be called after the view is initialized only.
	 * Returns a list of the views that are in the drawer menu.
	 * 
	 * @return a list of the views that are in the drawer menu.
	 */
	private List<View> getDrawerMenuItems() {
		// Make a new list
		List<View> selectionItems = new ArrayList<View>();

		// Add all the selection views to it
		selectionItems.add(this.findViewById(R.id.menuDrawerAccountsLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerTermsLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerSettingsLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerAboutLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerSecurityLayout));

		// return the result
		return selectionItems;
	}

	/**
	 * Does all the necessary tasks to set up the drawer layout.
	 * This incudes setting up the menu button so that it opens and
	 * closes the drawer layout appropriately.
	 * 
	 * @param savedInstanceState - The saved instance state 
	 */
	private void initializeDrawerLayout() {
		this.menuDrawer = (DrawerLayout) this.findViewById(R.id.oneWalletBaseDrawer);

		if (this.menuDrawer != null) {
			this.menuDrawer.setDrawerListener(this);

			// Set up for accounts section
			View accountsMenuButton = 
					this.findViewById(R.id.menuDrawerAccountsLayout);
			FragmentType.ACCOUNT_FRAGMENT_TYPE.setDrawerMenuView(
					accountsMenuButton);
			accountsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					//If accounts is currently selected, go to main accounts page and clear backstack so if user hits back, app exits.
					if (FragmentType.ACCOUNT_FRAGMENT_TYPE.toString().equals(
							ZWMainFragmentActivity.this.getCurrentlySelectedDrawerMenuOption())) {
						//clear back stack
						ZWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

						ZWMainFragmentActivity.this.showFragmentFromType(
								FragmentType.ACCOUNT_FRAGMENT_TYPE, false);

						//Else accounts is not selected so resume previous accounts activity
					} else if (!getSupportFragmentManager().popBackStackImmediate(
							ZWTags.ACCOUNTS_INNER, 0)) {
						ZWMainFragmentActivity.this.showFragmentFromType(
								FragmentType.ACCOUNT_FRAGMENT_TYPE, true);
						ZWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

					}
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
				}
			});

			// Set up for terms section
			View termsMenuButton = 
					this.findViewById(R.id.menuDrawerTermsLayout);
			FragmentType.TERMS_FRAGMENT_TYPE.setDrawerMenuView(
					termsMenuButton);
			termsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.TERMS_FRAGMENT_TYPE, true);
				}
			});

			// Set up for settings section
			View settingsMenuButton = 
					this.findViewById(R.id.menuDrawerSettingsLayout);
			FragmentType.SETTINGS_FRAGMENT_TYPE.setDrawerMenuView(
					settingsMenuButton);
			settingsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.SETTINGS_FRAGMENT_TYPE, true);
				}
			});

			// Set up for about section
			View aboutMenuButton = 
					this.findViewById(R.id.menuDrawerAboutLayout);
			FragmentType.ABOUT_FRAGMENT_TYPE.setDrawerMenuView(
					aboutMenuButton);
			aboutMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.ABOUT_FRAGMENT_TYPE, true);
				}
			});

			// Set up for contact section
			View securityMenuButton = 
					this.findViewById(R.id.menuDrawerSecurityLayout);
			FragmentType.SECURITY_FRAGMENT_TYPE.setDrawerMenuView(
					securityMenuButton);
			securityMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.SECURITY_FRAGMENT_TYPE, true);
				}
			});

		} else {
			ZLog.log("drawerMenu was null. ?");
		}

	}

	/**
	 * Initializes the drawer layout. Sets up some view fields
	 * and opens the right fragment if the bundle has extra information
	 * in it.
	 */
	private void initializeCoinType(Bundle args) {
		if (args != null) {
			if (args.getString(ZWCoin.TYPE_KEY) != null) {
				// Need to do this so that we populate the wallet header view as well
				this.setSelectedCoin(ZWCoin.valueOf(args.getString(ZWCoin.TYPE_KEY)));
			}
		}
	}

	private void initializeHeaderViewsVisibility(Bundle args) {
		if (args != null) {
			if (args.getInt(SEARCH_BAR_VISIBILITY_KEY, -1) != -1) {
				this.getSearchBar().setVisibility(args.getInt(SEARCH_BAR_VISIBILITY_KEY));
			}
		}
	}

	private void initializeBaseFragmentContainer(Bundle savedInstanceState) {
		// Set the base fragment container
		this.baseFragmentContainer = this.findViewById(R.id.oneWalletBaseHolder);

		// To make sure the content doesn't go behind the drawer menu
		this.baseFragmentContainer.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						// At this point the layout is complete and the 
						// dimensions of the view and any child views are known.
						float offset;
						if (drawerMenuIsOpen()) {
							offset = menuDrawer.findViewById(
									R.id.menuDrawerScrollView).getWidth();
						} else {
							offset = 0;
						}
						baseFragmentContainer.setTranslationX(offset);
					}
				});

		// If the app has just been launched (so the fragment
		// doesn't exist yet), we create the main fragment.
		if (savedInstanceState == null) {
			this.showFragmentFromType(FragmentType.ACCOUNT_FRAGMENT_TYPE, false);
		} else {
			// Here we must make sure that the drawer menu is still
			// showing which section of the app is currently open.
			String prevSelectedSectionString = savedInstanceState.getString(SELECTED_SECTION_KEY);
			if (prevSelectedSectionString != null) {
				FragmentType fragmentType = FragmentType.valueOf(prevSelectedSectionString);
				if (fragmentType.getDrawerMenuView() != null) {
					this.selectSingleDrawerMenuOption(fragmentType.getDrawerMenuView());
				} else {
					ZLog.log("The drawer menu was null, can't select... shouldn't happen1");
				}
			}
		}
	}

	private void initializeActionBar() {
		// Set up actionbar
		ActionBar actionbar = this.getActionBar();
		actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionbar.setCustomView(R.layout._app_header_bar);
	}

	private void initializeSearchBarText() {
		//listener for when searchBar text has focus, shows keyboard if focused and removes keyboard if not
		this.searchEditText = (EditText) findViewById(R.id.searchBarContainer).findViewWithTag(ZWTags.ZW_EDIT_TEXT);
		this.searchEditText.clearFocus();
		this.searchEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (hasFocus) {
					imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
				} else {
					imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
				}
			}
		});
	}

	/**
	 * Should be called whenever any of the menu items in the
	 * drawer menu are selected. Basically just contains code 
	 * common to all of the tasks so that we can avoid code
	 * duplication. 
	 * 
	 * @param selectedView The view selected.
	 */
	private void onAnyDrawerMenuItemClicked(View selectedView) {
		// Select the view and deselect all the other views
		this.selectSingleDrawerMenuOption(selectedView);

		// Menu must have been open, so calling this will just 
		// result in a close of the menu.
		this.toggleDrawerPosition();;

	}

	/**
	 * Selects the drawer menu option given, deselects all
	 * the other drawer menu options.
	 * 
	 * @param selectedView - The view to select.
	 */
	private void selectSingleDrawerMenuOption(View selectedView) {
		// Deselect all the other views
		for (View selectionView : this.getDrawerMenuItems()) {
			selectionView.setSelected(false);
		}

		// Mark this one as selected to have green selector view
		// next to it in the drawer layout
		selectedView.setSelected(true);
	}

	/**
	 * Gets the string identifier of the currently selected
	 * drawer menu option.
	 * For example, if we are in the accounts section, this
	 * method will return "ACCOUNT_FRAGMENT_TYPE".
	 */
	private String getCurrentlySelectedDrawerMenuOption() {
		// Deselect all the other views
		for (FragmentType fragType : FragmentType.values()) {
			if (fragType.getDrawerMenuView() != null && 
					fragType.getDrawerMenuView().isSelected()) {
				return fragType.toString();
			}
		}
		return null;
	}

	/**
	 * Gives a boolean that describes whether or not the drawer menu is
	 * currenly open from the left side of the screen. 
	 * 
	 * @return a boolean that describes whether or not the drawer menu is
	 * currenly open
	 */
	@SuppressLint("RtlHardcoded")
	private boolean drawerMenuIsOpen() {
		return this.menuDrawer.isDrawerOpen(Gravity.LEFT);
	}

	/**
	 * Called when the drawer menu item in the action bar is clicked.
	 * 
	 * This name is confusing because there is a drawer menu and then 
	 * there is the menu in the action bar that has a menu icon in it, 
	 * which is a clickable that opens the drawer menu.
	 */
	private void onActionBarMenuIconClicked() {
		this.toggleDrawerPosition();
	}

	/**
	 * Sets the icon to the appropriate statelist, depending on
	 * the given boolean to determine. Usually the boolean will
	 * be given by whether or not the drawer menu is open.
	 * 
	 * @param open - whether or not the drawer menu is open
	 */
	private void setActionBarMenuIcon(boolean open) {
		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		if (open) {
			menuButton.setImageResource(R.drawable.icon_menu_statelist_reverse);
		} else {
			menuButton.setImageResource(R.drawable.icon_menu_statelist);
		}
	}

	/**
	 * Sets the stored passphrase hash to be the specified
	 * hash.
	 * 
	 * @param oldPassphrase - Used to decrypt old keys, if not null
	 * @param newPassphrase - Used to encrypt all keys, should not be null
	 */
	private boolean changePassphrase(final String oldPassphrase, final String newPassphrase) {
		// TODO put up a blocking dialog while this is happening
		if (newPassphrase == null || newPassphrase.isEmpty()) {
			this.alertUser("A password with no text is no password at all!", "passphrase_is_empty_string");
			return false;
		}

		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				walletManager.changeEncryptionOfReceivingAddresses(oldPassphrase, newPassphrase);

				ZWMainFragmentActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						SharedPreferences prefs = ZWPreferencesUtils.getPrefs();
						Editor editor = prefs.edit();
						String saltedHash = ZiftrUtils.saltedHashString(newPassphrase);
						editor.putString(ZWPreferencesUtils.PREFS_PASSPHRASE_KEY, saltedHash);
						editor.commit();
					}
				});
			}
		});
		return true;
	}
	

	/**
	 * called after user enters password for creating new wallet 
	 * 
	 * @param bundle with ZWCoin of wallet to add
	 */
	public void addNewCurrency(ZWNewCurrencyListItem item) {
		ZWCoin newItem = item.getCoinId();
		// TODO we can probably get rid of this if it's slowing stuff down - unnecessary check 
		// Make sure that this view only has wallets
		// in it which the user do
		for (ZWCoin type : this.walletManager.readAllActivatedTypes()) {
			if (type == newItem) {
				// Already in list, shouldn't ever get here though because
				// we only show currencies in the dialog which we don't have
				this.onBackPressed();
				return;
			}
		}
		
		
		// We can assume the wallet hasn't been set up yet
		// or we wouldn't have gotten here 
		if (!walletManager.walletHasBeenSetUp(newItem)) {
			if (walletManager.setUpWallet(newItem)) {
			} else {
				Toast.makeText(this, "Error a wallet could not be set up!", Toast.LENGTH_LONG).show();
				return;
			}
		}
		Toast.makeText(this, "Wallet Created!", Toast.LENGTH_LONG).show();
		this.onBackPressed();

	}

	/**
	 * Only works for enabled types right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openWalletView(ZWCoin typeOfWalletToStart) {
		this.setSelectedCoin(typeOfWalletToStart);

		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(ZWTags.WALLET_FRAGMENT);
		if (fragToShow == null) {
			fragToShow = new ZWWalletFragment();
		}

		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, ZWTags.WALLET_FRAGMENT, 
				R.id.oneWalletBaseFragmentHolder, true, ZWTags.ACCOUNTS_INNER);
	}

	/**
	 * A convenience method that we can use to start a dialog from this bundle.
	 * The bundle should contain one string ZWCoin.___.toString() which 
	 * can be extracted by getting from the bundle with the key ZWCoin.TYPE_KEY.
	 * 
	 * @param info - The bundle used to tell which wallet to open.
	 */
	public void openWalletViewFromBundle(Bundle info) {
		if (info != null) {
			this.openWalletView(ZWCoin.valueOf(info.getString(ZWCoin.TYPE_KEY)));
		}
	}

	/**
	 * Only works for enabled types right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openReceiveCoinsView(ZWAddress address) {
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(ZWTags.RECIEVE_FRAGMENT);
		if (fragToShow == null) {
			fragToShow = new ZWReceiveCoinsFragment();
		}

		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, ZWTags.RECIEVE_FRAGMENT, R.id.oneWalletBaseFragmentHolder, 
				true, ZWTags.ACCOUNTS_INNER);
		if (address != null){
			((ZWReceiveCoinsFragment) fragToShow).setReceiveAddress(address);		
		}
	}

	/**
	 * Only works for enabled types type right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openSendCoinsView(String address, String amount) {
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(ZWTags.SEND_FRAGMENT);
		if (fragToShow == null) {
			fragToShow = new ZWSendCoinsFragment();
		}
		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, ZWTags.SEND_FRAGMENT, R.id.oneWalletBaseFragmentHolder, 
				true, ZWTags.ACCOUNTS_INNER);

		if (address!=null){
			((ZWSendCoinsFragment) fragToShow).setSendToAddress(address);		
		}
		if (amount!=null){
			((ZWSendCoinsFragment) fragToShow).setAmount(amount);		
		}
	}

	/**
	 * Open the view for transaction details
	 */
	public void openTxnDetails(ZWTransaction txItem) {
		ZWTransactionDetailsFragment fragToShow = (ZWTransactionDetailsFragment) 
				this.getSupportFragmentManager().findFragmentByTag(ZWTags.TXN_DETAILS);
		if (fragToShow == null) {
			fragToShow = new ZWTransactionDetailsFragment();
		}
		Bundle b= new Bundle();
		b.putString(ZWTransactionDetailsFragment.TX_ITEM_HASH_KEY, txItem.getSha256Hash());
		fragToShow.setArguments(b);
		this.showFragment(fragToShow, ZWTags.TXN_DETAILS, R.id.oneWalletBaseFragmentHolder, true, 
				ZWTags.ACCOUNTS_INNER);
	}

	/**
	 * Open view for add new currency
	 */
	public void openAddCurrency(List<ZWCoin> userCurWallets) {
		if (drawerMenuIsOpen()){
			this.menuDrawer.closeDrawer(Gravity.LEFT);
		}
		ZWNewCurrencyFragment fragToShow = (ZWNewCurrencyFragment) 
				this.getSupportFragmentManager().findFragmentByTag(ZWTags.ADD_CURRENCY);
		if (fragToShow == null) {
			fragToShow = new ZWNewCurrencyFragment();
		}
		// Here we get all the coins that the user doesn't currently 
		// have a wallet for because those are the ones we put in the new view.
		Set<ZWCoin> coinsNotInListCurrently = new HashSet<ZWCoin>(availCoins);
		for (ZWCoin type : userCurWallets){
			if (coinsNotInListCurrently.contains(type)){
				coinsNotInListCurrently.remove(type);
			}
		}
		Bundle b = new Bundle();
		for (ZWCoin type : coinsNotInListCurrently) {
			b.putBoolean(type.toString(), true);
		}
		fragToShow.setArguments(b);
		this.showFragment(fragToShow, ZWTags.ADD_CURRENCY, R.id.oneWalletBaseFragmentHolder, true, 
				ZWTags.ACCOUNTS_INNER);

	}
	
	//initialize available coin currencies the user can choose to add
	public void initAvailableCoins(){
		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				availCoins = ZWDataSyncHelper.getBlockChainWallets();
				//if API call failed
				if (availCoins.size() <= 0){
					availCoins = new ArrayList<ZWCoin>();
					for (ZWCoin coin : ZWCoin.TYPES){
						availCoins.add(coin);
					}
				}
				
				if (!ZWPreferencesUtils.getDebugMode()){
					availCoins.removeAll(Arrays.asList(ZWCoin.TYPES_TEST));
				}
				updateMarketValues();
			}
		});
	}
	
	//update currency exchange rates
	public void updateMarketValues(){
		ZWDataSyncHelper.getMarketValue();
	}

	/**
	 * Open View for selecting fiat currency in settings
	 */
	public void openSetFiatCurrency(){
		ZWSetFiatFragment fragToShow = (ZWSetFiatFragment) this.getSupportFragmentManager().findFragmentByTag(ZWTags.SET_FIAT);
		if (fragToShow == null){
			fragToShow = new ZWSetFiatFragment();
		}
		this.showFragment(fragToShow, ZWTags.SET_FIAT, R.id.oneWalletBaseFragmentHolder, true, ZWTags.SET_FIAT);

	}

	/**
	 * @return the walletManager
	 */
	public ZWWalletManager getWalletManager() {
		return walletManager;
	}

	/**
	 * @return the headerBar
	 */
	public View getSearchBar() {
		return this.findViewById(R.id.searchBar);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Placeholder for anything we might need to do with the results

		// Goes here first, then this calls the current fragment's on activity result
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDrawerClosed(View arg0) {
		this.setActionBarMenuIcon(false);
	}

	@Override
	public void onDrawerOpened(View arg0) {
		this.setActionBarMenuIcon(true);
	}

	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		// Here we need to 
		float moveFactor = (drawerView.getWidth() * (slideOffset - this.lastTranslate));
		this.baseFragmentContainer.setTranslationX(moveFactor);
	}

	@Override
	public void onDrawerStateChanged(int newState) {
		// Nothing to do
	}

	/**
	 * Pops up a dialog to give the user some information. 
	 * 
	 * @param message
	 * @param tag
	 */
	public void alertUser(String message, String tag) {
		ZWSimpleAlertDialog alertUserDialog = new ZWSimpleAlertDialog();

		Bundle args = new Bundle();
		args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, ZWRequestCodes.ALERT_USER_DIALOG);
		alertUserDialog.setArguments(args);

		// Set negative text to null to not have negative button
		alertUserDialog.setupDialog("ziftrWALLET", message, null, "OK", null);
		alertUserDialog.show(this.getSupportFragmentManager(), tag);
	}

	public void alertConfirmation(int requestcode, String message, String tag, Bundle args) {
		ZWConfirmationDialog confirmDialog = new ZWConfirmationDialog();

		args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, requestcode);
		confirmDialog.setArguments(args);
		confirmDialog.setupDialog("ziftrWALLET", message, "Continue", null, "Cancel");

		if (!isShowingDialog()) {
			confirmDialog.show(this.getSupportFragmentManager(), tag);
		}
	}

	/**
	 * generate dialog to ask for passphrase
	 * 
	 * @param requestcode = ZWRequestcodes parameter to differentiate where the password dialog is
	 * @param args = bundle with ZWCoinType of currency to add if user is adding currency
	 */
	public void showEditAddressLabelDialog(int requestcode, boolean isReceiving, Bundle args, String tag) {
		if (!isShowingDialog()) {
			ZWEditAddressLabelDialog dialog = new ZWEditAddressLabelDialog();

			args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, requestcode);
			args.putBoolean(ZWEditAddressLabelDialog.IS_RECEIVING_NOT_SENDING_KEY, isReceiving);
			dialog.setArguments(args);

			String message = "Edit the below address' label. ";
			dialog.setupDialog("ziftrWALLET", message, "Done", null, "Cancel");

			dialog.show(this.getSupportFragmentManager(), tag);
		}
	}

	
	/**
	 * generate dialog to ask for passphrase
	 * 
	 * @param requestcode = ZWRequestcodes parameter to differentiate where the password dialog is
	 * @param args = bundle with ZWCoinType of currency to add if user is adding currency
	 */
	public void showGetPassphraseDialog(int requestcode, Bundle args, String tag) {
		if (!isShowingDialog()) {
			ZWValidatePassphraseDialog passphraseDialog = new ZWValidatePassphraseDialog();

			args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, requestcode);
			passphraseDialog.setArguments(args);

			String message = "Please input your passphrase. ";
			passphraseDialog.setupDialog("ziftrWALLET", message, "Continue", null, "Cancel");

			passphraseDialog.show(this.getSupportFragmentManager(), tag);
		}
	}

	
	///////////// Handler methods /////////////

	@Override
	public void handleNegative(int requestCode) {
		switch(requestCode) {
		case ZWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_SEND:
			// Nothing to do
			break;
		case ZWRequestCodes.ALERT_USER_DIALOG:
			// Nothing to do
			break;
		case ZWRequestCodes.RESET_PASSPHRASE_DIALOG:
			// Nothing to do
			break;
		case ZWRequestCodes.DEACTIVATE_WALLET:
			// Nothing to do
			break;
		}
	}
	
	
	@Override
	public void handlePassphrasePositive(int requestCode, String passphrase, Bundle info) {
		byte[] inputHash = ZiftrUtils.saltedHash(passphrase);
		
		if (requestCode == ZWRequestCodes.DEBUG_MODE_PASSPHRASE_DIALOG && passphrase.equals("orca")){
			ZWPreferencesUtils.setDebugMode(true);
			this.initAvailableCoins(); //re-init coins to show testnet in debug mode
			((ZWSettingsFragment)this.getSupportFragmentManager(
					).findFragmentByTag(FragmentType.SETTINGS_FRAGMENT_TYPE.toString())).updateSettingsVisibility(true);
			return;
		}
		
		if (ZWPreferencesUtils.inputHashMatchesStoredHash(inputHash)) {
			switch(requestCode) {
			case ZWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_KEY:
				ZWReceiveCoinsFragment receiveFrag = (ZWReceiveCoinsFragment) getSupportFragmentManager(
						).findFragmentByTag(ZWTags.RECIEVE_FRAGMENT);
				receiveFrag.loadNewAddressFromDatabase(passphrase);
				break;
			case ZWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_SEND:
				ZWSendCoinsFragment sendFrag = (ZWSendCoinsFragment) getSupportFragmentManager(
						).findFragmentByTag(ZWTags.SEND_FRAGMENT);
				sendFrag.onClickSendCoins(passphrase);
				break;
			case ZWRequestCodes.DISABLE_PASSPHRASE_DIALOG:
				ZWPreferencesUtils.disablePassphrase();
				ZWPreferencesUtils.setPassphraseDisabled(true);
				((ZWSettingsFragment)this.getSupportFragmentManager(
						).findFragmentByTag(FragmentType.SETTINGS_FRAGMENT_TYPE.toString())).updateSettingsVisibility(false);

				break;
			}
		} else {
			this.alertUser("Error: Passphrases don't match. ", "wrong_passphrase");
		}

	}

	
	/**
	 * By positive, this means that the user has selected 'Yes' or 'Okay' 
	 * rather than cancel or exiting the dialog. This is called when
	 * the user opens a reset passphrase dialog and 
	 */
	@Override
	public void handleResetPassphrasePositive(int requestCode,
			String oldPassphrase, String newPassphrase, String confirmPassphrase) {
		// Can assume that there was a previously entered passphrase because 
		// of the way the dialog was set up.
		if (newPassphrase.equals(confirmPassphrase)) {
			byte[] oldPassphraseHash = ZiftrUtils.saltedHash(oldPassphrase);
			// It is possible that oldPassphraseHash and the value stored in prefs are both null
			// if the user didn't have a passphrase when this was called. 
			// In that case, input hash matches the stored hash because they are both null
			if (ZWPreferencesUtils.inputHashMatchesStoredHash(oldPassphraseHash)) {
				// If the old matches, set the new passphrase hash
				if (this.changePassphrase(oldPassphrase, newPassphrase)) { 
					//if the password was changed
					if (requestCode == ZWRequestCodes.CREATE_PASSPHRASE_DIALOG) { 
						//if we were setting the passphrase, turn disabled passphrase off
						ZWPreferencesUtils.setPassphraseDisabled(false);
						((ZWSettingsFragment)this.getSupportFragmentManager(
								).findFragmentByTag(FragmentType.SETTINGS_FRAGMENT_TYPE.toString())).updateSettingsVisibility(true);
					}
				}
			} else {
				// If don't match, tell user. 
				this.alertUser("The passphrase you entered doesn't match your "
						+ "previous passphrase. Your previous passphrase is "
						+ "still in place.", "passphrases_dont_match");
			} 
		} else {
			this.alertUser("The passphrases you entered don't match. Your previous "
					+ "passphrase is still in place.", "wrong_re-enter_passphrase");
		}
	}


	@Override
	public void handleConfirmationPositive(int requestCode, Bundle info) {
		switch (requestCode) {
		case ZWRequestCodes.DEACTIVATE_WALLET:
			ZWCoin coinId = ZWCoin.valueOf(info.getString(ZWCoin.TYPE_KEY));
			this.walletManager.updateTableActivatedStatus(coinId, ZWSQLiteOpenHelper.DEACTIVATED);
			ZWAccountsFragment frag = (ZWAccountsFragment) getSupportFragmentManager(
					).findFragmentByTag(FragmentType.ACCOUNT_FRAGMENT_TYPE.toString());
			frag.removeFromView(info.getInt("ITEM_LOCATION"));
			if (this.selectedCoin == coinId){
				this.selectedCoin = null;
			}
			break;
		case ZWRequestCodes.CONFIRM_CREATE_NEW_ADDRESS:
			ZWReceiveCoinsFragment receiveFrag = (ZWReceiveCoinsFragment) getSupportFragmentManager(
					).findFragmentByTag(ZWTags.RECIEVE_FRAGMENT);
			receiveFrag.loadNewAddressFromDatabase(null);
			break;
		}
	}

	@Override
	public void handleAddressLabelChange(int requestCode, final String address, 
			final String label, final boolean isReceiving) {
		ZLog.log("update address: ", address);
		ZLog.log("update label: ", label);
		ZLog.log("update isReceive: " + isReceiving);

		switch (requestCode) {
		case ZWRequestCodes.EDIT_ADDRESS_LABEL_FROM_ADDRESS_BOOK:
			ZiftrUtils.runOnNewThread(new Runnable() {
				@Override
				public void run() {
					getWalletManager().updateAddressLabel(getSelectedCoin(), address, label, isReceiving);
					updateTopFragmentView();
				}
			});
			break;
		}
	}

	@Override
	public void handleSetNamePositive(int requestCode, String newName){
		if (newName.isEmpty()){
			this.alertUser("Looks like you wanted to disable your name instead!", "set name is empty");
		} else {
			ZWPreferencesUtils.setUserName(newName);
			((ZWSettingsFragment)this.getSupportFragmentManager(
					).findFragmentByTag(FragmentType.SETTINGS_FRAGMENT_TYPE.toString())).updateSettingsVisibility(false);
		}
	}

	public void updateTopFragmentView() {
		// If already on the UI thread, then it just does this right now
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ZWFragment topFragment = getTopDisplayedFragment();
				if (topFragment != null) {
					topFragment.onDataUpdated();
				}
			}
		});
	}
	
	@Override
	public void handleNeutral(int requestCode) {
		switch (requestCode) {
		case ZWRequestCodes.ALERT_USER_DIALOG:
			break;
		}
	}

	public void setShowingDialog(boolean showing) {
		this.showingDialog = showing;
	}

	public boolean isShowingDialog() {
		return this.showingDialog;
	}

	/**
	 * @return the curSelectedCoinType
	 */
	public ZWCoin getSelectedCoin() {
		return selectedCoin;
	}

	/**
	 * @param selectedCoin the curSelectedCoinType to set
	 */
	public void setSelectedCoin(ZWCoin selectedCoin) {
		this.selectedCoin = selectedCoin;
	}

	/**
	 * Making this a separate method to increase abstraction and just in
	 * case we ever need to call this by itself. 
	 */
	public void populateWalletHeaderView(View headerView) {
		//now that a coin type is set, setup the header view for it
		ZWFiat selectedFiat = ZWPreferencesUtils.getFiatCurrency();

		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));
		coinLogo.setImageResource(this.selectedCoin.getLogoResId());

		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(this.selectedCoin.getLongTitle());

		TextView fiatExchangeRateText = (TextView) headerView.findViewById(R.id.bottomLeftTextView);
		BigDecimal unitPriceInFiat = ZWConverter.convert(BigDecimal.ONE, this.selectedCoin, selectedFiat);
		fiatExchangeRateText.setText(selectedFiat.getFormattedAmount(unitPriceInFiat, true));

		TextView walletBalanceTextView = (TextView) headerView.findViewById(R.id.topRightTextView);
		BigInteger atomicUnits = getWalletManager().getWalletBalance(this.selectedCoin, ZWSQLiteOpenHelper.BalanceType.AVAILABLE);
		BigDecimal walletBalance = this.selectedCoin.getAmount(atomicUnits);

		walletBalanceTextView.setText(this.selectedCoin.getFormattedAmount(walletBalance));

		TextView walletBalanceInFiatText = (TextView) headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceInFiat = ZWConverter.convert(walletBalance, this.selectedCoin, selectedFiat);
		walletBalanceInFiatText.setText(selectedFiat.getFormattedAmount(walletBalanceInFiat, true));

		syncButton = (ImageView) headerView.findViewById(R.id.rightIcon);
		syncButton.setImageResource(R.drawable.icon_sync_button_statelist);
		syncButton.setOnClickListener(this);
		if(isSyncing) {
			startSyncAnimation();
		}
	}


	/**
	 * Customize actionbar
	 * @param title - text in middle of actionbar
	 * @param menu - boolean determine if menu button to open drawer is visible, if false, 
	 * then the back button will be visible.
	 * @param home - boolean to display home button
	 * @param search - boolean to display search button
	 * @param addCurrency - boolean to display + button to add new currency
	 */
	public void changeActionBar(String title, boolean menu, boolean home, boolean addCurrency) {
		this.changeActionBar(title, menu, home, addCurrency, null, null);
	}

	/**
	 * Customize actionbar
	 * @param title - text in middle of actionbar
	 * @param menu - boolean determine if menu button to open drawer is visible, if false, 
	 * then the back button will be visible.
	 * @param home - boolean to display home button
	 * @param search - boolean to display search button
	 * @param addCurrency - boolean to display + button to add new currency
	 */
	public void changeActionBar(String title, boolean menu, boolean home, boolean addCurrency,
			final TextWatcher textWatcher, 
			final ZWSearchableListAdapter<? extends ZWSearchableListItem> adapter) {

		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		ImageView backButton = (ImageView) this.findViewById(R.id.actionBarBack);
		ImageView homeButton = (ImageView) this.findViewById(R.id.actionBarHome);
		ImageView searchButton = (ImageView) this.findViewById(R.id.actionBarSearch);
		ImageView addCurrencyButton = (ImageView) this.findViewById(R.id.actionBarAddCurrency);

		final View searchBar = findViewById(R.id.searchBar);

		TextView titleview = (TextView) findViewById(R.id.actionBarTitle);
		titleview.setText(title);

		if (menu) {
			backButton.setVisibility(View.GONE);
			menuButton.setVisibility(View.VISIBLE);
		} else {
			backButton.setVisibility(View.VISIBLE);
			backButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (ZWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}
					ZWMainFragmentActivity.this.onBackPressed();
				}

			});
			menuButton.setVisibility(View.GONE);
		}


		if (home) {
			homeButton.setVisibility(View.VISIBLE);

			//setup actionbar home click listener
			homeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (ZWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}					
					//clear backstack
					ZWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(
							null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
					//show home
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.ACCOUNT_FRAGMENT_TYPE, false);
				}
			});

		} else {
			homeButton.setVisibility(View.GONE);
		}

		if (addCurrency){
			addCurrencyButton.setVisibility(View.VISIBLE);
			addCurrencyButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					List<ZWCoin> usersCurWallets = new ArrayList<ZWCoin>();
					for (ZWCoin newItem : getWalletManager().readAllActivatedTypes()) {
						usersCurWallets.add(newItem);
					}
					openAddCurrency(usersCurWallets);
				}
			});
		} else {
			addCurrencyButton.setVisibility(View.GONE);
		}

		// For search bar
		if (textWatcher != null) {
			filterAccordingToVisibility(adapter);

			this.registerSearchBarTextWatcher(textWatcher);

			View clearbutton = findViewById(R.id.clearSearchBarTextIcon);
			clearbutton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					searchEditText.setText("");
				}

			});

			searchButton.setVisibility(View.VISIBLE);

			searchButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (ZWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}					

					if (!searchBarIsVisible()) {
						searchBar.setVisibility(View.VISIBLE);
						searchEditText.requestFocus();
					} else {
						searchBar.setVisibility(View.GONE);
					}

					filterAccordingToVisibility(adapter);
				}
			});

		} else {
			searchButton.setVisibility(View.GONE);
			searchBar.setVisibility(View.GONE);
		}
	}

	private void filterAccordingToVisibility(
			final ZWSearchableListAdapter<? extends ZWSearchableListItem> adapter) {
		if (!searchBarIsVisible()) {
			// Filter
			adapter.getFilter().filter("");
		} else {
			// Stop filtering
			adapter.getFilter().filter(this.searchEditText.getText());

		}
	}

	public void registerSearchBarTextWatcher(TextWatcher textWatcher) {
		// called in onResume of all fragments that use the search bar
		// Can, instead, also be called by calling the change action bar method
		this.searchEditText.addTextChangedListener(textWatcher);
	}

	public void unregisterSearchBarTextWatcher(TextWatcher textWatcher) {
		// TODO Called in onPause of all fragments that use the search bar
		this.searchEditText.removeTextChangedListener(textWatcher);
	}

	public boolean searchBarIsVisible() {
		View search = findViewById(R.id.searchBar);
		return search.getVisibility() == View.VISIBLE;
	}

	@Override
	public void handleExpiredLoginToken() {
		//TODO -wallet app doesn't yet use login tokens
	}

	@Override
	public void handleNetworkError(String errorMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleDataUpdated() {
		this.updateTopFragmentView();
	}

	@Override
	public void networkStarted() {
		
		ZLog.log("Network started.......");
		
		this.runOnUiThread( new Runnable() {
			
			@Override
			public void run() {
				startSyncAnimation();
			}
		});
		
	}

	@Override
	public void networkStopped() {

		ZLog.log("Network stopped.......");
		
		this.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				stopSyncAnimation();
			}
		});
		
	}

	private synchronized void startSyncAnimation() {
		isSyncing = true;
		if(syncButton != null && syncButton.getVisibility() == View.VISIBLE) {
			syncButton.clearAnimation(); //clear out any old animations before starting the new one
			
			Animation rotation = AnimationUtils.loadAnimation(ZWMainFragmentActivity.this, R.anim.rotation);
			rotation.setRepeatCount(Animation.INFINITE);
			syncButton.startAnimation(rotation);
			
			rotation.setRepeatCount(0);
		}
	}
	
	private synchronized void stopSyncAnimation() {
		isSyncing = false;
		if(syncButton != null && syncButton.getVisibility() == View.VISIBLE) {
			Animation rotation = syncButton.getAnimation();
			if(rotation != null) {
				rotation.setRepeatCount(0);
			}
		}
	}
	
	
}