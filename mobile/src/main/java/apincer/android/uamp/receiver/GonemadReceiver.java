package apincer.android.uamp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.utils.StringUtils;

/**
 * Created by Administrator on 11/10/17.
 */

public class GonemadReceiver extends AndroidReceiver{

    public GonemadReceiver(MusicService service) {
        super(service);
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        iF.addAction("gonbemad.dashclock.music.metachanged");
        iF.addAction("gonbemad.dashclock.music.playstatechanged");
        // iF.addAction("gonbemad.dashclock.music.playbackcomplete");

        context.registerReceiver(this, iF);
    }
}
