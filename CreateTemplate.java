package com.example.myfirstapp;
import java.io.File;
import java.io.FileOutputStream;
import com.example.myfirstapp.R.id;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

public class CreateTemplate extends Activity {
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	private Camera mCamera;
    private Preview mPreview;
    private PreviewDrawingSurface mDrawingSurface;
    private ImageButton captureButton;
    private ImageButton okButton;
    private ImageButton cancelButton;
    private Bitmap pictureTaken;
    private ImageView picturePreview;
    private FrameLayout cameraPreview;
    private FrameLayout croppingPreview;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_template);
		
		okButton = (ImageButton) findViewById(R.id.button_ok);
		cancelButton = (ImageButton) findViewById(R.id.button_cancel);
		okButton.setVisibility(View.GONE);
		cancelButton.setVisibility(View.GONE);
		
		mPreview = new Preview(this);
		mDrawingSurface = new PreviewDrawingSurface(this);
		
		safeCameraOpen(0);
		if (mCamera != null) {
			mPreview.setCamera(mCamera, 1);
			mDrawingSurface.initCroppingSquare(mCamera.getParameters().getSupportedPreviewSizes().get(0));
			cameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
			cameraPreview.addView(mPreview);
			
			captureButton = (ImageButton) findViewById(R.id.button_capture);
			captureButton.setOnClickListener(
			    new View.OnClickListener() {
			        @Override
			        public void onClick(View v) {
			            mCamera.takePicture(null, null, mPicture); // get an image from the camera
			            captureButton.setVisibility(View.GONE);
			        }
			    }
			);
		}
	}
	
	protected void onPause() {
		super.onPause();
		releaseCameraAndPreview();
	}
	
	/*protected void onStart() {
		safeCameraOpen(0);
		if (mCamera != null)
			mPreview.setCamera(mCamera);
	}*/
	
	protected void onResume() {
		super.onResume();
		
		safeCameraOpen(0);
		if (mCamera != null) 
			mPreview.setCamera(mCamera, 1);
	}
	
	protected void onStop() {
		super.onStop();
		releaseCameraAndPreview();
	}
	
	public void onDestroy() {
		super.onDestroy();
		releaseCameraAndPreview();
	}
	
	private boolean safeCameraOpen(int id) {
	    boolean qOpened = false;
	  
	    try {
	        releaseCameraAndPreview();
	        mCamera = Camera.open(id);
	        qOpened = (mCamera != null);
	    } catch (Exception e) {
	        Log.e(getString(R.string.app_name), "failed to open Camera");
	        e.printStackTrace();
	    }

	    return qOpened;    
	}
	
	private void releaseCameraAndPreview() {
	    mPreview.setCamera(null, 1);
	    if (mCamera != null) {
	        mCamera.release();
	        mCamera = null;
	    }
	}
	
	public Bitmap toGrayscale(Bitmap bmpOriginal)
	{        
	    int width, height;
	    height = bmpOriginal.getHeight();
	    width = bmpOriginal.getWidth();    

	    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	    Canvas c = new Canvas(bmpGrayscale);
	    Paint paint = new Paint();
	    ColorMatrix cm = new ColorMatrix();
	    cm.setSaturation(0);
	    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
	    paint.setColorFilter(f);
	    c.drawBitmap(bmpOriginal, 0, 0, paint);
	    return bmpGrayscale;
	}
	
	private void doCropImage() {
		Point leftTop = mDrawingSurface.getLeftTop();
		Point rightBottom = mDrawingSurface.getRightBottom();
		
		Size imageSize = mPreview.getPictureSize();
		Size PreviewSize = mPreview.getPreviewSize();
		
		int width = (rightBottom.x - leftTop.x) * imageSize.width / PreviewSize.width;
		int height = (rightBottom.y - leftTop.y) * imageSize.height / PreviewSize.height;

		int x = Math.round(leftTop.x * imageSize.width / PreviewSize.width);
		int y = Math.round(leftTop.y * imageSize.height / PreviewSize.height);
		
		Bitmap cropImage = Bitmap.createBitmap(pictureTaken, x, y, width, height);
		cropImage = toGrayscale(cropImage);
		
// Create Folder
		File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyFirstApp/Templates");
		dir.mkdir();
		
		int n = dir.listFiles().length;
		
		File templateImage;
		do templateImage = new File(dir.getAbsolutePath() + "/" + (++n) + ".PNG");
		while(templateImage.exists());

		try {
			FileOutputStream out = new FileOutputStream(templateImage);
			cropImage.compress(Bitmap.CompressFormat.PNG, 90, out);
		    out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private PictureCallback mPicture = new PictureCallback() {

	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {

	    	pictureTaken = BitmapFactory.decodeByteArray(data, 0, data.length);
	    	
	    	picturePreview = (ImageView) findViewById(id.image_preview);
	    	picturePreview.setImageBitmap(pictureTaken);
	    	
	    	croppingPreview = (FrameLayout) findViewById(R.id.drawing_surface);
	    	croppingPreview.addView(mDrawingSurface);
			
    		okButton.setVisibility(View.VISIBLE);
    		cancelButton.setVisibility(View.VISIBLE);
    		
    		okButton.setOnClickListener(
    			    new View.OnClickListener() {
    			        @Override
    			        public void onClick(View v) {
    			        	croppingPreview.removeAllViews();
    			        	picturePreview.setImageDrawable(null);
    			        	okButton.setVisibility(View.GONE);
    			        	cancelButton.setVisibility(View.GONE);
    			        	captureButton.setVisibility(View.VISIBLE);
    			        	doCropImage();
    			        }
    			    }
    			);
    		
    		cancelButton.setOnClickListener(
    			    new View.OnClickListener() {
    			        @Override
    			        public void onClick(View v) {
    			        	croppingPreview.removeAllViews();
    			        	picturePreview.setImageDrawable(null);
    			        	okButton.setVisibility(View.GONE);
    			        	cancelButton.setVisibility(View.GONE);
    			        	captureButton.setVisibility(View.VISIBLE);
    			        }
    			    }
    			);
	    }
	};
}
