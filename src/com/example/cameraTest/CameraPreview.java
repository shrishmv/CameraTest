
package com.example.cameraTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.cameraTest.CameraUtils.PreviewCallbacks;









import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

/**
 * Camera Preview surface, which extends surfaceview.
 * This view adjusts the margins and sets the best aspect ration for the image
 * NOTE: currently Flash is disabled
 * 
 * @author SHRISH 
 *
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{				
    
	private static String LOGTAG = "CameraPreview";
	
	@SuppressWarnings("unused")
	public final double 	 ASPECT_TOLERANCE	 				= 0.1;
	private SurfaceHolder	 mHolder;		
	private Camera			 mCamera;
	private PreviewCallbacks			mPreviewCallbacks;

	private double mAspectRatio;
    
	/**
	 * Constructor
	 * 
	 * @param context - application context
	 * @param camera - camera Object
	 * @param margins - margins to adjust the layout
	 */
    public CameraPreview(Context context, 
    					 Camera camera, 
    					 PreviewCallbacks PreviewCallbacks, 
    					 double aspectRatio) {
        super(context);	      
        
        mPreviewCallbacks = PreviewCallbacks;
        mCamera				= camera;
        mHolder 			= getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);
        setWillNotDraw(false);
		mAspectRatio = aspectRatio;
    }
   
    /**
     * Settings the aspect ratio
     * @param ar
     */
    public void setAspectRatio(double ar){
    	mAspectRatio = ar;
    }
    
    /**
     * Sets the new camera for the preview
     * @param camera
     */
    public void setCamera(Camera camera){
    	mCamera				= camera;
    }

    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
		if(null == mCamera) {
        	return;
         }
		
		 mPreviewCallbacks.stopCameraPreview();	
					
		 calculateImageSize(width, height);
	    
	    mPreviewCallbacks.startCameraPreview();	    
	    mPreviewCallbacks.surfaceChanged(width, height);
	    Log.d(LOGTAG, "EXIT: surfaceChanged");		
	}		

	/**
	 * Calculates the appropriate preview sizes and picture sizes for the
	 * current resolution 
	 */
	public void calculateImageSize(int width, int height){
		if(null == mCamera){
			return;
		}
		
	    Parameters parameters 	= mCamera.getParameters();		    
	    Camera.Size previewSize = getOptimalPreviewSize(width, height);
	    Camera.Size	pictureSize	= getOptimalPictureSize(previewSize.width, previewSize.height);		
	    
	    Log.i(LOGTAG, " cam preview picture w , h -"+pictureSize.width+" "+pictureSize.height);
	    Log.i(LOGTAG, " cam preview preview w , h -"+previewSize.width+" "+previewSize.height);
	    
		parameters.setPictureSize(pictureSize.width, pictureSize.height);
	    parameters.setPreviewSize(previewSize.width, previewSize.height);
	    mCamera.setParameters(parameters);
	}		

	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        
		if(null == mCamera) {
        	return;
         }
		
        /*
         * The Surface has been created, now tell the camera where to draw the preview.
         */
        try {	
        	 setWillNotDraw(false); 
            mCamera.setPreviewDisplay(holder);            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        mPreviewCallbacks.surfaceCreated();        
        Log.d(LOGTAG, "EXIT: surfaceCreated");	
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {			
		mPreviewCallbacks.surfaceDestroyed();		
		Log.d(LOGTAG, "EXIT: surfaceDestroyed");	
	}
	
	/**
	 * When this function returns, mCamera will be null and other applications can use camera
	 */
	public void resetCamera() {
	        mCamera = null;

	}
	
	/**
	 * Selects the optimal preview size for the camera based on the aspect ratios supported
	 * 
	 * @param previewSizeList
	 * @param targetWidth
	 * @param targetHeight
	 * @return
	 */
	private Camera.Size selectOptimalPreviewSize(ArrayList<Camera.Size> previewSizeList, 
													int targetWidth, int targetHeight) {
		Camera.Size				optimalSize			= null;
		int 					minDiffHeight		= Integer.MAX_VALUE;
		int 					minDiffWidth		= Integer.MAX_VALUE;
		ArrayList<Camera.Size>	temp				= new ArrayList<Camera.Size>();
		
		for(int i = 0 ; i < previewSizeList.size() ; i++) {
			Camera.Size	size		= previewSizeList.get(i);				
			int currentHeight		= size.height;
			int currentDiffHeight	= Math.abs(currentHeight - targetHeight);
			if(currentDiffHeight < minDiffHeight){
				minDiffHeight	= currentDiffHeight;
			}
		}
		
		for(int i = 0 ; i < previewSizeList.size() ; i++) {
			Camera.Size	size		= previewSizeList.get(i);				
					
			if(Math.abs(size.height - targetHeight) == minDiffHeight){
				temp.add(size);
			}
		}
		
        if(1 == temp.size()) {
        	return temp.get(0);
        }	         	       
        else {
			for(int i = 0 ; i < temp.size() ; i++) {
				Camera.Size	size		= temp.get(i);				
				int currentWidth		= size.width;
				int currentDiffWidth	= Math.abs(currentWidth - targetWidth);
				if(currentDiffWidth < minDiffWidth){
					minDiffWidth	= currentDiffWidth;
					optimalSize		= size;
				}
			}	        	
        }
		
		return optimalSize;
		
	}
	
	/**
	 * gets the optimal preview size for the camera based on the aspect ratios supported
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	private Camera.Size getOptimalPreviewSize(int width, int height) {        
        
		Parameters 				parameters 		= mCamera.getParameters();
	    List<Camera.Size> 		listPreviewSize = parameters.getSupportedPreviewSizes();
	    ArrayList<Camera.Size>	temp			= new ArrayList<Camera.Size>();
    	
        for (Camera.Size size : listPreviewSize) {	        
        	
            Double 	ratio 		= (double) size.width / size.height;

            
            /**
             * Since the camera and display will be 16:9, other resolutions
             * will cause grey band to appear
             */
            if(ratio.equals(mAspectRatio)){
            	temp.add(size);            	
            	break;
            }
        }    	
        
        if(1 == temp.size()) {
        	return temp.get(0);
        }else if(1 > temp.size()) {
        	return selectOptimalPreviewSize((ArrayList<Camera.Size>)listPreviewSize, width, 
        																			height);
        }else{
        	return selectOptimalPreviewSize(temp, width, height);	        	
        }
    }
	
	/**
	 * sets the optimal picture size for the camera capture based on the aspect ratios supported
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	private Camera.Size getOptimalPictureSize(int width, int height) {
			        	        
		Camera.Size				optimalSize			= null;
        double 		 			targetRatio		= (double)width / height;
        int 					maxResolution	= 0;
		Parameters 				parameters 		= mCamera.getParameters();
	    List<Camera.Size> 		listPictureSize = parameters.getSupportedPictureSizes();
	    ArrayList<Camera.Size>	temp			= new ArrayList<Camera.Size>();
	    
	    for (Camera.Size size : listPictureSize) {        	
            Double 	ratio 		= (double) size.width / size.height;	            

            
            /**
             * Since the camera and display will be 16:9, other resolutions
             * will cause grey band to appear
             */
            if (ratio.equals(mAspectRatio)) {
            	temp.add(size);	            		            	
	        	
	        	break;
            }
        }
	    
	    if(0 == temp.size()){
	    	
			optimalSize = listPictureSize.get(0);
			return optimalSize;
		}else if(1 == temp.size()) {
        	return temp.get(0);
        }else{
			for(int i = 0 ; i < temp.size() ; i++) {
				Camera.Size	size		= temp.get(i);				
				int currentResolution	= size.width * size.height; 				
						
				if(currentResolution > maxResolution){
					maxResolution	= currentResolution;
					optimalSize		= size;						
				}							
			}
			
			if(null == optimalSize){
				
				Log.e(LOGTAG, " NULL PICTURE SIZE");
			}
			return optimalSize;			
        }        
    }			

}
