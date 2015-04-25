package com.ziftr.android.ziftrwallet.dialog;

import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;


public abstract class ZiftrTaskDialogFragment extends ZiftrDialogFragment {
	
	
	protected abstract boolean doTask();

	
	private boolean paused = false;
	private boolean finished = false;
	private boolean successful = false;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		this.setRetainInstance(true);
		
		if(savedInstanceState != null) {
			if(title == null) title = savedInstanceState.getString("title");
			if(message == null) message = savedInstanceState.getString("message");
			if(yes == null) yes = savedInstanceState.getString("yes");
			if(no == null) no = savedInstanceState.getString("no");
		}
	
		ProgressDialog dialog = new ProgressDialog(this.getActivity());  //ProgressDialog. //.show(this, "Searching...", "Finding the best prices for " +upc +".", true);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
 
		//create a simple key listener to disable(consume) the back button press
		OnKeyListener keyListener = new OnKeyListener() {
 
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				
				if( keyCode == KeyEvent.KEYCODE_BACK){					
					return true;
				}
				return false;
			}
		};
		
		dialog.setOnKeyListener(keyListener);
		
		return dialog;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		if(finished) {
			finishTask();
		}
	}

	
	@Override
	public void onDestroyView() {
		//removes the dismiss message to avoid this retained and displayed dialog being dismissed
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		   
		super.onDestroyView();
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		this.startTask();
		super.show(manager, tag);
	}

	@Override
	public int show(FragmentTransaction transaction, String tag) {
		this.doTask();
		return super.show(transaction, tag);
	}
	
	
	@Override
	public void onPause() {
		paused = true;
		super.onPause();
	}


	@Override
	public void onResume() {
		super.onResume();
		paused = false;
		if(finished) {
			finishTask();
		}
	}
	
	
	protected void startTask() {
		
		ZiftrUtils.runOnNewThread( new Runnable() {
			
			@Override
			public void run() {
				successful = false;
				successful = doTask();
				finishTask();
			}
		});
	}
	
	

	protected void finishTask() {
		finished = true;
		
		boolean isVisible = (this.getDialog() != null && this.getDialog().isShowing());
		boolean isDetached = this.isDetached();
		
		if(!isDetached && isVisible) {
			final Activity act = this.getActivity();
			if(act != null) {
				act.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						
						if(!paused) {
							
							dismissAllowingStateLoss();
							
							if(act instanceof ZiftrDialogHandler) {
								//task dialogs can only be attached to activities, since they are retained
								//so no need to look for a parent fragment
								if(successful) {
									((ZiftrDialogHandler)act).handleDialogYes(ZiftrTaskDialogFragment.this);
								}
								else {
									((ZiftrDialogHandler)act).handleDialogNo(ZiftrTaskDialogFragment.this);
								}
							}
					
						}
						
					}
				});
			}
		}
	}

}