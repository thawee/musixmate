package apincer.android.uamp.ui;

/**
 * Created by Administrator on 8/26/17.
 */

import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import apincer.android.uamp.R;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
        }
    }
}