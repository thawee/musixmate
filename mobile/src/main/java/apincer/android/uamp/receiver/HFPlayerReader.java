package apincer.android.uamp.receiver;

import apincer.android.uamp.MusicService;

/**
 * Created by Administrator on 12/1/17.
 */
public class HFPlayerReader extends NotificationReader {
    private String[] packages = new String[]{"com.onkyo.jp.musicplayer"};
    @Override
    public int getTitleKey() {
        return 2131624061;
    }

    @Override
    public int getArtistKey() {
        return 2131624051;
    }

    @Override
    public int getAlbumKey() {
        return 2131624050;
    }

    @Override
    public String[] getPackageName() {
        return packages;
    }

    public HFPlayerReader(MusicService service) {
        super(service);
    }
}
