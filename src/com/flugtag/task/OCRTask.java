package com.flugtag.task;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.flugtag.util.Paths;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
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
public class OCRTask extends AsyncTask<Uri, Void, String> {
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
	protected String doInBackground(Uri... params) {
		Log.i(TAG, "OCR: starting");
		
		Uri data = params[0];
		
		long startMillis = System.currentTimeMillis();

		// Use our camera provided data		
		Pix pix = getPixFromUri(data);
		
		TessBaseAPI tessApi = new TessBaseAPI();
		tessApi.init(Paths.OCR_DATA, "eng");
		tessApi.setPageSegMode(TessBaseAPI.PSM_SINGLE_BLOCK);
		tessApi.setImage(pix);
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
	
	/**
	 * Get a Pix instance from our source data Uri
	 * 
	 * @param bmpUri The Uri of our BMP file
	 * @return The resulting Pix or null
	 */
	private Pix getPixFromUri(Uri bmpUri){
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
		
		InputStream is;
		try {
			is = context.getContentResolver().openInputStream(bmpUri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
		Pix pix = ReadFile.readBitmap(bmp);
		bmp.recycle();
		
		return pix; 
	}
}
