package com.example.cameraread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cameraread.R.id;
import com.googlecode.tesseract.android.TessBaseAPI;

public class ReadText extends Activity {
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
    public String lang = "eng"; //jpn, chi_sim
    public String DATA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CameraRead/";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initApp();
	}
	
	protected void onPause() {
		super.onPause();
		releaseCameraAndPreview();
	}
	
	protected void onStart() {
		super.onStart();
		initApp();
	}
	
	protected void onResume() {
		super.onResume();
		initApp();
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
	
	private void initApp() {
		setContentView(R.layout.activity_readtext);
		
// step 1: init global variables
		okButton = (ImageButton) findViewById(R.id.button_ok);
		cancelButton = (ImageButton) findViewById(R.id.button_cancel);
		okButton.setVisibility(View.GONE);
		cancelButton.setVisibility(View.GONE);
		
		mPreview = new Preview(this);
		mDrawingSurface = new PreviewDrawingSurface(this);
		
// step 2: init camera
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
			
// step 3: init trained data for text recognition
			File homeDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CameraRead/");
			homeDir.mkdir();
			
			String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

            for (String path : paths) {
                    File dir = new File(path);
                    if (!dir.exists()) {
                            if (!dir.mkdirs()) {
                                    Log.v("info", "ERROR: Creation of directory " + path + " on sdcard failed");
                                    return;
                            } else {
                                    Log.v("info", "Created directory " + path + " on sdcard");
                            }
                    }

            }
            
            Bundle b = getIntent().getExtras();
            lang = b.getString("lang");
            
            if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
                try {

                        AssetManager assetManager = getAssets();
                        InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                        //GZIPInputStream gin = new GZIPInputStream(in);
                        OutputStream out = new FileOutputStream(DATA_PATH
                                        + "tessdata/" + lang + ".traineddata");

                        // Transfer bytes from in to out
                        byte[] buf = new byte[1024];
                        int len;
                        //while ((lenf = gin.read(buff)) > 0) {
                        while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                        }
                        in.close();
                        //gin.close();
                        out.close();
                        
                        Log.v("info", "Copied " + lang + " traineddata");
                } catch (IOException e) {
                        Log.e("info", "Was unable to copy " + lang + " traineddata " + e.toString());
                }
            }
		}
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
		//cropImage = toGrayscale(cropImage);
		
		doRead(cropImage);
	}
	
	private void doRead(Bitmap cropImage) {
		 int w = cropImage.getWidth();
		 int h = cropImage.getHeight();

// Setting pre rotate
		 Matrix mtx = new Matrix();
		 mtx.preRotate( 90 ); 			// rotate 90 degrees

// Rotating Bitmap & convert to ARGB_8888, required by tess
		 cropImage = Bitmap.createBitmap(cropImage, 0, 0, w, h, mtx, false);
		 cropImage = cropImage.copy(Bitmap.Config.ARGB_8888, true);
		 
		 Log.i("info", "Before baseApi");

         TessBaseAPI baseApi = new TessBaseAPI();
         baseApi.setDebug(true);
         baseApi.init(DATA_PATH, lang);
         baseApi.setImage(cropImage);
         
         String recognizedText = baseApi.getUTF8Text();
         
         baseApi.end();
         
         TextView text = (TextView) findViewById(R.id.field);
         text.setText("Text: " + recognizedText);
         
         //EditText text = (EditText) findViewById(R.id.field);
         //text.setText(recognizedText);
         
         Toast.makeText(getApplicationContext(), "Text: "+ recognizedText, Toast.LENGTH_LONG).show();
	}
	
	private PictureCallback mPicture = new PictureCallback() {

	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {

	    	pictureTaken = BitmapFactory.decodeByteArray(data, 0, data.length);
	    	pictureTaken = toGrayscale(pictureTaken);
	    	
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
