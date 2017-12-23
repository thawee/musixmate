package apincer.android.uamp.ui.image;

/**
 * Created by Administrator on 11/8/17.
 */


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewOutlineProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import apincer.android.uamp.R;
import apincer.android.uamp.flexibleadapter.MediaItem;

public class SmartImageView extends AppCompatImageView {
    private Drawable foreground;
    private Drawable badge;
    private boolean drawBadge;
    private boolean badgeBoundsSet = false;
    private int badgeGravity;
    private int badgePadding;
    private String badgeText;
    private int badgeColor;
    private static final int LOADING_THREADS = 5;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(LOADING_THREADS);

    private SmartImageTask currentTask;

    public SmartImageView(Context context) {
        super(context);
        init(context, null);
    }

    public SmartImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SmartImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BadgedImageView, 0, 0);

        badgeGravity = Gravity.END | Gravity.BOTTOM;
        // a.getInt(R.styleable.BadgedImageView_badgeGravity, Gravity.END | Gravity
               // .BOTTOM);
        badgePadding = a.getDimensionPixelSize(R.styleable.BadgedImageView_badgePadding, 0);
        badgeText = a.getString(R.styleable.BadgedImageView_badgeText);
        badgeColor = a.getColor(R.styleable.BadgedImageView_badgeColor, Color.WHITE);
        badge = new BadgeDrawable(context, badgeText, badgeColor);

        final Drawable d = a.getDrawable(R.styleable.ForegroundView_android_foreground);
        if (d != null) {
            setForeground(d);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setOutlineProvider(ViewOutlineProvider.BOUNDS);

        a.recycle();
    }

    // Helpers to set image by URL
    public void setImageMediaItem(MediaItem item,final SmartImageTask.OnCompleteListener completeListener) {
        setImage(new SmartImage(item), completeListener);
    }

    public void setImage(final SmartImage image, final SmartImageTask.OnCompleteListener completeListener) {
        // Cancel any existing tasks for this image view
        if(currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }

        // Set up the new task
        currentTask = new SmartImageTask(getContext(), image);
        currentTask.setOnCompleteHandler(new SmartImageTask.OnCompleteHandler() {
            @Override
            public void onComplete(Bitmap bitmap) {
                if(bitmap != null) {
                    setImageBitmap(bitmap);
                } else {
                    setImageResource(R.drawable.ic_launcher);
                }
                //if (image.isNowPlaying()) {
                    //showBadge(true);
                    //setBadgeText("Playing");
                    //setBadgeColor(getContext().getColor(R.color.fab_listening_background));
                    // borderColor = R.color.fab_listening_background;
                   // setForeground(getContext().getDrawable(R.drawable.ic_music));
               // }else {
                    showBadge(false);
               // }
                if(completeListener != null){
                    completeListener.onComplete(bitmap);
                }
            }
        });

        // Run the task in a threadpool
        threadPool.execute(currentTask);
    }

    public static void cancelAllTasks() {
        threadPool.shutdownNow();
        threadPool = Executors.newFixedThreadPool(LOADING_THREADS);
    }


    public void showBadge(boolean show) {
        drawBadge = show;
    }

    public void setBadgeColor(@ColorInt int color) {
        badgeColor = color;
        updateBadge(getContext());
    }

    public void setBadgeText(String newText) {
        this.badgeText = newText;
        updateBadge(getContext());
    }

    private void updateBadge(Context context) {
        badge = new BadgeDrawable(context, badgeText, badgeColor);
        invalidate();
    }

    public boolean isBadgeVisible() {
        return drawBadge;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (foreground != null) {
            foreground.draw(canvas);
        }
        if (drawBadge) {
            if (!badgeBoundsSet) {
                layoutBadge();
            }
            badge.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (foreground != null) {
            foreground.setBounds(0, 0, w, h);
        }
        layoutBadge();
    }

    private void layoutBadge() {
        Rect badgeBounds = badge.getBounds();
        Gravity.apply(badgeGravity,
                badge.getIntrinsicWidth(),
                badge.getIntrinsicHeight(),
                new Rect(0, 0, getWidth(), getHeight()),
                badgePadding,
                badgePadding,
                badgeBounds);
        badge.setBounds(badgeBounds);
        badgeBoundsSet = true;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (who == foreground);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (foreground != null) foreground.jumpToCurrentState();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (foreground != null && foreground.isStateful()) {
            foreground.setState(getDrawableState());
        }
    }

    /**
     * Returns the drawable used as the foreground of this view. The
     * foreground drawable, if non-null, is always drawn on top of the children.
     *
     * @return A Drawable or null if no foreground was set.
     */
    public Drawable getForeground() {
        return foreground;
    }

    /**
     * Supply a Drawable that is to be rendered on top of the contents of this ImageView
     *
     * @param drawable The Drawable to be drawn on top of the ImageView
     */
    public void setForeground(Drawable drawable) {
        if (foreground != drawable) {
            if (foreground != null) {
                foreground.setCallback(null);
                unscheduleDrawable(foreground);
            }

            foreground = drawable;

            if (foreground != null) {
                foreground.setBounds(0, 0, getWidth(), getHeight());
                setWillNotDraw(false);
                foreground.setCallback(this);
                if (foreground.isStateful()) {
                    foreground.setState(getDrawableState());
                }
            } else {
                setWillNotDraw(true);
            }
            invalidate();
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (foreground != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                foreground.setHotspot(x, y);
        }
    }
}