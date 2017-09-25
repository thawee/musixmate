package apincer.android.uamp.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import apincer.android.uamp.R;

public class HeaderView extends LinearLayout {
	TextView title;
	TextView subTitle;

	public HeaderView(Context context) {
		this(context, null);
	}

	public HeaderView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public HeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		title = (TextView) findViewById(R.id.header_view_title);
		subTitle = (TextView) findViewById(R.id.header_view_sub_title);
	}

	public void bindTo(CharSequence title, CharSequence subTitle) {
        if(this.title==null) {
            this.title = (TextView) findViewById(R.id.header_view_title);
        }
		hideOrSetText(this.title, title);
        if(this.subTitle==null) {
            this.subTitle = (TextView) findViewById(R.id.header_view_sub_title);
        }
		hideOrSetText(this.subTitle, subTitle);
	}

	private static void hideOrSetText(TextView tv, CharSequence text) {
        if(tv==null) return;

		if (text == null || text.equals(""))
			tv.setVisibility(GONE);
		else
			tv.setText(text);
	}

}