package com.example.cameraread;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {
	
	private static final String TAG = "MyActivity";

    SurfaceHolder mHolder;
    private Camera mCamera;
    List<Size> mSupportedPreviewSizes; // preview size (need to match screen size)
    List<Size> mSupportedPictureSizes; // image size (when saved)

    Preview(Context context) {
        super(context);

// Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public Size getPictureSize() {
    	return mSupportedPictureSizes.get(1);
    }
    
    public Size getPreviewSize() {
    	return mSupportedPreviewSizes.get(0);
    }
    
    public void onPause() {
      mCamera.stopPreview();
    }
    
    public void setCamera(Camera camera, int mode) {   	
        if (mCamera == camera)
        	return;
        
        stopPreviewAndFreeCamera();
        
        mCamera = camera;
        
        if (mCamera != null) {
        	mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        	mSupportedPictureSizes = mCamera.getParameters().getSupportedPictureSizes();
            
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(mSupportedPreviewSizes.get(0).width, mSupportedPreviewSizes.get(0).height);
            parameters.setPictureSize(mSupportedPictureSizes.get(1).width, mSupportedPictureSizes.get(1).height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(parameters);
            
            requestLayout();
          
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

// Important: Call startPreview() to start updating the preview surface. Preview must be started before you can take a picture.
            mCamera.startPreview();
        }
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or reformatting changes here
        
		/*// Now that the size is known, set up the camera parameters and begin the preview.	
	    Camera.Parameters parameters = mCamera.getParameters();
	    parameters.setPreviewSize(h, w);
	    requestLayout();
	    mCamera.setParameters(parameters);*/

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
// The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	    // Surface will be destroyed when we return, so stop the preview.
	    if (mCamera != null) {
	        /*
	          Call stopPreview() to stop updating the preview surface.
	        */
	        mCamera.stopPreview();
	    }
	}

	/**
	  * When this function returns, mCamera will be null.
	  */
	private void stopPreviewAndFreeCamera() {

	    if (mCamera != null) {
	        /*
	          Call stopPreview() to stop updating the preview surface.
	        */
	        mCamera.stopPreview();
	    
	        /*
	          Important: Call release() to release the camera for use by other applications. 
	          Applications should release the camera immediately in onPause() (and re-open() it in
	          onResume()).
	        */
	        mCamera.release();
	    
	        mCamera = null;
	    }
	}
}
