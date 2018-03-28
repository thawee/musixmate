package apincer.android.uamp.model;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.Key;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

import apincer.android.uamp.R;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.utils.UIUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * MusicBrainz Song (recording) data
 */
public class RecordingItem extends AbstractFlexibleItem<RecordingItem.RecordingItemViewHolder> implements Serializable , Key {

    public AlbumInfo getAlbumItem() {
        return albumItem;
    }

    public void setAlbumItem(AlbumInfo albumItem) {
        this.albumItem = albumItem;
    }

    static class RecordingItemViewHolder extends FlexibleViewHolder {
        ImageView mImage;
        TextView mTitle;
        TextView mSubtitle;
        TextView mSubtitle2;
        Context mContext;

        public RecordingItemViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.mContext = view.getContext();
            this.mImage = view.findViewById(R.id.image);
            this.mTitle = view.findViewById(R.id.title);
            this.mSubtitle = view.findViewById(R.id.subtitle1);
            this.mSubtitle2 = view.findViewById(R.id.subtitle2);
         }

        public Context getContext() {
            return mContext;
        }

        @Override
        public float getActivationElevation() {
            return UIUtils.dpToPx(itemView.getContext(), 4f);
        }
    }

    /**
     * The MusicBrainz ID of the release
     */
    public String id;
    public String title;
    public String artist;
    public String artistId;
    public String album;
    public String albumId;
    public String genre;
    public String year;
    private AlbumInfo albumItem;

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(String.valueOf(albumId).getBytes());
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof RecordingItem) {
            RecordingItem inItem = (RecordingItem) inObject;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_list_item_mini;
    }

    @Override
    public RecordingItem.RecordingItemViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new RecordingItem.RecordingItemViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, RecordingItem.RecordingItemViewHolder holder, int position, List payloads) {
        holder.mTitle.setText(title);
        holder.mSubtitle.setText(artist);
        holder.mSubtitle2.setText(album);
        GlideApp.with(holder.mContext)
                .load(this)
                .placeholder(R.drawable.progress)
                .error(R.drawable.ic_broken_image_black_24dp)
                .into(holder.mImage);
    }

    @Override
    public String toString() {
        return "RecordingItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", albumId='" + albumId + '\'' +
                ", year='" + year + '\'' +
                '}';
    }

}