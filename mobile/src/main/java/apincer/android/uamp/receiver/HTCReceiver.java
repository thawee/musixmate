package apincer.android.uamp.receiver;

import android.content.Context;
import android.content.IntentFilter;

import apincer.android.uamp.MusicService;

/**
 * Created by Administrator on 11/10/17.
 */

public class HTCReceiver extends AndroidReceiver{

    public HTCReceiver(MusicService service) {
        super(service);
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.htc.music.metachanged");
        iF.addAction("com.htc.music.playstatechanged");
        // iF.addAction("com.htc.music.playbackcomplete");

        context.registerReceiver(this, iF);
    }
}
