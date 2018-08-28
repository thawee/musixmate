package apincer.android.uamp.glide;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

import apincer.android.uamp.model.RecordingItem;

/**
 * Created by e1022387 on 12/24/2017.
 */

public class RecordingItemModelLoader implements ModelLoader<RecordingItem, InputStream> {
    @android.support.annotation.Nullable
    @Override
    public LoadData<InputStream> buildLoadData(RecordingItem mediaItem, int width, int height, Options options) {
        return new LoadData<>(mediaItem, new RecordingItemDataFetcher(mediaItem));
    }

    @Override
    public boolean handles(RecordingItem mediaItem) {
        return true;
    }

    static class Factory implements ModelLoaderFactory<RecordingItem, InputStream> {
        private final ModelCache<RecordingItem, GlideUrl> modelCache = new ModelCache<>(500);

        @Override
        public ModelLoader<RecordingItem, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new RecordingItemModelLoader();
        }

        @Override
        public void teardown() {

        }
    }
}
