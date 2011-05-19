package com.flugtag;

import java.util.ArrayList;

import com.flugtag.model.CheckItem;
import com.flugtag.task.LanguageInstallTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
	
    /**
     * Handle the Activity create. 
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     * @category Activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(LanguageInstallTask.installRequired()){
        	new LanguageInstallTask(this).execute();
        }
        
        setContentView(R.layout.main);
    }
    
	/**
	 * Handle button click event
	 * 
	 * @param btn The click target button
	 * @category Button Event
	 */
    public void onScanBtnClick(View btn){
    	Intent intent = new Intent(btn.getContext(), CaptureActivity.class);
		startActivity(intent);
    }
    
	/**
	 * Handle button click event
	 * 
	 * @param btn The click target button
	 * @category Button Event
	 */
    public void onListBtnClick(View btn){
    	Intent intent = new Intent(btn.getContext(), ListActivity.class);
    	
    	//Send in some test/sample data 
		ArrayList<CheckItem> items = new ArrayList<CheckItem>();
		items.add(new CheckItem("PGM Wrap", 7.90));
		items.add(new CheckItem("Asian Chicken Wrap", 8.10));
		items.add(new CheckItem("Chicken Ceasar Grinder", 10.50));
		items.add(new CheckItem("Buffalo Chicken Salad", 7.50));
		items.add(new CheckItem("Buffalo Wing", 0.10));
		items.add(new CheckItem("French Fries", 0.99));
		
    	intent.putExtra(CheckItem.class.toString(), items);
    	
		startActivity(intent);
    }
}