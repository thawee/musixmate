package apincer.android.uamp.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v23Frames;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import apincer.android.uamp.flexibleadapter.HeaderItem;
import apincer.android.uamp.flexibleadapter.MediaItem;
import apincer.android.uamp.utils.BitmapHelper;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.Util;
import eu.davidea.flexibleadapter.items.IHeader;

/**
 * Wrapper class for accessing media information via media store and jaudiotagger
 * Created by e1022387 on 5/10/2017.
 */
public class MediaProvider {{}
    private static MediaProvider instance;
    public static int QUALITY_BIT_HIGH = 16;
    public static int QUALITY_SAMPLING_RATE_HIGH = 44100;
    public static int QUALITY_COMPRESS_BITRATE_GOOD = 320;
    public static int QUALITY_COMPRESS_BITRATE_LOW = 128;
    public static Map supportedFormat = new HashMap();

    public static void initialize(Context context) {
        if(instance==null) {
            instance = new MediaProvider(context);
        }
    }

    public static MediaProvider getInstance() {
        return instance;
    }

    public MediaItem getMediaItemById(int mediaId) {
        return queryMediaItem(mediaId);
    }

    public MediaItem queryMediaItem(String currentTitle, String currentArtist, String currentAlbum) {
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        String query = MediaStore.Audio.Media.TITLE +"="+ DatabaseUtils.sqlEscapeString(currentTitle);
        Cursor cur = mContentResolver.query(uri, null, query, null, "LOWER ("+MediaStore.Audio.Media.TITLE+") ASC");
        if (cur == null) {
            // Query failed...
            return null;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            cur.close();
            return null;
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        String mediaPath=cur.getString(dataColumn);
        String mediaTitle=cur.getString(titleColumn);
        String mediaAlbum=cur.getString(albumColumn);
        String mediaArtist=cur.getString(artistColumn);
        long mediaDuration=cur.getLong(durationColumn);
        int id = cur.getInt(idColumn);
        do {
            String newTitle = cur.getString(titleColumn);
            String newAlbum = cur.getString(albumColumn);
            String newArtist = cur.getString(artistColumn);
            if(StringUtils.compare(newTitle, currentTitle) && StringUtils.compare(newArtist, currentArtist) && StringUtils.compare(newAlbum, currentAlbum)) {
                mediaTitle=newTitle;
                mediaAlbum=newAlbum;
                mediaArtist=newArtist;
                mediaPath = cur.getString(dataColumn);
                mediaDuration = cur.getLong(durationColumn);
                id = cur.getInt(idColumn);
                break;
            }
        } while (cur.moveToNext());

        cur.close();

        return newMediaItem(id,mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);

    }


    public MediaItem queryMediaItem(int id) {
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        String query = MediaStore.Audio.Media._ID+"="+ id;
        Cursor cur = mContentResolver.query(uri, null, query, null, null);
        if (cur == null) {
            // Query failed...
            return null;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            cur.close();
            return null;
        }
        // retrieve the indices of the columns where the ID, title, etc. of the song are
      //  int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        // add each song to mItems
        String mediaPath=cur.getString(dataColumn);
        String mediaTitle=cur.getString(titleColumn);
        String mediaAlbum=cur.getString(albumColumn);
        String mediaArtist=cur.getString(artistColumn);
        long mediaDuration=cur.getLong(durationColumn);
        cur.close();

        return newMediaItem(id,mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);
    }

    public enum MediaTypes {SONGS,SIMILARITY,FILES}
    public enum MediaQuality {HIRES,HIGH,GOOD,NORMAL,LOW}
    private static final String TAG = LogHelper.makeLogTag(MediaProvider.class);
    private Context context;
    private AndroidFile androidFile;
    private ExecutorService executor;
    private static final List<MediaItem> mMediaItems = Collections.synchronizedList(new ArrayList<MediaItem>());;
    private static List<String> mMediaArtists= new ArrayList<>();
    private static List<String> mMediaAlbums= new ArrayList<>();
    private static List<String> mMediaAlbumArtists= new ArrayList<>();

    private MediaProvider(Context context) {
        this.context = context;
        this.androidFile = new AndroidFile(context);
        supportedFormat.put("MP3","MP3");
        supportedFormat.put("M4A","M4A");
        supportedFormat.put("ACC","ACC");
        //supportedFormat.put("OGG", "OGG");
        supportedFormat.put("ALAC", "ALAC");
        supportedFormat.put("FLAC", "FLAC");
        //supportedFormat.put("AIF", "AIF");
    }

    public static AudioFile getAudioFile(String path) {
            try {
                if(isValidForTag(path)) {
                    AudioFile audioFile = AudioFileIO.read(new File(path));
                    //audioFiles.put(path, audioFile);
                    return audioFile;
                }
            } catch (CannotReadException | IOException | TagException |ReadOnlyFileException |InvalidAudioFrameException e) {
               // e.printStackTrace();
            }
        //}
        return null;
    }

    private static boolean isValidForTag(String path) {
        String ext = AndroidFile.getFileExtention(path);
        if(StringUtils.isEmpty(ext)) {
            return false;
        }
        return supportedFormat.containsKey(ext.toUpperCase());
    }

    public List<MediaItem> getAllSongList(boolean forceReload) {
        if (forceReload || mMediaItems.isEmpty()) {
            queryAllMediaItems();
        }
        return mMediaItems;
    }

    private boolean queryAllMediaItems() {
        mMediaItems.clear();
        mMediaAlbumArtists.clear();

        if(executor!=null) {
            executor.shutdownNow();
        }
        executor = Executors.newFixedThreadPool(10);

        HeaderItem header = newHeader("MEDIAITEMS");
        header.setTitle("Music");
        ContentResolver mContentResolver = context.getContentResolver();
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
        int albumArtistColumn = cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
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
            String albumArtist = cur.getString(albumArtistColumn);
            if(!mMediaAlbumArtists.contains(albumArtist)) {
                mMediaAlbumArtists.add(albumArtist);
            }
            mMediaItems.add(newMediaItem(id,header, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration,index++));
        } while (cur.moveToNext());

        cur.close();

        return true;
    }

