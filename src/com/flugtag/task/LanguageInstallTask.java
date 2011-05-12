package com.flugtag.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.flugtag.util.FileUtils;
import com.flugtag.util.Paths;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;

/**
 * An AsyncTask that installs the languages in the correct
 * location for OCR library use. A progress dialog is shown
 * while processing.
 * 
 * Note:
 * Currently, the progress dialog is an spinner without
 * real progress information. This is due to the difficulties
 * in getting the size of compressed assets. This can be worked
 * around but will take some time.
 * 
 */
public class LanguageInstallTask extends AsyncTask<Void, Void, Boolean> {

	private Context context;
	private AssetManager assetManager;
	private ProgressDialog dialog;
	
	/**
	 * Construct a LanguageInstallTask with the provided context.
	 * 
	 * @param ctx The context to use when launching the progress dialog. 
	 */
	public LanguageInstallTask(Context ctx){
		this.context = ctx;
		this.assetManager = ctx.getAssets();
	}
	
	/**
	 * Checks if the environment required the
	 * install task to be run.
	 * 
	 * @return False if the language files are in place
	 */
	public static boolean installRequired(){
		File outFile = new File(Paths.EXT_TESSDATA);
        return !outFile.exists();
	}


	/**
	 * Opens the progress dialog.
	 * 
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		dialog = new ProgressDialog(context);
		dialog.setMessage("Installing Language Files...");		
		dialog.show();
	}

	
	/**
	 * Dismisses the progress dialog.
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		
		dialog.dismiss();
	}
	
	/**
	 * Performs the install task
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Boolean doInBackground(Void... params) {
        
        try {
			installLanguages();
		} catch (IOException e) {
			return false;
		}
		return true;
        
	}
	
	/**
	 * Copies the language files into place.
	 * 
	 * All files under assets/tessdata are copied into place.
	 * 
	 * @throws IOException
	 */
	private void installLanguages() throws IOException {
        File outFldr = new File(Paths.EXT_TESSDATA);
        if(!outFldr.exists()){
        	outFldr.mkdirs();
        	
        	String[] fileNames = assetManager.list(Paths.INT_TESSDATA);
        	for(String fileName : fileNames){
        		InputStream inStream = assetManager.open(Paths.INT_TESSDATA+File.separator+fileName);
        		
        		File outFile = new File(Paths.EXT_TESSDATA+File.separator+fileName);
        		FileUtils.copyFile(inStream, new FileOutputStream(outFile));
        	}
        }
	}
}
