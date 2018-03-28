package apincer.android.uamp.receiver;

import android.content.Context;
import android.content.IntentFilter;

import apincer.android.uamp.MusicService;

/**
 * Created by Administrator on 11/10/17.
 */

public class VIVOReceiver extends AndroidReceiver{

    public VIVOReceiver(MusicService service) {
        super(service);
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.bbkmusic.metachanged");

        context.registerReceiver(this, iF);
    }
}
