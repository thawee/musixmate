package apincer.android.menu.buttommenu;

import android.support.annotation.MenuRes;
import android.support.v4.app.FragmentManager;

import apincer.android.listener.OnMenuItemClickListener;

public interface CarouselMenu {
    void setOnMenuItemClickListener(OnMenuItemClickListener onOptionsItemSelected);
    void setTitle(String title);
    void setMessage(String message);
    void dismiss();
    void show(FragmentManager supportFragmentManager, String dialog);

    void setMenuRes(@MenuRes int menu_music_collection);

    void setOrientation(int i);
}
