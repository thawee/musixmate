package apincer.android.uamp.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.common.FlexibleItemDecoration;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.helpers.ActionModeHelper;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.flipview.FlipView;

/**
 * A placeholder fragment containing a simple view.
 */
public class BrowserViewPagerFragment extends Fragment {

	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_TAB_TYPE = "tab_type";
	private static final String TAG = LogHelper.makeLogTag(BrowserViewPagerFragment.class);

    private List<MediaItem> mediaItems = new ArrayList<>();

    protected OnFragmentInteractionListener mListener;
	private MediaTag.MediaTypes mMediaType;
	private BrowserFlexibleAdapter mAdapter;
	private RecyclerView mRecyclerView;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private MediaProvider mProvider;

	// Title
	private String mTitle;

    //
    private int selectionMode = SelectableAdapter.Mode.SINGLE;
    private ActionModeHelper mActionModeHelper;

	// listening FAB
	//private FrameLayout fraListeningFloat;
	private FloatingActionButton fabListeningAction;
	//private LinearLayout linFabListeningAction;
	//private TextView lblFabListeningAction;

	//listening receiver
	protected String listenTitle = "";
	protected String listenArtist = "";
    protected String listenAlbum = "";
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			listenTitle = intent.getStringExtra("title");
			listenArtist = intent.getStringExtra("artist");
            listenAlbum = intent.getStringExtra("album");
			showFloatingActionBar();
		}
	};

	public BrowserViewPagerFragment() {
	}

	/**
	 * Returns a new instance of this fragment for the given section
	 * number.
	 */
	public static BrowserViewPagerFragment newInstance(MediaTag.MediaTypes tabType) {
		BrowserViewPagerFragment fragment = new BrowserViewPagerFragment();
        fragment.setPageTitle(tabType.name());
		Bundle args = new Bundle();
		args.putString(ARG_TAB_TYPE, tabType.name());
		fragment.setArguments(args);
		return fragment;
	}

    private void setPageTitle(String name) {
        mTitle = name;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mProvider = new MediaProvider(getContext());
		if (getArguments() != null) {
			String name = getArguments().getString(ARG_TAB_TYPE);
			mMediaType = MediaTag.MediaTypes.valueOf(name);
            mTitle = name;
			if(mMediaType==null) {
				mMediaType = MediaTag.MediaTypes.SONGS;
                mTitle = mMediaType.name();
			}
			//mTitle = mMediaType.name();

			Log.d(TAG, "Creating new fragment for media type " + mMediaType);
		}

		// Contribution for specific action buttons in the Toolbar
		setHasOptionsMenu(true);

		// start listener
		IntentFilter iF = new IntentFilter();
		iF.addAction(MusicService.LISTENING_INTENT);
		LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver,iF);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_view_pager, container, false);

		//FAB listening
		fabListeningAction = (FloatingActionButton) rootView.findViewById(R.id.fabListeningAction);
		ViewCompat.animate(fabListeningAction).scaleX(0f).scaleY(0f).alpha(0f).start();
		// listening action
		fabListeningAction.setOnClickListener(new View.OnClickListener() {
		    @Override
		    public void onClick(View view) {
			if(!mAdapter.setListeningTitle(listenTitle, listenArtist,listenAlbum)) {
			    String message = getString(R.string.alert_listening_not_found, listenTitle);
			    Snackbar.make(getActivity().findViewById(R.id.main_view), message, Snackbar.LENGTH_SHORT).show();
                	}
		    }
		});

		showFloatingActionBar();
		return rootView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Settings for FlipView
		FlipView.resetLayoutAnimationDelay(true, 1000L);

        // Initialize RecyclerView
		initializeRecyclerView();

        initializeSwipeToRefresh();

		// Settings for FlipView
		FlipView.stopLayoutAnimation();
	}

	@Override
	public void onAttach( Context context) {
		super.onAttach(context);
		// If used on an activity that doesn't implement MediaFragmentListener, it
		// will throw an exception as expected:
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
	}

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

	// Listing FAB
    public void showFloatingActionBar() {
        if (mAdapter!=null && mAdapter.hasSearchText()) return; // not show on searching

	    if(!StringUtils.isEmpty(listenTitle) && mListener!=null) {
            mListener.setListeningTitle(listenTitle, listenArtist);
	    //lblFabListeningAction.setText(StringUtils.truncate(listenTitle, 40));
	    //linFabListeningAction.setVisibility(View.VISIBLE);
	        if(mMediaType == MediaTag.MediaTypes.SONGS) {
		    ViewCompat.animate(fabListeningAction)
				.scaleX(1f).scaleY(1f)
				.alpha(1f).setDuration(200)
				.setStartDelay(300L)
				.start();
	        }
	    }
    }

    public void hideFloatingActionBar() {
	//linFabListeningAction.setVisibility(View.INVISIBLE);
        ViewCompat.animate(fabListeningAction)
			.scaleX(0f).scaleY(0f)
			.alpha(0f).setDuration(100)
			.start();
    }

    private void initializeRecyclerView() {
	// Initialize Adapter and RecyclerView
	mAdapter = new BrowserFlexibleAdapter(loadMediaItems(false), getActivity());
	mAdapter.setListeningMode(mMediaType == MediaTag.MediaTypes.SONGS);
		//
		mAdapter.setAnimationOnScrolling(true);
		mAdapter.setAnimationOnReverseScrolling(true);

	// Experimenting NEW features (v5.0.0)
     //   mAdapter.setOnlyEntryAnimation(true)
      //          .setAnimationInterpolator(new DecelerateInterpolator())
      //          .setAnimationInitialDelay(500L)
      //          .setAnimationDelay(70L);

	mRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
        mRecyclerView.setItemViewCacheSize(0); //Setting ViewCache to 0 (default=2) will animate items better while scrolling down+up with LinearLayout
        mRecyclerView.setWillNotCacheDrawing(true);
        mRecyclerView.setLayoutManager(new SmoothScrollLinearLayoutManager(getActivity()));
	mRecyclerView.setAdapter(mAdapter);
	mRecyclerView.setHasFixedSize(true); //Size of RV will not change
	// NOTE: Use default item animator 'canReuseUpdatedViewHolder()' will return true if
	// a Payload is provided. FlexibleAdapter is actually sending Payloads onItemChange.
	mRecyclerView.setItemAnimator(new DefaultItemAnimator());
	// Divider item decorator with DrawOver enabled
	FlexibleItemDecoration itemDecoration = new FlexibleItemDecoration(getActivity());
	mRecyclerView.addItemDecoration(itemDecoration.withDivider(R.drawable.divider).withDrawOver(true));

	mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
		@Override
		public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
			super.onScrollStateChanged(recyclerView, newState);
			if(newState == RecyclerView.SCROLL_STATE_IDLE) {
				showFloatingActionBar();
			}else {
				hideFloatingActionBar();
			}
		}
	});

	// Add FastScroll to the RecyclerView, after the Adapter has been attached the RecyclerView!!!
	FastScroller fastScroller = (FastScroller) getView().findViewById(R.id.fast_scroller);
	fastScroller.setVisibility(View.VISIBLE);
	// Finally, assign the Fastscroller to the Adapter
	mAdapter.setFastScroller(fastScroller);
        //mAdapter.setLongPressDragEnabled(true) //Enable long press to drag items
         //.setHandleDragEnabled(true) //Enable handle drag (handle view must be set in the VH)
     //mAdapter.setSwipeEnabled(true); //Enable swipe items
     mAdapter.setDisplayHeadersAtStartUp(true); //Show Headers at startUp!
        mAdapter.setStickyHeaders(true); //Make headers sticky (headers need to be shown)!
      mAdapter.toggleFastScroller();

	mSwipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipeRefreshLayout);
	mSwipeRefreshLayout.setEnabled(true);

	mListener.onFragmentChange(this, mSwipeRefreshLayout, mRecyclerView, selectionMode);

	// Sticky Headers
	mAdapter.setDisplayHeadersAtStartUp(true).setStickyHeaders(true);
    }

    private void initializeSwipeToRefresh() {
        // Swipe down to force synchronize
        mSwipeRefreshLayout.setDistanceToTriggerSync(390);
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_purple, android.R.color.holo_blue_light,
                android.R.color.holo_green_light, android.R.color.holo_orange_light);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Passing true as parameter we always animate the changes between the old and the new data set
                mSwipeRefreshLayout.setRefreshing(true);
                mAdapter.updateDataSet(loadMediaItems(true), true);
                //mActionModeHelper.destroyActionModeIfCan();
                mSwipeRefreshLayout.setRefreshing(false);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Handling RecyclerView when empty.
     * <p><b>Note:</b> The order, how the 3 Views (RecyclerView, EmptyView, FastScroller)
     * are placed in the Layout, is important!</p>
     */
    public void onUpdateEmptyView(int size) {
        LogHelper.d(TAG, "onUpdateEmptyView size=" + size);
        FastScroller fastScroller = (FastScroller) getView().findViewById(R.id.fast_scroller);
        View emptyView = getView().findViewById(R.id.empty_view);
        TextView emptyText = (TextView) getView().findViewById(R.id.empty_text);
        if (emptyText != null)
            emptyText.setText(getString(R.string.no_items));
        if (size > 0) {
            fastScroller.setVisibility(View.VISIBLE);
            if(mSwipeRefreshLayout!=null) {
                // Now we call setRefreshing(false) to signal refresh has finished
                mSwipeRefreshLayout.setRefreshing(false);
            }
            emptyView.setAlpha(0);
        } else {
            emptyView.setAlpha(0);
            ViewCompat.animate(getView().findViewById(R.id.empty_view)).alpha(1);
            fastScroller.setVisibility(View.GONE);
        }
        if (mAdapter != null) {
            String message = (mAdapter.hasSearchText() ? "Filtered " : "Refreshed ");
            message += size + " items in " + mAdapter.getTime() + "ms";
            Snackbar.make(getActivity().findViewById(R.id.main_view), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    /* ====================================
 * OPTION MENU PREPARATION & MANAGEMENT
 * ==================================== */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	// MediaItem
	public List<MediaItem> loadMediaItems(final boolean forceReload) {
        final List mediaItemList = new ArrayList();
        PermissionManager permissionManager = PermissionManager.getInstance((BrowserViewPagerActivity)getActivity());
        List<String> permissions = Arrays.asList(BrowserViewPagerActivity.PERMISSIONS_ALL);
        permissionManager.checkPermissions(permissions, new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                switch (mMediaType) {
                    case SONGS:
                        //all songs
                        mediaItemList.addAll(mProvider.getAllSongList(forceReload));
                        break;
                    //case ARTIST:
                        // artist
                     //   mediaItemList.addAll(mProvider.getSongByArtist(getContext()));
                     //   break;
                    case FILES:
                        // files
                        mediaItemList.addAll(mProvider.getFoldersForSongs(getContext(),null));
                        break;
                    case SIMILARITY:
                        // similarity
                        mediaItemList.addAll(mProvider.getSimilarTitles(forceReload));
                        break;
                    default:
                        // all songs
                        mediaItemList.addAll(mProvider.getAllSongList(forceReload));
                }
                if(mActionModeHelper!=null) {
					mActionModeHelper.destroyActionModeIfCan();
            	}
            }


            @Override
            public void onPermissionDenied() {
                //return mediaItemList;
            }
        });

        mediaItems.clear();
        mediaItems.addAll(mediaItemList);
		return mediaItemList;
	}

	public String getTitle() {
		return mTitle;
	}

    public int getContextMenuResId() {
        //default Menu Context is returned
        return R.menu.menu_context;
    }

    public void reloadMediaItems(boolean forceReload) {
        // Passing true as parameter we always animate the changes between the old and the new data set
	mSwipeRefreshLayout.setRefreshing(true);
	mAdapter.updateDataSet(loadMediaItems(forceReload), true);
	//mActionModeHelper.destroyActionModeIfCan();
	mAdapter.notifyDataSetChanged();
	mSwipeRefreshLayout.setRefreshing(false);
    }

    public void setActionModeHelper(ActionModeHelper mActionModeHelper) {
        this.mActionModeHelper = mActionModeHelper;
    }

    public boolean filterItems(String newText) {
        if (mAdapter.hasNewSearchText(newText)) {
            LogHelper.d(TAG, "onQueryTextChange newText: " + newText);
            mAdapter.setSearchText(newText);
            // Fill and Filter mItems with your custom list and automatically animate the changes
            mAdapter.filterItems(mediaItems, 100);
        }
        // Disable SwipeRefresh if search is active!!
        if(StringUtils.isEmpty(newText)) {
            mSwipeRefreshLayout.setEnabled(true);
            showFloatingActionBar();
		}else {
            mSwipeRefreshLayout.setEnabled(false);
            hideFloatingActionBar();
		}

        return true;
    }

    public void onPageSelected() {
		if(mListener!=null) {
			mListener.onFragmentChange(this, mSwipeRefreshLayout, mRecyclerView, selectionMode);
		}
    }

    // Internal class
	public class BrowserFlexibleAdapter extends FlexibleAdapter<MediaItem> {
		private BrowserViewPagerActivity activity;
		private boolean listeningMode;

		public BrowserFlexibleAdapter(List<MediaItem> items, Object listeners) {
			//stableIds ? true = Items implement hashCode() so they can have stableIds!
			super(items, listeners, true);
			if(listeners instanceof BrowserViewPagerActivity) {
                this.activity = (BrowserViewPagerActivity) listeners;
            }
	}

	@Override
	public void onFastScrollerStateChange(boolean scrolling) {
		if (scrolling)  {
			hideFloatingActionBar();
		} else {
			showFloatingActionBar();
		}
	}

	@Override
	public String onCreateBubbleText(int position) {
            IFlexible iFlexible = getItem(position);
            if (iFlexible == null) return "";
            String text = iFlexible.toString();
            if (mMediaType == MediaTag.MediaTypes.FILES) {
                if (iFlexible instanceof MediaItem) {
                    // get header for artist
                    if (((MediaItem) iFlexible).getHeader() != null) {
                        // show full header
                        return (((MediaItem) iFlexible).getHeader().getTitle());
                    }
                } else {
                    return text.trim();
                }
            }
	    return StringUtils.getFirstWord(text);
        }

	@Override
	protected void onPostUpdate() {
		super.onPostUpdate();
		onUpdateEmptyView(mAdapter.getItemCount());
	}

	public boolean setListeningTitle(String newTitle, String newArtist, String newAlbum) {
		listenTitle = newTitle;
		listenArtist = newArtist;
        listenAlbum = newAlbum;
        mListener.setListeningTitle(listenTitle,listenArtist);
		int position = scrollToPositionByTitle(8);
		if(position<0) return false;

		notifyDataSetChanged();
		SmoothScrollLinearLayoutManager layoutManager = (SmoothScrollLinearLayoutManager) mRecyclerView.getLayoutManager();
		layoutManager.smoothScrollToPosition(mRecyclerView,null,position==0?position:(position-1));
            return true;
	}

	private int scrollToPositionByTitle(int offset) {
		int position = -1;
		if(StringUtils.isEmpty(listenTitle)) return position;
		int count = getItemCount();
		for (int i = 0; i < count; i++) {
			IFlexible flex = getItem(i);
			if(flex instanceof MediaItem) {
				MediaItem item = (MediaItem) flex;
				if (StringUtils.compare(item.getTitle(), listenTitle) && StringUtils.compare(item.getAlbum(), listenAlbum) && StringUtils.compare(item.getArtist(), listenArtist)) {
					position = i;
					break;
				}
			}
		}
		if(position>=0) {
			int positionWithOffset = position - offset;
			if (positionWithOffset < 0) {
				positionWithOffset = 0;
			}
			mRecyclerView.scrollToPosition(positionWithOffset);
		}

		return position;
	}

	public boolean isListeningTitle(String title, String artist,String album) {
	    if(listeningMode) {
		if (StringUtils.isEmpty(listenTitle)) return false;
	     	return StringUtils.compare(title, listenTitle) && StringUtils.compare(artist, listenArtist) && StringUtils.compare(album, listenAlbum);
	    }
	    return false;
        }

        public MediaItem getMediaItemById(String id) {
            int count = getItemCount();
            for (int i = 0; i <= count; i++) {
                IFlexible flex = getItem(i);
                if(flex instanceof MediaItem) {
                    MediaItem item = (MediaItem) flex;
                    if (item != null && id.equals(item.getId())) {
                        return item;
                    }
                }
            }
            return null;
        }

        public BrowserViewPagerActivity getActivity() {
            return activity;
        }


	public void setListeningMode(boolean listeningMode) {
	    this.listeningMode = listeningMode;
	}

        public MediaItem loadMediaItemFromMediaStore(BrowserViewPagerActivity browserViewPagerActivity, String path) {
            return mProvider.loadMediaItemFromMediaStore(browserViewPagerActivity, path);
        }

        public String getDisplayPath(String path) {
            return mProvider.getDisplayPath(path);
        }
    }
}
