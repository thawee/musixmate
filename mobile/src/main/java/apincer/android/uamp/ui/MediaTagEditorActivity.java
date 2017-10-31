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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
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
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.item.MediaItem;
import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.provider.MediaTag;
import apincer.android.uamp.provider.TagReader;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

/**
 * A full screen player that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class MediaTagEditorActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(MediaTagEditorActivity.class);

    private static final String ANSI_CHARSET="ISO8859_1";

    public static final String ARG_ID = "_id";
    public static final String ARG_MEDIA_PATH = "media_path";
    public static final String ARG_MEDIA_POSITION = "media_position";
    public static final String ARG_MEDIA_DURATION = "media_duration";
    public static final String ARG_MEDIA_OLD_PATH = "media_old_path";
    public static final String ARG_MEDIA_TITLE = "media_title";
    public static final String ARG_MEDIA_ARTIST = "media_artist";
    public static final String ARG_MEDIA_ALBUM = "media_album";
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private MediaProvider mediaStoreHelper;
    private String itemId;
    private String mediaPath;
    private long mediaDuration;
    private int mediaPosition;
    private TextView mTitleView;
    private AutoCompleteTextView mArtistView;
    private AutoCompleteTextView mAlbumView;
    private AutoCompleteTextView mAlbumArtistView;
    private TextView mGenreView;
    private TextView mYearView;
    private AutoCompleteTextView mCountryView;
    private TextView mCommentView;
    private TextView mLyricsView;
    private TextView mTrackView;
    private TextView mDiscView;

    private ViewTextWatcher mTextWatcher;

    private TextView mMediaPathView;
    private TextView mMediaInfoView;

    private MediaTag tagUpdate = null;
    private boolean fabStatus = false;
    private FloatingActionButton fabSaveAction;

    private Toolbar mToolbar;

    public static boolean navigate(Activity activity, MediaItem item, int position) {
        Intent intent = new Intent(activity, MediaTagEditorActivity.class);
        MediaProvider mediaMusicTagHelper = new MediaProvider(activity);
        if (mediaMusicTagHelper.isMediaFileExist(item)) {
            intent.putExtra(ARG_ID, item.getId());
            intent.putExtra(ARG_MEDIA_PATH, item.getPath());
            intent.putExtra(ARG_MEDIA_POSITION, position);
            intent.putExtra(ARG_MEDIA_DURATION, item.getDuration());
            ActivityCompat.startActivityForResult(activity, intent, BrowserViewPagerActivity.REQUEST_EDIT_MEDIA_TAG, null);
            return true;
        }else {
            mediaMusicTagHelper.deleteFromMediaStore(item.getPath());
            Toast.makeText(activity, activity.getString(R.string.alert_invalid_media_file, item.getTitle()), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            initActivityTransitions();
            setContentView(R.layout.activity_editor);

            ViewCompat.setTransitionName(findViewById(R.id.app_bar_layout), ARG_MEDIA_PATH);
            supportPostponeEnterTransition();

            mediaStoreHelper = new MediaProvider(this);

            mToolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            itemId = getIntent().getStringExtra(ARG_ID);
            mediaPath = getIntent().getStringExtra(ARG_MEDIA_PATH);
            mediaPosition = getIntent().getIntExtra(ARG_MEDIA_POSITION,0);
            mediaDuration = getIntent().getLongExtra(ARG_MEDIA_DURATION,0);
            tagUpdate = mediaStoreHelper.getMediaTag(mediaPath,mediaDuration);
            if (tagUpdate == null) {
                // should stop
                saveResult("INVALID", getIntent().getStringExtra(ARG_MEDIA_PATH),null);
                finish();
                return;
            }

            collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
            collapsingToolbarLayout.setTitle("");
            collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent, getTheme()));

            fabSaveAction = (FloatingActionButton) this.findViewById(R.id.fab_save_media);
            // save tag action
            fabSaveAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveMediaItem();
                }
            });
            hideFab(); //hide untill tag changed

            // file path
            String path = tagUpdate.getMediaPath();
            mMediaPathView = (TextView) findViewById(R.id.media_path);
            mMediaPathView.setText(tagUpdate.getDisplayPath());

            // file info
           // mMediaInfoView = (TextView) findViewById(R.id.media_info);
           // mMediaInfoView.setText(tagUpdate.getMediaDuration()+ StringUtils.MUSIC_SEP+tagUpdate.getMediaSampleRate()+StringUtils.MUSIC_SEP+tagUpdate.getMediaBitrate()+StringUtils.MUSIC_SEP+tagUpdate.getMediaSize());

           TextView view = (TextView) findViewById(R.id.media_format);
           view.setText(tagUpdate.getMediaFormat());

        view = (TextView) findViewById(R.id.media_samplerate);
        view.setText(tagUpdate.getMediaBitsPerSample()+ StringUtils.MUSIC_SEP+tagUpdate.getMediaSampleRate());

        view = (TextView) findViewById(R.id.media_bitrate);
        view.setText(tagUpdate.getMediaBitrate());

           view = (TextView) findViewById(R.id.media_duration);
           view.setText(tagUpdate.getMediaDuration());

        view = (TextView) findViewById(R.id.media_filesize);
        view.setText(tagUpdate.getMediaSize());

        mTextWatcher =new ViewTextWatcher() ;

        // title
            mTitleView = (TextView) findViewById(R.id.title);
            mTitleView.setText(tagUpdate.getTitle());
            setTitle("");
            mTitleView.addTextChangedListener(mTextWatcher);

            // artist
            mArtistView = (AutoCompleteTextView) findViewById(R.id.artist);
            mArtistView.setText(tagUpdate.getArtist());
            mArtistView.addTextChangedListener(mTextWatcher);
            ArrayAdapter<String> artistAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,mediaStoreHelper.getArtistsAsArray());
            mArtistView.setThreshold(2);//will start working from second character
            mArtistView.setAdapter(artistAdapter); //setting the adapter data into the AutoCompleteTextView

            // album
            mAlbumView = (AutoCompleteTextView) findViewById(R.id.album);
            mAlbumView.setText(tagUpdate.getAlbum());
            mAlbumView.addTextChangedListener(mTextWatcher);
        ArrayAdapter<String> albumAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,mediaStoreHelper.getAlbumAsArray());
        mAlbumView.setThreshold(2);//will start working from second character
        mAlbumView.setAdapter(albumAdapter); //setting the adapter data into the AutoCompleteTextView

            // album artist
            mAlbumArtistView = (AutoCompleteTextView) findViewById(R.id.album_arist);
            mAlbumArtistView.setText(tagUpdate.getAlbumArtist());
            mAlbumArtistView.addTextChangedListener(mTextWatcher);
        ArrayAdapter<String> albumArtistAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,mediaStoreHelper.getAlbumArtistAsArray());
        mAlbumArtistView.setThreshold(2);//will start working from second character
        mAlbumArtistView.setAdapter(albumArtistAdapter); //setting the adapter data into the AutoCompleteTextView

        // year
        mYearView = (TextView) findViewById(R.id.year);
        mYearView.setText(tagUpdate.getYear());
        mYearView.addTextChangedListener(mTextWatcher);

            // disc no
        mDiscView = (TextView) findViewById(R.id.diskno);
        mDiscView.setText(tagUpdate.getDisc());
        mDiscView.addTextChangedListener(mTextWatcher);

            // track
            mTrackView = (TextView) findViewById(R.id.track);
        mTrackView.setText(tagUpdate.getTrack());
        mTrackView.addTextChangedListener(mTextWatcher);

            // genre
            mGenreView = (TextView) findViewById(R.id.genre);
            mGenreView.setText(tagUpdate.getGenre());
            mGenreView.addTextChangedListener(mTextWatcher);

            // country
            mCountryView = (AutoCompleteTextView) findViewById(R.id.country);
            mCountryView.setText(tagUpdate.getCountry());
            mCountryView.addTextChangedListener(mTextWatcher);

            String [] langs = {"eng","tha"};
            ArrayAdapter<String> countryAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,langs);
            mCountryView.setThreshold(0);//will start working from first character
            mCountryView.setAdapter(countryAdapter); //setting the adapter data into the AutoCompleteTextView

            // comment
            mCommentView = (TextView) findViewById(R.id.comment);
            mCommentView.setText(tagUpdate.getComment());
            mCommentView.addTextChangedListener(mTextWatcher);

            // lyrics
            mLyricsView = (TextView) findViewById(R.id.lyrics);
            mLyricsView.setText(tagUpdate.getLyrics());
            mLyricsView.addTextChangedListener(mTextWatcher);

            //default album art
            final ImageView image = (ImageView) findViewById(R.id.image);
            if(tagUpdate.getArtwork()!=null) {
                image.setImageBitmap(tagUpdate.getArtwork());
            }
    }

    private void makePopForceShowIcon(PopupMenu popupMenu) {
        try {
            Field mFieldPopup=popupMenu.getClass().getDeclaredField("mPopup");
            mFieldPopup.setAccessible(true);
            MenuPopupHelper mPopup = (MenuPopupHelper) mFieldPopup.get(popupMenu);
            mPopup.setForceShowIcon(true);
        } catch (Exception e) {

        }
    }

    private void saveMediaItem() {
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
        if(mediaStoreHelper.saveMediaFile(mediaPath, tagUpdate,findViewById(R.id.main_view))) {
            hideFab();
            //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if(inputManager !=null) {
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
            }
            saveResult("SAVED", tagUpdate.getMediaPath(),null);
            ///onBackPressed();
        }
    }

    private String getText(TextView textView) {
        return StringUtils.trimToEmpty(String.valueOf(textView.getText()));
    }

    private void organizeMediaItem() {
        final String path = mediaPath;
        final String organizedPath = mediaStoreHelper.getOrganizedPath(path);
        if(path.equals(organizedPath)) {
            Snackbar.make(findViewById(R.id.main_view), getString(R.string.alert_organize_already), Snackbar.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(MediaTagEditorActivity.this);
        alert.setTitle(getString(R.string.alert_organize_title));
        alert.setMessage("From: \n" + path + "\n\nTo: \n" + organizedPath);
        alert.setPositiveButton(getString(R.string.alert_organize_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mediaStoreHelper.moveMediaFile(path, organizedPath,findViewById(R.id.main_view))) {
                    hideFab();
                    saveResult("ORGANIZED", organizedPath, path);
                    onBackPressed();
                }
                dialog.dismiss();
            }
        });
        alert.setNegativeButton(getString(R.string.alert_organize_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void deleteMediaItem() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MediaTagEditorActivity.this);
        alert.setMessage(getString(R.string.alert_delete_message)+ "\n"+tagUpdate.getTitle());
        alert.setPositiveButton(getString(R.string.alert_delete_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playNextSong(tagUpdate);
                if(mediaStoreHelper.deleteMediaFile(mediaPath,findViewById(R.id.main_view))) {
                    hideFab();
                    saveResult("DELETED", tagUpdate.getMediaPath(),null);
                    onBackPressed();
                }
                dialog.dismiss();
            }
        });
        alert.setNegativeButton(getString(R.string.alert_delete_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void playNextSong(MediaTag tagUpdate) {
        MusicService service = MusicService.getRunningService();
        if(service!=null) {
            service.nextSong(trimText(tagUpdate.getTitle()));
        }
    }

    private void saveResult(String organized, String path, String oldPath) {
        Intent data = new Intent();
        data.setData(Uri.parse(organized));
        data.putExtra(ARG_ID,itemId);
        data.putExtra(ARG_MEDIA_POSITION,mediaPosition);
        if(tagUpdate!=null) {
            data.putExtra(ARG_MEDIA_TITLE, tagUpdate.getTitle());
            data.putExtra(ARG_MEDIA_ARTIST, tagUpdate.getArtist());
            data.putExtra(ARG_MEDIA_ALBUM, tagUpdate.getAlbum());
        }
        if(!StringUtils.isEmpty(oldPath)) {
            data.putExtra(ARG_MEDIA_OLD_PATH, oldPath);
        }
        data.putExtra(ARG_MEDIA_PATH,path);
        setResult(RESULT_OK,data);
    }

    private void hideFab(){
        ViewCompat.animate(fabSaveAction)
                .scaleX(0f).scaleY(0f)
                .alpha(0f).setDuration(100)
                .start();
       fabStatus = false;
    }

    private void showFab(){
        ViewCompat.animate(fabSaveAction)
                .scaleX(1f).scaleY(1f)
                .alpha(1f).setDuration(200)
                .setStartDelay(300L)
                .start();
        fabStatus = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        for(int i = 0; i < menu.size(); i++){
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    setColorFilter(item, R.color.menu_delete_background);
                    break;
                //case R.id.menu_organize:
                //    setColorFilter(item, R.color.menu_delete_background);
                //    break;
                //case R.id.menu_language:
                //    setColorFilter(item, R.color.menu_editor_background);
                //    break;
                //case R.id.menu_tag_from_filename:
                default:
                    setColorFilter(item, R.color.menu_editor_background);
                    break;
            }
        }
        return true;
    }

    private void setColorFilter(MenuItem item, int colorResId) {
        Drawable drawable = item.getIcon();
        if(drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(getColor(colorResId), PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_manage_file:
                showFilePopMenu();
                break;
            case R.id.menu_get_tag:
                showGetTagPopMenu();
                break;
            case R.id.menu_format_tag:
                showFormatTagPopMenu();
                break;
            /*
            case R.id.menu_delete:
                deleteMediaItem();
                break;
            case R.id.menu_organize:
                organizeMediaItem();
                break;
            case R.id.menu_edit_tag:
                showEditTagPopMenu();
                break;*/
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFilePopMenu() {
        View anchor = findViewById(R.id.menu_manage_file);

        final PopupMenu popupMenu = new PopupMenu(this,anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_delete:
                        deleteMediaItem();
                        break;
                    case R.id.menu_organize:
                        organizeMediaItem();
                        break;
                    default:
                        break;
                }

                showFab();
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_editor_file);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            setColorFilter(item, R.color.menu_editor_background);
        }
        popupMenu.show();
    }


    private void showGetTagPopMenu() {
        View anchor = findViewById(R.id.menu_get_tag);

        final PopupMenu popupMenu = new PopupMenu(this,anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // load from media file
                // tagUpdate = mediaStoreHelper.getMediaTag( mediaPath,mediaDuration);
                File file = new File(mediaPath);
                if(!file.exists()) return true;

                TagReader reader = new TagReader();
                TagReader.Tag tag = null;

                switch (item.getItemId()) {
                    case R.id.menu_label_simple:
                        //format artist/album/track title
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.SIMPLE);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_label_smart_reader:
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.SM1);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_label_smart_reader2:
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.SM2);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_label_hierarchy:
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.HIERARCHY);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_label_internet:
                        // search net
                        // popup result for selecting, albumart, and text (title, album, artist, etc)
                        break;
                    default:
                        break;
                }

                //load from media file
                tagUpdate = mediaStoreHelper.getMediaTag( mediaPath,mediaDuration);
                showFab();
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_editor_tag);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            setColorFilter(item, R.color.menu_editor_background);
        }
        popupMenu.show();
    }

    private void showFormatTagPopMenu() {
        View anchor = findViewById(R.id.menu_format_tag);

        final PopupMenu popupMenu = new PopupMenu(this,anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.menu_charset_default:
                        updateViewByCharset(null);
                        break;
                    case R.id.menu_charset_thai:
                        updateViewByCharset("TIS-620");
                        break;
                    case R.id.menu_charset_unicode:
                        updateViewByCharset("UTF-8");
                        break;
                    case R.id.menu_format_tag:
                        formatTag();
                        break;
                    default:
                        break;
                }

                showFab();
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_editor_format);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            setColorFilter(item, R.color.menu_editor_background);
        }
        popupMenu.show();
    }
