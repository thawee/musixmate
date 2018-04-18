package apincer.android.uamp.model;

import android.animation.Animator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.load.Key;
import com.github.siyamed.shapeimageview.RoundedImageView;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.ui.MediaBrowserActivity;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.UIUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.helpers.AnimatorHelper;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * {@link eu.davidea.flexibleadapter.items.AbstractFlexibleItem`bstractFlexibleItem} to benefit of the already
 * implemented methods (getter and setters).
 */
public class MediaItem extends AbstractFlexibleItem<MediaItem.MediaItemViewHolder>
        implements IFilterable<String>, Serializable, Key {

    public static int MEDIA_QUALITY_HIRES = 8;
    public static int MEDIA_QUALITY_HIGH = 7;
    public static int MEDIA_QUALITY_GOOD = 6;
    public static int MEDIA_QUALITY_AVERAGE= 5;
    public static int MEDIA_QUALITY_LOW = 4;

    public MediaMetadata getMetadata() {
        return metadata;
    }

    private volatile MediaMetadata metadata;
    private volatile MediaMetadata pendingMetadata;

    public MediaMetadata getPendingMetadata() {
        return pendingMetadata;
    }

    public void setPendingMetadata(MediaMetadata pendingMetadata) {
        this.pendingMetadata= pendingMetadata;
    }

    protected int id;

    private String pendingArtworkPath;

    public String getPendingArtworkPath() {
        return pendingArtworkPath;
    }

    public void setPendingArtworkPath(String artworkPath) {
        this.pendingArtworkPath = artworkPath;
    }

    /*
    @Deprecated
    public MediaItem(int id) {
        this.id = id;
        setDraggable(false);
        setSwipeable(false);
        metadata = new MediaMetadata(id);
    }
    */

    public MediaItem(MediaMetadata metadata) {
        this.id = (int)metadata.id;
        setDraggable(false);
        setSwipeable(false);
        this.metadata = metadata;
    }

    public String getPath() {
        return metadata.getMediaPath();
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
         return StringUtils.trimToEmpty(metadata.getTitle());
    }

    public String getSubtitle() {
        String album = StringUtils.trimTitle(metadata.getAlbum());
        String artist = StringUtils.trimTitle(metadata.getArtist());
        if(StringUtils.isEmpty(artist)) {
            artist = StringUtils.trimTitle(metadata.getAlbumArtist());
        }
        if(StringUtils.isEmpty(album) && StringUtils.isEmpty(artist)) {
            return StringUtils.UNKNOWN_CAP + StringUtils.ARTIST_SEP+StringUtils.UNKNOWN_CAP;
        } else if(StringUtils.isEmpty(album)) {
            return artist;
        } else if(StringUtils.isEmpty(artist)) {
            return StringUtils.UNKNOWN_CAP + StringUtils.ARTIST_SEP+album;
        }
        return StringUtils.truncate(artist,40)+StringUtils.ARTIST_SEP+album;
    }
    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update((""+getId()+metadata.getLastModified()).getBytes());
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof MediaItem) {
            MediaItem inItem = (MediaItem) inObject;
            return this.id == inItem.id;
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_list_item;
    }

    @Override
    public MediaItem.MediaItemViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new MediaItem.MediaItemViewHolder(view, adapter);
    }

    @Override
    public void unbindViewHolder(FlexibleAdapter adapter, MediaItem.MediaItemViewHolder holder, int position) {

    }

    @Override
    public void bindViewHolder(final FlexibleAdapter adapter, final MediaItem.MediaItemViewHolder holder, int position, List payloads) {
        Context context = holder.itemView.getContext();
        holder.mediaId = getId();

        // When user scrolls, this line binds the correct selection status
        holder.itemView.setActivated(adapter.isSelected(position));
        // Background, when bound the first time
        /*
        if (payloads.size() == 0) {
            Drawable drawable = DrawableUtils.getSelectableBackgroundCompat(
                    Color.WHITE, Color.parseColor("#dddddd"), //Same color of divider
                    DrawableUtils.getColorControlHighlight(context));
            holder.setBackgroundCompat(drawable);
        }*/

        // bg format
        holder.mFormat.setBackground(context.getDrawable(R.drawable.shape_round_format_normal));
        // bg coding
        holder.mSamplingRate.setBackground(context.getDrawable(R.drawable.shape_round_format_normal));
        // bg coding
        holder.mDuration.setBackground(context.getDrawable(R.drawable.shape_round_format_normal));
        //
        holder.mFileSize.setBackground(context.getDrawable(R.drawable.shape_round_format_normal));

        MediaItem listeningItem = null;
        if (MusicService.getRunningService()!=null) {
            listeningItem = MusicService.getRunningService().getListeningSong();
        }

        holder.setListening(this.equals(listeningItem));
        MediaBrowserActivity.MediaItemAdapter mediaItemAdapter =((MediaBrowserActivity.MediaItemAdapter)adapter);
        mediaItemAdapter.getGlide()
                .load(this)
                    .into(holder.mCoverArtView);
            if (adapter.isSelectAll() || adapter.isLastItemInActionMode()) {
                // Consume the Animation
                holder.setSelectionAnimation(adapter.isSelected(position));
            } else {
                // Display the current flip status
                holder.setAnimation(adapter.isSelected(position));
            }

        //will load tag if not loaded
        /*if(!isMetadataLoaded()) {
            bindViewHolder(adapter,holder);
            holder.mSamplingRate.setText("Loading...");
            MediaItemProvider.getInstance().readMetadata(getMetadata());
        }*/

        bindViewHolder(adapter,holder);
    }

    private void bindViewHolder(final FlexibleAdapter adapter, final MediaItem.MediaItemViewHolder holder) {
        if(holder.mediaId == getId()) {
            holder.mTitle.setText(getTitle());
            holder.mSubtitle.setText(getSubtitle());
            holder.mExtra.setText(metadata.getDisplayPath());
            holder.mDuration.setText(metadata.getAudioDurationAsString());
            holder.mFormat.setText(metadata.getAudioFormatInfo());
            holder.mFileSize.setText(metadata.getMediaSize());
            holder.mSamplingRate.setText(metadata.getAudioCoding());
            holder.mFormat.setBackground(getQualityBackground(holder));
            holder.mSamplingRate.setBackground(getQualityBackground(holder));
            holder.mFileSize.setBackground(getQualityBackground(holder));
            holder.mDuration.setBackground(getQualityBackground(holder));

            // In case of searchText matches with Title or with a field this will be highlighted
            if (adapter.hasFilter()) {
                holder.highlightSearch((String)adapter.getFilter(String.class));
            }
        }
    }

    protected Drawable getQualityBackground(final MediaItem.MediaItemViewHolder holder) {
        int quality = getMetadata().getAudioEncodingQuality();
        if (quality == MEDIA_QUALITY_HIRES) {
            return holder.getContext().getDrawable(R.drawable.shape_round_format_hires);
        }else if (quality == MEDIA_QUALITY_HIGH) {
            return holder.getContext().getDrawable(R.drawable.shape_round_format_high);
        }else if (quality == MEDIA_QUALITY_GOOD) {
            return holder.getContext().getDrawable(R.drawable.shape_round_format_good);
        }else if (quality == MEDIA_QUALITY_LOW) {
            return holder.getContext().getDrawable(R.drawable.shape_round_format_low);
        }
        return holder.getContext().getDrawable(R.drawable.shape_round_format_normal);
    }

    @Override
    public boolean filter(String constraint) {
        return (StringUtils.contains(StringUtils.trimToEmpty(getMetadata().getTitle()), constraint)
               || StringUtils.contains(StringUtils.trimToEmpty(getMetadata().getArtist()), constraint)
               || StringUtils.contains(StringUtils.trimToEmpty(getMetadata().getAlbum()), constraint)
               || StringUtils.contains(StringUtils.trimToEmpty(getMetadata().getGenre()), constraint)
               || StringUtils.contains(metadata.getDisplayPath(), constraint));
    }

    @Override
    public String toString() {
        return getTitle();
    }

    static class MediaItemViewHolder extends FlexibleViewHolder {
        int mediaId;
        TextView mTitle;
        TextView mSubtitle;
        TextView mExtra;
        TextView mFormat;
        TextView mSamplingRate;
        TextView mDuration;
        TextView mFileSize;
        // ImageView mCoverArtView;
        RoundedImageView mCoverArtView;
        Context mContext;

        public MediaItemViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.mContext = view.getContext();
            this.mTitle = view.findViewById(R.id.item_title);
            this.mSubtitle = view.findViewById(R.id.item_subtitle);
            this.mExtra = view.findViewById(R.id.item_extra);
            // meta data
            this.mDuration = view.findViewById(R.id.item_duration);
            this.mFormat = view.findViewById(R.id.item_format);
            this.mSamplingRate = view.findViewById(R.id.item_sampling_rate);
            this.mCoverArtView = view.findViewById(R.id.item_image_coverart);
            this.mFileSize = view.findViewById(R.id.item_file_size);
        }

        public Context getContext() {
            return mContext;
        }

        @Override
        public float getActivationElevation() {
            return UIUtils.dpToPx(itemView.getContext(), 4f);
        }

        @Override
        protected boolean shouldActivateViewWhileSwiping() {
            return false;//default=false
        }

        @Override
        public void scrollAnimators(@NonNull List<Animator> animators, int position, boolean isForward) {//Linear layout
            if (mAdapter.isSelected(position)) {
                AnimatorHelper.slideInFromRightAnimator(animators, itemView, mAdapter.getRecyclerView(), 0.5f);
            } else {
                AnimatorHelper.slideInFromLeftAnimator(animators,itemView,mAdapter.getRecyclerView(),0.5f);
            }
        }
/*
        public void setBackgroundCompat(Drawable drawable) {
            DrawableUtils.setBackgroundCompat(itemView, drawable);
        }
*/
        public void setSelectionAnimation(boolean selected) {
        }

        public void setAnimation(boolean selected) {
        }

        public void highlightSearch(String keyword) {
            UIUtils.highlightSearchKeyword(mTitle, String.valueOf(mTitle.getText()), keyword);
            UIUtils.highlightSearchKeyword(mSubtitle, String.valueOf(mSubtitle.getText()), keyword);
            UIUtils.highlightSearchKeyword(mExtra, String.valueOf(mExtra.getText()), keyword);
        }

        @Override
        protected void setDragHandleView(@NonNull View view) {
            if (mAdapter.isHandleDragEnabled()) {
                view.setVisibility(View.VISIBLE);
                super.setDragHandleView(view);
            } else {
                view.setVisibility(View.GONE);
            }
        }

        @Override
        protected boolean shouldAddSelectionInActionMode() {
            return false;//default=false
        }

        public void setListening(boolean isListening) {
            if(isListening) {
                // set border
                this.itemView.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.selector_item_border));
            }else {
                // reset border
                this.itemView.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.selector_item));
            }
        }
    }
}
