package apincer.android.menu.buttommenu;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.OrientationHelper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import apincer.android.banner.BannerLayout;
import apincer.android.library.R;
import apincer.android.listener.OnMenuItemClickListener;

public class CarouselMenuDialogue extends DialogFragment implements CarouselMenu {
    private TextView mTitle, mMessage;
    private String title, message;
    private OnMenuItemClickListener onMenuItemClickListener;

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    private int orientation;

    public void setMenuRes(@MenuRes int menuRes) {
        this.menuRes = menuRes;
    }

    private int menuRes = -1;

    public void setOnMenuItemClickListener(OnMenuItemClickListener onOptionsItemSelected) {
        this.onMenuItemClickListener = onOptionsItemSelected;
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_carousel_menu, container, false);
        //getDialog().setTitle("Music Collection");
        getDialog().setCanceledOnTouchOutside(false);
        if (getDialog() != null) {
            // required com.github.searchy2:CustomAlertViewDialogue
            setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BottomSheetDialog);
            if(getDialog().getWindow() != null) {
                getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            }
        }
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

    public void setTitle(String title) {
        this.title = title;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
