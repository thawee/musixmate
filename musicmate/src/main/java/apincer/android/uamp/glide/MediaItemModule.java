package apincer.android.uamp.glide;

/**
 * Created by e1022387 on 12/24/2017.
 */

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

import apincer.android.uamp.model.AlbumInfo;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.RecordingItem;

/**
 * Ensures that Glide's generated API is created for the MediaIte,.
 */
@GlideModule
public class MediaItemModule extends AppGlideModule {
    // Intentionally empty.
    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.append(MediaItem.class, InputStream.class, new MediaItemModelLoader.Factory());
        registry.append(AlbumInfo.class, InputStream.class, new AlbumInfoModelLoader.Factory());
        registry.append(RecordingItem.class, InputStream.class, new RecordingItemModelLoader.Factory());
    }
}
