package apincer.android.uamp.service;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface ListeningReceiver {
    String PACKAGE_NEUTRON = "com.neutroncode.mp";
    String PACKAGE_UAPP = "com.extreamsd.usbaudioplayerpro";
    String PACKAGE_GOOGLE_MUSIC_PLAYER = "com.google.android.music";
    String PACKAGE_FOOBAR2000="com.foobar2000.foobar2000";
    String PACKAGE_POWERAMP = "com.maxmpz.audioplayer";
    String PREFIX_UAPP = "com.extreamsd.usbaudioplayershared";
    String PREFIX_VLC = "org.videolan.vlc";
    String INTENT_KEY_PACKAGE = "package";
    String INTENT_KEY_PLAYER = "player";
    String INTENT_KEY_SCROBBLING_SOURCE = "scrobbling_source";
    String INTENT_KEY_GMUSIC_VALIDATE = "currentContainerName";

    String PLAYER_NAME_FOOBAR2000 = "foobar2000";

    String getPlayerPackage();
    String getPlayerName();
    Bitmap getPlayerIconBitmap();
    Drawable getPlayerIconDrawable();
}