    public  List<MediaItem> getSimilarTitles(boolean forceReload) {
        List<MediaItem> similarTitles = new ArrayList<>();
        getAllSongList(forceReload);

        MediaItem preItem = null;
        String preTitle = "";
        boolean preAdded = false;
        for (MediaItem item:mMediaItems) {
            //similarity
            if (StringUtils.similarity(item.getTitle(), preTitle)>0.92) {
                if(!preAdded && preItem != null) {
                    similarTitles.add(preItem);
                }
                similarTitles.add(item);
                preAdded = true;
            }else {
                preAdded = false;
            }
            preTitle = item.getTitle();
            preItem = item;
        }

        return similarTitles;
    }

    public void deleteFromMediaStore(String pathToDelete) {
        try {
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
        }catch (Exception ex) {
            // ignore
        }
    }

    public static String formatDuration(long milliseconds) {

        long s = milliseconds / 1000 % 60;

        long m = milliseconds / 1000 / 60 % 60;

        long h = milliseconds / 1000 / 60 / 60 % 24;

        if (h == 0) return String.format(Locale.getDefault(), "%02d:%02d", m, s);

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }

    public static String formatAudioSampleRate(long rate) {
        double s = rate / 1000.00;
        String str = String.format(Locale.getDefault(),"%.1f", s);
        str = str.replace(".0", "");
        return str;
    }

