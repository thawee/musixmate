package apincer.android.uamp.ui;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.gigamole.navigationtabstrip.NavigationTabStrip;
import com.intentfilter.androidpermissions.PermissionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.item.MediaItem;
import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.provider.MediaTag;
import apincer.android.uamp.ui.listener.OnFragmentInteractionListener;
import apincer.android.uamp.ui.views.HeaderView;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.ResourceHelper;
import apincer.android.uamp.utils.StringUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.Payload;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.helpers.ActionModeHelper;

/**
 * Created by e1022387 on 6/4/2017.
 */

public class BrowserViewPagerActivity extends AppCompatActivity implements ActionMode.Callback, SearchView.OnQueryTextListener, OnFragmentInteractionListener //{
      , FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, ViewPager.OnPageChangeListener {
        private static final String TAG = LogHelper.makeLogTag(BrowserViewPagerActivity.class);
    // android 5 SD card permissions
    private static final int REQUEST_CODE_PERMISSION_All = 111;
    public static final int REQUEST_EDIT_MEDIA_TAG = 222;

    public static String[] PERMISSIONS_ALL = {Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * Bundle key representing the Active Fragment
     */
    private static final String STATE_ACTIVE_FRAGMENT = "active_fragment";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    protected Toast exitToast;
    //protected MediaItem mListeningItem;

    protected MediaProvider tagHelper;

    /**
     * RecyclerView and related objects
     */
    private RecyclerView mRecyclerView;
    private BrowserViewPagerFragment.BrowserFlexibleAdapter mAdapter;
    private ActionModeHelper mActionModeHelper;
    private Toolbar mToolbar;
    private HeaderView mHeaderView;
    private BrowserViewPagerFragment mFragment;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SearchView mSearchView;

    private int changedPosition =-1;

    @TargetApi(Build.VERSION_CODES.M)
    private void triggerStorageAccessFramework() {
        if( this.getContentResolver().getPersistedUriPermissions().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, REQUEST_CODE_PERMISSION_All);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);

        try {
            PermissionManager permissionManager = PermissionManager.getInstance(this);
            List<String> permissions = Arrays.asList(PERMISSIONS_ALL);
            permissionManager.checkPermissions(permissions, new PermissionManager.PermissionRequestListener() {
                @Override
                public void onPermissionGranted() {
                    //Toast.makeText(BrowserViewPagerActivity.this, "Permissions Granted", Toast.LENGTH_SHORT).show();
                    triggerStorageAccessFramework();
                }

                @Override
                public void onPermissionDenied() {
                    Toast.makeText(BrowserViewPagerActivity.this, "Permissions Denied", Toast.LENGTH_LONG).show();
                }
            });
        }catch (Exception ex) {
            LogHelper.e(TAG, ex);
        }

        // start service
        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);

        tagHelper = new MediaProvider(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mHeaderView = (HeaderView) findViewById(R.id.toolbar_header_view);
        mHeaderView.bindTo(getString(R.string.app_name), ""); //getString(R.string.viewpager));

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        NavigationTabStrip tabStrip = (NavigationTabStrip) findViewById(R.id.nts_tabs);
        tabStrip.setTitles(mSectionsPagerAdapter.getTitles());
        tabStrip.setOnPageChangeListener(this);

        tabStrip.setViewPager(mViewPager);

            // Since our app icon has the same color as colorPrimary, our entry in the Recent Apps
            // list gets weird. We need to change either the icon or the color
            // of the TaskDescription.
            ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(
                    getTitle().toString(),
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_white),
                    ResourceHelper.getThemeColor(this, R.attr.colorPrimary,
                            android.R.color.darker_gray));
            setTaskDescription(taskDesc);

        //Coordinatorlayout Status Bar Padding Disappears From Viewpager 2nd-page
        //http://stackoverflow.com/questions/31368781/coordinatorlayout-status-bar-padding-disappears-from-viewpager-2nd-page
        ViewCompat.setOnApplyWindowInsetsListener(mViewPager, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v,
                                                          WindowInsetsCompat insets) {
                insets = ViewCompat.onApplyWindowInsets(v, insets);
                if (insets.isConsumed()) {
                    return insets;
                }

                boolean consumed = false;
                for (int i = 0, count = mViewPager.getChildCount(); i < count; i++) {
                    ViewCompat.dispatchApplyWindowInsets(mViewPager.getChildAt(i), insets);
                    if (insets.isConsumed()) {
                        consumed = true;
                    }
                }
                return consumed ? insets.consumeSystemWindowInsets() : insets;
            }
        });

        exitToast = Toast.makeText(getApplicationContext(), getString(R.string.alert_back_to_exit), Toast.LENGTH_SHORT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(changedPosition>=0 && mAdapter!=null) {
            mAdapter.notifyItemChanged(changedPosition);
            mHeaderView = (HeaderView) findViewById(R.id.toolbar_header_view);
            if(mHeaderView!=null) {
                mHeaderView.bindTo(getString(R.string.app_name), mAdapter.getItemCount() + " songs"); //getString(R.string.viewpager));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        LogHelper.v(TAG, "onSaveInstanceState!");
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState(outState);
            getSupportFragmentManager().putFragment(outState, STATE_ACTIVE_FRAGMENT, mFragment);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore previous state
        if (savedInstanceState != null && mAdapter != null) {
            // Selection
            mAdapter.onRestoreInstanceState(savedInstanceState);
            mActionModeHelper.restoreSelection(this);
        }
    }

    @Override
    //public void onFragmentChange(Fragment fragment, SwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView, int mode) {
    public void onFragmentChange(Fragment fragment, SwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView, int mode) {
        if(fragment.equals(mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem()))) {
            mSwipeRefreshLayout = swipeRefreshLayout;
            mRecyclerView = recyclerView;
            mFragment = (BrowserViewPagerFragment) fragment;
            mAdapter = (BrowserViewPagerFragment.BrowserFlexibleAdapter) recyclerView.getAdapter();
            initializeActionModeHelper(mode);

            if(getIntent()!=null) {
                try {
                    Bundle extras = getIntent().getExtras();
                    String listenTitle = extras.getString("title", "");
                    String listenArtist = extras.getString("artist", "");
                    String listenAlbum = extras.getString("album", "");
                    if (!listenTitle.isEmpty()) {
                        mAdapter.setListeningTitle(listenTitle, listenArtist, listenAlbum);
                    }
                }catch(Exception ignore) {}
            }
        }
    }

	/* ======================
	 * INITIALIZATION METHODS
	 * ====================== */
    private void initializeActionModeHelper(int mode) {
        mAdapter.setMode(mode);
        mActionModeHelper = new ActionModeHelper(mAdapter, mFragment.getContextMenuResId(), this) {
            @Override
            public void updateContextTitle(int count) {
                if (mActionMode != null) {//You can use the internal ActionMode instance
                    mActionMode.setTitle(count == 1 ?
                            getString(R.string.action_selected_one, Integer.toString(count)) :
                            getString(R.string.action_selected_many, Integer.toString(count)));
                }
            }
        }.withDefaultMode(mode);
        mFragment.setActionModeHelper(mActionModeHelper);
    }

    /* ====================================
 * OPTION MENU PREPARATION & MANAGEMENT
 * ==================================== */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_pager, menu);

        initSearchView(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        LogHelper.v(TAG, "onPrepareOptionsMenu called!");

        if (mSearchView != null && mAdapter != null) {
            //Has searchText?
            if (!mAdapter.hasSearchText()) {
                LogHelper.d(TAG, "onPrepareOptionsMenu Clearing SearchView!");
                mSearchView.setIconified(true);// This also clears the text in SearchView widget
            } else {
                //Necessary after the restoreInstanceState
                menu.findItem(R.id.action_search).expandActionView();//must be called first
                //This restores the text, must be after the expandActionView()
                mSearchView.setQuery(mAdapter.getSearchText(), false);//submit = false!!!
                mSearchView.clearFocus();//Optionally the keyboard can be closed
                //mSearchView.setIconified(false);//This is not necessary
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
     /*   int id = item.getItemId();

        if (id == R.id.action_settings) {
            onBackPressed();
            return true;
        }

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);*/
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent myIntent = new Intent(this, SettingsActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                startActivity(myIntent);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (exitToast.getView().isShown()) {
            exitToast.cancel();
            finish();
        } else {
            exitToast.show();
        }
    }


    /* ====================================
* FlexibleAdapter.OnItemClickListener
* FlexibleAdapter.OnItemLongClickListener
* ==================================== */
    @Override
    public boolean onItemClick(int position) {
     /*   IFlexible flexibleItem = mAdapter.getItem(position);

        if (flexibleItem instanceof MediaItem) {
            if (!MediaTagEditorActivity.navigate(mAdapter.getActivity(), (MediaItem) flexibleItem,position)) {
                mAdapter.removeItem(position);
            }
            return true;
        }
*/
        // Action on elements are allowed if Mode is IDLE, otherwise selection has priority
        if (mAdapter.getMode() != SelectableAdapter.Mode.IDLE && mActionModeHelper != null) {
            boolean activate = mActionModeHelper.onClick(position);
            // Last activated position is now available
            Log.d(TAG, "Last activated position " + mActionModeHelper.getActivatedPosition());

            return activate;
        } else {
            // Handle the item click listener

            // We don't need to activate anything
            return false;
        }
    }

    @Override
    public void onItemLongClick(int position) {
        mActionModeHelper.onLongClick(this, position);
    }

    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == REQUEST_CODE_PERMISSION_All) {
            Uri treeUri = null;
            if (resultCode == Activity.RESULT_OK) {
                // Get Uri from Storage Access Framework.
                treeUri = resultData.getData();

                // Persist access permissions.
                //final int takeFlags = resultData.getFlags()
                //        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                this.getContentResolver().takePersistableUriPermission(treeUri, (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                mAdapter.updateDataSet(mFragment.loadMediaItems(false));
                mAdapter.notifyDataSetChanged();
                //mFragment.onUpdateEmptyView(mFragment.loadMediaItems().size());
            }
        }else if (requestCode == REQUEST_EDIT_MEDIA_TAG) {
            if (resultCode == Activity.RESULT_OK) {
                // Get RESULT String from Storage Access Framework.
                String resultString = resultData.getDataString();
                String id = resultData.getStringExtra(MediaTagEditorActivity.ARG_ID);
                String path = resultData.getStringExtra(MediaTagEditorActivity.ARG_MEDIA_PATH);
                changedPosition = resultData.getIntExtra(MediaTagEditorActivity.ARG_MEDIA_POSITION,-1);
                if(changedPosition<0) {
                    // quit if no position
                    return;
                }
                if("INVALID".equalsIgnoreCase(resultString) || "DELETED".equals(resultString)) {
                    deleteMediaFile(changedPosition, path);
                }else if ("ORGANIZED".equals(resultString)) {
                    String title = resultData.getStringExtra(MediaTagEditorActivity.ARG_MEDIA_TITLE);
                    String artist = resultData.getStringExtra(MediaTagEditorActivity.ARG_MEDIA_ARTIST);
                    String album =resultData.getStringExtra(MediaTagEditorActivity.ARG_MEDIA_ALBUM);
                    updateMediaItem(changedPosition,id,title, artist, album, path);
                }else if ("SAVED".equals(resultString)) {
                    //update tag on screen
                    String title = resultData.getStringExtra(MediaTagEditorActivity.ARG_MEDIA_TITLE);
                    String artist = resultData.getStringExtra(MediaTagEditorActivity.ARG_MEDIA_ARTIST);
                    String album =resultData.getStringExtra(MediaTagEditorActivity.ARG_MEDIA_ALBUM);
                    updateMediaItem(changedPosition,id,title, artist, album, path);
                }
                mAdapter.notifyItemChanged(changedPosition);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private void updateMediaItem(int position, String id, String title, String artist, String album, String path) {
        MediaItem item = getMediaItemById(id);
        if(item !=null) {
            if(title !=null) {
                item.setTitle(title);
            }
            if(artist!=null) {
                item.setArtist(artist);
            }
            if(album!=null) {
                item.setAlbum(album);
            }
            try {
                if(!StringUtils.isEmpty(path) && !path.equals(item.getPath())) {
                item.setPath(path);
                    item.setDisplayPath(mAdapter.getDisplayPath(path));
                }
            } catch (Exception ex) {}
        }
        mAdapter.notifyItemChanged(position);
    }

    private void updateMediaFile(int position, String path) {
        // reload media item form MediaStore
        // update on adapter and refresh UI

        MediaItem newItem = loadMediaItemFromMediaStore(path);
        MediaItem item = getMediaItemByPosition(position);
        if(newItem!=null && item !=null) {
            item.setArtist(newItem.getArtist());
            item.setAlbum(newItem.getAlbum());
            item.setTitle(newItem.getTitle());
        }
        mAdapter.notifyItemChanged(position);
    }

    private void moveMediaFile(int position, String path, String oldPath) {
        // reload media item form MediaStore
        // update on adapter and refresh UI

        MediaItem newItem = loadMediaItemFromMediaStore(path);
        MediaItem item = getMediaItemByPosition(position);

        if(item!=null && newItem!=null) {
            item.setArtist(newItem.getArtist());
            item.setAlbum(newItem.getAlbum());
            item.setTitle(newItem.getTitle());
            item.setPath(path);
            try {
                item.setDisplayPath(mAdapter.getDisplayPath(path));
            }catch (Exception ex){}
        }
        mAdapter.notifyItemChanged(position);
    }

    private void deleteMediaFile(int position, String path) {
        // remove in mediaStore
        tagHelper.deleteFromMediaStore(path);
        // update UI
        mAdapter.removeItem(position, Payload.MOVE);
        mAdapter.notifyItemChanged(position);
    }

    private MediaItem getMediaItemByPosition(int position) {
        return mAdapter.getItem(position);
    }

    private MediaItem loadMediaItemFromMediaStore(String path) {
        return mAdapter.loadMediaItemFromMediaStore(this, path);
    }

    private MediaItem getMediaItemById(String id) {
       return mAdapter.getMediaItemById(id);
    }

	/* ===========
	 * SEARCH VIEW
	 * =========== */

    public void initSearchView(final Menu menu) {
        // Associate searchable configuration with the SearchView
        LogHelper.i(TAG, "onCreateOptionsMenu setup SearchView!");
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            mSearchView.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
            mSearchView.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN);
            mSearchView.setQueryHint(getString(R.string.action_search));
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public void setListeningTitle(String listenTitle, String listenArtist) {
       /* if(mHeaderView==null) {
            mHeaderView = (HeaderView) findViewById(R.id.toolbar_header_view);;
        }
        if(mHeaderView!=null) {
            mHeaderView.bindTo("Now Listening", listenTitle + " - " + listenArtist);
        } */
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        LogHelper.i(TAG, "onQueryTextChange called!");
        if(newText.isEmpty() || newText.length()>2) {
            return mFragment.filterItems(newText);
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        LogHelper.i(TAG, "onQueryTextSubmit called!");
       // return onQueryTextChange(query);
        return mFragment.filterItems(query);
    }

    /* ==========================
     * ACTION MODE IMPLEMENTATION
     * ========================== */

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccentDark_light, this.getTheme()));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_all:
                if(mAdapter.isSelectAll()) {
                    mAdapter.clearSelection();
                }else {
                    mAdapter.selectAll();
                }
                mActionModeHelper.updateContextTitle(mAdapter.getSelectedItemCount());
                // We consume the event
                return true;

            case R.id.action_delete:
                // Build message before delete, for the SnackBar
                StringBuilder message = new StringBuilder();
                message.append(getString(R.string.action_deleted)).append(" ");
                for (Integer pos : mAdapter.getSelectedPositions()) {
                    message.append(extractTitleFrom(mAdapter.getItem(pos)));
                    if (mAdapter.getSelectedItemCount() > 1)
                        message.append(", ");
                }

                List<Integer> positions = mAdapter.getSelectedPositions();
                for (int position:positions) {
                    //MusicTagHelper.
                    //AndroidFile.deleteFile(, this);
                    mAdapter.removeItem(position);
                }
                // Enable Refreshing

                // Finish the action mode
                mActionModeHelper.destroyActionModeIfCan();
                // We consume the event
                return true;

            default:
                // If an item is not implemented we don't consume the event, so we finish the ActionMode
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark_light, this.getTheme()));
    }

    private String extractTitleFrom(MediaItem item) {
        if(item!=null) {
            return item.getTitle();
        }
        return "";
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mSectionsPagerAdapter.getItem(position).onPageSelected();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private List<BrowserViewPagerFragment> fragmentList = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            fragmentList.add(BrowserViewPagerFragment.newInstance(MediaTag.MediaTypes.SONGS));
            //fragmentList.add(BrowserViewPagerFragment.newInstance(MediaTag.MediaTypes.ARTIST));
            //fragmentList.add(BrowserViewPagerFragment.newInstance(MediaTag.MediaTypes.ALBUM));
            fragmentList.add(BrowserViewPagerFragment.newInstance(MediaTag.MediaTypes.SIMILARITY));
            //fragmentList.add(BrowserViewPagerFragment.newInstance(MediaTag.MediaTypes.FILES));
        }

        @Override
        public BrowserViewPagerFragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a FragmentViewPager (defined as a static inner class below).
           // return  BrowserViewPagerFragment.newInstance(position);
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            BrowserViewPagerFragment fragment = getItem(position);
            if(fragment!=null) {
                return fragment.getTitle();
            }
            return "";
        }

        public String[] getTitles() {
            String []titles = new String[getCount()];
            for (int i=0;i <getCount();i++) {
                titles[i] = String.valueOf(getPageTitle(i));
            }
            return titles;
        }
    }
}
