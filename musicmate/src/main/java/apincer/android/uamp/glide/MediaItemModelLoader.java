package apincer.android.uamp.glide;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

import apincer.android.uamp.model.MediaItem;

/**
 * Created by e1022387 on 12/24/2017.
 */

public class MediaItemModelLoader implements ModelLoader<MediaItem, InputStream> {
    @android.support.annotation.Nullable
    @Override
    public LoadData<InputStream> buildLoadData(MediaItem mediaItem, int width, int height, Options options) {
        return new LoadData<>(mediaItem, new MediaItemDataFetcher(mediaItem));
    }

    @Override
    public boolean handles(MediaItem mediaItem) {
        return true;
    }

    static class Factory implements ModelLoaderFactory<MediaItem, InputStream> {
        private final ModelCache<MediaItem, GlideUrl> modelCache = new ModelCache<>(500);

        @Override
        public ModelLoader<MediaItem, InputStream> build(MultiModelLoaderFactory multiFactory) {
          //  ModelLoader<MediaItem, InputStream> modelLoader = multiFactory.build(MediaItem.class, InputStream.class);
            return new MediaItemModelLoader();
        }

        @Override
        public void teardown() {

        }
    }
}
