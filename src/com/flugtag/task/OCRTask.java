package com.flugtag.task;

import java.text.MessageFormat;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.flugtag.util.Paths;
import com.googlecode.tesseract.android.TessBaseAPI;

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
public class OCRTask extends AsyncTask<byte[], Void, String> {
	private final String TAG = "OCRTask";
	
	private Context context;
	private ProgressDialog dialog;
	private AsyncTaskCompleteListener<String> listener;
	
	/**
	 * Construct a LanguageInstallTask with the provided context.
	 * 
	 * @param ctx The context to use when launching the progress dialog. 
	 */
	public OCRTask(Context ctx, AsyncTaskCompleteListener<String> listener){
		this.context = ctx;
		this.listener = listener;
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
		dialog.setMessage("Reading Receipt...");		
		dialog.show();
	}

	
	/**
	 * Dismisses the progress dialog.
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		
		dialog.dismiss();
		
		listener.onTaskComplete(result);
	}
	
	/**
	 * Performs the install task
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected String doInBackground(byte[]... params) {
		Log.i(TAG, "OCR; starting");
		
		byte[] data = params[0];
		
		long startMillis = System.currentTimeMillis();
		
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
		
		// Use our camera provided data
		Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		
		// Use our sample receipt
		//InputStream inStream = context.getResources().openRawResource(R.raw.tjreceipt);
		//Bitmap bmp = BitmapFactory.decodeStream(inStream, null, opts);
		
		TessBaseAPI tessApi = new TessBaseAPI();
		tessApi.init(Paths.OCR_DATA, "eng");
		tessApi.setPageSegMode(TessBaseAPI.PSM_SINGLE_BLOCK);
		tessApi.setImage(bmp);
		String result = tessApi.getUTF8Text();
		int confidence = tessApi.meanConfidence();
		tessApi.end();

		long endMillis = System.currentTimeMillis();
		long deltaMillis = endMillis - startMillis;
		
		Log.i(TAG,
				MessageFormat.format(
						"OCR: {0} milliseconds with {1} confidence",
						confidence,
						deltaMillis));
		
		return result;
	}
}
