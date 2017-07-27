package apincer.android.uamp.glide;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.module.GlideModule;

import java.io.InputStream;

import apincer.android.uamp.item.MediaItem;

/**
 * Created by Administrator on 7/25/17.
 */

public class CoverArtGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setMemoryCache(new LruResourceCache(10 * 1024 * 1024));
        // set size & external vs. internal
        int cacheSize100MegaBytes = 104857600;

        builder.setDiskCache(
                new InternalCacheDiskCacheFactory(context, cacheSize100MegaBytes)
        );
    }

    @Override
    public void registerComponents(Context context, Registry registry) {
        registry.append(MediaItem.class, InputStream.class, new CoverArtModelLoader.Factory(context));
    }

    // Disable manifest parsing to avoid adding similar modules twice.
    //@Override
    //public boolean isManifestParsingEnabled() {
    //    return false;
    //}
}

