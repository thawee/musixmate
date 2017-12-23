package apincer.android.uamp;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;

import org.michaelevans.colorart.library.ColorArt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apincer.android.uamp.flexibleadapter.MediaItem;
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
import apincer.android.uamp.utils.UIUtils;

/**
 * Created by e1022387 on 5/29/2017.
 */

public class MusicService extends AccessibilityService {
    // android 5 SD card permissions
    public static final int REQUEST_CODE_PERMISSION_All = 111;

    public static String[] PERMISSIONS_ALL = {Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static String[] PERMISSIONS_INTERNET = {Manifest.permission.INTERNET};

    private static final String CHANNEL_ID = "music_mate_now_listening";
    private static final  int NOTIFICATION_ID = 19099;
    private String TAG = LogHelper.makeLogTag(MusicService.class);
    private Context context;
    private static MusicService instance;
    private String currentTitle;
    private String currentArtist;
    private String currentAlbum;
    private static List<BroadcastReceiver> receivers = new ArrayList<>();
    private static List<NotificationReader> readers= new ArrayList<>();
    private static List<MediaItem> items= new ArrayList<>();

    private NotificationCompat.Builder builder;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        registerReceiver(new AndroidReceiver(this));
       // registerReceiver(new GonemadReceiver(this));
        registerReceiver(new HTCReceiver(this));
      //  registerReceiver(new JRStudioReceiver(this));
        registerReceiver(new SamsungReceiver(this));
        registerReceiver(new SonyReceiver(this));
       // registerReceiver(new MiuiReceiver(this));
        registerReceiver(new UAPPReceiver(this));
        registerReader(new RADSONEReader(this));
        registerReader(new TPlayerReader(this));
        registerReader(new HFPlayerReader(this));
        registerReceiver(new VIVOReceiver(this));

        instance = this;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager
                mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "Media playback";
        // The user-visible description of the channel.
        String description = "Media playback controls";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
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
/*
                    try {
                        LogHelper.logToFile(pack + " - " + AccessibilityEvent.eventTypeToString(event.getEventType()), notification.tickerText + ", extras[" + getExtras(notification.extras) + "]");
                        Map<Integer, String> text = extractTextFromContentView(notification);
                        LogHelper.logToFile(pack + " - " + AccessibilityEvent.eventTypeToString(event.getEventType()), "TextView[" + getText(text) + "]");
                    } catch (Exception ex) {
                        LogHelper.e(TAG, ex);
                    }
                */
            }
        }
    }

    public static MusicService getRunningService() {
        return instance;
    }

    public void addMediaItem(MediaItem item, boolean flushExisting) {
        if(flushExisting) {
            items.clear();
        }
        items.add(item);
    }

    public void addMediaItem(MediaItem item) {
        items.add(item);
    }

    public List<MediaItem> popMediaItem() {
        List<MediaItem> list = new ArrayList<MediaItem>();
        list.addAll(items);
        items.clear();
        return list;
    }

    @Override
    public void onInterrupt() {

    }

    public boolean isListening(String title, String artist,String album) {
        if (StringUtils.isEmpty(currentTitle)) {
            return false;
        }
        return StringUtils.compare(title, currentTitle) && StringUtils.compare(artist, currentArtist) && StringUtils.compare(album, currentAlbum);
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

        if(StringUtils.isEmpty(currentTitle) && StringUtils.isEmpty(currentArtist) && StringUtils.isEmpty(currentAlbum)) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        // display song info from broadcast directly
        //Notification notification = createNotification();
        //displayNotification(context, notification);

        // display song info from music library
        MediaItem item = getListeningItem();
        Notification notification = createNotification(item);
        displayNotification(context, notification);
    }

    private PendingIntent getPendingIntent(MediaItem item) {
        Intent intent = new Intent(this, MediaBrowserActivity.class);
        if (item!=null) {
            addMediaItem(item);
            //Intent intent = new Intent(this, MediaTagEditorActivity.class);
            //intent.putExtra(MediaTagEditorActivity.ARG_MEDIA_PATH, item.getPath()); // editor
            //intent.putExtra(MediaTagEditorActivity.ARG_MEDIA_ID, String.valueOf(item.getId())); // editor
            //intent.putExtra(MediaTagEditorActivity.ARG_SOURCE,MediaTagEditorActivity.class.getName()); // editor
            //return PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }//else {
//            Intent intent = new Intent(this, MediaBrowserActivity.class);
            return PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //}
    }

    private Notification createNotification(MediaItem item) {
        builder = createCustomNotificationBuilder(context);
        int layoutId = R.layout.view_notification;

        if(item!=null && item.getTag()!=null) {
            switch (item.getTag().getQuality()) {
                case HIRES:
                    layoutId = R.layout.view_notification_hires;
                    break;
                case HIGH:
                    layoutId = R.layout.view_notification_high;
                    break;
                case GOOD:
                    layoutId = R.layout.view_notification_good;
                    break;
                case LOW:
                    layoutId = R.layout.view_notification_low;
                    break;
                default:
                    layoutId = R.layout.view_notification;
                    break;
            }
        }

        try {
            RemoteViews contentView = new RemoteViews(getPackageName(), layoutId);
            contentView.setImageViewResource(R.id.notification_coverart, R.drawable.ic_launcher);
            contentView.setTextViewText(R.id.notification_title, currentTitle);
            contentView.setTextViewText(R.id.notification_artist, getSubtitle(currentAlbum, currentArtist));
            contentView.setTextViewText(R.id.notification_bitsample, "...");
            contentView.setTextViewText(R.id.notification_extension, "...");
            builder.setContent(contentView);
            int bgColor = -1;
            int textColor = Color.BLACK;
            if (item != null) {
                Bitmap bmp = MediaProvider.getInstance().getSmallArtwork(item);
                if (bmp != null) {
                    contentView.setImageViewBitmap(R.id.notification_coverart, bmp);
                    ColorArt colorArt = new ColorArt(bmp);
                    bgColor = UIUtils.lighten(colorArt.getBackgroundColor(), 0.7);
                }
                contentView.setTextViewText(R.id.notification_bitsample, item.getTag().getAudioCoding());
                contentView.setTextViewText(R.id.notification_extension, item.getAudioCodingFormat());
            }
            textColor = Color.BLACK;
            if (UIUtils.isColorDark(bgColor)) {
                textColor = Color.WHITE;
            }
            contentView.setTextColor(R.id.notification_title, textColor);
            contentView.setTextColor(R.id.notification_artist, textColor);

            if(bgColor!=-1) {
                contentView.setInt(R.id.notification_bgcolor, "setColorFilter", bgColor);
            }
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                contentView.setInt(R.id.notification_bgcolor, "setImageAlpha", bgColor);
            } else{
                contentView.setInt(R.id.notification_bgcolor, "setAlpha", WidgetPrefs.loadBgAlpha(context, appWidgetId));
            }*/

            builder.setContentIntent(getPendingIntent(item));
            //builder.setColorized(true);
        }catch(Exception ex) {
            LogHelper.e(TAG, ex);
        }

        return builder.build();
    }

    private NotificationCompat.Builder createCustomNotificationBuilder(Context context) {

        return new NotificationCompat.Builder(context, null)
                .setSmallIcon(R.drawable.ic_launcher)
                .setShowWhen(false)
                .setOngoing(true)
                .setGroup(CHANNEL_ID)
                .setGroupSummary(false)
                // .setStyle(new  NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(false);
    }

    private void displayNotification(Context context, Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private String getSubtitle(String album, String artist) {
        String title = "";
        if(StringUtils.isEmpty(artist) && StringUtils.isEmpty(album)) {
            title = "Tab to open on Music Mate...";
        }else if(StringUtils.isEmpty(artist)){
            title = album;
        }else if(StringUtils.isEmpty(album)){
            title = artist;
        }else {
            title = artist+" / "+album;
        }
        return title;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void nextSong(String title) {
        if(StringUtils.trimToEmpty(getCurrentTitle()).equalsIgnoreCase(title)) {
            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
            sendOrderedBroadcast(i, null);

            i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
            sendOrderedBroadcast(i, null);
        }
    }

    public MediaItem getListeningItem() {
        MediaProvider provider = MediaProvider.getInstance();
        if(provider!=null) {
            MediaProvider.initialize(this);
            provider = MediaProvider.getInstance();
        }
        try {
            return provider.queryMediaItem(currentTitle, currentArtist, currentAlbum);
        }catch (Exception ex){}

        return null;
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

    public boolean haveMediaItems() {
        return items.size()>0;
    }

    public void clearMediaItem() {
        items.clear();
    }

}
