/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import android.content.res.Resources;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.util.ZiftrTextWatcher;

// TODO documentation
// TODO make it so that the user can start editing another entry in the middle of 
// an edit.
public class ZWEditableTextBoxController<T> extends ZiftrTextWatcher implements OnClickListener {

	public interface EditHandler<T> {
		public void onEditStart(ZWEditState state, T t);
		public void onEdit(ZWEditState state, T t);
		public void onEditEnd(T t);
		public boolean isInEditingMode();
		public boolean isEditing(T t);
		public ZWEditState getCurEditState();
		public Resources getResources();
	}

	private EditHandler<T> handler;

	private EditText textView;
	private ImageView imgView;

	private T returnData;

	public ZWEditableTextBoxController(EditHandler<T> handler, EditText textView, 
			ImageView imgView, String initialText, T returnData) {
		this.handler = handler;
		this.textView = textView;
		this.imgView = imgView;
		this.textView.setText(initialText);

		this.returnData = returnData;

		// This is necessary because on screen rotates we need
		// to initialze correctly
		this.toggleEditNote(!handler.isEditing(this.returnData));
	}

	/**
	 * 
	 * @param txNote - EditText for note view
	 * @param imgView - ImageView for edit button
	 * @param toggleOff - boolean to determine how to toggle
	 */
	public void toggleEditNote(boolean toggleOff) {

		if (toggleOff) {
			this.textView.setFocusable(false);
			this.textView.setFocusableInTouchMode(false);
			this.textView.setBackgroundColor(handler.getResources().getColor(R.color.transparent));
			//this is the same padding as the editbox_background_normal
			this.textView.setPadding(5, 8, 13, 8);
			this.textView.setClickable(false);
			imgView.setImageResource(R.drawable.button_edit_pencil_clickable);
			handler.onEditEnd(this.returnData);
		} else {
			this.textView.setBackgroundResource(android.R.drawable.editbox_background_normal);
			this.textView.setPadding(5, 8, 13, 8);
			this.textView.setFocusable(true);
			this.textView.setFocusableInTouchMode(true);
			this.textView.setClickable(true);
			this.textView.setSelection(this.textView.getText().length());
			imgView.setImageResource(R.drawable.button_cancel);
			this.textView.requestFocus();
			handler.onEditStart(getNewEditState(), this.returnData);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == this.imgView) {
			// If the handler is already editing then we 
			if (!handler.isInEditingMode() || handler.isEditing(this.returnData)) {
				toggleEditNote(handler.isEditing(this.returnData));
			}
		}
	}

	@Override
	public void afterTextChanged(Editable text) {
		this.handler.onEdit(getNewEditState(), returnData);
	}

	/**
	 * @return
	 */
	private ZWEditState getNewEditState() {
		ZWEditState state = new ZWEditState();
		state.text = this.textView.getText().toString();
		return state;
	}

	///////////////////////////////////////////
	//////////  Getters and Setters  //////////
	///////////////////////////////////////////

	/**
	 * @return the handler
	 */
	public EditHandler<T> getHandler() {
		return handler;
	}

	/**
	 * @param handler the handler to set
	 */
	public void setHandler(EditHandler<T> handler) {
		this.handler = handler;
	}

	/**
	 * @return the textView
	 */
	public EditText getTextView() {
		return textView;
	}

	/**
	 * @param textView the textView to set
	 */
	public void setTextView(EditText textView) {
		this.textView = textView;
	}

	/**
	 * @return the imgView
	 */
	public ImageView getImgView() {
		return imgView;
	}

	/**
	 * @param imgView the imgView to set
	 */
	public void setImgView(ImageView imgView) {
		this.imgView = imgView;
	}

	/**
	 * @return the returnData
	 */
	public T getReturnData() {
		return returnData;
	}

	/**
	 * @param returnData the returnData to set
	 */
	public void setReturnData(T returnData) {
		this.returnData = returnData;
	}

}

