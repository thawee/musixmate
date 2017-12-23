package apincer.android.uamp.receiver;

import android.content.Context;
import android.content.IntentFilter;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.receiver.AndroidReceiver;

/**
 * Created by Administrator on 11/10/17.
 */

public class SamsungReceiver extends AndroidReceiver{

    public SamsungReceiver(MusicService service) {
        super(service);
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.samsung.music.metachanged");
        iF.addAction("com.samsung.sec.metachanged");
        iF.addAction("com.samsung.sec.android.metachanged");
        iF.addAction("com.samsung.MusicPlayer.metachanged");
        iF.addAction("com.sec.android.music.state.META_CHANGED");

        context.registerReceiver(this, iF);
    }
}
