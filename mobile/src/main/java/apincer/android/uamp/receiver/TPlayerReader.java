package apincer.android.uamp.receiver;

import apincer.android.uamp.MusicService;

/**
 * Created by Administrator on 12/1/17.
 */
public class TPlayerReader extends NotificationReader {
    private String[] packages = new String[]{"com.wiseschematics.resoundmethods01"};
    @Override
    public int getTitleKey() {
        return 2131492878;
    }

    @Override
    public int getArtistKey() {
        return 2131492880;
    }

    @Override
    public int getAlbumKey() {
        return 2131492879;
    }

    @Override
    public String[] getPackageName() {
        return packages;
    }

    public TPlayerReader(MusicService service) {
        super(service);
    }
}
