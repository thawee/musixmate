package apincer.android.uamp.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.yalantis.filter.adapter.FilterAdapter;
import com.yalantis.filter.listener.FilterListener;
import com.yalantis.filter.model.FilterModel;
import com.yalantis.filter.widget.Filter;
import com.yalantis.filter.widget.FilterItem;

import java.util.ArrayList;
import java.util.List;

import apincer.android.uamp.R;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.StringUtils;

public class MetadataSearchFragment extends Fragment {
    private WebView webView;
    private Filter mFilter;
    private String keyword;
    private List<Tag> tags = new ArrayList();

    private FilterListener<Tag> mListener = new FilterListener<Tag>() {
        @Override
        public void onFiltersSelected(ArrayList<Tag> filters) {
            keyword = "";
            for(Tag tag: filters) {
                keyword =  keyword +" " +tag.getText();
            }
            keyword =  StringUtils.trimToEmpty(keyword);
        }

        @Override
        public void onNothingSelected() {
            keyword = getDefaultTag();
            keyword =  StringUtils.trimToEmpty(keyword);
        }

        @Override
        public void onFilterSelected(Tag item) {
            keyword =  item.getText();
            keyword =  StringUtils.trimToEmpty(keyword);
        }

        @Override
        public void onFilterDeselected(Tag item) {
            keyword =  keyword.replace(" " +item.getText()," ");
            keyword =  StringUtils.trimToEmpty(keyword);
            if(StringUtils.isEmpty(keyword)) {
                onNothingSelected();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFilter = (Filter<Tag>) view.findViewById(R.id.filter);
        mFilter.setAdapter(new Adapter(buildTags()));
        mFilter.setListener(mListener);

        //the text to show when there's no selected items
        mFilter.setNoSelectedItemText(getDefaultTag());
        mFilter.build();
        keyword = getDefaultTag();

        webView = view.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        String mbrainzUrl = "https://musicbrainz.org/search?type=recording&limit=20&method=indexed&query="+keyword;
        doSearch(mbrainzUrl);
    }

    private List<? extends Tag> buildTags() {
        tags.clear();
        List<apincer.android.uamp.model.MediaItem> items = MetadataActivity.getEditItems();
        String path = "";
        if(items.size()==1) {
            MediaItem item = items.get(0);
            path = item.getPath();
            addToTag(item.getMetadata().getTitle());
            addToTag(item.getMetadata().getArtist());
            addToTag(item.getMetadata().getAlbum());
            addToTag(item.getMetadata().getAlbumArtist());
        }else {
            for (MediaItem item : items) {
                addToTag(item.getMetadata().getArtist());
                addToTag(item.getMetadata().getAlbum());
                addToTag(item.getMetadata().getAlbumArtist());
            }
        }
        if(tags.isEmpty()) {
            if(!StringUtils.isEmpty(path)) {
                tags.add(new Tag(MediaItemProvider.removeExtension(path)));
            }else {
                tags.add(new Tag("Unknown"));
            }
        }
        return tags;
    }

    private String getDefaultTag() {
        if(tags.size()>0) {
            return tags.get(0).text;
        }
        return "";
    }

    private void addToTag(String text) {
        if(StringUtils.isEmpty(text)) return;
        Tag tag = new Tag(text);
        if(!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
       // String key = MetadataActivity.getEditItems().get(0).getTitle();
        //String url = "http://www.google.com/search?tbm=isch&source=lnms&sa=X&q=" + key;
        //String url = "https://musicbrainz.org/search?type=recording&limit=20&method=indexed&query="+key;
    }

    private void doSearch(String url) {
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                WebView webView = (WebView) view;
                WebView.HitTestResult hr = webView.getHitTestResult();
                if (hr.getExtra() != null) {
                    String dum_url = hr.getExtra();
                    String[] dum_url_s = dum_url.split("\\.", 0);
                    int s_count = dum_url_s.length;
                    if (s_count > 1) {
                        String ext_url = dum_url_s[s_count - 1].toLowerCase();
                        if (ext_url.matches("jpg.*") || ext_url.matches("png.*") || ext_url.matches("gif.*") || ext_url.matches("jpeg.*") || ext_url.matches("bmp.*")) {
                            //   new ImageGetTask_s().execute(new String[]{dum_url});
                            Toast.makeText(getActivity().getBaseContext(), "select: "+ext_url, Toast.LENGTH_LONG).show();
                        } else if (!dum_url.equals(webView.getUrl())) {
                            webView.loadUrl(dum_url);
                        } else if (dum_url.equals(".*www.google.com.*") || dum_url.equals(".*gstatic.com.*")) {
                            // Toast.makeText(getActivity().getBaseContext(), Search_image44.this.getText(R.string.img_fail_g), 0).show();
                        }
                    } else if (!dum_url.equals(webView.getUrl())) {
                        webView.loadUrl(dum_url);
                    } else if (dum_url.equals(".*www.google.com.*") || dum_url.equals(".*gstatic.com.*")) {
                        //Toast.makeText(Search_image44.this.getBaseContext(), Search_image44.this.getText(R.string.img_fail_g), 0).show();
                    }
                }
                return false;
            }
        });
    }

    class Adapter extends FilterAdapter<Tag> {

        Adapter(List<? extends Tag> items) {
            super(items);
        }

        @Override
        public FilterItem createView(int position, Tag item) {
            FilterItem filterItem = new FilterItem(getActivity());
            filterItem.setStrokeColor(getActivity().getColor(R.color.grey200));
            filterItem.setTextColor(getActivity().getColor(R.color.colorPrimaryDark));
            filterItem.setCheckedTextColor(ContextCompat.getColor(getActivity(), R.color.now_playing));
            filterItem.setColor(ContextCompat.getColor(getActivity(), android.R.color.white));
            filterItem.setCheckedColor(getActivity().getColor(R.color.material_color_blue_A700));
            filterItem.setText(item.getText());
            filterItem.deselect();

            return filterItem;
        }
    }

    private class Tag implements FilterModel {
        private String text;

        Tag(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Tag) {
                text.equals(((Tag) obj).getText());
            }
            return false;
        }
    }
}