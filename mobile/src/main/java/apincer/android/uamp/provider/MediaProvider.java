package apincer.android.uamp.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.bumptech.glide.RequestManager;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import apincer.android.uamp.R;
import apincer.android.uamp.file.AndroidFile;
import apincer.android.uamp.glide.GlideApp;
import apincer.android.uamp.item.HeaderItem;
import apincer.android.uamp.item.MediaItem;
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
    private List<MediaItem> mMediaItems;
    private List<String> mMediaArtists;
    private List<String> mMediaAlbums;
    private List<String> mMediaAlbumArtists;
    private RequestManager glide;

    public MediaProvider(Context context) {
        this.context = context;
        this.androidFile = new AndroidFile(context);
        mMediaItems = new ArrayList<>();
        mMediaArtists = new ArrayList<>();
        mMediaAlbums = new ArrayList<>();
        mMediaAlbumArtists = new ArrayList<>();
        glide = GlideApp.with(context);

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

    public Collection getAllSongList(boolean forceReload) {
        if (forceReload || mMediaItems.isEmpty()) {
            loadMediaItems();
        }
        return mMediaItems;
    }

    private boolean loadMediaItems() {
        mMediaItems.clear();
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
            mMediaItems.add(newMediaItem(header, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration));
        } while (cur.moveToNext());

        cur.close();

        return true;
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
            mItems.add(newMediaItem(header, mediaTitle, mediaArtist, mediaAlbum, mediaPath, mediaDuration));
        } while (cur.moveToNext());

        return mItems;
    }


    public  Collection getSimilarTitles(boolean forceReload) {
        List<MediaItem> similarTitles = new ArrayList<>();
        getAllSongList(forceReload);

        MediaItem preItem = null;
        String preTitle = "";
        boolean preAdded = false;
        for (MediaItem item:mMediaItems) {
            //similarity
            if (StringUtils.similarity(item.getTitle(), preTitle)>0.9) {
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

    public Bitmap getArtworkFromFile(String path) {
        try {
            AudioFile read = AudioFileIO.read(new File(path));
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

    public static String formatSampleRate(long rate) {
        double s = rate / 1000.00;
        String str = String.format(Locale.getDefault(),"%.1f", s);
        str = str.replace(".0", "");
        return str;
    }

    public void updateTag(MediaTag mediaTag, Tag tag) {
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
        if (mediaTag.countryHasChanged) {
            setTagField(tag,FieldKey.MUSICBRAINZ_RELEASE_COUNTRY, mediaTag.getCountry());
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

    public boolean saveMediaFile(String mediaPath, MediaTag tagUpdate, View view) {
        if (mediaPath == null) {
            return false;
        }

        try {
            boolean isCacheMode = false;
            File file = new File(mediaPath);
            if(!androidFile.isWritable(file)) {
                isCacheMode = true;
                Snackbar.make(view, context.getString(R.string.progress_move_from_sd), Snackbar.LENGTH_SHORT).show();
                //LogHelper.d(TAG, "saf document:"+file.getAbsolutePath());
                file = androidFile.safToCache(file);
                //LogHelper.d(TAG, "cached saf document:"+file.getAbsolutePath());
            }

            Snackbar.make(view, context.getString(R.string.progress_write_tags), Snackbar.LENGTH_SHORT).show();
            AudioFile audioFile = AudioFileIO.read(file);
            Tag newTag = audioFile.getTagOrCreateAndSetDefault();
            updateTag(tagUpdate, newTag);
            audioFile.commit();

            if(isCacheMode) {
                Snackbar.make(view, context.getString(R.string.progress_move_to_sd), Snackbar.LENGTH_SHORT).show();
                //LogHelper.i(TAG, "saf path:"+mediaPath);
                androidFile.safFromCache(file, mediaPath);
            }
            Snackbar.make(view, context.getString(R.string.progress_update_media_store), Snackbar.LENGTH_SHORT).show();
            updateOnMediaStore(mediaPath, tagUpdate);
        } catch (Exception e) {
            Snackbar.make(view, context.getString(R.string.alert_write_tag_fail), Snackbar.LENGTH_SHORT).show();
            LogHelper.e(TAG, e);
            return false;
        }
        return true;
    }

    public boolean deleteMediaFile(String mediaPath, View view) {
       try {
           Snackbar.make(view, context.getString(R.string.progress_delete_file), Snackbar.LENGTH_SHORT).show();
           if (androidFile.deleteFile(new File(mediaPath))) {
               Snackbar.make(view, context.getString(R.string.progress_update_media_store), Snackbar.LENGTH_SHORT).show();
               deleteFromMediaStore(mediaPath);
               return true;
           }
       }catch(Exception e) {
           Snackbar.make(view, context.getString(R.string.alert_delete_fail), Snackbar.LENGTH_SHORT).show();
           LogHelper.e(TAG, e);
       }
       return false;
    }

    public boolean moveMediaFile(String path, String organizedPath, View view) {
        try {
            Snackbar.make(view, context.getString(R.string.progress_organize_file), Snackbar.LENGTH_SHORT).show();
            if(androidFile.moveFile(path, organizedPath) ) {
                Snackbar.make(view, context.getString(R.string.progress_update_media_store), Snackbar.LENGTH_SHORT).show();
                updatePathOnMediaStore(path, organizedPath);
                return true;
            }
        }catch(Exception e) {
            Snackbar.make(view, context.getString(R.string.alert_organize_fail), Snackbar.LENGTH_SHORT).show();
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
            MediaItem item = newMediaItem(null, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);
            cur.close();
            return item;

    }

    public MediaTag getMediaTag(String path, long mediaDuration) {
        try {
            File file = new File(path);
            AudioFile read = AudioFileIO.read(file);
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
        mediaTag.mediaFormat = getFormat(audioFile);
        mediaTag.mediaBitsPerSample = getBitsPerSample(audioFile);
        mediaTag.mediaBitrate = getBitrate(audioFile,true);
        mediaTag.mediaSampleRate = getSampleRate(audioFile);
        mediaTag.mediaPath = audioFile.getFile().getAbsolutePath();
        mediaTag.displayPlath = androidFile.getDisplayPath(audioFile.getFile().getAbsolutePath());
        mediaTag.mediaSize = getMediaSize(audioFile);
        mediaTag.mediaDuration = getMediaDuration(audioFile, media,mediaDuration);
        mediaTag.setTitle(AndroidFile.getNameWithoutExtension(audioFile.getFile()));

        if(!tag.isEmpty()) {
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
                List<String> artists = tag.getAll(FieldKey.ARTIST);
                mediaTag.setArtist(StringUtils.merge(artists, ";"));
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
                mediaTag.setTrackTotal(tag.getFirst(FieldKey.TRACK_TOTAL));
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
                mediaTag.setCountry(tag.getFirst(FieldKey.MUSICBRAINZ_RELEASE_COUNTRY));
            } catch (UnsupportedOperationException ignored) {
            }
            mediaTag.artwork = getArtwork(tag);
        }else {
            // read tag form file
            //<album>/<track number> <artist> - <title>
            //mediaTag.album = audioFile.getFile().getParentFile().getName();
            TagReader reader = new TagReader();
            TagReader.Tag mTag = reader.parser(path, TagReader.READ_MODE.HIERARCHY);
            mediaTag.setAlbum(mTag.album);
            mediaTag.setTrack(mTag.track);
            mediaTag.setArtist(mTag.artist);
            mediaTag.setTitle(mTag.title);
        }

        return mediaTag;
    }

    private String getFormat(AudioFile audioFile) {
        String fullFormat = audioFile.getAudioHeader().getFormat();
        String bit = getBitsPerSample(audioFile);
        return fullFormat.replace(bit, "").trim();
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

            return String.format("%.2f", length)+ " MB";
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

    private String getBitrate(AudioFile read, boolean withUnit) {
        try {
            if(withUnit) {
                return read.getAudioHeader().getBitRate() + " kbps";
            } else {
                return read.getAudioHeader().getBitRate();
            }
        }catch (Exception ex) {
            return "";
        }
    }

    private String getSampleRate(AudioFile read) {
        try {
            long rate = read.getAudioHeader().getSampleRateAsNumber();
            return formatSampleRate(rate)+" kHz";
        }catch (Exception ex) {
            return "";
        }
    }

    private String getBitsPerSample(AudioFile read) {
        try {
            long rate = read.getAudioHeader().getBitsPerSample();
            return rate+" bits";
        }catch (Exception ex) {
            return "";
        }
    }

    public String getOrganizedPath(String path) {
        // /format/<album|albumartist|artist>/<track no> <artist>-<title>
        final String ReservedChars = "|\\*<\":>[]~#%^@.";
        try {
            String musicPath ="/Music/";
            String storeagePath = androidFile.getStoragePath(context, path);
            if(storeagePath.endsWith(File.separator)) {
                storeagePath = storeagePath.substring(0,storeagePath.length()-1);
            }
            storeagePath = storeagePath+musicPath;

            AudioFile read = AudioFileIO.read(new File(path));
            MediaTag tag = readMediaTag(read, path, null, 0);

            String ext = AndroidFile.getFileExtention(path);
            StringBuffer filename = new StringBuffer();

            // country or format
            filename.append(getFormatAndCounty(path,ext,tag)).append(File.separator);

            // albumArtist or artist
            boolean useAlbumArtist = false;
            if(!StringUtils.isEmpty(tag.getAlbumArtist())) {
                filename.append(tag.getAlbumArtist()).append(File.separator);
                useAlbumArtist = true;
            }else if(!StringUtils.isEmpty(tag.getArtist())) {
                filename.append(tag.getArtist()).append(File.separator);
            }

            // album
            if(!StringUtils.isEmpty(tag.getAlbum())) {
                // album!=albumarist, add album as parent folder
                if(!tag.getAlbum().equalsIgnoreCase(tag.getAlbumArtist())) {
                    filename.append(tag.getAlbum()).append(File.separator);
                }
            }

            // track
            boolean hasTrackOrArtist = false;
            if(!StringUtils.isEmpty(tag.getTrack())) {
                filename.append(tag.getTrack());
                hasTrackOrArtist = true;
            }

            // artist
            if((!StringUtils.isEmpty(tag.getArtist())) && useAlbumArtist) {
                // add artist to file name only have albumArtist
                if(hasTrackOrArtist) {
                    filename.append(" ");
                }
                filename.append(tag.getArtist());
                hasTrackOrArtist = true;
            }

            // artist
            if(hasTrackOrArtist) {
                filename.append(" - ");
            }

            // title
            if(!StringUtils.isEmpty(tag.getTitle())) {
                filename.append(tag.getTitle());
            }else {
                filename.append(AndroidFile.getNameWithoutExtension(read.getFile()));
            }

            String newPath =  storeagePath+filename.toString();
            for(int i=0;i<ReservedChars.length();i++) {
                newPath = newPath.replace(String.valueOf(ReservedChars.charAt(i)),"");
            }

            newPath = newPath+"."+ext;

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

    private String getFormatAndCounty(String path, String ext, MediaTag tag) {
        ext = ext.toUpperCase();
        String tagString = "";
         if(!StringUtils.isEmpty(tag.getCountry())) {
            tagString = ext+"_"+tag.getCountry().trim();
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

    public  MediaItem getMediaItemByPath(Context context, String path) {
        try {
            AudioFile read = AudioFileIO.read(new File(path));
            // add each song to mItems
            String mediaPath =  path;
            String mediaTitle = read.getTag().getFirst(FieldKey.KEY.TITLE);
            String mediaAlbum = read.getTag().getFirst(FieldKey.KEY.ALBUM);
            String mediaArtist = read.getTag().getFirst(FieldKey.KEY.ARTIST);
            long mediaDuration = read.getAudioHeader().getTrackLength();
            return newMediaItem(null, mediaTitle,mediaArtist, mediaAlbum,mediaPath,mediaDuration);
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
    protected MediaItem newMediaItem(IHeader header, String mediaTitle, String mediaArtist, String mediaAlbum, String mediaPath, long mediaDuration) {
        final MediaItem item = new MediaItem(mediaPath, (HeaderItem) header);
        item.setTitle(mediaTitle);
        item.setAlbum(mediaAlbum);
        item.setArtist(mediaArtist);
        item.setPath(mediaPath);
        item.setDuration(mediaDuration);
        item.setDisplayPath(androidFile.getDisplayPath(item.getPath()));
        item.setMediaType(androidFile.getFileExtention(item.getPath()));
/*
        try {
            MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
            metaRetriver.setDataSource(mediaPath);
            String bitRate = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            String sampleRate = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            item.setMediaBitRate(bitRate+" bits");
            item.setMediaSampleRate(sampleRate);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
/*
        try {
            MediaExtractor mex = new MediaExtractor();
            mex.setDataSource(mediaPath);// the adresss location of the sound on sdcard.
            MediaFormat mf = mex.getTrackFormat(0);
            int bitRate = mf.getInteger(MediaFormat.KEY_BIT_RATE);
            int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            item.setMediaBitRate(bitRate+" bits");
            item.setMediaSampleRate(formatSampleRate(sampleRate)+" kHz");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
*/
        item.setGlide(glide);

/*
        new AsyncTask<MediaItem,Void,Boolean>() {
            @Override
            protected Boolean doInBackground(MediaItem[] objects) {
                try {
                    MediaItem item = objects[0];
                    MediaTag tag = getMediaTag(item.getPath(), item.getDuration());
                    item.setTitle(tag.getTitle());
                    item.setAlbum(tag.getAlbum());
                    item.setArtist(tag.getArtist());
                    item.setMediaFormat(tag.getMediaFormat());
                    item.setMediaBitRate(tag.getMediaBitrate());
                    item.setMediaSampleRate(tag.getMediaSampleRate());
                    item.setMediaFileSize(tag.getMediaSize());
                    item.setMediaDuration(tag.getMediaDuration());
                }catch (Exception ex) {}
                return true;
            }
        }.execute(item);
*/
        return item;
    }

    public String getDisplayPath(String path) {
        return androidFile.getDisplayPath(path);
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

    public InputStream openCoverArtInputStream(MediaItem model) throws Exception {
        Bitmap bitmap = null;
        try {
            AudioFile read = AudioFileIO.read(new File(model.getPath()));
            Tag tag = read.getTag();
            if (tag == null) {
                return null;
            }
            Artwork artwork = tag.getFirstArtwork();
             if (null != artwork) {
                    byte[] artworkData = artwork.getBinaryData();
                    bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.length);
             }
        }
        catch (Exception ex) {}

        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
        }

         ByteArrayOutputStream stream = new ByteArrayOutputStream();
         bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
         return new ByteArrayInputStream(stream.toByteArray());
    }

    private final Cursor makeArtistCursor() {
        return  context.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.ArtistColumns.ARTIST}, null, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
    }

    public String[] getArtistsAsArray() {
        //ArrayList<String> mArtistsList = new ArrayList<String>();
        if(mMediaArtists.isEmpty()) {
            Cursor mCursor = makeArtistCursor();
            if (mCursor != null && mCursor.moveToFirst()) {
                do {
                    final String artistName = mCursor.getString(0);
                    mMediaArtists.add(artistName);
                } while (mCursor.moveToNext());
            }
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
        }
        return (String[])mMediaArtists.toArray(new String[0]);
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
                mCursor.close();
                mCursor = null;
            }
        }
        return (String[])mMediaAlbums.toArray(new String[0]);
    }

    private Cursor makeAlbumCursor() {
        return  context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.AlbumColumns.ALBUM}, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }

    public String[]  getAlbumArtistAsArray() {
        if(mMediaAlbumArtists.isEmpty()) {
            Cursor mCursor = makeAlbumArtistCursor();
            if (mCursor != null && mCursor.moveToFirst()) {
                do {
                    final String artistName = mCursor.getString(0);
                    mMediaAlbumArtists.add(artistName);
                } while (mCursor.moveToNext());
            }
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
        }
        return (String[])mMediaAlbumArtists.toArray(new String[0]);
    }

    private Cursor makeAlbumArtistCursor() {
        return  context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.AlbumColumns.ALBUM_ART}, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }
}
