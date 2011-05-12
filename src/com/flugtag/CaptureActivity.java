package com.flugtag;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.flugtag.task.AsyncTaskCompleteListener;
import com.flugtag.task.OCRTask;

public class CaptureActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback{
	private final String TAG = "CaptureActivity";
	
	private Camera camera;

    /**
     * Handle the Activity create 
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     * @category Activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.capture);
        
        //Init the SurfaceHolder and listen for changes
        SurfaceView sv = (SurfaceView) findViewById(R.id.camSurfaceView);
        SurfaceHolder holder = sv.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }    


	/**
	 * Handle the SurfaceHolder create callback
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 * @category SurfaceHolder.Callback
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where to draw.
		camera = Camera.open();
        try {
        	camera.setPreviewDisplay(holder);
        } catch (IOException exception) {
        	camera.release();
        	camera = null;
        	//TODO: Display user message, preview image failed
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }	
	}

	/**
	 * Handle the SurfaceHolder change callback
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
	 * @category SurfaceHolder.Callback
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(camera != null){
			Camera.Parameters params = camera.getParameters();
			params.setPreviewSize(width, height);	
			camera.setParameters(params);
			camera.startPreview();
		}
	}


	/**
	 * Handle the SurfaceHolder destroy callback
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 * @category SurfaceHolder.Callback
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
		if(camera != null){
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}


	/**
	 * Handle button click event
	 * 
	 * @param btn The click target button
	 * @category Button Event
	 */
	public void snapBtnClick(View btn){
		if(camera != null){
			camera.takePicture(null, null, null, this);
		}
	}


	/**
	 * Handle the camera picture taken callback
	 * 
	 * @see android.hardware.Camera.PictureCallback#onPictureTaken(byte[], android.hardware.Camera)
	 * @category Camera.PictureCallback
	 */
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		Log.i(TAG, "onPictureTaken");
		
		//perform the OCR and call back on complete 
		new OCRTask(this, new TaskCompleteListener()).execute(data);
	}
	
	
	public class TaskCompleteListener implements AsyncTaskCompleteListener<String> {

		/**
		 * 
		 * 
		 * @see com.flugtag.task.AsyncTaskCompleteListener#onTaskComplete(java.lang.Object)
		 */
		@Override
		public void onTaskComplete(String result) {
			//TODO: launch an actual activity
			Context ctx = CaptureActivity.this;
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder
				.setTitle("OCR Text")
				.setMessage(result)
				.setPositiveButton("Ok", null)
				.show();
		}

	}
	
}
