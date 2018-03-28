package apincer.android.uamp.model;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.load.Key;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

import apincer.android.uamp.R;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.utils.UIUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * MusicBrainz Album (release) data
 */
public class AlbumInfo extends AbstractFlexibleItem<AlbumInfo.AlbumInfoViewHolder>
        implements Serializable, Key{

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof AlbumInfo) {
            AlbumInfo inItem = (AlbumInfo) inObject;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(String.valueOf(id).getBytes());
    }


    @Override
    public int getLayoutRes() {
        return R.layout.view_list_auto_image;
    }

    @Override
    public AlbumInfo.AlbumInfoViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new AlbumInfo.AlbumInfoViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, AlbumInfo.AlbumInfoViewHolder holder, int position, List<Object> payloads) {
        GlideApp.with(holder.mContext)
                .load(this)
               // .placeholder(R.drawable.loading_spinner)
               // .error(R.drawable.ic_launcher)
                .into(holder.mCover);
    }
    static class AlbumInfoViewHolder extends FlexibleViewHolder {
        ImageView mCover;
        Context mContext;

        public AlbumInfoViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.mContext = view.getContext();
            this.mCover = view.findViewById(R.id.row_cover);
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSmallCoverUrl() {
        return smallCoverUrl;
    }

    public void setSmallCoverUrl(String smallCoverUrl) {
        this.smallCoverUrl = smallCoverUrl;
    }

    public String getLargeCoverUrl() {
        return largeCoverUrl;
    }

    public void setLargeCoverUrl(String largeCoverUrl) {
        this.largeCoverUrl = largeCoverUrl;
    }

    public String name;
    public String smallCoverUrl;
    public String largeCoverUrl;


    @Override
    public String toString() {
        return "AlbumInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", small CoverArt='" + smallCoverUrl+ '\'' +
                ", large CoverArt='" + largeCoverUrl + '\'' +
                '}';
    }
}