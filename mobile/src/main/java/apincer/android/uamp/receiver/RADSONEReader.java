package apincer.android.uamp.receiver;

import apincer.android.uamp.MusicService;

/**
 * Created by Administrator on 12/1/17.
 */

public class RADSONEReader  extends NotificationReader {
    private String[] packages = new String[]{"com.radsone.dct"};
    @Override
    public int getTitleKey() {
        return 2131362003;
    }

    @Override
    public int getArtistKey() {
        return 2131362009;
    }

    @Override
    public int getAlbumKey() {
        return -1;
    }

    @Override
    public String[] getPackageName() {
        return packages;
    }

    public RADSONEReader(MusicService service) {
        super(service);
    }
}
