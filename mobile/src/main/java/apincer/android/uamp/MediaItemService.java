package apincer.android.uamp;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.MimeTypeMap;

import org.jaudiotagger.audio.SupportedFileFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.model.MediaTag;
import apincer.android.uamp.provider.MediaFileProvider;
import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.StringUtils;
import stream.custompermissionsdialogue.utils.PermissionUtils;

/**
 * Created by e1022387 on 1/8/2018.
 */

public class MediaItemService extends IntentService {
    public static final String ACTION = "com.apincer.uamp.MediaItemService";
    private Context mContext;
    private List<MediaItem> pendingItems = new ArrayList<>();
    private static List<MediaItem> mPlayedItems = Collections.synchronizedList(new ArrayList<MediaItem>());
    private static final List<MediaItem> mMediaItems = Collections.synchronizedList(new ArrayList<MediaItem>());
    private static final List<String> mMediaArtists= new ArrayList<>();
    private static final List<String> mMediaAlbums= new ArrayList<>();
    private static final List<String> mMediaAlbumArtists= new ArrayList<>();

    private static double MIN_TITLE = 0.70;
    private static double MIN_ARTIST = 0.60;

    public  void addPending(MediaItem item) {
        if(pendingItems==null) {
            pendingItems = new ArrayList<>();
        }
        pendingItems.add(item);
    }

    public static void addPlaying(MediaItem item) {
        if(mPlayedItems==null) {
            mPlayedItems = new ArrayList<>();
        }
        mPlayedItems.add(item);
    }

    public static Collection<? extends MediaItem> getPlayedItems() {
        List<MediaItem> songList = new ArrayList();
        Map<String, MediaItem> mapped = new HashMap<>();
        for(int ix=(mPlayedItems.size()-1); ix >0;ix--) {
            MediaItem item = mPlayedItems.get(ix);
            String id = String.valueOf(item.getId());
            if(MediaItemProvider.getInstance().isMediaFileExist(item) && !mapped.containsKey(id)) {
                songList.add(item);
                mapped.put(id,item);
            }
        }
        return songList;
    }

    public static Collection<? extends MediaItem> getMediaItems() {
        return mMediaItems;
    }

    public static MediaItem getMediaItem(int index) {
        return mMediaItems.get(index);
    }

    public static void startService(Context context, String command) {
        // Construct our Intent specifying the Service
        Intent intent = new Intent(context, MediaItemService.class);
        // Add extras to the bundle
        intent.putExtra("command", command);
        // Start the service
        context.startService(intent);
    }

    public static Collection<? extends MediaItem> getSimilarSongs() {
        List<MediaItem> similarTitles = new ArrayList<>();
        MediaItem preItem = null;
        boolean preAdded = false;
        for (int i=0; i<getMediaItems().size();i++) {
            MediaItem item = getMediaItem(i);
            //similarity
           // if (StringUtils.similarity(item.getTag().getTitle(), preTitle)>0.92) {
            if (preItem!=null && (StringUtils.similarity(item.getTag().getTitle(), preItem.getTag().getTitle())>MIN_TITLE) && (StringUtils.similarity(item.getTag().getArtist(), preItem.getTag().getArtist())>MIN_ARTIST)) {
                if(!preAdded && preItem != null) {
                    similarTitles.add(preItem);
                }
                similarTitles.add(item);
                preAdded = true;
            }else {
                preAdded = false;
            }
            preItem = item;
        }

        return similarTitles;
    }


    public static Collection<? extends MediaItem> getSimilarTitle() {
        List<MediaItem> similarTitles = new ArrayList<>();
        MediaItem preItem = null;
        boolean preAdded = false;
        for (MediaItem item:getMediaItems()) {
            //similarity
            if (preItem!=null && StringUtils.similarity(item.getTag().getTitle(), preItem.getTag().getTitle())>MIN_TITLE) {
                if(!preAdded && preItem != null) {
                    similarTitles.add(preItem);
                }
                similarTitles.add(item);
                preAdded = true;
            }else {
                preAdded = false;
            }
            preItem = item;
        }

        return similarTitles;
    }

    public static Collection<? extends MediaItem> getNewMediaItems() {
        List<MediaItem> items = new ArrayList<>();
       for (MediaItem item:mMediaItems) {
            String path = item.getPath();
            if (!path.contains("/Music/") || path.contains("/znew")) {
                items.add(item);
            }
        }
        return items;
    }

