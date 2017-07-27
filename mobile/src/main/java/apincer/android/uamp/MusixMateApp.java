package apincer.android.uamp;

import android.app.Application;
import android.util.Log;

import apincer.android.uamp.utils.LogHelper;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MusixMateApp extends Application {
    @Override public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
    }

    /** A tree which logs important information for crash reporting. */
    private static class CrashReportingTree extends Timber.Tree {
        @Override protected void log(int priority, String tag, String message, Throwable t) {
            LogHelper.logToFile("GLIDE", Log.getStackTraceString(t));
        }
    }
}