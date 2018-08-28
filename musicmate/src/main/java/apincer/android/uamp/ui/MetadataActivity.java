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
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.zaaach.toprightmenu.TopRightMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import apincer.android.uamp.Constants;
import apincer.android.uamp.R;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.service.MediaItemIntentService;
import apincer.android.uamp.service.MusicListeningService;
import apincer.android.uamp.utils.UIUtils;
import de.mateware.snacky.Snacky;
import stream.customalert.CustomAlertDialogue;

import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_GOOD;
import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_HIGH;
import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_HIRES;
import static apincer.android.uamp.model.MediaItem.MEDIA_QUALITY_LOW;

/**
 * A full screen editor that shows the current  music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class MetadataActivity extends AppCompatActivity {

    private static List<MediaItem> editItems = new ArrayList<>();

    private ImageView mImageView;
    private ImageView mAvatarView;
    private TextView mFormatView;
    private TextView mSamplerateView;
    private TextView mBitrateView;
    private TextView mDurationView;
    private TextView mFileSizeView;
    private TextView mMediaPathView;
    private TextView mTitleView;

    private ViewPager viewPager;
    private View panelTabs;
    private View panelTabTag;
    private View panelTabMusicBraninz;
    private View tabTag;
    private View tabMusicBraninz;
    private Toolbar toolbar;
    private TopRightMenu mTopRightMenu;
    private boolean itemReloaded;

    public static boolean startActivity(AppCompatActivity  context, List<MediaItem> mediaItems) {
        if(mediaItems==null || mediaItems.isEmpty()) {
            return false;
        }
        if(editItems==null) {
            editItems = new ArrayList<>();
        }
        editItems.clear();
        for(MediaItem item: mediaItems) {
            item.resetPendingMetadata(false);
            editItems.add(item);
        }
        Intent intent = new Intent(context, MetadataActivity.class);
        ActivityCompat.startActivityForResult(context, intent, MusicListeningService.REQUEST_CODE_EDIT_MEDIA_TAG, null);
        return true;
    }

    public static List<MediaItem> getEditItems() {
        return editItems;
    }

    @Override
    public void onBackPressed() {
        int currentFragment = viewPager.getCurrentItem();

        if(currentFragment==0) {
            editItems.clear();
            super.onBackPressed();
        }else {
            viewPager.setCurrentItem(currentFragment-1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransitions();
        setContentView(R.layout.activity_metadata);
        //StatusBarUtil.setColor(this, getColor(R.color.colorPrimaryDark_light),120);
       // StatusBarUtil.setTranslucentForCoordinatorLayout(this, 120);

        setTitle("");

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        setupToolbar();

        setUpHeader();
        showHeader();
        setUpPages();
    }

    private void setUpPages() {
        viewPager = findViewById(R.id.viewpager);
        panelTabs = findViewById(R.id.tab_panel);
        panelTabTag = findViewById(R.id.tab_tag_panel);
        panelTabMusicBraninz = findViewById(R.id.tab_music_brainz_panel);
        tabTag = findViewById(R.id.tab_tag);
        tabMusicBraninz = findViewById(R.id.tab_music_brainz);
        //arcMenu = findViewById(R.id.arcMenu);
        tabTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabActivated(0, true);
            }
        });
        tabMusicBraninz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabActivated(1,true);
            }
        });
        tabActivated(0,false);

        viewPager.setAdapter(new TabsAdapter(getSupportFragmentManager(), 2));
        viewPager.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        viewPager.getBackground().setAlpha(128);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tabActivated(position,false);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    private void tabActivated(int tab, boolean changeTab) {
        if(tab == 0) {
            if(changeTab) {
                viewPager.setCurrentItem(tab);
            }
            tabMusicBraninz.setBackgroundColor(getColor(R.color.grey600_transparent));
            tabTag.setBackgroundColor(getColor(R.color.colorPrimary));
            panelTabTag.bringToFront();
            panelTabs.invalidate();
        }else if(tab == 1) {
            if(changeTab) {
                viewPager.setCurrentItem(tab);
            }
            tabMusicBraninz.setBackgroundColor(getColor(R.color.colorPrimary));
            tabTag.setBackgroundColor(getColor(R.color.grey600_transparent));
            panelTabMusicBraninz.bringToFront();
            panelTabs.invalidate();
        }
    }

    private void setupToolbar() {
        mTopRightMenu = new TopRightMenu(this);
        mTopRightMenu
                .setHeight(RecyclerView.LayoutParams.WRAP_CONTENT)
                //.setWidth(320)
                .showIcon(true)
                .dimBackground(true)
                .needAnimationStyle(true)
                .setAnimationStyle(R.style.TRM_ANIM_STYLE)
                .addMenuItem(new com.zaaach.toprightmenu.MenuItem("move", R.drawable.ic_move_to_inbox_black_24dp, "Manage Songs"))
               // .addMenuItem(new com.zaaach.toprightmenu.MenuItem("transfer", R.drawable.ic_content_copy_black_24dp, "Transfer Songs"))
                .addMenuItem(new com.zaaach.toprightmenu.MenuItem("delete", R.drawable.ic_delete_black_24dp, "Delete Songs"))
                .addMenuItem(new com.zaaach.toprightmenu.MenuItem("open", R.drawable.ic_folder_open_black_24dp, "Open Directory"))
                .setOnMenuItemClickListener(new TopRightMenu.OnMenuItemClickListener() {
                    @Override
                    public void onMenuItemClick(int position) {
                        switch (position) {
                            case 0:
                                doMoveMediaItem();
                                break;
                           /* case 1:
                                doMoveMediaItem();
                                break; */
                            case 1:
                                doDeleteMediaItem();
                                break;
                            case 2:
                                doOpenFileManager();
                                break;
                        }
                    }
                });
    }

    private void setUpHeader() {
        mImageView = findViewById(R.id.main_imageview_placeholder); //findViewById(R.id.image);
       /* mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show metadata details or about list details.

            }
        }); */
        mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doSaveCoverart();
                return false;
            }
        });

        mAvatarView = findViewById(R.id.avatar_imageview_placeholder);
        mTitleView = findViewById(R.id.main_textview_title);
        mMediaPathView = findViewById(R.id.media_path);
        mFormatView = findViewById(R.id.media_format);
        mSamplerateView = findViewById(R.id.media_samplerate);
        mBitrateView = findViewById(R.id.media_bitrate);
        mDurationView = findViewById(R.id.media_duration);
        mFileSizeView = findViewById(R.id.media_filesize);
    }

    public void onMediaItemsReloaded() {
        itemReloaded = true;
        showHeader();
    }

    private void showHeader() {
        if(getEditItems() == null || getEditItems().size()==0) {
            return;
        }
        // path
        MediaMetadata displayTag = getEditItems().get(0).getMetadata();
        if(getEditItems().size() ==1) {
            mTitleView.setText(displayTag.getTitle());
        }else {
            mTitleView.setText(displayTag.getAlbum());
        }
        mMediaPathView.setText(MediaItemProvider.getInstance().buildDisplayName(displayTag.getMediaPath()));
        mFormatView.setText(displayTag.getAudioFormat());
        mSamplerateView.setText(displayTag.getAudioBitCountAndSamplingrate());
        mBitrateView.setText(displayTag.getAudioBitRate());
        mDurationView.setText(displayTag.getAudioDurationAsString());
        mFileSizeView.setText(displayTag.getMediaSize());
        updateAudioFormatQualityView(displayTag);

        Bitmap art = MediaItemProvider.getInstance().getArtwork(editItems.get(0).getPath());
        if(art !=null) {
           // int height = art.getHeight();
           // int width = art.getWidth();
            mImageView.setImageBitmap(art);
            mAvatarView.setImageBitmap(art);
           // Palette palette = Palette.from(art).generate();
          //  int backgroundColor = palette.getDominantColor(getApplicationContext().getColor(R.color.grey600));

          //  mainView.setBackgroundColor(backgroundColor);
        }else {
            mImageView.setImageDrawable(getApplicationContext().getDrawable(R.drawable.ic_music));
        }
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
         mBitrateView.setBackground(getApplicationContext().getDrawable(drawableId));
         //mDurationView.setBackground(getApplicationContext().getDrawable(drawableId));
         //mFileSizeView.setBackground(getApplicationContext().getDrawable(drawableId));
    }

    private void initActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_metadata, menu);
        for(int i=0; i<menu.size();i++) {
            MenuItem item = menu.getItem(i);
            //if(item.getItemId() == R.id.menu_metadata_file_main) {
            //    item.setIcon();
            //}
            UIUtils.setColorFilter(item, getColor(R.color.colorAccent));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_delete_file:
                doDeleteMediaItem();
                break;
            case R.id.menu_main_manage_file:
                doMoveMediaItem();
                break;
            case R.id.menu_main_transfer_file:
                doMoveMediaItem();
                break;
            case R.id.menu_main_file_manager:
                doOpenFileManager();
                break;
            case R.id.menu_metadata_file_main:
                mTopRightMenu.showAsDropDown(findViewById(R.id.menu_metadata_file_main), -100, -10);
                break;
            default:
                break;
        }
        return true;
    }

    public void updateCoverArt(File pendingCoverartFile) {
         GlideApp.with(getApplicationContext())
                        .load(pendingCoverartFile)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .signature(new ObjectKey(String.valueOf(pendingCoverartFile.lastModified())))
                        .into(mImageView);
    }

    private class TabsAdapter extends FragmentStatePagerAdapter {
        private int amountTabs;

        public TabsAdapter(FragmentManager supportFragmentManager, int amountTabs) {
            super(supportFragmentManager);
            this.amountTabs = amountTabs;
        }

        @Override
        public Fragment getItem(int position) {
            if(position==0) {
                return new MetadataEditorFragment();
            }else if(position==1){
                return new MetadataMusicBrainzFragment();
               // return new MetadataSearchFragment();
            }else {
                return new MetadataGoogleSearchFragment();
            }
        }

        @Override
        public int getCount() {
            return amountTabs;
        }
    }


    private void doSaveCoverart() {
        File theFilePath = MediaItemProvider.getDownloadPath(editItems.get(0).getTitle()+".png");
        MediaItemProvider.getInstance().saveArtworkToFile(editItems.get(0), theFilePath.getAbsolutePath());
        Snacky.builder().setActivity(this)
                .setText("Save Artwork to "+theFilePath.getName())
                .setDuration(Snacky.LENGTH_LONG)
                .setMaxLines(1)
                .success()
                .show();
    }

    private void doDeleteMediaItem() {
        String text = "Delete ";
        if(editItems.size()>1) {
            text = text + editItems.size() + " songs?";
        }else {
            text = text + "'"+editItems.get(0).getTitle()+"' song?";
        }
        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(this)
                .setStyle(CustomAlertDialogue.Style.DIALOGUE)
                .setCancelable(false)
                .setTitle("Delete Songs")
                .setMessage(text)
                .setPositiveText("Delete")
                .setPositiveColor(R.color.negative)
                .setPositiveTypeface(Typeface.DEFAULT_BOLD)
                .setOnPositiveClicked(new CustomAlertDialogue.OnPositiveClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        MediaItemIntentService.startService(getApplicationContext(), Constants.COMMAND_DELETE,editItems);
                        //MediaItemProvider.getInstance().deleteMediaItems(editItems);
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
                .setDecorView(getWindow().getDecorView())
                .build();
        alert.show();
    }

    private void doMoveMediaItem() {
        String text = "Move ";
        if(editItems.size()>1) {
            text = text + editItems.size() + " songs to Music Directory?";
        }else {
            text = text + "'"+editItems.get(0).getTitle()+"' song to Music Directory?";
        }
        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(this)
                .setStyle(CustomAlertDialogue.Style.DIALOGUE)
                .setCancelable(false)
                .setTitle("Manage Songs")
                .setMessage(text)
                .setPositiveText("Move")
                .setPositiveColor(R.color.negative)
                .setPositiveTypeface(Typeface.DEFAULT_BOLD)
                .setOnPositiveClicked(new CustomAlertDialogue.OnPositiveClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        MediaItemIntentService.startService(getApplicationContext(),Constants.COMMAND_MOVE,editItems);
                        //MediaItemProvider.getInstance().manageMediaItems(editItems);
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
                .setDecorView(getWindow().getDecorView())
                .build();
        alert.show();
    }


    private void doOpenFileManager() {
        if(editItems.size()>1) {
            return;
        }
        File mediaFile = new File(editItems.get(0).getPath());
        Uri uri = Uri.parse(mediaFile.getParent());
        Intent intent = getPackageManager().getLaunchIntentForPackage("pl.solidexplorer2");
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
}
