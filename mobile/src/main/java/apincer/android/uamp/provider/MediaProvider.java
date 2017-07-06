package apincer.android.uamp.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.reference.ID3V2Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import apincer.android.uamp.file.AndroidFile;
import apincer.android.uamp.item.HeaderItem;
import apincer.android.uamp.item.MediaItem;
import apincer.android.uamp.ui.BrowserViewPagerActivity;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import eu.davidea.flexibleadapter.items.IHeader;

/**
 * Wrapper class for accessing media information via media store and jaudiotagger
 * Created by e1022387 on 5/10/2017.
 */
public class MediaProvider {
    private static final String TAG = LogHelper.makeLogTag(MediaProvider.class);
    private Context context;
    private AndroidFile androidFile;

    public MediaProvider(Context context) {
        this.context = context;
        this.androidFile = new AndroidFile(context);

        //TagOptionSingleton.getInstance().setAndroid(true);
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V23);
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
        TagOptionSingleton.getInstance().setResetTextEncodingForExistingFrames(true);
        TagOptionSingleton.getInstance().setId3v1Save(true);
        TagOptionSingleton.getInstance().setLyrics3Save(true);
        TagOptionSingleton.getInstance().setWriteChunkSize(2097152);
        TagOptionSingleton.getInstance().setPersistedUri(androidFile.getPersistedUri());
    }

    public Collection getAllSongList(Context context) {
        List<MediaItem> mItems = new ArrayList<>();
        HeaderItem header = newHeader("ALL_SONGS");
        header.setTitle("All Songs");
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, "LOWER ("+MediaStore.Audio.Media.TITLE+") ASC");
        if (cur == null) {
            // Query failed...
            return new ArrayList();
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            return new ArrayList();
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        do {
            String mediaPath =  cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            mItems.add(newMediaItem(context, header, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration));
        } while (cur.moveToNext());

        cur.close();

        return mItems;
    }


    public Collection getSongByArtist(Context context) {
        List<MediaItem> mItems = new ArrayList<>();
        Map<String, HeaderItem> headers = new HashMap<>();
        //HeaderItem header = newHeader("All Songs", 0);
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, "LOWER (" + MediaStore.Audio.Media.ARTIST + ") ASC, LOWER (" + MediaStore.Audio.Media.TITLE + ") ASC");
        if (cur == null) {
            // Query failed...
            return new ArrayList();
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            return new ArrayList();
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumArtistColumn = cur.getColumnIndex(MediaStore.Audio.AlbumColumns.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        do {
            String mediaPath = cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbumArtist = cur.getString(albumArtistColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            String artist = mediaArtist;
            if(!StringUtils.isEmpty(mediaAlbumArtist)) {
                artist = mediaAlbumArtist;
            }
            HeaderItem header = headers.get(artist);
            if(header==null) {
                header = newHeader(artist);
                header.setTitle(artist);
                headers.put(artist, header);
            }
            mItems.add(newMediaItem(context, header, mediaTitle, mediaArtist, mediaAlbum, mediaPath, mediaDuration));
        } while (cur.moveToNext());

        return mItems;
    }

    public Collection getSongByAlbum(Context context) {
        List<MediaItem> mItems = new ArrayList<>();
        Map<String, HeaderItem> headers = new HashMap<>();
        //HeaderItem header = newHeader("All Songs", 0);
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, "LOWER (" + MediaStore.Audio.Media.ALBUM + ") ASC, LOWER (" + MediaStore.Audio.Media.TITLE + ") ASC");
        if (cur == null) {
            // Query failed...
            return new ArrayList();
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            return new ArrayList();
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        do {
            String mediaPath = cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            HeaderItem header = headers.get(mediaAlbum);
            if(header==null) {
                header = newHeader(mediaAlbum);
                header.setTitle(mediaAlbum);
                headers.put(mediaAlbum, header);
            }
            mItems.add(newMediaItem(context, header, mediaTitle, mediaArtist, mediaAlbum, mediaPath, mediaDuration));
        } while (cur.moveToNext());

        return mItems;
    }

    public  Collection getSongForFolder(Context context) {
        List<MediaItem> mItems = new ArrayList<>();
        Map<String, HeaderItem> headers = new HashMap<>();
        //HeaderItem header = newHeader("All Songs", 0);
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, "LOWER (" + MediaStore.Audio.Media.DATA + ") ASC, LOWER (" + MediaStore.Audio.Media.TITLE + ") ASC");
        if (cur == null) {
            // Query failed...
            return new ArrayList();
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            return new ArrayList();
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        do {
            String mediaPath = cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            File file = new File(mediaPath);
            String displayPath = androidFile.getDisplayPath(file.getParentFile().getName());
            HeaderItem header = headers.get(displayPath);
            if(header==null) {
                header = newHeader(displayPath);
                header.setTitle(displayPath);
                headers.put(displayPath, header);
            }
            mItems.add(newMediaItem(context, header, mediaTitle, mediaArtist, mediaAlbum, mediaPath, mediaDuration));
        } while (cur.moveToNext());

        return mItems;
    }


    public  Collection getSimilarSongList(Context context) {
        List<MediaItem> mItems = new ArrayList<>();
        HeaderItem header = newHeader("SIMILARITY");
        header.setTitle("Similar Songs");
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, "LOWER ("+MediaStore.Audio.Media.TITLE+") ASC");
        if (cur == null) {
            // Query failed...
            return new ArrayList();
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            return new ArrayList();
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        MediaItem preItem = null;
        String preTitle = "";
        boolean preAdded = false;

        // add each song to mItems
        do {
            String mediaPath =  cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            MediaItem item = newMediaItem(context, header, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);
            //similarity
            if (StringUtils.similarity(mediaTitle, preTitle)>0.9d) {
                if(!preAdded && preItem != null) {
                    mItems.add(preItem);
                }
                mItems.add(item);
                preAdded = true;
            }else {
                preAdded = false;
            }
            preTitle = mediaTitle;
            preItem = item;
        } while (cur.moveToNext());

        return mItems;
    }

    public Bitmap getArtworkFromPath(Uri path) {
        try {
            AudioFile read = AudioFileIO.read(new File(path.toString()));
            return getArtwork(read.getTagOrCreateDefault());
        } catch (CannotReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TagException e) {
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteFromMediaStore(String pathToDelete) {
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null) {
            Cursor query = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media._ID, "_data"}, "_data = ?", new String[]{pathToDelete}, null);
            if (query != null && query.getCount() > 0) {
                query.moveToFirst();
                while (!query.isAfterLast()) {
                    context.getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, (long) query.getInt(query.getColumnIndex(MediaStore.Audio.Media._ID))), null, null);
                    query.moveToNext();
                }
                query.close();
            }
        }
    }

    public void notifyMediaStoreChanges(String filePath) {
        context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.fromFile(new File(filePath))));
    }

    public static String formatDuration(long milliseconds) {

        long s = milliseconds / 1000 % 60;

        long m = milliseconds / 1000 / 60 % 60;

        long h = milliseconds / 1000 / 60 / 60 % 24;

        if (h == 0) return String.format(Locale.getDefault(), "%02d:%02d", m, s);

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }


    public void updateTag(MediaTag mediaTag, Tag tag) {
        if (tag == null || mediaTag==null) {
            return;
        }
        if (mediaTag.titleHasChanged) {
            try {
                tag.setField(FieldKey.TITLE, mediaTag.title);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.albumHasChanged) {
            try {
                tag.setField(FieldKey.ALBUM, mediaTag.album);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.artistHasChanged) {
            try {
                tag.setField(FieldKey.ARTIST, mediaTag.artist);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.albumArtistHasChanged) {
            try {
                tag.setField(FieldKey.ALBUM_ARTIST, mediaTag.albumArtist);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.genreHasChanged) {
            try {
                tag.setField(FieldKey.GENRE, mediaTag.genre);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.yearHasChanged) {
            try {
                tag.setField(FieldKey.YEAR, mediaTag.year);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.trackHasChanged) {
            try {
                tag.setField(FieldKey.TRACK, mediaTag.track);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.trackTotalHasChanged) {
            try {
                tag.setField(FieldKey.TRACK_TOTAL, mediaTag.trackTotal);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.discHasChanged) {
            try {
                tag.setField(FieldKey.DISC_NO, mediaTag.disc);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.discTotalHasChanged) {
            try {
                tag.setField(FieldKey.DISC_TOTAL, mediaTag.discTotal);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.lyricsHasChanged) {
            try {
                tag.setField(FieldKey.LYRICS, mediaTag.lyrics);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.commentHasChanged) {
            try {
                tag.setField(FieldKey.COMMENT, mediaTag.comment);
            } catch (Exception ignored) {
            }
        }
        if (mediaTag.countryHasChanged) {
            try {
                tag.setField(FieldKey.MUSICBRAINZ_RELEASE_COUNTRY, mediaTag.country);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean saveMediaFile(String mediaPath, MediaTag tagUpdate) {
        if (mediaPath == null) {
            return false;
        }

        try {
            boolean isCacheMode = false;
            File file = new File(mediaPath);
            if(!androidFile.isWritable(file)) {
                isCacheMode = true;
                LogHelper.d(TAG, "saf document:"+file.getAbsolutePath());
                file = androidFile.safToCache(file);
                LogHelper.d(TAG, "cached saf document:"+file.getAbsolutePath());
            }

            AudioFile audioFile = AudioFileIO.read(file);
            Tag newTag = audioFile.getTagOrCreateAndSetDefault();
            updateTag(tagUpdate, newTag);
            audioFile.commit();

            if(isCacheMode) {
                LogHelper.i(TAG, "saf path:"+mediaPath);
                androidFile.safFromCache(file, mediaPath);
            }
            updateOnMediaStore(mediaPath, tagUpdate);
        } catch (Exception e) {
            LogHelper.i(TAG, "Exception: ", e.getMessage());
            LogHelper.e(TAG, e);
            return false;
        }
        return true;
    }

    public boolean deleteMediaFile(String mediaPath) {
       // AndroidFile.deleteFile(mediaPath, context);
       try {
           if (androidFile.deleteFile(new File(mediaPath))) {
               deleteFromMediaStore(mediaPath);
               return true;
           }
       }catch(Exception e) {
           LogHelper.i(TAG, "Exception: ", e.getMessage());
           LogHelper.e(TAG, e);
       }
       return false;
    }

    public boolean moveMediaFile(String path, String organizedPath) {
        try {
            if(androidFile.moveFile(path, organizedPath) ) {
                updatePathOnMediaStore(path, organizedPath);
                return true;
            }
        }catch(Exception e) {
            LogHelper.i(TAG, "Exception: ", e.getMessage());
            LogHelper.e(TAG, e);
        }
        return false;
    }

    private boolean updateOnMediaStore(String path, MediaTag tag) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.TITLE, sqlEscapeString(tag.getTitle()));
        values.put(MediaStore.Audio.Media.ALBUM, sqlEscapeString(tag.getAlbum()));
        values.put(MediaStore.Audio.Media.ARTIST, sqlEscapeString(tag.getArtist()));
        boolean successMediaStore = context.getContentResolver().update(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values,
                MediaStore.Audio.Media.DATA + "=?", new String[]{ path }) == 1;
        return successMediaStore;
    }

    private String sqlEscapeString(String sqlString) {
        StringBuilder sb = new StringBuilder();
        if (sqlString.indexOf('\'') != -1) {
            int length = sqlString.length();
            for (int i = 0; i < length; i++) {
                char c = sqlString.charAt(i);
                if (c == '\'') {
                    sb.append('\'');
                }
                sb.append(c);
            }
        } else {
            sb.append(sqlString);
        }
        return sb.toString();
    }

    private boolean updatePathOnMediaStore(String path, String newPath) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DATA, sqlEscapeString(newPath));
        boolean successMediaStore = context.getContentResolver().update(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values,
                MediaStore.Audio.Media.DATA + "=?", new String[]{ path }) == 1;
        return successMediaStore;
    }

    public  MediaItem loadMediaItemFromMediaStore(Context context, String path) {
            ContentResolver mContentResolver = context.getContentResolver();
            Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            // Perform a query on the content resolver. The URI we're passing specifies that we
            // want to query for all audio media on external storage (e.g. SD card)
            Cursor cur = mContentResolver.query(uri, null, MediaStore.Audio.Media.DATA+ " = ?",new String[]{path}, null);
            if (cur == null) {
                // Query failed...
                return null;
            }
            if (!cur.moveToFirst()) {
                // Nothing to query. There is no music on the device. How boring.
                return null;
            }
            // retrieve the indices of the columns where the ID, title, etc. of the song are
            int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

            // add each song to mItems
            String mediaPath =  cur.getString(dataColumn);
            String mediaTitle = cur.getString(titleColumn);
            String mediaAlbum = cur.getString(albumColumn);
            String mediaArtist = cur.getString(artistColumn);
            long mediaDuration = cur.getLong(durationColumn);
            MediaItem item = newMediaItem(context, null, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);
            cur.close();
            return item;

    }

    public MediaTag getMediaTag(String path, long mediaDuration) {
        try {
            AudioFile read = AudioFileIO.read(new File(path));
            return readMediaTag(read, path, null, mediaDuration);
        } catch (CannotReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TagException e) {
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MediaTag readMediaTag(AudioFile audioFile, String path, MediaItem media, long mediaDuration) {
        MediaTag mediaTag = new MediaTag(path);
        Tag tag =audioFile.getTagOrCreateDefault();
        mediaTag.mediaBitrate = getBitrate(audioFile);
        mediaTag.mediaSampleRate = getSampleRate(audioFile);
        mediaTag.mediaPath = audioFile.getFile().getAbsolutePath();
        mediaTag.displayPlath = androidFile.getDisplayPath(audioFile.getFile().getAbsolutePath());
        mediaTag.mediaSize = getMediaSize(audioFile);
        mediaTag.mediaDuration = getMediaDuration(audioFile, media,mediaDuration);
        mediaTag.title = AndroidFile.getNameWithoutExtension(audioFile.getFile());
        try {
            mediaTag.title = tag.getFirst(FieldKey.TITLE);
        } catch (UnsupportedOperationException ignored) {
            //default to file name
            mediaTag.title = AndroidFile.getNameWithoutExtension(audioFile.getFile());
        }
        try {
            mediaTag.album = tag.getFirst(FieldKey.ALBUM);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.artist = tag.getFirst(FieldKey.ARTIST);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.genre = tag.getFirst(FieldKey.GENRE);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.year = tag.getFirst(FieldKey.YEAR);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.track = tag.getFirst(FieldKey.TRACK);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.trackTotal = tag.getFirst(FieldKey.TRACK_TOTAL);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.disc = tag.getFirst(FieldKey.DISC_NO);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.discTotal = tag.getFirst(FieldKey.DISC_TOTAL);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.lyrics = tag.getFirst(FieldKey.LYRICS);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.comment = tag.getFirst(FieldKey.COMMENT);
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            mediaTag.country = tag.getFirst(FieldKey.MUSICBRAINZ_RELEASE_COUNTRY);
        } catch (UnsupportedOperationException ignored) {
        }
        mediaTag.artwork = getArtwork(tag);

        return mediaTag;
    }


    public Bitmap getArtwork(Tag tag) {
        Bitmap bitmap = null;
        Artwork artwork = tag.getFirstArtwork();
        if (null != artwork) {
            byte[] artworkData = artwork.getBinaryData();
            bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.length);
        }
        return bitmap;
    }

    private String getMediaSize(AudioFile audioFile) {
        try {
            double length = audioFile.getFile().length();
            length = (length/(1024*1024));

            return String.format("%.2f", length)+ "MB";
        }catch (Exception ex) {
            return "";
        }
    }

    private String getMediaDuration(AudioFile audioFile, MediaItem media, long mediaDuration) {
        try {
            long length = mediaDuration;
            if(length==0) {
                if (media != null) {
                    length = media.getDuration();
                } else {
                    // read form jaudiotagger if mediaItem is null, may get in-corrected duration
                    length = audioFile.getAudioHeader().getTrackLength();
                }
            }
            return MediaProvider.formatDuration(length);
        }catch (Exception ex) {
            return "00:00";
        }
    }

    private String getBitrate(AudioFile read) {
        try {
            return read.getAudioHeader().getBitRate() + "kbps";
        }catch (Exception ex) {
            return "";
        }
    }

    private String getSampleRate(AudioFile read) {
        try {
            long rate = read.getAudioHeader().getSampleRateAsNumber();
            return rate+"Hz";
        }catch (Exception ex) {
            return "";
        }
    }


    public String getOrganizedPath(String path) {
        final String ReservedChars = "|\\?*<\":>+[]~#%^@!'";
        try {
            String musicPath ="/Music/";
            String storeagePath = "";
            int musicIndex = path.indexOf(musicPath);
            if(musicIndex >= 0) {
                // use Music as base directory
                storeagePath = path.substring(0, musicIndex + musicPath.length());
            }else {
                // create new Music directory
                storeagePath = androidFile.getStoragePath(context, path);
                if(storeagePath.endsWith(File.separator)) {
                    storeagePath = storeagePath.substring(0,storeagePath.length()-1);
                }
                storeagePath = storeagePath+musicPath;
            }
            AudioFile read = AudioFileIO.read(new File(path));
                MediaTag tag = readMediaTag(read, path, null, 0);

            if(StringUtils.isEmpty(tag.getTitle())) {
                return path;
            }

            String ext = AndroidFile.getFileExtention(path);

            StringBuffer filename = new StringBuffer();

            // country
            if(!StringUtils.isEmpty(tag.getCountry())) {
                filename.append(tag.getCountry()).append(File.separator);
            }

            // albumArtist or artist
            boolean useAlbumArtist = false;
            if(!StringUtils.isEmpty(tag.getAlbumArtist())) {
                filename.append(tag.getAlbumArtist()).append(File.separator);
                useAlbumArtist = true;
            }else if(!StringUtils.isEmpty(tag.getArtist())) {
                filename.append(tag.getArtist()).append(File.separator);
            }

            // album
            // ignore album if albumartist equals to album
            if(!StringUtils.isEmpty(tag.getAlbum()) && !(useAlbumArtist && tag.getAlbumArtist().equals(tag.getAlbum()))) {
                filename.append(tag.getAlbum()).append(File.separator);
            }

            // track
            if(!StringUtils.isEmpty(tag.getTrack())) {
                filename.append(tag.getTrack()).append(" ");
            }

            // title
            filename.append(tag.getTitle()).append(".").append(ext);

            String newPath =  storeagePath+filename.toString();
            for(int i=0;i<ReservedChars.length();i++) {
                newPath = newPath.replace(ReservedChars.charAt(i),'_');
            }
            return newPath;
        } catch (CannotReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TagException e) {
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
        }
        return path;
    }

    public boolean isMediaFileExist(MediaItem item) {
        if(item == null || item.getPath()==null) {
            return false;
        }
        File file = new File(item.getPath());
        return file.exists();
    }

    public  MediaItem getMediaItemByPath(Context context, String path) {
        try {
            AudioFile read = AudioFileIO.read(new File(path));
            // add each song to mItems
            String mediaPath =  path;
            String mediaTitle = read.getTag().getFirst(FieldKey.KEY.TITLE);
            String mediaAlbum = read.getTag().getFirst(FieldKey.KEY.ALBUM);
            String mediaArtist = read.getTag().getFirst(FieldKey.KEY.ARTIST);
            long mediaDuration = read.getAudioHeader().getTrackLength();
            return newMediaItem(context, null, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);
        } catch (CannotReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TagException e) {
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Creates a Header item.
    */
    public static HeaderItem newHeader(String title) {
        HeaderItem header = new HeaderItem(title);
        header.setTitle("");
        //header is hidden and un-selectable by default!
        return header;
    }

    /*
     * Creates a normal item with a Header linked.
    */
    protected MediaItem newMediaItem(Context context, IHeader header, String mediaTitle, String mediaArtist, String mediaAlbum, String mediaPath, long mediaDuration) {
        // if(albumArtCache==null) {
        //     albumArtCache = AlbumArtCache.getInstance(new MediaProvider(context));
        //}
        final MediaItem item = new MediaItem(mediaPath, (HeaderItem) header);
        item.setTitle(mediaTitle);
        item.setAlbum(mediaAlbum);
        item.setArtist(mediaArtist);
        item.setPath(mediaPath);
        item.setDuration(mediaDuration);
        item.setDisplayPath(androidFile.getDisplayPath(item.getPath()));
        // FIXME get icon bitmap
        /*
        Bitmap art = albumArtCache.getIconImage(path);
        if(art==null) {
            albumArtCache.fetch(pth, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage) {
                    item.setIconBitmap(iconImage);
                }
            });
        }*/
        return item;
    }

    public String getDisplayPath(BrowserViewPagerActivity browserViewPagerActivity, String path) {
        return androidFile.getDisplayPath(path);
    }
}