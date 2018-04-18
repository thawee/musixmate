package apincer.android.uamp.receiver;

import android.graphics.Bitmap;

public interface ListeningReceiver {
    String PACKAGE_UAPP = "com.extreamsd.usbaudioplayerpro";
    String PREFIX_UAPP = "com.extreamsd.usbaudioplayershared";
    String getPlayerPackage();
    String getPlayerName();
    Bitmap getPlayerIcon();
   // String[] getPackageName();
}
