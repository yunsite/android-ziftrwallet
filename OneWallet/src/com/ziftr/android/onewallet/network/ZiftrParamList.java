package com.ziftr.android.onewallet.network;

import java.util.ArrayList;

public class ZiftrParamList {

	private ArrayList<String[]> params;
	
	public ZiftrParamList () {
		params = new ArrayList<String[]>();
	}
	
	public ZiftrParamList(int capacity) {
		params = new ArrayList<String[]>(capacity);
	}

	public void add(String name, String value) {
		String[] paramPair = new String[]{name, value};
		params.add(paramPair);
	}
	
	public String getName(int index) {
		return params.get(index)[0];
	}
	
	public String getValue(int index) {
		return params.get(index)[1];
	}
	
	public int size() {
		return params.size();
	}
}
