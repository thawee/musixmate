package apincer.android.uamp.item;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.List;

import apincer.android.uamp.R;
import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.ui.BrowserViewPagerFragment;
import apincer.android.uamp.ui.MediaTagEditorActivity;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.Utils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.helpers.AnimatorHelper;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.ISectionable;
import eu.davidea.flexibleadapter.utils.DrawableUtils;
import eu.davidea.flipview.FlipView;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * {@link apincer.android.uamp.item.AbstractItem} to benefit of the already
 * implemented methods (getter and setters).
 */
public class MediaItem extends AbstractItem<MediaItem.MediaItemViewHolder>
        implements ISectionable<MediaItem.MediaItemViewHolder, HeaderItem>, IFilterable, Serializable {

    /* The header of this item */
    HeaderItem header;
    private String path;
    private String displayPath;
    private String artist;
    private String album;
    private long duration;
    private Bitmap iconBitmap;

    private MediaItem(String id) {
        super(id);
        setDraggable(true);
        setSwipeable(true);
    }

    public MediaItem(String id, HeaderItem header) {
        this(id);
        this.header = header;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long dutation) {
        this.duration = dutation;
    }

    public Bitmap getIconBitmap() {
        return iconBitmap;
    }

    public void setIconBitmap(Bitmap iconBitmap) {
        this.iconBitmap = iconBitmap;
    }

    @Override
    public String getSubtitle() {
	if(StringUtils.isEmpty(getArtist()) && StringUtils.isEmpty(getAlbum())) {
		return "";
	} else if(StringUtils.isEmpty(getArtist())) {
        	return getAlbum();
	} else if(StringUtils.isEmpty(getAlbum())) {
        	return getArtist();
	}
        return getArtist()+" - "+getAlbum();
    }

    @Override
    public HeaderItem getHeader() {
        return header;
    }

    @Override
    public void setHeader(HeaderItem header) {
        this.header = header;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.recycler_media_item;
    }

    /*
    @Override
    public MediaItemViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new MediaItemViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    } */

    @Override
    public MediaItemViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new MediaItemViewHolder(view, adapter);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void bindViewHolder(final FlexibleAdapter adapter, MediaItemViewHolder holder, int position, List payloads) {
        Context context = holder.itemView.getContext();

        // Background, when bound the first time
        if (payloads.size() == 0) {
            Drawable drawable = DrawableUtils.getSelectableBackgroundCompat(
                    Color.WHITE, Color.parseColor("#dddddd"), //Same color of divider
                    DrawableUtils.getColorControlHighlight(context));
            DrawableUtils.setBackgroundCompat(holder.itemView, drawable);
            DrawableUtils.setBackgroundCompat(holder.frontView, drawable);
        }

        //if(iconBitmap!=null) {
        //    holder.mFlipView.setFrontImageBitmap(iconBitmap);
        //}

        // INNER ANIMATION ImageView - Handle Flip Animation
	if (adapter.isSelectAll() || adapter.isLastItemInActionMode()) {
		// Consume the Animation
		holder.mFlipView.flip(adapter.isSelected(position), 200L);
	} else {
        	// Display the current flip status
        	holder.mFlipView.flipSilently(adapter.isSelected(position));
	}

        // In case of searchText matches with Title or with a field this will be highlighted
        if (adapter.hasSearchText()) {
            Utils.highlightText(holder.mTitle, getTitle(), adapter.getSearchText());
            Utils.highlightText(holder.mSubtitle, getSubtitle(), adapter.getSearchText());
            Utils.highlightText(holder.mExtra, getDisplayPath(), adapter.getSearchText());
        } else if (((BrowserViewPagerFragment.BrowserFlexibleAdapter)adapter).isListeningTitle(getTitle(),getArtist(), getAlbum())) {
            Utils.highlightText(holder.mTitle, getTitle(), getTitle());
            Utils.highlightText(holder.mSubtitle, getSubtitle(), getSubtitle());
            holder.mExtra.setText(getDisplayPath());
            //holder.mSubtitle.setText(getSubtitle());
        } else {
            holder.mTitle.setText(getTitle());
            holder.mSubtitle.setText(getSubtitle());
            holder.mExtra.setText(getDisplayPath());
        }
        //holder.mSubtitle.setText(getSubtitle());
        holder.mDuration.setText(MediaProvider.formatDuration(getDuration()));
        //holder.mExtra.setText(getDisplayPath());
    }

    @Override
    public boolean filter(String constraint) {
        return (StringUtils.contains(getTitle(), constraint) 
               //|| StringUtils.contains(getArtist(), constraint)
               //|| StringUtils.contains(getAlbum(), constraint) 
               || StringUtils.contains(getSubtitle(), constraint) 
               || StringUtils.contains(getDisplayPath(), constraint));
        //return getTitle() != null && getTitle().toLowerCase().trim().contains(constraint) ||
        //        getSubtitle() != null && getSubtitle().toLowerCase().trim().contains(constraint);
    }

    @Override
    public String toString() {
        return title;
    }

    public String getDisplayPath() {
        return displayPath;
    }

    public void setDisplayPath(String displayPath) {
        this.displayPath = displayPath;
    }

    static final class MediaItemViewHolder extends FlexibleViewHolder {
        public boolean swiped = false;
        FlipView mFlipView;
        TextView mTitle;
        TextView mSubtitle;
        TextView mExtra;
        TextView mDuration;
        ImageView mHandleView;
        Context mContext;
        View frontView;
        View rearLeftView;
        View rearRightView;

        MediaItemViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.mContext = view.getContext();
            this.mTitle = (TextView) view.findViewById(R.id.title);
            this.mSubtitle = (TextView) view.findViewById(R.id.subtitle);
            this.mDuration = (TextView) view.findViewById(R.id.duration);
            this.mExtra = (TextView) view.findViewById(R.id.extra);
            this.mFlipView = (FlipView) view.findViewById(R.id.image);
            this.mFlipView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAdapter.mItemLongClickListener != null) {
                        mAdapter.mItemLongClickListener.onItemLongClick(getAdapterPosition());
                        Toast.makeText(mContext, "ImageClick on " + mTitle.getText() + " position " + getAdapterPosition(), Toast.LENGTH_SHORT).show();
                        toggleActivation();
                    }
                }
            });
            this.mHandleView = (ImageView) view.findViewById(R.id.row_handle);
            setDragHandleView(mHandleView);

            this.frontView = view.findViewById(R.id.front_view);
            this.rearLeftView = view.findViewById(R.id.rear_left_view);
            this.rearRightView = view.findViewById(R.id.rear_right_view);
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
        public void onClick(View view) {
            super.onClick(view);
            if(mAdapter.getMode()== SelectableAdapter.Mode.SINGLE) {
                int position = getAdapterPosition();
                if (!MediaTagEditorActivity.navigate(((BrowserViewPagerFragment.BrowserFlexibleAdapter)mAdapter).getActivity(), ((MediaItem) this.mAdapter.getItem(position)),position)) {
                    mAdapter.removeItem(position);
                }
            }
        }
