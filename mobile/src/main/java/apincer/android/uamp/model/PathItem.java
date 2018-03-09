package apincer.android.uamp.model;

import android.view.View;
import android.widget.TextView;

import java.io.Serializable;
import java.util.List;

import apincer.android.uamp.R;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by e1022387 on 12/29/2017.
 */

public class PathItem  extends AbstractFlexibleItem<PathItem.PathItemViewHolder>
        implements Serializable {
    int id;
    String title;
    String path;
    String subtitle;

    public PathItem(int id, String title, String subtitle, String organizedPath) {
        super();
        this.id =id;
        this.title = title;
        this.subtitle = subtitle;
        this.path=organizedPath;
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof PathItem) {
            PathItem inItem = (PathItem) inObject;
            return this.id == inItem.id;
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_list_item_editor;
    }

    @Override
    public PathItem.PathItemViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new PathItem.PathItemViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, PathItem.PathItemViewHolder holder, int position, List<Object> payloads) {
        holder.titleView.setText(title);
        holder.subtitleView.setText(subtitle);
        holder.pathView.setText(path);
    }

    public class PathItemViewHolder extends FlexibleViewHolder {
        TextView titleView;
        TextView subtitleView;
        TextView pathView;
        public PathItemViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            titleView = view.findViewById(R.id.title);
            subtitleView = view.findViewById(R.id.subtitle);
            pathView = view.findViewById(R.id.path);
        }
    }
}
