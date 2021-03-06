/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.dialog;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;

public abstract class ZiftrDialogManager {
	
	private static WeakReference<ZiftrDialogHandler> handlerHolder = new WeakReference<ZiftrDialogHandler>(null);
	
	private static String waitingDialogMessage;
	private static ZiftrDialogFragment waitingDialog;
	private static String waitingDialogTag;
	
	public static synchronized void registerHandler(ZiftrDialogHandler handler) {
		handlerHolder = new WeakReference<ZiftrDialogHandler>(handler);
		
		if(waitingDialogMessage != null && handler != null) {
			showSimpleAlert(handler.getSupportFragmentManager(), waitingDialogMessage);
			waitingDialogMessage = null;
		}
		
		if(waitingDialog != null && handler != null) {
			waitingDialog.show(handler.getSupportFragmentManager(), waitingDialogTag);
			waitingDialog = null;
			waitingDialogTag = null;
		}
	}
	
	
	public static synchronized void dialogClickedYes(DialogFragment fragment) {
		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			handler.handleDialogYes(fragment);
		}
	}

	
	public static synchronized void dialogClickedNo(DialogFragment fragment) {
		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			handler.handleDialogNo(fragment);
		}
	}
	
	public static synchronized void dialogCancelled(DialogFragment fragment) {
		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			handler.handleDialogCancel(fragment);
		}
	}
	
	
	public static synchronized void showWaitingDialogs() {
		ZiftrDialogHandler handler = handlerHolder.get();
		
		if(waitingDialogMessage != null && handler != null) {
			showSimpleAlert(handler.getSupportFragmentManager(), waitingDialogMessage);
			waitingDialogMessage = null;
		}
	}
	

	/**
	 * Construct and immediately display a dialog with a simple message and an OK button.
	 * No handling of button clicks is done, this is simply for displaying quick info to the user
	 * @param fragmentManager the fragment manager used to display this dialog fragment
	 * @param messageResId the resource id of the string to be displayed
	 */
	public static void showSimpleAlert(FragmentManager fragmentManager, int messageResId) {
		ZiftrSimpleDialogFragment dialog = new ZiftrSimpleDialogFragment();
		dialog.setupDialog(0, messageResId, R.string.zw_dialog_ok, 0);
		
		dialog.show(fragmentManager, "simple_alert_dialog");
	}
	
	
	public static void showSimpleAlert(FragmentManager fragmentManager, String message) {
		Context appContext = ZWApplication.getApplication();
		
		ZiftrSimpleDialogFragment dialog = new ZiftrSimpleDialogFragment();
		String title = appContext.getString(R.string.zw_app_name);
		String ok = appContext.getString(R.string.zw_dialog_ok);
		
		dialog.setupDialog(title, message, ok, null);
		
		dialog.show(fragmentManager, "simple_alert_dialog");
	}

	
	public static synchronized void showSimpleAlert(String message) {

		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			try {
				showSimpleAlert(handler.getSupportFragmentManager(), message);
			}
			catch(Exception e) {
				waitingDialogMessage = message;
			}
		}
		else {
			waitingDialogMessage = message;
		}
		
	}
	
	
	/**
	 * This can be used to allow background threads/tasks to show a dialog fragment as soon as the UI is available
	 * @param dialog the dialog to show
	 */
	public static synchronized void showDialog(ZiftrDialogFragment dialog, String tag) {
		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			try {
				dialog.show(handler.getSupportFragmentManager(), tag);
			}
			catch(Exception e) {
				waitingDialog = dialog;
				waitingDialogTag = tag;
			}
		}
		else {
			waitingDialog = dialog;
			waitingDialogTag = tag;
		}
	}
	
	
}
