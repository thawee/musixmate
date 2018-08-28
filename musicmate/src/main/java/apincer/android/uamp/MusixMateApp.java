package apincer.android.uamp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.util.Log;

import com.balsikandar.crashreporter.CrashReporter;
import com.balsikandar.crashreporter.utils.CrashUtil;
import com.github.moduth.blockcanary.BlockCanary;
import com.github.moduth.blockcanary.BlockCanaryContext;
import com.github.moduth.blockcanary.internal.BlockInfo;
import com.squareup.leakcanary.LeakCanary;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import apincer.android.uamp.service.MusicListeningService;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.viewmodel.MediaItemListViewModel;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MusixMateApp extends Application {
    private static Logger jAudioTaggerLogger1 = Logger.getLogger("org.jaudiotagger.audio");
    private static Logger jAudioTaggerLogger2 = Logger.getLogger("org.jaudiotagger");

    @Override public void onCreate() {
        super.onCreate();

        CrashReporter.initialize(this, CrashUtil.getDefaultPath());

        // to detect not expected thread
/*        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
*/
        // TURN OFF log for JAudioTagger
        if (BuildConfig.DEBUG) {
            jAudioTaggerLogger1.setLevel(Level.SEVERE);
            jAudioTaggerLogger2.setLevel(Level.SEVERE);
            Timber.plant(new DebugTree());
        } else {
            jAudioTaggerLogger1.setLevel(Level.SEVERE);
            jAudioTaggerLogger2.setLevel(Level.SEVERE);
        }

        // start service
        Intent serviceIntent = new Intent(this, MusicListeningService.class);
        startService(serviceIntent);

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        BlockCanary.install(this, new BlockCanaryContext() {
            /**
             * Implement in your project.
             *
             * @return Qualifier which can specify this installation, like version + flavor.
             */
            public String provideQualifier() {
                return "unknown";
            }

            /**
             * Implement in your project.
             *
             * @return user id
             */
            public String provideUid() {
                return "uid";
            }

            /**
             * Network type
             *
             * @return {@link String} like 2G, 3G, 4G, wifi, etc.
             */
            public String provideNetworkType() {
                return "unknown";
            }

            /**
             * Config monitor duration, after this time BlockCanary will stop, use
             * with {@code BlockCanary}'s isMonitorDurationEnd
             *
             * @return monitor last duration (in hour)
             */
            public int provideMonitorDuration() {
                return -1;
            }

            /**
             * Config block threshold (in millis), dispatch over this duration is regarded as a BLOCK. You may set it
             * from performance of device.
             *
             * @return threshold in mills
             */
            public int provideBlockThreshold() {
                return 1000;
            }

            /**
             * Thread stack dump interval, use when block happens, BlockCanary will dump on main thread
             * stack according to current sample cycle.
             * <p>
             * Because the implementation mechanism of Looper, real dump interval would be longer than
             * the period specified here (especially when cpu is busier).
             * </p>
             *
             * @return dump interval (in millis)
             */
            public int provideDumpInterval() {
                return provideBlockThreshold();
            }

            /**
             * Path to save log, like "/blockcanary/", will save to sdcard if can.
             *
             * @return path of log files
             */
            public String providePath() {
                return "/blockcanary/";
            }

            /**
             * If need notification to notice block.
             *
             * @return true if need, else if not need.
             */
            public boolean displayNotification() {
                return true;
            }

            /**
             * Implement in your project, bundle files into a zip file.
             *
             * @param src  files before compress
             * @param dest files compressed
             * @return true if compression is successful
             */
            public boolean zip(File[] src, File dest) {
                return false;
            }

            /**
             * Implement in your project, bundled log files.
             *
             * @param zippedFile zipped file
             */
            public void upload(File zippedFile) {
                throw new UnsupportedOperationException();
            }


            /**
             * Packages that developer concern, by default it uses process name,
             * put high priority one in pre-order.
             *
             * @return null if simply concern only package with process name.
             */
            public List<String> concernPackages() {
                return null;
            }

            /**
             * Filter stack without any in concern package, used with @{code concernPackages}.
             *
             * @return true if filter, false it not.
             */
            public boolean filterNonConcernStack() {
                return false;
            }

            /**
             * Provide white list, entry in white list will not be shown in ui list.
             *
             * @return return null if you don't need white-list filter.
             */
            public List<String> provideWhiteList() {
                LinkedList<String> whiteList = new LinkedList<>();
                whiteList.add("org.chromium");
                return whiteList;
            }

            /**
             * Whether to delete files whose stack is in white list, used with white-list.
             *
             * @return true if delete, false it not.
             */
            public boolean deleteFilesInWhiteList() {
                return true;
            }

            /**
             * Block interceptor, developer may provide their own actions.
             */
            public void onBlock(Context context, BlockInfo blockInfo) {

            }
        }).start();
    }

    public MediaItemListViewModel getRepository() {
        return null;
    }
}