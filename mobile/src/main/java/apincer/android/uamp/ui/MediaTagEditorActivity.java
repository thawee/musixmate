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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.gdacciaro.iOSDialog.iOSDialog;
import com.gdacciaro.iOSDialog.iOSDialogBuilder;
import com.gdacciaro.iOSDialog.iOSDialogClickListener;
import com.github.javiersantos.bottomdialogs.BottomDialog;
import com.intentfilter.androidpermissions.PermissionManager;
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;

import net.galmiza.android.spectrogram.FrequencyView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import apincer.android.uamp.BuildConfig;
import apincer.android.uamp.FileManagerService;
import apincer.android.uamp.MediaItemService;
import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.analyzer.FLACAnalyzer;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.model.MediaTag;
import apincer.android.uamp.model.PathItem;
import apincer.android.uamp.model.RecordingItem;
import apincer.android.uamp.musicbrainz.MusicBrainz;
import apincer.android.uamp.provider.AndroidFile;
import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.provider.TagReader;
import apincer.android.uamp.ui.view.LinearDividerItemDecoration;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.MusicMateArtwork;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.UIUtils;
import de.mateware.snacky.Snacky;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * A full screen editor that shows the current  music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class MediaTagEditorActivity extends AppCompatActivity implements FlexibleAdapter.OnItemClickListener {
    private static final String TAG = LogHelper.makeLogTag(MediaTagEditorActivity.class);
    private static final int fftResolution = 2048; //;
    private static final String ANSI_CHARSET="ISO8859_1";
  // private static final String ANSI_CHARSET="TIS-620";

    public static final int REQUEST_EDIT_MEDIA_TAG = 222;
    public static final int REQUEST_GET_CONTENT_IMAGE = 555;
    public static final int REQUEST_SAVE_FILE_RESULT_CODE = 888;
    public static final String ARG_MEDIA_PATH = "media_path";
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private MediaProvider mediaStoreHelper;
    private boolean tagChanged;
    private boolean coverartChanged;
    private List<MediaItem> mediaItems;
    private MediaTag displayTag;
    private File coverartFile;

    private MaterialAutoCompleteTextView mTitleView;
    private MaterialAutoCompleteTextView mArtistView;
    private MaterialAutoCompleteTextView mAlbumView;
    private MaterialAutoCompleteTextView mAlbumArtistView;
    private MaterialAutoCompleteTextView mGenreView;
    private MaterialEditText mYearView;
    private MaterialAutoCompleteTextView mComposerView;
    private MaterialAutoCompleteTextView mCountryView;
    private MaterialEditText mCommentView;
    private MaterialEditText mLyricsView;
    private MaterialEditText mTrackView;
    private MaterialEditText mDiscView;
    private TextView mFormatView;
    private TextView mSamplerateView;
   // private TextView mBitrateView;
    private TextView mDurationView;
    private TextView mFileSizeView;
    private TextView mMediaPathView;
    private TextView mImageDimensionView;

    // multiple items
    private View mMediaItemsPanelView;
    private RecyclerView mMediaItemListView;
    private MediaItemsEditorAdapter mMediaItemAdapter;

    private View mainView;
    private boolean showSimpleMode = true;
    private TextView mMiscInfoView;
    private TextView mSongListAction;
    private boolean showSongList = true;

    private View mSpectrumView;
    private FrequencyView mFrequencyView;
    private View mMusicBrainzView;
    private RecyclerView mMusicBrainzRCView;

    private View editorCardview;

    private ViewTextWatcher mTextWatcher;

    private FloatingActionButton fabSaveAction;

    private Toolbar mToolbar;
    private Toolbar mTagsToolbar;

    private Intent mResultData;
    private MaterialProgressBar mMaterialProgressBar;
    private Snackbar mSnackbar;

    public static boolean navigateForResult(AppCompatActivity context, MediaItem item) {
        Intent intent = new Intent(context, MediaTagEditorActivity.class);
        MediaProvider mediaMusicTagHelper = MediaProvider.getInstance();
        if (mediaMusicTagHelper.isMediaFileExist(item)) {
            FileManagerService.addToEdit(item);
            ActivityCompat.startActivityForResult(context, intent, REQUEST_EDIT_MEDIA_TAG, null);
            return true;
        }else {
            try {
                mediaMusicTagHelper.deleteMediaFile(item.getPath());
                mediaMusicTagHelper.deleteFromMediaStore(item.getPath());
            }catch (Exception ex) {}
            alertFail(context, R.string.alert_error_title,context.getString(R.string.alert_invalid_media_file, item.getTag().getTitle()));
            return false;
        }
    }

    private static void alertFail(AppCompatActivity context, int titleResId, String text) {
        new iOSDialogBuilder(context)
                .setTitle(context.getString(titleResId))
                .setSubtitle(text)
                .setBoldPositiveLabel(true)
                .setCancelable(true)
                .setPositiveListener(context.getString(R.string.alert_btn_ok),new iOSDialogClickListener() {
                    @Override
                    public void onClick(iOSDialog dialog) {
                        dialog.dismiss();

                    }
                })
                /*
                .setNegativeListener(getString(R.string.dismiss), new iOSDialogClickListener() {
                    @Override
                    public void onClick(iOSDialog dialog) {
                        dialog.dismiss();
                    }
                })*/
                .build().show();
    }

    public static boolean navigateForResult(Activity context, List<MediaItem> items) {
       FileManagerService.addToEdit(items);
       Intent intent = new Intent(context, MediaTagEditorActivity.class);
       ActivityCompat.startActivityForResult(context, intent, REQUEST_EDIT_MEDIA_TAG, null);
       return true;
    }

    private boolean isSingleMediaItem() {
        return mediaItems.size()==1;
    }

    private MediaItem getSingleMediaItem() {
        return mediaItems.get(0);
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

            // file toolbar
            mToolbar = findViewById(R.id.toolbar);
           // mToolbar.setBackgroundResource(R.drawable.md_transparent);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            // tag toolbar
        mTagsToolbar = findViewById(R.id.tags_toolbar);
        mTagsToolbar.inflateMenu(R.menu.menu_editor_tags_main);
      //  mTagsToolbar.setBackgroundResource(R.drawable.md_transparent);
        mTagsToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        for(int i = 0; i < mTagsToolbar.getMenu().size(); i++) {
            MenuItem item = mTagsToolbar.getMenu().getItem(i);
            UIUtils.setColorFilter(item, getColor(R.color.menu_delete_background));
        }
        mTagsToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(showSimpleMode) {
                    enableFullViewMode();
                    showSimpleMode=false;
                }else {
                    enableSimpleViewMode();
                    showSimpleMode=true;
                }
            }
        });

        mediaItems = new ArrayList<>();
        mediaItems.addAll(FileManagerService.getEditItems());
        prepareDisplayTag(mediaItems);

        setUpProgressBar();
        setUpSpectrumAnalyzer();

            if (displayTag == null) {
                // should stop
                setResultData("INVALID", getIntent().getStringExtra(ARG_MEDIA_PATH),null);
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
            mMusicBrainzView = findViewById(R.id.musicbrainz);
        mSpectrumView = findViewById(R.id.spectrum);


        toggleSaveFabAction(); //hide until tag changed

        MediaMetadata metadata = getSingleMediaItem().getMetadata();
        mMediaPathView = findViewById(R.id.media_path);
        mMediaPathView.setText(metadata.getDisplayPath());

        mFormatView = findViewById(R.id.media_format);
        mFormatView.setText(metadata.getAudioCodingFormat());

        mSamplerateView = findViewById(R.id.media_samplerate);
        mSamplerateView.setText(metadata.getAudioCoding());

       // mBitrateView = findViewById(R.id.media_bitrate);
       // mBitrateView.setText(metadata.getAudioBitRate());

        mDurationView = findViewById(R.id.media_duration);
        mDurationView.setText(metadata.getAudioDurationAsString());

        mFileSizeView = findViewById(R.id.media_filesize);
        mFileSizeView.setText(metadata.getMediaSize());

        mMediaItemsPanelView = findViewById(R.id.media_items_panel);
        mMediaItemListView = findViewById(R.id.media_items_list);
        mMediaItemAdapter = new MediaItemsEditorAdapter(new ArrayList());
        mMediaItemListView.setAdapter(mMediaItemAdapter);
        mMediaItemListView.setLayoutManager(new SmoothScrollLinearLayoutManager(this));
        mMediaItemListView.setHasFixedSize(true);
        RecyclerView.ItemDecoration itemDecoration = new LinearDividerItemDecoration(this, Color.WHITE,2);
        mMediaItemListView.addItemDecoration(itemDecoration);

        mTextWatcher =new ViewTextWatcher() ;

        // title
            mTitleView = findViewById(R.id.title);
            mTitleView.setText(displayTag.getTitle());
            setTitle("");
            mTitleView.addTextChangedListener(mTextWatcher);

            // artist
            mArtistView = findViewById(R.id.artist);
            mArtistView.setText(displayTag.getArtist());
            mArtistView.addTextChangedListener(mTextWatcher);
            //ArrayAdapter<String> artistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,mediaStoreHelper.getArtistsAsArray());
            ArrayAdapter<String> artistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item, (String[])MediaItemService.getArtists().toArray(new String[0]));
            mArtistView.setThreshold(2);//will start working from second character
            mArtistView.setAdapter(artistAdapter); //setting the adapter data into the AutoCompleteTextView

            // album
            mAlbumView = findViewById(R.id.album);
            mAlbumView.setText(displayTag.getAlbum());
            mAlbumView.addTextChangedListener(mTextWatcher);
        //ArrayAdapter<String> albumAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,mediaStoreHelper.getAlbumAsArray());
        ArrayAdapter<String> albumAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,MediaItemService.getAlbums().toArray(new String[0]));
        mAlbumView.setThreshold(2);//will start working from second character
        mAlbumView.setAdapter(albumAdapter); //setting the adapter data into the AutoCompleteTextView

            // album artist
            mAlbumArtistView = findViewById(R.id.album_arist);
            mAlbumArtistView.setText(displayTag.getAlbumArtist());
            mAlbumArtistView.addTextChangedListener(mTextWatcher);
        //ArrayAdapter<String> albumArtistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,mediaStoreHelper.getAlbumArtistAsArray());
        ArrayAdapter<String> albumArtistAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,MediaItemService.getAlbumArtists().toArray(new String[0]));
        mAlbumArtistView.setThreshold(2);//will start working from second character
        mAlbumArtistView.setAdapter(albumArtistAdapter); //setting the adapter data into the AutoCompleteTextView

        // year
        mYearView = findViewById(R.id.year);
        mYearView.setText(displayTag.getYear());
        mYearView.addTextChangedListener(mTextWatcher);

            // disc no
        mDiscView = findViewById(R.id.diskno);
        mDiscView.setText(displayTag.getDisc());
        mDiscView.addTextChangedListener(mTextWatcher);

            // track
            mTrackView = findViewById(R.id.track);
        mTrackView.setText(displayTag.getTrack());
        mTrackView.addTextChangedListener(mTextWatcher);

            // genre
            mGenreView = findViewById(R.id.genre);
            mGenreView.setText(displayTag.getGenre());
            mGenreView.addTextChangedListener(mTextWatcher);

            // country
            mCountryView = findViewById(R.id.group);
            mCountryView.setText(displayTag.getGrouping());
            mCountryView.addTextChangedListener(mTextWatcher);

            String [] langs = {"classic","eng","tha"};
            ArrayAdapter<String> countryAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_item,langs);
            mCountryView.setThreshold(0);//will start working from first character
            mCountryView.setAdapter(countryAdapter); //setting the adapter data into the AutoCompleteTextView

        //composer
        mComposerView = findViewById(R.id.composer);
        mComposerView.setText(displayTag.getComposer());
        mComposerView.addTextChangedListener(mTextWatcher);

            // comment
            mCommentView = findViewById(R.id.comment);
            mCommentView.setText(displayTag.getComment());
            mCommentView.addTextChangedListener(mTextWatcher);

            // lyrics
            mLyricsView = findViewById(R.id.lyrics);
            mLyricsView.setText(displayTag.getLyrics());
            mLyricsView.addTextChangedListener(mTextWatcher);
        mMiscInfoView = findViewById(R.id.media_others_information);
        mSongListAction = findViewById(R.id.media_song_list);
        mSongListAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    toggleSongListView();
                }
            });

        if(isSingleMediaItem()) {
            updateAudioFormatQualityView(getSingleMediaItem().getMetadata().getQuality());
        }else {
            hideSingleItemFields();
        }
        enableSimpleViewMode();
        //enableFullViewMode();

        // always get artwork from first item
        final ImageView imageView = findViewById(R.id.image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //displayBottomMenu();
                displayBottomImageMenu();
            }
        });
        displayDefaultCoverart();
        tagChanged = false;
    }

    private void setUpSpectrumAnalyzer() {
        int fftResolution = 2048; //
        mFrequencyView = findViewById(R.id.frequency_view);
        mFrequencyView.setFFTResolution(fftResolution);
        mFrequencyView.setSamplingRate((int)getSingleMediaItem().getMetadata().getAudioSampleRateAsInt());
        mFrequencyView.setDuration(getSingleMediaItem().getMetadata().getAudioDuration());
        mFrequencyView.setBackgroundColor(Color.BLACK);

        // run
        //mFrequencyView.setMagnitudes(re);

        /*
        Resources res = getResources();
        mAnalyzerParam = new AnalyzerParameters(res);
        mAnalyzerParam.audioSourceId = 2000; // testing data
        mAnalyzerParam.wndFuncName = "Hanning";
        mAnalyzerParam.spectrogramDuration = 6.0;
        mAnalyzerParam.overlapPercent = 50.0;
        mAnalyzerParam.fftLen=2048;
        mAnalyzerParam.hopLen = (int)(mAnalyzerParam.fftLen*(1 - mAnalyzerParam.overlapPercent/100) + 0.5);
        mAnalyzerParam.sampleRate = (int)getSingleMediaItem().getMetadata().getAudioSampleRateAsInt();
        mAnalyzerParam.isAWeighting = true;

       // samplingThread = new MusicSamplingLoop(analyzerParam);
        mAnalyzerViews = new AnalyzerViews(this);

        // Settings of graph view
        // spectrum
        mAnalyzerViews.graphView.setShowLines(true);
        // set spectrum show range
        mAnalyzerViews.graphView.setSpectrumDBLowerBound(AnalyzerGraphic.minDB);

        // spectrogram
        mAnalyzerViews.graphView.setSpectrogramModeShifting(false);
        mAnalyzerViews.graphView.setShowTimeAxis(true);
        mAnalyzerViews.graphView.setShowFreqAlongX(true);
        mAnalyzerViews.graphView.setSmoothRender(false);
        mAnalyzerViews.graphView.setColorMap("Hot");
        // set spectrogram show range
        mAnalyzerViews.graphView.setSpectrogramDBLowerBound(mAnalyzerViews.graphView.spectrogramPlot.spectrogramBMP.dBLowerBound);
        mAnalyzerViews.graphView.setLogAxisMode(true);

        mAnalyzerViews.bWarnOverrun = false;
      //  mAnalyzerViews.setFpsLimit();
        mAnalyzerViews.setupView(mAnalyzerParam);
        */
    }

    private void displayDefaultCoverart() {
        final ImageView imageView = findViewById(R.id.image);
        mImageDimensionView = findViewById(R.id.image_dimension);
        Bitmap art = mediaStoreHelper.getArtwork(getSingleMediaItem());
        if(art !=null) {
            int height = art.getHeight();
            int width = art.getWidth();
            mImageDimensionView.setText(width+" x "+height +" px");
            imageView.setImageBitmap(art);
            Palette palette = Palette.from(art).generate();
            int primaryColor = palette.getVibrantColor(getColor(R.color.colorPrimaryDark_light));
            int backgroundColor = palette.getDominantColor(getColor(R.color.grey600));

            mainView.setBackgroundColor(backgroundColor);
        }else {
            imageView.setImageDrawable(getDrawable(R.drawable.ic_music));
            mImageDimensionView.setText("No Coverart");
        }
    }

    private void displayBottomFileMenu() {
        new BottomDialog.Builder(this)
                .autoDismiss(false)
                .setTitle("File Options...")
                .setIcon(R.drawable.ic_save_black_24dp)
                .setMenu(R.menu.menu_editor_file)
                .onMenuItemClick(new BottomDialog.OnMenuItemClick() {
                    @Override
                    public boolean onMenuItemClick(BottomDialog dialog, MenuItem item) {
                        dialog.dismiss();
                        return onOptionsItemSelected(item);
                    }
                })
                .show();
    }


    private void displayBottomTagMenu() {
        new BottomDialog.Builder(this)
                .autoDismiss(false)
                .setTitle("Tags Options...")
                .setIcon(R.drawable.ic_tag_outline_black_24dp)
                .setMenu(R.menu.menu_editor_tag)
                .onMenuItemClick(new BottomDialog.OnMenuItemClick() {
                    @Override
                    public boolean onMenuItemClick(BottomDialog dialog, MenuItem item) {
                        dialog.dismiss();
                        return onOptionsItemSelected(item);
                    }
                })
                .show();
    }

    private void displayBottomImageMenu() {
        new BottomDialog.Builder(this)
                .autoDismiss(false)
                .setTitle("Cover Art Options...")
                .setIcon(R.drawable.ic_image_black_24dp)
                .setMenu(R.menu.menu_editor_image)
                .onMenuItemClick(new BottomDialog.OnMenuItemClick() {
                    @Override
                    public boolean onMenuItemClick(BottomDialog dialog, MenuItem item) {
                        dialog.dismiss();
                        return onOptionsItemSelected(item);
                    }
                })
                .show();
    }

    private void displayMusicBrainz() {
        final List<RecordingItem> songs = new ArrayList<>();
        //RxAndroid
        Observable<Boolean> observable = Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                songs.addAll(MusicBrainz.findSongInfo(displayTag.getTitle(), displayTag.getArtist(),null));
                return true;
            }
        });
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {
                // start progress
                startProgressBar();
            }

            @Override
            public void onNext(Boolean actionResult) {
                stopProgressBar();
            }

            @Override
            public void onError(Throwable e) {
                stopProgressBar();
            }

            @Override
            public void onComplete() {
                if(songs.size()>0) {
                    mMusicBrainzRCView = findViewById(R.id.musicbrainz_items_list);
                    FlexibleAdapter adapter = new FlexibleAdapter(songs);
                    adapter.addListener(MediaTagEditorActivity.this);
                    mMusicBrainzRCView.setAdapter(adapter);
                    mMusicBrainzRCView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
                    RecyclerView.ItemDecoration itemDecoration = new LinearDividerItemDecoration(MediaTagEditorActivity.this, Color.WHITE, 2);
                    mMusicBrainzRCView.addItemDecoration(itemDecoration);
                    mMusicBrainzView.setVisibility(View.VISIBLE);
                }else {
                    mSnackbar = Snacky.builder().setActivity(MediaTagEditorActivity.this)
                            .setText("No matched found on MusicBrainz!")
                            .setDuration(Snacky.LENGTH_LONG)
                            .setMaxLines(1)
                            .error();
                    mSnackbar.show();
                }
                stopProgressBar();
            }
        });
    }

    private void displaySpectrum() {
        // start progress
        startProgressBar();
        mSpectrumView.setVisibility(View.VISIBLE);

        try {
            Thread analyzer = new FLACAnalyzer(this, mFrequencyView, fftResolution, new FileInputStream(getSingleMediaItem().getPath()));
            analyzer.start();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        stopProgressBar();
    }
/*
    private SamplingLoop getAnalyzer() {
        try {
            if (getSingleMediaItem().getPath().toLowerCase().endsWith("wav")) {
                return new FLACAnalyzer(mAnalyzerParam, fftResolution, new FileInputStream(getSingleMediaItem().getPath()));
            } else if (getSingleMediaItem().getPath().toLowerCase().endsWith("flac")) {
                return new FLACAnalyzer(mAnalyzerParam, fftResolution, new FileInputStream(getSingleMediaItem().getPath()));
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
*/

    private void doSaveMediaItem() {
        if(!(tagChanged || coverartChanged)) {
            return;
        }
        for(MediaItem item:mediaItems) {
            item.setArtworkPath(null);
            if(coverartChanged && coverartFile!=null) {
                item.setArtworkPath(coverartFile.getAbsolutePath());
                // This method must be called on the main thread.
                Glide.get(getApplicationContext()).clearMemory();
            }
            if(tagChanged) {
                MediaTag tagUpdate = prepareNewTags(item);
                item.setNewTag(tagUpdate);
            }
            FileManagerService.addToSave(item);
        }
        if(isSingleMediaItem()) {
            MediaItem mediaItem = getSingleMediaItem();
            final String title = mediaItem.getTitle();
            final String path = mediaItem.getMetadata().getDisplayPath();
        }
        startProgressBar();
        // Construct our Intent specifying the Service
        Intent intent = new Intent(getApplicationContext(), FileManagerService.class);
        // Add extras to the bundle
        intent.putExtra("command", "save");
        // Start the service
        startService(intent);
        setResultData("save",null,null);
        tagChanged = false;
        coverartChanged = false;
        editorCardview.clearFocus();
        toggleSaveFabAction();
    }

    private void hideSingleItemFields() {
        mFormatView.setVisibility(View.GONE);
        mSamplerateView.setVisibility(View.GONE);
       // mBitrateView.setVisibility(View.GONE);
        mDurationView.setVisibility(View.GONE);
        mFileSizeView.setVisibility(View.GONE);
        //mTitleView.setVisibility(View.GONE);
        mLyricsView.setVisibility(View.GONE);
        mMediaPathView.setVisibility(View.GONE);
    }

    private void enableSimpleViewMode() {
        mMiscInfoView.setVisibility(View.GONE);
        mCommentView.setVisibility(View.GONE);
        mLyricsView.setVisibility(View.GONE);
        mComposerView.setVisibility(View.GONE);
       // mDiscView.setVisibility(View.GONE);
       // mYearView.setVisibility(View.GONE);
        if(isSingleMediaItem()) {
            mMediaItemsPanelView.setVisibility(View.GONE);
            mSongListAction.setVisibility(View.GONE);
        }else {
            mSongListAction.setVisibility(View.VISIBLE);
            mSongListAction.setText(getString(R.string.editor_song_list, String.valueOf(mediaItems.size())));
            //showSongList = false;
            //toggleSongListView();
        }
        showSimpleMode = true;
    }

    private void enableFullViewMode() {
        mComposerView.setVisibility(View.VISIBLE);
        mAlbumView.setVisibility(View.VISIBLE);
        mAlbumArtistView.setVisibility(View.VISIBLE);
       // mDiscView.setVisibility(View.VISIBLE);
       // mYearView.setVisibility(View.VISIBLE);
        if(isSingleMediaItem()) {
            mMiscInfoView.setVisibility(View.VISIBLE);
            mCommentView.setVisibility(View.VISIBLE);
            mLyricsView.setVisibility(View.VISIBLE);
            mMediaItemsPanelView.setVisibility(View.GONE);
            mSongListAction.setVisibility(View.GONE);
        }else {
            mMiscInfoView.setVisibility(View.GONE);
            mCommentView.setVisibility(View.GONE);
            mLyricsView.setVisibility(View.GONE);
            mSongListAction.setVisibility(View.VISIBLE);
            //showSongList = true;
            //toggleSongListView();
        }
        showSimpleMode = false;
    }

    private  void toggleSongListView() {
        mSongListAction.setText(getString(R.string.editor_song_list, String.valueOf(mediaItems.size())));
        if(showSongList) {
            mMediaItemsPanelView.setVisibility(View.VISIBLE);
            mMediaItemAdapter.setDisplayHeadersAtStartUp(false);
            mMediaItemAdapter.setStickyHeaders(false);
            mMediaItemAdapter.updateDataSet(getPathItems(false));
            mMediaItemAdapter.notifyDataSetChanged();
            mSongListAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_editor_info, 0, R.drawable.ic_arrow_drop_up_black_24dp, 0);
        }else {
            mMediaItemsPanelView.setVisibility(View.GONE);
            mSongListAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_editor_info, 0, R.drawable.ic_arrow_drop_down_black_24dp, 0);
        }
        showSongList = (!showSongList);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void triggerStorageAccessFramework() {
        if( this.getContentResolver().getPersistedUriPermissions().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, MusicService.REQUEST_CODE_PERMISSION_All);
        }
    }

    private void prepareDisplayTag(List<MediaItem> items) {
        if(items.size()==1) {
            if(!items.get(0).isLoadedEncoding()) {
                mediaStoreHelper.readId3Tag(items.get(0), null);
            }
           displayTag = items.get(0).getTag().clone();
        }else if(items.size()>1){
            MediaItem baseItem = getSingleMediaItem();
            if(!items.get(0).isLoadedEncoding()) {
                mediaStoreHelper.readId3Tag(baseItem, null);
            }
            displayTag = baseItem.getTag().clone();

            displayTag.setTrack("");
            displayTag.setLyrics("");
            for (int i=1;i<items.size();i++) {
                MediaItem item = items.get(i);
                if(!item.isLoadedEncoding()) {
                    mediaStoreHelper.readId3Tag(item, null);
                }
                if(!equals(displayTag.getTitle(), item.getTag().getTitle())) {
                    displayTag.setTitle(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getTrack(), item.getTag().getTrack())) {
                    displayTag.setTrack(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getAlbum(), item.getTag().getAlbum())) {
                    displayTag.setAlbum(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getArtist(), item.getTag().getArtist())) {
                    displayTag.setArtist(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getAlbumArtist(), item.getTag().getAlbumArtist())) {
                    displayTag.setAlbumArtist(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getGenre(), item.getTag().getGenre())) {
                    displayTag.setGenre(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getYear(), item.getTag().getYear())) {
                    displayTag.setYear(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getTrackTotal(), item.getTag().getTrackTotal())) {
                    displayTag.setTrackTotal(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getDisc(), item.getTag().getDisc())) {
                    displayTag.setDisc(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getDiscTotal(), item.getTag().getDiscTotal())) {
                    displayTag.setDiscTotal(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getComment(), item.getTag().getComment())) {
                    displayTag.setComment(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getComposer(), item.getTag().getComposer())) {
                    displayTag.setComposer(StringUtils.MULTI_VALUES);
                }
                if(!equals(displayTag.getGrouping(), item.getTag().getGrouping())) {
                    displayTag.setCountry(StringUtils.MULTI_VALUES);
                }
            }
        }
    }

    private boolean equals(String album, String album1) {
       return StringUtils.trimToEmpty(album).equals(StringUtils.trimTitle(album1));
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

    private void setUpProgressBar() {
        mMaterialProgressBar = findViewById(R.id.progress_bar);
        mMaterialProgressBar.setVisibility(View.GONE);
        //   mMaterialProgressBar.setVisibility(View.INVISIBLE);
    }

    private void updateAudioFormatQualityView(MediaMetadata.MediaQuality quality) {
        switch (quality) {
            case HIRES:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
              //  mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_hires));
                break;
            case HIGH:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_high));
               // mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_high));
                break;
            case GOOD:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_good));
              //  mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_good));
                break;
            case LOW:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_low));
               // mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_low));
                break;
            default:
                mFormatView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                mSamplerateView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
               // mBitrateView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                mDurationView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                mFileSizeView.setBackground(getDrawable(R.drawable.shape_round_format_normal));
                break;
        }
    }

    private MediaTag prepareNewTags(MediaItem item) {
        final MediaTag tagUpdate = item.getTag().clone();
        if(isSingleMediaItem()) {
            if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mLyricsView)) ) {
                tagUpdate.setLyrics(getText(mLyricsView));
            }
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mTitleView)) ) {
            tagUpdate.setTitle(getText(mTitleView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mTrackView)) ) {
            tagUpdate.setTrack(getText(mTrackView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mAlbumView)) ) {
            tagUpdate.setAlbum(getText(mAlbumView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mAlbumArtistView)) ) {
            tagUpdate.setAlbumArtist(getText(mAlbumArtistView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mArtistView)) ) {
            tagUpdate.setArtist(getText(mArtistView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mComposerView)) ) {
            tagUpdate.setComposer(getText(mComposerView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mCommentView)) ) {
            tagUpdate.setComment(getText(mCommentView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mCountryView)) ) {
            tagUpdate.setCountry(getText(mCountryView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mGenreView)) ) {
            tagUpdate.setGenre(getText(mGenreView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mYearView)) ) {
            tagUpdate.setYear(getText(mYearView));
        }
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(getText(mDiscView)) ) {
            tagUpdate.setDisc(getText(mDiscView));
        }
        return tagUpdate;
    }

    private String getText(TextView textView) {
        return StringUtils.trimToEmpty(String.valueOf(textView.getText()));
    }

    private List<PathItem> getPathItems(boolean withOrganizedPath) {
        List<PathItem> pathList = new ArrayList<>();
        for(MediaItem item: mediaItems) {
            String path = item.getMetadata().getDisplayPath();
            if(withOrganizedPath) {
                path = mediaStoreHelper.getOrganizedPath(item);
                path = mediaStoreHelper.getDisplayPath(path);
            }
            pathList.add(new PathItem(item.getId(), item.getTitle(), item.getSubtitle(), path));
        }
        return pathList;
    }

    private void setResultData(String action, String path, String oldPath) {
       if(mResultData==null) {
           mResultData = new Intent();
       }
       if(action!=null) {
           mResultData.putExtra("action", action);
       }
       setResult(RESULT_OK,mResultData);
    }

    private void toggleSaveFabAction(){
        if(tagChanged || coverartChanged) {
            ViewCompat.animate(fabSaveAction)
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f).setDuration(200)
                    .setStartDelay(300L)
                    .start();
        }else {
            ViewCompat.animate(fabSaveAction)
                    .scaleX(0f).scaleY(0f)
                    .alpha(0f).setDuration(100)
                    .start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
 /*
        getMenuInflater().inflate(R.menu.menu_editor_file_main, menu);
        for(int i = 0; i < menu.size(); i++){
            MenuItem item = menu.getItem(i);
            UIUtils.setColorFilter(item,  getColor(R.color.menu_delete_background));

            switch (item.getItemId()) {
                case R.id.menu_main_delete_file:
                    //if(mediaItems.size()==1) {
                        UIUtils.setColorFilter(item, getColor(R.color.menu_delete_background));
                    //}else {
                    //    item.setEnabled(false);
                    //}
                    break;
                case R.id.menu_file_manager:
                    if(!isSingleMediaItem()) {
                        item.setEnabled(false);
                    }
                    UIUtils.setColorFilter(item,  getColor(R.color.menu_delete_background));
                    break;
                case R.id.menu_play_song:
                    if(!isSingleMediaItem()) {
                        item.setEnabled(false);
                    }
                    UIUtils.setColorFilter(item,  getColor(R.color.menu_delete_background));
                    break;
                default:
                    UIUtils.setColorFilter(item,  getColor(R.color.menu_delete_background));
                    break;
            }
        }
        */
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(FileManagerService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mOperationReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mOperationReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_main_delete_file:
                doDeleteMediaItem();
                break;
            case R.id.menu_main_transfer_file:
                doMoveMediaItem();
                break;
            case R.id.menu_file_manager:
                doOpenFileManager();
                break;
            case R.id.menu_change_charset:
                //FIXME not decode properly yet
                doCreateCharsetOptions();
                //doChangeCharset("TIS-620");
                break;
            case R.id.menu_format_tag:
                doFormatTags();
                break;
            //case R.id.menu_label_simple:
             case R.id.menu_label_smart_reader:
             //case R.id.menu_label_smart_reader2:
             case R.id.menu_label_hierarchy:
                 doReadTags(item);
                 break;
             case R.id.menu_label_swap_artist:
                 doSwapArtistTitle();
                 break;
            case R.id.menu_play_song:
                doPlayOnMusicPlayer();
                break;
            case R.id.menu_label_pick_cover_image:
                pickCoverart();
                break;
            case R.id.menu_label_save__cover_image:
                saveCoverart();
                break;
            //case R.id.menu_label_remove__cover_image:
            //    removeCoverart();
            //    break;
            case R.id.menu_view_mode:
                if(showSimpleMode) {
                    enableFullViewMode();
                    item.setIcon(R.drawable.ic_view_stream_black_24dp);
                }else {
                    enableSimpleViewMode();
                    item.setIcon(R.drawable.ic_view_list_black_24dp);
                }
                break;
            case R.id.menu_editor_musicbrainz:
                if(mMusicBrainzView.getVisibility() == View.GONE) {
                    displayMusicBrainz();
                }else {
                    mMusicBrainzView.setVisibility(View.GONE);
                }
                break;
            case R.id.menu_editor_file_main:
                displayBottomFileMenu();
                break;
            case R.id.menu_editor_tags_main:
                displayBottomTagMenu();
                break;
            case R.id.menu_editor_spectrum:
                if(mSpectrumView.getVisibility() == View.GONE) {
                    displaySpectrum();
                }else {
                    mSpectrumView.setVisibility(View.GONE);
                }
                break;

            //case R.id.menu_view_simple:
            //    enableSimpleViewMode();
            //    break;
        }
        toggleSaveFabAction();
        return super.onOptionsItemSelected(item);
    }

    private void pickCoverart() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,REQUEST_GET_CONTENT_IMAGE);
    }

    private void removeCoverart() {
        coverartFile = null;
        coverartChanged = true;
       // final ImageView image = findViewById(R.id.image);
       // image.reImageResource(null);
    }

    private void saveCoverart() {
            File theFilePath = mediaStoreHelper.getDownloadPath(getSingleMediaItem().getTitle()+".png");
            mediaStoreHelper.saveArtworkToFile(getSingleMediaItem(), theFilePath.getAbsolutePath());
            Snacky.builder().setActivity(MediaTagEditorActivity.this)
                    .setText("save artwork on "+theFilePath.getName())
                    .setDuration(Snacky.LENGTH_LONG)
                    .setMaxLines(1)
                    .success()
                    .show();
    }

    private void doDeleteMediaItem() {
        final String selectedSongs = String.valueOf(mediaItems.size());

        verifyStoragePermissions();;
        List<PathItem> pathList = getPathItems(false);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.view_custom_media_editor, null);
        RecyclerView filesView = customView.findViewById(R.id.custom_files_view);
        UIUtils.setHeight(filesView, 110, pathList.size(),240);
        filesView.setAdapter(new FlexibleAdapter(pathList));
        filesView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        RecyclerView.ItemDecoration itemDecoration = new LinearDividerItemDecoration(this, Color.WHITE,2);
        filesView.addItemDecoration(itemDecoration);

        new BottomDialog.Builder(this)
                .autoDismiss(false)
                .setTitle(getString(R.string.alert_delete_confirm_many,selectedSongs))
               // .setContent(getString(R.string.alert_delete_confirm_many,selectedSongs))
                .setIcon(R.drawable.ic_delete_black_24dp)
                //.setCustomView(customView,8,8,8,10)
                .setCustomView(customView,0,8,0,0)
                .setPositiveText(R.string.alert_btn_delete)
                .setPositiveBackgroundColorResource(R.color.colorPrimary)
                .setPositiveTextColorResource(android.R.color.white)
                .onPositive(new BottomDialog.ButtonCallback() {
                    @Override
                    public void onClick(BottomDialog dialog) {
                        for (MediaItem item : mediaItems) {
                            FileManagerService.addToDelete(item);
                        }
                        // Construct our Intent specifying the Service
                        Intent intent = new Intent(getApplicationContext(), FileManagerService.class);
                        // Add extras to the bundle
                        intent.putExtra("command", "delete");
                        // Start the service
                        startService(intent);
                        dialog.dismiss();
                        setResultData("delete",null,null);
                        onBackPressed();
                    }
                })
                .setNegativeText(R.string.alert_btn_cancel)
                .onNegative(new BottomDialog.ButtonCallback() {
                    @Override
                    public void onClick(@NonNull BottomDialog bottomDialog) {
                        bottomDialog.dismiss();
                    }
                })
                .show();
    }

    private void doMoveMediaItem() {
        final String selctedSongs = String.valueOf(mediaItems.size());

        verifyStoragePermissions();
        List<PathItem> pathList = getPathItems(true);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.view_custom_media_editor, null);
        RecyclerView filesView = customView.findViewById(R.id.custom_files_view);
        UIUtils.setHeight(filesView, 110, pathList.size(),240);
        filesView.setAdapter(new FlexibleAdapter(pathList));
        filesView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        RecyclerView.ItemDecoration itemDecoration = new LinearDividerItemDecoration(this, Color.WHITE,2);
        filesView.addItemDecoration(itemDecoration);

        new BottomDialog.Builder(this)
                .autoDismiss(false)
                .setTitle(getString(R.string.alert_organize_confirm_many, selctedSongs))
                //.setContent(getString(R.string.alert_organize_confirm_many, selctedSongs))
                //.setCustomView(customView,8,8,8,10)
                .setCustomView(customView,0,8,0,0)
                .setIcon(R.drawable.ic_move_to_inbox_black_24dp)
                .setPositiveText(R.string.alert_btn_move)
                .setPositiveBackgroundColorResource(R.color.colorPrimary)
                .setPositiveTextColorResource(android.R.color.white)
                .onPositive(new BottomDialog.ButtonCallback() {
                    @Override
                    public void onClick(BottomDialog dialog) {
                        for(MediaItem item:mediaItems) {
                            FileManagerService.addToMove(item);
                        }
                        startProgressBar();
                        // Construct our Intent specifying the Service
                        Intent intent = new Intent(getApplicationContext(), FileManagerService.class);
                        // Add extras to the bundle
                        intent.putExtra("command", "move");
                        // Start the service
                        startService(intent);
                        setResultData("move",null,null);
                        dialog.dismiss();
                    }
                })
                .setNegativeText(R.string.alert_btn_cancel)
                .onNegative(new BottomDialog.ButtonCallback() {
                    @Override
                    public void onClick(@NonNull BottomDialog bottomDialog) {
                        bottomDialog.dismiss();
                    }
                })
                .show();
    }

    private void doPlayOnMusicPlayer() {
        if(!isSingleMediaItem()) {
            return;
        }
        Intent intent = new Intent();
       // intent.setAction(Intent.ACTION_VIEW);

        // Get URI and MIME type of file
        //Uri uri = Uri.parse("file://"+ getSingleMediaItem().getPath());
        Uri uri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                new File(getSingleMediaItem().getPath()));
       // String mime = getContentResolver().getType(uri);
        String mime= "audio/*";
        //String mime= "*/*";
        intent.setDataAndType(uri, mime);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        //shareIntent.setType("image/jpeg");

       // startActivity(Intent.createChooser(intent, "Open file with"));
        //startActivity(intent);

        startDefaultAppOrPromptUserForSelection(uri, mime);
    }

    private void findMusicPlayersByLuancher(List<PackageListItem> items) {
        final String[] apps = {
                // package // name - nb installs (thousands)
                "com.neutroncode.mp", // Neutron Player - 5 000
                "com.radsone.dct", // Radsone - 10 000
                "com.wiseschematics.resoundmethods01", // T Player - 5 000
                "com.android.bbkmusic", // Vivo Music - 5 000
                "com.onkyo.jp.musicplayer" }; // OnKyo - 5 000 };
        final PackageManager packageManager = getPackageManager();

        for (int i = 0; i < apps.length; i++) {
            Intent intent = packageManager.getLaunchIntentForPackage(apps[i]);
            if(intent!=null) {
                List<ResolveInfo> resInfos = packageManager.queryIntentActivities(intent,0);

                for (ResolveInfo resInfo : resInfos) {
                    String context = resInfo.activityInfo.packageName;
                    String packageClassName = resInfo.activityInfo.name;
                    CharSequence label = resInfo.loadLabel(packageManager);
                    Drawable icon = resInfo.loadIcon(packageManager);
                    items.add(new PackageListItem(label.toString(), icon, context, packageClassName));
                }
               // String context = resInfo.activityInfo.packageName;
               // String packageClassName = intent.get resInfo.activityInfo.name;
               // CharSequence label = packageManager.getAcresInfo.loadLabel(pm);
               // Drawable icon = packageManager.getActivityIcon(intent);
               // items.add(new PackageListItem(label.toString(), icon, context, packageClassName));
            }
        }
    }

    public void startDefaultAppOrPromptUserForSelection(final Uri uri, final String mime) {
        final List <PackageListItem> items = new ArrayList();
        findMusicPlayersByIntent(items);
        findMusicPlayersByLuancher(items);

            ListAdapter adapter = new ArrayAdapter<PackageListItem>(
                    this,
                    android.R.layout.select_dialog_item,
                    android.R.id.text1,
                    items){

                public View getView(int position, View convertView, ViewGroup parent) {
                    // User super class to create the View
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView)v.findViewById(android.R.id.text1);

                    // Put the icon drawable on the TextView (support various screen densities)
                    int dpS = (int) (32 * getResources().getDisplayMetrics().density * 0.5f);
                    items.get(position).icon.setBounds(0, 0, dpS, dpS);
                    tv.setCompoundDrawables(items.get(position).icon, null, null, null);

                    // Add margin between image and name (support various screen densities)
                    int dp5 = (int) (5 * getResources().getDisplayMetrics().density  * 0.5f);
                    tv.setCompoundDrawablePadding(dp5);

                    return v;
                }
            };

            // Build the list of send applications
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Play by:");
           // builder.setIcon(R.drawable.ic_play_arrow_black_24dp);

            // Set the adapter of items in the list
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    // Start the selected activity sending it the URLs of the resized images
                    Intent intent;
                    intent = new Intent(Intent.ACTION_VIEW);
                    //intent.setType("audio/*");
                    intent.setData(uri);
                    //intent.setDataAndType(uri, mime);
                   // intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setClassName(items.get(which).context, items.get(which).packageClassName);
                    startActivity(intent);
                    finish();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

    }

    private void findMusicPlayersByIntent(List<PackageListItem> items) {
        String action = Intent.ACTION_VIEW;

        // Get list of handler apps that can send
        Intent intent = new Intent(action);
        //intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("audio/*");
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);

        // Referenced http://stackoverflow.com/questions/3920640/how-to-add-icon-in-alert-dialog-before-each-item

        for (ResolveInfo resInfo : resInfos) {
            String context = resInfo.activityInfo.packageName;
            String packageClassName = resInfo.activityInfo.name;
            CharSequence label = resInfo.loadLabel(pm);
            Drawable icon = resInfo.loadIcon(pm);
            items.add(new PackageListItem(label.toString(), icon, context, packageClassName));
        }
    }

    private void doSwapArtistTitle() {
        for(MediaItem mItem:mediaItems) {
            MediaTag tag = mItem.getTag();
            String title = StringUtils.trimToEmpty(tag.getTitle());
            String artist = StringUtils.trimToEmpty(tag.getArtist());
            tag.setTitle(mediaStoreHelper.formatTitle(artist));
            tag.setArtist(mediaStoreHelper.formatTitle(title));
        }
        prepareDisplayTag(mediaItems);
        updateTagView(false);
    }

    private void doReadTags(MenuItem item) {
        //enable for single item only
        TagReader reader = new TagReader();
        for(MediaItem mItem:mediaItems) {
            String mediaPath =  mItem.getPath();
            File file = new File(mediaPath);
            if(!file.exists()) continue;
            MediaTag newTag = null;
            if(item.getItemId() == R.id.menu_label_smart_reader) {
                newTag = reader.parser(mItem, TagReader.READ_MODE.SM1);
            }else {
                // menu_label_hierarchy
                newTag = reader.parser(mItem, TagReader.READ_MODE.HIERARCHY);
            }
            if(newTag!=null) {
                MediaTag tag = mItem.getTag();
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
            }
        }
        prepareDisplayTag(mediaItems);
        updateTagView(false);
    }

    private void doCreateCharsetOptions() {
        View anchor = findViewById(R.id.menu_editor_tags_main);
        @SuppressLint("RestrictedApi") Context wrapper = new ContextThemeWrapper(this, R.style.PopupMenuStyle);
        final PopupMenu popupMenu = new PopupMenu(wrapper, anchor);
        UIUtils.makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //onOptionsItemSelected(item);
                return true;
            }
        });
        Map<String, Charset> charsets = Charset.availableCharsets();
        String title = displayTag.getTitle();
        for(Charset charset:charsets.values()) {
            popupMenu.getMenu().add(charset.name() +" - "+encodeText(displayTag.getTitle(), charset.name()));
        }
