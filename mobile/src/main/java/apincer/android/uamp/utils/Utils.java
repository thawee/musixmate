package apincer.android.uamp.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by e1022387 on 6/4/2017.
 */

public class Utils  {
    public static final int INVALID_COLOR = -1;
    public static int colorAccent = INVALID_COLOR;

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

    public static void highlightText(@NonNull TextView textView,
                                     @Nullable String originalText, @Nullable String constraint) {
        int accentColor = fetchAccentColor(textView.getContext(), 1);
        highlightText(textView, originalText, constraint, null, accentColor);
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
}
