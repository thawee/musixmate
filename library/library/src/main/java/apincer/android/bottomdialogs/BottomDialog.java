package apincer.android.bottomdialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import apincer.android.library.R;

public class BottomDialog {

    protected final Builder mBuilder;
    protected ImageView vIcon ;
    protected TextView vTitle ;
    protected TextView vContent;
    protected FrameLayout vCustomView;
    protected Button vNegative ;
    protected Button vPositive;
    protected Map<Integer,MenuItem> mMenuItems;
    private int padding;
    private int icon;

    public final Builder getBuilder() {
        return mBuilder;
    }

    public final ImageView getIconImageView() {
        return vIcon;
    }

    public final TextView getTitleTextView() {
        return vTitle;
    }

    public final TextView getContentTextView() {
        return vContent;
    }

    public final Button getNegativeButton() {
        return vNegative;
    }

    public final Button getPositiveButton() {
        return vPositive;
    }

    protected BottomDialog(Builder builder) {
        mBuilder = builder;
        mMenuItems = new HashMap();
        icon = builder.context.getResources().getDimensionPixelSize(R.dimen.dimen_32_dp);
        padding = builder.context.getResources().getDimensionPixelSize(R.dimen.dimen_8_dp);
        mBuilder.bottomDialog = initBottomDialog(builder);
    }

    @UiThread
    public void show() {
        if (mBuilder != null && mBuilder.bottomDialog != null)
            mBuilder.bottomDialog.show();
    }

    @UiThread
    public void dismiss() {
        if (mBuilder != null && mBuilder.bottomDialog != null)
            mBuilder.bottomDialog.dismiss();
    }

    @SuppressLint("RestrictedApi")
    @UiThread
    private Dialog initBottomDialog(final Builder builder) {
        final Dialog bottomDialog = new Dialog(builder.context, R.style.BottomDialogs);
        View view = LayoutInflater.from(builder.context).inflate(R.layout.library_bottom_dialog, null);

        vIcon = (ImageView) view.findViewById(R.id.bottomDialog_icon);
        vTitle = (TextView) view.findViewById(R.id.bottomDialog_title);
        vContent = (TextView) view.findViewById(R.id.bottomDialog_content);
        vCustomView = (FrameLayout) view.findViewById(R.id.bottomDialog_custom_view);
        vNegative = (Button) view.findViewById(R.id.bottomDialog_cancel);
        vPositive = (Button) view.findViewById(R.id.bottomDialog_ok);

        if (builder.icon != null) {
            vIcon.setVisibility(View.VISIBLE);
            vIcon.setImageDrawable(builder.icon);
            if(builder.title_color>0) {
                vIcon.setColorFilter(builder.context.getColor(builder.title_color));
            }
        }

        if (builder.title != null) {
            vTitle.setText(builder.title);
            if(builder.title_color>0) {
                vTitle.setTextColor(builder.context.getColor(builder.title_color));
            }
        }

        if (builder.content != null) {
            vContent.setText(builder.content);
        }else {
            vContent.setVisibility(View.GONE);
        }

        if(builder.menu_items != -1) {
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout container = new LinearLayout(builder.context);
            container.setLayoutParams(params);
            container.setBackgroundColor(Color.WHITE);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(0, 0, 0, 0);
            ScrollView scrollView = new ScrollView(builder.context);
            scrollView.addView(container);
            MenuInflater menuInflater = new SupportMenuInflater(builder.context);
            MenuBuilder menuBuilder = new MenuBuilder(builder.context);
            menuInflater.inflate(builder.menu_items, menuBuilder);
            View.OnClickListener onClickListener = null;
            if(builder.menu_callback!=null) {
                onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        builder.menu_callback.onMenuItemClick(BottomDialog.this, mMenuItems.get(v.getId()));
                    }
                };
            }

