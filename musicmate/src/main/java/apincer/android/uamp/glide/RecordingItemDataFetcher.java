package apincer.android.uamp.glide;

import android.support.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

import apincer.android.uamp.model.AlbumInfo;
import apincer.android.uamp.model.RecordingItem;
import apincer.android.uamp.musicbrainz.MusicBrainz;
import timber.log.Timber;

/**
 * Created by e1022387 on 12/24/2017.
 */

public class RecordingItemDataFetcher implements DataFetcher<InputStream> {
    private boolean isCanceled;
    private RecordingItem mediaItem;
    InputStream mInputStream = null;
    public RecordingItemDataFetcher(RecordingItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        try {
            Timber.e(mediaItem.album);
            if (!isCanceled) {
                AlbumInfo albumInfo = mediaItem.getAlbumItem();
                if (albumInfo == null) {
                    albumInfo = new AlbumInfo();
                    mediaItem.setAlbumItem(albumInfo);
                }
                albumInfo.setId(mediaItem.albumId);
                albumInfo.setName(mediaItem.album);
                albumInfo = MusicBrainz.getAlbumArt(albumInfo);
                if (albumInfo.getLargeCoverUrl() != null) {
                    mInputStream = MusicBrainz.getInputStream(albumInfo.getLargeCoverUrl());
                }
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
