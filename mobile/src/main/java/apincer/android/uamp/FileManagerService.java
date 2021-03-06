package apincer.android.uamp;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.provider.MediaItemProvider;

/**
 * Created by e1022387 on 1/8/2018.
 */

public class FileManagerService extends IntentService {
    public static final String ACTION = "com.apincer.uamp.FileManagerService";
    private static List<MediaItem> deleteItems = new ArrayList<>();
    private static List<MediaItem> moveItems = new ArrayList<>();
    private static List<MediaItem> saveItems = new ArrayList<>();
    public static List<MediaItem> editItems = new ArrayList<>();

    public static void addToDelete(MediaItem item) {
        if(deleteItems==null) {
            deleteItems = new ArrayList<>();
        }
        deleteItems.add(item);
    }
    public static void addToEdit(List<MediaItem> items) {
        if(editItems==null) {
            editItems = new ArrayList<>();
        }else {
            editItems.clear();
        }
        editItems.addAll(items);
    }
    public static void addToMove(MediaItem item) {
        if(moveItems==null) {
            moveItems = new ArrayList<>();
        }
        moveItems.add(item);
    }
    public static void addToSave(MediaItem item) {
        if(saveItems==null) {
            saveItems = new ArrayList<>();
        }
        saveItems.add(item);
    }
    public static List<MediaItem> getEditItems() {
        return editItems;
    }

    public FileManagerService() {
        super(FileManagerService.class.getName());
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FileManagerService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String command = intent.getStringExtra("command");
        if(apincer.android.uamp.Constants.COMMAND_DELETE.equalsIgnoreCase(command)) {
            if(!deleteItems.isEmpty()) {
                int index=0;
                for(MediaItem item: deleteItems) {
                    deleteFile(item,++index);
                }
                deleteItems.clear();
            }
        }else if(apincer.android.uamp.Constants.COMMAND_SAVE.equalsIgnoreCase(command)) {
            if(!saveItems.isEmpty()) {
                int index=0;
                for(MediaItem item: saveItems) {
                    saveFile(item,++index);
                }
                saveItems.clear();
            }
        }else if(apincer.android.uamp.Constants.COMMAND_MOVE.equalsIgnoreCase(command)) {
            if(!moveItems.isEmpty()) {
                int index = 0;
                for(MediaItem item: moveItems) {
                    moveFile(item,++index);
                }
                moveItems.clear();
            }
        }
    }

    protected void showNotification(final int total, final MediaItem item, final int index, final String command, final String status, final String message){
        // Fire the broadcast with intent packaged
        // Construct our Intent specifying the Service
        Intent intent = new Intent(ACTION);
        // Add extras to the bundle
        intent.putExtra("resultCode", Activity.RESULT_OK);
        intent.putExtra("command", command);
        intent.putExtra("status", status);
        intent.putExtra("message", message);
        intent.putExtra("totalItems", total);
        intent.putExtra("currentItem", index); // index start from 0
        if(item !=null) {
            intent.putExtra("mediaId", item.getId());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void deleteFile(MediaItem item, int index) {
        boolean status = false;
        String indexStr = String.valueOf(index);
        String totalStr = String.valueOf(deleteItems.size());
        try {
            String msg = getString(R.string.alert_delete_start, item.getTitle());
            if(deleteItems.size()>1) {
                msg = getString(R.string.alert_many, indexStr, totalStr, msg);
            }
            showNotification(deleteItems.size(),item, index, apincer.android.uamp.Constants.COMMAND_DELETE, "start", msg);
            status = MediaItemProvider.getInstance().deleteMediaFile(item.getPath());
            playNextSong(item);
        } catch (Exception|OutOfMemoryError ex) {
            status = false;
        }

        String msg = status?getString(R.string.alert_delete_success, item.getTitle()):getString(R.string.alert_delete_fail, item.getTitle());
        String statusStr = status?"success":"fail";
        if(deleteItems.size()>1) {
            msg = getString(R.string.alert_many, indexStr, totalStr, msg);
        }
        showNotification(deleteItems.size(),item, index, apincer.android.uamp.Constants.COMMAND_DELETE, statusStr, msg);
    }

    private void moveFile(MediaItem item,int index) {
        boolean status = false;
        String indexStr = String.valueOf(index);
        String totalStr = String.valueOf(moveItems.size());
        try {
            String msg = getString(R.string.alert_organize_start, item.getTitle());
            if(moveItems.size()>1) {
                msg = getString(R.string.alert_many, indexStr, totalStr, msg);
            }
            showNotification(moveItems.size(),item, index, apincer.android.uamp.Constants.COMMAND_MOVE, "start", msg);
            if(MediaItemProvider.getInstance().moveToManagedDirectory(item)) {
                playNextSong(item);
            }
            status = true;
        }catch(Exception|OutOfMemoryError ex) {
            status = false;
        }

        String msg = status?getString(R.string.alert_organize_success, item.getTitle()):getString(R.string.alert_organize_fail, item.getTitle());
        String statusStr = status?"success":"fail";
        if(moveItems.size()>1) {
            msg = getString(R.string.alert_many, indexStr, totalStr, msg);
        }
        showNotification(moveItems.size(), item, index, apincer.android.uamp.Constants.COMMAND_MOVE, statusStr, msg);
    }

    private void saveFile(MediaItem item,int index) {
        boolean status = false;
        String indexStr = String.valueOf(index);
        String totalStr = String.valueOf(saveItems.size());
        try {
            String msg = getString(R.string.alert_write_tag_start, item.getTitle());
            if(saveItems.size()>1) {
                msg = getString(R.string.alert_many, indexStr, totalStr, msg);
            }
            showNotification(saveItems.size(), item, index, apincer.android.uamp.Constants.COMMAND_SAVE, "start", msg);
            MediaItemProvider provider = MediaItemProvider.getInstance();

            // call new API
            if(provider.saveMetadata(item)) {
                provider.readMetadata(item.getMetadata());
            }
            provider.saveMediaArtwork(item);
            status = true;
        }catch (Exception|OutOfMemoryError ex) {
            status = false;
        }

        String msg = status?getString(R.string.alert_write_tag_success, item.getTitle()):getString(R.string.alert_write_tag_fail, item.getTitle());
        String statusStr = status?"success":"fail";
        if(saveItems.size()>1) {
            msg = getString(R.string.alert_many, indexStr, totalStr, msg);
        }
        showNotification(saveItems.size(), item, index, apincer.android.uamp.Constants.COMMAND_SAVE, statusStr, msg);
    }

    private void playNextSong(MediaItem item) {
        MusicService service = MusicService.getRunningService();
        if(service!=null && item.equals(service.getListeningSong())) {
            service.playNextSong();
        }
    }
}
