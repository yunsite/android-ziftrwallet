package com.ziftr.android.ziftrwallet;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity.FragmentType;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWReceiveCoinsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSettingsFragment;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable.reencryptionStatus;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWActivityDialogHandler implements ZiftrDialogHandler {

	public static final String DIALOG_DATABASE_ERROR_TAG = "activity_database_error";
	public static final String DIALOG_OLD_PASSWORD_TAG = "activity_old_password";
	
	FragmentActivity activity;
	
	public ZWActivityDialogHandler(FragmentActivity activity) {
		this.activity = activity;
	}
	
	
	private FragmentManager getSupportFragmentManager() {
		return this.activity.getSupportFragmentManager();
	}
	
	
	public Fragment getFragment(FragmentType fragmentType) {
		return this.getSupportFragmentManager().findFragmentByTag(fragmentType.toString());
	}
	
	
	private boolean changePassphrase(final String oldPassphrase, final String newPassphrase) {
		// TODO put up a blocking dialog while this is happening
	
		//note: null/blank newPassphrase is ok now, it's how password is cleared
		final reencryptionStatus status = ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(oldPassphrase, newPassphrase);
		
		if (status == reencryptionStatus.encrypted || status == reencryptionStatus.error){
			//tried to set passphrase on encrypted database private keys
			this.activity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					if (status == reencryptionStatus.encrypted){
						ZiftrTextDialogFragment decryptOldWalletDialog = new ZiftrTextDialogFragment();
						decryptOldWalletDialog.setupDialog(R.string.zw_dialog_old_encryption);
						decryptOldWalletDialog.setupTextboxes();
						
						decryptOldWalletDialog.show(getSupportFragmentManager(), DIALOG_OLD_PASSWORD_TAG);
					} 
					else {
						ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_old_encryption_fatal);
					}
				}
				
			});
			return false;
		} else {
			//reencryption was successful
			String saltedHash = ZiftrUtils.saltedHashString(newPassphrase);
			ZWPreferences.setStoredPasswordHash(saltedHash);
		}
		return true;
	}
	
	
	
	@Override
	public void handleDialogYes(DialogFragment fragment) {
		
		if(ZWReceiveCoinsFragment.DIALOG_NEW_ADDRESS_TAG.equals(fragment.getTag())) {
			//user is creating a new address
			ZWReceiveCoinsFragment receiveFragment = 
					(ZWReceiveCoinsFragment) getSupportFragmentManager().findFragmentByTag(ZWReceiveCoinsFragment.FRAGMENT_TAG);
			receiveFragment.loadNewAddressFromDatabase();
		}
		else if(ZWReceiveCoinsFragment.DIALOG_ENTER_PASSWORD_TAG.equals(fragment.getTag())) {
			String enteredPassword = ((ZiftrTextDialogFragment)fragment).getEnteredTextTop();
			byte[] inputHash = ZiftrUtils.saltedHash(enteredPassword);
			
			if (ZWPreferences.inputHashMatchesStoredHash(inputHash)) {
				ZWPreferences.setCachedPassword(enteredPassword);
				
				ZWReceiveCoinsFragment receiveFragment = 
						(ZWReceiveCoinsFragment) getSupportFragmentManager().findFragmentByTag(ZWReceiveCoinsFragment.FRAGMENT_TAG);
				if(receiveFragment != null) {
					
					receiveFragment.loadNewAddressFromDatabase();
				}
			}
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_password);
			}
		}
		else if(ZWSettingsFragment.DIALOG_ENABLE_DEBUG_TAG.equals(fragment.getTag())) {
			ZWPreferences.setDebugMode(true);
			ZWDataSyncHelper.updateCoinData();
			ZWSettingsFragment settingsFragment = (ZWSettingsFragment)this.getFragment(FragmentType.SETTINGS_FRAGMENT_TYPE);
			if(settingsFragment != null) {
				settingsFragment.updateSettingsVisibility();
			}
		}
		else if(ZWSettingsFragment.DIALOG_CREATE_PASSWORD_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment passwordFragment = (ZiftrTextDialogFragment) fragment;
			String password = passwordFragment.getEnteredTextTop();
			String confirmedPassword = passwordFragment.getEnteredTextMiddle();
			
			if(password.equals(confirmedPassword)) {
				if (this.changePassphrase(null, password)) {
					ZWPreferences.setPasswordWarningDisabled(true);
					ZWSettingsFragment settingsFragment = (ZWSettingsFragment)this.getFragment(FragmentType.SETTINGS_FRAGMENT_TYPE);
					if(settingsFragment != null) {
						settingsFragment.updateSettingsVisibility();	
					}
					ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_set_password_complete);
				} 
				else {
					//this is an epic failure, it basically measn the database is corrupted, or somehow double encrypted
					//TODO -alert the user?
					return;
				}
			}
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_new_password_match);
			}
		}
		else if(ZWSettingsFragment.DIALOG_CHANGE_PASSWORD_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment passwordFragment = (ZiftrTextDialogFragment) fragment;
			String oldPassword = passwordFragment.getEnteredTextTop();
			String newPassword = passwordFragment.getEnteredTextMiddle();
			String confirmPassword = passwordFragment.getEnteredTextBottom();
		
			if(newPassword == confirmPassword || newPassword.equals(confirmPassword)) {
				byte[] oldPasswordHash = ZiftrUtils.saltedHash(oldPassword);
				
				if (ZWPreferences.inputHashMatchesStoredHash(oldPasswordHash)) {
					//everything looks ok, try and change passwords
					if (this.changePassphrase(oldPassword, newPassword)) {
						ZWPreferences.setPasswordWarningDisabled(true);
						ZWSettingsFragment settingsFragment = (ZWSettingsFragment)this.getFragment(FragmentType.SETTINGS_FRAGMENT_TYPE);
						if(settingsFragment != null) {
							settingsFragment.updateSettingsVisibility();	
						}
						
						//if newpassword is null, it means the password is being removed
						if(newPassword == null) {
							ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_disabled_password);
						}
						else {
							ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_set_password_complete);
						}
						
					} 
					else {
						//this is an epic failure, it basically measn the database is corrupted, or somehow double encrypted
						//TODO -alert the user?
						return;
					}
				}
				else {
					//entered password wrong
					ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_reset_password);
				}
			}
			else {
				//new passwords don't match
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_reset_password_match);
			}
			
		}
		else if(ZWActivityDialogHandler.DIALOG_OLD_PASSWORD_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment passwordFragment = (ZiftrTextDialogFragment) fragment;
			String password = passwordFragment.getEnteredTextTop();

			//TODO -fix this, why are we "attempting" then doing it for real?
			if (ZWWalletManager.getInstance().attemptDecrypt(password)){
				ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(password, null);
				
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_old_encryption_changed);
			} 
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_old_encryption_failed);
			}
 
		}
		else if(ZWActivityDialogHandler.DIALOG_DATABASE_ERROR_TAG.equals(fragment.getTag())) {
			//user pressed the restart button
			System.exit(0);
		}
		
	}

	@Override
	public void handleDialogNo(DialogFragment fragment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleDialogCancel(DialogFragment fragment) {
		// TODO Auto-generated method stub
		
	}
	
}