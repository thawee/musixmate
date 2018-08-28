package apincer.android.uamp.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import apincer.android.dialog.BottomSheetDialog;
import apincer.android.listener.OnMenuItemClickListener;
import apincer.android.menu.residemenu.ResideMenu;
import apincer.android.menu.residemenu.ResideMenuItem;
import apincer.android.uamp.Constants;
import apincer.android.uamp.R;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.model.HeaderItem;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.service.MediaItemIntentService;
import apincer.android.uamp.service.MediaItemScanService;
import apincer.android.uamp.service.MusicListeningService;
import apincer.android.uamp.ui.view.LinearDividerItemDecoration;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.UIUtils;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.helpers.ActionModeHelper;
import eu.davidea.flexibleadapter.items.IFlexible;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import stream.customalert.CustomAlertDialogue;
import stream.custompermissionsdialogue.PermissionsDialogue;
import stream.custompermissionsdialogue.utils.PermissionUtils;

/**
 * Created by Administrator on 11/23/17.
 */

public class MediaBrowserActivity extends AppCompatActivity implements
        ActionMode.Callback, FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {
    private static final String TAG = LogHelper.makeLogTag(MediaBrowserActivity.class);
    private static final int RECYCLEVIEW_ITEM_POSITION_OFFSET=10;
    private static final int FORMAT_MENU_ID = 888888888;
    private static final int CODEC_MENU_ID = 999999999;

    private List<Integer> changedPositions = new ArrayList<>();
    //private int displayPosition =-1;
    private boolean displayListenningSong = false;

    private BottomNavigationView bottomNavigation;

    private ResideMenu mResideMenu;

    private MediaItem prevListeningSong;

  //  private StyleableToast mExitSnackbar;
   // private Snackbar mSnackbar;

    //Header
    View vHeaderPanel;
  //  View vHeaderSearchPanel;
    View vHeaderTitlePanel;
    TextView txtHeaderTitle;
    TextView txtHeaderTitle1;
    TextView txtHeaderSubtitle;

    //search box
   // View vSearchAction;
   // ImageView btnSearchAction;
    // View vSearchClose;
   // EditText searchText;


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
       // getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccentDark_light, this.getTheme()));

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
                .setPositiveText("Delete")
                .setPositiveColor(R.color.negative)
                .setPositiveTypeface(Typeface.DEFAULT_BOLD)
                .setOnPositiveClicked(new CustomAlertDialogue.OnPositiveClicked() {
                    @Override
                    public void OnClick(View view, Dialog dialog) {
                        MediaItemIntentService.startService(getApplicationContext(),Constants.COMMAND_DELETE,itemsList);
                       // MediaItemProvider.getInstance().deleteMediaItems(itemsList);
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
            text = text + itemsList.size() + " songs to Music Directory?";
        }else {
            text = text + "'"+itemsList.get(0).getTitle()+"' song to Music Directory?";
        }
        CustomAlertDialogue.Builder alert = new CustomAlertDialogue.Builder(MediaBrowserActivity.this)
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
                        MediaItemIntentService.startService(getApplicationContext(),Constants.COMMAND_MOVE,itemsList);
                       // MediaItemProvider.getInstance().manageMediaItems(itemsList);
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

    private void doShowMusicMate() {
        BottomSheetDialog.Builder builder =new BottomSheetDialog.Builder(this);
        builder.setTitle("Music Mate");
        //builder.setIcon(R.drawable.);
        builder.setTitleColor(R.color.colorAccent);
       // builder.setContent("Test Content.....");
        builder.setMenuOrientation(BottomSheetDialog.Builder.ORIENTATION.HORIZONTAL);
        builder.setMenuRes(R.menu.menu_music_mate);
       // builder.setMenuCarousel(true);
        builder.onMenuItemClick(new OnMenuItemClickListener(){
            @Override
            public void onClick(MenuItem item) {
                onOptionsItemSelected(item);
            }
        });
        builder.show(getSupportFragmentManager());

        /*
        final CarouselMenu newFragment = new CarouselMenuButtomSheet();
        newFragment.setTitle("Music Mate");
        newFragment.setMenuRes(R.menu.menu_music_mate);
        newFragment.setOnMenuItemClickListener(new OnMenuItemClickListener(){
            @Override
            public void onClick(MenuItem item) {
                onOptionsItemSelected(item);
                newFragment.dismiss();
            }
        });
        newFragment.setOrientation(1);
        newFragment.show(getSupportFragmentManager(), "dialog");
        */
    }

    private void doScanMediaItems() {
        MediaItemScanService.startService(getApplicationContext(),Constants.COMMAND_SCAN);
                        MediaItemScanService.startService(getApplicationContext(),Constants.COMMAND_CLEAN_DB);
        /*if(mSnackbar!=null) {
            mSnackbar.dismiss();
            mSnackbar= null;
        }
        mSnackbar = Snacky.builder().setActivity(MediaBrowserActivity.this)
                .setText("Starting background metadata scaning service.")
                .setDuration(Snacky.LENGTH_LONG)
                .setMaxLines(1)
                .info();
        mSnackbar.show();
        */
       // StyleableToast.makeText(getApplicationContext(), "Start scanning metadata in background.", R.style.ScanSongStyles).show();
        mLibraryAdapter.showNotification("Starting scan metadata in background.", getColor(R.color.colorPrimaryDark), 1000);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Finish the action mode
        mActionModeHelper.destroyActionModeIfCan();
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark_light, this.getTheme()));
    }

    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == MusicListeningService.REQUEST_CODE_EDIT_MEDIA_TAG) {
            for (int changedPosition : changedPositions) {
                MediaItem item = (MediaItem) mLibraryAdapter.getItem(changedPosition);
                if (item == null || !MediaItemProvider.isMediaFileExist(item)) {
                    mLibraryAdapter.removeItem(changedPosition);
                    // remove selection
                    if (mLibraryAdapter.isSelected(changedPosition)) {
                        mLibraryAdapter.removeSelection(changedPosition);
                    }
                }else {
                    mLibraryAdapter.notifyItemChanged(changedPosition);
                }
            }
           // mLibraryAdapter.updateHeader();
        }else if (requestCode == MusicListeningService.REQUEST_CODE_SD_PERMISSION) {
           if (resultCode == Activity.RESULT_OK) {
                // Get Uri from Storage Access Framework.
                // Persist access permissions.
                this.getContentResolver().takePersistableUriPermission(resultData.getData(), (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
               MediaItemScanService.startService(getApplicationContext(),Constants.COMMAND_SCAN);
               // MediaItemProvider.getInstance().scanFromMediaStore(true);
           }
        }
    }

    @Override
    public boolean onItemClick(View view, final int position) {
        if (mLibraryAdapter == null) {
            return false;
        }

        // Action on elements are allowed if Mode is IDLE, otherwise selection has priority
        if (mActionModeHelper != null && mLibraryAdapter.getMode() != SelectableAdapter.Mode.IDLE) {
            boolean activate = mActionModeHelper.onClick(position);
            // Last activated position is now available
            LogHelper.d(TAG, "Last activated position " + mActionModeHelper.getActivatedPosition());
            return activate;
        } else {
            // Handle the item click listener
            IFlexible flexibleItem = mLibraryAdapter.getItem(position);
            if (flexibleItem instanceof MediaItem) {
                final MediaItem item = (MediaItem) flexibleItem;
                changedPositions.clear();
                displayListenningSong = false;
                if(MediaItemProvider.isMediaFileExist(item)) {
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
                                   // mLibraryAdapter.updateHeader();
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
            return;
        }


      /*  if(!btnSearch.isIconified()) {
            btnSearch.setIconified(true);
            return ;
        }
        */

        super.onBackPressed();
        //if(!StringUtils.isEmpty(String.valueOf(mSearchView.getQuery()))) {
        //    mSearchView.setQuery("",true);
        //    return ;
        //}
        /*
        if (!mExitSnackbar.isShown()) {
            mExitSnackbar.show();
        } else {
            stopService(new Intent(getApplicationContext(),MusicListeningService.class));
            finish();
            mExitSnackbar.cancel();
        } */
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
        displayListenningSong = false;
        mActionModeHelper.onLongClick(this, position);
    }

    public enum BROWSER_MODE {ALL_SONGS, RECENTLY_PLAYED, NEW_SONGS, HD_AUDIO, SIMILAR_SONGS,SIMILAR_TITLES,LLAC_AUDIO,LSAC_AUDIO,SAMPLING_RATE,AUDIO_FORMAT};

    public class MediaItemAdapter extends FlexibleAdapter {
        private BROWSER_MODE browserMode = BROWSER_MODE.ALL_SONGS;
        private String browserTitle = "";
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

            if(displayListenningSong) {
                scrollToListening();
                displayListenningSong = false;
            }
        }

        @Override
        protected void onPostFilter() {
            super.onPostFilter();
          //  updateHeader();
        }

        public void showNotification(String message, int color, long durations) {
           /* Spannable spanText = Spannable.Factory.getInstance().newSpannable(message);
            spanText.setSpan(new ForegroundColorSpan(color), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mHeaderStorage.setText(spanText, TextView.BufferType.SPANNABLE);
            Handler handler =new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                   // updateHeader();
                }
            },durations);
            */
            StyleableToast.makeText(getApplicationContext(),message,R.style.ManageSongStyles);
        }

        private void resetAdapter() {
            if(mLibraryAdapter!=null) {
                mLibraryAdapter.setFilter("");
            }
            mActionModeHelper.destroyActionModeIfCan();
        }

        protected void setMediaItems(BROWSER_MODE mode, List<MediaItem> items) {
            browserMode = mode;
            mLibraryAdapter.resetAdapter();
            mLibraryAdapter.updateDataSet(items);
            mLibraryAdapter.notifyDataSetChanged();
        }

        public BROWSER_MODE getBrowserMode() {
            return browserMode;
        }

        public RequestManager getGlide() {
            return glide;
        }

        public int getItemPosition(String mediaPath) {
            if(StringUtils.isEmpty(mediaPath)) return -1;
            for(int i=0;i<getItemCount();i++) {
                IFlexible flexible = getItem(i);
                if(flexible instanceof  MediaItem) {
                    MediaItem item = (MediaItem) getItem(i);
                    if (item!=null && item.getPath().equals(mediaPath)) {
                        return i;
                    }
                }
            }
            return -1;
        }

        public String getBrowserTitle() {
            return browserTitle;
        }

        public void setBrowserTitle(String title) {
            this.browserTitle = title;
        }
    }

    protected FastScroller mFastScroller;
    protected RecyclerView mRecyclerView;
    private MediaItemAdapter mLibraryAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ActionModeHelper mActionModeHelper;

    private List<MediaItem> mediaItems = new ArrayList<>();

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
        super.onCreate(savedInstanceState);
        initActivityTransitions();
        setContentView(R.layout.activity_browser);

        setUpPermissions();
        setUpBottomNavigation();
        setUpRecycleView();
        setUpHeaderPanel();
        setUpSwipeToRefresh();
        setUpResideMenus();

        setUpActionModeHelper(SelectableAdapter.Mode.IDLE);
        mLibraryAdapter.setMode(SelectableAdapter.Mode.IDLE);
        displayListenningSong = true;
        buildMediaItems(BROWSER_MODE.ALL_SONGS,"All");
/*
        mExitSnackbar = Snacky.builder().setActivity(MediaBrowserActivity.this)
                .setText(getString(R.string.alert_back_to_exit))
                .setDuration(Snacky.LENGTH_LONG)
                .setMaxLines(1)
                .warning();
                */
       // mExitSnackbar = StyleableToast.makeText(getApplicationContext(), getString(R.string.alert_back_to_exit), R.style.ExitStyles);
    }

    private void setUpHeaderPanel() {
       // vSearchAction = findViewById(R.id.header_search_action);
       // btnSearchAction = findViewById(R.id.btn_search_action);
       // vSearchClose = findViewById(R.id.header_close_search);
        vHeaderPanel = findViewById(R.id.header_panel);
        //vHeaderSearchPanel = findViewById(R.id.header_search_bar);
        vHeaderTitlePanel = findViewById(R.id.header_title_bar);
        txtHeaderTitle1 = findViewById(R.id.header_title1);
        txtHeaderTitle = findViewById(R.id.header_title);
        txtHeaderSubtitle = findViewById(R.id.header_subtitle);
       // searchText = findViewById(R.id.searchText);

        vHeaderPanel.setVisibility(View.VISIBLE);
        vHeaderTitlePanel.setVisibility(View.VISIBLE);
      //  vHeaderSearchPanel.setVisibility(View.GONE);
      //  btnSearchAction.setImageDrawable(UIUtils.getTintedDrawable(getApplicationContext(), R.drawable.ic_search_black_24dp,getColor(R.color.colorAccent)));
/*
        vHeaderTitlePanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHeaderSearchPanel();
            }
        }); */
/*
        vSearchAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(vHeaderSearchPanel.getVisibility()==View.GONE) {
                    // open
                    showHeaderSearchPanel();
                }else {
                    //close
                    showHeaderTitlePanel();
                }
            }
        }); */
        /*
        searchText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == EditorInfo.IME_ACTION_SEARCH ||
                        keyCode == EditorInfo.IME_ACTION_DONE ||
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    mLibraryAdapter.setFilter(searchText.getText().toString());
                    mLibraryAdapter.filterItems();
                }
                return false; // pass on to other listeners.
            }
        }); */
    }

    private void setUpBottomNavigation() {
        bottomNavigation = findViewById(R.id.navigationView);
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.navigation_collections:
                            doShowLeftMenus();
                            return false;
                    case R.id.navigation_settings:
                        doShowMusicMate();
                        return false;
                    case R.id.navigation_search:
                        displayListenningSong = false;
                        doShowSearch();
                        return false;
                    case R.id.navigation_listening:
                        //hideKeyboard(getApplicationContext());
                        displayListenningSong = true;
                        scrollToListening();
                        return false;
                    default:
                            return false;
                }
            }
        });
    }

    private void doShowSearch() {
        View cview = getLayoutInflater().inflate(R.layout.view_searchbox, null);
        BottomSheetDialog.Builder builder = new BottomSheetDialog.Builder(this);
       // builder.setType(BottomSheetDialog.Builder.TYPE.BOTTOM_SHEET);
        builder.setCustomView(cview);
        builder.setCancelable(true);
        builder.setDimBrackground(false);
        // final DialogBottomSheet newFragment = new DialogBottomSheet();

       // builder.setTitle("Search Music");
        //builder.setIcon(R.drawable.ic_search_black_24dp);
        //builder.setTitleColor(R.color.colorAccent);
      //  builder.setNegativeText(R.string.cancel);
      //  builder.setPositiveText("UPDATE");
      //  builder.setMenu(R.menu.menu_editor);
      //  builder.setMenuOrientation(BottomSheetDialog.Builder.ORIENTATION.VERTICAL);

       // newFragment.setCustomView(cview);
       // newFragment.show(getSupportFragmentManager(), "dialog");
        DialogFragment dialogFragment = builder.build();
        dialogFragment.show(getSupportFragmentManager(), "search");

        TextView txtSearch = cview.findViewById(R.id.searchText);
        String keyword = (String) mLibraryAdapter.getFilter(String.class);
        if(!StringUtils.isEmpty(keyword)) {
            txtSearch.setText(keyword);
        }
        txtSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == EditorInfo.IME_ACTION_SEARCH ||
                        keyCode == EditorInfo.IME_ACTION_DONE ||
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String keyword = txtSearch.getText().toString();
                    mLibraryAdapter.setFilter(keyword);
                    mLibraryAdapter.filterItems();
                    MenuItem item = bottomNavigation.getMenu().getItem(2);
                    item.setTitle(keyword);
                    dialogFragment.dismiss();
                }
                return false; // pass on to other listeners.
            }
        });

