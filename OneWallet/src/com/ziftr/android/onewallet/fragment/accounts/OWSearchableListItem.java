package com.ziftr.android.onewallet.fragment.accounts;

public interface OWSearchableListItem {

	/**
	 * This method is used to determine if an item should be added to a list view
	 * while being searched. The nextItem is added because sometimes whether or 
	 * not an item should be added depends on what items surround it.
	 * 
	 * @param constraint - The constraint used for searching.
	 * @param nextItem - The next item which passes the search.
	 * @return
	 */
	public boolean matches(CharSequence constraint, OWSearchableListItem nextItem);

}