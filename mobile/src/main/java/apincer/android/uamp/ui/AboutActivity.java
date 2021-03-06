package apincer.android.uamp.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import apincer.android.uamp.BuildConfig;
import apincer.android.uamp.R;
import husaynhakeem.com.aboutpage.AboutPage;
import husaynhakeem.com.aboutpage.Item;

/**
 * Created by e1022387 on 2/13/2018.
 */

public class AboutActivity extends AppCompatActivity {
    String versionName = BuildConfig.VERSION_NAME;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new AboutPage(this)
                .setBackground(android.R.color.white)
                .setDescription("Music Mate\nManaging music collections on Android\n")
                .setImage(R.drawable.ic_launcher)
                .addItem(new Item("Version "+versionName,null,null))
                //.addItem(new Item("Version 1.1.1",null,null))
                .addEmail("thaweemail@gmail.com")
                .addGithub("thawee")
                .addWebsite("https://github.com/thawee/musixmate")
                //.addPlayStore("apincer.android")
                .addItem(new Item("Thanks you for downloading", null, null))
                .create());
    }
}