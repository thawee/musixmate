package apincer.android.uamp.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Locale;

import apincer.android.uamp.R;

/**
 * Created by e1022387 on 6/4/2017.
 */

public class UIUtils  {
    public static final int INVALID_COLOR = -1;
    public static int colorAccent = INVALID_COLOR;

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
        int color = textView.getContext().getColor(R.color.grey200);
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
}
