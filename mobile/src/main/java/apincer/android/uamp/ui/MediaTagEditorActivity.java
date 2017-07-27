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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import apincer.android.uamp.R;
import apincer.android.uamp.file.AndroidFile;
import apincer.android.uamp.item.MediaItem;
import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.provider.MediaTag;
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
    private TextView mArtistView;
    private TextView mAlbumView;
    private TextView mAlbumArtistView;
    private TextView mGenreView;
    private TextView mCountryView;
    private TextView mCommentView;
    private TextView mLyricsView;
    private TextView mTrackView;

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
            mMediaInfoView = (TextView) findViewById(R.id.media_info);
            mMediaInfoView.setText(tagUpdate.getMediaDuration()+ StringUtils.MUSIC_SEP+tagUpdate.getMediaSampleRate()+StringUtils.MUSIC_SEP+tagUpdate.getMediaBitrate()+StringUtils.MUSIC_SEP+tagUpdate.getMediaSize());

            // title
            mTitleView = (TextView) findViewById(R.id.title);
            mTitleView.setText(tagUpdate.getTitle());
            setTitle("");
            mTitleView.addTextChangedListener(new ViewTextWatcher(mTitleView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetTitle(String.valueOf(view.getText()));
                }
            });

            // artist
            mArtistView = (TextView) findViewById(R.id.artist);
            mArtistView.setText(tagUpdate.getArtist());
            mArtistView.addTextChangedListener(new ViewTextWatcher(mArtistView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetArtist(String.valueOf(view.getText()));
                }
            });

            // album
            mAlbumView = (TextView) findViewById(R.id.album);
            mAlbumView.setText(tagUpdate.getAlbum());
            mAlbumView.addTextChangedListener(new ViewTextWatcher(mAlbumView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetAlbum(String.valueOf(view.getText()));
                }
            });

            // album artist
            mAlbumArtistView = (TextView) findViewById(R.id.album_arist);
            mAlbumArtistView.setText(tagUpdate.getAlbumArtist());
            mAlbumArtistView.addTextChangedListener(new ViewTextWatcher(mAlbumArtistView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetAlbumArtist(String.valueOf(view.getText()));
                }
            });

            // year
            TextView year = (TextView) findViewById(R.id.year);
            year.setText(tagUpdate.getYear());
            year.addTextChangedListener(new ViewTextWatcher(year) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetYear(String.valueOf(view.getText()));
                }
            });

            // disc no
            TextView discno = (TextView) findViewById(R.id.diskno);
            discno.setText(tagUpdate.getDisc());
            discno.addTextChangedListener(new ViewTextWatcher(discno) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetDisc(String.valueOf(view.getText()));
                }
            });

            // track
            mTrackView = (TextView) findViewById(R.id.track);
        mTrackView.setText(tagUpdate.getTrack());
        mTrackView.addTextChangedListener(new ViewTextWatcher(mTrackView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetTrack(String.valueOf(view.getText()));
                }
            });

            // genre
            mGenreView = (TextView) findViewById(R.id.genre);
            mGenreView.setText(tagUpdate.getGenre());
            mGenreView.addTextChangedListener(new ViewTextWatcher(mGenreView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetGenre(String.valueOf(view.getText()));
                }
            });

            // country
            mCountryView = (TextView) findViewById(R.id.country);
            mCountryView.setText(tagUpdate.getCountry());
            mCountryView.addTextChangedListener(new ViewTextWatcher(mCountryView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetCountry(String.valueOf(view.getText()));
                }
            });

            // comment
            mCommentView = (TextView) findViewById(R.id.comment);
            mCommentView.setText(tagUpdate.getComment());
            mCommentView.addTextChangedListener(new ViewTextWatcher(mCommentView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetComment(String.valueOf(view.getText()));
                }
            });

            // lyrics
            mLyricsView = (TextView) findViewById(R.id.lyrics);
            mLyricsView.setText(tagUpdate.getLyrics());
            mLyricsView.addTextChangedListener(new ViewTextWatcher(mLyricsView) {
                @Override
                public void tagUpdate(Editable editable) {
                    tagUpdate.softSetLyrics(String.valueOf(view.getText()));
                }
            });

            //default album art
            final ImageView image = (ImageView) findViewById(R.id.image);
            if(tagUpdate.getArtwork()!=null) {
                image.setImageBitmap(tagUpdate.getArtwork());
            }
    }

