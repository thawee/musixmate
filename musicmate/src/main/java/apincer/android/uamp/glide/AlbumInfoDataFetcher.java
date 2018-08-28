package apincer.android.uamp.glide;

import android.support.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

import apincer.android.uamp.model.AlbumInfo;
import apincer.android.uamp.musicbrainz.MusicBrainz;
import timber.log.Timber;

/**
 * Created by e1022387 on 12/24/2017.
 */

public class AlbumInfoDataFetcher implements DataFetcher<InputStream> {
    private boolean isCanceled;
    private AlbumInfo mediaItem;
    InputStream mInputStream = null;
    public AlbumInfoDataFetcher(AlbumInfo mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        try {
            Timber.e(mediaItem.name);
            if (!isCanceled) {
                AlbumInfo albumInfo = MusicBrainz.getAlbumArt(mediaItem);

                mInputStream = MusicBrainz.getInputStream(albumInfo.getSmallCoverUrl());
            }
        } catch (Exception e) {
            callback.onLoadFailed(e);
            Timber.e(e);
        }
        callback.onDataReady(mInputStream);
    }

    @Override
    public void cleanup() {
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                Timber.e(e);
            }
        }
    }

    @Override
    public void cancel() {
        isCanceled = true;
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
