package apincer.android.uamp.receiver;

import android.content.Context;
import android.content.IntentFilter;

import apincer.android.uamp.MusicService;

/**
 * Created by Administrator on 11/10/17.
 */

public class UAPPReceiver extends AndroidReceiver{

    public UAPPReceiver(MusicService service) {
        super(service);
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.extreamsd.usbaudioplayershared.metachanged"); // API < 14
        iF.addAction("com.extreamsd.usbaudioplayershared.playstatechanged"); // API >= 14

        context.registerReceiver(this, iF);
    }
}
