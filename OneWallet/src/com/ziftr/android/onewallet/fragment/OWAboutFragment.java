package com.ziftr.android.onewallet.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.ZLog;


public class OWAboutFragment extends OWFragment {
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.section_about_layout, container, false);
	}
	
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().editTitle("ABOUT");
	}
	
}