package com.example.myfirstapp;

import com.example.myfirstapp.R.id;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;

//OpenCV
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class SearchTemplate extends Activity {
	
    private Camera mCamera;
    private Preview mPreview;
    private ImageButton scanButton;
    private ImageButton selectButton;
    private FrameLayout cameraPreview;
    private ViewPager viewPager;
    private Bitmap pictureTaken;
    private Bitmap template;
    private ImageView picturePreview;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_template);
		
		mPreview = new Preview(this);		
	    scanButton = (ImageButton) findViewById(R.id.button_scan);
	    scanButton.setVisibility(View.GONE);
		
		viewPager = (ViewPager) findViewById(R.id.view_pager);
	    ImageAdapter adapter = new ImageAdapter(this);
	    viewPager.setAdapter(adapter);

	    selectButton = (ImageButton) findViewById(R.id.button_select);
	    selectButton.setOnClickListener(
			    new View.OnClickListener() {
			        @Override
			        public void onClick(View v) {
			        	viewPager.removeAllViews();
			        	selectButton.setVisibility(View.GONE);
			        	template = ((ImageAdapter)viewPager.getAdapter()).getItem( viewPager.getCurrentItem() );
			        	
// done with template selection, now use camera
			        	safeCameraOpen(0);
			    		if (mCamera != null) {
			    			mPreview.setCamera(mCamera, 1);
			    			cameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
			    			cameraPreview.addView(mPreview);
			    			
			    			scanButton.setVisibility(View.VISIBLE);
			    			scanButton.setOnClickListener(
			    				    new View.OnClickListener() {
			    				        @Override
			    				        public void onClick(View v) {
			    				            mCamera.takePicture(null, null, mPicture); // get an image from the camera
			    				            scanButton.setVisibility(View.GONE);
			    				        }
			    				    }
			    				);
			    		}
			        }
			    }
			);	
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
		
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
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
	
// for opencv
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {
	                //Log.i(TAG, "OpenCV loaded successfully");
	                //mOpenCvCameraView.enableView();
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	};
	
	private void doSearch() {		
// convert Bitmap to Mat		
		Mat templateMat = new Mat ( template.getHeight(), template.getWidth(), CvType.CV_8U, new Scalar(4));
        Bitmap bitmapCopy1 = template.copy(Bitmap.Config.RGB_565, true);
        Utils.bitmapToMat(bitmapCopy1, templateMat);
        
        bitmapCopy1.recycle();
        
        Mat imageMat = new Mat ( pictureTaken.getHeight(), pictureTaken.getWidth(), CvType.CV_8U, new Scalar(4));
        Bitmap bitmapCopy2 = pictureTaken.copy(Bitmap.Config.RGB_565, true);
        Utils.bitmapToMat(bitmapCopy2, imageMat);
        
        bitmapCopy2.recycle();       
		
// feature detection
		FeatureDetector myFeatures = FeatureDetector.create(FeatureDetector.FAST);
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        
        Mat rgb1 = new Mat();
        Mat rgb2 = new Mat();
        Imgproc.cvtColor(templateMat, rgb1, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(   imageMat, rgb2, Imgproc.COLOR_RGBA2RGB);
        
        myFeatures.detect(rgb1, keypoints1);
        myFeatures.detect(rgb2, keypoints2);
        
// compute descriptors
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();
        extractor.compute(templateMat, keypoints1, descriptors1);
        extractor.compute(imageMat, keypoints2, descriptors2);
        
        
// match descriptors
/*------------------------- match method 1 ---------------------------*/
        /*DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);    
        List<MatOfDMatch> matchMatList = new ArrayList<MatOfDMatch>();
        matcher.knnMatch(descriptors1, descriptors2, matchMatList, 2);
        
// find min and max distance
        List<DMatch> goodMatchList = new ArrayList<DMatch>(); 
        double nnDistanceRatio = 0.9;
        for( int i = 0; i < matchMatList.size() ; i++ ) { 
        	DMatch[] matchArray = matchMatList.get(i).toArray();
        	if (matchArray.length < 2)
        		continue;
        	
        	if (matchArray[0].distance < nnDistanceRatio * matchArray[1].distance)
        		goodMatchList.add(matchArray[0]);
        }*/
        
/*------------------------- match method 2 ---------------------------*/
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);    
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);
        
        DMatch[] matchArray = matches.toArray();
            
// find min and max distance
        double max_dist = 0; double min_dist = 130;
        for( int i = 0; i < matchArray.length; i++ ) { 
        	float dist = matchArray[i].distance;
        	
        	if( dist < min_dist ) min_dist = dist;
        	if( dist > max_dist ) max_dist = dist;
        }
     
// get only the good matches    
        List<DMatch> goodMatchList = new ArrayList<DMatch>();       
        for( int i = 0; i < matchArray.length; i++ ) { 
        	float dist = matchArray[i].distance;
        	
        	if( dist < min_dist * 2 ) 
        		goodMatchList.add(matchArray[i]);
        }
/*------------------------- end of method 2 ---------------------------*/
        
        int n = goodMatchList.size();
        DMatch[] goodMatchArray = new DMatch[n]; //(DMatch[]) goodMatchList.toArray();
        
        for (int i=0; i < n; i++)
        	goodMatchArray[i] = goodMatchList.get(i);
        
        MatOfDMatch good_matches = new MatOfDMatch( goodMatchArray );
        
// output result        
        Mat outputMat = new Mat();
        Features2d.drawMatches(rgb1, keypoints1, rgb2, keypoints2, good_matches, outputMat);

        Bitmap bitmapOutput = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.RGB_565); //template.getWidth() template.getHeight()
        Utils.matToBitmap(outputMat, bitmapOutput);
        picturePreview.setImageBitmap(bitmapOutput);
	}
	
	private PictureCallback mPicture = new PictureCallback() {

	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {

	    	pictureTaken = BitmapFactory.decodeByteArray(data, 0, data.length);
	    	
	    	picturePreview = (ImageView) findViewById(id.image_preview);
	    	//picturePreview.setImageBitmap(pictureTaken);
	    	doSearch();
	    }
	};
}