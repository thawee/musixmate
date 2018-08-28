package apincer.android.uamp.glide;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

import apincer.android.uamp.model.AlbumInfo;

/**
 * Created by e1022387 on 12/24/2017.
 */

public class AlbumInfoModelLoader implements ModelLoader<AlbumInfo, InputStream> {
    @android.support.annotation.Nullable
    @Override
    public LoadData<InputStream> buildLoadData(AlbumInfo mediaItem, int width, int height, Options options) {
        return new LoadData<>(mediaItem, new AlbumInfoDataFetcher(mediaItem));
    }

    @Override
    public boolean handles(AlbumInfo mediaItem) {
        return true;
    }

    static class Factory implements ModelLoaderFactory<AlbumInfo, InputStream> {
        private final ModelCache<AlbumInfo, GlideUrl> modelCache = new ModelCache<>(500);

        @Override
        public ModelLoader<AlbumInfo, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new AlbumInfoModelLoader();
        }

        @Override
        public void teardown() {

        }
    }
}
