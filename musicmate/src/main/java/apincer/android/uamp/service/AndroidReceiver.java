package apincer.android.uamp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.List;

import apincer.android.uamp.utils.BitmapHelper;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

/**
 * Supporting android standard broadcast
 *  - Google Music
 *  - Foobar200
 *  - Neutron Player
 *  - VIVO Music Player, cannot get player details
 *
 *  NOT supported player - not send any broadcast at all
 *      - HFPlayer (com.onkyo.jp.musicplayer)
 *      - Radsone DCT
 *      - Hiby Music
 */
public class AndroidReceiver extends BroadcastReceiver implements ListeningReceiver {

    private MusicListeningService service;
    protected String title;
    protected String artist;
    protected String album;
    protected String player;
    protected String playerPackage;
    protected Bitmap iconBitmap = null;
    protected Drawable iconDrawable = null;
    public static String DEAFULT_PLAYER_NAME = "UNKNOWN Player";

    public AndroidReceiver(MusicListeningService service) {
        this.service = service;
        player = DEAFULT_PLAYER_NAME;
        playerPackage = "unknown";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //KEEP for DEBUG
            Bundle extras = intent.getExtras();
            String string = "";
            for (String key : extras.keySet()) {
                string += " " + key + " => " + extras.get(key) + ";";
            }

            boolean paused = intent.getBooleanExtra("paused", false);
            if(!paused) {
                try {
                    extractPlayer(context, intent);
                    extractTitle(context, intent);
                    displayNotification();
                    service.setListeningReceiver(this);
                }catch (Exception ex) {
                    LogHelper.e("onReceive", ex);
                }
            }
      //  MediaSessionManager mMediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);

       //     String a = "";
    }

    protected void extractPlayer(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.startsWith(PREFIX_UAPP)) {
            playerPackage = PACKAGE_UAPP;
            setPlayer(context, playerPackage,null);
        }else {
            playerPackage = intent.getStringExtra(INTENT_KEY_PACKAGE);
            String playerName = intent.getStringExtra(INTENT_KEY_PLAYER);
            if(PLAYER_NAME_FOOBAR2000.equalsIgnoreCase(playerName)) {
                playerPackage = PACKAGE_FOOBAR2000;
            }else if(intent.getStringExtra(INTENT_KEY_GMUSIC_VALIDATE)!=null) {
                playerPackage = PACKAGE_GOOGLE_MUSIC_PLAYER;
            }else if(PACKAGE_POWERAMP.equals(intent.getStringExtra(INTENT_KEY_SCROBBLING_SOURCE))) {
                playerPackage = PACKAGE_POWERAMP;
            }

            if(playerPackage==null) {
                playerPackage = getDefaultPlayerPackage(context);
            }
            setPlayer(context, playerPackage,playerName);
        }
    }

    protected String getDefaultPlayerPackage(Context context) {
        try {
            Intent musicPlayerIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(musicPlayerIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if(list!=null && list.size()>0) {
                // get last in the list
                return list.get(list.size()-1).activityInfo.packageName;
            }
        }catch (Exception ex) {}
        return null;
    }

    protected void setPlayer(Context context, String packageName, String playerName) {
        ApplicationInfo ai = service.getApplicationInfo(packageName);
        if(ai!=null) {
                iconDrawable = context.getPackageManager().getApplicationIcon(ai);
                if (iconDrawable != null) {
                    iconBitmap = BitmapHelper.drawableToBitmap(iconDrawable);
                }else {
                    iconBitmap = null;
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
    public Bitmap getPlayerIconBitmap() {
        return iconBitmap;
    }

    @Override
    public Drawable getPlayerIconDrawable() {
        return iconDrawable;
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();
        // Android
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
        iF.addAction("com.android.music.updateprogress");

        // USB Audio Player Pro
        iF.addAction("com.extreamsd.usbaudioplayershared.metachanged"); // API < 14
        //iF.addAction("com.extreamsd.usbaudioplayershared.playstatechanged"); // API >= 14, no need

        // Sony
        iF.addAction("com.sonyericson.music.metachanged");

        //VIVO Music
         iF.addAction("com.android.bbkmusic.metachanged");

         //MIUI
        iF.addAction("com.miui.player.metachanged");

        //HTC
        iF.addAction("com.htc.music.metachanged");
        iF.addAction("com.htc.music.playstatechanged");

        // GoneMAD
        iF.addAction("gonbemad.dashclock.music.metachanged");
        iF.addAction("gonbemad.dashclock.music.playstatechanged");

        //JR
        iF.addAction("com.jrstudio.music.metachanged");
        iF.addAction("com.jrstudio.music.playstatechanged");

        // SAMSUNG
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.samsung.music.metachanged");
        iF.addAction("com.samsung.sec.metachanged");
        iF.addAction("com.samsung.sec.android.metachanged");
        iF.addAction("com.samsung.MusicPlayer.metachanged");
        iF.addAction("com.sec.android.music.state.META_CHANGED");

        // hiby
       // iF.addAction("com.hiby.music");

        //Audio
       // iF.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
       // iF.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
      //  iF.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);

        context.registerReceiver(this, iF);
    }

    protected void extractTitle(Context context, Intent intent) {
       // String action = intent.getAction();
       artist = intent.getStringExtra("artist");
       album = intent.getStringExtra("album");
       title = intent.getStringExtra("track");
       if(PACKAGE_POWERAMP.equals(playerPackage)) {
           title = StringUtils.trimToEmpty(title.replace( " - "+artist+" - "+ album, ""));
       }
    }

    protected final void displayNotification() {
        service.setListeningSong(title, artist,album);
    }
}
