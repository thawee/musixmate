package apincer.android.uamp.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import apincer.android.uamp.R;

/**
 * Created by e1022387 on 6/4/2017.
 */

public class UIUtils  {
    public static final int INVALID_COLOR = -1;
    public static int colorAccent = INVALID_COLOR;


    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Convert a dp float value to pixels
     *
     * @param dp      float value in dps to convert
     * @return DP value converted to pixels
     */
    public static int dp2px(Context context, float dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
        return Math.round(px);
    }

    public static void setHeight(View view, int itemHeight, int itemCount, int maxHeight) {
        if((itemHeight*itemCount) > maxHeight) {
            view.getLayoutParams().height = maxHeight;
        }else {
            view.getLayoutParams().height = itemHeight*itemCount;
        }
    }

    @SuppressLint("RestrictedApi")
    public static void makePopForceShowIcon(PopupMenu popupMenu) {
        try {
            Field mFieldPopup=popupMenu.getClass().getDeclaredField("mPopup");
            mFieldPopup.setAccessible(true);
            MenuPopupHelper mPopup = (MenuPopupHelper) mFieldPopup.get(popupMenu);
            mPopup.setForceShowIcon(true);
        } catch (Exception e) {

        }
    }

    public static boolean colorizeToolbarOverflowButton(@NonNull Toolbar toolbar, @ColorInt int toolbarIconsColor) {
        final Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon == null)
            return false;
        toolbar.setOverflowIcon(getTintedDrawable(overflowIcon, toolbarIconsColor));
        return true;
    }

    public static Drawable getTintedDrawable(@NonNull Drawable inputDrawable, @ColorInt int color) {
        Drawable wrapDrawable = DrawableCompat.wrap(inputDrawable);
        DrawableCompat.setTint(wrapDrawable, color);
        DrawableCompat.setTintMode(wrapDrawable, PorterDuff.Mode.SRC_IN);
        return wrapDrawable;
    }

    public static void getTintedDrawable(@NonNull ImageButton btn, @ColorInt int color) {
        Drawable wrapDrawable = DrawableCompat.wrap(btn.getDrawable());
        DrawableCompat.setTint(wrapDrawable, color);
        DrawableCompat.setTintMode(wrapDrawable, PorterDuff.Mode.SRC_IN);
    }

    public static void getTintedDrawable(@NonNull SearchView searchView, @ColorInt int color) {
        try {
            Field searchField = SearchView.class
                    .getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchBtn = (ImageView) searchField.get(searchView);
            Drawable wrapDrawable = DrawableCompat.wrap(searchBtn.getDrawable());
            DrawableCompat.setTint(wrapDrawable, color);
            DrawableCompat.setTintMode(wrapDrawable, PorterDuff.Mode.SRC_IN);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
    }

    public static void setSearchViewTextColor(@NonNull SearchView searchView, @ColorInt int color, @ColorInt int hintColor) {
        try {
            Field searchField = SearchView.class
                    .getDeclaredField("mSearchSrcTextView");
            searchField.setAccessible(true);
            TextView searchBtn = (TextView) searchField.get(searchView);
            searchBtn.setTextColor(color);
            searchBtn.setHintTextColor(hintColor);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
    }

    public static Drawable getTintedDrawable(@NonNull Context context, @DrawableRes int drawableId, @ColorInt int color) {
        Drawable inputDrawable = context.getDrawable(drawableId);
        Drawable wrapDrawable = DrawableCompat.wrap(inputDrawable);
        DrawableCompat.setTint(wrapDrawable, color);
        DrawableCompat.setTintMode(wrapDrawable, PorterDuff.Mode.SRC_IN);
        return wrapDrawable;
    }

    /**
     * Get a color value from a theme attribute.
     * @param context used for getting the color.
     * @param attribute theme attribute.
     * @param defaultColor default to use.
     * @return color value
     */
    public static int getThemeColor(Context context, int attribute, int defaultColor) {
        int themeColor = 0;
        String packageName = context.getPackageName();
        try {
            Context packageContext = context.createPackageContext(packageName, 0);
            ApplicationInfo applicationInfo =
                    context.getPackageManager().getApplicationInfo(packageName, 0);
            packageContext.setTheme(applicationInfo.theme);
            Resources.Theme theme = packageContext.getTheme();
            TypedArray ta = theme.obtainStyledAttributes(new int[] {attribute});
            themeColor = ta.getColor(0, defaultColor);
            ta.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return themeColor;
    }

    public static Bitmap tintImage(Bitmap bitmap, int color) {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap bitmapResult = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapResult);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmapResult;
    }

    public static void setColorFilter(MenuItem item, int color) {
        Drawable drawable = item.getIcon();
        if(drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        return context.getResources().getDisplayMetrics();
    }

    public static float dpToPx(Context context, float dp) {
        return Math.round(dp * getDisplayMetrics(context).density);
    }
    public static int fetchAccentColor(Context context, @ColorInt int defColor) {
        if (colorAccent == INVALID_COLOR) {
            int attr = android.R.attr.colorAccent;
            TypedArray androidAttr = context.getTheme().obtainStyledAttributes(new int[]{attr});
            colorAccent = androidAttr.getColor(0, defColor);
            androidAttr.recycle();
        }
        return colorAccent;
    }

    public static GradientDrawable createGradient(int backgroundColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadii(new float[]{8, 8, 8, 8, 0, 0, 0, 0});
        shape.setColor(backgroundColor);
        shape.setStroke(3, Color.parseColor("#d4e2ff"));
        return shape;
    }

    public static void highlightSearchKeyword(@NonNull TextView textView,
                                     @Nullable String originalText, @Nullable String constraint) {
        int color = textView.getContext().getColor(R.color.search_bar_color_end);
        originalText = StringUtils.trimToEmpty(originalText);
        constraint = StringUtils.trimToEmpty(constraint);
        int i = originalText.toLowerCase(Locale.getDefault()).indexOf(constraint.toLowerCase(Locale.getDefault()));
        if ((!StringUtils.isEmpty(originalText)) && (i != -1)) {
            Spannable spanText = Spannable.Factory.getInstance().newSpannable(originalText);
            if (i != -1 && !StringUtils.isEmpty(constraint)) {
                spanText.setSpan(new BackgroundColorSpan(color), i, i + constraint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanText.setSpan(new ForegroundColorSpan(Color.BLACK), i, i + constraint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(spanText, TextView.BufferType.SPANNABLE);
        } else {
            textView.setText(originalText, TextView.BufferType.NORMAL);
        }
    }

    public static void highlightSearchKeywordOnTitle(@NonNull TextView textView,
                                              @Nullable String originalText, @Nullable String constraint) {
        int color = textView.getContext().getColor(R.color.search_bar_color_start);
        originalText = StringUtils.trimToEmpty(originalText);
        constraint = StringUtils.trimToEmpty(constraint);
        int i = originalText.toLowerCase(Locale.getDefault()).indexOf(constraint.toLowerCase(Locale.getDefault()));
        if ((!StringUtils.isEmpty(originalText)) && (i != -1)) {
            Spannable spanText = Spannable.Factory.getInstance().newSpannable(originalText);
            if (i != -1 && !StringUtils.isEmpty(constraint)) {
                spanText.setSpan(new ForegroundColorSpan(color), i, i + constraint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(spanText, TextView.BufferType.SPANNABLE);
        } else {
            textView.setText(originalText, TextView.BufferType.NORMAL);
        }
    }

    public static void highlightText(@NonNull TextView textView, @Nullable String originalText,
                                     @Nullable String constraint, String constraint2, @ColorInt int color) {
        originalText = StringUtils.trimToEmpty(originalText);
        constraint = StringUtils.trimToEmpty(constraint);
        constraint2 = StringUtils.trimToEmpty(constraint2);
        int i = originalText.toLowerCase(Locale.getDefault()).indexOf(constraint.toLowerCase(Locale.getDefault()));
        int ii = originalText.toLowerCase(Locale.getDefault()).indexOf(constraint2.toLowerCase(Locale.getDefault()));
        if ((!StringUtils.isEmpty(originalText)) && (i != -1 || ii != -1)) {
            Spannable spanText = Spannable.Factory.getInstance().newSpannable(originalText);
            if(i != -1 && !StringUtils.isEmpty(constraint)) {
                spanText.setSpan(new ForegroundColorSpan(color), i, i + constraint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanText.setSpan(new StyleSpan(Typeface.BOLD), i, i + constraint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if(ii != -1 && !StringUtils.isEmpty(constraint2)) {
                spanText.setSpan(new ForegroundColorSpan(color), ii, ii + constraint2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanText.setSpan(new StyleSpan(Typeface.BOLD), ii, ii + constraint2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(spanText, TextView.BufferType.SPANNABLE);
        } else {
            textView.setText(originalText, TextView.BufferType.NORMAL);
        }
    }

    public static boolean isColorDark(int color) {
        double darkness = 1-(0.299*Color.red(color) +
                0.587*Color.green(color)+
                0.144*Color.blue(color))/255;
        if(darkness<0.5) {
            return false; // light color
        }
        return true; // dark color
    }

    public static int lighten(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = lightenColor(red, fraction);
        green = lightenColor(green, fraction);
        blue = lightenColor(blue, fraction);
        int alpha = Color.alpha(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static int darken(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = darkenColor(red, fraction);
        green = darkenColor(green, fraction);
        blue = darkenColor(blue, fraction);
        int alpha = Color.alpha(color);

        return Color.argb(alpha, red, green, blue);
    }

    private static int darkenColor(int color, double fraction) {
        return (int)Math.max(color - (color * fraction), 0);
    }

    private static int lightenColor(int color, double fraction) {
        return (int) Math.min(color + (color * fraction), 255);
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(inputManager !=null && view.getRootView().getWindowToken()!=null) {
            inputManager.hideSoftInputFromWindow(view.getRootView().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void showKeyboard(Context context, View view) {
        try {
            view.requestFocus();
            InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A helper class for providing a shadow on sheets
     */
    @TargetApi(21)
    public static class ShadowOutline extends ViewOutlineProvider {

        int width;
        int height;

        public ShadowOutline(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, width, height);
        }
    }

    public static Drawable buildGradientBackground(int bgColor) {
        try {
            // convert to HSV to lighten and darken
            int alpha = Color.alpha(bgColor);
            float[] hsv = new float[3];
            Color.colorToHSV(bgColor, hsv);
            hsv[2] -= .6;
            int darker = Color.HSVToColor(alpha, hsv);
            hsv[2] += .8;
            int lighter = Color.HSVToColor(alpha, hsv);

            // create gradient using lighter and darker colors
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,new int[] { darker, lighter});
            gd.setGradientType(GradientDrawable.SWEEP_GRADIENT);

            return gd;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap buildGradientBitmap( Context context, int bgColor, int width, int height,int topRight, int topLeft,int bottomLeft, int bottomRight) {
        try {
            // convert to HSV to lighten and darken
            int alpha = Color.alpha(bgColor);
            float[] hsv = new float[3];
            Color.colorToHSV(bgColor, hsv);
            hsv[2] -= .1;
            int darker = Color.HSVToColor(alpha, hsv);
            hsv[2] += .3;
            int lighter = Color.HSVToColor(alpha, hsv);

            // create gradient using lighter and darker colors
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,new int[] { darker, lighter});
            gd.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            // set corner size
            // top-left, top-right, bottom-right, bottom-left
            //gd.setCornerRadii(new float[] {4,4,4,4,8,8,8,8});
            gd.setCornerRadii(new float[] {topLeft,topLeft,topRight,topRight,bottomRight,bottomRight,bottomLeft,bottomLeft});
            // get density to scale bitmap for device
            float dp = context.getResources().getDisplayMetrics().density;

            // create bitmap based on width and height of widget
            Bitmap bitmap = Bitmap.createBitmap(Math.round(width * dp), Math.round(height * dp),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas =  new Canvas(bitmap);
            gd.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            gd.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap buildGradientBitmap( Context context, Bitmap source, int width, int height, int border, int corner) {
        try {
            Palette palette = Palette.from(source).generate();
            int bgColor = context.getColor(R.color.grey200);
           // bgColor = palette.getDominantColor(bgColor);
            bgColor = palette.getMutedColor(bgColor);

            // convert to HSV to lighten and darken
            int alpha = Color.alpha(bgColor);
            float[] hsv = new float[3];
            Color.colorToHSV(bgColor, hsv);
            hsv[2] -= .1;
            int darker = Color.HSVToColor(alpha, hsv);
            hsv[2] += .3;
            int lighter = Color.HSVToColor(alpha, hsv);

            // create gradient using lighter and darker colors
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,new int[] { darker, lighter});
            gd.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            // set corner size
            // top-left, top-right, bottom-right, bottom-left
            //gd.setCornerRadii(new float[] {4,4,4,4,8,8,8,8});
            gd.setCornerRadii(new float[] {corner,corner,corner,corner,corner,corner,corner,corner});
            // get density to scale bitmap for device
            float dp = context.getResources().getDisplayMetrics().density;

            // create bitmap based on width and height of widget
            Bitmap bitmap = Bitmap.createBitmap(Math.round(width), Math.round(height),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas =  new Canvas(bitmap);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(source, border, border, paint);
            gd.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            gd.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static  Bitmap addBorderToBitmap(Bitmap srcBitmap, int borderWidth, int borderColor){
        // Initialize a new Bitmap to make it bordered bitmap
        Bitmap dstBitmap = Bitmap.createBitmap(
                srcBitmap.getWidth() + borderWidth*2, // Width
                srcBitmap.getHeight() + borderWidth*2, // Height
                Bitmap.Config.ARGB_8888 // Config
        );

        /*
            Canvas
                The Canvas class holds the "draw" calls. To draw something, you need 4 basic
                components: A Bitmap to hold the pixels, a Canvas to host the draw calls (writing
                into the bitmap), a drawing primitive (e.g. Rect, Path, text, Bitmap), and a paint
                (to describe the colors and styles for the drawing).
        */
        // Initialize a new Canvas instance
        Canvas canvas = new Canvas(dstBitmap);

        // Initialize a new Paint instance to draw border
        Paint paint = new Paint();
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        paint.setAntiAlias(true);

        /*
            Rect
                Rect holds four integer coordinates for a rectangle. The rectangle is represented by
                the coordinates of its 4 edges (left, top, right bottom). These fields can be accessed
                directly. Use width() and height() to retrieve the rectangle's width and height.
                Note: most methods do not check to see that the coordinates are sorted correctly
                (i.e. left <= right and top <= bottom).
        */
        /*
            Rect(int left, int top, int right, int bottom)
                Create a new rectangle with the specified coordinates.
        */

        // Initialize a new Rect instance
        /*
            We set left = border width /2, because android draw border in a shape
            by covering both inner and outer side.
            By padding half border size, we included full border inside the canvas.
        */
        Rect rect = new Rect(
                borderWidth / 2,
                borderWidth / 2,
                canvas.getWidth() - borderWidth / 2,
                canvas.getHeight() - borderWidth / 2
        );

        /*
            public void drawRect (Rect r, Paint paint)
                Draw the specified Rect using the specified Paint. The rectangle will be filled
                or framed based on the Style in the paint.

            Parameters
                r : The rectangle to be drawn.
                paint : The paint used to draw the rectangle

        */
        // Draw a rectangle as a border/shadow on canvas
       // canvas.drawRect(rect,paint);
        canvas.drawCircle(borderWidth / 2, borderWidth / 2, canvas.getWidth(), paint);
        canvas.drawColor(borderColor);
        /*
            public void drawBitmap (Bitmap bitmap, float left, float top, Paint paint)
                Draw the specified bitmap, with its top/left corner at (x,y), using the specified
                paint, transformed by the current matrix.

                Note: if the paint contains a maskfilter that generates a mask which extends beyond
                the bitmap's original width/height (e.g. BlurMaskFilter), then the bitmap will be
                drawn as if it were in a Shader with CLAMP mode. Thus the color outside of the
                original width/height will be the edge color replicated.

                If the bitmap and canvas have different densities, this function will take care of
                automatically scaling the bitmap to draw at the same density as the canvas.

            Parameters
                bitmap : The bitmap to be drawn
                left : The position of the left side of the bitmap being drawn
                top : The position of the top side of the bitmap being drawn
                paint : The paint used to draw the bitmap (may be null)
        */

        // Draw source bitmap to canvas
        canvas.drawBitmap(srcBitmap, borderWidth, borderWidth, null);


        // Return the bordered circular bitmap
        return dstBitmap;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 12;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Returns {@code null} if this couldn't be determined.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @SuppressLint("PrivateApi")
    public static Boolean hasNavigationBar() {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            IBinder serviceBinder = (IBinder)serviceManager.getMethod("getService", String.class).invoke(serviceManager, "window");
            Class<?> stub = Class.forName("android.view.IWindowManager$Stub");
            Object windowManagerService = stub.getMethod("asInterface", IBinder.class).invoke(stub, serviceBinder);
            Method hasNavigationBar = windowManagerService.getClass().getMethod("hasNavigationBar");
            return (boolean)hasNavigationBar.invoke(windowManagerService);
        } catch (ClassNotFoundException | ClassCastException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.w("YOUR_TAG_HERE", "Couldn't determine whether the device has a navigation bar", e);
            return null;
        }
    }

    public static boolean hasSoftKeys(WindowManager windowManager){
        Display d = windowManager.getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        return (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
    }
}
