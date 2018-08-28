/**
 * Spectrogram Android application
 * Copyright (c) 2013 Guillaume Adam  http://www.galmiza.net/

 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the use of this software.
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:

 * 1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package net.galmiza.android.spectrogram;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

/**
 * Class associated with the spectrogram view
 * Handles events:
 *  onSizeChanged, onTouchEvent, onDraw
 */
public class FrequencyView extends View {
    
    // Attributes
  //  private Activity activity;
    private Paint paint = new Paint();
    private Bitmap bitmap;
    private Canvas canvas;
    private int pos;
    private int samplingRate;
    private int width, height;
    private float[] magnitudes;
    private int[] colorRainbow = new int[] {    0xFFFFFFFF, 0xFFFF00FF, 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFF000000 };
    private int[] colorFire = new int[] {    0xFFFFFFFF, 0xFFFFFF00, 0xFFFF0000, 0xFF000000 };
    private int[] colorIce = new int[] {    0xFFFFFFFF, 0xFF00FFFF, 0xFF0000FF, 0xFF000000 };
    private int[] colorGrey = new int[] {    0xFFFFFFFF, 0xFF000000 };
    private long duration;

    // Window
    float minX, minY, maxX, maxY;
    public FrequencyView(Context context) {
        super(context);
      //  activity = (Activity) findViewById(android.R.id.content).getContext();
    }
    public FrequencyView(Context context, AttributeSet attrs) {
        super(context, attrs);
       // activity = (Activity) findViewById(android.R.id.content).getContext();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        if (bitmap!=null)    bitmap.recycle();
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }
    @SuppressLint("ClickableViewAccessibility")
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        invalidate();
        return true;
    }
    
    /**
     * Simple sets
     */
    public void setFFTResolution(int res) {
        magnitudes = new float[res];
    }
    public void setSamplingRate(int sampling) {
        samplingRate = sampling;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }
    public void setMagnitudes(float[] m) {
        System.arraycopy(m, 0, magnitudes, 0, m.length);
    }
    
    /**
     * Called whenever a redraw is needed
     * Renders spectrogram and scale on the right
     * Frequency scale can be linear or logarithmic
     */
    @Override
    public void onDraw(Canvas canvas) {
        int[] colors = null;
        String colorScale = "Rainbow"; //Misc.getPreference(activity, "color_scale", activity.getString(R.string.preferences_color_scale_default_value));
             if (colorScale.equals("Grey"))     colors = colorGrey;
        else if (colorScale.equals("Fire"))     colors = colorFire;
        else if (colorScale.equals("Ice"))      colors = colorIce;
        else if (colorScale.equals("Rainbow"))  colors = colorRainbow;

        int wColor = 18;
       // int wColorLabel = 40;
        int wFrequency = 52;
        int rWidth = width-wColor-wFrequency;
        int hDuration = 12;
        int rHeight = height-hDuration;
        paint.setStrokeWidth(1);
        
        // Get scale preferences
		//String defFrequency = activity.getString(R.string.preferences_frequency_scale_default_value);
    	boolean logFrequency = false; //!Misc.getPreference("frequency_scale", defFrequency).equals(defFrequency);
        
        // Update buffer bitmap
        paint.setColor(Color.BLACK);
       // this.canvas.drawLine(pos%rWidth, 0, pos%rWidth, height, paint);
        this.canvas.drawLine(pos%rWidth, 0, pos%rWidth, rHeight, paint);
        for (int i=0; i<rHeight; i++) {
        	float j = getValueFromRelativePosition((float)(rHeight-i)/rHeight, 1, samplingRate/2, logFrequency);
            j /= samplingRate/2;
        	float mag = magnitudes[(int) (j*magnitudes.length/2)];
            float db = (float) Math.max(0,-20*Math.log10(mag));
            int c = getInterpolatedColor(colors, db*0.009f);
            paint.setColor(c);
            int x = pos%rWidth;
            int y = i;
            this.canvas.drawPoint(x, y, paint);
            this.canvas.drawPoint(x, y, paint); // make color brighter
            //this.canvas.drawPoint(pos%rWidth, height-i, paint); // make color even brighter
        }
        
        // Draw bitmap
        if (pos<rWidth) {
            canvas.drawBitmap(bitmap, wColor, 0, paint);
        } else {
            canvas.drawBitmap(bitmap, (float) wColor - pos%rWidth, 0, paint);
            canvas.drawBitmap(bitmap, (float) wColor + (rWidth - pos%rWidth), 0, paint);
        }

        // Draw color scale
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, wColor, height, paint);
        for (int i=0; i<height; i++) {
            int c = getInterpolatedColor(colors, ((float) i)/height);
            paint.setColor(c);
            canvas.drawLine(0, i, wColor-5, i, paint);
        }
        
        // Draw frequency scale
        float ratio = 0.7f*getResources().getDisplayMetrics().density;
        paint.setTextSize(12f*ratio);
        paint.setColor(Color.BLACK);
        canvas.drawRect(rWidth + wColor, 0, width, rHeight, paint);
        paint.setColor(Color.WHITE);
        //canvas.drawText("kHz", rWidth + wColor, 12*ratio, paint);
        if (logFrequency) {
        	for (int i=1; i<5; i++) {
	    		float y = getRelativePosition((float) Math.pow(10,i), 1, samplingRate/2, logFrequency);
	    		canvas.drawText("1e"+i, rWidth + wColor, (1f-y)*rHeight, paint);
    		}
        } else {
            int inc = 5000;
	        //for (int i=0; i<(samplingRate-500)/2; i+=1000) {
            for (int i=0; i<(samplingRate-500)/2; i+=inc) {
                if ((i / 1000) > 0) {
                    canvas.drawText(" " + i / 1000 + " kHz", rWidth + wColor, rHeight * (1f - (float) i / (samplingRate / 2)), paint);
                }
            }
        }

        // Draw duration scale

       // ratio = 0.7f*getResources().getDisplayMetrics().density;
        paint.setTextSize(12f*ratio);
        paint.setColor(Color.BLACK);
        canvas.drawRect(wColor, rHeight, width-wFrequency, height, paint);
        paint.setColor(Color.WHITE);
        int inc = (int)duration/5;
        int wDuration = (width-wColor-wFrequency)/5;
        int endWidth = (width - wColor - wFrequency);
        for (int i=0; i<duration; i+=inc) {
            if((endWidth-(((i / inc) * wDuration) + wColor))>=(wDuration/2)) {
                canvas.drawText(formatDuration(i), ((i / inc) * wDuration) + wColor, height, paint);
            }
        }
        canvas.drawText(formatDuration(duration), endWidth, height, paint);

        pos++;
    }

    public static String formatDuration(long milliseconds) {
        long s = milliseconds / 1000 % 60;
        long m = milliseconds / 1000 / 60 % 60;
        long h = milliseconds / 1000 / 60 / 60 % 24;
        if (h == 0) return String.format(Locale.getDefault(), "%02d:%02d", m, s);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }

    
    /**
     * Converts relative position of a value within given boundaries
     * Log=true for logarithmic scale
     */
    private float getRelativePosition(float value, float minValue, float maxValue, boolean log) {
    	if (log)	return ((float) Math.log10(1+value-minValue) / (float) Math.log10(1+maxValue-minValue));
    	else		return (value-minValue)/(maxValue-minValue);
    }
    
    /**
     * Returns a value from its relative position within given boundaries
     * Log=true for logarithmic scale
     */
    private float getValueFromRelativePosition(float position, float minValue, float maxValue, boolean log) {
    	if (log)	return (float) (Math.pow(10, position*Math.log10(1+maxValue-minValue))+minValue-1);
    	else		return minValue + position*(maxValue-minValue);
    }
    
    /**
     * Calculate rainbow colors
     */
    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }
    public int getInterpolatedColor(int[] colors, float unit) {
        if (unit <= 0) return colors[0];
        if (unit >= 1) return colors[colors.length - 1];
        
        float p = unit * (colors.length - 1);
        int i = (int) p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i + 1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }
    
}
