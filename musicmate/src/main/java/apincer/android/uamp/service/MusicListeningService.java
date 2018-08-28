package apincer.android.uamp.service;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

import apincer.android.uamp.Constants;
import apincer.android.uamp.R;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.ui.MediaBrowserActivity;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.UIUtils;

/**
 * Created by e1022387 on 5/29/2017.
 */

public class MusicListeningService extends Service { //extends AccessibilityService
    public static final String ACTION = "com.apincer.uamp.MusicListeningService";
//    private List<String> readablePackages = new ArrayList<>();
    public Bitmap DEFAULT_PLAYER_ICON;

    // android 5 SD card permissions
    public static final int REQUEST_CODE_SD_PERMISSION = 1010;
    public static final int REQUEST_CODE_EDIT_MEDIA_TAG = 2020;

    public static final String FLAG_SHOW_LISTENING = "__FLAG_SHOW_LISTENING";

    public static String[] PERMISSIONS_ALL = {Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //public static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final String CHANNEL_ID = "music_mate_now_listening";
    private static final  int NOTIFICATION_ID = 19099;
    private String TAG = LogHelper.makeLogTag(MusicListeningService.class);
    private Context context;
    private static MusicListeningService instance;
    private MediaItem listeningSong;
    private static List<ListeningReceiver> receivers = new ArrayList<>();

    private NotificationCompat.Builder builder;
    private NotificationChannel mChannel;
    private ListeningReceiver listeningReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        DEFAULT_PLAYER_ICON = BitmapFactory.decodeResource(getResources(), R.drawable.ic_broken_image_black_24dp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        registerReceiver(new AndroidReceiver(this));
        registerReceiver(new NotificationReader(this));

        instance = this;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager
                mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "Music Mate Listening";
        // The user-visible description of the channel.
        String description = "Song currently listen...";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
        mChannel.setDescription(description);
        mChannel.setShowBadge(true);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(builder!=null) {
            builder.setOngoing(false);
            displayNotification(context, builder.build());
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
        unregisterReceivers();
        instance = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
/*
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pack = String.valueOf(event.getPackageName());

        if(StringUtils.isEmpty(pack)) return;

        if(event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && receivers.size()>0) {
            Parcelable data = event.getParcelableData();
            if(data instanceof  Notification) {
                Notification notification = (Notification)data;
                for (ListeningReceiver reader: receivers) {
                    if(reader instanceof NotificationReader) {
                        NotificationReader rd = (NotificationReader) reader;
                        if(rd.isPackageAccepted(pack)) {
                            rd.process(notification, pack);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {

    } */

    private void registerReceiver(ListeningReceiver receiver) {
        if(receiver instanceof AndroidReceiver) {
            ((AndroidReceiver)receiver).register(context);
        }
       // if(receiver instanceof NotificationReader) {
       //     readablePackages.addAll(((NotificationReader)receiver).getPackages());
       // }
        receivers.add(receiver);
    }

    private void unregisterReceivers() {
        if(!receivers.isEmpty()) {
            for (ListeningReceiver receiver : receivers) {
                try {
                    if(receiver instanceof BroadcastReceiver) {
                        unregisterReceiver((BroadcastReceiver)receiver);
                    }
                } catch (Exception ex) {
                    LogHelper.e(TAG, ex);
                }
            }
            receivers.clear();
        }
    }

    public static MusicListeningService getInstance() {
        return instance;
    }

    /*
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        if(readablePackages.size()>0) {
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.flags = AccessibilityServiceInfo.DEFAULT;
            info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.packageNames = (String[]) readablePackages.toArray(new String[0]); // keySet().toArray(new String[0]);
            setServiceInfo(info);
        }
    } */

    public void setListeningSong(String currentTitle, String currentArtist, String currentAlbum) {
        // FIXME move to RXAndroid
        currentTitle = StringUtils.trimTitle(currentTitle);
        currentArtist = StringUtils.trimTitle(currentArtist);
        currentAlbum = StringUtils.trimTitle(currentAlbum);
        searchListeningMediaItem(currentTitle, currentArtist, currentAlbum);

        if(StringUtils.isEmpty(currentTitle) && StringUtils.isEmpty(currentArtist) && StringUtils.isEmpty(currentAlbum)) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        // display song info from music library
        if(listeningSong!=null) {
           sendBroadcast("playing", listeningSong.getPath());
        }

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean showNotification = prefs.getBoolean("preference_notification",false);
        if(showNotification) {
            Notification notification = createNotification(listeningSong, currentTitle,currentArtist,currentAlbum);
            displayNotification(context, notification);
        }
    }

    private PendingIntent getPendingIntent(MediaItem item) {
        Intent intent = new Intent(this, MediaBrowserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (item!=null) {
           // addMediaItem(item);
            intent.putExtra(FLAG_SHOW_LISTENING, "yes");
            intent.putExtra(Constants.KEY_MEDIA_PATH, item.getPath());
        }
        return PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification createNotification(MediaItem item, String currentTitle, String currentArtist, String currentAlbum) {
        builder = createCustomNotificationBuilder(context);
        int layoutId = R.layout.view_notification;
        try {
            if(item==null) {
                layoutId = R.layout.view_notification_missing;

            }
            RemoteViews contentView = new RemoteViews(getPackageName(), layoutId);
            builder.setContent(contentView);
            int panelLabelColor = getColor(R.color.now_playing);
            int panelColor = getColor(R.color.grey200);
            int textColor = Color.WHITE;

            // get icon from package
            Bitmap ico = getPlayerIconBitmap();
            ico = ico==null?DEFAULT_PLAYER_ICON:ico;
            contentView.setImageViewBitmap(R.id.notification_coverart, ico);
            Palette paletteIco = Palette.from(ico).generate();
            panelColor = paletteIco.getDominantColor(panelColor);
            panelLabelColor = paletteIco.getMutedColor(panelLabelColor);

            if (item != null) {
                Bitmap bmp = MediaItemProvider.getInstance().getSmallArtwork(item);
                if (bmp != null) {
                    contentView.setImageViewBitmap(R.id.notification_coverart, bmp);
                    Palette palette = Palette.from(bmp).generate();
                    panelColor = palette.getDominantColor(panelColor);
                    panelLabelColor = palette.getMutedColor(panelLabelColor);
                }
                contentView.setTextViewText(R.id.notification_title, item.getTitle());
                contentView.setTextViewText(R.id.notification_artist, item.getSubtitle());
                contentView.setTextViewText(R.id.notification_bitsample, item.getMetadata().getAudioBitCountAndSamplingrate());
                contentView.setTextViewText(R.id.notification_extension, item.getMetadata().getAudioFormat());
                contentView.setTextViewText(R.id.notification_filesize, item.getMetadata().getMediaSize());
                contentView.setTextViewText(R.id.notification_duration, item.getMetadata().getAudioDurationAsString());

                int qualityColor = getQualityColor(item);
                contentView.setInt(R.id.notification_format_bgcolor, "setColorFilter", qualityColor);
                contentView.setInt(R.id.notification_bitsample_bgcolor, "setColorFilter", qualityColor);
               // contentView.setInt(R.id.notification_filesize_bgcolor, "setColorFilter", qualityColor);
               // contentView.setInt(R.id.notification_duration_bgcolor, "setColorFilter", qualityColor);
            } else {
                contentView.setTextViewText(R.id.notification_title, currentTitle);
                contentView.setTextViewText(R.id.notification_artist, getSubtitle(currentAlbum, currentArtist));
                contentView.setTextViewText(R.id.notification_player, getPlayerName());
                contentView.setTextColor(R.id.notification_player, textColor);
            }

            contentView.setTextColor(R.id.notification_title, textColor);
            contentView.setTextColor(R.id.notification_artist, textColor);

            Bitmap background = UIUtils.buildGradientBitmap(getApplicationContext(),panelLabelColor, 1024, 60, 4,4,4,4);
           // background.

            contentView.setImageViewBitmap(R.id.notification_bgcolor, UIUtils.buildGradientBitmap(getApplicationContext(), panelColor, 1024, 60, 4,4,10,10));
            contentView.setImageViewBitmap(R.id.notification_text_bgcolor, background);

            builder.setContentIntent(getPendingIntent(item));
            builder.setColorized(true);
        }catch(Exception ex) {
            LogHelper.e(TAG, ex);
        }

        return builder.build();
    }

    private int getQualityColor(MediaItem item) {
        int qualityColor = getColor(R.color.quality_normal);
        if(item!=null) {
            int quality = item.getMetadata().getAudioEncodingQuality();
            if (quality == MediaItem.MEDIA_QUALITY_LOW) {
                qualityColor = getColor(R.color.quality_low);
            }else if (quality == MediaItem.MEDIA_QUALITY_AVERAGE) {
                qualityColor = getColor(R.color.quality_normal);
            }else if (quality == MediaItem.MEDIA_QUALITY_GOOD) {
                qualityColor = getColor(R.color.quality_good);
            }else if (quality == MediaItem.MEDIA_QUALITY_HIGH) {
                qualityColor = getColor(R.color.quality_high);
            }else if (quality == MediaItem.MEDIA_QUALITY_HIRES) {
                qualityColor = getColor(R.color.quality_hires);
            }
        }
        return qualityColor;
    }

    public ApplicationInfo getApplicationInfo(String packageName) {
        ApplicationInfo ai;
        try {
            ai = getPackageManager().getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return ai;
    }

    public Bitmap getPlayerIconBitmap() {
        if(listeningReceiver!=null) {
            return listeningReceiver.getPlayerIconBitmap();
        }
        return null;
    }

    public Drawable getPlayerIconDrawable() {
        if(listeningReceiver!=null) {
            return listeningReceiver.getPlayerIconDrawable();
        }
        return null;
    }

    public String getPlayerName() {
        if(listeningReceiver!=null) {
            return listeningReceiver.getPlayerName();
        }
        return "UNKNOWN Player";
    }

    private NotificationCompat.Builder createCustomNotificationBuilder(Context context) {

        return new NotificationCompat.Builder(context, null)
                .setSmallIcon(R.drawable.ic_launcher)
                .setShowWhen(true)
                .setOngoing(true)
                .setGroup(CHANNEL_ID)
                .setGroupSummary(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setChannelId(CHANNEL_ID)
                .setAutoCancel(false);
    }

    private void displayNotification(Context context, Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public static String getSubtitle(String album, String artist) {
        String title;
        if(StringUtils.isEmpty(artist) && StringUtils.isEmpty(album)) {
            title = "Tab to open on Music Mate...";
        }else if(StringUtils.isEmpty(artist)){
            title = album;
        }else if(StringUtils.isEmpty(album)){
            title = artist;
        }else {
            title = artist+ StringUtils.ARTIST_SEP+album;
        }
        return title;
    }

    public void playNextSong() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100,VibrationEffect.DEFAULT_AMPLITUDE));
            }else{
                //deprecated in API 26
                vibrator.vibrate(100);
            }
        }catch(Exception ex) {}

        if(ListeningReceiver.PACKAGE_UAPP.equals(listeningReceiver.getPlayerPackage()) ||
                ListeningReceiver.PACKAGE_FOOBAR2000.equals(listeningReceiver.getPlayerPackage()) ||
                ListeningReceiver.PACKAGE_GOOGLE_MUSIC_PLAYER.equals(listeningReceiver.getPlayerPackage()) ||
                ListeningReceiver.PREFIX_VLC.equals(listeningReceiver.getPlayerPackage())) {
            // default for all player
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
            audioManager.dispatchMediaKeyEvent(event);
        }else {
            // neutron player need this
            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
            sendOrderedBroadcast(i, null);

            i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
            sendOrderedBroadcast(i, null);
        }
    }

    public MediaItem getListeningSong() {
        return listeningSong;
    }

    private void searchListeningMediaItem(String currentTitle, String currentArtist, String currentAlbum) {
        MediaItemProvider provider = MediaItemProvider.getInstance();
        listeningSong = null;
        if(provider!=null) {
            try {
                listeningSong = provider.searchMediaItem(currentTitle, currentArtist, currentAlbum);
            } catch (Exception ex) {
                LogHelper.e(TAG, ex);
            }
        }
        //if(listeningSong==null) {
        //    listeningSong = new MediaItem();
        //}
    }

    public void finishNotification() {
        if(builder!=null) {
            builder.setOngoing(false);
            displayNotification(context, builder.build());
        }else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    protected void sendBroadcast(final String command, String mediaPath){
        // Fire the broadcast with intent packaged
        // Construct our Intent specifying the Service
        Intent intent = new Intent(ACTION);
        // Add extras to the bundle
        intent.putExtra("resultCode", Activity.RESULT_OK);
        if(!StringUtils.isEmpty(mediaPath)) {
            intent.putExtra(Constants.KEY_MEDIA_PATH, mediaPath);
        }
        intent.putExtra("command", command);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void setListeningReceiver(ListeningReceiver androidReceiver) {
        this.listeningReceiver = androidReceiver;
    }

    public void playNextSongifMatched(MediaItem item) {
        if(item.equals(getListeningSong())) {
            playNextSong();
        }
    }
}
