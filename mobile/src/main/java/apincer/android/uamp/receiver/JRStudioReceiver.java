package apincer.android.uamp.receiver;

import android.content.Context;
import android.content.IntentFilter;

import apincer.android.uamp.MusicService;

/**
 * Created by Administrator on 11/10/17.
 */

public class JRStudioReceiver extends AndroidReceiver{

    public JRStudioReceiver(MusicService service) {
        super(service);
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.jrstudio.music.metachanged");
        iF.addAction("com.jrstudio.music.playstatechanged");
        // iF.addAction("com.jrstudio.music.playbackcomplete");

        context.registerReceiver(this, iF);
    }
}
