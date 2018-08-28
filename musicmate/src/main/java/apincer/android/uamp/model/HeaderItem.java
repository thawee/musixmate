package apincer.android.uamp.model;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import apincer.android.provider.StorageProvider;
import apincer.android.storage.StorageUtils;
import apincer.android.uamp.R;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.StringUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.flexibleadapter.items.IFilterable;
import eu.davidea.flexibleadapter.items.ISectionable;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * This is a header item with custom layout for section headers.
 * <p><b>Note:</b> THIS ITEM IS NOT A SCROLLABLE HEADER.</p>
 * A Section should not contain others Sections and headers are not Sectionable!
 */
public class HeaderItem
        extends AbstractHeaderItem<HeaderItem.HeaderViewHolder>
        implements IFilterable<String> {

    private String id;
    private String title;
    private String subtitle;

    public HeaderItem(String id) {
        super();
        this.id = id;
        setDraggable(true);
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof HeaderItem) {
            HeaderItem inItem = (HeaderItem) inObject;
            return this.getId().equals(inItem.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        //if(!StringUtils.isEmpty(this.subtitle)) {
        //    return this.subtitle + (getUpdates() > 0 ? " - u" + getUpdates() : "");
        //}
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    @Override
    public int getSpanSize(int spanCount, int position) {
        return spanCount;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_list_header_item;
    }

    @Override
    public HeaderViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new HeaderViewHolder(view, adapter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void bindViewHolder(FlexibleAdapter adapter, HeaderViewHolder holder, int position, List payloads) {
       /* if (payloads.size() > 0) {
            Log.d(this.getClass().getSimpleName(), "HeaderItem " + id + " Payload " + payloads);
        } else {
            holder.mTitle.setText(getTitle());
        }
        */
      // String filter = (String) adapter.getFilter(String.class);
      // if(StringUtils.isEmpty(filter)) {
      //     holder.vCollectionTitleBar.setVisibility(View.VISIBLE);
      // }else {
      //     holder.vCollectionTitleBar.setVisibility(View.GONE);
      // }

       if(StringUtils.isEmpty(getSubtitle())) {
            List<ISectionable> sectionableList = adapter.getSectionItems(this);
            int size = sectionableList.size();
            setSubtitle(size == 0 ? "Empty section" : size + " songs");
        }
        //holder.mSubtitle.setText(getTitle()+": "+getSubtitle());
        //holder.mSubtitle.setText(getTitle()+"]-> "+getSubtitle());
       // holder.mSubtitle.setText(getSubtitle());
       // holder.mCollectionTitle.setText(getTitle().toUpperCase());
        holder.mTitle.setText(getTitle());
        //updateStorage(holder);
    }

    @Deprecated
    public void updateStorage(HeaderViewHolder holder) {
        Map<String, StorageProvider.RootInfo> infos = MediaItemProvider.getInstance().getRootPaths();
        String storage = "";
        StorageUtils utils = new StorageUtils(holder.context);
        //List<String> storages = new ArrayList();
        long free = 0;
        long total = 0;
        for(StorageProvider.RootInfo info: infos.values()) {
            if(!MediaItemProvider.isDeviceStorage(info)){
                free = free+utils.getPartitionSize(info.path.getAbsolutePath(), false);
                total = total+utils.getPartitionSize(info.path.getAbsolutePath(), true);
                //long free = utils.getPartitionSize(info.path.getAbsolutePath(), false);
                //long total = utils.getPartitionSize(info.path.getAbsolutePath(), true);
               // String storageTitle = MediaItemProvider.getStorageTitle(info) + ": ";
               // storages.add(StringUtils.formatStorageSize(free));
               // storages.add(StringUtils.formatStorageSize(total));
               // storage = storage + storageTitle + StringUtils.formatStorageSize(free) + " free of " + StringUtils.formatStorageSize(total) + " | ";
            }
        }
        storage = StringUtils.formatStorageSize(free) + " free of " + StringUtils.formatStorageSize(total);

        // if(storage.indexOf("|")>0) {
       //     storage = storage.substring(0, storage.indexOf("|"));
       // }
        holder.mTitle.setText(StringUtils.trimToEmpty(storage));
    }

    @Override
    public boolean filter(String constraint) {
        return getTitle() != null && getTitle().toLowerCase().trim().contains(constraint);
    }

    static class HeaderViewHolder extends FlexibleViewHolder {
        Context context;
       // TextView mCollectionTitle;
        TextView mTitle;
       // TextView mSubtitle;
       // SearchView searchView;
       // EditText searchText;
      //  View btnSearch;
      //  View vSearchBtnBar;
       // View vSearchBar;
       // View vCollectionTitleBar;
       // MediaBrowserActivity.MediaItemAdapter mAdapter;

        HeaderViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);//True for sticky
            context = view.getContext();
          //  mCollectionTitle = view.findViewById(R.id.header_collection_title);
          //  vCollectionTitleBar = view.findViewById(R.id.header_collection_bar);
            mTitle = view.findViewById(R.id.media_header_title);
          //  mSubtitle = view.findViewById(R.id.header_subtitle);
           // searchText = view.findViewById(R.id.searchText);
           // searchView = view.findViewById(R.id.search);

           // btnSearch = view.findViewById(R.id.header_open_search);
           // vSearchBtnBar = view.findViewById(R.id.header_search_btn_bar);
          //  vSearchBar = view.findViewById(R.id.header_search_bar);
           // mAdapter = (MediaBrowserActivity.MediaItemAdapter)adapter;

            /*
            vCollectionTitleBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if( vSearchBar.getVisibility() == View.VISIBLE) {
                        vSearchBar.setVisibility(View.GONE);
                        searchText.setText("");
                        mAdapter.setFilter("");
                        mAdapter.filterItems();
                        UIUtils.hideKeyboard(context, searchText);
                        vSearchBtnBar.setVisibility(View.VISIBLE);
                    }
                }
            }); */

            /*
            btnSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vSearchBtnBar.setVisibility(View.GONE);
                    //searchView.onActionViewExpanded();
                    vSearchBar.setVisibility(View.VISIBLE);
                    UIUtils.showKeyboard(context, searchText);
                }
            }); */
/*
            searchText.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() < 1 || start >= s.length() || start < 0) {
                        return;
                    }

                    // If it was Enter
                    if (s.subSequence(start, start + 1).toString().equalsIgnoreCase("\n")) {
                        mAdapter.setFilter(String.valueOf(s));
                        mAdapter.filterItems();
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
                        mAdapter.setFilter(searchText.getText().toString());
                        mAdapter.filterItems();
                    }
                    return false; // pass on to other listeners.
                }
            }); */
/*
            searchText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int DRAWABLE_LEFT = 0;
                    final int DRAWABLE_TOP = 1;
                    final int DRAWABLE_RIGHT = 2;
                    final int DRAWABLE_BOTTOM = 3;

                    if(event.getAction() == MotionEvent.ACTION_UP) {
                        if(event.getRawX() >= (searchText.getRight() - searchText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                            // your action here
                            UIUtils.hideKeyboard(context, searchText);
                            searchText.setText("");
                            //mAdapter.setFilter("");
                            //mAdapter.filterItems();
                            vSearchBtnBar.setVisibility(View.VISIBLE);
                            vSearchBar.setVisibility(View.GONE);
                            return true;
                        }
                    }
                    return false; // pass on to other listeners.
                }
            }); */

            /*
            searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    mAdapter.setFilter(String.valueOf(query));
                    mAdapter.filterItems();
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if(newText.isEmpty()) {
                        mAdapter.setFilter(String.valueOf(newText));
                        mAdapter.filterItems();
                    }else if(newText.length()>=3) {
                        mAdapter.setFilter(String.valueOf(newText));
                        mAdapter.filterItems();
                    }else {
                        return true;
                    }
                    return false;
                }
            }); */

            /*
            if(mAdapter.hasFilter()) {
                String keyword = (String)mAdapter.getFilter(String.class);
                searchText.setText(keyword);
                //searchView.setQuery(keyword, false);
                vSearchBar.setVisibility(View.VISIBLE);
                vSearchBtnBar.setVisibility(View.GONE);
            }else {
                vSearchBar.setVisibility(View.GONE);
                vSearchBtnBar.setVisibility(View.VISIBLE);
            } */

            // Support for StaggeredGridLayoutManager
            setFullSpan(true);
        }

        @Override
        public String toString() {
            return super.toString() + " " + mTitle.getText();
        }
    }

    @Override
    public String toString() {
        return "HeaderItem[id=" + id +
                ", title=" + title + "]";
    }

}