/*
        @Override
        public boolean onLongClick(View view) {
            super.onLongClick(view);
            if(mAdapter.getMode()!=FlexibleAdapter.MODE_MULTI) {
                mAdapter.setMode(FlexibleAdapter.MODE_MULTI);
                mAdapter.toggleSelection(getAdapterPosition());
            }
            Toast.makeText(mContext, "LongClick on " + mTitle.getText() + " position " + getAdapterPosition(), Toast.LENGTH_SHORT).show();
            return false; //super.onLongClick(view);
        }
*/
        @Override
        public void toggleActivation() {
            super.toggleActivation();
            // Here we use a custom Animation inside the ItemView
            mFlipView.flip(mAdapter.isSelected(getAdapterPosition()));
        }

        @Override
        public float getActivationElevation() {
            return apincer.android.uamp.utils.Utils.dpToPx(itemView.getContext(), 4f);
        }

        @Override
        protected boolean shouldActivateViewWhileSwiping() {
            return false;//default=false
        }

        @Override
        protected boolean shouldAddSelectionInActionMode() {
            return false;//default=false
        }

        @Override
        public View getFrontView() {
            return frontView;
        }

        @Override
        public View getRearLeftView() {
            return rearLeftView;
        }

        @Override
        public View getRearRightView() {
            return rearRightView;
        }

        @Override
        public void scrollAnimators(@NonNull List<Animator> animators, int position, boolean isForward) {
            if (mAdapter.getRecyclerView().getLayoutManager() instanceof GridLayoutManager ||
                    mAdapter.getRecyclerView().getLayoutManager() instanceof StaggeredGridLayoutManager) {
                if (position % 2 != 0)
                    AnimatorHelper.slideInFromRightAnimator(animators, itemView, mAdapter.getRecyclerView(), 0.5f);
                else
                    AnimatorHelper.slideInFromLeftAnimator(animators, itemView, mAdapter.getRecyclerView(), 0.5f);
            } else {
                //Linear layout
                if (mAdapter.isSelected(position))
                    AnimatorHelper.slideInFromRightAnimator(animators, itemView, mAdapter.getRecyclerView(), 0.5f);
                else
                    AnimatorHelper.slideInFromLeftAnimator(animators, itemView, mAdapter.getRecyclerView(), 0.5f);
            }
        }

        @Override
        public void onItemReleased(int position) {
            swiped = (mActionState == ItemTouchHelper.ACTION_STATE_SWIPE);
            super.onItemReleased(position);
        }
    }

}
