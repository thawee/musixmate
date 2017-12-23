package apincer.android.uamp.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by e1022387 on 6/4/2017.
 */

public class UIUtils  {
    public static final int INVALID_COLOR = -1;
    public static int colorAccent = INVALID_COLOR;

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

    public static int getDominantColor(Bitmap bitmap) {
        if (bitmap == null) {
            return Color.TRANSPARENT;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int pixels[] = new int[size];
        //Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_4444, false);
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int color;
        int r = 0;
        int g = 0;
        int b = 0;
        int a;
        int count = 0;
        for (int i = 0; i < pixels.length; i++) {
            color = pixels[i];
            a = Color.alpha(color);
            if (a > 0) {
                r += Color.red(color);
                g += Color.green(color);
                b += Color.blue(color);
                count++;
            }
        }
        r /= count;
        g /= count;
        b /= count;
        r = (r << 16) & 0x00FF0000;
        g = (g << 8) & 0x0000FF00;
        b = b & 0x000000FF;
        color = 0xFF000000 | r | g | b;
        return color;
    }

    /**
     * Calculate the average red, green, blue color values of a bitmap
     *
     * @param bitmap
     *            a {@link android.graphics.Bitmap}
     * @return
     */
    public static int[] getAverageColorRGB(Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        int size = width * height;
        int pixelColor;
        int r, g, b;
        r = g = b = 0;
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                pixelColor = bitmap.getPixel(x, y);
                if (pixelColor == 0) {
                    size--;
                    continue;
                }
                r += Color.red(pixelColor);
                g += Color.green(pixelColor);
                b += Color.blue(pixelColor);
            }
        }
        r /= size;
        g /= size;
        b /= size;
        return new int[] {
                r, g, b
        };
    }

    public static GradientDrawable createGradient(int backgroundColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadii(new float[]{8, 8, 8, 8, 0, 0, 0, 0});
        shape.setColor(backgroundColor);
        shape.setStroke(3, Color.parseColor("#d4e2ff"));
        return shape;
    }

    public static void highlightText(@NonNull TextView textView,
                                     @Nullable String originalText, @Nullable String constraint) {
        int accentColor = fetchAccentColor(textView.getContext(), 1);
        highlightText(textView, originalText, constraint, null, accentColor);
    }

    public static void highlightSearchKeyword(@NonNull TextView textView,
                                     @Nullable String originalText, @Nullable String constraint) {
        highlightText(textView, originalText, constraint, null, Color.RED);
    }

    public static void highlightText(@NonNull TextView textView,
                                     @Nullable String originalText, @Nullable String constraint, String constraint2) {
        int accentColor = fetchAccentColor(textView.getContext(), 1);
        highlightText(textView, originalText, constraint, constraint2, accentColor);
    }

    public static void highlightText(@NonNull TextView textView, @Nullable String originalText,
                                     @Nullable String constraint, String constraint2, @ColorInt int color) {
        if (originalText == null) originalText = "";
        if (constraint == null) constraint = "";
        if (constraint2 == null) constraint2 = "";
        int i = originalText.toLowerCase(Locale.getDefault()).indexOf(constraint.toLowerCase(Locale.getDefault()));
        int ii = originalText.toLowerCase(Locale.getDefault()).indexOf(constraint2.toLowerCase(Locale.getDefault()));
        if (i != -1 || ii != -1) {
            Spannable spanText = Spannable.Factory.getInstance().newSpannable(originalText);
            if(i != -1) {
                spanText.setSpan(new ForegroundColorSpan(color), i, i + constraint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanText.setSpan(new StyleSpan(Typeface.BOLD), i, i + constraint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if(ii != -1) {
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
}