    private void updateTag(MediaTag mediaTag, Tag tag) {
        if (tag == null || mediaTag==null) {
            return;
        }
        if (mediaTag.titleHasChanged) {
            setTagField(tag,FieldKey.TITLE, mediaTag.getTitle());
         }
        if (mediaTag.albumHasChanged) {
            setTagField(tag,FieldKey.ALBUM, mediaTag.getAlbum());
        }
        if (mediaTag.artistHasChanged) {
            setTagField(tag,FieldKey.ARTIST, mediaTag.getArtist());
        }
        if (mediaTag.albumArtistHasChanged) {
            setTagField(tag,FieldKey.ALBUM_ARTIST, mediaTag.getAlbumArtist());
        }
        if (mediaTag.genreHasChanged) {
            setTagField(tag,FieldKey.GENRE, mediaTag.getGenre());
        }
        if (mediaTag.yearHasChanged) {
            setTagField(tag,FieldKey.YEAR, mediaTag.getYear());
        }
        if (mediaTag.trackHasChanged) {
            setTagField(tag,FieldKey.TRACK, mediaTag.getTrack());
        }
        if (mediaTag.trackTotalHasChanged) {
            setTagField(tag,FieldKey.TRACK_TOTAL, mediaTag.getTrackTotal());
        }
        if (mediaTag.discHasChanged) {
            setTagField(tag,FieldKey.DISC_NO, mediaTag.getDisc());
        }
        if (mediaTag.discTotalHasChanged) {
            setTagField(tag,FieldKey.DISC_TOTAL, mediaTag.getDiscTotal());
        }
        if (mediaTag.lyricsHasChanged) {
            setTagField(tag,FieldKey.LYRICS, mediaTag.getLyrics());
        }
        if (mediaTag.commentHasChanged) {
            setTagField(tag,FieldKey.COMMENT, mediaTag.getComment());
        }
        if (mediaTag.groupingHasChanged) {
            setTagField(tag,FieldKey.GROUPING, mediaTag.getGrouping());
        }
        if (mediaTag.composerHasChanged) {
            setTagField(tag,FieldKey.COMPOSER, mediaTag.getComposer());
        }
    }

    private void setTagField(Tag tag,FieldKey key, String text) {
        try {
            if(StringUtils.isEmpty(text)) {
                tag.deleteField(key);
            }else {
                tag.setField(key, text);
            }
        } catch (Exception ignored) {
        }
    }


    public boolean saveMediaFile(String mediaPath,MediaTag tagUpdate) throws Exception{
        if (mediaPath == null) {
            return false;
        }

            boolean isCacheMode = false;
            File file = new File(mediaPath);
            if(!androidFile.isWritable(file)) {
                isCacheMode = true;
                file = androidFile.safToCache(file);
            }

            AudioFile audioFile = getAudioFile(file.getAbsolutePath());
            String ext = AndroidFile.getFileExtention(file.getAbsolutePath());
            if("mp3".equalsIgnoreCase(ext)) {
                MP3File mp3 = (MP3File) audioFile;
                if (mp3.hasID3v1Tag()) {
                    ID3v1Tag oldTag = mp3.getID3v1Tag();
                    mp3.delete(oldTag);
                }
                AbstractID3v2Tag newTag = null;
                if (mp3.hasID3v2Tag()) {
                    newTag = mp3.getID3v2Tag();
                }else {
                    newTag = new ID3v23Tag();
                    mp3.setID3v2Tag(newTag);
                }
                updateTag(tagUpdate, newTag);
            } else {
                updateTag(tagUpdate, audioFile.getTagOrCreateDefault());
            }
            audioFile.commit();

            if(isCacheMode) {
                androidFile.safFromCache(file, mediaPath);
            }
            updateOnMediaStore(mediaPath, tagUpdate);

        return true;
    }

    public boolean deleteMediaFile(String mediaPath)  throws Exception {
        File file = new File(mediaPath);
        File directory = file.getParentFile();
        if (androidFile.deleteFile(file)) {
                androidFile.cleanEmptyDirectory(directory);
                deleteFromMediaStore(mediaPath);
                return true;
        }
        return false;
    }

