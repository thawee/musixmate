package apincer.android.uamp.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

import apincer.android.uamp.item.MediaItem;

/**
 * Created by Administrator on 7/25/17.
 */
@GlideModule
public class CoverArtGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        int cacheSize100MegaBytes = 104857600;
        int cacheSize500MegaBytes = 504857600;
        //builder.setMemoryCache(new LruResourceCache(10 * 1024 * 1024));
        builder.setMemoryCache(new LruResourceCache(cacheSize100MegaBytes));
        // set size & external vs. internal

        builder.setDiskCache(
                new InternalCacheDiskCacheFactory(context, cacheSize500MegaBytes)
        );
    }

    //@Override
    //public void registerComponents(Context context, Registry registry) {
    //    registry.append(MediaItem.class, InputStream.class, new CoverArtModelLoader.Factory(context));
    //}

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.append(MediaItem.class, InputStream.class, new CoverArtModelLoader.Factory(context));
    }

    // Disable manifest parsing to avoid adding similar modules twice.
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}

