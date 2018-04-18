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
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;

import com.jaeger.library.StatusBarUtil;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItem;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems;

import java.util.List;

import apincer.android.uamp.FileManagerService;
import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.model.MediaItem;

/**
 * A full screen editor that shows the current  music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class MetadataActivity extends AppCompatActivity {
    private ViewPager viewPager;

    public static boolean startActivity(AppCompatActivity  context, List<MediaItem> mediaItems) {
        if(mediaItems==null || mediaItems.isEmpty()) {
            return false;
        }
        FileManagerService.addToEdit(mediaItems);
        Intent intent = new Intent(context, MetadataActivity.class);
        ActivityCompat.startActivityForResult(context, intent, MusicService.REQUEST_CODE_EDIT_MEDIA_TAG, null);
        return true;
    }

    @Override
    public void onBackPressed() {
        int currentFragment = viewPager.getCurrentItem();

        if(currentFragment==0) {
            super.onBackPressed();
        }else {
            viewPager.setCurrentItem(currentFragment-1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransitions();
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_metadata);
        StatusBarUtil.setColor(this, getColor(R.color.colorPrimaryDark_light));
        setTitle("");

        viewPager = findViewById(R.id.viewpager);
        SmartTabLayout viewPagerTab = findViewById(R.id.viewpagertab);

        FragmentPagerItems pages = new FragmentPagerItems(this);
        pages.add(FragmentPagerItem.of("Edit Metadata", MetadataEditorFragment.class));
        if(FileManagerService.getEditItems().size()>1) {
            pages.add(FragmentPagerItem.of("Selected Songs", MetadataDetailsFragment.class));
        }
        pages.add(FragmentPagerItem.of("Search", MetadataSearchFragment.class));

        FragmentPagerItemAdapter adapter = new FragmentPagerItemAdapter(
                getSupportFragmentManager(), pages);

        viewPager.setAdapter(adapter);
        viewPagerTab.setViewPager(viewPager);
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
}
