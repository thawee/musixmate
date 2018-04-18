package apincer.android.uamp;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.MimeTypeMap;

import org.jaudiotagger.audio.SupportedFileFormat;

import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.provider.MediaItemProvider;
import stream.custompermissionsdialogue.utils.PermissionUtils;

/**
 * Created by e1022387 on 1/8/2018.
 */

public class MediaItemService extends IntentService {
    public static final String ACTION = "com.apincer.uamp.MediaItemService";
    private Context mContext;

    public static void startService(Context context, String command) {
        // Construct our Intent specifying the Service
        Intent intent = new Intent(context, MediaItemService.class);
        // Add extras to the bundle
        intent.putExtra("command", command);
        // Start the service
        context.startService(intent);
    }

    public MediaItemService() {
        super(MediaItemService.class.getName());
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MediaItemService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
        this.mContext = getApplicationContext();

       // IntentFilter filter = new IntentFilter(FileManagerService.ACTION);
      //  LocalBroadcastManager.getInstance(this).registerReceiver(operationReceiver, filter);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String command = intent.getStringExtra("command");
        if(!PermissionUtils.IsPermissionsEnabled(getApplicationContext(), MusicService.PERMISSIONS_STORAGE)) {
            return ;
        }
        if("load".equalsIgnoreCase(command) ) {
            scanMetadataFromMediaStore();
            // loadMediaItems();
            // send broadcast load completed
            sendBroadcast("load",-1,"success",null);
           // for(MediaItem item: pendingItems) {
           //     MediaItemProvider.getInstance().readMetadata(item, null);
           //     updateAlbumArtist(item.getTag().getAlbumArtist());
           // }
           // pendingItems.clear();
           // sendBroadcast("read",-1,"success",null);
        }
    }

    public static String buildMediaSelection(long dateModified){
        StringBuilder selection=new StringBuilder();
        for(SupportedFileFormat format : SupportedFileFormat.values()) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format.name().toLowerCase());
            selection.append("(" + MediaStore.Files.FileColumns.MIME_TYPE + "=='"+ mimeType+ "') OR ");
        }
        selection.append(" ("+MediaStore.Audio.Media.IS_MUSIC + " = 1 ) ");
        if(dateModified>0) {
            selection.append(" AND ("+MediaStore.Audio.Media.DATE_MODIFIED + " > "+dateModified+" ) ");
        }
        return selection.substring(0,selection.lastIndexOf(")") + 1);
    }
/*
    @Deprecated
    private boolean loadMediaItems() {
        mMediaItems.clear();
        mMediaArtists.clear();
        mMediaAlbums.clear();
        mMediaAlbumArtists.clear();
        ContentResolver mContentResolver = mContext.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
       // String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1";
        String selection = buildMediaSelection(-1);
        Cursor cur = mContentResolver.query(uri, null, selection, null, "LOWER ("+MediaStore.Audio.Media.TITLE+") ASC");
        if (cur == null) {
            // Query failed...
            return false;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            cur.close();
            return false;
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        int index = 0;
        do {
            int id = cur.getInt(idColumn);
            String mediaPath =  cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            mMediaItems.add(buildMediaItem(id,mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration,index++));
            updateArtist(mediaArtist);
            updateAlbum(mediaAlbum);
        } while (cur.moveToNext());

        cur.close();

        return true;
    } */

    private boolean scanMetadataFromMediaStore() {
        ContentResolver mContentResolver = mContext.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        // String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1";
        String selection = buildMediaSelection(MediaItemProvider.getInstance().getLastestModifiedFromDatabase());
        Cursor cur = mContentResolver.query(uri, null, selection, null, "LOWER ("+MediaStore.Audio.Media.TITLE+") ASC");
        if (cur == null) {
            // Query failed...
            return false;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            cur.close();
            return false;
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        do {
            int id = cur.getInt(idColumn);
            String mediaPath =  cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            MediaMetadata metadata = buildMediaMetadata(id,mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);
            if(metadata!=null) {
                MediaItemProvider.getInstance().addToDatabase(metadata);
            }
        } while (cur.moveToNext());

        cur.close();

        return true;
    }

    protected void sendBroadcast(final String command, int mediaId, final String status, final String message){
        // Fire the broadcast with intent packaged
        // Construct our Intent specifying the Service
        Intent intent = new Intent(ACTION);
        // Add extras to the bundle
        intent.putExtra("resultCode", Activity.RESULT_OK);
        if(mediaId>-1) {
            intent.putExtra("mediaId", mediaId);
        }
        intent.putExtra("command", command);
        intent.putExtra("status", status);
        if(message!=null) {
            intent.putExtra("message", message);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /*
     * Creates a normal item with a Header linked.
     */
    protected MediaMetadata buildMediaMetadata(long id, String mediaTitle, String mediaArtist, String mediaAlbum, String mediaPath, long mediaDuration) {
        if(MediaItemProvider.isMediaFileExist(mediaPath)) {
            MediaMetadata metadata = new MediaMetadata();
            metadata.setId(id);
            metadata.setMediaPath(mediaPath);
            metadata.setMediaType(MediaItemProvider.getExtension(mediaPath).toUpperCase());
            metadata.setTitle(mediaTitle);
            metadata.setAlbum(mediaAlbum);
            metadata.setArtist(mediaArtist);
            metadata.setAudioDuration(mediaDuration);
            metadata.setAudioFormatInfo(metadata.getMediaType() == null ? "" : metadata.getMediaType().toUpperCase());
            metadata.setDisplayPath(MediaItemProvider.getInstance().buildDisplayName(mediaPath));
            MediaItemProvider.getInstance().readMetadata(metadata);
            return metadata;
        }
        return null;
    }

    // Define the callback for what to do when data is received
    /*
    private BroadcastReceiver operationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mediaId = intent.getIntExtra("mediaId",-1);
            //String command = intent.getStringExtra("command");
            String status = intent.getStringExtra("status");

            if("success".equalsIgnoreCase(status) && mediaId>-1) {
                for(int i =0; i<mMediaItems.size();i++) {
                    MediaItem item = mMediaItems.get(i);
                    if(item.getId() == mediaId) {
                        if(MediaItemProvider.getInstance().isMediaFileExist(item)) {
                            MediaItemProvider.getInstance().readMetadata(item, null);
                            sendBroadcast("read",item.getId(),"success",null);
                        }else {
                            mMediaItems.remove(i);
                        }
                        break;
                    }
                }
            }
        }
    }; */
}
