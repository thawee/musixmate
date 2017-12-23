package apincer.android.uamp.ui.image;

/**
 * Created by Administrator on 11/8/17.
 */

import android.content.Context;
import android.graphics.Bitmap;

import apincer.android.uamp.flexibleadapter.MediaItem;
import apincer.android.uamp.provider.MediaProvider;

public class SmartImage {
    private MediaItem item;
    private static SmartImageCache imageCache;
    public SmartImage(MediaItem item) {
        this.item = item;
    }

    public Bitmap getBitmap(Context context) {
        // Don't leak context
        if(imageCache == null) {
            imageCache = new SmartImageCache(context);
        }

        // Try getting bitmap from cache first
        Bitmap bitmap = null;
        if(item != null) {
            if(!item.isLoadedEncoding()) {
                MediaProvider.getInstance().loadMediaTag(item, null);
            }
            bitmap = imageCache.get(item.getPath());
            if(bitmap == null) {
                bitmap = MediaProvider.getInstance().getArtwork(item);
                if(bitmap != null){
                    imageCache.put(item.getPath(), bitmap);
                }
            }
        }

        return bitmap;

    }

    public boolean isNowPlaying() {
        return item.isNowPlaying();
    }
}
