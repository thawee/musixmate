package apincer.android.uamp.receiver;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

/**
 * Created by Administrator on 12/1/17.
 */

public abstract class NotificationReader implements ListeningReceiver{
    private String TAG = LogHelper.makeLogTag(NotificationReader.class);

    protected MusicService service;
    public abstract  int getTitleKey();
    public abstract  int getArtistKey();
    public abstract  int getAlbumKey();
    public abstract  String[] getPackageName();
    protected String player;
    protected String playerPackage;
    protected Bitmap iconBitmap;

    public NotificationReader(MusicService service) {
        this.service = service;
        iconBitmap = BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_broken_image_black_24dp);
        player = "UNKNOWN Player";
    }

    public void process(Notification notification) {
        Map<Integer, String> text = extractTextFromContentView(notification);
        String track = StringUtils.trimTitle(text.get(getTitleKey()));
        String artist = StringUtils.trimTitle(text.get(getArtistKey()));
        String album = StringUtils.trimTitle(text.get(getAlbumKey()));
        service.setListeningSong(track, artist, album);
    }

    public boolean isValidPackage(String pack) {
        if (pack == null) return false;
        String readerPacks[] = getPackageName();
        for (String readerPack : readerPacks) {
            if(pack.equalsIgnoreCase(readerPack)) {
                return true;
            }
        }
        return false;
    }

    protected Map<Integer,String> extractTextFromContentView(Notification notification) {
        RemoteViews views = notification.bigContentView;
        if(views ==null) {
            views = notification.contentView;
        }
        Map<Integer, String> text = new HashMap<Integer, String>();

        if(views==null) {
            return text;
        }

        try {
            Class secretClass = views.getClass();

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
        if(extras ==null) return "";

        StringBuilder sb= new StringBuilder();
        for(String key : extras.keySet()) {
            sb.append(key).append("=").append(extras.get(key));
            sb.append(", ");
        }

        return sb.toString();
    }

    @Override
    public String getPlayerPackage(){
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
}
