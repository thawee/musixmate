package apincer.android.uamp.ui.views;


import android.content.Context;
import android.support.v7.view.CollapsibleActionView;
import android.support.v7.widget.LinearLayoutCompat;

/**
 * A widget that provides a user interface for the user to enter a search query and submit a request
 * to a search provider. Shows a list of query suggestions or results, if available, and allows the
 * user to pick a suggestion or result to launch into.
 *
 * <p class="note"><strong>Note:</strong> This class is included in the <a
 * href="{@docRoot}tools/extras/support-library.html">support library</a> for compatibility
 * with API level 7 and higher. If you're developing your app for API level 11 and higher
 * <em>only</em>, you should instead use the framework {@link android.widget.SearchView} class.</p>
 *
 * <p>
 * When the SearchView is used in an {@link android.support.v7.app.ActionBar}
 * as an action view, it's collapsed by default, so you must provide an icon for the action.
 * </p>
 * <p>
 * If you want the search field to always be visible, then call
 * {@link #setIconifiedByDefault(boolean) setIconifiedByDefault(false)}.
 * </p>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about using {@code SearchView}, read the
 * <a href="{@docRoot}guide/topics/search/index.html">Search</a> API guide.
 * Additional information about action views is also available in the <<a
 * href="{@docRoot}guide/topics/ui/actionbar.html#ActionView">Action Bar</a> API guide</p>
 * </div>
 */

public class LanguageView extends LinearLayoutCompat implements CollapsibleActionView {
    public LanguageView(Context context) {
        super(context);
    }

    @Override
    public void onActionViewExpanded() {

    }

    @Override
    public void onActionViewCollapsed() {

    }
}