    public boolean moveMediaFile(String path, String organizedPath)  throws Exception{
        if(androidFile.moveFile(path, organizedPath) ) {
                updatePathOnMediaStore(path, organizedPath);
                File file = new File(path);
                androidFile.cleanEmptyDirectory(file.getParentFile());
                return true;
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
                    MediaStore.Audio.Media.DATA + "=?", new String[]{path}) == 1;
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
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DATA, sqlEscapeString(newPath));
            boolean result = context.getContentResolver().update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values,
                    MediaStore.Audio.Media.DATA + "=?", new String[]{path}) == 1;
         }catch (Exception ex) {
            deleteFromMediaStore(path);
        }
        return true;
    }

    public void loadMediaTag(MediaItem mediaItem, String path) {
        try {
            if(mediaItem.isLoadedEncoding()) return;
            MediaTag mediaTag = mediaItem.getTag();
            if (mediaTag == null) {
                mediaTag = new MediaTag(mediaItem.getPath());
            }

            if(path == null) {
                path = mediaItem.getPath();
            }else {
                mediaItem.setPath(path);
            }
            mediaTag.setDisplayPath(androidFile.getDisplayPath(path));

            AudioFile audioFile = getAudioFile(path);

            if(audioFile==null) return;

            loadAudioCodingFormat(audioFile,mediaItem); //MP3-xxx &&128/256/320kbps
            loadAudioCoding(audioFile,mediaTag); //16/24/32 bit and 44.1/48/96/192 kHz
            mediaTag.mediaSize = getMediaSize(audioFile);
            mediaTag.setTitle(AndroidFile.getNameWithoutExtension(audioFile.getFile()));

            Tag tag = null;
            String ext = AndroidFile.getFileExtention(path);
            if("mp3".equalsIgnoreCase(ext)) {
                MP3File mp3 = (MP3File)audioFile;
                if(mp3.hasID3v2Tag()) {
                    loadMP3V2Tag(mp3, mediaTag);
                }else {
                    // mp3 v1 tag
                    loadTag(audioFile, mediaTag);
                }
            }else {
               loadTag(audioFile, mediaTag);
            }
            mediaItem.setLoadedEncoding(true);
        } catch (Exception |OutOfMemoryError oom) {
            LogHelper.e(TAG, oom);
            System.gc();
        }
    }

    private void loadMP3V2Tag(MP3File mp3, MediaTag mediaTag) {
        AbstractID3v2Tag tag = mp3.getID3v2Tag();
        if (tag != null && !tag.isEmpty()) {
            try {
                mediaTag.setTitle(tag.getFirst(ID3v23Frames.FRAME_ID_V3_TITLE));
            } catch (UnsupportedOperationException ignored) {
                //default to file name
                mediaTag.setTitle(AndroidFile.getNameWithoutExtension(mp3.getFile()));
            }
            try {
                mediaTag.setAlbum(tag.getFirst(ID3v23Frames.FRAME_ID_V3_ALBUM));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                //List<String> artists = tag.getFields(FieldKey.ARTIST);
                //mediaTag.setArtist(StringUtils.merge(artists, ";"));
                mediaTag.setArtist(tag.getFirst(ID3v23Frames.FRAME_ID_V3_ARTIST));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setAlbumArtist(tag.getFirst(ID3v23Frames.FRAME_ID_V3_ACCOMPANIMENT));  //TPE2
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setGenre(tag.getFirst(ID3v23Frames.FRAME_ID_V3_GENRE));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setYear(tag.getFirst(ID3v23Frames.FRAME_ID_V3_TYER));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setTrack(tag.getFirst(ID3v23Frames.FRAME_ID_V3_TRACK));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setComposer(tag.getFirst(ID3v23Frames.FRAME_ID_V3_COMPOSER));
            } catch (UnsupportedOperationException ignored) {
            }

            try {
                mediaTag.setDisc(tag.getFirst(ID3v23Frames.FRAME_ID_V3_SET)); //TPOS
            } catch (UnsupportedOperationException ignored) {
           }
//            try {
//                mediaTag.setDiscTotal(tag.getFirst(ID3v23Frames.FRAME_ID_V3_SET));  //TPOS
//            } catch (UnsupportedOperationException ignored) {
//            }
            try {
                mediaTag.setLyrics(tag.getFirst(ID3v23Frames.FRAME_ID_V3_LYRICIST));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setComment(tag.getFirst(ID3v23Frames.FRAME_ID_V3_COMMENT));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setCountry(tag.getFirst(ID3v23Frames.FRAME_ID_V3_CONTENT_GROUP_DESC));
            } catch (UnsupportedOperationException ignored) {
            }
        }
    }

    private void loadTag(AudioFile audioFile, MediaTag mediaTag) {

        Tag tag = audioFile.getTagOrCreateDefault();
        if (tag != null && !tag.isEmpty()) {
            try {
                mediaTag.setTitle(tag.getFirst(FieldKey.TITLE));
            } catch (UnsupportedOperationException ignored) {
                //default to file name
                mediaTag.setTitle(AndroidFile.getNameWithoutExtension(audioFile.getFile()));
            }
            try {
                mediaTag.setAlbum(tag.getFirst(FieldKey.ALBUM));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                //List<String> artists = tag.getFields(FieldKey.ARTIST);
                //mediaTag.setArtist(StringUtils.merge(artists, ";"));
                mediaTag.setArtist(tag.getFirst(FieldKey.ARTIST));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setAlbumArtist(tag.getFirst(FieldKey.ALBUM_ARTIST));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setGenre(tag.getFirst(FieldKey.GENRE));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setYear(tag.getFirst(FieldKey.YEAR));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setTrack(tag.getFirst(FieldKey.TRACK));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setComposer(tag.getFirst(FieldKey.COMPOSER));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setDisc(tag.getFirst(FieldKey.DISC_NO));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setDiscTotal(tag.getFirst(FieldKey.DISC_TOTAL));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setLyrics(tag.getFirst(FieldKey.LYRICS));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setComment(tag.getFirst(FieldKey.COMMENT));
            } catch (UnsupportedOperationException ignored) {
            }
            try {
                mediaTag.setCountry(tag.getFirst(FieldKey.GROUPING));
            } catch (UnsupportedOperationException ignored) {
            }
        }
    }

    private void loadAudioCodingFormat(AudioFile audioFile, MediaItem mediaItem) {
        try {
            MediaTag tag = mediaItem.getTag();
            long bitrate = audioFile.getAudioHeader().getBitRateAsNumber();
            tag.audioCompressBitrate = String.format(Locale.getDefault(),"%dkbps",new Object[]{bitrate});

            if(audioFile.getAudioHeader().isLossless()) {
                mediaItem.getTag().audioCodingFormat = mediaItem.getMediaType();
             }else {
               // mediaItem.getTag().audioCodingFormat = mediaItem.getMediaType()+" "+bitrate;
                mediaItem.getTag().audioCodingFormat = String.format(Locale.getDefault(), "%s %s", new Object[]{mediaItem.getMediaType(), String.valueOf(bitrate)});
            }
        }catch (Exception ex) {}
    }

    public Bitmap getArtwork(MediaItem mediaItem) {
            AudioFile audioFile = getAudioFile(mediaItem.getPath());

            if(audioFile==null) {
                return null;
            }

            return getArtwork(audioFile.getTagOrCreateDefault(),false);
    }

    public Bitmap getSmallArtwork(MediaItem mediaItem) {
        AudioFile audioFile = getAudioFile(mediaItem.getPath());

        if(audioFile==null) {
            return null;
        }

        return getArtwork(audioFile.getTagOrCreateDefault(),true);
    }

    private Bitmap getArtwork(Tag tag, boolean smallImage) {
        Bitmap bitmap = null;
        Artwork artwork = tag.getFirstArtwork();
        if (null != artwork) {
            byte[] artworkData = artwork.getBinaryData();
            if(smallImage) {
                bitmap = BitmapHelper.decodeBitmap(artworkData, 240,240);
                if(bitmap!=null) {
                //    bitmap = transformCircle(bitmap, 240, 4);
                    bitmap = BitmapHelper.getRoundedBitmap(bitmap,40);
                }
            }else {
                bitmap = BitmapHelper.decodeBitmap(artworkData, 800,800);
            }
            return bitmap;
        }
        return null;
    }

    private String getMediaSize(AudioFile audioFile) {
        try {
            double length = audioFile.getFile().length();
            length = (length/(1024*1024));

            return String.format("%.2f", length)+ " MB";
        }catch (Exception ex) {
            return "";
        }
    }

    private void loadAudioCoding(AudioFile read, MediaTag tag) {
        try {
            long sampling = read.getAudioHeader().getSampleRateAsNumber(); //44100/48000 Hz
            long rate = read.getAudioHeader().getBitsPerSample(); //16/24/32
            long bitrate = read.getAudioHeader().getBitRateAsNumber(); //128/256/320

            tag.setQuality(MediaQuality.NORMAL);
            if(sampling<QUALITY_SAMPLING_RATE_HIGH || rate < QUALITY_BIT_HIGH) {
                tag.setQuality(MediaQuality.LOW);
            }else if(read.getAudioHeader().isLossless()) {
                if(sampling > QUALITY_SAMPLING_RATE_HIGH || rate > QUALITY_BIT_HIGH) {
                    tag.setQuality(MediaQuality.HIRES);
                }else {
                    tag.setQuality(MediaQuality.HIGH);
                }
            }else {
                if(bitrate>=QUALITY_COMPRESS_BITRATE_GOOD) {
                    tag.setQuality(MediaQuality.GOOD);
                }else if(bitrate<QUALITY_COMPRESS_BITRATE_LOW) {
                    tag.setQuality(MediaQuality.LOW);
                }
            }
            tag.audioBitsPerSample= rate+"bit";
            tag.audioSampleRate= formatAudioSampleRate(sampling)+"kHz";
        }catch (Exception ex) {}
    }

    public String getOrganizedPath(MediaItem item) {
        // /format/<album|albumartist|artist>/<track no> <artist>-<title>
        final String ReservedChars = "?|\\*<\":>[]~#%^@.";
        try {
            String musicPath ="/Music/";
            String storeagePath = androidFile.getStoragePath(context, item.getPath());
            if(storeagePath.endsWith(File.separator)) {
                storeagePath = storeagePath.substring(0,storeagePath.length()-1);
            }
            storeagePath = storeagePath+musicPath;

            //AudioFile read = getAudioFile(path);
            //populateMediaTag(read,item);
            //MediaTag tag = readMediaTag(read, item);
            MediaTag tag = item.getTag();

            String ext = AndroidFile.getFileExtention(item.getPath());
            StringBuffer filename = new StringBuffer();

            // country or format
            filename.append(StringUtils.trimToEmpty(getFormatAndCounty(item.getPath(),ext,tag))).append(File.separator);

            // albumArtist or artist
            boolean useAlbumArtist = false;
            if(!StringUtils.isEmpty(tag.getAlbumArtist())) {
                filename.append(formatTitle(tag.getAlbumArtist())).append(File.separator);
                useAlbumArtist = true;
            }else if(!StringUtils.isEmpty(tag.getArtist())) {
                filename.append(formatTitle(tag.getArtist())).append(File.separator);
            }

            // album
            if(!StringUtils.isEmpty(tag.getAlbum())) {
                // album!=albumarist, add album as parent folder
                if(!tag.getAlbum().equalsIgnoreCase(tag.getAlbumArtist())) {
                    filename.append(formatTitle(tag.getAlbum())).append(File.separator);
                }
            }

            // track
            boolean hasTrackOrArtist = false;
            String track = StringUtils.trimToEmpty(tag.getTrack());
            if(!StringUtils.isEmpty(track)) {
                int indx = track.indexOf("/");
                if(indx >0) {
                    filename.append(StringUtils.trimToEmpty(track.substring(0,indx)));
                }else {
                    filename.append(StringUtils.trimToEmpty(track));
                }
                hasTrackOrArtist = true;
            }

            // artist, if albumartist and arttist != albumartist
            if((!StringUtils.isEmpty(tag.getArtist())) && useAlbumArtist && !tag.getArtist().equalsIgnoreCase(tag.getAlbumArtist())) {
                // add artist to file name only have albumArtist
                if(hasTrackOrArtist) {
                    filename.append(" ");
                }
                filename.append(formatTitle(tag.getArtist()));
                hasTrackOrArtist = true;
            }

            // artist
            if(hasTrackOrArtist) {
                filename.append(" - ");
            }

            // title
            if(!StringUtils.isEmpty(tag.getTitle())) {
                filename.append(formatTitle(tag.getTitle()));
            }else {
                filename.append(formatTitle(AndroidFile.getNameWithoutExtension(item.getPath())));
            }

            String newPath =  storeagePath+filename.toString();
            for(int i=0;i<ReservedChars.length();i++) {
                newPath = newPath.replace(String.valueOf(ReservedChars.charAt(i)),"");
            }

            newPath = newPath+"."+ext;

            return newPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item.getPath();
    }

    private String getFormatAndCounty(String path, String ext, MediaTag tag) {
        ext = ext.toUpperCase();
        String tagString = "";
         if(!StringUtils.isEmpty(tag.getGrouping())) {
            tagString = ext+"_"+tag.getGrouping().trim();
          }else {
            tagString = ext;
         }
        return tagString;
    }

    public boolean isMediaFileExist(MediaItem item) {
        if(item == null || item.getPath()==null) {
            return false;
        }
        File file = new File(item.getPath());
        if(file.exists() && file.length() ==0) {
            return false;
        }
        return file.exists();
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
    protected MediaItem newMediaItem(int id, IHeader header, String mediaTitle, String mediaArtist, String mediaAlbum, String mediaPath, long mediaDuration, int index) {
        final MediaItem item = new MediaItem(id, (HeaderItem) header);
        item.setIndex(index);
        item.setPath(mediaPath);
        item.setMediaType(androidFile.getFileExtention(item.getPath()));
        MediaTag tag = new MediaTag(mediaPath);
        item.setTag(tag);
        tag.setTitle(mediaTitle);
        tag.setAudioDuration(mediaDuration);
        tag.setAlbum(mediaAlbum);
        tag.setArtist(mediaArtist);
        tag.setDisplayPath(androidFile.getDisplayPath(item.getPath()));
        tag.setMediaPath(item.getPath());

        return item;
    }

    protected MediaItem newMediaItem(int id,String mediaTitle, String mediaArtist, String mediaAlbum, String mediaPath, long mediaDuration) {
        final MediaItem item = new MediaItem(id, null);
        item.setIndex(0);
        item.setPath(mediaPath);
        item.setMediaType(androidFile.getFileExtention(item.getPath()));
        MediaTag tag = new MediaTag(mediaPath);
        item.setTag(tag);
        tag.setTitle(mediaTitle);
        tag.setAudioDuration(mediaDuration);
        tag.setAlbum(mediaAlbum);
        tag.setArtist(mediaArtist);
        tag.setDisplayPath(androidFile.getDisplayPath(item.getPath()));
        tag.setMediaPath(item.getPath());
        loadMediaTag(item, null);

        return item;
    }

    public Collection getFoldersForSongs(Context context, String parentFolder) {
        List<MediaItem> mItems = new ArrayList<>();
        if(parentFolder==null) {
            // list all internal Memory  and SDCARD
        }else {
            // list all directory and music files
        }
        return mItems;
    }

    private final Cursor makeArtistCursor() {
        return  context.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.ArtistColumns.ARTIST}, null, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
    }

    public String[] getArtistsAsArray() {
        if(mMediaArtists.isEmpty()) {
            Cursor mCursor = makeArtistCursor();
            if (mCursor != null && mCursor.moveToFirst()) {
                do {
                    final String artistName = mCursor.getString(0);
                    mMediaArtists.add(artistName);
                } while (mCursor.moveToNext());
            }
            if (mCursor != null) {
                Util.closeSilently(mCursor);
                mCursor = null;
            }
        }
        return mMediaArtists.toArray(new String[0]);
    }

    public String[] getAlbumAsArray() {
        if(mMediaAlbums.isEmpty()) {
            Cursor mCursor = makeAlbumCursor();
            if (mCursor != null && mCursor.moveToFirst()) {
                do {
                    final String artistName = mCursor.getString(0);
                    mMediaAlbums.add(artistName);
                } while (mCursor.moveToNext());
            }
            if (mCursor != null) {
                Util.closeSilently(mCursor);
                mCursor = null;
            }
        }
        return mMediaAlbums.toArray(new String[0]);
    }

    private Cursor makeAlbumCursor() {
        return  context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.AlbumColumns.ALBUM}, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }

    public String[]  getAlbumArtistAsArray() {
        return mMediaAlbumArtists.toArray(new String[0]);
    }

    public String formatTitle(CharSequence text) {
        // trim space
        // format as word, first letter of word is capital
        if(text==null) {
            return "";
        }
        String str = text.toString().trim();
        char [] delimiters = {' ','.','(','['};
        return StringUtils.capitalize(str, delimiters);
    }
}
