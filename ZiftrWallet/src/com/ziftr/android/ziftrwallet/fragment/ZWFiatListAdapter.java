/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;

public class ZWFiatListAdapter extends ArrayAdapter<ZWFiat>{

	private LayoutInflater inflater;

	public ZWFiatListAdapter(Context ctx, List<ZWFiat> objects) {
		super(ctx, 0, objects);
		this.inflater = LayoutInflater.from(ctx);
	}
	/**
	 * Given the position of the item in the list, we get the list item
	 * and recreate the view from it. Note that this method recycles 
	 * the convertView when we have enough list elements. 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		ZWFiat fiat = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.inflater.inflate(R.layout.settings_fiat_single_item, null);
		}

		TextView fiatName = (TextView) 
				convertView.findViewById(R.id.fiatName);
		fiatName.setText(fiat.getName());
		return convertView;

	}
}