            addMenuSeperater(builder,container);
            for (int i = 0; i < menuBuilder.size(); i++) {
                MenuItem menuItem = menuBuilder.getItem(i);
                addMenuItem(builder,onClickListener,container,menuItem);
                addMenuSeperater(builder,container);
                mMenuItems.put(menuItem.getItemId(), menuItem);
            }
            vCustomView.addView(scrollView);
            vCustomView.setPadding(0, 0, 0, 0);
        } else if (builder.customView != null) {
            if (builder.customView.getParent() != null)
                ((ViewGroup) builder.customView.getParent()).removeAllViews();
            vCustomView.addView(builder.customView);
            vCustomView.setPadding(builder.customViewPaddingLeft, builder.customViewPaddingTop, builder.customViewPaddingRight, builder.customViewPaddingBottom);
        }

        if (builder.btn_positive != null) {
            vPositive.setVisibility(View.VISIBLE);
            vPositive.setText(builder.btn_positive);
            vPositive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (builder.btn_positive_callback != null)
                        builder.btn_positive_callback.onClick(BottomDialog.this);
                    if (builder.isAutoDismiss)
                        bottomDialog.dismiss();
                }
            });

            if (builder.btn_colorPositive != 0) {
                vPositive.setTextColor(builder.btn_colorPositive);
            }

            if (builder.btn_colorPositiveBackground == 0) {
                TypedValue v = new TypedValue();
                boolean hasColorPrimary = builder.context.getTheme().resolveAttribute(R.attr.colorPrimary, v, true);
                builder.btn_colorPositiveBackground = !hasColorPrimary ? v.data : ContextCompat.getColor(builder.context, R.color.colorPrimary);
            }

            Drawable buttonBackground = UtilsLibrary.createButtonBackgroundDrawable(builder.context, builder.btn_colorPositiveBackground);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                vPositive.setBackground(buttonBackground);
            } else {
                // noinspection deprecation
                vPositive.setBackgroundDrawable(buttonBackground);
            }
        }

        if (builder.btn_negative != null) {
            vNegative.setVisibility(View.VISIBLE);
            vNegative.setText(builder.btn_negative);
            vNegative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (builder.btn_negative_callback != null)
                        builder.btn_negative_callback.onClick(BottomDialog.this);
                    if (builder.isAutoDismiss)
                        bottomDialog.dismiss();
                }
            });

            if (builder.btn_colorNegative != 0) {
                vNegative.setTextColor(builder.btn_colorNegative);
            }
        }

        bottomDialog.setContentView(view);
        bottomDialog.setCancelable(builder.isCancelable);
        bottomDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bottomDialog.getWindow().setGravity(Gravity.BOTTOM);

        return bottomDialog;
    }

    private void addMenuSeperater(Builder builder, LinearLayout container) {
        int size = builder.context.getResources().getDimensionPixelSize(R.dimen.dimen_1_dp);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size);
        View row = new View(builder.context);
        row.setLayoutParams(params);
        if(builder.title_color>0) {
            row.setBackgroundColor(builder.context.getColor(builder.title_color));
        }else {
            row.setBackgroundColor(Color.GRAY);
        }
        container.addView(row);
    }

    private void addMenuItem(final Builder builder, View.OnClickListener onClickListener, LinearLayout container, MenuItem item) {
        int size = icon + padding + padding;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size);
        TextView row = new TextView(builder.context);
        row.setId(item.getItemId());
        row.setLayoutParams(params);
        row.setMaxLines(1);
        row.setEllipsize(TextUtils.TruncateAt.END);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setText(item.getTitle());
        row.setTypeface(Typeface.DEFAULT);
        row.setTextSize(12);
        if(onClickListener!=null) {
            row.setOnClickListener(onClickListener);
        }
        row.setTextColor(Utils.colorStateListText(builder.context));
        TypedValue typedValue = new TypedValue();
        builder.context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        row.setBackgroundResource(typedValue.resourceId);
        if (item.getIcon() != null) {
            row.setCompoundDrawablesWithIntrinsicBounds(icon(builder,item.getIcon()), null, null, null);
            row.setCompoundDrawablePadding(padding);
            row.setPadding(padding, padding, padding, padding);
        } else {
            row.setPadding(size, padding, padding, padding);
        }
        container.addView(row);
    }

    /**
     * @param drawable Drawable from menu item
     * @return Drawable resized 32dp x 32dp and colored with color textColorSecondary
     */
    private Drawable icon(Builder builder, Drawable drawable) {
        if (drawable != null) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Drawable resizeIcon = new BitmapDrawable(builder.context.getResources(), Bitmap.createScaledBitmap(bitmap, icon, icon, true));
            Drawable.ConstantState state = resizeIcon.getConstantState();
            resizeIcon = DrawableCompat.wrap(state == null ? resizeIcon : state.newDrawable()).mutate();
            if(builder.title_color>0) {
                DrawableCompat.setTint(resizeIcon, builder.context.getColor(builder.title_color));
            }else {
                DrawableCompat.setTintList(resizeIcon, Utils.colorStateListIcon(builder.context));
            }
            return resizeIcon;
        }
        return null;
    }

    public interface OnMenuItemClick {
        boolean onMenuItemClick(BottomDialog dialog, MenuItem item);
    }

    public static class Builder {
        protected Context context;

        // Bottom Dialog
        protected Dialog bottomDialog;

        // Icon, Title and Content
        protected Drawable icon;
        protected CharSequence title, content;

        protected int menu_items = -1;
        protected OnMenuItemClick menu_callback;

        // Buttons
        protected CharSequence btn_negative, btn_positive;
        protected ButtonCallback btn_negative_callback, btn_positive_callback;
        protected boolean isAutoDismiss;

        // Button text colors
        protected int btn_colorNegative, btn_colorPositive;

        // Button background colors
        protected int btn_colorPositiveBackground;

        // Text text colors
        protected int title_color;

        // Custom View
        protected View customView;
        protected int customViewPaddingLeft, customViewPaddingTop, customViewPaddingRight, customViewPaddingBottom;

        // Other options
        protected boolean isCancelable;

        public Builder(@NonNull Context context) {
            this.context = context;
            this.isCancelable = true;
            this.isAutoDismiss = true;
        }

        public Builder setTitle(@StringRes int titleRes) {
            setTitle(this.context.getString(titleRes));
            return this;
        }

        public Builder setTitleColor(@ColorRes int colorRes) {
            title_color = colorRes;
            return this;
        }

        public Builder setTitle(@NonNull CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setContent(@StringRes int contentRes) {
            setContent(this.context.getString(contentRes));
            return this;
        }

        public Builder setContent(@NonNull CharSequence content) {
            this.content = content;
            return this;
        }

        public Builder setIcon(@NonNull Drawable icon) {
            this.icon = icon;
            return this;
        }

        public Builder setIcon(@DrawableRes int iconRes) {
            this.icon = ResourcesCompat.getDrawable(context.getResources(), iconRes, null);
            return this;
        }

        public Builder setPositiveBackgroundColorResource(@ColorRes int buttonColorRes) {
            this.btn_colorPositiveBackground = ResourcesCompat.getColor(context.getResources(), buttonColorRes, null);
            return this;
        }

        public Builder setPositiveBackgroundColor(int color) {
            this.btn_colorPositiveBackground = color;
            return this;
        }

        public Builder setPositiveTextColorResource(@ColorRes int textColorRes) {
            this.btn_colorPositive = ResourcesCompat.getColor(context.getResources(), textColorRes, null);
            return this;
        }

        public Builder setPositiveTextColor(int color) {
            this.btn_colorPositive = color;
            return this;
        }

        public Builder setPositiveText(@StringRes int buttonTextRes) {
            setPositiveText(this.context.getString(buttonTextRes));
            return this;
        }

        public Builder setPositiveText(@NonNull CharSequence buttonText) {
            this.btn_positive = buttonText;
            return this;
        }

        public Builder onPositive(@NonNull ButtonCallback buttonCallback) {
            this.btn_positive_callback = buttonCallback;
            return this;
        }

        public Builder setNegativeTextColorResource(@ColorRes int textColorRes) {
            this.btn_colorNegative = ResourcesCompat.getColor(context.getResources(), textColorRes, null);
            return this;
        }

        public Builder setNegativeTextColor(int color) {
            this.btn_colorNegative = color;
            return this;
        }

        public Builder setNegativeText(@StringRes int buttonTextRes) {
            setNegativeText(this.context.getString(buttonTextRes));
            return this;
        }

        public Builder setNegativeText(@NonNull CharSequence buttonText) {
            this.btn_negative = buttonText;
            return this;
        }

        public Builder onNegative(@NonNull ButtonCallback buttonCallback) {
            this.btn_negative_callback = buttonCallback;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.isCancelable = cancelable;
            return this;
        }

        public Builder autoDismiss(boolean autodismiss) {
            this.isAutoDismiss = autodismiss;
            return this;
        }

        public Builder setCustomView(View customView) {
            this.customView = customView;
            this.customViewPaddingLeft = 0;
            this.customViewPaddingRight = 0;
            this.customViewPaddingTop = 0;
            this.customViewPaddingBottom = 0;
            return this;
        }

        public Builder setCustomView(View customView, int left, int top, int right, int bottom) {
            this.customView = customView;
            this.customViewPaddingLeft = UtilsLibrary.dpToPixels(context, left);
            this.customViewPaddingRight = UtilsLibrary.dpToPixels(context, right);
            this.customViewPaddingTop = UtilsLibrary.dpToPixels(context, top);
            this.customViewPaddingBottom = UtilsLibrary.dpToPixels(context, bottom);
            return this;
        }

        public Builder setMenu(int menu) {
            this.menu_items = menu;
            return this;
        }

        public Builder onMenuItemClick(@NonNull OnMenuItemClick buttonCallback) {
            this.menu_callback = buttonCallback;
            return this;
        }

        @UiThread
        public BottomDialog build() {
            return new BottomDialog(this);
        }

        @UiThread
        public BottomDialog show() {
            BottomDialog bottomDialog = build();
            bottomDialog.show();
            return bottomDialog;
        }

    }

    public interface ButtonCallback {

        void onClick(@NonNull BottomDialog dialog);
    }

}
