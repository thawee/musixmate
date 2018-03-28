package apincer.android.uamp;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.receiver.AndroidReceiver;
import apincer.android.uamp.receiver.HFPlayerReader;
import apincer.android.uamp.receiver.HTCReceiver;
import apincer.android.uamp.receiver.NotificationReader;
import apincer.android.uamp.receiver.RADSONEReader;
import apincer.android.uamp.receiver.SamsungReceiver;
import apincer.android.uamp.receiver.SonyReceiver;
import apincer.android.uamp.receiver.TPlayerReader;
import apincer.android.uamp.receiver.UAPPReceiver;
import apincer.android.uamp.receiver.VIVOReceiver;
import apincer.android.uamp.ui.MediaBrowserActivity;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

/**
 * Created by e1022387 on 5/29/2017.
 */

public class MusicService extends AccessibilityService {
    public static final String ACTION = "com.apincer.uamp.MusicService";
    // android 5 SD card permissions
    public static final int REQUEST_CODE_SD_PERMISSION = 1010;
    public static final int REQUEST_CODE_EDIT_MEDIA_TAG = 2020;

    public static final String FLAG_SHOW_LISTENING = "__FLAG_SHOW_LISTENING";

    public static String[] PERMISSIONS_ALL = {Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final String CHANNEL_ID = "music_mate_now_listening";
    private static final  int NOTIFICATION_ID = 19099;
    private String TAG = LogHelper.makeLogTag(MusicService.class);
    private Context context;
    private static MusicService instance;
    private String currentTitle;
    private String currentArtist;
    private String currentAlbum;
    private MediaItem listeningSong;
    private static List<BroadcastReceiver> receivers = new ArrayList<>();
    private static List<NotificationReader> readers= new ArrayList<>();

    private NotificationCompat.Builder builder;
    private NotificationChannel mChannel;
    private Object listeningReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        //createNotificationChannel();
        registerReceiver(new AndroidReceiver(this));
       // registerReceiver(new GonemadReceiver(this));
       // registerReceiver(new HTCReceiver(this));
      //  registerReceiver(new JRStudioReceiver(this));
      //  registerReceiver(new SamsungReceiver(this));
        registerReceiver(new SonyReceiver(this));
       // registerReceiver(new MiuiReceiver(this));
        registerReceiver(new UAPPReceiver(this));
        registerReceiver(new VIVOReceiver(this));

        registerReader(new RADSONEReader(this));
        registerReader(new TPlayerReader(this));
        registerReader(new HFPlayerReader(this));


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
        String description = "Music Mate listening song...";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
        instance = null;
    }

    private void registerReader(NotificationReader reader) {
        readers.add(reader);
    }

    private void registerReceiver(AndroidReceiver receiver) {
        receiver.register(context);
        receivers.add(receiver);
    }

