package com.flugtag.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * This class is useful for using inside of ListView that needs
 * to have checkable items.
 * 
 * Implements Checkable and delegates all check actions to the
 * first Checkable child.
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
	private Checkable _checkbox;

	public CheckableLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * On inflate, find our Checkable to delegate to.
	 * 
	 * @see android.view.View#onFinishInflate()
	 * @category View
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// find checked text view
		int childCount = getChildCount();
		for (int i = 0; i < childCount; ++i) {
			View v = getChildAt(i);
			if (v instanceof Checkable) {
				_checkbox = (Checkable) v;
			}
		}
	}

	/**
	 * @see android.widget.Checkable#isChecked()
	 * @category Checkable
	 */
	@Override
	public boolean isChecked() {
		return _checkbox != null ? _checkbox.isChecked() : false;
	}

	/**
	 * @see android.widget.Checkable#setChecked(boolean)
	 * @category Checkable
	 */
	@Override
	public void setChecked(boolean checked) {
		if (_checkbox != null) {
			_checkbox.setChecked(checked);
		}
	}

	/**
	 * @see android.widget.Checkable#toggle()
	 * @category Checkable
	 */
	@Override
	public void toggle() {
		if (_checkbox != null) {
			_checkbox.toggle();
		}
	}
}