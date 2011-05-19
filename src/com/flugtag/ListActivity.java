package com.flugtag;

import java.util.List;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.flugtag.model.CheckItem;
import com.flugtag.ui.CheckItemArrayApapter;

public class ListActivity extends android.app.ListActivity {

    /**
     * Handle the Activity create. 
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     * @category Activity
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		CheckItemArrayApapter aa = new CheckItemArrayApapter(this);
		populate(aa);
		setListAdapter(aa);
		
		ListView list = getListView();
		list.setItemsCanFocus(false);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}

	/**
	 * Populate an ArrayAdapter with the contents of
	 * the provided Intent.
	 * 
	 * @param arrayAdptr The ArrayAdpater to populate
	 */
	@SuppressWarnings("unchecked")
	protected void populate(ArrayAdapter<CheckItem> arrayAdptr) {
		List<CheckItem> items = (List<CheckItem>) getIntent()
				.getSerializableExtra(CheckItem.class.toString());
		for(CheckItem item : items){
			arrayAdptr.add(item);
		}
	}
	
}
