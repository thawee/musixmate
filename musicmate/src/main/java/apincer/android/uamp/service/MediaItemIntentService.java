package apincer.android.uamp.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apincer.android.uamp.Constants;
import apincer.android.uamp.jaudiotagger.MusicMateArtwork;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

public class MediaItemIntentService extends IntentService {
    private static final String TAG = LogHelper.makeLogTag(MediaItemIntentService.class);
    private static Map<String, List<MediaItem>> itemList = new HashMap<>();
    private volatile Looper serviceLooper;
    private volatile MediaItemDeleteHandler deleteHandler;
    private volatile MediaItemSaveHandler saveHandler;
    private volatile MediaItemManageHandler manageHandler;
    private Artwork artwork = null;

    private final class MediaItemDeleteHandler extends Handler {
        public MediaItemDeleteHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
             MediaItem item = (MediaItem) msg.obj;
            MediaItemProvider.getInstance().deleteMediaItem(item);
        }
    }


    private final class MediaItemSaveHandler extends Handler {
        public MediaItemSaveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaItem item = (MediaItem) msg.obj;
            MediaItemProvider.getInstance().saveMediaItem(item, artwork);
        }
    }


    private final class MediaItemManageHandler extends Handler {
        public MediaItemManageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaItem item = (MediaItem) msg.obj;
            MediaItemProvider.getInstance().manageMediaItem(item);
        }
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MediaItemIntentService(String name) {
        super(name);
    }

    public MediaItemIntentService() {
        super("MediaItemIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("MediaItemIntentService");
        thread.start();

        serviceLooper = thread.getLooper();
        deleteHandler = new MediaItemDeleteHandler(serviceLooper);
        manageHandler = new MediaItemManageHandler(serviceLooper);
        saveHandler = new MediaItemSaveHandler(serviceLooper);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String command = intent.getStringExtra(Constants.KEY_COMMAND);
        List<MediaItem> items = itemList.get(command);
        if (Constants.COMMAND_DELETE.equals(command) && items !=null) {
            for(MediaItem item: items) {
                Message msg = deleteHandler.obtainMessage();
                msg.obj = item;
                deleteHandler.sendMessage(msg);
            }
        }else if (Constants.COMMAND_SAVE.equals(command) && items !=null) {
            artwork = null;
            String pendingArtworkPath = intent.getStringExtra(Constants.KEY_COVER_ART_PATH);
            if(!StringUtils.isEmpty(pendingArtworkPath)) {
                File artworkFile = new File(pendingArtworkPath);
                if(artworkFile.exists()) {
                    try {
                        artwork = MusicMateArtwork.createArtworkFromFile(artworkFile);
                    } catch (IOException e) {
                        LogHelper.e(TAG, e);
                    }
                }
            }
            for(MediaItem item: items) {
                Message msg = saveHandler.obtainMessage();
                msg.obj = item;
                saveHandler.sendMessage(msg);
            }
        }else if (Constants.COMMAND_MOVE.equals(command) && items !=null) {
            for(MediaItem item: items) {
                Message msg = manageHandler.obtainMessage();
                msg.obj = item;
                manageHandler.sendMessage(msg);
            }
        }
  }

  public static void startService(Context context, String command, List<MediaItem> items) {
      Intent msgIntent = new Intent(context, MediaItemIntentService.class);
      msgIntent.putExtra(Constants.KEY_COMMAND, command);
      if(items!=null) {
          itemList.put(command, items);
      }
      context.startService(msgIntent);
  }

  public static void startService(Context context, String command, List<MediaItem> items, String artworkPath) {
      Intent msgIntent = new Intent(context, MediaItemIntentService.class);
      msgIntent.putExtra(Constants.KEY_COMMAND, command);
      if(artworkPath != null) {
          msgIntent.putExtra(Constants.KEY_COVER_ART_PATH, artworkPath);
      }
      if(items!=null) {
          itemList.put(command, items);
      }
      context.startService(msgIntent);
  }
}
