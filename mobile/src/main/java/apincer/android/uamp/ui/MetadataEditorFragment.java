package apincer.android.uamp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment;
import com.yalantis.contextmenu.lib.MenuObject;
import com.yalantis.contextmenu.lib.MenuParams;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemClickListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apincer.android.uamp.Constants;
import apincer.android.uamp.FileManagerService;
import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.MediaTagParser;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.UIUtils;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import stream.customalert.CustomAlertDialogue;

import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_GOOD;
import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_HIGH;
import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_HIRES;
import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_LOW;

public class MetadataEditorFragment extends Fragment {
    private static final String TAG = LogHelper.makeLogTag(MetadataEditorFragment.class);
    public static final int REQUEST_GET_CONTENT_IMAGE = 555;

    private List<MediaItem> mediaItems;
    private ViewHolder viewHolder;
    private FloatingActionButton fabSaveAction;
    private Toolbar metadataToolbar;
    private ContextMenuDialogFragment fileMenusDialogFragment;
    private FragmentManager fragmentManager;

    private MaterialProgressBar mMaterialProgressBar;
    private Snackbar mSnackbar;

    private File pendingCoverartFile;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root= inflater.inflate(R.layout.fragment_editor, container, false);
        mediaItems = FileManagerService.getEditItems();
        viewHolder = new ViewHolder(root);
        setupToolbar(root);
        setupProgressBar(root);
        setupFab(root);
        toggleSaveFabAction();
        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        fragmentManager = getActivity().getSupportFragmentManager();
        setupMainMenus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(FileManagerService.ACTION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mFileManagerReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mFileManagerReceiver);
    }

    private void setupToolbar(View view) {
        metadataToolbar = view.findViewById(R.id.metadata_toolbar);
        metadataToolbar.inflateMenu(R.menu.menu_metadata);
        metadataToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        metadataToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        for(int i = 0; i < metadataToolbar.getMenu().size(); i++) {
            MenuItem item = metadataToolbar.getMenu().getItem(i);
            UIUtils.setColorFilter(item, getActivity().getColor(R.color.menu_delete_background));
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewHolder.bindViewHolder(buildDisplayTag());
        viewHolder.resetState();
        toggleSaveFabAction();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_GET_CONTENT_IMAGE) {
            try {
                if (data == null || data.getData() == null) {
                    return;
                }
                InputStream input = getApplicationContext().getContentResolver().openInputStream(data.getData());
                File outputDir = getApplicationContext().getCacheDir(); // context being the Activity pointer
                pendingCoverartFile = new File(outputDir, "tmp_cover_art");
                if (pendingCoverartFile.exists()) {
                    pendingCoverartFile.delete();
                }
                MediaItemProvider.getInstance().copy(input, pendingCoverartFile);
                GlideApp.with(getApplicationContext())
                        .load(pendingCoverartFile)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .signature(new ObjectKey(String.valueOf(pendingCoverartFile.lastModified())))
                        .into(viewHolder.mImageView);
                viewHolder.coverartChanged = true;
                toggleSaveFabAction();
            }catch (IOException ex) {
                LogHelper.e(TAG, ex);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int menuItemId = item.getItemId();
        switch (menuItemId) {
            case R.id.menu_metadata_file_main:
                fileMenusDialogFragment.show(fragmentManager, "FileContextMenuDialogFragment");
                break;
            case R.id.menu_label_pick_cover_image:
                doPickCoverart();
                break;
            case R.id.menu_label_save__cover_image:
                doSaveCoverart();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doPickCoverart() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,REQUEST_GET_CONTENT_IMAGE);
    }

    private void doSaveCoverart() {
            File theFilePath = MediaItemProvider.getDownloadPath(mediaItems.get(0).getTitle()+".png");
            MediaItemProvider.getInstance().saveArtworkToFile(mediaItems.get(0), theFilePath.getAbsolutePath());
            Snacky.builder().setActivity(getActivity())
                    .setText("Save Artwork to "+theFilePath.getName())
                    .setDuration(Snacky.LENGTH_LONG)
                    .setMaxLines(1)
                    .success()
                    .show();
    }

    private void setupMainMenus() {
        List<MenuObject> menuObjects = new ArrayList<>();
        MenuObject close = new MenuObject();
        close.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_close_black_24dp,getActivity().getColor(R.color.menu_close_icon_color)));
        menuObjects.add(close);

        MenuObject menu = new MenuObject("Move to Music Directory");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_move_to_inbox_black_24dp,getActivity().getColor(R.color.menu_file_icon_color)));
        menuObjects.add(menu);

        menu = new MenuObject("Delete from Android");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_delete_black_24dp,getActivity().getColor(R.color.menu_file_icon_color)));
        menuObjects.add(menu);

        menu = new MenuObject("Open on FileManager");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_folder_open_black_24dp,getActivity().getColor(R.color.menu_file_icon_color)));
        menuObjects.add(menu);

        menu = new MenuObject("Metadata by File Name");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_insert_drive_file_black_24dp,getActivity().getColor(R.color.menu_parser_icon_color)));
        //menu.setColor(getActivity().getColor(R.color.menu_delete_background));
        menuObjects.add(menu);

        menu = new MenuObject("Metadata by Path Hierarchy");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_file_tree_black_24dp,getActivity().getColor(R.color.menu_parser_icon_color)));
        menuObjects.add(menu);

        menu = new MenuObject("Swap Metadata (Title ~ Artist)");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_swap_horiz_black_24dp,getActivity().getColor(R.color.menu_tag_icon_color)));
        menuObjects.add(menu);

        menu = new MenuObject("Encoding Metadata");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_translate_black_24dp,getActivity().getColor(R.color.menu_tag_icon_color)));
        menuObjects.add(menu);

        menu = new MenuObject("Reformat Metadata");
        menu.setDrawable(UIUtils.getTintedDrawable(getActivity(), R.drawable.ic_text_format_black_24dp,getActivity().getColor(R.color.menu_tag_icon_color)));
        menuObjects.add(menu);

        MenuParams menuParams = new MenuParams();
        menuParams.setActionBarSize((int) getResources().getDimension(R.dimen.tool_bar_height));
        menuParams.setMenuObjects(menuObjects);
        menuParams.setClosableOutside(false);
        menuParams.setFitsSystemWindow(true);
        menuParams.setAnimationDuration(10);
        // set other settings to meet your needs
        fileMenusDialogFragment = ContextMenuDialogFragment.newInstance(menuParams);
        fileMenusDialogFragment.setItemClickListener(new OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(View clickedView, int position) {
                if(position==0) return;
                switch (position) {
                    case 1:
                        doMoveMediaItem();
                        break;
                    case 2:
                        doDeleteMediaItem();
                        break;
                    case 3:
                        doOpenFileManager();
                        break;
                    case 4:
                        doReadTags(MediaTagParser.READ_MODE.SMART);
                        break;
                    case 5:
                        doReadTags(MediaTagParser.READ_MODE.HIERARCHY);
                        break;
                    case 6:
                        doSwapArtistTitle();
                        break;
                    case 7:
                        doShowCharsetOptions();
                        break;
                    case 8:
                        doFormatTags();
                        break;
                }
            }
        });
    }

    private void startProgressBar() {
        if(mMaterialProgressBar!=null) {
            mMaterialProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void stopProgressBar() {
        if(mMaterialProgressBar!=null) {
            mMaterialProgressBar.setVisibility(View.GONE);
        }
    }

    private void setupProgressBar(View view) {
        mMaterialProgressBar = view.findViewById(R.id.progress_bar);
        mMaterialProgressBar.setVisibility(View.GONE);
    }

    private void setupFab(View view) {
        fabSaveAction = view.findViewById(R.id.fab_save_media);
        // save tag action
        fabSaveAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSaveMediaItem();
            }
        });
    }

    //
    private MediaMetadata buildDisplayTag() {
        MediaItem baseItem = mediaItems.get(0);
        MediaMetadata displayTag = baseItem.getPendingMetadata()==null?baseItem.getMetadata().clone():baseItem.getPendingMetadata();
        if(mediaItems.size()==1) {
            return displayTag;
        }

        for (int i=1;i<mediaItems.size();i++) {
            MediaItem item = mediaItems.get(i);
            MediaMetadata displayTag2 = item.getPendingMetadata()==null?item.getMetadata().clone():item.getPendingMetadata();
            if(displayTag2==null) {
                continue;
            }
            if(!StringUtils.equals(displayTag.getTitle(), displayTag2.getTitle())) {
                displayTag.setTitle(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getTrack(), displayTag2.getTrack())) {
                displayTag.setTrack(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getAlbum(), displayTag2.getAlbum())) {
                displayTag.setAlbum(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getArtist(), displayTag2.getArtist())) {
                displayTag.setArtist(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getAlbumArtist(), displayTag2.getAlbumArtist())) {
                displayTag.setAlbumArtist(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getGenre(), displayTag2.getGenre())) {
                displayTag.setGenre(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getYear(), displayTag2.getYear())) {
                displayTag.setYear(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getTrackTotal(), displayTag2.getTrackTotal())) {
                displayTag.setTrackTotal(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getDisc(), displayTag2.getDisc())) {
                displayTag.setDisc(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getDiscTotal(), displayTag2.getDiscTotal())) {
                displayTag.setDiscTotal(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getComment(), displayTag2.getComment())) {
                displayTag.setComment(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getComposer(), displayTag2.getComposer())) {
                displayTag.setComposer(StringUtils.MULTI_VALUES);
            }
            if(!StringUtils.equals(displayTag.getGrouping(), displayTag2.getGrouping())) {
                displayTag.setGrouping(StringUtils.MULTI_VALUES);
            }
        }
        return displayTag;
    }


    private void buildPendingTags(MediaItem item) {
        // update new tag with data from UI
        MediaMetadata tagUpdate = item.getPendingMetadata();
        if(tagUpdate==null) {
            tagUpdate = item.getMetadata().clone();
            item.setPendingMetadata(tagUpdate);
        }
        if(!viewHolder.tagChanged) {
            return;
        }
        if(mediaItems.size()==1) {
            tagUpdate.setLyrics(buildTag(viewHolder.mLyricsView, tagUpdate.getLyrics()));
        }

        tagUpdate.setTitle(buildTag(viewHolder.mTitleView, tagUpdate.getTitle()));
        tagUpdate.setTrack(buildTag(viewHolder.mTrackView, tagUpdate.getTrack()));
        tagUpdate.setAlbum(buildTag(viewHolder.mAlbumView, tagUpdate.getAlbum()));
        tagUpdate.setAlbumArtist(buildTag(viewHolder.mAlbumArtistView, tagUpdate.getAlbumArtist()));
        tagUpdate.setArtist(buildTag(viewHolder.mArtistView, tagUpdate.getArtist()));
        tagUpdate.setComposer(buildTag(viewHolder.mComposerView, tagUpdate.getComposer()));
        tagUpdate.setComment(buildTag(viewHolder.mCommentView, tagUpdate.getComment()));
        tagUpdate.setGrouping(buildTag(viewHolder.mCountryView, tagUpdate.getGrouping()));
        tagUpdate.setGenre(buildTag(viewHolder.mGenreView, tagUpdate.getGenre()));
        tagUpdate.setYear(buildTag(viewHolder.mYearView, tagUpdate.getYear()));
        tagUpdate.setDisc(buildTag(viewHolder.mDiscView, tagUpdate.getDisc()));
    }

    private String buildTag(TextView textView, String oldVal) {
        String text = getText(textView);
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(text) ) {
            return text;
        }
        return oldVal;
    }

    private String getText(TextView textView) {
        return StringUtils.trimToEmpty(String.valueOf(textView.getText()));
    }

    private class ViewHolder {
        private MediaMetadata displayTag;
        protected MaterialAutoCompleteTextView mTitleView;
        private MaterialAutoCompleteTextView mArtistView;
        private MaterialAutoCompleteTextView mAlbumView;
        private MaterialAutoCompleteTextView mAlbumArtistView;
        private MaterialAutoCompleteTextView mGenreView;
        private MaterialEditText mYearView;
        private MaterialAutoCompleteTextView mComposerView;
        private MaterialAutoCompleteTextView mCountryView;
        private MaterialEditText mCommentView;
        protected MaterialEditText mLyricsView;
        protected MaterialEditText mTrackView;
        private MaterialEditText mDiscView;
        private TextView mFormatView;
        private TextView mSamplerateView;
        private TextView mDurationView;
        private TextView mFileSizeView;
        private TextView mMediaPathView;
        private TextView mImageDimensionView;
        private ImageView mImageView;
        private View mainView;
        private View mEditorCardview;

        private ViewTextWatcher mTextWatcher;
        protected boolean tagChanged;
        protected boolean coverartChanged;

        public ViewHolder(View view) {
            tagChanged = false;
            coverartChanged = false;
            mTextWatcher =new ViewTextWatcher();
            mEditorCardview = view.findViewById(R.id.editorCardview);
            mainView = view.findViewById(R.id.main_view);
            mMediaPathView = view.findViewById(R.id.media_path);
            mFormatView = view.findViewById(R.id.media_format);
            mSamplerateView = view.findViewById(R.id.media_samplerate);
            mDurationView = view.findViewById(R.id.media_duration);
            mFileSizeView = view.findViewById(R.id.media_filesize);

            // title
            mTitleView = setupAutoCompleteTextView(view, R.id.title);

            // artist
            mArtistView = setupAutoCompleteTextView(view, R.id.artist);
            ArrayAdapter<String> artistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item, MediaItemProvider.getInstance().getAllArtists().toArray(new String[0]));
            mArtistView.setThreshold(2);//will start working from second character
            mArtistView.setAdapter(artistAdapter); //setting the adapter data into the AutoCompleteTextView

            // album
            mAlbumView = setupAutoCompleteTextView(view, R.id.album);
            ArrayAdapter<String> albumAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,MediaItemProvider.getInstance().getAllAlbums().toArray(new String[0]));
            mAlbumView.setThreshold(2);//will start working from second character
            mAlbumView.setAdapter(albumAdapter); //setting the adapter data into the AutoCompleteTextView

            // album artist
            mAlbumArtistView = setupAutoCompleteTextView(view, R.id.album_arist);
            ArrayAdapter<String> albumArtistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,MediaItemProvider.getInstance().getAllAlbumArtists().toArray(new String[0]));
            mAlbumArtistView.setThreshold(2);//will start working from second character
            mAlbumArtistView.setAdapter(albumArtistAdapter); //setting the adapter data into the AutoCompleteTextView

            // year
            mYearView = setupAutoEditTextView(view, R.id.year);

            // disc no
            mDiscView = setupAutoEditTextView(view, R.id.diskno);

            // track
            mTrackView = setupAutoEditTextView(view, R.id.track);

            // genre
            mGenreView = setupAutoCompleteTextView(view, R.id.genre);
            ArrayAdapter<String> genreAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,MediaItemProvider.getInstance().getAllGenres().toArray(new String[0]));
            mGenreView.setThreshold(2);//will start working from second character
            mGenreView.setAdapter(genreAdapter); //setting the adapter data into the AutoCompleteTextView

            // country
            mCountryView = setupAutoCompleteTextView(view, R.id.group);

            //composer
            mComposerView = setupAutoCompleteTextView(view, R.id.composer);

            // comment
            mCommentView = setupAutoEditTextView(view, R.id.comment);

            // lyrics
            mLyricsView = setupAutoEditTextView(view, R.id.lyrics);
            mImageDimensionView = view.findViewById(R.id.image_dimension);
            mImageView = view.findViewById(R.id.image);
            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doShowImageOptions();;
                }
            });
        }

        private MaterialEditText setupAutoEditTextView(View view, int vId) {
            MaterialEditText textView = view.findViewById(vId);
            textView.addTextChangedListener(mTextWatcher);
            return textView;
        }

        private MaterialAutoCompleteTextView setupAutoCompleteTextView(View view, int vId) {
            MaterialAutoCompleteTextView textView = view.findViewById(vId);
            textView.addTextChangedListener(mTextWatcher);
            return textView;
        }

        void resetState() {
            tagChanged = false;
            coverartChanged = false;
            mEditorCardview.clearFocus();
        }

        public void bindViewHolder(MediaMetadata mediaTag) {
            this.displayTag = mediaTag;
            // title
            mTitleView.setText(StringUtils.trimToEmpty(displayTag.getTitle()));
            // artist
            mArtistView.setText(StringUtils.trimToEmpty(displayTag.getArtist()));

            //genre
            mGenreView.setText(StringUtils.trimToEmpty(displayTag.getGenre()));

            // album
            mAlbumView.setText(StringUtils.trimToEmpty(displayTag.getAlbum()));
            // album artist
            mAlbumArtistView.setText(StringUtils.trimToEmpty(displayTag.getAlbumArtist()));

            mCountryView.setText(StringUtils.trimToEmpty(displayTag.getGrouping()));
            // album artist
            mCommentView.setText(StringUtils.trimToEmpty(displayTag.getComment()));
            // album artist
            mTrackView.setText(StringUtils.trimToEmpty(displayTag.getTrack()));
            // path
           mMediaPathView.setText(displayTag.getDisplayPath());
            mFormatView.setText(displayTag.getAudioCodecInfo());
            mSamplerateView.setText(displayTag.getAudioCoding());
            mDurationView.setText(displayTag.getAudioDurationAsString());
            mFileSizeView.setText(displayTag.getMediaSize());
            Bitmap art = MediaItemProvider.getInstance().getArtwork(displayTag.getMediaPath());
            if(art !=null) {
                int height = art.getHeight();
                int width = art.getWidth();
                mImageDimensionView.setText(width+" x "+height +" px");
                mImageView.setImageBitmap(art);
                Palette palette = Palette.from(art).generate();
                int backgroundColor = palette.getDominantColor(getApplicationContext().getColor(R.color.grey600));

                mainView.setBackgroundColor(backgroundColor);
            }else {
                mImageView.setImageDrawable(getApplicationContext().getDrawable(R.drawable.ic_music));
                mImageDimensionView.setText("No Cover Art");
            }
            updateAudioFormatQualityView(displayTag);
        }

        private void updateAudioFormatQualityView(MediaMetadata metadata) {
            int quality = metadata.getAudioEncodingQuality();
            if (quality == MEDIA_QUALITY_HIRES) {
                updateAudioFormatQualityView(R.drawable.shape_round_format_hires);
            }else if (quality == MEDIA_QUALITY_HIGH) {
                updateAudioFormatQualityView(R.drawable.shape_round_format_high);
            }else if (quality == MEDIA_QUALITY_GOOD) {
                updateAudioFormatQualityView(R.drawable.shape_round_format_good);
            }else if (quality == MEDIA_QUALITY_LOW) {
                updateAudioFormatQualityView(R.drawable.shape_round_format_low);
            }else {
                updateAudioFormatQualityView(R.drawable.shape_round_format_normal);
            }
        }

        private void updateAudioFormatQualityView( int drawableId) {
            mFormatView.setBackground(getApplicationContext().getDrawable(drawableId));
            mSamplerateView.setBackground(getApplicationContext().getDrawable(drawableId));
            mDurationView.setBackground(getApplicationContext().getDrawable(drawableId));
            mFileSizeView.setBackground(getApplicationContext().getDrawable(drawableId));
        }

        public  class ViewTextWatcher implements TextWatcher {
            public ViewTextWatcher() {
            }
            public void afterTextChanged(Editable editable) {
                tagChanged = true;
                toggleSaveFabAction();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        }
    }

    private void doShowImageOptions() {
        ArrayList<String> other = new ArrayList<String>();
        other.add("Pick CoverArt from Image");
        other.add("Save CoverArt to Image");

        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(getActivity())
                .setStyle(CustomAlertDialogue.Style.ACTIONSHEET)
                .setTitle("Cover Art Options...")
                .setTitleColor(R.color.text_default)
                .setCancelText(getString(R.string.alert_btn_cancel))
                .setOnCancelClicked(new CustomAlertDialogue.OnCancelClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        dialog.dismiss();
                    }
                })
                .setOthers(other)
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        String selection = adapterView.getItemAtPosition(i).toString();
                        switch (selection)
                        {
                            case "Pick CoverArt from Image":
                                CustomAlertDialogue.getInstance().dismiss();
                                doPickCoverart();
                                break;
                            case "Save CoverArt to Image":
                                CustomAlertDialogue.getInstance().dismiss();
                                doSaveCoverart();
                                break;
                        }
                    }
                })
                .setDecorView(getActivity().getWindow().getDecorView())
                .build();
        alert.show();
    }

    private void toggleSaveFabAction(){
        if(viewHolder.tagChanged || viewHolder.coverartChanged) {
            ViewCompat.animate(fabSaveAction)
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f).setDuration(200)
                    .setStartDelay(300L)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(View view) {
                            view.setVisibility(View.VISIBLE);
                        }
                    })
                    .start();
        }else {
            ViewCompat.animate(fabSaveAction)
                    .scaleX(0f).scaleY(0f)
                    .alpha(0f).setDuration(100)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            view.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    private void doDeleteMediaItem() {
        String text = "Delete ";
        if(mediaItems.size()>1) {
            text = text + mediaItems.size() + " songs?";
        }else {
            text = text + "'"+mediaItems.get(0).getTitle()+"' song?";
        }
        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(getActivity())
                .setStyle(CustomAlertDialogue.Style.DIALOGUE)
                .setCancelable(false)
                .setTitle("Delete Songs")
                .setMessage(text)
                .setPositiveText("Confirm")
                .setPositiveColor(R.color.negative)
                .setPositiveTypeface(Typeface.DEFAULT_BOLD)
                .setOnPositiveClicked(new CustomAlertDialogue.OnPositiveClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        for (MediaItem item : mediaItems) {
                            FileManagerService.addToDelete(item);
                        }
                        // Construct our Intent specifying the Service
                        Intent intent = new Intent(getActivity(), FileManagerService.class);
                        // Add extras to the bundle
                        intent.putExtra(Constants.KEY_COMMAND, Constants.COMMAND_DELETE);
                        // Start the service
                        getActivity().startService(intent);
                        //setResultData(Constants.COMMAND_DELETE,null,null);
                        dialog.dismiss();
                    }
                })
                .setNegativeText("Cancel")
                .setNegativeColor(R.color.positive)
                .setOnNegativeClicked(new CustomAlertDialogue.OnNegativeClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        dialog.dismiss();
                    }
                })
                .setDecorView(getActivity().getWindow().getDecorView())
                .build();
        alert.show();
    }

    private void doMoveMediaItem() {
        String text = "Move ";
        if(mediaItems.size()>1) {
            text = text + mediaItems.size() + " songs to Music Library?";
        }else {
            text = text + "'"+mediaItems.get(0).getTitle()+"' song to Music Library?";
        }
        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(getActivity())
                .setStyle(CustomAlertDialogue.Style.DIALOGUE)
                .setCancelable(false)
                .setTitle("Move Songs")
                .setMessage(text)
                .setPositiveText("Confirm")
                .setPositiveColor(R.color.negative)
                .setPositiveTypeface(Typeface.DEFAULT_BOLD)
                .setOnPositiveClicked(new CustomAlertDialogue.OnPositiveClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        for(MediaItem item:mediaItems) {
                            FileManagerService.addToMove(item);
                        }
                        startProgressBar();
                        // Construct our Intent specifying the Service
                        Intent intent = new Intent(getActivity(), FileManagerService.class);
                        // Add extras to the bundle
                        intent.putExtra(Constants.KEY_COMMAND, Constants.COMMAND_MOVE);
                        // Start the service
                        getActivity().startService(intent);
                       // setResultData(Constants.COMMAND_MOVE,null,null);
                        dialog.dismiss();
                    }
                })
                .setNegativeText("Cancel")
                .setNegativeColor(R.color.positive)
                .setOnNegativeClicked(new CustomAlertDialogue.OnNegativeClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        dialog.dismiss();
                    }
                })
                .setDecorView(getActivity().getWindow().getDecorView())
                .build();
        alert.show();
    }

    private void doSaveMediaItem() {
        if(!(viewHolder.tagChanged || viewHolder.coverartChanged)) {
            return;
        }
        for(MediaItem item:mediaItems) {
            item.setPendingArtworkPath(null);
            if(viewHolder.coverartChanged && pendingCoverartFile!=null) {
                item.setPendingArtworkPath(pendingCoverartFile.getAbsolutePath());
            }
            if(viewHolder.tagChanged) {
                buildPendingTags(item);
            }
            FileManagerService.addToSave(item);
        }
        startProgressBar();
        // Construct our Intent specifying the Service
        Intent intent = new Intent(getActivity(), FileManagerService.class);
        // Add extras to the bundle
        intent.putExtra(Constants.KEY_COMMAND, Constants.COMMAND_SAVE);
        // Start the service
        getActivity().startService(intent);
        //setResultData(Constants.COMMAND_SAVE,null,null);
        viewHolder.resetState();
        toggleSaveFabAction();
    }

    private void doOpenFileManager() {
        if(mediaItems.size()>1) {
            return;
        }
        File mediaFile = new File(mediaItems.get(0).getPath());
        Uri uri = Uri.parse(mediaFile.getParent());
        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("pl.solidexplorer2");
        if(intent!=null) {
            intent.setAction("org.openintents.action.VIEW_DIRECTORY");
            intent.setData(uri);
        }else {
            intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "resource/folder");
        }
        startActivity(intent);
    }

    private void doFormatTags() {
        startProgressBar();
        for(MediaItem mItem:mediaItems) {
            MediaMetadata tag = mItem.getPendingMetadata();
            if(tag==null) {
                tag = mItem.getMetadata().clone();
                mItem.setPendingMetadata(tag);
            }
            tag.setTitle(MediaItemProvider.formatTitle(tag.getTitle()));
            tag.setArtist(MediaItemProvider.formatTitle(tag.getArtist()));
            tag.setAlbum(MediaItemProvider.formatTitle(tag.getAlbum()));
            tag.setAlbumArtist(MediaItemProvider.formatTitle(tag.getAlbumArtist()));
            tag.setGenre(MediaItemProvider.formatTitle(tag.getGenre()));
            // clean albumArtist if same value as artist
            if(tag.getAlbumArtist().equals(tag.getAlbum())) {
                tag.setAlbumArtist("");
            }
            // if album empty, add single
            if(StringUtils.isEmpty(tag.getAlbum()) && !StringUtils.isEmpty(tag.getArtist())) {
                tag.setAlbum(tag.getArtist()+" - Single");
            }
        }
        viewHolder.bindViewHolder(buildDisplayTag());
        stopProgressBar();
        toggleSaveFabAction();
    }

    private void doChangeCharset(String charset) {
        startProgressBar();
        if(StringUtils.isEmpty(charset)) {
            // re-load from media file
            for (MediaItem item : mediaItems) {
                MediaItemProvider.getInstance().readMetadata(item.getMetadata());
            }
        }else {
            for(MediaItem item: mediaItems) {
                MediaMetadata tag = item.getPendingMetadata();
                if(tag==null) {
                    tag = item.getMetadata().clone();
                    item.setPendingMetadata(tag);
                }
                tag.setTitle(StringUtils.encodeText(tag.getTitle(),charset));
                tag.setArtist(StringUtils.encodeText(tag.getArtist(),charset));
                tag.setAlbum(StringUtils.encodeText(tag.getAlbum(),charset));
                tag.setAlbumArtist(StringUtils.encodeText(tag.getAlbumArtist(),charset));
                tag.setGenre(StringUtils.encodeText(tag.getGenre(),charset));
                tag.setComment(StringUtils.encodeText(tag.getComment(),charset));
                tag.setComposer(StringUtils.encodeText(tag.getComment(),charset));
                tag.setLyrics(StringUtils.encodeText(tag.getLyrics(),charset));
                tag.setGrouping(StringUtils.encodeText(tag.getGrouping(),charset));
            }
        }
        viewHolder.bindViewHolder(buildDisplayTag());
        stopProgressBar();
    }

    private void doReadTags(MediaTagParser.READ_MODE mode) {
        //enable for single item only
        MediaTagParser reader = new MediaTagParser();
        for(MediaItem item:mediaItems) {
            String mediaPath =  item.getPath();
            File file = new File(mediaPath);
            if(!file.exists()) continue;
            if(mode == MediaTagParser.READ_MODE.SMART) {
                reader.parse(item, MediaTagParser.READ_MODE.SMART);
            }else {
                // menu_label_hierarchy
                reader.parse(item, MediaTagParser.READ_MODE.HIERARCHY);
            }
            /*
            if(newTag!=null) {
                MediaMetadata tag = item.getPendingMetadata();
                if(tag==null) {
                    tag = item.getMetadata().clone();
                    item.setPendingMetadata(tag);
                }
                if(!StringUtils.isEmpty(newTag.getTitle())) {
                    tag.setTitle(newTag.getTitle());
                }
                if(!StringUtils.isEmpty(newTag.getArtist())) {
                    tag.setArtist(newTag.getArtist());
                }
                if(!StringUtils.isEmpty(newTag.getAlbum())) {
                    tag.setAlbum(newTag.getAlbum());
                }
                if(!StringUtils.isEmpty(newTag.getAlbumArtist())) {
                    tag.setAlbumArtist(newTag.getAlbumArtist());
                }
                if(!StringUtils.isEmpty(newTag.getTrack())) {
                    tag.setTrack(newTag.getTrack());
                }
            } */
        }
        viewHolder.bindViewHolder(buildDisplayTag());
    }

    private void doShowCharsetOptions() {
        final Map<Integer,String> charsetList = new HashMap<>();
        MediaMetadata matadata = mediaItems.get(0).getMetadata();
        String title = matadata.getTitle()+ StringUtils.ARTIST_SEP+ MusicService.getSubtitle(matadata.getAlbum(),matadata.getArtist());

        ArrayList<String> other = new ArrayList<>();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Set<String> encodings = prefs.getStringSet("preference_matadata_encodings",null);
        if(encodings==null || encodings.isEmpty() ) {
            return;
        }

        int indx=0;
        for(String charset:encodings) {
            other.add(StringUtils.encodeText(title, charset));
            charsetList.put(indx++, charset);
        }

        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(getActivity())
                .setStyle(CustomAlertDialogue.Style.ACTIONSHEET)
                .setTitle("Please select readable metadata...")
                .setTitleColor(R.color.text_default)
                .setCancelText(getString(R.string.alert_btn_cancel))
                .setOnCancelClicked(new CustomAlertDialogue.OnCancelClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        dialog.dismiss();
                    }
                })
                .setOthers(other)
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        CustomAlertDialogue.getInstance().dismiss();
                        doChangeCharset(charsetList.get(i));
                    }
                })
                .setDecorView(getActivity().getWindow().getDecorView())
                .build();
        alert.show();
    }

    private void doSwapArtistTitle() {
        for(MediaItem item:mediaItems) {
            MediaMetadata tag = item.getPendingMetadata();
            if(tag==null) {
                tag = item.getMetadata().clone();
                item.setPendingMetadata(tag);
            }
            String title = StringUtils.trimToEmpty(tag.getTitle());
            String artist = StringUtils.trimToEmpty(tag.getArtist());
            tag.setTitle(MediaItemProvider.formatTitle(artist));
            tag.setArtist(MediaItemProvider.formatTitle(title));
        }
        viewHolder.bindViewHolder(buildDisplayTag());
    }

    private Context getApplicationContext() {
        return getActivity().getApplicationContext();
    }

    // Define the callback for what to do when data is received
    private BroadcastReceiver mFileManagerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            if (resultCode == Activity.RESULT_OK) {
                String command = intent.getStringExtra("command");
                String status = intent.getStringExtra("status");
                String message = intent.getStringExtra("message");
                int total = intent.getIntExtra("totalItems",-1);
                int current = intent.getIntExtra("currentItem",-1);

                if(current >= total) {
                    stopProgressBar();
                }
                if("success".equalsIgnoreCase(status)) {
                    if(current >= total) {
                        // remove snackbar
                        if(mSnackbar!=null) {
                            mSnackbar.dismiss();
                            mSnackbar = null;
                        }
                        if (apincer.android.uamp.Constants.COMMAND_DELETE.equalsIgnoreCase(command)) {
                            // back to main activity after delete all
                            getActivity().onBackPressed();
                        }else {
                            // refresh view
                            viewHolder.bindViewHolder(buildDisplayTag()); // silent mode
                            viewHolder.resetState();
                            toggleSaveFabAction();
                        }
                    }
                }
                else if("start".equalsIgnoreCase(status)) {
                    // String text = current + "/" + total+ " - " + message;
                    if(mSnackbar!=null) {
                        mSnackbar.dismiss();
                        mSnackbar = null;
                    }
                    mSnackbar = Snacky.builder().setActivity(getActivity())
                            .setText(message)
                            .setDuration(Snacky.LENGTH_INDEFINITE)
                            .setMaxLines(1)
                            .info();
                    mSnackbar.show();
                }else if("fail".equalsIgnoreCase(status)) {
                    if(mSnackbar!=null) {
                        mSnackbar.dismiss();
                        mSnackbar= null;
                    }
                    mSnackbar = Snacky.builder().setActivity(getActivity())
                            .setText(message)
                            .setDuration(Snacky.LENGTH_LONG)
                            .setMaxLines(1)
                            .error();
                    mSnackbar.show();
                }
            }
        }
    };
}