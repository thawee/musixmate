package apincer.android.uamp.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItem;

import apincer.android.uamp.FileManagerService;
import apincer.android.uamp.R;
import apincer.android.uamp.utils.StringUtils;

public class MetadataSearchFragment extends Fragment {
    private WebView webView;
    private Spinner engines;
    private Button btnSubmit;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
       // int position = FragmentPagerItem.getPosition(getArguments());
       // TextView title = (TextView) view.findViewById(R.id.item_title);
       // title.setText(String.valueOf(position));
        engines = view.findViewById(R.id.search_engines);
        webView = view.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        btnSubmit = view.findViewById(R.id.search_action);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String engine = (String) engines.getSelectedItem();
                String key = FileManagerService.getEditItems().get(0).getTitle();
                String googleImageUrl = "http://www.google.com/search?tbm=isch&source=lnms&sa=X&q=" + key;
                String googleUrl = "http://www.google.com/search?source=lnms&sa=X&q=" + key;
                String mbrainzUrl = "https://musicbrainz.org/search?type=recording&limit=20&method=indexed&query="+key;
                switch (engine) {
                    case "MusicBrainz":
                        doSearch(mbrainzUrl);
                        break;
                    case "Google (Images)":
                        doSearch(googleImageUrl);
                        break;
                    default:
                        doSearch(googleUrl);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String key = FileManagerService.getEditItems().get(0).getTitle();
        //String url = "http://www.google.com/search?tbm=isch&source=lnms&sa=X&q=" + key;
        String url = "https://musicbrainz.org/search?type=recording&limit=20&method=indexed&query="+key;
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
}