// newFragment.setTitle("Music Mate");
        //newFragment.setNegativeText(R.string.cancel);
        //newFragment.setPositiveText("UPDATE");
        //builder.onPositive(new BottomDialog.ButtonCallback() {

        /*
        BottomDialog.Builder builder = new BottomDialog.Builder(getContext());
        builder.setCancelable(true);
        builder.setNegativeText(R.string.cancel);
        builder.setPositiveText("UPDATE");
        // builder.setTitle("Song information on Musicbrainz");
        View cview = getActivity().getLayoutInflater().inflate(R.layout.view_musicbrainz_preview, null);
        TextView tTitle = cview.findViewById(R.id.title);
        tTitle.setText(recordingItem.title);
        TextView tArtist = cview.findViewById(R.id.artist);
        tArtist.setText(recordingItem.artist);
        TextView tAlbum = cview.findViewById(R.id.album);
        tAlbum.setText(recordingItem.album);
        TextView tGenre = cview.findViewById(R.id.genre);
        tGenre.setText(recordingItem.genre);
        TextView tYear = cview.findViewById(R.id.year);
        tYear.setText(recordingItem.year);
        ImageView cover = cview.findViewById(R.id.coverart);
        GlideApp.with(cview)
                .asBitmap()
                .load(recordingItem)
                .placeholder(R.drawable.progress)
                .error(R.drawable.ic_broken_image_black_24dp)
                .into(new SimpleTarget<Bitmap>(MusicMateArtwork.MAX_ALBUM_ART_SIZE, MusicMateArtwork.MAX_ALBUM_ART_SIZE) {
                    @Override
                    public void onResourceReady(Bitmap resource,Transition<? super Bitmap> transition) {
                        cover.setImageBitmap(resource);
                        File theFilePath = MediaItemProvider.getDownloadPath(recordingItem.id+".png");
                        writePNG(theFilePath, resource);
                    }
                });
        builder.setCustomView(cview);
        builder.onPositive(new BottomDialog.ButtonCallback() {
            @Override
            public void onClick(@NonNull BottomDialog dialog) {
                // set sog and cover to songs
                List<MediaItem> mediaItems = MetadataActivity.getEditItems();
                boolean singleTrack = mediaItems.size()==1;

                // MediaItemProvider.getInstance().saveArtworkToFile(mediaItems.get(0), theFilePath.getAbsolutePath());
                String artworkPath = null;
                File theFilePath = MediaItemProvider.getDownloadPath(recordingItem.id+".png");
                if(theFilePath!=null && theFilePath.exists()) {
                    artworkPath = theFilePath.getAbsolutePath();
                }

                String title = String.valueOf(tTitle.getText());
                String artist = String.valueOf(tArtist.getText());
                String album = String.valueOf(tAlbum.getText());
                String genre = String.valueOf(tGenre.getText());
                String year = String.valueOf(tYear.getText());

                for(MediaItem item:mediaItems) {
                    buildPendingTags(item, title, artist, album, genre, year, singleTrack);
                }
                MediaItemIntentService.startService(getContext(), Constants.COMMAND_SAVE,mediaItems, artworkPath);
                dialog.dismiss();
            }
        });
        builder.show(); */
    }

    private void doShowLeftMenus() {
        List<ResideMenuItem> list = mResideMenu.getMenuItems(ResideMenu.DIRECTION_LEFT);
        for(ResideMenuItem item: list) {
        }
        mResideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return mResideMenu.dispatchTouchEvent(ev);
    }

    private void setUpResideMenus() {
        // attach to current activity;
        mResideMenu = new ResideMenu(this);
        mResideMenu.setBackground(R.drawable.bg);
        mResideMenu.attachToActivity(this);
        mResideMenu.setScaleValue(0.54f);
        mResideMenu.setSwipeDirectionDisable(ResideMenu.DIRECTION_RIGHT);
        mResideMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public void onClick(MenuItem item) {
                onOptionsItemSelected(item);
                mResideMenu.closeMenu();
            }
        });

        // create left menus
       // mResideMenu.setMenuResLeft(R.menu.menu_music_mate);
        // create right menus
        mResideMenu.setMenuRes(R.menu.menu_music_collection, ResideMenu.DIRECTION_LEFT);
        List<String> formats = MediaItemProvider.getInstance().getAudioFormats();
        for(String format:formats) {
            mResideMenu.addMenuItem(FORMAT_MENU_ID, R.drawable.format_white, format, ResideMenu.DIRECTION_LEFT);
        }

        List<String> samplingRates = MediaItemProvider.getInstance().getSamplingRates();
        for(String samplingRate:samplingRates) {
            mResideMenu.addMenuItem(CODEC_MENU_ID, R.drawable.sampling_white, samplingRate, ResideMenu.DIRECTION_LEFT);
        }
    }

    private void setUpPermissions() {
        if (!PermissionUtils.IsPermissionsEnabled(this, MusicListeningService.PERMISSIONS_ALL)) {
            PermissionsDialogue.Builder alertPermissions = new PermissionsDialogue.Builder(MediaBrowserActivity.this)
                    .setCancelable(false)
                    .setMessage(getString(R.string.app_name) + " requires the following permissions to manage music")
                    .setIcon(R.drawable.ic_launcher)
                    .setRequireStorage(PermissionsDialogue.REQUIRED)
                    .setOnContinueClicked(new PermissionsDialogue.OnContinueClicked() {
                        @Override
                        public void OnClick(View view, Dialog dialog) {
                            dialog.dismiss();
                            if(externalMemoryAvailable()) {
                                setUpPermissionSAF();
                            }else {
                                MediaItemScanService.startService(getApplicationContext(),Constants.COMMAND_SCAN);
                            }
                        }
                    })
                    .setDecorView(getWindow().getDecorView())
                    .build();
            alertPermissions.show();
        }
    }

    public boolean externalMemoryAvailable() {
        if (Environment.isExternalStorageRemovable()) {
            //device support sd card. We need to check sd card availability.
            String state = Environment.getExternalStorageState();
            return state.equals(Environment.MEDIA_MOUNTED) || state.equals(
                    Environment.MEDIA_MOUNTED_READ_ONLY);
        } else {
            //device not support sd card.
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setUpPermissionSAF() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, MusicListeningService.REQUEST_CODE_SD_PERMISSION);
    }

    private int getItemPosition(String mediaPath) {
        if(StringUtils.isEmpty(mediaPath)) return -1;

        for(int i=0; i< mediaItems.size();i++) {
            if(mediaItems.get(i) != null && mediaPath.equalsIgnoreCase(mediaItems.get(i).getPath()))  {
                return i;
            }
        }
        return -1;
    }

    private boolean scrollToListening() {
        if(!displayListenningSong) return false;

        if(MusicListeningService.getInstance()==null) return false;

        if(mLibraryAdapter==null) return false;

        MediaItem item = MusicListeningService.getInstance().getListeningSong();
        if(item == null) return false;

        try {
            int position = getItemPosition(item.getPath());
            position = scrollToPosition(position, true);
            if (position < 0) return false;

            mLibraryAdapter.notifyDataSetChanged();
            SmoothScrollLinearLayoutManager layoutManager = (SmoothScrollLinearLayoutManager) mRecyclerView.getLayoutManager();
            layoutManager.smoothScrollToPosition(mRecyclerView, null, position == 0 ? position : (position - 1));
            return true;
        }catch (Exception ex) {
        }
        return false;
    }

    private boolean isOnListening() {
        if(MusicListeningService.getInstance() == null) {
            return false;
        }

        MediaItem item = MusicListeningService.getInstance().getListeningSong();

        if(item==null) return false;

        if(prevListeningSong==null) {
            prevListeningSong = item;
            return false;
        }
        if(prevListeningSong.equals(item)) {
            return true;
        }
        prevListeningSong = item;
        return false;
    }

    private int scrollToPosition(int position, boolean offset) {
        if(position>-1) {
            int positionWithOffset = position;
            if(offset) {
                positionWithOffset = position - RECYCLEVIEW_ITEM_POSITION_OFFSET;
                if (positionWithOffset < 0) {
                    positionWithOffset = 0;
                }
            }
            mRecyclerView.scrollToPosition(positionWithOffset);
        }
        return position;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // show listening on all songs mode only.
        displayListenningSong = true;
        buildMediaItems(BROWSER_MODE.ALL_SONGS,"");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(MediaItemProvider.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(operationReceiver, filter);

        filter = new IntentFilter(MusicListeningService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(playingReceiver, filter);

       // if(mLibraryAdapter!=null && !mLibraryAdapter.hasFilter()) {
       //     scrollToListening();
       // }

/*
        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
        */
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
        /*
        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        } */
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }else if(item.getItemId() == R.id.menu_all_music) {
            buildMediaItems(BROWSER_MODE.ALL_SONGS,"");
            return true;
        }else if(item.getItemId() == R.id.menu_new_collection) {
            buildMediaItems(BROWSER_MODE.NEW_SONGS,"");
            return true;
            /*
        }else if(item.getItemId() == R.id.menu_hd_collection) {
            displayPosition = 0;
            buildMediaItems(BROWSER_MODE.HD_AUDIO);
            return true;
        }else if(item.getItemId() == R.id.menu_llac_collection) {
            buildMediaItems(BROWSER_MODE.LLAC_AUDIO,"'");
            return true;
        }else if(item.getItemId() == R.id.menu_lsac_collection) {
            buildMediaItems(BROWSER_MODE.LSAC_AUDIO,"");
            return true; */
        }else if(item.getItemId() == R.id.menu_similar_title) {
            buildMediaItems(BROWSER_MODE.SIMILAR_TITLES,"");
            return true;
        } else if(item.getItemId() == FORMAT_MENU_ID) {
            buildMediaItems(BROWSER_MODE.AUDIO_FORMAT, (String) item.getTitle());
            return true;
        } else if(item.getItemId() == CODEC_MENU_ID) {
            buildMediaItems(BROWSER_MODE.SAMPLING_RATE, (String) item.getTitle());
            return true;
        }else if(item.getItemId() == R.id.menu_similar_title_artist) {
            buildMediaItems(BROWSER_MODE.SIMILAR_SONGS, "");
            return true;
        }else if(item.getItemId() == R.id.menu_settings) {
            Intent myIntent = new Intent(MediaBrowserActivity.this, SettingsActivity.class);
            startActivity(myIntent);
            return true;
        }else if(item.getItemId() == R.id.menu_load_media_items) {
            doScanMediaItems();
            return true;
        }else if(item.getItemId() == R.id.menu_about_music_mate) {
            Intent aboutIntent = new Intent(MediaBrowserActivity.this, AboutActivity.class);
            startActivity(aboutIntent);
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
    }

    protected void buildMediaItems(final BROWSER_MODE browserMode, String title) {
        mLibraryAdapter.resetAdapter();
        //searchText.setText("");
      //  showHeaderTitlePanel();

        //RxAndroid
        Observable<Boolean> observable = Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                mediaItems.clear();
                if (browserMode == BROWSER_MODE.SIMILAR_SONGS) {
                    mediaItems.addAll(MediaItemProvider.getInstance().querySimilarArtistAndTitle());
                    mLibraryAdapter.setBrowserTitle(MediaItemProvider.SIMILAR_TITLE_ARTIST);
                }else if (browserMode == BROWSER_MODE.SIMILAR_TITLES) {
                    mediaItems.addAll(MediaItemProvider.getInstance().querySimilarTitle());
                    mLibraryAdapter.setBrowserTitle(MediaItemProvider.SIMILAR_TITLE);
                } else if (browserMode == BROWSER_MODE.NEW_SONGS) {
                    mediaItems.addAll(MediaItemProvider.getInstance().queryNewMediaItems());
                    mLibraryAdapter.setBrowserTitle(MediaItemProvider.INCOMING_SONGS);
                //} else if(browserMode == BROWSER_MODE.HD_AUDIO) {
                //    mediaItems.addAll(MediaItemProvider.getInstance().loadHiResMediaItems());
                } else if(browserMode == BROWSER_MODE.LLAC_AUDIO) {
                    mediaItems.addAll(MediaItemProvider.getInstance().queryLLACMediaItems());
                    mLibraryAdapter.setBrowserTitle(MediaItemProvider.LOSSLESS_AUDIO_FORMAT);
                } else if(browserMode == BROWSER_MODE.LSAC_AUDIO) {
                    mediaItems.addAll(MediaItemProvider.getInstance().queryLSACMediaItems());
                    mLibraryAdapter.setBrowserTitle(MediaItemProvider.OTHER_AUDIO_FORMAT);
                } else if(browserMode == BROWSER_MODE.AUDIO_FORMAT) {
                    mediaItems.addAll(MediaItemProvider.getInstance().queryMediaItemsByAudioFormat(title));
                    mLibraryAdapter.setBrowserTitle(title);
                } else if(browserMode == BROWSER_MODE.SAMPLING_RATE) {
                    mediaItems.addAll(MediaItemProvider.getInstance().queryMediaItemsBySamplingRate(title));
                    mLibraryAdapter.setBrowserTitle(title);
                } else {
                    mediaItems.addAll(MediaItemProvider.getInstance().queryMediaItems());
                    mLibraryAdapter.setBrowserTitle(MediaItemProvider.ALL_SONGS);
                }
                return true;
            }
        });
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {
                // start progress
                if(mSwipeRefreshLayout!=null) {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
                if(mActionModeHelper!=null) {
                    mActionModeHelper.destroyActionModeIfCan();
                }
            }

            @Override
            public void onNext(Boolean actionResult) {
            }

            @Override
            public void onError(Throwable e) {
                if(mSwipeRefreshLayout!=null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                mLibraryAdapter.setMediaItems(browserMode, mediaItems);
                mLibraryAdapter.updateDataSet(mediaItems);
                mLibraryAdapter.notifyDataSetChanged();
                if(mSwipeRefreshLayout!=null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
                if(!mediaItems.isEmpty()) {
                  //  showSearchBox();
                }
                MenuItem item = bottomNavigation.getMenu().getItem(0);
                item.setTitle(mLibraryAdapter.getBrowserTitle());
            }
        });
    }

    private void updateHeader() {
        txtHeaderTitle1.setText(mLibraryAdapter.getBrowserTitle());
        if(mediaItems.isEmpty()) {
            txtHeaderSubtitle.setText("");
        }else {
            HeaderItem header = mediaItems.get(0).getHeader();
            if(header != null) {
                txtHeaderSubtitle.setText(header.getSubtitle());
            }
        }
        txtHeaderTitle.setText(MediaItemProvider.getInstance().getStorageInfo());
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
                buildMediaItems(mLibraryAdapter.getBrowserMode(),mLibraryAdapter.getBrowserTitle());
            }
            });
    }

    protected void setUpRecycleView() {
        // Initialize Adapter and RecyclerView
        mLibraryAdapter = new  MediaItemAdapter(mediaItems,GlideApp.with(this));
        mLibraryAdapter.addListener(this);

        mLibraryAdapter.setDisplayHeadersAtStartUp(true);
        mLibraryAdapter.setStickyHeaderElevation(4)
                .setStickyHeaders(true);

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
        RecyclerView.ItemDecoration itemDecoration = new LinearDividerItemDecoration(this, getColor(R.color.item_divider),1);
     //   RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, R.drawable.shadow_below);

        mRecyclerView.addItemDecoration(itemDecoration);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if(mFastScroller!=null) {
                            mFastScroller.hideScrollbar();
                        }
                      //  showHeaderPanel();
                    }else {
                       // hideHeaderPanel();
                        if(mFastScroller!=null) {
                            mFastScroller.showScrollbar();
                        }
                    }
                }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

            }
            });

        // Add FastScroll to the RecyclerView, after the Adapter has been attached the RecyclerView!!!
        mFastScroller = findViewById(R.id.fast_scroller);
        mFastScroller.setVisibility(View.VISIBLE);
        mFastScroller.setBubbleAndHandleColor(getColor(R.color.colorPrimary_alpha_200));
        // 0 pixel is the default value. When > 0 it mimics the fling gesture
        // OnScrollStateChangeListener remains optional
        mFastScroller.setMinimumScrollThreshold(70);
        mFastScroller.setRecyclerView(mRecyclerView);
        mFastScroller.addOnScrollStateChangeListener(new FastScroller.OnScrollStateChangeListener() {
            @Override
            public void onFastScrollerStateChange(boolean scrolling) {
                if (scrolling) {
                  //  hideSearchPanel();
                }else {
                   // showSearchPanel();
                }
            }
        });
        // Finally, assign the Fastscroller to the Adapter
        mLibraryAdapter.setFastScroller(mFastScroller);
        mLibraryAdapter.toggleFastScroller();
    }
