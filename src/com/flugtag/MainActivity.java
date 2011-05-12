package com.flugtag;

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
}