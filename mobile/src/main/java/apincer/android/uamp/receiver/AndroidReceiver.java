package apincer.android.uamp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.utils.BitmapHelper;
import apincer.android.uamp.utils.StringUtils;

/**
 * Supporting android standard broadcast
 *  - Google Music
 *  - Foobar200
 *  - Neutron Player
 *  - VIVO Music Player
 * Created by Administrator on 11/10/17.
 */

public class AndroidReceiver extends BroadcastReceiver implements ListeningReceiver {
    private MusicService service;
    protected String title;
    protected String artist;
    protected String album;
    protected String player;
    protected String playerPackage;
    protected Bitmap iconBitmap = null;
    public static String DEAFULT_PLAYER_NAME = "UNKNOWN Player";
    //public static Bitmap DEAFULT_PLAYER_ICON = null;

    public AndroidReceiver(MusicService service) {
        this.service = service;
        player = DEAFULT_PLAYER_NAME;
        playerPackage = "unknown";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(isStopPlaying(intent)) {
            service.finishNotification();
        }else {
          //  Bundle extras = intent.getExtras();
          //  String string = "";
          //  for (String key : extras.keySet()) {
          //      string += " " + key + " => " + extras.get(key) + ";";
          //  }
            extractTitle(context, intent);
            extractPlayer(context, intent);
            displayNotification();
        }
        service.setListeningReceiver(this);
    }

    protected void extractPlayer(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.startsWith(PREFIX_UAPP)) {
            playerPackage = PACKAGE_UAPP;
            setPlayer(context, playerPackage,null);
        }else {
            Bundle extras = intent.getExtras();
            String string = "";
            for (String key : extras.keySet()) {
                string += " " + key + " => " + extras.get(key) + ";";
            }

            playerPackage = intent.getStringExtra("package");
            String playerName = intent.getStringExtra("player");
            setPlayer(context, playerPackage,playerName);
        }
    }

    protected void setPlayer(Context context, String packageName, String playerName) {
        ApplicationInfo ai = service.getApplicationInfo(packageName);
        if(ai!=null) {
                Drawable d = context.getPackageManager().getApplicationIcon(ai);
                if (d != null) {
                    iconBitmap = BitmapHelper.drawableToBitmap(d);
                }
                if(playerName==null) {
                    playerName = String.valueOf(context.getPackageManager().getApplicationLabel(ai));
                }
        }
        player = playerName==null?DEAFULT_PLAYER_NAME:playerName;
    }

    @Override
    public String getPlayerPackage() {
        return playerPackage;
    }

    @Override
    public String getPlayerName(){
        return player;
    }

    @Override
    public Bitmap getPlayerIcon() {
        return iconBitmap;
    }

    private boolean isStopPlaying(Intent intent) {
        //if(intent.getAction().contains("playstatechanged")) {
        //    return "true".equalsIgnoreCase(intent.getStringExtra("playing", "false"));
        //}
        return false;
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        // Android
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        // USB Audio Player Pro
        iF.addAction("com.extreamsd.usbaudioplayershared.metachanged"); // API < 14
        iF.addAction("com.extreamsd.usbaudioplayershared.playstatechanged"); // API >= 14

        iF.addAction("com.sonyericson.music.metachanged");

        //VIVO Music
         iF.addAction("com.android.bbkmusic.metachanged");

        iF.addAction("com.miui.player.metachanged");

        iF.addAction("com.htc.music.metachanged");
        iF.addAction("com.htc.music.playstatechanged");

        iF.addAction("gonbemad.dashclock.music.metachanged");
        iF.addAction("gonbemad.dashclock.music.playstatechanged");

        iF.addAction("com.jrstudio.music.metachanged");
        iF.addAction("com.jrstudio.music.playstatechanged");

        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.samsung.music.metachanged");
        iF.addAction("com.samsung.sec.metachanged");
        iF.addAction("com.samsung.sec.android.metachanged");
        iF.addAction("com.samsung.MusicPlayer.metachanged");
        iF.addAction("com.sec.android.music.state.META_CHANGED");

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
        service.setListeningSong(title, artist,album);
    }

}
