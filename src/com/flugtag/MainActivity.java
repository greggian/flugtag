package com.flugtag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.flugtag.model.CheckItem;
import com.flugtag.task.AsyncTaskCompleteListener;
import com.flugtag.task.LanguageInstallTask;
import com.flugtag.task.OCRTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

public class MainActivity extends Activity {
	
	private static final int IMAGE_CAPTURE = 0;
	private static final int IMAGE_PICK = 1;
	
	private Uri tmpFileUri;

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
        //check that there is an activity for the image capture intent
        if(!isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
				.setTitle("No image capture activity!")
				.setMessage("There is no application available to take a picture.")
				.setPositiveButton("Ok", null)
				.show();
			
			finish();
			return;
        }
        
		try {
	        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File tmpFile = File.createTempFile("flugtag", "", Environment.getExternalStorageDirectory());
			tmpFileUri = Uri.fromFile(tmpFile);
	        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tmpFileUri);
	        
	        startActivityForResult(captureIntent, IMAGE_CAPTURE);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	/**
	 * Handle button click event
	 * 
	 * @param btn The click target button
	 * @category Button Event
	 */
    public void onPickBtnClick(View btn){
        //check that there is an activity for the image capture intent
        if(!isIntentAvailable(this, Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
				.setTitle("No image media activity!")
				.setMessage("There is no application available to pick a picture.")
				.setPositiveButton("Ok", null)
				.show();
			
			finish();
			return;
        }
        
		Intent captureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(captureIntent, IMAGE_PICK);
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
    
	/**
	 * Handle result from startActivityForResult.
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 * @see android.app.Activity#startActivityForResult(Intent, int)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK && requestCode == IMAGE_CAPTURE){
			
			//perform the OCR and call back on complete 
			new OCRTask(this, new OCRTaskCompleteListener()).execute(tmpFileUri);
			
		}else if(resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK){
			Uri selectedImage = data.getData();
			
			//perform the OCR and call back on complete 
			new OCRTask(this, new OCRTaskCompleteListener()).execute(selectedImage);
		}
	}
    
    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action, Uri uri) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action, uri);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    
    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    
    
    /**
     * An AsyncTaskCompleteListener to handle the completion of an OCRTask.
     */
	public class OCRTaskCompleteListener implements AsyncTaskCompleteListener<String> {

		/**
		 * Handle completion of OCRTask. 
		 * 
		 * @see com.flugtag.task.AsyncTaskCompleteListener#onTaskComplete(java.lang.Object)
		 */
		@Override
		public void onTaskComplete(String result) {
			//TODO: launch an actual activity
			Context ctx = MainActivity.this;
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder
				.setTitle("OCR Text")
				.setMessage(result)
				.setPositiveButton("Ok", null)
				.show();
		}

	}
}