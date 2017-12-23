/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package apincer.android.uamp.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.intentfilter.androidpermissions.PermissionManager;
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.michaelevans.colorart.library.ColorArt;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.flexibleadapter.AlbumInfo;
import apincer.android.uamp.flexibleadapter.MediaItem;
import apincer.android.uamp.flexibleadapter.RecordingItem;
import apincer.android.uamp.musicbrainz.MusicBrainz;
import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.provider.MediaTag;
import apincer.android.uamp.provider.TagReader;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.UIUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * A full screen editor that shows the current  music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class MediaTagEditorActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(MediaTagEditorActivity.class);

    private static final String ANSI_CHARSET="ISO8859_1";

    public static final int REQUEST_EDIT_MEDIA_TAG = 222;
    public static final String ARG_MEDIA_PATH = "media_path";
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private MediaProvider mediaStoreHelper;

    private FlexibleAdapter mAutoSongAdapter;
    private FlexibleAdapter mAutoCoverAdapter;

    private boolean tagChanged;
    private List<MediaItem> mediaItems;
    private MediaTag mediaTag;

    private MaterialAutoCompleteTextView mTitleView;
    private MaterialAutoCompleteTextView mArtistView;
    private MaterialAutoCompleteTextView mAlbumView;
    private MaterialAutoCompleteTextView mAlbumArtistView;
    private MaterialAutoCompleteTextView mGenreView;
    private MaterialEditText mYearView;
    private MaterialAutoCompleteTextView mCountryView;
    private MaterialEditText mCommentView;
    private MaterialEditText mLyricsView;
    private MaterialEditText mTrackView;
    private MaterialEditText mDiscView;
    private TextView mFormatView;
    private TextView mSamplerateView;
    private TextView mBitrateView;
    private TextView mDurationView;
    private TextView mFileSizeView;
    private TextView mMediaPathView;
    private View mainView;
    private DialogHelper mDialogHelper;

    //auto tag
    private RecyclerView mMatchTagView;
    private RecyclerView mMatchCoverImages;

    private View autoCardview;
    private View editorCardview;

    private ViewTextWatcher mTextWatcher;

    private boolean fabStatus = false;
    private FloatingActionButton fabSaveAction;

    private Toolbar mToolbar;

    public static boolean navigateForResult(Activity context, MediaItem item) {
        Intent intent = new Intent(context, MediaTagEditorActivity.class);
        MediaProvider mediaMusicTagHelper = MediaProvider.getInstance();
        if (mediaMusicTagHelper.isMediaFileExist(item)) {
            MusicService.getRunningService().clearMediaItem();
            MusicService.getRunningService().addMediaItem(item,true);
            ActivityCompat.startActivityForResult(context, intent, REQUEST_EDIT_MEDIA_TAG, null);
            return true;
        }else {
            try {
                mediaMusicTagHelper.deleteMediaFile(item.getPath());
                mediaMusicTagHelper.deleteFromMediaStore(item.getPath());
            }catch (Exception ex) {}
            new DialogHelper(context).displayFailedAlert(R.string.alert_error_title,context.getString(R.string.alert_invalid_media_file, item.getTitle()),-1);
            return false;
        }
    }

    public static boolean navigateForResult(Activity context, List<MediaItem> items) {
        if(MusicService.getRunningService()!=null) {
            MusicService.getRunningService().clearMediaItem();
            for (MediaItem  item: items) {
                MusicService.getRunningService().addMediaItem(item);
            }
            Intent intent = new Intent(context, MediaTagEditorActivity.class);
            ActivityCompat.startActivityForResult(context, intent, REQUEST_EDIT_MEDIA_TAG, null);
            return true;
        }
        return false;
    }

    @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            initActivityTransitions();
            setContentView(R.layout.activity_editor);

            ViewCompat.setTransitionName(findViewById(R.id.app_bar_layout), ARG_MEDIA_PATH);
            supportPostponeEnterTransition();

            MediaProvider.initialize(getApplicationContext());
            mediaStoreHelper = MediaProvider.getInstance();

            mToolbar = findViewById(R.id.toolbar);
            mToolbar.setBackgroundResource(R.drawable.shape_round_format_black);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            mDialogHelper = new DialogHelper(this);

            setUpAutoView();

            mediaItems = new ArrayList<>();
            mediaItems.addAll(MusicService.getRunningService().popMediaItem());
            populateMediaTag(mediaItems);

            if (mediaTag == null) {
                // should stop
                saveResult("INVALID", getIntent().getStringExtra(ARG_MEDIA_PATH),null);
                finish();
                return;
            }

            collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
            collapsingToolbarLayout.setTitle("");
            collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent, getTheme()));

            fabSaveAction = this.findViewById(R.id.fab_save_media);
            // save tag action
            fabSaveAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doSaveMediaItem();
                }
            });

            mainView = findViewById(R.id.main_view);
            editorCardview = findViewById(R.id.editorCardview);
            autoCardview  = findViewById(R.id.autoCardview);

        toggleSaveFabAction(); //hide untill tag changed

        mMediaPathView = findViewById(R.id.media_path);
        mMediaPathView.setText(mediaTag.getDisplayPath());

        //mTagContainer = findViewById(R.id.tagContainer);

        mFormatView = findViewById(R.id.media_format);
        mFormatView.setText(mediaItems.get(0).getAudioCodingFormat());

        mSamplerateView = findViewById(R.id.media_samplerate);
        mSamplerateView.setText(mediaTag.getAudioCoding());

        mBitrateView = findViewById(R.id.media_bitrate);
        mBitrateView.setText(mediaTag.getAudioCompressBitrate());

        mDurationView = findViewById(R.id.media_duration);
        mDurationView.setText(mediaTag.getAudioDurationAsString());

        mFileSizeView = findViewById(R.id.media_filesize);
        mFileSizeView.setText(mediaTag.getMediaSize());

        updateAudioFormatQualityView(mediaTag.getQuality());

        mTextWatcher =new ViewTextWatcher() ;

        // title
            mTitleView = findViewById(R.id.title);
            mTitleView.setText(mediaTag.getTitle());
            setTitle("");
            mTitleView.addTextChangedListener(mTextWatcher);

            // artist
            mArtistView = findViewById(R.id.artist);
            mArtistView.setText(mediaTag.getArtist());
            mArtistView.addTextChangedListener(mTextWatcher);
            ArrayAdapter<String> artistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,mediaStoreHelper.getArtistsAsArray());
            mArtistView.setThreshold(2);//will start working from second character
            mArtistView.setAdapter(artistAdapter); //setting the adapter data into the AutoCompleteTextView

            // album
            mAlbumView = findViewById(R.id.album);
            mAlbumView.setText(mediaTag.getAlbum());
            mAlbumView.addTextChangedListener(mTextWatcher);
        ArrayAdapter<String> albumAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,mediaStoreHelper.getAlbumAsArray());
        mAlbumView.setThreshold(2);//will start working from second character
        mAlbumView.setAdapter(albumAdapter); //setting the adapter data into the AutoCompleteTextView

            // album artist
            mAlbumArtistView = findViewById(R.id.album_arist);
            mAlbumArtistView.setText(mediaTag.getAlbumArtist());
            mAlbumArtistView.addTextChangedListener(mTextWatcher);
        ArrayAdapter<String> albumArtistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,mediaStoreHelper.getAlbumArtistAsArray());
        mAlbumArtistView.setThreshold(2);//will start working from second character
        mAlbumArtistView.setAdapter(albumArtistAdapter); //setting the adapter data into the AutoCompleteTextView

        // year
        mYearView = findViewById(R.id.year);
        mYearView.setText(mediaTag.getYear());
        mYearView.addTextChangedListener(mTextWatcher);

            // disc no
        mDiscView = findViewById(R.id.diskno);
        mDiscView.setText(mediaTag.getDisc());
        mDiscView.addTextChangedListener(mTextWatcher);

            // track
            mTrackView = findViewById(R.id.track);
        mTrackView.setText(mediaTag.getTrack());
        mTrackView.addTextChangedListener(mTextWatcher);

            // genre
            mGenreView = findViewById(R.id.genre);
            mGenreView.setText(mediaTag.getGenre());
            mGenreView.addTextChangedListener(mTextWatcher);

            // country
            mCountryView = findViewById(R.id.group);
            mCountryView.setText(mediaTag.getGrouping());
            mCountryView.addTextChangedListener(mTextWatcher);

            String [] langs = {"classic","eng","tha"};
            ArrayAdapter<String> countryAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,langs);
            mCountryView.setThreshold(0);//will start working from first character
            mCountryView.setAdapter(countryAdapter); //setting the adapter data into the AutoCompleteTextView

            // comment
            mCommentView = findViewById(R.id.comment);
            mCommentView.setText(mediaTag.getComment());
            mCommentView.addTextChangedListener(mTextWatcher);

            // lyrics
            mLyricsView = findViewById(R.id.lyrics);
            mLyricsView.setText(mediaTag.getLyrics());
            mLyricsView.addTextChangedListener(mTextWatcher);

            // always get artwork from first item
        Bitmap art = mediaStoreHelper.getArtwork(mediaItems.get(0));
        if(art !=null) {
            final ImageView image = findViewById(R.id.image);
            image.setImageBitmap(art);
            ColorArt colorArt = new ColorArt(art);
            int primaryColor = colorArt.getPrimaryColor();
            int backgroundColor = colorArt.getPrimaryColor();
            backgroundColor = UIUtils.darken(backgroundColor , 0.3);
          //  int lightPrimaryColor = UIUtils.lighten(primaryColor , 0.9);
            primaryColor = UIUtils.darken(primaryColor , 0.7);

            //mTagContainer.setBackgroundColor(backgroundColor);
            mainView.setBackgroundColor(backgroundColor);
           // mMediaPathView.setTextColor(lightPrimaryColor);

            mTitleView.setTextColor(primaryColor);
            mTitleView.setFloatingLabelTextColor(backgroundColor);
            mArtistView.setTextColor(primaryColor);
            mArtistView.setFloatingLabelTextColor(backgroundColor);
            mAlbumView.setTextColor(primaryColor);
            mAlbumView.setFloatingLabelTextColor(backgroundColor);
            mAlbumArtistView.setTextColor(primaryColor);
            mAlbumArtistView.setFloatingLabelTextColor(backgroundColor);
            mYearView.setTextColor(primaryColor);
            mYearView.setFloatingLabelTextColor(backgroundColor);
            mDiscView.setTextColor(primaryColor);
            mDiscView.setFloatingLabelTextColor(backgroundColor);
            mTrackView.setTextColor(primaryColor);
            mTrackView.setFloatingLabelTextColor(backgroundColor);
            mGenreView.setTextColor(primaryColor);
            mGenreView.setFloatingLabelTextColor(backgroundColor);
            mCountryView.setTextColor(primaryColor);
            mCountryView.setFloatingLabelTextColor(backgroundColor);
            mCommentView.setTextColor(primaryColor);
            mCommentView.setFloatingLabelTextColor(backgroundColor);
            mLyricsView.setTextColor(primaryColor);
            mLyricsView.setFloatingLabelTextColor(backgroundColor);

        }

        tagChanged = false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void triggerStorageAccessFramework() {
        if( this.getContentResolver().getPersistedUriPermissions().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, MusicService.REQUEST_CODE_PERMISSION_All);
        }
    }

    private void populateMediaTag(List<MediaItem> items) {
        //MediaTag tag = null;
        if(items.size()==1) {
            items.get(0).setLoadedEncoding(false);
            mediaStoreHelper.loadMediaTag(items.get(0),null);
           mediaTag = items.get(0).getTag().clone();
        }else if(items.size()>1){
            items.get(0).setLoadedEncoding(false);
            mediaStoreHelper.loadMediaTag(items.get(0),null);
            mediaTag = items.get(0).getTag().clone();
            mediaTag.setMediaSize("...");
            mediaTag.setDisplayPath(items.size()+" songs");
            mediaTag.setTitle("...");
            mediaTag.setTrack("...");
            mediaTag.setLyrics("...");
            for (int i=1;i<items.size();i++) {
                MediaItem item = items.get(i);
                mediaTag.setAudioDuration(mediaTag.getAudioDuration()+item.getTag().getAudioDuration());
                if(!equals(mediaTag.getAlbum(), item.getTag().getAlbum())) {
                    mediaTag.setAlbum("...");
                }
                if(!equals(mediaTag.getArtist(), item.getTag().getArtist())) {
                    mediaTag.setArtist("...");
                }
                if(!equals(mediaTag.getAlbumArtist(), item.getTag().getAlbumArtist())) {
                    mediaTag.setAlbumArtist("...");
                }
                if(!equals(mediaTag.getGenre(), item.getTag().getGenre())) {
                    mediaTag.setGenre("...");
                }
                if(!equals(mediaTag.getYear(), item.getTag().getYear())) {
                    mediaTag.setYear("...");
                }
                if(!equals(mediaTag.getTrackTotal(), item.getTag().getTrackTotal())) {
                    mediaTag.setTrackTotal("...");
                }
                if(!equals(mediaTag.getDisc(), item.getTag().getDisc())) {
                    mediaTag.setDisc("...");
                }
                if(!equals(mediaTag.getDiscTotal(), item.getTag().getDiscTotal())) {
                    mediaTag.setDiscTotal("...");
                }
                if(!equals(mediaTag.getComment(), item.getTag().getComment())) {
                    mediaTag.setComment("...");
                }
                if(!equals(mediaTag.getGrouping(), item.getTag().getGrouping())) {
                    mediaTag.setCountry("...");
                }
                /*
                tag.album=album;
                tag.artist=artist;
                tag.albumArtist=albumArtist;
                tag.genre=genre;
                tag.year=year;
                tag.trackTotal=trackTotal;
                tag.disc=disc;
                tag.discTotal=discTotal;
                tag.comment=comment;
                tag.country=country;
                */
            }
        }
    }

    private boolean equals(String album, String album1) {
       return StringUtils.trimToEmpty(album).equals(StringUtils.trimTitle(album1));
    }

    private void setUpAutoView() {
        mMatchCoverImages = findViewById(R.id.matchCoverView);
        mMatchTagView = findViewById(R.id.matchTagView);
    }

    private void updateAudioFormatQualityView(MediaProvider.MediaQuality quality) {
        switch (quality) {
            case HIRES:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                break;
            case HIGH:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                break;
            case GOOD:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                break;
            case LOW:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                break;
            default:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    private void makePopForceShowIcon(PopupMenu popupMenu) {
        try {
            Field mFieldPopup=popupMenu.getClass().getDeclaredField("mPopup");
            mFieldPopup.setAccessible(true);
            MenuPopupHelper mPopup = (MenuPopupHelper) mFieldPopup.get(popupMenu);
            mPopup.setForceShowIcon(true);
        } catch (Exception e) {

        }
    }

    private void doSaveMediaItem() {
        final MediaTag tagUpdate = mediaTag;//mediaItem.getTag();
        tagUpdate.setTitle(getText(mTitleView));
        tagUpdate.setAlbum(getText(mAlbumView));
        tagUpdate.setAlbumArtist(getText(mAlbumArtistView));
        tagUpdate.setArtist(getText(mArtistView));
        tagUpdate.setComment(getText(mCommentView));
        tagUpdate.setCountry(getText(mCountryView));
        tagUpdate.setGenre(getText(mGenreView));
        tagUpdate.setTrack(getText(mTrackView));
        tagUpdate.setLyrics(getText(mLyricsView));
        tagUpdate.setYear(getText(mYearView));
        tagUpdate.setDisc(getText(mDiscView));
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if(inputManager !=null) {
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
        }

        //RxAndroid
        Observable<Boolean> observable = Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                for(MediaItem item:mediaItems) {
                    mediaStoreHelper.saveMediaFile(item.getPath(), tagUpdate);
                    item.setLoadedEncoding(false);
                    mediaStoreHelper.loadMediaTag(item,null);
                }
                return true;
            }
        });
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {
                mDialogHelper.displayProgressAlert(R.string.alert_write_tag_title,getString(R.string.alert_write_tag_inprogress,mediaItems.get(0).getTitle(),mediaItems.get(0).getPath()),R.drawable.ic_beenhere_black_24dp);
            }

            @Override
            public void onNext(Boolean actionResult) {
                mDialogHelper.displaySuccessAlert( R.string.alert_write_tag_title,getString(R.string.alert_write_tag_success,mediaItems.get(0).getTitle()),R.drawable.ic_beenhere_black_24dp);
            }

            @Override
            public void onError(Throwable e) {
                mDialogHelper.displayFailedAlert( R.string.alert_write_tag_title,getString(R.string.alert_write_tag_fail,mediaItems.get(0).getTitle(),mediaItems.get(0).getPath()),R.drawable.ic_beenhere_black_24dp);
            }

            @Override
            public void onComplete() {
                tagChanged = false;
                editorCardview.clearFocus();
                mDialogHelper.dismissProgressDailog();
                toggleSaveFabAction();
                saveResult("SAVED", tagUpdate.getMediaPath(),null);
            }
        });
    }

    private String getText(TextView textView) {
        return StringUtils.trimToEmpty(String.valueOf(textView.getText()));
    }

    private void doMoveMediaItem() {
        final String path = mediaItems.get(0).getPath();
        final String title = mediaItems.get(0).getTitle();
        final String organizedPath = mediaStoreHelper.getOrganizedPath(mediaItems.get(0));
        if(path.equals(organizedPath)) {
            mDialogHelper.displayFailedAlert(-1,getString(R.string.alert_organize_already,title),R.drawable.ic_move_to_inbox_black_24dp);
            return;
        }

        verifyStoragePermissions();;

        new MaterialStyledDialog.Builder(this)
                .setTitle(R.string.alert_organize_title)
                .setDescription(getString(R.string.alert_organize_confirm, title,organizedPath))
                .setIcon(R.drawable.ic_move_to_inbox_black_24dp)
                .setPositiveText(R.string.alert_organize_confirm_btn)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(final MaterialDialog dialog, DialogAction which) {
                                    //RxAndroid

                Observable<Boolean> observable = Observable.fromCallable(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        for(MediaItem item:mediaItems) {
                            String newPath = mediaStoreHelper.getOrganizedPath(item);
                            if(mediaStoreHelper.moveMediaFile(item.getPath(), newPath)) {
                                playNextSong(item);
                                item.setPath(newPath);
                            }
                        }
                        return true;
                    }
                });
                observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDialogHelper.displayProgressAlert( R.string.alert_organize_title,getString(R.string.alert_organize_inprogress, title,organizedPath),R.drawable.ic_move_to_inbox_black_24dp);
                    }

                    @Override
                    public void onNext(Boolean actionResult) {
                        mDialogHelper.displaySuccessAlert( R.string.alert_organize_title,getString(R.string.alert_organize_success,title),R.drawable.ic_move_to_inbox_black_24dp);
                        for(MediaItem item: mediaItems) {
                            item.setLoadedEncoding(false);
                            mediaStoreHelper.loadMediaTag(item, null);
                        }
                        populateMediaTag(mediaItems);
                        updateTagView();
                        editorCardview.clearFocus();
                        tagChanged=false;
                        toggleSaveFabAction();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mDialogHelper.displayFailedAlert( R.string.alert_organize_title,getString(R.string.alert_organize_fail, title,organizedPath),R.drawable.ic_move_to_inbox_black_24dp);
                        dialog.dismiss();
                    }

                    @Override
                    public void onComplete() {
                        mDialogHelper.dismissProgressDailog();
                        dialog.dismiss();
                    }
                });
                                }
                            })
                .setNegativeText(R.string.alert_organize_cancel_btn)
                .show();
    }

    private void doDeleteMediaItem() {
        verifyStoragePermissions();;
        // enable for sing item only
        new MaterialStyledDialog.Builder(this)
                .setTitle(R.string.alert_delete_title)
                .setDescription(getString(R.string.alert_delete_confirm,mediaTag.getTitle(),mediaItems.get(0).getPath()))
                .setIcon(R.drawable.ic_delete_black_24dp)
                .setPositiveText(R.string.alert_delete_confirm_btn)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, DialogAction which) {
                        //RxAndroid
                        Observable<Boolean> observable = Observable.fromCallable(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                for(MediaItem item: mediaItems) {
                                    playNextSong(item);
                                     mediaStoreHelper.deleteMediaFile(item.getPath());
                                }
                                return true;
                            }
                        });
                        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                mDialogHelper.displayProgressAlert(R.string.alert_delete_title, getString(R.string.alert_delete_inprogress,mediaItems.get(0).getTitle(),mediaItems.get(0).getPath()),R.drawable.ic_delete_black_24dp);
                            }

                            @Override
                            public void onNext(Boolean actionResult) {
                                playNextSong(mediaItems.get(0));
                                mDialogHelper.displaySuccessAlert(R.string.alert_delete_title,getString(R.string.alert_delete_success,mediaItems.get(0).getTitle()),R.drawable.ic_delete_black_24dp);
                                toggleSaveFabAction();
                                saveResult("DELETED", mediaItems.get(0).getPath(),null);
                                onBackPressed();
                            }

                            @Override
                            public void onError(Throwable e) {
                                mDialogHelper.displayFailedAlert(R.string.alert_delete_title,e.getMessage(),R.drawable.ic_delete_black_24dp);
                                dialog.dismiss();
                            }

                            @Override
                            public void onComplete() {
                                mDialogHelper.dismissProgressDailog();
                                dialog.dismiss();
                            }
                        });
                    }
                })
                .setNegativeText(R.string.alert_delete_cancel_btn)
                 .show();
    }

    private void playNextSong(MediaItem item) {
        MusicService service = MusicService.getRunningService();
        if(service!=null) {
            service.nextSong(StringUtils.trimToEmpty(item.getTag().getTitle()));
        }
    }

    private void saveResult(String organized, String path, String oldPath) {
        Intent data = new Intent();
       // data.setData(Uri.parse(organized));
       // data.putExtra(ARG_MEDIA_ID,itemId);
       // data.putExtra(ARG_MEDIA_POSITION,mediaPosition);
       // if(mediaItems.size()==1) {
       //     data.putExtra(ARG_MEDIA_TITLE, mediaItem.getTag().getTitle());
       //     data.putExtra(ARG_MEDIA_ARTIST, mediaItem.getTag().getArtist());
       //     data.putExtra(ARG_MEDIA_ALBUM, mediaItem.getTag().getAlbum());
       //     mediaItem.setPath(path);
       // }
       // if(!StringUtils.isEmpty(oldPath)) {
        //    data.putExtra(ARG_MEDIA_OLD_PATH, oldPath);
       // }
       // data.putExtra(ARG_MEDIA_PATH,path);
        setResult(RESULT_OK,data);
    }

    private void toggleSaveFabAction(){
        if(tagChanged) {
            ViewCompat.animate(fabSaveAction)
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f).setDuration(200)
                    .setStartDelay(300L)
                    .start();
            fabStatus = true;
        }else {
            ViewCompat.animate(fabSaveAction)
                    .scaleX(0f).scaleY(0f)
                    .alpha(0f).setDuration(100)
                    .start();
            fabStatus = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_editor_main, menu);
        for(int i = 0; i < menu.size(); i++){
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.menu_main_delete_file:
                    if(mediaItems.size()==1) {
                        UIUtils.setColorFilter(item, getColor(R.color.menu_delete_background));
                    }else {
                        item.setEnabled(false);
                    }
                    break;
                //case R.id.menu_organize:
                //    setColorFilter(item, R.color.menu_delete_background);
                //    break;
                //case R.id.menu_language:
                //    setColorFilter(item, R.color.menu_editor_background);
                //    break;
                //case R.id.menu_tag_from_filename:
                default:
                    UIUtils.setColorFilter(item,  getColor(R.color.menu_editor_background));
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_main_coverart:
                toggleAutoTagPanel();
                break;
            case R.id.menu_main_delete_file:
                doDeleteMediaItem();
                break;
            case R.id.menu_main_transfer_file:
                doMoveMediaItem();
                break;
            case R.id.menu_file_manager:
                doOpenFileManager();
                break;
            case R.id.menu_main_full:
                onCreatePopMenuFull();
                break;
         /*   case R.id.menu_charset_default:
                updateViewByCharset(null);
                break;*/
            case R.id.menu_charset_thai:
                updateViewByCharset("TIS-620");
                break;
          /*  case R.id.menu_charset_unicode:
                updateViewByCharset("UTF-8");
                break;*/
            case R.id.menu_format_tag:
                formatTagView();
                break;
            //case R.id.menu_label_simple:
             case R.id.menu_label_smart_reader:
             case R.id.menu_label_smart_reader2:
             case R.id.menu_label_hierarchy:
                 onOptionsTagReaderSelected(item);
                 break;
        }
        toggleSaveFabAction();
        return super.onOptionsItemSelected(item);
    }

    private void toggleAutoTagPanel() {
        if(autoCardview!=null && autoCardview.getVisibility()==View.VISIBLE) {
            autoCardview.setVisibility(View.GONE);
            editorCardview.setVisibility(View.VISIBLE);
            toggleSaveFabAction();
        }else {
            final  List<RecordingItem> songs = new ArrayList<>();
            final  List<AlbumInfo> albums = new ArrayList<>();
            //RxAndroid
            Observable<Boolean> observable = Observable.fromCallable(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    songs.addAll(MusicBrainz.findSongInfo(mediaTag.getTitle(),mediaTag.getArtist(),null));
                    albums.addAll(MusicBrainz.findAlbumArts(songs));
                    return true;
                }
            });
            observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
                @Override
                public void onSubscribe(Disposable d) {
                    // start progress
                    mDialogHelper.displayProgressAlert(R.string.alert_find_music_title,getString(R.string.alert_find_music_information,mediaTag.getTitle(),mediaTag.getArtist()), R.drawable.icon_inmenu_info);
                }

                @Override
                public void onNext(Boolean actionResult) {
                    editorCardview.setVisibility(View.GONE);
                    autoCardview.setVisibility(View.VISIBLE);

                    try {
                        if(mAutoSongAdapter ==null) {
                            mAutoSongAdapter = new FlexibleAdapter(songs);
                            mMatchTagView.setAdapter(mAutoSongAdapter);
                        }else {
                            mAutoSongAdapter.updateDataSet(songs);
                            mAutoSongAdapter.notifyDataSetChanged();
                        }
/*
                        if(mAutoCoverAdapter==null) {
                            mAutoCoverAdapter = new FlexibleAdapter(albums);
                            mMatchCoverImages.setAdapter(mAutoCoverAdapter);
                            LinearLayoutManager layoutManager
                                    = new LinearLayoutManager(MediaTagEditorActivity.this, LinearLayoutManager.HORIZONTAL, false);
                            mMatchCoverImages.setLayoutManager(layoutManager);

                        }else {
                            mAutoCoverAdapter.updateDataSet(albums);
                            mAutoCoverAdapter.notifyDataSetChanged();
                        }*/
                    } catch (Exception e) {
                        LogHelper.d(TAG, e);
                    }
                    mDialogHelper.dismissProgressDailog();
                }

                @Override
                public void onError(Throwable e) {
                    editorCardview.setVisibility(View.GONE);
                    autoCardview.setVisibility(View.VISIBLE);
                    mDialogHelper.dismissProgressDailog();
                }

                @Override
                public void onComplete() {
                    editorCardview.setVisibility(View.GONE);
                    autoCardview.setVisibility(View.VISIBLE);
                    mDialogHelper.dismissProgressDailog();
                }
            });

            //load from musicbrenz
            toggleSaveFabAction();
        }
    }

    private void onOptionsTagReaderSelected(MenuItem item) {
        //enable for single item only
        String mediaPath = mediaItems.get(0).getPath();
        File file = new File(mediaPath);
        if(!file.exists()) return;

        TagReader reader = new TagReader();

        switch (item.getItemId()) {
            //case R.id.menu_label_simple:
                //format artist/album/track title
            //    mediaTag = reader.parser(mediaPath, TagReader.READ_MODE.SIMPLE);
            //    break;
            case R.id.menu_label_smart_reader:
                mediaTag = reader.parser(mediaPath, TagReader.READ_MODE.SM1);
                break;
            case R.id.menu_label_smart_reader2:
                mediaTag = reader.parser(mediaPath, TagReader.READ_MODE.SM2);
                break;
            case R.id.menu_label_hierarchy:
                mediaTag = reader.parser(mediaPath, TagReader.READ_MODE.HIERARCHY);
                break;
            default:
                break;
        }
        updateTagView();

        //load from media file
        //mediaStoreHelper.loadMediaTag(mediaItems.get(0), null);
    }

    private void onCreatePopMenuFull() {
        View anchor = findViewById(R.id.menu_main_full);
        @SuppressLint("RestrictedApi") Context wrapper = new ContextThemeWrapper(this, R.style.PopupMenuStyle);
        final PopupMenu popupMenu = new PopupMenu(wrapper, anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onOptionsItemSelected(item);
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_editor_full);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            UIUtils.setColorFilter(item, getColor(R.color.menu_editor_background));
        }
        popupMenu.show();
    }

    private void doOpenFileManager() {
        File mediaFile = new File(mediaItems.get(0).getPath());
        Uri uri = Uri.parse(mediaFile.getParent());
        Intent intent = this.getPackageManager().getLaunchIntentForPackage("pl.solidexplorer2");
        if(intent!=null) {
            intent.setAction("org.openintents.action.VIEW_DIRECTORY");
            intent.setData(uri);
        }else {
            intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "resource/folder");
        }
        startActivityForResult(intent,1);
    }

    private void updateTagView() {
        // title
        mTitleView.setText(StringUtils.trimToEmpty(mediaTag.getTitle()));
        // artist
        mArtistView.setText(StringUtils.trimToEmpty(mediaTag.getArtist()));
        // album
        mAlbumView.setText(StringUtils.trimToEmpty(mediaTag.getAlbum()));
        // album artist
        mAlbumArtistView.setText(StringUtils.trimToEmpty(mediaTag.getAlbumArtist()));

        mCountryView.setText(StringUtils.trimToEmpty(mediaTag.getGrouping()));
        // album artist
        mCommentView.setText(StringUtils.trimToEmpty(mediaTag.getComment()));
        // album artist
        mTrackView.setText(StringUtils.trimToEmpty(mediaTag.getTrack()));
        // path
        mMediaPathView.setText(StringUtils.trimToEmpty(mediaTag.getDisplayPath()));
    }

    private void formatTagView() {
        // title
        mTitleView.setText(mediaStoreHelper.formatTitle(mTitleView.getText()));
        // artist
        mArtistView.setText(mediaStoreHelper.formatTitle(mArtistView.getText()));
        // album
        mAlbumView.setText(mediaStoreHelper.formatTitle(mAlbumView.getText()));
        // album artist
        mAlbumArtistView.setText(mediaStoreHelper.formatTitle(mAlbumArtistView.getText()));
        // genre
        mGenreView.setText(mediaStoreHelper.formatTitle(mGenreView.getText()));

        // clean albumArtist if same value as artist
        if(mArtistView.getText().equals(mAlbumArtistView.getText())) {
            mAlbumArtistView.setText("");
        }

        toggleSaveFabAction();
    }

    private void verifyStoragePermissions() {
        try {
            PermissionManager permissionManager = PermissionManager.getInstance(this);
            List<String> permissions = Arrays.asList(MusicService.PERMISSIONS_STORAGE);
            permissionManager.checkPermissions(permissions, new PermissionManager.PermissionRequestListener() {
                @Override
                public void onPermissionGranted() {
                    triggerStorageAccessFramework();
                }

                @Override
                public void onPermissionDenied() {
                    Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception ex) {
            LogHelper.e(TAG, ex);
        }
    }

    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == MusicService.REQUEST_CODE_PERMISSION_All) {
            Uri treeUri = null;
            if (resultCode == Activity.RESULT_OK) {
                // Get Uri from Storage Access Framework.
                treeUri = resultData.getData();

                // Persist access permissions.
                this.getContentResolver().takePersistableUriPermission(treeUri, (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            }
        }
    }

            @Override
     public void onBackPressed() {
        if(tagChanged) {
            mDialogHelper.displayWarningAlert(R.string.alert_warning_title,getString(R.string.alert_media_file_not_save), R.drawable.ic_mode_edit_black_24dp);
        }
        if (fabStatus) {
           // mediaStoreHelper.loadMediaTag(mediaItem, null);
            populateMediaTag(mediaItems);
            updateTagView();
            tagChanged = false;
            toggleSaveFabAction();
            return;
        }

      //  if(!fromBrowser) {
      //      Intent intent = new Intent(this, MediaBrowserActivity.class);
      //      startActivity( intent);
      //  }else {
            super.onBackPressed();
      //  }
    }

    private void updateViewByCharset(String charset) {
        if(StringUtils.isEmpty(charset)) {
            // re-load from media file
            populateMediaTag(mediaItems);
           // mediaStoreHelper.loadMediaTag(mediaItem, null);
        }
       // MediaTag tagUpdate = mediaItem.getTag();
        // title
        mTitleView.setText(encodeText(mediaTag.getTitle(), charset));
        // artist
        mArtistView.setText(encodeText(mediaTag.getArtist(), charset));
        // album
        mAlbumView.setText(encodeText(mediaTag.getAlbum(), charset));
        // album artist
        mAlbumArtistView.setText(encodeText(mediaTag.getAlbumArtist(), charset));
        // genre
        mGenreView.setText(encodeText(mediaTag.getGenre(), charset));
        // country
        mCountryView.setText(encodeText(mediaTag.getGrouping(), charset));
        // comment
        mCommentView.setText(encodeText(mediaTag.getComment(), charset));
        // lyrics
        mLyricsView.setText(encodeText(mediaTag.getLyrics(), charset));
    }

    private String encodeText(String text, String charset) {
        if(StringUtils.isEmpty(charset)) {
            return text;
        }
        if(StringUtils.isEmpty(text)) {
            return "";
        }
        try {
            return new String(text.getBytes(ANSI_CHARSET), charset);
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }

    @Override
        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            try {
                return super.dispatchTouchEvent(motionEvent);
            } catch (NullPointerException e) {
                return false;
            }
        }

        private void initActivityTransitions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Slide transition = new Slide();
                transition.excludeTarget(android.R.id.statusBarBackground, true);
                getWindow().setEnterTransition(transition);
                getWindow().setReturnTransition(transition);
            }
        }

        private void applyPalette(Palette palette) {
            int primaryDark = getResources().getColor(R.color.bt_accent, getTheme());
            int primary = getResources().getColor(R.color.black, getTheme());
            collapsingToolbarLayout.setContentScrimColor(palette.getMutedColor(primary));
            collapsingToolbarLayout.setStatusBarScrimColor(palette.getDarkMutedColor(primaryDark));
            if(fabSaveAction.getVisibility()==View.VISIBLE) {
                updateBackground(fabSaveAction, palette);
            }
            supportStartPostponedEnterTransition();
        }
/*
    private static void displaySuccessAlert(Activity activity,int titleResId, String text, int iconResId) {
        if(Alerter.isShowing()) {
            Alerter.hide();
        }
        Alerter.clearCurrent(activity);
        Alerter alert = Alerter.create(activity);
        alert. setBackgroundColorRes(R.color.material_color_blue_A700); // or setBackgroundColorInt(Color.CYAN)
        alert.setDuration(1000)
                .enableSwipeToDismiss();
        if(titleResId!=-1) {
            alert.setTitle(titleResId);
        }
        alert.setText(text);
        if(iconResId!= -1) {
            alert.setIcon(iconResId);
        }
        alert.show();
    }

    private static void displayProgressAlert(Activity activity,int titleResId, String text, int iconResId) {
        if(Alerter.isShowing()) {
            Alerter.hide();
        }
        Alerter.clearCurrent(activity);
        Alerter alert = Alerter.create(activity);
        alert. setBackgroundColorRes(R.color.material_color_purple_A700); // or setBackgroundColorInt(Color.CYAN)
        alert.setDuration(50000);
        alert.enableProgress(true);
        if(titleResId!=-1) {
            alert.setTitle(titleResId);
        }
        alert.setText(text);
        if(iconResId!= -1) {
            alert.setIcon(iconResId);
        }
        alert.show();
    }

    private static void displayFailedAlert(Activity activity,int titleResId, String text, int iconResId) {
        if(Alerter.isShowing()) {
            Alerter.hide();
        }
        Alerter.clearCurrent(activity);
        Alerter alert = Alerter.create(activity);
        alert. setBackgroundColorRes(R.color.material_color_red_A700) // or setBackgroundColorInt(Color.CYAN)
                .setDuration(2000)
                .enableSwipeToDismiss();

        if(titleResId!=-1) {
            alert.setTitle(titleResId);
        }
        if(text!=null) {
            alert.setText(text);
        }
        if(iconResId!= -1) {
            alert.setIcon(iconResId);
        }
        alert.show();
    }
*/
        private void updateBackground(FloatingActionButton fab, Palette palette) {
            int lightVibrantColor = palette.getLightVibrantColor(getResources().getColor(android.R.color.white, getTheme()));
            int vibrantColor = palette.getVibrantColor(getResources().getColor(R.color.bt_accent, getTheme()));

            fab.setRippleColor(lightVibrantColor);
            fab.setBackgroundTintList(ColorStateList.valueOf(vibrantColor));
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
