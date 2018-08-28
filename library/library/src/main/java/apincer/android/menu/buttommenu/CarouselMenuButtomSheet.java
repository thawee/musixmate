package apincer.android.menu.buttommenu;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.OrientationHelper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import apincer.android.banner.BannerLayout;
import apincer.android.library.R;
import apincer.android.listener.OnMenuItemClickListener;

public class CarouselMenuButtomSheet extends BottomSheetDialogFragment implements CarouselMenu {
    private TextView mTitle, mMessage;
    private String title, message;
    private int buttomMargin = 0;

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    private int orientation;

    public void setMenuRes(@MenuRes int menuRes) {
        this.menuRes = menuRes;
    }

    private int menuRes = -1;
    private OnMenuItemClickListener onMenuItemClickListener;

    public void setOnMenuItemClickListener(OnMenuItemClickListener onOptionsItemSelected) {
        this.onMenuItemClickListener = onOptionsItemSelected;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TransparentBottomSheetStyle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_carousel_menu, container, false);
        if (getDialog() != null) {
            if(getDialog().getWindow() != null) {
                getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                getDialog().getWindow().getDecorView().setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        getDialog().setCanceledOnTouchOutside(false);
        BannerLayout banner = view.findViewById(R.id.recycler);
        if(menuRes!=-1) {
            MenuItemRecyclerAdapter adapter = new MenuItemRecyclerAdapter(getContext(), menuRes,orientation);
            adapter.setOnMenuItemClickListener(onMenuItemClickListener);
            banner.setAdapter(adapter);
        }
        if(orientation == 0) {
            banner.setOrientation(OrientationHelper.HORIZONTAL);
        }else {
            banner.setOrientation(OrientationHelper.VERTICAL);
            banner.setShowIndicator(false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Common elements
        mTitle = view.findViewById(R.id.title);
        mMessage = view.findViewById(R.id.message);
        if(title==null) {
            mTitle.setVisibility(View.GONE);
        }else {
            mTitle.setText(title);
        }
        mMessage.setVisibility(View.GONE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.windowAnimations = R.style.BottomSheetDialogAnimation;
        wlp.gravity = Gravity.CENTER;
        window.setAttributes(wlp);

        return dialog;
    }

/*
    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        if (dialog != null) {
            // required com.github.searchy2:CustomAlertViewDialogue
            setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialog);
            if(dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            }
        }
    }
*/
    public void setTitle(String title) {
        this.title = title;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public void setDialogBorder(Dialog dialog) {
        FrameLayout bottomSheet = (FrameLayout) dialog.getWindow().findViewById(android.support.design.R.id.design_bottom_sheet);
        bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
        setMargins(bottomSheet, 10, 0, 10, 20);
    }

    private void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }
}