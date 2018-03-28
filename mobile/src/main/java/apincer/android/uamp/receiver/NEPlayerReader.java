package apincer.android.uamp.receiver;

import android.app.Notification;

import java.util.Map;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.utils.StringUtils;

/**
 * Created by Administrator on 12/1/17.
 */

public class NEPlayerReader extends NotificationReader {
    private String[] packages = new String[]{"jp.co.radius.neplayer_ver2","jp.co.radius.neplayer"};
     @Override
    public int getTitleKey() {
        return 2131623999;
    }

    @Override
    public int getArtistKey() {
        return 2131624075;
    }

    @Override
    public int getAlbumKey() {
        return -1;
    }

    @Override
    public String[] getPackageName() {
        return packages;
    }

    @Override
    public void process(Notification notification) {
        Map<Integer, String> text = extractTextFromContentView(notification);
        String track = StringUtils.trimTitle(text.get(getTitleKey()));
        String artist = StringUtils.trimTitle(text.get(getArtistKey()));
        String album = "";
        if(artist.indexOf("/")>0) {
            album = StringUtils.trimToEmpty(artist.substring(artist.indexOf("/")+1, artist.length()));
            artist = StringUtils.trimToEmpty(artist.substring(0,artist.indexOf("/")));
        }
        service.showNotification(track, artist, album);
    }

    public NEPlayerReader(MusicService service) {
        super(service);
    }
}
