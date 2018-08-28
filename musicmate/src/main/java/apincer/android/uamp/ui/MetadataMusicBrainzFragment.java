package apincer.android.uamp.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yalantis.filter.adapter.FilterAdapter;
import com.yalantis.filter.listener.FilterListener;
import com.yalantis.filter.model.FilterModel;
import com.yalantis.filter.widget.Filter;
import com.yalantis.filter.widget.FilterItem;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import apincer.android.dialog.BottomSheetDialog;
import apincer.android.listener.OnDialogButtonClickListener;
import apincer.android.uamp.Constants;
import apincer.android.uamp.R;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.jaudiotagger.MusicMateArtwork;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.model.RecordingItem;
import apincer.android.uamp.musicbrainz.MusicBrainz;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.service.MediaItemIntentService;
import apincer.android.uamp.ui.view.LinearDividerItemDecoration;
import apincer.android.uamp.utils.StringUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.items.IFlexible;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MetadataMusicBrainzFragment extends Fragment implements FlexibleAdapter.OnItemClickListener{
    private List<RecordingItem> itemList = new ArrayList<>();
    private FlexibleAdapter<RecordingItem> mAdapter = new FlexibleAdapter<RecordingItem>(itemList);
    private RecyclerView mRecyclerView;
    private View vEmptyView;
    private Filter mFilter;
    private String keywordTitle = null;
    private String keywordArtist = null;
    private String keywordAlbum = null;
    private String songTitle = null;
    private String songArtist = null;
    private String songAlbum = null;

    private FilterListener<Tag> mFilterListener = new FilterListener<Tag>() {
        boolean skip = false;
        @Override
        public void onFiltersSelected(ArrayList<Tag> filters) {
            keywordTitle = null;
            keywordArtist = null;
            keywordAlbum = null;
            skip = true;
            for(Tag tag: filters) {
                onFilterSelected(tag);
            }
            skip = false;
            doSearch();
        }

        @Override
        public void onNothingSelected() {
            keywordTitle = songTitle;
            keywordArtist = songArtist;
            keywordAlbum = songAlbum;
            doSearch();
        }

        @Override
        public void onFilterSelected(Tag item) {
            if("TITLE".equalsIgnoreCase(item.getType())) {
                keywordTitle = item.getText();
            }else if("ARTIST".equalsIgnoreCase(item.getType())) {
                keywordArtist = item.getText();
            }else if("ALBUM".equalsIgnoreCase(item.getType())) {
                keywordAlbum = item.getText();
            }
            if(!skip) {
                doSearch();
            }
        }

        @Override
        public void onFilterDeselected(Tag item) {
            if("TITLE".equalsIgnoreCase(item.getType())) {
                keywordTitle = null;
            }else if("ARTIST".equalsIgnoreCase(item.getType())) {
                keywordArtist = null;
            }else if("ALBUM".equalsIgnoreCase(item.getType())) {
                keywordAlbum = null;
            }
            doSearch();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_musicbraniz, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buildKeywords();
        mFilter = (Filter<Tag>) view.findViewById(R.id.filter);
        mFilter.setAdapter(new Adapter(getTags()));
        mFilter.setListener(mFilterListener);

        //the text to show when there's no selected items
        mFilter.setNoSelectedItemText("Songs on Musicbrainz");
        mFilter.build();

        mAdapter.addListener(this);

       // mAdapter.setDisplayHeadersAtStartUp(true);
       // mAdapter.setStickyHeaderElevation(4)
       //         .setStickyHeaders(true);

        //mLibraryAdapter.setAnimateToLimit(100);
        // When true, filtering on big list is very slow!
        mAdapter.setNotifyMoveOfFilteredItems(false)
                .setNotifyChangeOfUnfilteredItems(false)
                .setAnimationInitialDelay(100L)
                .setAnimationOnForwardScrolling(true)
                .setAnimationOnReverseScrolling(false)
                .setOnlyEntryAnimation(true);

        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setItemViewCacheSize(0); //Setting ViewCache to 0 (default=2) will animate items better while scrolling down+up with LinearLayout
        mRecyclerView.setWillNotCacheDrawing(true);
        mRecyclerView.setLayoutManager(new SmoothScrollLinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true); //Size of RV will not change
        // NOTE: Use default item animator 'canReuseUpdatedViewHolder()' will return true if
        // a Payload is provided. FlexibleAdapter is actually sending Payloads onItemChange.
        //mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        RecyclerView.ItemDecoration itemDecoration = new LinearDividerItemDecoration(getActivity(),getActivity().getColor(R.color.item_divider),1);
        //   RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, R.drawable.shadow_below);

        mRecyclerView.addItemDecoration(itemDecoration);
        vEmptyView = view.findViewById(R.id.empty_view);
    }

    private List<? extends Tag> getTags() {
        List<Tag> tags = new ArrayList();
        if(!StringUtils.isEmpty(songTitle)) {
            addToTag(tags, "TITLE", songTitle);
        }
        if(!StringUtils.isEmpty(songArtist)) {
            addToTag(tags, "ARTIST", songArtist);
        }
        if(!StringUtils.isEmpty(songAlbum)) {
            addToTag(tags, "ALBUM", songAlbum);
        }
        return tags;
    }

    private void buildKeywords() {
        //tags.clear();
        List<MediaItem> items = MetadataActivity.getEditItems();
        if(!items.isEmpty()) {
            MediaItem item = items.get(0);
            if(items.size()==1) {
                songTitle = item.getMetadata().getTitle();
                if(StringUtils.isEmpty(songTitle)) {
                    songTitle = MediaItemProvider.removeExtension(item.getPath());
                }
             //   tags.add(new Tag("TITLE", songTitle));
            }else if(StringUtils.isEmpty(item.getMetadata().getAlbum())){
                File file = new File(item.getPath());
                songAlbum = file.getParentFile().getName();
            }

            songArtist = item.getMetadata().getArtist();
            if(!StringUtils.isEmpty(songArtist) && !StringUtils.isEmpty(item.getMetadata().getAlbum())) {
                songAlbum = item.getMetadata().getAlbum();
            }
           // addToTag("ARTIST", songArtist);
           // addToTag("ALBUM", songAlbum);
        }

        keywordTitle = songTitle;
        keywordArtist = songArtist;
        keywordAlbum =songAlbum;

      //  return tags;
    }

    private void addToTag(List<Tag> tags, String type, String text) {
        if(StringUtils.isEmpty(text)) return;
        Tag tag = new Tag(type, text);
        if(!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        doSearch();
       // String key = MetadataActivity.getEditItems().get(0).getTitle();
        //String url = "http://www.google.com/search?tbm=isch&source=lnms&sa=X&q=" + key;
        //String url = "https://musicbrainz.org/search?type=recording&limit=20&method=indexed&query="+key;
    }

    public void doSearch() {
        Observable<Boolean> observable = Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<MediaItem> items = MetadataActivity.getEditItems();
                if(items.size()>0) {
                    MediaItem item = items.get(0);
                    List<RecordingItem> songs = MusicBrainz.findSongInfo(keywordTitle,keywordArtist,keywordAlbum);
                    itemList.clear();
                    itemList.addAll(songs);
                }
                return true;
            }
        });
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Boolean actionResult) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
                if(itemList.isEmpty()) {
                    vEmptyView.setVisibility(View.VISIBLE);
                    mRecyclerView.setVisibility(View.GONE);
                }else {
                    vEmptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mAdapter.updateDataSet(itemList);
                }
            }
        });
    }

    @Override
    public boolean onItemClick(View view, int position) {
        if (mAdapter == null) {
            return false;
        }
        IFlexible flexibleItem = mAdapter.getItem(position);
        if (flexibleItem instanceof RecordingItem) {
            final RecordingItem recordingItem = (RecordingItem) flexibleItem;
            //BottomDialog.Builder builder = new BottomDialog.Builder(getContext());
            BottomSheetDialog.Builder builder = new BottomSheetDialog.Builder(getContext());

            builder.setCancelable(true);
            builder.setNegativeText(R.string.cancel);
            builder.setPositiveText("UPDATE");
           // builder.setTitle("Song information on Musicbrainz");
            View cview = getActivity().getLayoutInflater().inflate(R.layout.view_musicbrainz_preview, null);
            TextView tTitle = cview.findViewById(R.id.title);
            tTitle.setText(recordingItem.title);
            TextView tArtist = cview.findViewById(R.id.artist);
            tArtist.setText(recordingItem.artist);
            TextView tAlbum = cview.findViewById(R.id.album);
            tAlbum.setText(recordingItem.album);
            TextView tGenre = cview.findViewById(R.id.genre);
            tGenre.setText(recordingItem.genre);
            TextView tYear = cview.findViewById(R.id.year);
            tYear.setText(recordingItem.year);
            ImageView cover = cview.findViewById(R.id.coverart);
            GlideApp.with(cview)
                    .asBitmap()
                    .load(recordingItem)
                    .placeholder(R.drawable.progress)
                    .error(R.drawable.ic_broken_image_black_24dp)
                    .into(new SimpleTarget<Bitmap>(MusicMateArtwork.MAX_ALBUM_ART_SIZE, MusicMateArtwork.MAX_ALBUM_ART_SIZE) {
                        @Override
                        public void onResourceReady(Bitmap resource,Transition<? super Bitmap> transition) {
                            cover.setImageBitmap(resource);
                            File theFilePath = MediaItemProvider.getDownloadPath(recordingItem.id+".png");
                            writePNG(theFilePath, resource);
                        }
                    });
            builder.setCustomView(cview);
            builder.onPositive(new OnDialogButtonClickListener() {
                @Override
                public void onClick(@NonNull DialogFragment dialog) {
                    // set sog and cover to songs
                    List<MediaItem> mediaItems = MetadataActivity.getEditItems();
                    boolean singleTrack = mediaItems.size()==1;

                   // MediaItemProvider.getInstance().saveArtworkToFile(mediaItems.get(0), theFilePath.getAbsolutePath());
                    String artworkPath = null;
                    File theFilePath = MediaItemProvider.getDownloadPath(recordingItem.id+".png");
                    if(theFilePath!=null && theFilePath.exists()) {
                        artworkPath = theFilePath.getAbsolutePath();
                    }

                    String title = String.valueOf(tTitle.getText());
                    String artist = String.valueOf(tArtist.getText());
                    String album = String.valueOf(tAlbum.getText());
                    String genre = String.valueOf(tGenre.getText());
                    String year = String.valueOf(tYear.getText());

                    for(MediaItem item:mediaItems) {
                        buildPendingTags(item, title, artist, album, genre, year, singleTrack);
                    }
                    MediaItemIntentService.startService(getContext(), Constants.COMMAND_SAVE,mediaItems, artworkPath);
                    dialog.dismiss();
                }
            });
            builder.show(getFragmentManager());
        }
        return false;
    }

    private void buildPendingTags(MediaItem item, String title, String artist, String album, String genre, String year, boolean singleTrack) {
        MediaMetadata tagUpdate = item.getPendingMetadataOrCreate();
        if(singleTrack) {
            tagUpdate.setTitle(StringUtils.trimToEmpty(title));
        }
        tagUpdate.setAlbum(StringUtils.trimToEmpty(album));
        tagUpdate.setArtist(StringUtils.trimToEmpty(artist));
        tagUpdate.setGenre(StringUtils.trimToEmpty(genre));
        tagUpdate.setYear(StringUtils.trimToEmpty(year));
    }

    private void writePNG(File file, Bitmap bitmap) {
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file); // getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private void buildPendingTags(RecordingItem recordingItem, MediaItem item, boolean singleTrack) {
        // update new tag with data from UI
        MediaMetadata tagUpdate = item.getPendingMetadataOrCreate();
        if(recordingItem!=null) {
            return;
        }
        if(singleTrack) {
            tagUpdate.setTitle(recordingItem.title);
            //tagUpdate.setTrack(recordingItem.);
        }
        tagUpdate.setAlbum(recordingItem.album);
