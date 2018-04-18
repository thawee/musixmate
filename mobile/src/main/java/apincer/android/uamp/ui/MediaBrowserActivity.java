package apincer.android.uamp.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.transition.Slide;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.jaeger.library.StatusBarUtil;
import com.lapism.searchview.Search;
import com.lapism.searchview.widget.SearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import apincer.android.provider.StorageProvider;
import apincer.android.residemenu.ResideMenu;
import apincer.android.residemenu.ResideMenuItem;
import apincer.android.storage.StorageUtils;
import apincer.android.uamp.Constants;
import apincer.android.uamp.FileManagerService;
import apincer.android.uamp.MediaItemService;
import apincer.android.uamp.MusicService;
import apincer.android.uamp.R;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.ui.view.DividerItemDecoration;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.UIUtils;
import de.mateware.snacky.Snacky;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.helpers.ActionModeHelper;
import eu.davidea.flexibleadapter.items.IFlexible;
import stream.customalert.CustomAlertDialogue;
import stream.custompermissionsdialogue.PermissionsDialogue;
import stream.custompermissionsdialogue.utils.PermissionUtils;

/**
 * Created by Administrator on 11/23/17.
 */

public class MediaBrowserActivity extends AppCompatActivity implements
        ActionMode.Callback, FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {
    private static final String TAG = LogHelper.makeLogTag(MediaBrowserActivity.class);
    private List<Integer> changedPositions = new ArrayList<>();
    private SearchView mSearchView;
    private TextView mHeaderTitle;
    private TextView mHeaderStorage;
    private int displayPosition =-1;

    // reside menu
    private ResideMenu mResideMenu;
    private ResideMenuItem mRMenuItemLibrary;
    private ResideMenuItem mRMenuItemNew;
    private ResideMenuItem mRMenuItemSimilar;
    private ResideMenuItem mRMenuItemSimilarTitles;
    private ResideMenuItem mRMenuItemHiRes;
    private ResideMenuItem mRMenuItemSettings;
    private ResideMenuItem mRMenuItemAbout;

    private Snackbar mExitSnackbar;
    private Snackbar mSnackbar;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccentDark_light, this.getTheme()));

        mLibraryAdapter.setMode(SelectableAdapter.Mode.MULTI);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        for(int i=0; i< menu.size();i++) {
            MenuItem item = menu.getItem(i);
            UIUtils.setColorFilter(item, getColor(R.color.menu_delete_background));
        }
         return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        List<Integer> selected = mLibraryAdapter.getSelectedPositions();
        switch (item.getItemId()) {
            case R.id.action_select_all:
                if(mLibraryAdapter.isSelectAll()) {
                    item.setIcon(R.drawable.ic_select_all_black_24dp);
                    UIUtils.setColorFilter(item, getColor(R.color.menu_delete_background));
                    mLibraryAdapter.clearSelection();
                    mActionModeHelper.updateContextTitle(mLibraryAdapter.getSelectedItemCount());
                }else {
                    item.setIcon(R.drawable.ic_select_off_white_24dp);
                    UIUtils.setColorFilter(item, getColor(R.color.menu_delete_background));
                    mLibraryAdapter.selectAll();
                    mActionModeHelper.updateContextTitle(mLibraryAdapter.getSelectedItemCount());
                    // We consume the event
                }
                return true;
            case R.id.action_edit_metadata:
                changedPositions.clear();
                List<MediaItem> items = new ArrayList();
                for (int position : selected) {
                    changedPositions.add(position);
                    MediaItem mItem = (MediaItem) mLibraryAdapter.getItem(position);
                    items.add(mItem);
                }
                mActionModeHelper.destroyActionModeIfCan();
                return MetadataActivity.startActivity(this, items);
            case R.id.action_transfer_file:
                doMoveMediaItems(selected);
                mLibraryAdapter.clearSelection();
                mActionModeHelper.destroyActionModeIfCan();
                return true;
            case R.id.action_delete:
                doDeleteMediaItems(selected);
                mLibraryAdapter.clearSelection();
                mActionModeHelper.destroyActionModeIfCan();
               return true;
        }
        mActionModeHelper.destroyActionModeIfCan();
        return false;
    }

    private void doDeleteMediaItems(List<Integer> selectedPositions) {
        final List<MediaItem> itemsList = new ArrayList<>();
        for (int position : selectedPositions) {
            MediaItem mediaItem = (MediaItem) mLibraryAdapter.getItem(position);
            itemsList.add(mediaItem);
        }
        String text = "Delete ";
        if(itemsList.size()>1) {
            text = text + itemsList.size() + " songs?";
        }else {
            text = text + "'"+itemsList.get(0).getTitle()+"' song?";
        }
        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(MediaBrowserActivity.this)
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
                        for (MediaItem item : itemsList) {
                            FileManagerService.addToDelete(item);
                        }
                        // Construct our Intent specifying the Service
                        Intent intent = new Intent(getApplicationContext(), FileManagerService.class);
                        // Add extras to the bundle
                        intent.putExtra(Constants.KEY_COMMAND, apincer.android.uamp.Constants.COMMAND_DELETE);
                        // Start the service
                        startService(intent);
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


    private void doMoveMediaItems(List<Integer> selectedPositions) {
        String text = "Move ";
        final List<MediaItem> itemsList = new ArrayList<>();
        for (int position : selectedPositions) {
            MediaItem mediaItem = (MediaItem) mLibraryAdapter.getItem(position);
            itemsList.add(mediaItem);
        }
        if(itemsList.size()>1) {
            text = text + itemsList.size() + " songs to Music Library?";
        }else {
            text = text + "'"+itemsList.get(0).getTitle()+"' song to Music Library?";
        }
        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(MediaBrowserActivity.this)
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
                        for(MediaItem item:itemsList) {
                            FileManagerService.addToMove(item);
                        }
                        // Construct our Intent specifying the Service
                        Intent intent = new Intent(getApplicationContext(), FileManagerService.class);
                        // Add extras to the bundle
                        intent.putExtra(Constants.KEY_COMMAND, Constants.COMMAND_MOVE);
                        // Start the service
                        startService(intent);
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

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Finish the action mode
        mActionModeHelper.destroyActionModeIfCan();
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark_light, this.getTheme()));
    }

    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == MusicService.REQUEST_CODE_EDIT_MEDIA_TAG) {
            for (int changedPosition : changedPositions) {
                MediaItem item = (MediaItem) mLibraryAdapter.getItem(changedPosition);
                if (item == null || !mProvider.isMediaFileExist(item)) {
                    mLibraryAdapter.removeItem(changedPosition);
                    // remove selection
                    if (mLibraryAdapter.isSelected(changedPosition)) {
                        mLibraryAdapter.removeSelection(changedPosition);
                    }
                }else {
                    mLibraryAdapter.notifyItemChanged(changedPosition);
                }
            }
            mLibraryAdapter.updateHeader();
        }else if (requestCode == MusicService.REQUEST_CODE_SD_PERMISSION) {
           if (resultCode == Activity.RESULT_OK) {
                // Get Uri from Storage Access Framework.
                // Persist access permissions.
                this.getContentResolver().takePersistableUriPermission(resultData.getData(), (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                MediaItemService.startService(getApplicationContext(),"load");
           }
        }
    }

    @Override
    public boolean onItemClick(View view, final int position) {
        if (mLibraryAdapter == null) {
            return false;
        }

        // Action on elements are allowed if Mode is IDLE, otherwise selection has priority
        if (mLibraryAdapter.getMode() != SelectableAdapter.Mode.IDLE && mActionModeHelper != null) {
            boolean activate = mActionModeHelper.onClick(position);
            toggleFloatingActionBar();
            // Last activated position is now available
            LogHelper.d(TAG, "Last activated position " + mActionModeHelper.getActivatedPosition());
            return activate;
        } else {
            // Handle the item click listener
            IFlexible flexibleItem = mLibraryAdapter.getItem(position);
            if (flexibleItem instanceof MediaItem) {
                final MediaItem item = (MediaItem) flexibleItem;
                changedPositions.clear();
                if(MediaItemProvider.getInstance().isMediaFileExist(item)) {
                    changedPositions.add(position);
                    List<MediaItem> items = new ArrayList();
                    items.add(item);
                    return MetadataActivity.startActivity(this, items);
                }else {
                    CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(this)
                            .setStyle(CustomAlertDialogue.Style.DIALOGUE)
                            .setTitle(getString(R.string.alert_error_title))
                            .setMessage(getString(R.string.alert_invalid_media_file, item.getMetadata().getTitle()))
                            .setNegativeText("OK")
                            .setNegativeColor(R.color.negative)
                            .setNegativeTypeface(Typeface.DEFAULT_BOLD)
                            .setOnNegativeClicked(new CustomAlertDialogue.OnNegativeClicked() {
                                @Override
                                public void OnClick(View view, Dialog dialog) {
                                    MediaItemProvider.getInstance().removeFromDatabase(item.getMetadata());
                                    mLibraryAdapter.removeItem(position);
                                    mLibraryAdapter.updateHeader();
                                    dialog.dismiss();
                                }
                            })
                            .setDecorView(getWindow().getDecorView())
                            .build();
                    alert.show();
                }
            }
            // We don't need to activate anything
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        // If ActionMode is active, back key closes it
        if (mActionModeHelper.destroyActionModeIfCan()) {
            toggleFloatingActionBar();
            return;
        }
        if(!StringUtils.isEmpty(String.valueOf(mSearchView.getQuery()))) {
            mSearchView.setQuery("",true);
            return ;
        }
        if (!mExitSnackbar.isShown()) {
            mExitSnackbar.show();
        } else {
            stopService(new Intent(getApplicationContext(),MusicService.class));
            finish();
            mExitSnackbar.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mLibraryAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore previous state
        if (savedInstanceState != null && mLibraryAdapter != null) {
            //Selection
            mLibraryAdapter.onRestoreInstanceState(savedInstanceState);
            mActionModeHelper.restoreSelection(this);
        }
    }

    @Override
    public void onItemLongClick(int position) {
        mActionModeHelper.onLongClick(this, position);
        toggleFloatingActionBar();
    }

    public enum BROWSER_MODE {ALL_SONGS, RECENTLY_PLAYED, NEW_SONGS, HI_RES_AUDIO, SIMILAR_SONGS,SIMILAR_TITLES};

    public class MediaItemAdapter extends FlexibleAdapter {
        private RequestManager glide;
        public MediaItemAdapter(List<MediaItem> itemList, RequestManager glide) {
            super(itemList);
            this.glide = glide;
            RequestOptions options = RequestOptions.errorOf(R.drawable.ic_broken_image_black_24dp);
            options.dontAnimate().placeholder(R.drawable.progress);
            options.fallback(R.drawable.ic_broken_image_black_24dp);
            this.glide.setDefaultRequestOptions(options);
        }

        @Override
        public String onCreateBubbleText(int position) {
            IFlexible iFlexible = getItem(position);
            if (iFlexible == null) return "";
            String text = StringUtils.trimToEmpty(iFlexible.toString());
            return StringUtils.getChars(text, 1).toLowerCase();
        }

        @Override
        protected void onPostUpdate() {
            super.onPostUpdate();
            updateHeader();
            toggleFloatingActionBar();

            if(displayPosition>-1) {
                scrollToPosition(8, displayPosition);
                mLibraryAdapter.notifyDataSetChanged();
                SmoothScrollLinearLayoutManager layoutManager = (SmoothScrollLinearLayoutManager) mRecyclerView.getLayoutManager();
                layoutManager.smoothScrollToPosition(mRecyclerView,null,displayPosition==0?displayPosition:(displayPosition-1));
            }
        }

        @Override
        protected void onPostFilter() {
            super.onPostFilter();
            updateHeader();
            hideFloatingActionBar();
        }

        public void updateHeader() {
            String title;
            switch (getBrowserMode()) {
                case RECENTLY_PLAYED:
                    title = "Recently Played Songs";
                    break;
                case SIMILAR_SONGS:
                    title = "Similar Title & Artist";
                    break;
                case SIMILAR_TITLES:
                    title = "Similar Title";
                    break;
                case NEW_SONGS:
                    title = "Non-Managed Songs";
                    break;
                case HI_RES_AUDIO:
                    title = "High-Resolution Songs";
                    break;
                default:
                    title = "Music Library";
                    break;
            }
            title = title + ": "+getItemCount() + " songs - ";
            long duration = 0;
            for(MediaItem item: mediaItems) {
                    duration = duration + item.getMetadata().getAudioDuration();
            }
            title = title+ MediaItemProvider.formatDuration(duration, true);
            mHeaderTitle.setText(title);

            Map<String, StorageProvider.RootInfo> infos = MediaItemProvider.getInstance().getRootPaths();
            String storage = "";
            StorageUtils utils = new StorageUtils(getApplicationContext());
            List<String> storages = new ArrayList();
            for(StorageProvider.RootInfo info: infos.values()) {
                if(!MediaItemProvider.isDeviceStorage(info)){
                    long free = utils.getPartitionSize(info.path.getAbsolutePath(), false);
                    long total = utils.getPartitionSize(info.path.getAbsolutePath(), true);
                    String storageTitle = MediaItemProvider.getStorageTitle(info) + ": ";
                    storages.add(StringUtils.formatStorageSize(free));
                    storages.add(StringUtils.formatStorageSize(total));
                    storage = storage + storageTitle + StringUtils.formatStorageSize(free) + " free of " + StringUtils.formatStorageSize(total) + " | ";
                }
            }
            if(!StringUtils.isEmpty(storage)) {
                String originalText = StringUtils.trimToEmpty(storage.substring(0, storage.lastIndexOf("|")));
                Spannable spanText = Spannable.Factory.getInstance().newSpannable(originalText);
                for (String sTitle : storages) {
                    int indx = originalText.indexOf(sTitle);
                    if (indx >= 0 && !StringUtils.isEmpty(sTitle)) {
                        spanText.setSpan(new ForegroundColorSpan(getColor(R.color.quality_hires)), indx, indx + sTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spanText.setSpan(new StyleSpan(Typeface.BOLD), indx, indx + sTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                mHeaderStorage.setText(spanText, TextView.BufferType.SPANNABLE);
            }
        }

        public void setupMediaItems() {
                    if (browserMode == BROWSER_MODE.SIMILAR_SONGS) {
                        mediaItems.clear();
                        mediaItems.addAll(MediaItemProvider.getInstance().getSimilarArtistAndTitle());
                    }else if (browserMode == BROWSER_MODE.SIMILAR_TITLES) {
                        mediaItems.clear();
                        mediaItems.addAll(MediaItemProvider.getInstance().getSimilarTitle());
                    } else if (browserMode == BROWSER_MODE.NEW_SONGS) {
                        mediaItems.clear();
                        mediaItems.addAll(MediaItemProvider.getInstance().getNewMediaItems());
                    } else if(browserMode == BROWSER_MODE.HI_RES_AUDIO) {
                        mediaItems.clear();
                        mediaItems.addAll(MediaItemProvider.getInstance().getHiresMediaItems());
                    } else {
                        mediaItems.clear();
                        mediaItems.addAll(MediaItemProvider.getInstance().getMediaItems());
                    }
                    updateDataSet(mediaItems);
                    notifyDataSetChanged();
        }

        private void resetAdapter() {
            if(mSearchView!=null && !StringUtils.isEmpty(String.valueOf(mSearchView.getQuery()))) {
                mSearchView.setQuery("", false);
            }
            if(mLibraryAdapter!=null) {
                mLibraryAdapter.setFilter("");
            }
            mActionModeHelper.destroyActionModeIfCan();
        }

        public BROWSER_MODE getBrowserMode() {
            return browserMode;
        }

        public RequestManager getGlide() {
            return glide;
        }

        public int getItemPosition(int mediaId) {
            for(int i=0;i<getItemCount();i++) {
                MediaItem item = (MediaItem) getItem(i);
                if(item.getId() == mediaId) {
                    return i;
                }
            }
            return -1;
        }
    }

    protected FastScroller mFastScroller;
    protected RecyclerView mRecyclerView;
    private MediaItemAdapter mLibraryAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton fabListeningAction;
    private ActionModeHelper mActionModeHelper;

    private List<MediaItem> mediaItems = new ArrayList<>();
    private MediaItemProvider mProvider;
    private BROWSER_MODE browserMode = BROWSER_MODE.ALL_SONGS;

    private void initActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       // getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        super.onCreate(savedInstanceState);
        initActivityTransitions();
        setContentView(R.layout.activity_browser);
        StatusBarUtil.setColor(this, getColor(R.color.colorPrimaryDark_light));
        setUpPermissions();

        // Initialize the views
        mProvider = MediaItemProvider.getInstance();

        mExitSnackbar = Snacky.builder().setActivity(MediaBrowserActivity.this)
                .setText(getString(R.string.alert_back_to_exit))
                .setDuration(Snacky.LENGTH_LONG)
                .setMaxLines(1)
                .warning();

        setUpResideMenu();
        setUpSearchView();
        setUpRecycleView();
        setUpFloatingAction();
        setUpActionModeHelper(SelectableAdapter.Mode.IDLE);
        loadMediaItems(BROWSER_MODE.ALL_SONGS);
        setUpSwipeToRefresh();
        mLibraryAdapter.notifyDataSetChanged();
    }

    private void setUpPermissions() {
        if (!PermissionUtils.IsPermissionsEnabled(this, MusicService.PERMISSIONS_ALL)) {
            PermissionsDialogue.Builder alertPermissions = new PermissionsDialogue.Builder(MediaBrowserActivity.this)
                    .setCancelable(false)
                    .setMessage(getString(R.string.app_name) + " requires the following permissions to manage music on Android: ")
                    .setIcon(R.drawable.ic_launcher)
                    .setRequireStorage(PermissionsDialogue.REQUIRED)
                    .setOnContinueClicked(new PermissionsDialogue.OnContinueClicked() {
                        @Override
                        public void OnClick(View view, Dialog dialog) {
                            dialog.dismiss();
                            setUpPermissionSAF();
                        }
                    })
                    .setDecorView(getWindow().getDecorView())
                    .build();
            alertPermissions.show();
        }else {
            MediaItemService.startService(getApplicationContext(),"load");
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setUpPermissionSAF() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, MusicService.REQUEST_CODE_SD_PERMISSION);
    }

    private void extractDisplayPosition(Intent intent) {
        String nowPlaying = intent.getStringExtra(MusicService.FLAG_SHOW_LISTENING);
        if("yes".equalsIgnoreCase(nowPlaying)) {
            int mediaId = intent.getIntExtra("mediaId", -1);
            if(mediaId>-1) {
                int position = getItemPositionForAllSongs(mediaId);
                if(position>-1) {
                    displayPosition = position;
                }
            }
        }
    }

    private void setUpResideMenu() {
        mResideMenu = new ResideMenu(this);
        mResideMenu.setUse3D(true);
        mResideMenu.setShadowVisible(true);
        mResideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_RIGHT);
        mResideMenu.setBackground(R.drawable.bg);
        mResideMenu.attachToActivity(this);
        //valid scale factor is between 0.0f and 1.0f. leftmenu' width is 180dip.
        mResideMenu.setScaleValue(0.6f);
       // LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
       // View customView = inflater.inflate(R.layout.view_list_header, null);
       // mResideMenu.setCustomHeader(customView);

        // items
        mRMenuItemLibrary = new ResideMenuItem(this, R.drawable.ic_library_music_black_24dp,     "Music Library");
        mRMenuItemHiRes = new ResideMenuItem(this, R.drawable.ic_library_music_black_24dp,     "Hi-Res Library");
        mRMenuItemNew = new ResideMenuItem(this, R.drawable.ic_filter_new_black_24dp,     "#NEW Songs");
        mRMenuItemSimilar = new ResideMenuItem(this, R.drawable.ic_filter_2_black_24dp,     "#Similar Songs");
        mRMenuItemSimilarTitles = new ResideMenuItem(this, R.drawable.ic_filter_1_black_24dp,     "#Similar Titles");
        mRMenuItemSettings = new ResideMenuItem(this, R.drawable.ic_settings_applications_black_24dp,     "Settings");
        mRMenuItemAbout = new ResideMenuItem(this, R.drawable.ic_copyright_black_24dp,     "About");

        // add listener to item
        View.OnClickListener onClickListener =new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v == mRMenuItemLibrary) {
                    // music library
                    displayPosition = 0;
                    mSearchView.close( );
                    loadMediaItems(BROWSER_MODE.ALL_SONGS);
                    mResideMenu.closeMenu();
                }else if(v == mRMenuItemHiRes) {
                    // similar titles
                    displayPosition = 0;
                    mSearchView.close( );
                    loadMediaItems(BROWSER_MODE.HI_RES_AUDIO);
                    mResideMenu.closeMenu();
                }else if(v == mRMenuItemSimilar) {
                    // similar titles
                    displayPosition = 0;
                    mSearchView.close();
                    loadMediaItems(BROWSER_MODE.SIMILAR_SONGS);
                    mResideMenu.closeMenu();
                }else if(v == mRMenuItemSimilarTitles) {
                    // similar titles
                    displayPosition = 0;
                    mSearchView.close();
                    loadMediaItems(BROWSER_MODE.SIMILAR_TITLES);
                    mResideMenu.closeMenu();
            }else if(v == mRMenuItemNew) {
                    // untagged titles
                    displayPosition = 0;
                    mSearchView.close();
                    loadMediaItems(BROWSER_MODE.NEW_SONGS);
                    mResideMenu.closeMenu();
                }else if(v==mRMenuItemSettings) {
                    // settings
                    Intent myIntent = new Intent(MediaBrowserActivity.this, SettingsActivity.class);
                    startActivity(myIntent);
                }else if(v==mRMenuItemAbout) {
                    // about
                    Intent myIntent = new Intent(MediaBrowserActivity.this, AboutActivity.class);
                    startActivity(myIntent);
                }
            }
        };

        mRMenuItemLibrary.setOnClickListener(onClickListener);
        mRMenuItemHiRes.setOnClickListener(onClickListener);
        mRMenuItemNew.setOnClickListener(onClickListener);
        mRMenuItemSimilar.setOnClickListener(onClickListener);
        mRMenuItemSimilarTitles.setOnClickListener(onClickListener);
        mRMenuItemSettings.setOnClickListener(onClickListener);
        mRMenuItemAbout.setOnClickListener(onClickListener);

        //add to reside menu
        mResideMenu.addMenuItem(mRMenuItemLibrary, ResideMenu.DIRECTION_LEFT);
        mResideMenu.addMenuItem(mRMenuItemHiRes, ResideMenu.DIRECTION_LEFT);
        mResideMenu.addMenuItem(mRMenuItemNew, ResideMenu.DIRECTION_LEFT);
        mResideMenu.addMenuItem(mRMenuItemSimilarTitles, ResideMenu.DIRECTION_LEFT);
        mResideMenu.addMenuItem(mRMenuItemSimilar, ResideMenu.DIRECTION_LEFT);
        mResideMenu.addMenuItem(mRMenuItemSettings, ResideMenu.DIRECTION_LEFT);
        mResideMenu.addMenuItem(mRMenuItemAbout, ResideMenu.DIRECTION_LEFT);
    }

    private void loadMediaItems(BROWSER_MODE mode) {
        browserMode = mode;
        mLibraryAdapter.resetAdapter();
        mLibraryAdapter.setupMediaItems();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mResideMenu.dispatchTouchEvent(ev);
    }

    private int getItemPositionForAllSongs(int mediaId) {
        MediaItem[] items = (MediaItem[]) MediaItemProvider.getInstance().getMediaItems().toArray(new MediaItem[0]);
        for(int i=0; i<items.length;i++) {
            MediaItem item = items[i];
            if(item.getId() == mediaId) {
                return i;
            }
        }
        return -1;
    }

    private void setUpFloatingAction() {
        //FAB listening
        fabListeningAction = findViewById(R.id.fabListeningAction);
        ViewCompat.animate(fabListeningAction).scaleX(0f).scaleY(0f).alpha(0f).start();
        // listening action
        fabListeningAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();
                scrollToListening();
            }
        });
        fabListeningAction.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                hideKeyboard();
                MusicService.getRunningService().playNextSong();
                return true;
            }
        });

        toggleFloatingActionBar();;
    }

    private boolean scrollToListening() {
        if(MusicService.getRunningService()==null) return false;

        if(mLibraryAdapter==null) return false;

        MediaItem item = MusicService.getRunningService().getListeningSong();
        if(item == null) return false;
        int position = getItemPositionForAllSongs(item.getId());
        position = scrollToPosition(8,position);
        if(position<0) return false;

        mLibraryAdapter.notifyDataSetChanged();
        SmoothScrollLinearLayoutManager layoutManager = (SmoothScrollLinearLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.smoothScrollToPosition(mRecyclerView,null,position==0?position:(position-1));
        return true;
    }

    private int scrollToPosition(int offset, int position) {
        if(position>-1) {
            int positionWithOffset = position - offset;
            if (positionWithOffset < 0) {
                positionWithOffset = 0;
            }
            mRecyclerView.scrollToPosition(positionWithOffset);
        }
        return position;
    }

    @Override

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        extractDisplayPosition(intent);
        // show listening on all songs mode only.
        browserMode = BROWSER_MODE.ALL_SONGS;
        mSearchView.setQuery("", false);
        mLibraryAdapter.resetAdapter();

        mLibraryAdapter.setupMediaItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(FileManagerService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(operationReceiver, filter);

        filter = new IntentFilter(MusicService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(playingReceiver, filter);

        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(operationReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playingReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpActionModeHelper(@SelectableAdapter.Mode int mode) {
        mActionModeHelper = new ActionModeHelper(mLibraryAdapter, R.menu.menu_context, this) {
            // Override to customize the title
            @Override
            public void updateContextTitle(int count) {
                // You can use the internal mActionMode instance
                if (mActionMode != null) {
                    mActionMode.setTitle(count == 1 ?
                            getString(R.string.action_selected_one, Integer.toString(count)) :
                            getString(R.string.action_selected_many, Integer.toString(count)));
                }
            }
        }
        .disableDragOnActionMode(true)
        .disableSwipeOnActionMode(true)
        .withDefaultMode(mode);
        mLibraryAdapter.setMode(mode);
    }

    protected void setUpSearchView() {
        mHeaderStorage = findViewById(R.id.header_storage);
        mHeaderTitle = findViewById(R.id.header_title);
        mHeaderTitle.setOnLongClickListener(new View.OnLongClickListener() {
                                                @Override
                                                public boolean onLongClick(View v) {
                                                    scrollToPosition(0, 0);
                                                    return false;
                                                }
                                            });
        mSearchView = findViewById(R.id.searchView); // from API 26
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(new Search.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(CharSequence query) {
                    filterItems(query.toString());
                    return true;
                }

                @Override
                public void onQueryTextChange(CharSequence newText) {
                    filterItems(newText.toString());
                }
            });
            mSearchView.setOnLogoClickListener(new Search.OnLogoClickListener() {
                @Override
                public void onLogoClick() {
                    mResideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
                }
            });
        }
    }

    private void toggleFloatingActionBar() {
        // hide
        hideFloatingActionBar();

        if(MusicService.getRunningService()!=null && MusicService.getRunningService().getListeningSong()!=null) {
            if(browserMode == BROWSER_MODE.ALL_SONGS && !mLibraryAdapter.hasFilter()) {
                if(mActionModeHelper==null || mActionModeHelper.getActionMode()==null || mActionModeHelper.getActionMode().equals(SelectableAdapter.Mode.IDLE)){
                    fabListeningAction.setImageBitmap(buildPlayerIcon());
                    ViewCompat.animate(fabListeningAction)
                            .scaleX(1f).scaleY(1f)
                            .alpha(1f).setDuration(200)
                            .setStartDelay(300L)
                            .start();
                }
            }
        }
    }

    private Bitmap buildPlayerIcon() {
        Bitmap bitmap = MusicService.getRunningService().getPlayerIcon();
        if(bitmap==null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_touch_app_white_24dp);
        }
        return bitmap;
    }

    private void hideFloatingActionBar() {
        ViewCompat.animate(fabListeningAction)
                .scaleX(0f).scaleY(0f)
                .alpha(0f).setDuration(100)
                .start();
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if(inputManager !=null) {
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void setUpSwipeToRefresh() {
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setDistanceToTriggerSync(390);
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_purple, android.R.color.holo_blue_light,
                android.R.color.holo_green_light, android.R.color.holo_orange_light);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Passing true as parameter we always animate the changes between the old and the new data set
                mSwipeRefreshLayout.setRefreshing(true);
                mActionModeHelper.destroyActionModeIfCan();
                loadMediaItems(browserMode);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    protected void setUpRecycleView() {
        // Initialize Adapter and RecyclerView
       // mediaItems.clear();
       // mediaItems.addAll(MediaItemProvider.getInstance().getMediaItems());
        mLibraryAdapter = new  MediaItemAdapter(mediaItems,GlideApp.with(this));
        mLibraryAdapter.addListener(this);
        mLibraryAdapter.setDisplayHeadersAtStartUp(false);
        mLibraryAdapter.setStickyHeaderElevation(2)
                .setStickyHeaders(false);
        //mLibraryAdapter.setAnimateToLimit(100);
        // When true, filtering on big list is very slow!
        mLibraryAdapter.setNotifyMoveOfFilteredItems(false)
                .setNotifyChangeOfUnfilteredItems(false)
                .setAnimationInitialDelay(100L)
                .setAnimationOnForwardScrolling(true)
                .setAnimationOnReverseScrolling(false)
                .setOnlyEntryAnimation(true);

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setItemViewCacheSize(0); //Setting ViewCache to 0 (default=2) will animate items better while scrolling down+up with LinearLayout
        mRecyclerView.setWillNotCacheDrawing(true);
        mRecyclerView.setLayoutManager(new SmoothScrollLinearLayoutManager(this));
        mRecyclerView.setAdapter(mLibraryAdapter);
        mRecyclerView.setHasFixedSize(true); //Size of RV will not change
            // NOTE: Use default item animator 'canReuseUpdatedViewHolder()' will return true if
            // a Payload is provided. FlexibleAdapter is actually sending Payloads onItemChange.
            //mRecyclerView.setItemAnimator(new DefaultItemAnimator());
      //  RecyclerView.ItemDecoration itemDecoration = new LinearDividerItemDecoration(this, Color.WHITE,2);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, R.drawable.shadow_below);

        mRecyclerView.addItemDecoration(itemDecoration);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                        toggleFloatingActionBar();
                        if(mFastScroller!=null) {
                            mFastScroller.hideScrollbar();
                        }
                    }else {
                        hideFloatingActionBar();
                        if(mFastScroller!=null) {
                            mFastScroller.showScrollbar();
                        }
                    }
                }
            });

        // Add FastScroll to the RecyclerView, after the Adapter has been attached the RecyclerView!!!
        mFastScroller = findViewById(R.id.fast_scroller);
        mFastScroller.setVisibility(View.VISIBLE);
        //fastScroller.setBubbleAndHandleColor(getColor(R.color.colorPrimary_alpha_200));
        mFastScroller.setBubbleAndHandleColor(getColor(R.color.now_playing));
        // 0 pixel is the default value. When > 0 it mimics the fling gesture
        // OnScrollStateChangeListener remains optional
        mFastScroller.setMinimumScrollThreshold(70);
        mFastScroller.setRecyclerView(mRecyclerView);
        mFastScroller.addOnScrollStateChangeListener(new FastScroller.OnScrollStateChangeListener() {
            @Override
            public void onFastScrollerStateChange(boolean scrolling) {
                if (scrolling) {
                    hideFloatingActionBar();
                }else {
                    toggleFloatingActionBar();
                }
            }
        });
        // Finally, assign the Fastscroller to the Adapter
        mLibraryAdapter.setFastScroller(mFastScroller);
        mLibraryAdapter.toggleFastScroller();
    }

    public boolean filterItems(String newText) {
        LogHelper.d(TAG, "onQueryTextChange newText: " + newText);
        if (mLibraryAdapter.hasNewFilter(newText)) {
            mLibraryAdapter.setFilter(newText);
            // Fill and Filter mItems with your custom list and automatically animate the changes
            mLibraryAdapter.filterItems(mediaItems, 100);
        }
        // Disable SwipeRefresh if search is active!!
        mSwipeRefreshLayout.setEnabled(!mLibraryAdapter.hasFilter());
        return true;
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, MediaBrowserActivity.class);
        context.startActivity(starter);
    }

    // Define the callback for what to do when data is received
    private BroadcastReceiver operationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra(Constants.KEY_RESULT_CODE, RESULT_CANCELED);
            if (resultCode == RESULT_OK) {
                String status = intent.getStringExtra(Constants.KEY_STATUS);
                String message = intent.getStringExtra(Constants.KEY_MESSAGE);
                int mediaId = intent.getIntExtra(Constants.KEY_MEDIA_ID, -1);
                String command = intent.getStringExtra(Constants.KEY_COMMAND);

                if(Constants.STATUS_SUCCESS.equalsIgnoreCase(status)) {
                    if (mSnackbar != null) {
                        mSnackbar.dismiss();
                        mSnackbar = null;
                    }
                    if(Constants.COMMAND_MOVE.equalsIgnoreCase(command) ||
                            Constants.COMMAND_SAVE.equalsIgnoreCase(command)) {
                        int position = mLibraryAdapter.getItemPosition(mediaId);
                        if(position>-1) {
                            mLibraryAdapter.notifyItemChanged(position);
                        }
                    }else if(Constants.COMMAND_DELETE.equalsIgnoreCase(command)) {
                        int position = mLibraryAdapter.getItemPosition(mediaId);
                        if(position>-1) {
                            MediaItem item = (MediaItem) mLibraryAdapter.getItem(position);
                            if(!mProvider.isMediaFileExist(item)) {
                                mLibraryAdapter.removeItem(position);
                                mLibraryAdapter.updateHeader();
                            }
                        }
                    }
                }else if (Constants.STATUS_START.equalsIgnoreCase(status)) {
                    if(mSnackbar!=null) {
                        mSnackbar.dismiss();
                        mSnackbar= null;
                    }
                    mSnackbar = Snacky.builder().setActivity(MediaBrowserActivity.this)
                            .setText(message)
                            .setDuration(Snacky.LENGTH_INDEFINITE)
                            .setMaxLines(1)
                            .info();
                    mSnackbar.show();
                } else if(Constants.STATUS_FAIL.equalsIgnoreCase(status)) {
                    if(mSnackbar!=null) {
                        mSnackbar.dismiss();
                        mSnackbar= null;
                    }
                    mSnackbar = Snacky.builder().setActivity(MediaBrowserActivity.this)
                            .setText(message)
                            .setDuration(Snacky.LENGTH_LONG)
                            .setMaxLines(1)
                            .error();
                    mSnackbar.show();
                }
            }
        }
    };

    // Define the callback for what to do when data is received
    private BroadcastReceiver playingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra(Constants.KEY_RESULT_CODE, RESULT_CANCELED);
            if (resultCode == RESULT_OK && mLibraryAdapter!=null) {
                if(browserMode==BROWSER_MODE.ALL_SONGS && !mLibraryAdapter.hasFilter()) {
                    scrollToListening();
                }
            }
        }
    };
}