/*
    private void hideHeaderPanel() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                vHeaderPanel.setVisibility(View.GONE);
                vSearchAction.setVisibility(View.GONE);
            }
        }, 100);
    }

    private void showHeaderPanel() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                vHeaderPanel.setVisibility(View.VISIBLE);
                vSearchAction.setVisibility(View.VISIBLE);
            }
        }, 300);
    } */

/*
    private void showHeaderSearchPanel() {
        if(vHeaderSearchPanel.getVisibility() == View.GONE) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    vHeaderTitlePanel.setVisibility(View.GONE);
                    vHeaderSearchPanel.setVisibility(View.VISIBLE);
                    btnSearchAction.setImageDrawable(UIUtils.getTintedDrawable(getApplicationContext(), R.drawable.ic_close_black_24dp, getColor(R.color.colorAccent)));

                    //btnSearchAction.setImageDrawable(getDrawable(R.drawable.ic_close_black_24dp));
                    // vHeaderTitlePanel.setVisibility(View.GONE);
                    // vHeaderSearchPanel.setVisibility(View.VISIBLE);
                    // btnSearchAction.setImageDrawable(getDrawable(R.drawable.ic_close_black_24dp));
                    UIUtils.showKeyboard(getApplicationContext(), searchText);
                }
            }, 100);
        }
    } */

