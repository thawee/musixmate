package apincer.android.uamp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Created by e1022387 on 5/29/2017.
 */

public class MusicService extends Service {

    public static final String LISTENING_INTENT = "apincer.android.uamp.ListeningIntent";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent broadcastIntent = new Intent(LISTENING_INTENT);
            broadcastIntent.putExtra("title",intent.getStringExtra("track"));
            broadcastIntent.putExtra("artist",intent.getStringExtra("artist"));
            broadcastIntent.putExtra("album",intent.getStringExtra("album"));
            sendBroadcast(broadcastIntent);
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.htc.music.metachanged");
        iF.addAction("fm.last.android.metachanged");
        iF.addAction("com.sec.android.app.music.metachanged");
        iF.addAction("com.nullsoft.winamp.metachanged");
        iF.addAction("com.amazon.mp3.metachanged");
        iF.addAction("com.miui.player.metachanged");
        iF.addAction("com.real.IMP.metachanged");
        iF.addAction("com.sonyericsson.music.metachanged");
        iF.addAction("com.rdio.android.metachanged");
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.andrew.apollo.metachanged");
        iF.addAction("gonemad.dashclock.music.metachanged");
        iF.addAction("com.piratemedia.musicmod.metachanged");
        iF.addAction("com.tbig.playerpro.metachanged");
        iF.addAction("org.abrantix.rockon.rockonnggl.metachanged");
        iF.addAction("com.maxmpz.audioplayer.metachanged");
        iF.addAction("com.doubleTwist.androidPlayer.metachanged");
        registerReceiver(mReceiver, iF);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Wont be called as service is not bound
        return null;
    }
}
