package apincer.android.uamp;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import apincer.android.uamp.ui.BrowserViewPagerActivity;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

/**
 * Created by e1022387 on 5/29/2017.
 */

public class MusicService extends AccessibilityService {
    private String TAG = LogHelper.makeLogTag(MusicService.class);
    private AudioManager mAudioManager;
    private MediaSession mMediaSession;
    private Context context;
    private static MusicService instance;
    private String currentTitle;
    private String currentArtist;
    private String currentAlbum;

    public static final String LISTENING_INTENT = "apincer.android.uamp.ListeningIntent";

    private static String RADSONE_PACKAGE="com.radsone.dct";
    private static int RADSONE_ARTIST=2131362009;
    private static int RADSONE_TITLE=2131362003;
    private static String TPLAYER_PACKAGE="com.wiseschematics.resoundmethods01";
    private static int TPLAYER_TITLE=2131492879;
    private static int TPLAYER_ARTIST=2131492880;
    private static int TPLAYER_ALBUM=2131492881;
    private static String NEUTRON_PACKAGE="com.neutroncode.mp";
    private static int NEUTRON_TITLE=2131165211;
    private static int NEUTRON_ARTIST=2131165212;
    private static int NEUTRON_ALBUM=2131165213;
/*
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String track = trimText(intent.getStringExtra("track"));
            String source = trimText(intent.getStringExtra("original_source"));
            if(StringUtils.isEmpty(source) && !StringUtils.isEmpty(track)) {
                String artist = trimText(intent.getStringExtra("artist"));
                String album = trimText(intent.getStringExtra("album"));
                sendNowPlayingToActivity(track,artist,album);
                showNotification(track, artist, album);
            }
        }
    }; */

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaSession = new MediaSession(this, TAG);
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        instance = this;
/*
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
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
        iF.addAction("android.intent.action.ANY_ACTION");
        registerReceiver(mReceiver, iF);
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(mReceiver);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(1909);
        mMediaSession.release();
        instance = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pack = String.valueOf(event.getPackageName());

        if(StringUtils.isEmpty(pack)) return;
        if("com.rhmsoft.edit.pro".equalsIgnoreCase(pack)) return;
        if("io.friendy".equalsIgnoreCase(pack)) return;
        if("pl.solidexplorer2".equalsIgnoreCase(pack)) return;
        if("com.freevpn.vpn_master".equalsIgnoreCase(pack)) return;
        if("jp.naver.line.android".equalsIgnoreCase(pack)) return;
        if("com.mokee.yubrowser".equalsIgnoreCase(pack)) return;
        if("com.anod.appwatcher".equalsIgnoreCase(pack)) return;
        if("apincer.android.uamp".equalsIgnoreCase(pack)) return;
        if("android".equalsIgnoreCase(pack)) return;
        if(pack.startsWith("com.android")) return;
        if(pack.startsWith("com.google")) return;
        if(pack.startsWith("com.microsoft")) return;

         if(event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Parcelable data = event.getParcelableData();
            if(data instanceof  Notification) {
                Notification notification = (Notification)data;
                if(NEUTRON_PACKAGE.equalsIgnoreCase(pack)) {
                    processNEUTRON(notification);
                }else if(RADSONE_PACKAGE.equalsIgnoreCase(pack)) {
                    processRADSONE(notification);
                }else if(TPLAYER_PACKAGE.equalsIgnoreCase(pack)) {
                    processTPLAYER(notification);
                }else {
                    try {
                        LogHelper.logToFile(pack + " - " + AccessibilityEvent.eventTypeToString(event.getEventType()), notification.tickerText + ", extras[" + getExtras(notification.extras) + "]");
                        Map<Integer, String> text = extractTextFromContentView(notification);
                        LogHelper.logToFile(pack + " - " + AccessibilityEvent.eventTypeToString(event.getEventType()), "TextView[" + getText(text) + "]");
                    }catch(Exception ex) {
                        LogHelper.e(TAG,ex);
                    }
                }
            }
        }
    }

    public static MusicService getRunningService() {
        return instance;
    }

    private void processRADSONE(Notification notification) {
        Map<Integer, String> text = extractTextFromContentView(notification);
        String track = trimText(text.get(RADSONE_TITLE));
        String artist = trimText(text.get(RADSONE_ARTIST));
        sendNowPlayingToActivity(track,artist,"");
        showNotification(track, artist, "");
        sendNowPlayingToBluetooth(track, artist, "");
    }

    private void processTPLAYER(Notification notification) {
        Map<Integer, String> text = extractTextFromContentView(notification);
        String track = trimText(text.get(TPLAYER_TITLE));
        String artist = trimText(text.get(TPLAYER_ARTIST));
        String album = trimText(text.get(TPLAYER_ALBUM));
        sendNowPlayingToActivity(track,artist,album);
        showNotification(track, artist, album);
        sendNowPlayingToBluetooth(track, artist, album);
    }

    private void processNEUTRON(Notification notification) {
        Map<Integer, String> text = extractTextFromContentView(notification);
        String track = trimText(text.get(NEUTRON_TITLE));
        String artist = trimText(text.get(NEUTRON_ARTIST));
        String album = trimText(text.get(NEUTRON_ALBUM));
        sendNowPlayingToActivity(track,artist,album);
        showNotification(track, artist, album);
        sendNowPlayingToBluetooth(track, artist, album);
    }

    private Map<Integer,String> extractTextFromContentView(Notification notification) {
        RemoteViews views = notification.bigContentView;
        if(views ==null) {
            views = notification.contentView;
        }
        Map<Integer, String> text = new HashMap<Integer, String>();

        if(views==null) {
            return text;
        }

        Class secretClass = views.getClass();

        try {
            Field outerField = secretClass.getDeclaredField("mActions");
            outerField.setAccessible(true);
            ArrayList<Object> actions = (ArrayList<Object>) outerField.get(views);

            for (Object action : actions) {
                Field innerFields[] = action.getClass().getDeclaredFields();
                Field innerFieldsSuper[] = action.getClass().getSuperclass().getDeclaredFields();

                Object value = null;
                Integer type = null;
                Integer viewId = null;
                for (Field field : innerFields) {
                    field.setAccessible(true);
                    if (field.getName().equals("value")) {
                        value = field.get(action);
                    } else if (field.getName().equals("type")) {
                        type = field.getInt(action);
                    }
                }
                for (Field field : innerFieldsSuper) {
                    field.setAccessible(true);
                    if (field.getName().equals("viewId")) {
                        viewId = field.getInt(action);
                    }
                }

                if (value != null && type != null && viewId != null && (type == 9 || type == 10)) {
                    text.put(viewId, value.toString());
                }
            }
        } catch (Exception e) {
            LogHelper.e(TAG,e);
        }
        return text;
    }

    private String getText(Map<Integer,String> text) {
        StringBuilder sb= new StringBuilder();
        for(Integer key : text.keySet()) {
            sb.append(key).append("=").append(text.get(key));
            sb.append(", ");
        }

        return sb.toString();
    }

    private String getExtras(Bundle extras) {
        StringBuilder sb= new StringBuilder();
        for(String key : extras.keySet()) {
            sb.append(key).append("=").append(extras.get(key));
            sb.append(", ");
        }

        return sb.toString();
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.packageNames = new String[] {NEUTRON_PACKAGE,TPLAYER_PACKAGE,RADSONE_PACKAGE};
        setServiceInfo(info);
    }

    public void sendNowPlayingToActivity(String title, String artist, String album) {
        Intent broadcastIntent = new Intent(LISTENING_INTENT);
        currentTitle = trimText(title);
        currentArtist = trimText(artist);
        currentAlbum = trimText(album);
        if(!StringUtils.isEmpty(currentTitle)) {
            broadcastIntent.putExtra("title", currentTitle);
            broadcastIntent.putExtra("artist", currentArtist);
            broadcastIntent.putExtra("album", currentAlbum);
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
        }
    }

    public  void sendNowPlayingToBluetooth(String title, String artist, String album) {
        if (mAudioManager.isBluetoothA2dpOn()) {
            PlaybackState state = new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY)
                    .setState(PlaybackState.STATE_PLAYING, 1, 1.0f, SystemClock.elapsedRealtime())
                    .build();
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                    .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, 1)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, 10)
                    .build();
            mMediaSession.setActive(true);
            mMediaSession.setMetadata(metadata);
            mMediaSession.setPlaybackState(state);
        }

        /*
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        if (mMediaSession == null) {
            mMediaSession = new MediaSessionCompat(this, "PlayerServiceMediaSession");
            mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mMediaSession.setActive(true);
        }

        if (audioManager.isBluetoothA2dpOn()) {
            try {
                String songTitle = trimText(title);
                String artistTitle = trimText(artist);
                // String radioImageUri = getImagesArr().get(0);
                // String songImageUri = getImagesArr().get(1);
                // long duration = getDuration();

                final MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder();

                metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, songTitle);
                metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, songTitle);
                metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistTitle);
                metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, artistTitle);
                // metadata.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, radioImageUri);
                // metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, radioImageUri);
                // metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, songImageUri);
                // metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap);
                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);

                mMediaSession.setMetadata(metadata.build());
*/
                /*
                imageCounter = 0;

                Glide.with(act)
                        .load(Uri.parse(radioImageUri))
                        .asBitmap()
                        .into(new SimpleTarget<Bitmap>(250, 250) {
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
                                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap);

                                imageCounter = imageCounter + 1;

                                if(imageCounter == 2) {
                                    mMediaSession.setMetadata(metadata.build());
                                }
                            }
                        });

                Glide.with(act)
                        .load(Uri.parse(songImageUri))
                        .asBitmap()
                        .into(new SimpleTarget<Bitmap>(250, 250) {
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);

                                imageCounter = imageCounter + 1;

                                if(imageCounter == 2) {
                                    mMediaSession.setMetadata(metadata.build());
                                }
                            }
                        }); */
                /*
            } catch (Exception e) {
                e.printStackTrace();
            } */
       // }
    }

    public void showNotification(String title, String artist, String album) {
        Intent intent = new Intent(this, BrowserViewPagerActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("artist", artist);
        intent.putExtra("album", album);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(BrowserViewPagerActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title+(StringUtils.isEmpty(artist)?"":" / "+artist))
                        .setContentText("Tap here open by MusixMate...")
                        .setAutoCancel(false)
                        .setContentIntent(pendingIntent)
                        .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1909, notification);
    }

    private String trimText(String text) {
        if(text == null) return "";
        if("-".equals(text)) return "";
        if("-/-".equals(text)) return "";
        return text;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public String getCurrentArtist() {
        return currentArtist;
    }

    public void setCurrentArtist(String currentArtist) {
        this.currentArtist = currentArtist;
    }

    public String getCurrentAlbum() {
        return currentAlbum;
    }

    public void setCurrentAlbum(String currentAlbum) {
        this.currentAlbum = currentAlbum;
    }
}