/*
    private void showHeaderTitlePanel() {
        if(vHeaderSearchPanel.getVisibility() == View.VISIBLE) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    searchText.setText("");
                    mLibraryAdapter.setFilter("");
                    mLibraryAdapter.filterItems();
                    UIUtils.hideKeyboard(getApplicationContext(), searchText);
                    vHeaderSearchPanel.setVisibility(View.GONE);
                    vHeaderTitlePanel.setVisibility(View.VISIBLE);
                    btnSearchAction.setImageDrawable(UIUtils.getTintedDrawable(getApplicationContext(), R.drawable.ic_search_black_24dp,getColor(R.color.colorAccent)));
                    //btnSearchAction.setImageDrawable(getDrawable(R.drawable.ic_search_black_24dp));
                }
            }, 100);
        }
    } */

    // Define the callback for what to do when data is received
    private BroadcastReceiver operationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra(Constants.KEY_RESULT_CODE, RESULT_CANCELED);
            if (resultCode == RESULT_OK) {
                String status = intent.getStringExtra(Constants.KEY_STATUS);
                String message = intent.getStringExtra(Constants.KEY_MESSAGE);
                String mediaPath = intent.getStringExtra(Constants.KEY_MEDIA_PATH);
                String command = intent.getStringExtra(Constants.KEY_COMMAND);

                if(Constants.STATUS_SUCCESS.equalsIgnoreCase(status)) {
                   /* if (mSnackbar != null) {
                        mSnackbar.dismiss();
                        mSnackbar = null;
                    } */
                    if(Constants.COMMAND_MOVE.equalsIgnoreCase(command) ||
                            Constants.COMMAND_SAVE.equalsIgnoreCase(command)) {
                        int position = mLibraryAdapter.getItemPosition(mediaPath);
                        if(position>-1) {
                            mLibraryAdapter.notifyItemChanged(position);
                        }
                    }else if(Constants.COMMAND_DELETE.equalsIgnoreCase(command)) {
                        int position = mLibraryAdapter.getItemPosition(mediaPath);
                        if(position>-1) {
                            MediaItem item = (MediaItem) mLibraryAdapter.getItem(position);
                            if(!MediaItemProvider.isMediaFileExist(item)) {
                                mLibraryAdapter.removeItem(position);
                                MediaItemProvider.getInstance().removeFromDatabase(mediaPath);
                               // mLibraryAdapter.updateHeader();
                            }
                        }
                    }
                }else if (Constants.STATUS_START.equalsIgnoreCase(status)) {
                   /* if(mSnackbar!=null) {
                        mSnackbar.dismiss();
                        mSnackbar= null;
                    }
                    mSnackbar = Snacky.builder().setActivity(MediaBrowserActivity.this)
                            .setText(message)
                            .setDuration(Snacky.LENGTH_INDEFINITE)
                            .setMaxLines(1)
                            .info();
                    mSnackbar.show();
                    */
                    mLibraryAdapter.showNotification(message, getColor(R.color.colorPrimaryDark), 1000);
                   /*
                    if(Constants.COMMAND_MOVE.equalsIgnoreCase(command)) {
                        StyleableToast.makeText(getApplicationContext(), message, R.style.ManageSongStyles).show();
                    }
                    else if (Constants.COMMAND_SAVE.equalsIgnoreCase(command)) {
                        StyleableToast.makeText(getApplicationContext(), message, R.style.SaveSongStyles).show();
                    }else if(Constants.COMMAND_DELETE.equalsIgnoreCase(command)) {
                        StyleableToast.makeText(getApplicationContext(), message, R.style.DeleteSongStyles).show();
                    }
                    StyleableToast.makeText(getApplicationContext(), message, R.style.ScanSongStyles).show();
                    */
                } else if(Constants.STATUS_FAIL.equalsIgnoreCase(status)) {
                    /*
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
                    */
                    mLibraryAdapter.showNotification(message, getColor(R.color.menu_delete_background), 1000);
                  //  StyleableToast.makeText(getApplicationContext(), message, R.style.FailStyles).show();
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
                if(!mLibraryAdapter.hasFilter() && !isOnListening()) {
                    scrollToListening();
                }
            }
        }
    };
}
