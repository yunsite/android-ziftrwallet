/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWTransactionDetailsFragment extends ZWWalletUserFragment 
implements ZWEditableTextBoxController.EditHandler<ZWTransaction>, OnClickListener {
	
	public static String FRAGMENT_TAG = "transaction_details_fragment";

	public static final String TX_ITEM_HASH_KEY = "txItemHash";

	private static final String IS_EDITING_KEY = "isEditing";

	private View rootView;
	private ImageView editLabelButton;
	private EditText labelEditText;
	private TextView amount;
	private TextView amountLabel;
	private TextView timeLabel;
	private TextView currency;
	private TextView confirmationFee;
	private TextView currencyType;
	private TextView time;
	private TextView addressTextView;
	private TextView status;
	private TextView timeLeft;
	private ProgressBar progressBar;
	private ImageView coinLogo;
	private ImageView reuseAddress;

	private ZWTransaction txItem;

	private ZWEditState curEditState;

	private boolean isEditing;

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.accounts_transaction_details, container, false);

		// Resize view when keyboard pops up instead of just default pan so user 
		// can see field more clearly
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		this.rootView = inflater.inflate(R.layout.accounts_transaction_details, container, false);

		this.txItem = getWalletManager().getTransaction(this.getSelectedCoin(), this.getArguments().getString(TX_ITEM_HASH_KEY));

		this.editLabelButton = (ImageView) rootView.findViewById(R.id.edit_txn_note);
		this.labelEditText = (EditText) rootView.findViewById(R.id.txn_note);
		this.amount = (TextView) rootView.findViewById(R.id.amount);
		this.amountLabel = (TextView) rootView.findViewById(R.id.amountLabel);
		this.timeLabel = (TextView) rootView.findViewById(R.id.date_label);
		this.currency = (TextView) rootView.findViewById(R.id.currencyValue);
		this.confirmationFee = (TextView) rootView.findViewById(R.id.confirmation_fee_amount);
		this.currencyType = (TextView) rootView.findViewById(R.id.currencyType);
		this.time = (TextView) rootView.findViewById(R.id.date);
		this.addressTextView = (TextView) rootView.findViewById(R.id.routing_address);
		this.status = (TextView) rootView.findViewById(R.id.status);
		this.timeLeft = (TextView) rootView.findViewById(R.id.time_left);
		this.progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
		this.coinLogo = (ImageView) rootView.findViewById(R.id.coin_logo);
		this.reuseAddress = (ImageView) rootView.findViewById(R.id.reuse_address);
		this.initFields(savedInstanceState);

		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(IS_EDITING_KEY, this.isEditing);
		if (this.isEditing) {
			ZWEditState.saveIntoBundle(curEditState, outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("TRANSACTION", false, false, false);
	}

	/**
	 * Get the arguments passed from the activity and set the text fields 
	 * on the view
	 */
	public void initFields(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(IS_EDITING_KEY)) {
				this.isEditing = savedInstanceState.getBoolean(IS_EDITING_KEY);
				this.curEditState = ZWEditState.loadFromBundle(savedInstanceState);

			}
		}
		
		this.coinLogo.setImageResource(this.getSelectedCoin().getLogoResId());

		ZWFiat fiat = ZWPreferences.getFiatCurrency();
		this.populateAmount();
		this.populateCurrency();
		
		String feeString = txItem.getCoin().getFormattedAmount(txItem.getFee());
		this.confirmationFee.setText(feeString);
		
		feeString = txItem.getCoin().getFormattedAmount(txItem.getFee());
		this.confirmationFee.setText(feeString);
		
		
		this.currencyType.setText(fiat.getName());

		Date date = new Date(this.txItem.getTxTime());
		this.time.setText(ZiftrUtils.formatterNoTimeZone.format(date));

		this.populateAddress();

		this.populatePendingInformation();
		this.reuseAddress.setOnClickListener(this);
		ZWEditableTextBoxController<ZWTransaction> controller = new ZWEditableTextBoxController<ZWTransaction>(
				this, labelEditText, editLabelButton, this.txItem.getNote(), txItem);
		editLabelButton.setOnClickListener(controller);
		this.labelEditText.addTextChangedListener(controller);
	}

	private void populateAmount() {
		BigInteger baseAmount = this.txItem.getAmount();
		BigDecimal amountValue = txItem.getCoin().getAmount(baseAmount); 
		amount.setText(txItem.getCoin().getFormattedAmount(amountValue));
		if (this.txItem.getAmount().compareTo(BigInteger.ZERO) < 0) {
			// This means the tx is sent (relative to user)
			this.amountLabel.setText("Amount Sent");
			this.timeLabel.setText("Sent");
			this.reuseAddress.setImageResource(R.drawable.send_yellow_clickable);
		} else {
			// This means the tx is received (relative to user)
			this.amountLabel.setText("Amount Received");
			this.timeLabel.setText("Received");
			this.reuseAddress.setImageResource(R.drawable.received_yellow_clickable);
		}

		if (txItem.isPending()) {
			this.amount.setTextColor(getResources().getColor(R.color.Crimson));
			this.amountLabel.append(" (pending)");
		}
	}

	private void populateCurrency() {
		ZWFiat fiat = ZWPreferences.getFiatCurrency();
		
		BigInteger fiatAmt = ZWConverter.convert(txItem.getAmount(), 
				txItem.getCoin(), fiat);
		String formattedfiatAmt = fiat.getFormattedAmount(fiatAmt);

		currency.setText(formattedfiatAmt);

		TextView currencyType = (TextView) rootView.findViewById(R.id.currencyType);
		currencyType.setText(fiat.getName());

		TextView time = (TextView) rootView.findViewById(R.id.date);
		Date date = new Date(this.txItem.getTxTime());
		time.setText(ZiftrUtils.formatterNoTimeZone.format(date));
	}

	private void populateAddress() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < txItem.getDisplayAddresses().size(); i++) {
			String a = txItem.getDisplayAddresses().get(i);
			sb.append(a);
			ZLog.log("displaying addresses in txndetails: " + a);
			if (i != txItem.getDisplayAddresses().size()-1) {
				sb.append("\n");
			}
		}
		addressTextView.setText(sb.toString());

	}

	@SuppressLint("NewApi")  //we call getWindowManager().getDefaultDisplay().getSize(size); but only after checking we are a high enough api level
	@SuppressWarnings("deprecation")
	private void populatePendingInformation() {
		int totalConfirmations = txItem.getCoin().getNumRecommendedConfirmations();
		long confirmed = txItem.getConfirmationCount();
		if (((int)confirmed) >= totalConfirmations){
			this.status.setText("Confirmed");
		} else {
			this.status.setText("Confirmed (" + confirmed + " of " + totalConfirmations + ")");
		}
		
		//TO prevent estimated time and Confirmed text views from overlapping,
		//we only show estimated time if the screen width > 3 * width of the confirmed textview
		int screenWidth;
		if (android.os.Build.VERSION.SDK_INT < 13){
			screenWidth = getZWMainActivity().getWindowManager().getDefaultDisplay().getWidth();
		} else {
			Point size = new Point();
			getZWMainActivity().getWindowManager().getDefaultDisplay().getSize(size);
			screenWidth = size.x; 
		}
		this.status.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		
		if (confirmed < totalConfirmations && screenWidth > this.status.getMeasuredWidth() * 3){
			long estimatedTime = txItem.getCoin().getSecondsPerAverageBlockSolve()*(totalConfirmations-confirmed);
			this.timeLeft.setText("ETC: " + formatEstimatedTime(estimatedTime));
		}
		//in theory a really old network, or a network with fast enough blocks, could have more blocks than an int can hold
		//however even in this case it's needed confirmations would still be a smaller number
		//this just checks it so that if we're for some reason checking the progress of a really old transactions 
		//it won't have usses due to converting a long to an int
		int progress;
		if(confirmed <= totalConfirmations) {
			progress = (int) confirmed;
		}
		else {
			progress = totalConfirmations;
		}
		this.progressBar.setMax(totalConfirmations);
		this.progressBar.setProgress(progress);
	}

	public void setTxItem(ZWTransaction txItem) {
		this.txItem = txItem;
	}
	

	public String formatEstimatedTime(long estimatedTime) {
		StringBuilder sb = new StringBuilder();
		if (estimatedTime > 3600) {
			long hours = estimatedTime / 3600;
			estimatedTime = estimatedTime % 3600;
			sb.append(hours + " hours, ");
		}
		long minutes = estimatedTime/60;
		estimatedTime = estimatedTime % 60;
		sb.append (minutes + " minutes");
		if (estimatedTime != 0) {
			sb.append(", " + estimatedTime + " seconds");
		}
		return sb.toString();
	}

	//////////////////////////////////////////////////////
	////////// Methods for being an EditHandler //////////
	//////////////////////////////////////////////////////

	@Override
	public void onEditStart(ZWEditState state, ZWTransaction t) {
		if (t != this.txItem) {
			ZLog.log("Should have gotten the same object, something went wrong. ");
			return;
		}
		//open keyboard when user toggles edit text
		InputMethodManager imm = (InputMethodManager) getZWMainActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(this.labelEditText, InputMethodManager.SHOW_IMPLICIT);
		this.curEditState = state;
		this.isEditing = true;
	}

	@Override
	public void onEdit(ZWEditState state, ZWTransaction t) {
		if (t != this.txItem) {
			ZLog.log("Should have gotten the same object, something went wrong. ");
			return;
		}
		this.curEditState = state;
		// TODO Auto-generated method stub
		txItem.setNote(state.text);
		getWalletManager().updateTransactionNote(txItem);
	}

	@Override
	public void onEditEnd(ZWTransaction t) {
		if (t != this.txItem) {
			ZLog.log("Should have gotten the same object, something went wrong. ");
			return;
		}

		// Now that we are done editing 
		this.isEditing = false;
		
		Activity mainActivity = this.getActivity();
		if(mainActivity != null) {
			InputMethodManager inputManager = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(mainActivity.getCurrentFocus().getWindowToken(), 0);
		}
	}

	@Override
	public boolean isInEditingMode() {
		return this.isEditing;
	}

	@Override
	public boolean isEditing(ZWTransaction t) {
		return this.isEditing && t == this.txItem; 
	}

	/**
	 * @return the curEditState
	 */
	@Override
	public ZWEditState getCurEditState() {
		return this.curEditState;
	}

	@Override
	public void onClick(View v) {
		if (v==this.reuseAddress){
			if (this.txItem.getAmount().compareTo(BigInteger.ZERO) < 0) {
				//sent to this address
				getZWMainActivity().openSendCoinsView(txItem.getDisplayAddresses().get(0));
			} else {
				//received on this address
				try {
					ZWAddress address = getWalletManager().getAddress(txItem.getCoin(), txItem.getDisplayAddresses().get(0), true);
					getZWMainActivity().openReceiveCoinsView(address);
				} catch (Exception e) {
					ZLog.log("Error trying to get ZWAddress from display address  " + e);
				}
			}
		}
	}

}
