package com.flugtag.task;

import java.io.File;
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
import com.googlecode.eyesfree.textdetect.HydrogenTextDetector;
import com.googlecode.eyesfree.textdetect.Thresholder;
import com.googlecode.leptonica.android.AdaptiveMap;
import com.googlecode.leptonica.android.Binarize;
import com.googlecode.leptonica.android.Constants;
import com.googlecode.leptonica.android.Convert;
import com.googlecode.leptonica.android.Enhance;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Scale;
import com.googlecode.leptonica.android.WriteFile;
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
	
	private static final boolean DEBUG = true;
	
    /** We'd rather not process anything larger than 720p. */
    private static final int MAX_IMAGE_AREA = 1280 * 720;
	
	private Context context;
	private ProgressDialog dialog;
	private AsyncTaskCompleteListener<String> listener;

	private Uri dbgUri;
	
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
	 * 
	 * Performs the install task
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected String doInBackground(Uri... params) {
		Log.i(TAG, "OCR: starting");
		long startMillis = System.currentTimeMillis();

        // Use our camera provided data         
		Uri data = params[0];
		dbgUri = new Uri.Builder().path("/sdcard/").appendPath("flug"+data.getLastPathSegment()).build();
        Pix pix = getPixFromUri(data);
        pix = preprocess(pix);
        
		Pixa pixa = slice(pix);
		
		TessBaseAPI tessApi = new TessBaseAPI();
		tessApi.init(Paths.OCR_DATA, "eng");
		tessApi.setPageSegMode(TessBaseAPI.PSM_SINGLE_LINE);
		StringBuilder sb = new StringBuilder();
		int confidence = 0;
		int num = pixa.size();
		for(int i=0;i<num;i++){
			Pix pixi = pixa.getPix(i);
			tessApi.setImage(pixi);

			sb.append(tessApi.getUTF8Text()).append("\n");
			confidence += tessApi.meanConfidence() / num;
			tessApi.clear();
			
			dumpDebugImage("part"+i, pixi);
			pixi.recycle();
		}
		pixa.recycle();
		tessApi.end();

		long endMillis = System.currentTimeMillis();
		long deltaMillis = endMillis - startMillis;
		
		Log.i(TAG,
				MessageFormat.format(
						"OCR: {0} milliseconds with {1} confidence",
						deltaMillis,
						confidence));
		
		return sb.toString();
	}
	
	private Pixa slice(Pix pix){
		HydrogenTextDetector htd = new HydrogenTextDetector();
        HydrogenTextDetector.Parameters hydrogenParams = htd.getParameters();
        hydrogenParams.debug = true;
        hydrogenParams.skew_enabled = true;
        htd.setParameters(hydrogenParams);

		htd.setSourceImage(pix);
		pix.recycle();
		htd.detectText();
        Pixa unsorted = htd.getTextAreas();
        Pixa pixa = unsorted.sort(Constants.L_SORT_BY_Y, Constants.L_SORT_INCREASING);
        unsorted.recycle();
        htd.clear();
        return pixa;
	}
	
	private Pix preprocess(Pix pix){
		// Shrink if necessary
		pix = resizePix(pix);
		pix = convertTo8(pix);
		pix = adaptiveMap(pix);
		//pix = binarize(pix);
		//pix = enhance(pix);
		//pix = threshold(pix);
		return pix;
	}
	
	private Pix threshold(Pix pix){
		Pix temp = Thresholder.edgeAdaptiveThreshold(pix);
		pix.recycle();
		dumpDebugImage("EdgeAdaptiveThreshold", temp);
		return temp;
	}
	
	private Pix enhance(Pix pix){
		Pix temp = Enhance.unsharpMasking(pix, 2, (float) 0.5);
		pix.recycle();
		dumpDebugImage("Enhance", temp);
		return temp;
	}
	
	private Pix binarize(Pix pix){
		Pix temp = Binarize.otsuAdaptiveThreshold(pix);
		pix.recycle();
		dumpDebugImage("Binarize", temp);
		return temp;
	}
	
	private Pix adaptiveMap(Pix pix){
		Pix temp = AdaptiveMap.backgroundNormMorph(pix);
		pix.recycle();
		dumpDebugImage("AdaptiveMap", temp);
		return temp;
	}
	
	private void dumpDebugImage(String name, Pix pix){
		if(DEBUG && dbgUri != null && pix != null){
			File file = new File(dbgUri.getPath() + name + ".bmp");
			Log.i(TAG, "Writing debug image to: "+file.getPath());
			WriteFile.writeImpliedFormat(pix, file);
		}
	}
	
	private Pix convertTo8(Pix pix){
		Pix temp = Convert.convertTo8(pix);
		pix.recycle();
		dumpDebugImage("Convert", temp);
		return temp;
	}
	
	private Pix resizePix(Pix pix){
		int[] dimensions = pix.getDimensions();
		int area = dimensions[0] * dimensions[1];
		if (area > MAX_IMAGE_AREA) {
			float scale = MAX_IMAGE_AREA / (float) area;
			Log.i(TAG, "Scaling input image to a factor of " + scale);
			Pix temp = Scale.scale(pix, scale);
			pix.recycle();
			dumpDebugImage("Scale", temp);
			return temp;
		}
		return pix;
	}
	
	
	/**
	 * Get a Pix instance from our source data Uri
	 * 
	 * @param bmpUri The Uri of our BMP file
	 * @return The resulting Pix or null
	 */
	private Pix getPixFromUri(Uri bmpUri) {
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
