package apincer.android.uamp.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import apincer.android.uamp.Constants;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.LogHelper;

public class MediaItemScanService extends IntentService {
    private static final String TAG = LogHelper.makeLogTag(MediaItemScanService.class);
    private volatile Looper serviceLooper;
    private volatile MediaItemScanHandler scanHandler;
    private volatile MediaItemCleanHandler cleanHandler;

    private final class MediaItemScanHandler extends Handler {
        public MediaItemScanHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaItemProvider.getInstance().scanFromMediaStore();
        }
    }

    private final class MediaItemCleanHandler extends Handler {
        public MediaItemCleanHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaItemProvider.getInstance().cleanDatabase();
        }
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MediaItemScanService(String name) {
        super(name);
    }


    public MediaItemScanService() {
        super("MediaItemScanService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("MediaItemScanService");
        thread.start();

        serviceLooper = thread.getLooper();
        scanHandler = new MediaItemScanHandler(serviceLooper);
        cleanHandler = new MediaItemCleanHandler(serviceLooper);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String command = intent.getStringExtra(Constants.KEY_COMMAND);
        if (Constants.COMMAND_SCAN.equals(command) || Constants.COMMAND_SCAN_FULL.equals(command)) {
            Message msg = scanHandler.obtainMessage();
            msg.obj = intent;
            if(Constants.COMMAND_SCAN.equals(command)) {
                msg.arg1 = 1;
            }else {
                msg.arg1 = 0;
            }
            scanHandler.sendMessage(msg);
        }else if(Constants.COMMAND_CLEAN_DB.equals(command)) {
            Message msg = cleanHandler.obtainMessage();
            msg.obj = intent;
            cleanHandler.sendMessage(msg);
        }
  }

  public static void startService(Context context, String command) {
      Intent msgIntent = new Intent(context, MediaItemScanService.class);
      msgIntent.putExtra(Constants.KEY_COMMAND, command);
      context.startService(msgIntent);
  }
}
