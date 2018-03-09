package apincer.android.uamp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import apincer.android.uamp.MusicService;

/**
 * Supporting android standard broadcast
 *  - google music
 *  - foobar200
 *  - neutron player
 * Created by Administrator on 11/10/17.
 */

public class AndroidReceiver extends BroadcastReceiver {
    private MusicService service;
    protected String title;
    protected String artist;
    protected String album;

    public AndroidReceiver(MusicService service) {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(isStopPlaying(intent)) {
            service.finishNotification();
        }else {
            extractTitle(context, intent);
            displayNotification();
        }
    }

    private boolean isStopPlaying(Intent intent) {
        //if(intent.getAction().contains("playstatechanged")) {
        //    return "true".equalsIgnoreCase(intent.getStringExtra("playing", "false"));
        //}
        return false;
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        // iF.addAction("com.android.music.playbackcomplete");
        // iF.addAction("com.android.music.queuechanged");

        context.registerReceiver(this, iF);
    }

    protected void extractTitle(Context context, Intent intent) {
        String action = intent.getAction();
       // if (action.equals("com.android.music.metachanged")) {
            artist = intent.getStringExtra("artist");
            album = intent.getStringExtra("album");
            title = intent.getStringExtra("track");
       // }
    }

    protected final void displayNotification() {
        service.showNotification(title, artist,album);
    }
}