    private void unregisterReceivers() {
        for (BroadcastReceiver receiver: receivers){
            unregisterReceiver(receiver);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pack = String.valueOf(event.getPackageName());

        if(StringUtils.isEmpty(pack)) return;

         if(event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && readers.size()>0) {
            Parcelable data = event.getParcelableData();
            if(data instanceof  Notification) {
                Notification notification = (Notification)data;
                for (NotificationReader reader: readers) {
                    if(reader.isValidPackage(pack)) {
                       reader.process(notification);
                    }
                }
            }
        }
    }

    public static MusicService getRunningService() {
        return instance;
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        if(readers.size()>0) {
            List<String> packages = new ArrayList<String>();
            for (int i = 0; i < readers.size(); i++) {
                packages.addAll(Arrays.asList(readers.get(i).getPackageName()));
            }
            info.packageNames = packages.toArray(new String[0]);
        }
        setServiceInfo(info);
    }

    public void showNotification(String title, String artist, String album) {
        currentTitle = StringUtils.trimTitle(title);
        currentArtist = StringUtils.trimTitle(artist);
        currentAlbum = StringUtils.trimTitle(album);
        getListeningItem();

        if(StringUtils.isEmpty(currentTitle) && StringUtils.isEmpty(currentArtist) && StringUtils.isEmpty(currentAlbum)) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        // display song info from music library
        if(listeningSong!=null) {
            MediaItemService.addPlaying(listeningSong);
           // if(mAutoScrollToListening) {
                sendBroadcast("playing", listeningSong.getId());
           //     mAutoScrollToListening = false;
           // }
        }
        Notification notification = createNotification(listeningSong);
        displayNotification(context, notification);
    }

    private PendingIntent getPendingIntent(MediaItem item) {
        Intent intent = new Intent(this, MediaBrowserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (item!=null) {
           // addMediaItem(item);
            intent.putExtra(FLAG_SHOW_LISTENING, "yes");
            intent.putExtra("mediaId", item.getId());
        }
        return PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification createNotification(MediaItem item) {
        builder = createCustomNotificationBuilder(context);
        int layoutId = R.layout.view_notification;
        try {
            if(item==null) {
                layoutId = R.layout.view_notification_missing;
            }
            RemoteViews contentView = new RemoteViews(getPackageName(), layoutId);
            contentView.setImageViewResource(R.id.notification_coverart, R.drawable.ic_broken_image_black_24dp);
            builder.setContent(contentView);
            int panelLabelColor = getColor(R.color.now_playing);
            int panelColor = getColor(R.color.grey200);
            int textColor = Color.WHITE;
            if (item != null) {
                Bitmap bmp = MediaProvider.getInstance().getSmallArtwork(item);
                if (bmp != null) {
                    contentView.setImageViewBitmap(R.id.notification_coverart, bmp);
                    Palette palette = Palette.from(bmp).generate();
                    panelColor = palette.getDominantColor(panelColor);
                    panelLabelColor = palette.getMutedColor(panelLabelColor);
                }
                contentView.setTextViewText(R.id.notification_title, item.getTitle());
                contentView.setTextViewText(R.id.notification_artist, item.getSubtitle());
                contentView.setTextViewText(R.id.notification_bitsample, item.getMetadata().getAudioCoding());
                contentView.setTextViewText(R.id.notification_extension, item.getMetadata().getAudioFormatInfo());
                contentView.setTextViewText(R.id.notification_filesize, item.getMetadata().getMediaSize());
                contentView.setTextViewText(R.id.notification_duration, item.getMetadata().getAudioDurationAsString());

                int qualityColor = getQualityColor(item);
                contentView.setInt(R.id.notification_format_bgcolor, "setColorFilter", qualityColor);
                contentView.setInt(R.id.notification_bitsample_bgcolor, "setColorFilter", qualityColor);
                contentView.setInt(R.id.notification_filesize_bgcolor, "setColorFilter", qualityColor);
                contentView.setInt(R.id.notification_duration_bgcolor, "setColorFilter", qualityColor);
            }else {
                contentView.setTextViewText(R.id.notification_title, currentTitle);
                contentView.setTextViewText(R.id.notification_artist, getSubtitle(currentAlbum, currentArtist));
            }

            contentView.setTextColor(R.id.notification_title, textColor);
            contentView.setTextColor(R.id.notification_artist, textColor);

            Bitmap background = getBackground(panelLabelColor, 1024, 60, getApplicationContext());
           // background.

            contentView.setImageViewBitmap(R.id.notification_bgcolor, getBackground(panelColor, 1024, 60, getApplicationContext()));
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
        if(item!=null && item.getTag()!=null) {
            MediaMetadata.MediaQuality quality = item.getMetadata().getQuality();
            switch (quality) {
                case LOW:
                    qualityColor = getColor(R.color.quality_low);
                    break;
                case NORMAL:
                    qualityColor = getColor(R.color.quality_normal);
                    break;
                case GOOD:
                    qualityColor = getColor(R.color.quality_good);
                    break;
                case HIGH:
                    qualityColor = getColor(R.color.quality_high);
                    break;
                case HIRES:
                    qualityColor = getColor(R.color.quality_hires);
                    break;
            }
        }
        return qualityColor;
    }

    public static Bitmap getBackground(int bgColor, int width, int height, Context context) {
        try {
            // convert to HSV to lighten and darken
            int alpha = Color.alpha(bgColor);
            float[] hsv = new float[3];
            Color.colorToHSV(bgColor, hsv);
            hsv[2] -= .1;
            int darker = Color.HSVToColor(alpha, hsv);
            hsv[2] += .3;
            int lighter = Color.HSVToColor(alpha, hsv);

            // create gradient useng lighter and darker colors
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,new int[] { darker, lighter});
            gd.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            // set corner size
            // top-left, top-right, bottom-right, bottom-left
            gd.setCornerRadii(new float[] {4,4,4,4,4,4,4,4});

            // get density to scale bitmap for device
            float dp = context.getResources().getDisplayMetrics().density;

            // create bitmap based on width and height of widget
            Bitmap bitmap = Bitmap.createBitmap(Math.round(width * dp), Math.round(height * dp),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas =  new Canvas(bitmap);
            gd.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            gd.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private NotificationCompat.Builder createCustomNotificationBuilder(Context context) {

        return new NotificationCompat.Builder(context, null)
                .setSmallIcon(R.drawable.ic_launcher)
                .setShowWhen(true)
                .setOngoing(true)
                .setGroup(CHANNEL_ID)
                .setGroupSummary(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
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
        if(listeningReceiver instanceof UAPPReceiver) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
            audioManager.dispatchMediaKeyEvent(event);
        }else {
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

    private void getListeningItem() {
        MediaProvider provider = MediaProvider.getInstance();
        if(provider!=null) {
            MediaProvider.initialize(this);
            provider = MediaProvider.getInstance();
        }
        try {
            listeningSong = provider.searchListeningMediaItem(currentTitle, currentArtist, currentAlbum);
        }catch (Exception ex){}
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

    protected void sendBroadcast(final String command, int mediaId){
        // Fire the broadcast with intent packaged
        // Construct our Intent specifying the Service
        Intent intent = new Intent(ACTION);
        // Add extras to the bundle
        intent.putExtra("resultCode", Activity.RESULT_OK);
        if(mediaId>-1) {
            intent.putExtra("mediaId", mediaId);
        }
        intent.putExtra("command", command);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void setListeningReceiver(Object androidReceiver) {
        this.listeningReceiver = androidReceiver;
    }
}