    public static Collection<? extends MediaItem> getHiResMediaItems() {
        List<MediaItem> songList = new ArrayList<>();
        for (MediaItem item:getMediaItems()) {
            if (item.getMetadata().getQuality() == MediaMetadata.MediaQuality.HIRES) {
                songList.add(item);
            }
        }
        return songList;
    }

    public static Collection<? extends String> getAlbums() {
        return mMediaAlbums;
    }
    public static Collection<? extends String> getAlbumArtists() {
        return mMediaAlbumArtists;
    }
    public static Collection<? extends String> getArtists() {
        return mMediaArtists;
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

        IntentFilter filter = new IntentFilter(FileManagerService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(operationReceiver, filter);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String command = intent.getStringExtra("command");
        if(!PermissionUtils.IsPermissionsEnabled(getApplicationContext(), MusicService.PERMISSIONS_STORAGE)) {
            return ;
        }
        if("load".equalsIgnoreCase(command) ) {
            loadMediaItems();
            // send broadcast load completed
            sendBroadcast("load",-1,"success",null);
            for(MediaItem item: pendingItems) {
                MediaItemProvider.getInstance().readId3Tag(item, null);
                updateAlbumArtist(item.getTag().getAlbumArtist());
                //sendBroadcast("read", item.getId(),"success",null);
            }
            pendingItems.clear();
            sendBroadcast("read",-1,"success",null);
        }
    }

    public static String buildMediaSelection(){
        StringBuilder selection=new StringBuilder();
        for(SupportedFileFormat format : SupportedFileFormat.values()) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format.name().toLowerCase());
            selection.append("(" + MediaStore.Files.FileColumns.MIME_TYPE + "=='"+ mimeType+ "') OR ");
        }
        selection.append(" ("+MediaStore.Audio.Media.IS_MUSIC + " = 1 ) ");
        return selection.substring(0,selection.lastIndexOf(")") + 1);
    }

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
        String selection = buildMediaSelection();
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
    }

    private boolean loadMediaItemsMusicOnly() {
        mMediaItems.clear();
        mMediaArtists.clear();
        mMediaAlbums.clear();
        mMediaAlbumArtists.clear();
        ContentResolver mContentResolver = mContext.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, "LOWER ("+MediaStore.Audio.Media.TITLE+") ASC");
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
    }

    private void updateAlbum(String album) {
        if(!StringUtils.isEmpty(album) && !mMediaAlbums.contains(album)) {
            mMediaAlbums.add(album);
        }
    }

    private void updateArtist(String artist) {
        if(!StringUtils.isEmpty(artist) && !mMediaArtists.contains(artist)) {
            mMediaArtists.add(artist);
        }
    }

    private void updateAlbumArtist(String albumArtist) {
        if(!StringUtils.isEmpty(albumArtist) && !mMediaAlbumArtists.contains(albumArtist)) {
            mMediaAlbumArtists.add(albumArtist);
        }
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
    protected MediaItem buildMediaItem(int id, String mediaTitle, String mediaArtist, String mediaAlbum, String mediaPath, long mediaDuration, int index) {
        final MediaItem item = new MediaItem(id);
        item.setPath(mediaPath);
        MediaTag tag = item.getTag();
        MediaMetadata metadata = item.getMetadata();
        metadata.setMediaType(MediaFileProvider.getExtension(item.getPath()).toUpperCase());
        tag.setAndroidTitle(mediaTitle);
        tag.setTitle(mediaTitle);
        tag.setAndroidAlbum(mediaAlbum);
        tag.setAlbum(mediaAlbum);
        tag.setAndroidArtist(mediaArtist);
        tag.setArtist(mediaArtist);
        metadata.setAudioDuration(mediaDuration);
        metadata.setAudioFormatInfo(metadata.getMediaType()==null?"":metadata.getMediaType().toUpperCase());
        metadata.setDisplayPath(item.getPath());
        metadata.setMediaPath(item.getPath());
        addPending(item); // pending for read tags
        return item;
    }

    // Define the callback for what to do when data is received
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
                            item.setLoadedEncoding(false);
                            MediaItemProvider.getInstance().readId3Tag(item, null);
                            sendBroadcast("read",item.getId(),"success",null);
                        }else {
                            mMediaItems.remove(i);
                           // sendBroadcast(apincer.android.uamp.Constants.COMMAND_DELETE,item.getId(),"success",null);
                        }
                        break;
                    }
                }
            }
        }
    };
}
