package com.flugtag.ui;

import com.flugtag.R;
import com.flugtag.model.CheckItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
/**
 * We extend ArrayAdapter only to provide a properly
 * populated View from getView.
 *
 */
public class CheckItemArrayApapter extends ArrayAdapter<CheckItem> {

	public CheckItemArrayApapter(Context context) {
		/* we don't ever use the textViewResourceId since we override getView */
		super(context, -1);
	}

	
	/**
	 * We override getView so that we can properly populate the multiple textViews.
	 * 
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
	 * @category ArrayAdapter
	*/ 
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout view = (LinearLayout)convertView;
		if(view == null) {
			Context ctx = this.getContext();
			LayoutInflater li = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (LinearLayout)li.inflate(R.layout.list_item, null);
		}
		
		CheckItem item = getItem(position);
		((TextView)view.findViewById(R.id.checkBox1)).setText(item.getLabel());
		((TextView)view.findViewById(R.id.textView1)).setText(item.getPriceLabel());
		return view;
	}
	
}
