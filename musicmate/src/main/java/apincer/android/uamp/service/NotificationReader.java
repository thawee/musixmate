package apincer.android.uamp.service;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apincer.android.uamp.utils.BitmapHelper;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

public class NotificationReader implements ListeningReceiver {
    private static final String DCT_PACKAGE = "com.radsone.dct";
    private static final String HIBY_PACKAGE = "com.hiby.music";
    private String TAG = LogHelper.makeLogTag(NotificationReader.class);

    public MusicListeningService getService() {
        return service;
    }

    private MusicListeningService service;
    protected String playerName;
    protected String playerPackage;
    protected Bitmap iconBitmap = null;
    protected Drawable iconDrawable = null;
    public static String DEAFULT_PLAYER_NAME = "UNKNOWN Player";

    public NotificationReader(MusicListeningService service) {
        this.service = service;
    }

    public String getPlayerPackage() {
        return playerPackage;
    }

    @Override
    public String getPlayerName() {
        if(playerName==null) {
            initPlayerInfos();
        }
        return playerName;
    }

    @Override
    public Bitmap getPlayerIconBitmap() {
        if(iconBitmap==null) {
            initPlayerInfos();
        }
        return iconBitmap;
    }

    @Override
    public Drawable getPlayerIconDrawable() {
        if(iconDrawable==null) {
            initPlayerInfos();
        }
        return iconDrawable;
    }

    private void initPlayerInfos() {
        ApplicationInfo ai = service.getApplicationInfo(getPlayerPackage());
        if(ai!=null) {
            iconDrawable = service.getApplication().getApplicationContext().getPackageManager().getApplicationIcon(ai);
            if (iconDrawable != null) {
                iconBitmap = BitmapHelper.drawableToBitmap(iconDrawable);
            }else {
                iconBitmap = null;
            }
            if(playerName==null) {
                playerName = String.valueOf(service.getApplication().getApplicationContext().getPackageManager().getApplicationLabel(ai));
            }
        }
    }

    protected Map<Integer,String> extractTextFromContentView(Notification notification) {
        RemoteViews views = notification.bigContentView;
        if(views ==null) {
            views = notification.contentView;
        }
        Map<Integer, String> text = new HashMap<>();

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

    public void process(Notification notification, String pack) {
        this.playerPackage = pack;
        Map<Integer, String> text = extractTextFromContentView(notification);
        String track = "";
        String artist = "";
        String album = "";

        if (DCT_PACKAGE.equals(playerPackage)) {
            track = StringUtils.trimTitle(text.get(2131362003));
            artist = StringUtils.trimTitle(text.get(2131362009));
            album = StringUtils.trimTitle(text.get(-1));
        }else if(HIBY_PACKAGE.equals(playerPackage)) {
            track = StringUtils.trimTitle(text.get(2131362003));
            artist = StringUtils.trimTitle(text.get(2131362009));
            album = StringUtils.trimTitle(text.get(-1));
        }

        getService().setListeningSong(track, artist, album);
    }

    public boolean isPackageAccepted(String pack) {
        if(DCT_PACKAGE.equalsIgnoreCase(pack)) {
            return true;
        }else if(HIBY_PACKAGE.equalsIgnoreCase(pack)) {
            return true;
        }
        return false;
    }

    public List getPackages() {
        String [] packages = {DCT_PACKAGE,HIBY_PACKAGE};
        return Arrays.asList(packages);
    }
}