/*
    private void showEditTagPopMenu() {
        View anchor = findViewById(R.id.menu_edit_tag);

        final PopupMenu popupMenu = new PopupMenu(this,anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // load from media file
               // tagUpdate = mediaStoreHelper.getMediaTag( mediaPath,mediaDuration);
                File file = new File(mediaPath);
                if(!file.exists()) return true;

                TagReader reader = new TagReader();
                TagReader.Tag tag = null;

              //  String title = AndroidFile.getNameWithoutExtension(file);
              //  String artist = "";
              //  String album = "";
              //  String albumArtist = "";
              //  String track = "";
              //  String comment = mediaPath;

                switch (item.getItemId()) {
                    case R.id.menu_label_simple:
                        //format artist/album/track title
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.SIMPLE);
                        //album = title;
                        //artist = title;
                        //updateTagByFilename(title, artist, album,albumArtist, track, comment);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_label_smart_reader:
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.SM1);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_label_smart_reader2:
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.SM2);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_label_hierarchy:
                        //replace title/album by filename, artist by folder
                        tag = reader.parser(mediaPath, TagReader.READ_MODE.HIERARCHY);
                        updateTagByFilename(tag);
                        break;
                    case R.id.menu_charset_default:
                        updateViewByCharset(null);
                        break;
                    case R.id.menu_charset_thai:
                        updateViewByCharset("TIS-620");
                        break;
                    case R.id.menu_charset_unicode:
                        updateViewByCharset("UTF-8");
                        break;
                    case R.id.menu_format_tag:
                        formatTag();
                        break;
                    default:
                        break;
                }

                //load from media file
                tagUpdate = mediaStoreHelper.getMediaTag( mediaPath,mediaDuration);
                showFab();
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_editor_format);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            setColorFilter(item, R.color.menu_editor_background);
        }
        popupMenu.show();
    }
*/
    private void updateTagByFilename(TagReader.Tag tag) {
        // title
        //tagUpdate.setTitle(trimText(tag.getTitle()));
        mTitleView.setText(trimText(tag.getTitle()));
        // artist
        //tagUpdate.setArtist(trimText(tag.getArtist()));
        mArtistView.setText(trimText(tag.getArtist()));
        // album
        //tagUpdate.setAlbum(trimText(tag.getAlbum()));
        mAlbumView.setText(trimText(tag.getAlbum()));
        // album artist
        //tagUpdate.setAlbumArtist("");
        mAlbumArtistView.setText("");

        // album artist
        //mCommentView.setText(formatText(comment));
        // album artist
        //tagUpdate.setTrack(trimText(tag.getTrack()));
        mTrackView.setText(trimText(tag.getTrack()));

    }

    private String trimText(String text) {
        // trim space
        // format as word, first letter of word is capital
        if(text==null) {
            return "";
        }
        return text.trim();
    }

    private void formatTag() {
        // title
        mTitleView.setText(formatText(mTitleView.getText()));
        // artist
        mArtistView.setText(formatText(mArtistView.getText()));
        // album
        mAlbumView.setText(formatText(mAlbumView.getText()));
        // album artist
        mAlbumArtistView.setText(formatText(mAlbumArtistView.getText()));
        // genre
        mGenreView.setText(formatText(mGenreView.getText()));

        // clean albumArtist if same value as artist
        if(mArtistView.getText().equals(mAlbumArtistView.getText())) {
            mAlbumArtistView.setText("");
        }

        showFab();
    }

    private String formatText(CharSequence text) {
        // trim space
        // format as word, first letter of word is capital
        if(text==null) {
            return "";
        }
        String str = text.toString().trim();
        char [] delimiters = {' ','.','(','['};
        return StringUtils.capitalize(str, delimiters);
    }

    @Override
     public void onBackPressed() {
        if (fabStatus) {
            hideFab();
            return;
        }

        super.onBackPressed();
    }

    private void updateViewByCharset(String charset) {
        if(StringUtils.isEmpty(charset)) {
            // re-load from media file
            tagUpdate = mediaStoreHelper.getMediaTag(mediaPath, mediaDuration);
        }
        // title
        mTitleView.setText(encodeText(tagUpdate.getTitle(), charset));
        // artist
        mArtistView.setText(encodeText(tagUpdate.getArtist(), charset));
        // album
        mAlbumView.setText(encodeText(tagUpdate.getAlbum(), charset));
        // album artist
        mAlbumArtistView.setText(encodeText(tagUpdate.getAlbumArtist(), charset));
        // genre
        mGenreView.setText(encodeText(tagUpdate.getGenre(), charset));
        // country
        mCountryView.setText(encodeText(tagUpdate.getCountry(), charset));
        // comment
        mCommentView.setText(encodeText(tagUpdate.getComment(), charset));
        // lyrics
        mLyricsView.setText(encodeText(tagUpdate.getLyrics(), charset));
    }

    private String encodeText(String text, String charset) {
        if(StringUtils.isEmpty(charset)) {
            return text;
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
                tagUpdate.setChanged(true);
                showFab();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
    }
