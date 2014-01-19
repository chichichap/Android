package com.example.cameraread;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera.Size;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class PreviewDrawingSurface extends View /*implements SurfaceHolder.Callback*/ {
	int screenWidth;
	int screenHeight;
	private static Point leftTop, rightBottom, previousPoint;
	boolean movingTopLine, movingLeftLine, movingRightLine, movingBottomLine;
			
	Paint paint = new Paint();
	public PreviewDrawingSurface(Context context) {
		super(context);
		
		movingTopLine = false; movingLeftLine  = false; movingRightLine  = false; movingBottomLine = false;
	}
	
	public void initCroppingSquare(Size screenSize) {
		screenWidth = screenSize.width;
		screenHeight = screenSize.height;
		
		int w = 100;
		int h = 100;
		leftTop = new Point(screenWidth/2 -w, screenHeight/2 -h);
		rightBottom = new Point(screenWidth/2 +w, screenHeight/2 +h);
	}
	
	public Point getLeftTop() {
		return leftTop;
	}
	
	public Point getRightBottom() {
		return rightBottom;
	}
	
	public void onDraw(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.GREEN);
        canvas.drawRect(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y, paint);
    }
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
		int x,y,dx,dy;
		switch (event.getAction()) { 
			case MotionEvent.ACTION_DOWN:
				x = (int)event.getX();
				y = (int)event.getY();
				previousPoint = new Point(x,y);
				
				if (Math.abs(x - leftTop.x) < 30)
					movingLeftLine = true;
				else if (Math.abs(y - leftTop.y) < 30)
					movingTopLine = true;
				else if (Math.abs(y - rightBottom.y) < 30)
					movingBottomLine = true;
				else if (Math.abs(x - rightBottom.x) < 30)
					movingRightLine = true;
				
				/*if (x < leftTop.x && y > leftTop.y && y < rightBottom.y)
					movingLeftLine = true;
				else if (x > rightBottom.x && y > leftTop.y && y < rightBottom.y)
					movingRightLine = true;
				else if (y < leftTop.y && x > leftTop.x && x < rightBottom.x)
					movingTopLine = true;
				else if (y > rightBottom.y && x > leftTop.x && x < rightBottom.x)
					movingBottomLine = true;*/
				break;
				
			case MotionEvent.ACTION_MOVE:
				x = (int)event.getX();
				y = (int)event.getY();
				dx = x - previousPoint.x;
				dy = y - previousPoint.y;
				
				if (movingLeftLine && leftTop.x + dx >= 0 && leftTop.x + dx < rightBottom.x - 20)
					leftTop.x += dx;
				else if (movingRightLine && rightBottom.x + dx < screenWidth && rightBottom.x + dx > leftTop.x + 20)
					rightBottom.x += dx;
				else if (movingTopLine && leftTop.y + dy >= 0 && leftTop.y + dy < rightBottom.y - 20)
					leftTop.y += dy;
				else if (movingBottomLine && rightBottom.y + dy < screenHeight && rightBottom.y + dy > leftTop.y + 20)
					rightBottom.y += dy;
				
				previousPoint = new Point(x,y); //update previous point
				break;
				
			case MotionEvent.ACTION_UP:
				
				movingTopLine = false; movingLeftLine  = false; movingRightLine  = false; movingBottomLine = false;
				break;
				
		}
		
		invalidate(); //repaint
		return true;
	}
}
