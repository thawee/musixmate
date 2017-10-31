package apincer.android.uamp.glide;

import android.content.Context;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.IOException;
import java.io.InputStream;

import apincer.android.uamp.item.MediaItem;
import apincer.android.uamp.provider.MediaProvider;

/**
 * Created by Administrator on 7/25/17.
 */

public class CoverArtModelLoader implements ModelLoader<MediaItem,InputStream> {
    public final Context context;
    private MediaProvider mediaprovider;

    public static class Factory implements ModelLoaderFactory<MediaItem, InputStream> {
        private final Context context;

        public Factory(Context context) {
            this.context = context;
        }

        @Override
        public ModelLoader<MediaItem, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new CoverArtModelLoader(context);
        }

        @Override
        public void teardown() {
        }
    }

    public CoverArtModelLoader(Context context) {
        super();
        this.context=context;
        this.mediaprovider = new MediaProvider(context);
       // this.context = context.getApplicationContext();
    }

    @Override
    public LoadData<InputStream> buildLoadData(MediaItem model, int width, int height,Options options) {
        //CoverArtFetcher fetcher = new CoverArtFetcher(context, model);

        return new LoadData<>(new ObjectKey(model.getPath()), new CoverArtFetcher(model));
    }

    @Override
    public boolean handles(MediaItem model) {
        return true;
    }

    private class CoverArtFetcher  implements DataFetcher<InputStream> {
        private InputStream inputStream;
       // private MediaProvider mediaprovider;
        private MediaItem model;

        public CoverArtFetcher(MediaItem model) {
            //mediaprovider = new MediaProvider(context);
           // this.mediaprovider = mediaprovider;
            this.model = model;
        }

        @Override
        public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
            try {
                inputStream = mediaprovider.openCoverArtInputStream(model);
                if (inputStream!=null) {
                    callback.onDataReady(inputStream);
                }else {
                    callback.onLoadFailed(new Exception("No Cover Art"));
                }
            } catch (Exception e) {
                callback.onLoadFailed(e);
                return;
            }
        }

        @Override
        public void cleanup() {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignored.
                }
            }
        }

        @Override
        public void cancel() {

        }

        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}