/*
        MenuItem menuItem = popupMenu.getMenu().add(encodeText(displayTag.getTitle(), "TIS-620"));
        menuItem = popupMenu.getMenu().add(encodeText(displayTag.getTitle(), "MS874"));
        menuItem = popupMenu.getMenu().add(encodeText(displayTag.getTitle(), "CP874"));
        menuItem = popupMenu.getMenu().add(encodeText(displayTag.getTitle(), "ISO8859_1"));
        menuItem = popupMenu.getMenu().add(encodeText(displayTag.getTitle(), "ISO8859_11"));
        menuItem = popupMenu.getMenu().add(encodeText(displayTag.getTitle(), "UTF-8"));
*/
        //popupMenu.inflate(R.menu.menu_editor_full);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            UIUtils.setColorFilter(item, getColor(R.color.menu_editor_background));
        }
        popupMenu.show();
    }

    private void doOpenFileManager() {
        if(!isSingleMediaItem()) {
            return;
        }
        File mediaFile = new File(getSingleMediaItem().getPath());
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
        startActivity(intent);
    }

    private void updateTagView(boolean silentUpdate) {
        // title
        mTitleView.setText(StringUtils.trimToEmpty(displayTag.getTitle()));
        // artist
        mArtistView.setText(StringUtils.trimToEmpty(displayTag.getArtist()));
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
        mMediaPathView.setText(StringUtils.trimToEmpty(getSingleMediaItem().getMetadata().getDisplayPath()));
        if(silentUpdate) {
            tagChanged = false;
        }
    }

    private void doFormatTags() {
        for(MediaItem mItem:mediaItems) {
            MediaTag tag = mItem.getTag();
            tag.setTitle(mediaStoreHelper.formatTitle(tag.getTitle()));
            tag.setArtist(mediaStoreHelper.formatTitle(tag.getArtist()));
            tag.setAlbum(mediaStoreHelper.formatTitle(tag.getAlbum()));
            tag.setAlbumArtist(mediaStoreHelper.formatTitle(tag.getAlbumArtist()));
            tag.setGenre(mediaStoreHelper.formatTitle(tag.getGenre()));
            // clean albumArtist if same value as artist
            if(tag.getAlbumArtist().equals(tag.getAlbum())) {
                tag.setAlbumArtist("");
            }
            // if album empty, add single
            if(StringUtils.isEmpty(tag.getAlbum()) && !StringUtils.isEmpty(tag.getArtist())) {
                tag.setAlbum(tag.getArtist()+" - Single");
            }
        }
        prepareDisplayTag(mediaItems);
        updateTagView(false);
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
        }else if(requestCode == REQUEST_GET_CONTENT_IMAGE) {
            try {
                    if (resultData == null || resultData.getData() == null) {
                        return;
                    }
                    InputStream input = getContentResolver().openInputStream(resultData.getData());
                    MediaItem item = getSingleMediaItem();
                    File outputDir = getCacheDir(); // context being the Activity pointer
                    coverartFile = new File(outputDir, item.getId() + "_cover_art");
                    if (coverartFile.exists()) {
                        coverartFile.delete();
                    }
                    AndroidFile.copy(input, coverartFile);
                    final ImageView image = findViewById(R.id.image);
                    GlideApp.with(getApplicationContext())
                            .load(coverartFile)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .signature(new ObjectKey(String.valueOf(coverartFile.lastModified())))
                            .into(image);
                    coverartChanged = true;
                    toggleSaveFabAction();

            }catch (IOException ex) {
                LogHelper.e(TAG, ex);
            }
        }else if(requestCode == REQUEST_SAVE_FILE_RESULT_CODE) {
            if (resultCode==RESULT_OK && resultData!=null && resultData.getData()!=null) {
                String theFilePath = resultData.getData().getPath();
                mediaStoreHelper.saveArtworkToFile(getSingleMediaItem(), theFilePath);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(tagChanged || coverartChanged) {
            if(tagChanged) {
                final Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        for (MediaItem item : mediaItems) {
                            mediaStoreHelper.readId3Tag(item, null);
                        }
                        prepareDisplayTag(mediaItems);
                        updateTagView(true);
                        tagChanged = false;
                        toggleSaveFabAction();
                    }
                };
                new iOSDialogBuilder(this)
                        .setTitle(getString(R.string.alert_warning_title))
                        .setSubtitle(getString(R.string.alert_media_file_not_save))
                        .setBoldPositiveLabel(true)
                        .setCancelable(false)
                        .setPositiveListener(getString(R.string.alert_btn_ok),new iOSDialogClickListener() {
                            @Override
                            public void onClick(iOSDialog dialog) {
                                dialog.dismiss();
                                run.run();
                            }
                        })
                        .setNegativeListener(getString(R.string.alert_btn_cancel), new iOSDialogClickListener() {
                            @Override
                            public void onClick(iOSDialog dialog) {
                                dialog.dismiss();
                            }
                        })
                        .build().show();
            }
            if(coverartChanged) {
                displayDefaultCoverart();
                coverartFile = null;
                coverartChanged = false;
            }
            toggleSaveFabAction();
            return;
            //super.onBackPressed();
        }
        super.onBackPressed();
    }

    private void doChangeCharset(String charset) {
        if(StringUtils.isEmpty(charset)) {
            // re-load from media file
            for (MediaItem item : mediaItems) {
                mediaStoreHelper.readId3Tag(item, null);
            }
        }else {
            for(MediaItem item: mediaItems) {
                MediaTag tag = item.getTag();
                tag.setTitle(encodeText(tag.getTitle(),charset));
                tag.setArtist(encodeText(tag.getArtist(),charset));
                tag.setAlbum(encodeText(tag.getAlbum(),charset));
                tag.setAlbumArtist(encodeText(tag.getAlbumArtist(),charset));
                tag.setGenre(encodeText(tag.getGenre(),charset));
                tag.setComment(encodeText(tag.getComment(),charset));
                tag.setComposer(encodeText(tag.getComment(),charset));
                tag.setLyrics(encodeText(tag.getLyrics(),charset));
                tag.setCountry(encodeText(tag.getGrouping(),charset));
            }
        }
        prepareDisplayTag(mediaItems);
        updateTagView(true);
    }

    private String encodeText(String text, String encode) {
        if(StringUtils.isEmpty(encode)) {
            return text;
        }
        if(StringUtils.isEmpty(text)) {
            return "";
        }
        try {
            //return new String(text.getBytes(ANSI_CHARSET), encode);
            //String temp = new String(text.getBytes(), encode);
            //return new String(temp.getBytes(), "UTF-8");
            byte [] byteString = ASCIIToUnicode(text).getBytes();
            return new String(byteString,encode);
//            return convertToThaiTIS620();
            //return new String(text.getBytes(ANSI_CHARSET), encode);
            //return new String(theString.getBytes("UTF-8"));
        } catch (Exception e) {
            return text;
        }
    }

    public static String ASCIIToUnicode(String ascii) {
//initial temporary space of unicode
        StringBuffer unicode = new StringBuffer(ascii);
        int code;

//continue loop based on number of character.
        for (int i = 0; i < ascii.length(); i++) {
            code = (int) ascii.charAt(i);

//check the value is Thai language in ASCII scope or not.
            if ((0xA1 <= code) && (code <= 0xFB)) {
//if yes, it will be converted to Thai language in Unicode scope.
                unicode.setCharAt(i, (char) (code + 0xD60));
            }
        }

//convert unicode to be as String type to use continue.
        return unicode.toString();
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

        private void updateBackground(FloatingActionButton fab, Palette palette) {
            int lightVibrantColor = palette.getLightVibrantColor(getResources().getColor(android.R.color.white, getTheme()));
            int vibrantColor = palette.getVibrantColor(getResources().getColor(R.color.bt_accent, getTheme()));

            fab.setRippleColor(lightVibrantColor);
            fab.setBackgroundTintList(ColorStateList.valueOf(vibrantColor));
        }

    @Override
    public boolean onItemClick(View view, int position) {
        // update tag
        if(mMusicBrainzView==null) return false;

        final RecordingItem item = (RecordingItem) ((FlexibleAdapter)mMusicBrainzRCView.getAdapter()).getItem(position);
        final View customView = getLayoutInflater().inflate(R.layout.view_tags_selecter_view, null);
            ImageView imageView = customView.findViewById(R.id.coverart);
            if(imageView!=null) {
                GlideApp.with(getApplicationContext())
                        .load(item)
                        .into(imageView);
            }
            setTextView(customView, R.id.title, item.title);
            setTextView(customView, R.id.artist, item.artist);
            setTextView(customView, R.id.album, item.album);
            setTextView(customView, R.id.genre, item.genre);
            setTextView(customView, R.id.year, item.year);

            final Runnable runner = new Runnable() {
                @Override
                public void run() {
                    displayTag.setTitle(getTextViewValue(customView, R.id.title));
                    displayTag.setArtist(getTextViewValue(customView, R.id.artist));
                    displayTag.setAlbum(getTextViewValue(customView, R.id.album));
                    displayTag.setYear(getTextViewValue(customView, R.id.year));
                    displayTag.setGenre(getTextViewValue(customView, R.id.genre));
                    if(item.getAlbumItem()!=null && item.getAlbumItem().getLargeCoverUrl()!=null){
                        // FIXME load image to temp
                        // save image to file
                        GlideApp.with(getApplicationContext())
                                .asBitmap()
                                .load(item.getAlbumItem().getLargeCoverUrl())
                                .into(new SimpleTarget<Bitmap>(MusicMateArtwork.MAX_ALBUM_ART_SIZE,MusicMateArtwork.MAX_ALBUM_ART_SIZE) {
                                    @Override
                                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                        File outputDir = getCacheDir(); // context being the Activity pointer
                                        coverartFile = new File(outputDir, getSingleMediaItem().getId() + "_cover_art");
                                        if (coverartFile.exists()) {
                                            coverartFile.delete();
                                        }
                                        AndroidFile.saveImage(resource, coverartFile);
                                        final ImageView image = findViewById(R.id.image);
                                        GlideApp.with(getApplicationContext())
                                                .load(coverartFile)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .signature(new ObjectKey(String.valueOf(coverartFile.lastModified())))
                                                .into(image);
                                        coverartChanged = true;
                                    }
                                });
                    }
                    updateTagView(false);
                }
            };

        new BottomDialog.Builder(this)
                .setTitle(R.string.alert_select_tags)
                .setPositiveText(R.string.alert_btn_set)
                .setCustomView(customView,8,8,8,10)
                .setPositiveBackgroundColorResource(R.color.colorPrimary)
                .setPositiveTextColorResource(android.R.color.white)
                .onPositive(new BottomDialog.ButtonCallback() {
                    @Override
                    public void onClick(BottomDialog dialog) {
                        runner.run();
                        dialog.dismiss();
                    }
                })
                .setNegativeText(R.string.alert_btn_cancel)
                .show();
        return true;
            /*
            new MaterialStyledDialog.Builder(this)
                .setCustomView(customView,8,8,8,10)
                .setPositiveText(R.string.alert_btn_set)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, DialogAction which) {
                        runner.run();
                        dialog.dismiss();
                    }
                })
                .setNegativeText(R.string.alert_btn_cancel)
                .show();
                */
    }

    private void setTextView(View customView, int rescId, String title) {
        TextView textView = customView.findViewById(rescId);
        if(textView!=null) {
            textView.setText(StringUtils.trimToEmpty(title));
        }
    }

    private String getTextViewValue(View customView, int rescId) {
        TextView textView = customView.findViewById(rescId);
        if(textView!=null) {
            return String.valueOf(textView.getText());
        }
        return "";
    }

    // Class for a singular activity item on the list of apps to send to
    class PackageListItem {
        public final String name;
        public final Drawable icon;
        public final String context;
        public final String packageClassName;
        public PackageListItem(String text, Drawable icon, String context, String packageClassName) {
            this.name = text;
            this.icon = icon;
            this.context = context;
            this.packageClassName = packageClassName;
        }
        @Override
        public String toString() {
            return name;
        }
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

    private class MediaItemsEditorAdapter extends FlexibleAdapter {
        public MediaItemsEditorAdapter(ArrayList arrayList) {
            super(arrayList);
        }
    }

    // Define the callback for what to do when data is received
    private BroadcastReceiver mOperationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);
            if (resultCode == RESULT_OK) {
                //int mediaId = intent.getIntExtra("mediaId", -1);
                String command = intent.getStringExtra("command");
                String status = intent.getStringExtra("status");
                String message = intent.getStringExtra("message");
                int total = intent.getIntExtra("totalItems",-1);
                int current = intent.getIntExtra("currentItem",-1);

                if(current >= total) {
                    stopProgressBar();
                    //mDialogHelper.dismissAlert();
                }
                if("success".equalsIgnoreCase(status)) {
                   if(current >= total) {
                       // remove snackbar
                       if(mSnackbar!=null) {
                           mSnackbar.dismiss();
                           mSnackbar = null;
                       }
                       if ("delete".equalsIgnoreCase(command)) {
                            // back to main activity after delete all
                            onBackPressed();
                        }else {
                            // refresh view
                            prepareDisplayTag(mediaItems);
                            updateTagView(true); // silent mode
                            toggleSaveFabAction();
                            if(showSimpleMode) {
                                enableSimpleViewMode();
                                showSimpleMode = true;
                            }else {
                                enableFullViewMode();
                                showSimpleMode = false;
                            }
                        }
                    }
                }
                else if("start".equalsIgnoreCase(status)) {
                   // String text = current + "/" + total+ " - " + message;
                    if(mSnackbar!=null) {
                        mSnackbar.dismiss();
                        mSnackbar = null;
                    }
                    mSnackbar = Snacky.builder().setActivity(MediaTagEditorActivity.this)
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
                    mSnackbar = Snacky.builder().setActivity(MediaTagEditorActivity.this)
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