//        tagUpdate.setAlbumArtist(buildTag(viewHolder.mAlbumArtistView, tagUpdate.getAlbumArtist()));
        tagUpdate.setArtist(recordingItem.artist);
       // tagUpdate.setComposer(recordingItem..buildTag(viewHolder.mComposerView, tagUpdate.getComposer()));
       // tagUpdate.setComment(buildTag(viewHolder.mCommentView, tagUpdate.getComment()));
       // tagUpdate.setGrouping(buildTag(viewHolder.mCountryView, tagUpdate.getGrouping()));
        tagUpdate.setGenre(recordingItem.genre);
        tagUpdate.setYear(recordingItem.year);
       // tagUpdate.setDisc(recordingItem.buildTag(viewHolder.mDiscView, tagUpdate.getDisc()));
    }

    private String buildTag(String text, String oldVal) {
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(text) ) {
            return text;
        }
        return oldVal;
    }

    class Adapter extends FilterAdapter<Tag> {

        Adapter(List<? extends Tag> items) {
            super(items);
        }

        @Override
        public FilterItem createView(int position, Tag item) {
            FilterItem filterItem = new FilterItem(getActivity());
            filterItem.setStrokeColor(getActivity().getColor(R.color.grey200));
            filterItem.setTextColor(getActivity().getColor(R.color.colorPrimaryDark));
            filterItem.setCheckedTextColor(ContextCompat.getColor(getActivity(), R.color.now_playing));
            filterItem.setColor(ContextCompat.getColor(getActivity(), android.R.color.white));
            filterItem.setCheckedColor(getActivity().getColor(R.color.material_color_blue_A700));
            filterItem.setText(item.getText());
            filterItem.deselect();

            return filterItem;
        }
    }

    private class Tag implements FilterModel {
        private String text;
        private String type;

        Tag(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Tag) {
                text.equals(((Tag) obj).getText());
            }
            return false;
        }
    }
}