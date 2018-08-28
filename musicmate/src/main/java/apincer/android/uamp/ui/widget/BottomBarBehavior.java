package apincer.android.uamp.ui.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class BottomBarBehavior extends CoordinatorLayout.Behavior<LinearLayout> {
    public BottomBarBehavior(Context context, AttributeSet attrs) {}

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, LinearLayout child, View dependency) {
        return ( dependency instanceof Snackbar.SnackbarLayout ) ||
                ( dependency instanceof DrawerLayout);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, LinearLayout child, View dependency) {
        if( dependency instanceof Snackbar.SnackbarLayout ) {
            float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
            child.setTranslationY(translationY);
        }
        return true;
    }
}