/*
    private void showTagFromFilenamePopMenu() {
        View anchor = findViewById(R.id.menu_tag_from_filename);

        final PopupMenu popupMenu = new PopupMenu(this,anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // load from media file
                tagUpdate = mediaStoreHelper.getMediaTag( mediaPath,mediaDuration);
                File file = new File(mediaPath);
                if(!file.exists()) return true;

                String title = AndroidFile.getNameWithoutExtension(file);
                String artist = "";
                String album = "";
                String albumArtist = "";
                String track = "";
                switch (item.getItemId()) {
                    case R.id.menu_label_default:
                        //format artist/album/track title
                        file = file.getParentFile();
                        if(file!=null) {
                            album = file.getName();
                        }
                        file = file.getParentFile();
                        if(file!=null) {
                            artist = file.getName();
                        }
                        break;
                    case R.id.menu_label_default_albumartist:
                        //format albumartist/album/track arist-title
                        file = file.getParentFile();
                        if(file!=null) {
                            album = file.getName();
                        }
                        file = file.getParentFile();
                        if(file!=null) {
                            albumArtist = file.getName();
                        }
                        break;
                    case R.id.menu_label_folder_artist:
                        //replace title/album by filename, artist by folder
                        file = file.getParentFile();
                        if(file!=null) {
                            album = file.getName();
                            artist = album;
                            albumArtist = album;
                        }
                        break;
                    default:
                        break;
                }
                updateTagByFilename(title, artist, album,albumArtist, track);
                showFab();
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_tag_from_file);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            setColorFilter(item, R.color.menu_editor_background);
        }
        popupMenu.show();
    }*/

    /*
    private void showLanguagePopMenu() {
         View anchor = findViewById(R.id.menu_language);

        final PopupMenu popupMenu = new PopupMenu(this,anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String charset=null;
                switch (item.getItemId()) {
                    case R.id.menu_charset_default:
                        charset = null;
                        break;
                    case R.id.menu_charset_thai:
                        charset = "TIS-620";
                        break;
                    case R.id.menu_charset_unicode:
                        charset = "UTF-8";
                        break;
                    default:
                        break;
                }
                updateViewByCharset(charset);
                showFab();
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_charset);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            setColorFilter(item, R.color.menu_editor_background);
        }
        popupMenu.show();
    } */

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
        if(mediaStoreHelper.saveMediaFile(mediaPath, tagUpdate,findViewById(R.id.main_view))) {
            hideFab();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            saveResult("SAVED", tagUpdate.getMediaPath(),null);
            ///onBackPressed();
        }
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
            case R.id.menu_delete:
                deleteMediaItem();
                break;
            case R.id.menu_organize:
                organizeMediaItem();
                break;
            case R.id.menu_edit_tag:
                showEditTagPopMenu();
                break;
            /*
            case R.id.menu_tag_from_filename:
                showTagFromFilenamePopMenu();
                break;
            case R.id.menu_language:
                showLanguagePopMenu();
                break;
            case R.id.menu_format_tag:
                formatTag();
                break;
                */
        }

        return super.onOptionsItemSelected(item);
    }

    private void showEditTagPopMenu() {
        View anchor = findViewById(R.id.menu_edit_tag);

        final PopupMenu popupMenu = new PopupMenu(this,anchor);
        makePopForceShowIcon(popupMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // load from media file
                tagUpdate = mediaStoreHelper.getMediaTag( mediaPath,mediaDuration);
                File file = new File(mediaPath);
                if(!file.exists()) return true;

                String title = AndroidFile.getNameWithoutExtension(file);
                String artist = "";
                String album = "";
                String albumArtist = "";
                String track = "";
                String comment = mediaPath;
                switch (item.getItemId()) {
                    case R.id.menu_label_default:
                        //format artist/album/track title
                        album = title;
                        artist = title;
                        updateTagByFilename(title, artist, album,albumArtist, track, comment);
                        break;
                    case R.id.menu_label_smart_reader:
                    case R.id.menu_label_smart_reader2:
                        //<tract>. <arist> (<featering>) - <tltle>
                        String featuring = "";
                        int trackIndx = title.indexOf(".");
                        int titleIndx = title.indexOf("- ");
                        if(trackIndx>0) {
                            track = title.substring(0, trackIndx);
                        }
                        if(trackIndx>0 && titleIndx>0) {
                            artist = title.substring(trackIndx+1,titleIndx);
                            if(artist.indexOf("(") >0 && artist.indexOf(")") >0) {
                                featuring = artist.substring(artist.indexOf("(")+1, artist.indexOf(")"));
                                artist = artist.substring(0, artist.indexOf("("));
                            }
                        }

                        if(titleIndx>0) {
                            title = title.substring(titleIndx+1, title.length()) + " "+featuring;
                        }
                        title.trim();
                        track.trim();
                        artist.trim();
                        if(item.getItemId() == R.id.menu_label_smart_reader) {
                            updateTagByFilename(title, artist, album, albumArtist, track, comment);
                        }else {
                            updateTagByFilename(artist, title, album, albumArtist, track, comment);
                        }
                        break;
                    case R.id.menu_label_folder_artist:
                        //replace title/album by filename, artist by folder
                        file = file.getParentFile();
                        if(file!=null) {
                            album = file.getName();
                            artist = album;
                            albumArtist = album;
                        }
                        updateTagByFilename(title, artist, album,albumArtist, track, comment);
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
                showFab();
                return true;
            }
        });
        popupMenu.inflate(R.menu.menu_editor_extras);
        for(int cn=0; cn< popupMenu.getMenu().size();cn++) {
            MenuItem item = popupMenu.getMenu().getItem(cn);
            setColorFilter(item, R.color.menu_editor_background);
        }
        popupMenu.show();
    }

    private void updateTagByFilename(String title, String artist, String album,String albumArtist, String track, String comment) {
        // title
        mTitleView.setText(formatText(title));
        // artist
        mArtistView.setText(formatText(artist));
        // album
        mAlbumView.setText(formatText(album));
        // album artist
        if(artist.equals(albumArtist)) {
            mAlbumArtistView.setText("");
        }else {
            mAlbumArtistView.setText(formatText(albumArtist));
        }
        // album artist
        //mCommentView.setText(formatText(comment));
        // album artist
        mTrackView.setText(formatText(track));
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

        showFab();
    }

    private String formatText(CharSequence text) {
        // trim space
        // format as word, first letter of word is capital
        if(text==null) {
            return "";
        }
        String str = text.toString().trim();
        return StringUtils.capitalize(str, ' ');
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

        public abstract class ViewTextWatcher implements TextWatcher {
            TextView view;
            public ViewTextWatcher(TextView view) {
                this.view = view;
            }
            public void afterTextChanged(Editable editable) {
                tagUpdate(editable);
                showFab();
            }

            protected abstract void tagUpdate(Editable editable);

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
    }
