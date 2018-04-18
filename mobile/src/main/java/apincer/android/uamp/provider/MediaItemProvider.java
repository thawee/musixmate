package apincer.android.uamp.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.reference.ID3V2Version;
import org.jaudiotagger.tag.vorbiscomment.VorbisAlbumArtistSaveOptions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import apincer.android.uamp.MusixMateApp;
import apincer.android.uamp.jaudiotagger.MusicMateArtwork;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.model.MediaMetadataDao;
import apincer.android.uamp.utils.BitmapHelper;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;

/**
 * Wrapper class for accessing media information via media store and jaudiotagger
 * Created by e1022387 on 5/10/2017.
 */
public class  MediaItemProvider extends MediaFileProvider {
    private static MediaItemProvider instance;
    public static int QUALITY_BIT_DEPTH_GOOD = 16;
    public static int QUALITY_SAMPLING_RATE_HIGH = 44100;
    public static int QUALITY_COMPRESS_BITRATE_GOOD = 256;
    public static int QUALITY_COMPRESS_BITRATE_LOW = 128;

    private final List<MediaItem> mMediaItems = new ArrayList<>();
    private final List<String> mMediaArtists = new ArrayList<>();
    private final List<String> mMediaAlbums = new ArrayList<>();
    private final List<String> mMediaAlbumArtists = new ArrayList<>();
    private final List<String> mMediaGenres = new ArrayList<>();
    private boolean hasChanged = false;

    private static double MIN_TITLE = 0.70;
    private static double MIN_ARTIST = 0.60;

    public MediaItemProvider() {
        instance = this;
    }

    public static MediaItemProvider getInstance() {
        return instance;
    }

    public InputStream getArtworkAsStream(MediaItem mediaItem) {
        AudioFile audioFile = buildAudioFile(mediaItem.getPath(),"r");

        if(audioFile==null) {
            return null;
        }
        Artwork artwork = audioFile.getTagOrCreateDefault().getFirstArtwork();
        if (null != artwork) {
            byte[] artworkData = artwork.getBinaryData();
            return new ByteArrayInputStream(artworkData);
        }
        return null;
    }

    public byte[] getArtworkAsByte(MediaItem mediaItem) {
        AudioFile audioFile = buildAudioFile(mediaItem.getPath(),"r");

        if(audioFile==null) {
            return null;
        }
        Artwork artwork = audioFile.getTagOrCreateDefault().getFirstArtwork();
        if (null != artwork) {
            return artwork.getBinaryData();
        }
        return null;
    }

    private static final String TAG = LogHelper.makeLogTag(MediaItemProvider.class);

    public AudioFile buildAudioFile(String path, String mode) {
            try {
                if(isValidForTag(path)) {
                    setupTagOptionsForReading();
                    if(mode!=null && mode.indexOf("w")>=0) {
                        setupTagOptionsForWriting();
                    }
                    AudioFile audioFile = AudioFileIO.read(new File(path));
                    return audioFile;
                }
            } catch (CannotReadException | IOException | TagException |ReadOnlyFileException |InvalidAudioFrameException e) {
                e.printStackTrace();
            }
        return null;
    }

    private static void setupTagOptionsForReading() {
        TagOptionSingleton.getInstance().setId3v23DefaultTextEncoding(TextEncoding.ISO_8859_1); // default = ISO_8859_1
        TagOptionSingleton.getInstance().setId3v24DefaultTextEncoding(TextEncoding.UTF_8); // default = ISO_8859_1
        TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_16); // default UTF-16

//        TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_8);

        //  TagOptionSingleton.getInstance().setAndroid(true);
    }

    private static void setupTagOptionsForWriting() {
        TagOptionSingleton.getInstance().setResetTextEncodingForExistingFrames(true);
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V24);
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
        TagOptionSingleton.getInstance().setPadNumbers(true);
        TagOptionSingleton.getInstance().setRemoveTrailingTerminatorOnWrite(true);
        TagOptionSingleton.getInstance().setRemoveID3FromFlacOnSave(true);
        //TagOptionSingleton.getInstance().setId3v1Save(true);
        //TagOptionSingleton.getInstance().setId3v2Save(true);
        TagOptionSingleton.getInstance().setLyrics3Save(true);
        TagOptionSingleton.getInstance().setVorbisAlbumArtistSaveOptions(VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST_AND_DELETE_JRIVER_ALBUMARTIST);
    }

    public static String buildSearchMediaItemSelection(String currentTitle){
        StringBuilder selection=new StringBuilder();
        String [] texts = TextUtils.split(currentTitle, " ");
        if(texts.length==1) {
            selection.append("(" + MediaStore.Files.FileColumns.DATA + " LIKE " + DatabaseUtils.sqlEscapeString("%" + texts[0] + "%")+ ") OR ");
            selection.append("(" + MediaStore.Audio.Media.TITLE +" LIKE "+ DatabaseUtils.sqlEscapeString("%"+texts[0]+"%")+ ") OR ");
        } else {
            for (int i = 0; i < texts.length; i++) {
                if (!StringUtils.isDigitOnly(texts[i])) {
                    selection.append("(" + MediaStore.Files.FileColumns.DATA + " LIKE " + DatabaseUtils.sqlEscapeString("%" + texts[i] + "%") + ") OR ");
                    selection.append("(" + MediaStore.Audio.Media.TITLE +" LIKE "+ DatabaseUtils.sqlEscapeString("%"+texts[0]+"%")+ ") OR ");
                }
            }
        }
        return selection.substring(0,selection.lastIndexOf(")") + 1);
    }

    public MediaItem searchMediaItem(String currentTitle, String currentArtist, String currentAlbum) {
        List<MediaMetadata> list = MusixMateApp.getDaoSession().getMediaMetadataDao().queryBuilder().list();
        //
        double prvTitleScore = 0.0;
        double prvArtistScore = 0.0;
        double prvAlbumScore = 0.0;
        double titleScore = 0.0;
        double artistScore = 0.0;
        double albumScore = 0.0;
        MediaMetadata matchedMeta = null;

        for(MediaMetadata metadata: list) {
            titleScore = StringUtils.similarity(currentTitle, metadata.getTitle());
            artistScore = StringUtils.similarity(currentArtist, metadata.getArtist());
            albumScore = StringUtils.similarity(currentAlbum, metadata.getAlbum());

            if (getSimilarScore(titleScore, artistScore, albumScore) > getSimilarScore(prvTitleScore, prvArtistScore, prvAlbumScore)) {
                    matchedMeta = metadata;
                    prvTitleScore = titleScore;
                    prvArtistScore = artistScore;
                    prvAlbumScore = albumScore;
            }
        }
        if(matchedMeta!=null) {
            return new MediaItem(matchedMeta);
        }
        return null;
    }

    /*
    @Deprecated
    public MediaItem searchMediaItemMediaStore(String currentTitle, String currentArtist, String currentAlbum) {
        ContentResolver mContentResolver = getContext().getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        if(StringUtils.isEmpty(currentTitle)) {
            return null;
        }
        if("unknown artist".equalsIgnoreCase(currentArtist)) {
            currentArtist = "";
        }
        if("unknown album".equalsIgnoreCase(currentAlbum)) {
            currentAlbum= "";
        }

       // String query = "";
        StringBuilder selection=new StringBuilder(buildSearchMediaItemSelection(currentTitle));
        selection.append(" AND ");
        selection.append( "( " +MediaItemService.buildMediaSelection(-1) +" )");
        String query = selection.toString();
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
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

        //
        double prvTitleScore = 0.0;
        double prvArtistScore = 0.0;
        double prvAlbumScore = 0.0;
        double titleScore = 0.0;
        double artistScore = 0.0;
        double albumScore = 0.0;

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
            titleScore = StringUtils.similarity(currentTitle, newTitle);
            artistScore = StringUtils.similarity(currentArtist, newArtist);
            albumScore = StringUtils.similarity(currentAlbum, newAlbum);

            if(getSimilarScore(titleScore,artistScore,albumScore) > getSimilarScore(prvTitleScore,prvArtistScore,prvAlbumScore)) {
                mediaTitle=newTitle;
                mediaAlbum=newAlbum;
                mediaArtist=newArtist;
                mediaPath = cur.getString(dataColumn);
                mediaDuration = cur.getLong(durationColumn);
                id = cur.getInt(idColumn);
                prvTitleScore = titleScore;
                prvArtistScore=artistScore;
                prvAlbumScore=albumScore;
            }
        } while (cur.moveToNext());
        cur.close();

        // load media detail
        MediaMetadata metadata = new MediaMetadata();
        metadata.setMediaPath(mediaPath);
        final MediaItem item = new MediaItem(metadata);

        metadata.setMediaType(getExtension(item.getPath()).toUpperCase());
        metadata.setAudioDuration(mediaDuration);
        metadata.setAudioFormatInfo(metadata.getMediaType()==null?"":metadata.getMediaType().toUpperCase());
        metadata.setDisplayPath(buildDisplayName(item.getPath()));
        metadata.setMediaPath(item.getPath());
        readMetadata(item,null); // pending for read tags
        return item;
    } */

    private double getSimilarScore(double titleScore, double artistScore, double albumScore) {
        return (titleScore*60)+(artistScore*20)+(albumScore*20);
    }

    private static boolean isValidForTag(String path) {
        String ext = getExtension(path);
        if(StringUtils.isEmpty(ext)) {
            return false;
        }
        try {
            SupportedFileFormat.valueOf(ext.toUpperCase());
            return true;
        }catch(Exception ex) {
            return false;
        }
    }

    public boolean saveArtworkToFile(MediaItem item, String filePath) {
        boolean isFileSaved = false;
        try {
            File f = new File(filePath);
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(getArtworkAsByte(item));
            fos.flush();
            fos.close();
            isFileSaved = true;
            // File Saved
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOException");
            e.printStackTrace();
        }
        return isFileSaved;
    }

    private void deleteFromMediaStore(String pathToDelete) {
        try {
            ContentResolver contentResolver = getContext().getContentResolver();
            if (contentResolver != null) {
                Cursor query = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media._ID, "_data"}, "_data = ?", new String[]{pathToDelete}, null);
                if (query != null && query.getCount() > 0) {
                    query.moveToFirst();
                    while (!query.isAfterLast()) {
                        getContext().getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, (long) query.getInt(query.getColumnIndex(MediaStore.Audio.Media._ID))), null, null);
                        query.moveToNext();
                    }
                    query.close();
                }
            }
        }catch (Exception ex) {
            // ignore
        }
    }

    public static String formatDuration(long milliseconds, boolean withUnit) {
        long s = milliseconds / 1000 % 60;
        long m = milliseconds / 1000 / 60 % 60;
        long h = milliseconds / 1000 / 60 / 60 % 24;
        String format = "%02d:%02d";
        String formatHrs = "%02d:%02d:%02d";
        if(withUnit) {
            format = format +" min.";
            formatHrs = formatHrs +" hrs.";
        }
        if (h == 0) return String.format(Locale.getDefault(), format, m, s);
        return String.format(Locale.getDefault(), formatHrs, h, m, s);
    }

    public static String formatAudioSampleRate(long rate) {
        double s = rate / 1000.00;
        String str = String.format(Locale.getDefault(),"%.1f", s);
        str = str.replace(".0", "");
        return str;
    }

    /*
    @Deprecated
    private void updateTag(AudioFile audioFile, MediaTag changedTag, Tag id3Tag) {
        if (id3Tag == null || changedTag==null) {
            return;
        }
        setTagField(audioFile, id3Tag,FieldKey.TITLE, changedTag.getTitle());
        setTagField(audioFile,id3Tag,FieldKey.ALBUM, changedTag.getAlbum());
        setTagField(audioFile,id3Tag,FieldKey.ARTIST, changedTag.getArtist());
        setTagField(audioFile,id3Tag,FieldKey.ALBUM_ARTIST, changedTag.getAlbumArtist());
        setTagField(audioFile,id3Tag,FieldKey.GENRE, changedTag.getGenre());
        setTagField(audioFile,id3Tag,FieldKey.YEAR, changedTag.getYear());
        setTagField(audioFile,id3Tag,FieldKey.TRACK, changedTag.getTrack());
        setTagField(audioFile,id3Tag,FieldKey.TRACK_TOTAL, changedTag.getTrackTotal());
        setTagField(audioFile,id3Tag,FieldKey.DISC_NO, changedTag.getDisc());
        setTagField(audioFile,id3Tag,FieldKey.DISC_TOTAL, changedTag.getDiscTotal());
        setTagField(audioFile,id3Tag,FieldKey.LYRICS, changedTag.getLyrics());
        setTagField(audioFile,id3Tag,FieldKey.COMMENT, changedTag.getComment());
        setTagField(audioFile,id3Tag,FieldKey.GROUPING, changedTag.getGrouping());
        setTagField(audioFile,id3Tag,FieldKey.COMPOSER, changedTag.getComposer());
    } */

    private boolean isValidTagValue(String newTag) {
        if(!StringUtils.MULTI_VALUES.equalsIgnoreCase(newTag)) { // && !StringUtils.trimToEmpty(oldTag).equals(newTag)) {
            return true;
        }
        return false;
    }

    private String getId3TagValue(Tag id3Tag, FieldKey key) {
        if(id3Tag == null) {
            return "";
        }
        return StringUtils.trimToEmpty(id3Tag.getFirst(key));
    }

    private void setTagField(AudioFile audioFile, Tag id3Tag,FieldKey key, String text) {
        try {
            if(isValidTagValue(text)) {
                if (StringUtils.isEmpty(text)) {
                    id3Tag.deleteField(key);
                } else {
                    id3Tag.setField(key, text);
                }
                audioFile.commit();
            }
        } catch (Exception ignored) {
            LogHelper.e(TAG, ignored);
        }
    }

    private boolean saveMediaArtwork(AudioFile audioFile, Tag tag, String artworkPath) {
        if (artworkPath == null) {
            return false;
        }
        try {
            if (tag != null) {
                Artwork artwork = MusicMateArtwork.createArtworkFromFile(new File(artworkPath));
                tag.deleteArtworkField();
                tag.addField(artwork);
                audioFile.commit();
            }
        } catch(Exception ex) {
            LogHelper.e(TAG, ex);
        }
        return true;
    }

    /*
    @Deprecated
    public boolean saveMediaArtwork(String mediaPath, String artworkPath) {
        if (mediaPath == null || artworkPath == null) {
            return false;
        }
        try {
            boolean isCacheMode = false;
            File file = new File(mediaPath);

            if(!isWritable(file)) {
                isCacheMode = true;
                file = safToCache(file);
            }

            setupTagOptionsForWriting();
            AudioFile audioFile = buildAudioFile(file.getAbsolutePath(),"rw");

            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            if (tag != null) {
                Artwork artwork = MusicMateArtwork.createArtworkFromFile(new File(artworkPath));
                tag.deleteArtworkField();
                tag.addField(artwork);
                audioFile.commit();
            }

            if(isCacheMode) {
                safFromCache(file, mediaPath);
            }
        } catch(Exception ex) {
            LogHelper.e(TAG, ex);
        }
        return true;
    } */

    public boolean cleanMediaTag(String mediaPath) throws Exception{
       /* if (mediaPath == null) {
            return false;
        }

        File file = new File(mediaPath);
        file = cleanTagsToCache(file);
        if(file!=null) {
            safFromCache(file, mediaPath);
        }
        return true; */
        if (mediaPath == null) {
            return false;
        }

        boolean isCacheMode = false;
        File file = new File(mediaPath);
        if(!isWritable(file)) {
            isCacheMode = true;
            file = safToCache(file);
        }
        setupTagOptionsForWriting();
        AudioFile audioFile = buildAudioFile(file.getAbsolutePath(),"rw");
        AudioFileIO.delete(audioFile);
        if(isCacheMode) {
            safFromCache(file, mediaPath);
        }

        return true;
    }

    public boolean saveMetadata(MediaItem item) throws Exception{
        if (item == null || item.getPath() == null) {
            return false;
        }

        if(item.getPendingMetadata()==null) {
            return false;
        }

        boolean isCacheMode = false;
        File file = new File(item.getPath());
        if(!isWritable(file)) {
            isCacheMode = true;
            file = safToCache(file);
        }
        setupTagOptionsForWriting();
        AudioFile audioFile = buildAudioFile(file.getAbsolutePath(),"rw");

        // save default tags
        Tag existingTags = audioFile.getTagOrCreateAndSetDefault();
        saveMetadata(audioFile, existingTags, item.getPendingMetadata());

        if(isCacheMode) {
            safFromCache(file, item.getPath());
        }
        item.setPendingMetadata(null); // reset pending tag

        //updateOnMediaStore(item.getPath(), changedTag);
        return true;
    }

    private void saveMetadata(AudioFile audioFile, Tag tags, MediaMetadata pendingMetadata) {
        if (tags == null || pendingMetadata==null) {
            return;
        }
        setTagField(audioFile,tags,FieldKey.TITLE, pendingMetadata.getTitle());
        setTagField(audioFile,tags,FieldKey.ALBUM, pendingMetadata.getAlbum());
        setTagField(audioFile,tags,FieldKey.ARTIST, pendingMetadata.getArtist());
        setTagField(audioFile,tags,FieldKey.ALBUM_ARTIST, pendingMetadata.getAlbumArtist());
        setTagField(audioFile,tags,FieldKey.GENRE, pendingMetadata.getGenre());
        setTagField(audioFile,tags,FieldKey.YEAR, pendingMetadata.getYear());
        setTagField(audioFile,tags,FieldKey.TRACK, pendingMetadata.getTrack());
        setTagField(audioFile,tags,FieldKey.TRACK_TOTAL, pendingMetadata.getTrackTotal());
        setTagField(audioFile,tags,FieldKey.DISC_NO, pendingMetadata.getDisc());
        setTagField(audioFile,tags,FieldKey.DISC_TOTAL, pendingMetadata.getDiscTotal());
        setTagField(audioFile,tags,FieldKey.LYRICS, pendingMetadata.getLyrics());
        setTagField(audioFile,tags,FieldKey.COMMENT, pendingMetadata.getComment());
        setTagField(audioFile,tags,FieldKey.GROUPING, pendingMetadata.getGrouping());
        setTagField(audioFile,tags,FieldKey.COMPOSER, pendingMetadata.getComposer());
    }

    /*
    @Deprecated
    public boolean saveMediaTag(String mediaPath,MediaTag changedTag, String artworkFile) throws Exception{
        if (mediaPath == null) {
            return false;
        }


            boolean isCacheMode = false;
            File file = new File(mediaPath);
            if(!isWritable(file)) {
                isCacheMode = true;
                file = safToCache(file);
            }
            setupTagOptionsForWriting();
            AudioFile audioFile = buildAudioFile(file.getAbsolutePath(),"rw");

            // save default tags
            Tag existingTags = audioFile.getTagOrCreateAndSetDefault();
            updateTag(audioFile, changedTag, existingTags);
            if (artworkFile != null) {
                saveMediaArtwork(audioFile, existingTags, artworkFile);
            }

            if(isCacheMode) {
                safFromCache(file, mediaPath);
            }

        updateOnMediaStore(mediaPath, changedTag);
        return true;
    } */

    public boolean deleteMediaFile(String mediaPath) {
        File file = new File(mediaPath);
        File directory = file.getParentFile();
        if (delete(file)) {
            cleanEmptyDirectory(directory);
            deleteFromDatabase(mediaPath);
            deleteFromMediaStore(mediaPath);
            return true;
        }
        return false;
    }

    private void deleteFromDatabase(String mediaPath) {
        hasChanged = true;
        MediaMetadataDao dao = MusixMateApp.getDaoSession().getMediaMetadataDao();
        List<MediaMetadata> list = dao.queryBuilder()
                .where(MediaMetadataDao.Properties.MediaPath.eq(mediaPath))
                .list();
        if(list.size()==1) {
            dao.delete(list.get(0));
        }
    }

    /*
    @Deprecated
    public boolean moveMediaFile(String path, String organizedPath)  throws Exception{
        if(move(path, organizedPath) ) {
                updatePathOnMediaStore(path, organizedPath);
                File file = new File(path);
                cleanEmptyDirectory(file.getParentFile());
            return true;
        }
        return false;
    } */

    /*
    @Deprecated
    private boolean updateOnMediaStore(String path, MediaTag tag) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.TITLE, sqlEscapeString(tag.getTitle()));
            values.put(MediaStore.Audio.Media.ALBUM, sqlEscapeString(tag.getAlbum()));
            values.put(MediaStore.Audio.Media.ARTIST, sqlEscapeString(tag.getArtist()));
            boolean successMediaStore = getContext().getContentResolver().update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values,
                    MediaStore.Audio.Media.DATA + "=?", new String[]{path}) == 1;
            return successMediaStore;
    } */

    private boolean updateOnMediaStore(String path, MediaMetadata tag) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.TITLE, sqlEscapeString(tag.getTitle()));
        values.put(MediaStore.Audio.Media.ALBUM, sqlEscapeString(tag.getAlbum()));
        values.put(MediaStore.Audio.Media.ARTIST, sqlEscapeString(tag.getArtist()));
        boolean successMediaStore = getContext().getContentResolver().update(
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
            boolean result = getContext().getContentResolver().update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values,
                    MediaStore.Audio.Media.DATA + "=?", new String[]{path}) == 1;
         }catch (Exception ex) {
            deleteFromMediaStore(path);
        }
        return true;
    }

    /*
    @Deprecated
    public void readMetadata(MediaItem mediaItem, String path) {
        try {
            //if(mediaItem.isLoadedEncoding()) return;
            //mediaItem.setNewTag(null); // reset updated tag
           // MediaTag mediaTag = mediaItem.getTag();
            MediaMetadata metadata = mediaItem.getMetadata();

            if(path == null) {
                path = mediaItem.getPath();
            }//else {
                //mediaItem.setPath(path);
            //}
            metadata.setDisplayPath(buildDisplayName(path));
            setupTagOptionsForReading();
            AudioFile audioFile = buildAudioFile(path, "r");

            if(audioFile==null) {
              //  mediaItem.setIdv3Tag(false);
                return;
            }

            readAudioCoding(audioFile,metadata); //16/24/32 bit and 44.1/48/96/192 kHz
            metadata.setMediaSize(getMediaSize(audioFile));
            metadata.setLossless(audioFile.getAudioHeader().isLossless());

            metadata.setLastModified(audioFile.getFile().lastModified());
            readMetadata(audioFile,metadata);

            //if(!readId3Tag(audioFile, mediaTag)) {
               // mediaItem.setIdv3Tag(false);
           // }
            //mediaItem.setLoadedEncoding(true);
            //mediaItem.setIdv3Tag(true);
        } catch (Exception |OutOfMemoryError oom) {
            LogHelper.e(TAG, oom);
        }
    } */
/*
    @Deprecated
    public void readMetadata(MediaItem mediaItem, boolean forcedRead) {
        try {
            //if(mediaItem.isMetadataLoaded() && !forcedRead) return;

            MediaMetadata metadata = mediaItem.getMetadata();
            String path = mediaItem.getPath();
            metadata.setDisplayPath(buildDisplayName(path));
            AudioFile audioFile = buildAudioFile(path, "r");

            if(audioFile==null) {
              //  mediaItem.setValidForMetadata(false);
                return;
            }

            readAudioCoding(audioFile,metadata); //16/24/32 bit and 44.1/48/96/192 kHz
            metadata.setMediaSize(getMediaSize(audioFile));
            metadata.setLossless(audioFile.getAudioHeader().isLossless());

            //mediaItem.setValidForMetadata(true);
            if(!readMetadata(audioFile, metadata)) {
                metadata.setLastModified(audioFile.getFile().lastModified());
              //  mediaItem.setValidForMetadata(false);
            }
            //mediaItem.setMetadataLoaded(true);
        } catch (Exception |OutOfMemoryError oom) {
            LogHelper.e(TAG, oom);
        }
    } */

    public void readMetadata(MediaMetadata metadata) {
        try {

            String path = metadata.getMediaPath();
            metadata.setDisplayPath(buildDisplayName(path));
            AudioFile audioFile = buildAudioFile(path, "r");

            if(audioFile==null) {
                return;
            }

            readAudioCoding(audioFile,metadata); //16/24/32 bit and 44.1/48/96/192 kHz
            metadata.setMediaSize(getMediaSize(audioFile));
            metadata.setLossless(audioFile.getAudioHeader().isLossless());

            if(!readMetadata(audioFile, metadata)) {
                metadata.setLastModified(audioFile.getFile().lastModified());
            }
        } catch (Exception |OutOfMemoryError oom) {
            LogHelper.e(TAG, oom);
        }
    }

    /*
    @Deprecated
    private boolean readId3Tag(AudioFile audioFile, MediaTag mediaTag) {
        Tag tag = audioFile.getTag(); //TagOrCreateDefault();
        if (tag != null && !tag.isEmpty()) {
            mediaTag.setTitle(getId3TagValue(tag,FieldKey.TITLE));
            if(StringUtils.isEmpty(mediaTag.getTitle())) {
                //default to file name
                mediaTag.setTitle(removeExtension(audioFile.getFile()));
            }
            mediaTag.setAlbum(getId3TagValue(tag, FieldKey.ALBUM));
            mediaTag.setArtist(getId3TagValue(tag, FieldKey.ARTIST));
            mediaTag.setAlbumArtist(getId3TagValue(tag, FieldKey.ALBUM_ARTIST));
            mediaTag.setGenre(getId3TagValue(tag, FieldKey.GENRE));
            mediaTag.setYear(getId3TagValue(tag, FieldKey.YEAR));
            mediaTag.setTrack(getId3TagValue(tag, FieldKey.TRACK));
            mediaTag.setComposer(getId3TagValue(tag, FieldKey.COMPOSER));
            mediaTag.setDisc(getId3TagValue(tag, FieldKey.DISC_NO));
            mediaTag.setDiscTotal(getId3TagValue(tag, FieldKey.DISC_TOTAL));
            mediaTag.setLyrics(getId3TagValue(tag, FieldKey.LYRICS));
            mediaTag.setComment(getId3TagValue(tag, FieldKey.COMMENT));
            mediaTag.setCountry(getId3TagValue(tag, FieldKey.GROUPING));
            return true;
        }
        return false;
    } */

    private boolean readMetadata(AudioFile audioFile, MediaMetadata mediaTag) {
        Tag tag = audioFile.getTag(); //TagOrCreateDefault();
        if (tag != null && !tag.isEmpty()) {
            mediaTag.setTitle(getId3TagValue(tag,FieldKey.TITLE));
            if(StringUtils.isEmpty(mediaTag.getTitle())) {
                //default to file name
                mediaTag.setTitle(removeExtension(audioFile.getFile()));
            }
            mediaTag.setAlbum(getId3TagValue(tag, FieldKey.ALBUM));
            mediaTag.setArtist(getId3TagValue(tag, FieldKey.ARTIST));
            mediaTag.setAlbumArtist(getId3TagValue(tag, FieldKey.ALBUM_ARTIST));
            mediaTag.setGenre(getId3TagValue(tag, FieldKey.GENRE));
            mediaTag.setYear(getId3TagValue(tag, FieldKey.YEAR));
            mediaTag.setTrack(getId3TagValue(tag, FieldKey.TRACK));
            mediaTag.setComposer(getId3TagValue(tag, FieldKey.COMPOSER));
            mediaTag.setDisc(getId3TagValue(tag, FieldKey.DISC_NO));
            mediaTag.setDiscTotal(getId3TagValue(tag, FieldKey.DISC_TOTAL));
            mediaTag.setLyrics(getId3TagValue(tag, FieldKey.LYRICS));
            mediaTag.setComment(getId3TagValue(tag, FieldKey.COMMENT));
            mediaTag.setGrouping(getId3TagValue(tag, FieldKey.GROUPING));
            return true;
        }
        return false;
    }

    public Bitmap getArtwork(MediaItem mediaItem) {
            AudioFile audioFile = buildAudioFile(mediaItem.getPath(),"r");

            if(audioFile==null) {
                return null;
            }
            return getArtwork(audioFile.getTagOrCreateDefault(),false);
    }

    public Bitmap getArtwork(String path) {
        AudioFile audioFile = buildAudioFile(path,"r");

        if(audioFile==null) {
            return null;
        }
        return getArtwork(audioFile.getTagOrCreateDefault(),false);
    }

    public Bitmap getSmallArtwork(MediaItem mediaItem) {
        AudioFile audioFile = buildAudioFile(mediaItem.getPath(),"r");

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
            }else {
                bitmap = BitmapHelper.decodeBitmap(artworkData, 800,800);
            }
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

    private void readAudioCoding(AudioFile read, MediaMetadata metadata) {
        try {
            long sampling = read.getAudioHeader().getSampleRateAsNumber(); //44100/48000 Hz
            long bitdepth = read.getAudioHeader().getBitsPerSample(); //16/24/32
            long bitrate = read.getAudioHeader().getBitRateAsNumber(); //128/256/320
            String codec = read.getAudioHeader().getEncodingType();

            if(metadata.getAudioDuration()==0.00) {
                metadata.setAudioDuration(read.getAudioHeader().getTrackLength());
            }
            metadata.setAudioBitRate(String.format(Locale.getDefault(),"%dkbps",new Object[]{bitrate}));
            metadata.setAudioFormatInfo(String.format(Locale.getDefault(), "%s/%dkbps", new Object[]{metadata.getMediaType(), bitrate}));
            metadata.setAudioCodecInfo(String.format(Locale.getDefault(), "%s/%dkbps", new Object[]{codec.toUpperCase(), bitrate}));
            metadata.setLossless(read.getAudioHeader().isLossless());
            if(sampling<QUALITY_SAMPLING_RATE_HIGH || bitdepth < QUALITY_BIT_DEPTH_GOOD) {
                metadata.setAudioEncodingQuality(MediaItem.MEDIA_QUALITY_LOW);
            }else if(read.getAudioHeader().isLossless()) {
                if(sampling > QUALITY_SAMPLING_RATE_HIGH || bitdepth > QUALITY_BIT_DEPTH_GOOD) {
                    metadata.setAudioEncodingQuality(MediaItem.MEDIA_QUALITY_HIRES);
                }else {
                    metadata.setAudioEncodingQuality(MediaItem.MEDIA_QUALITY_HIGH);
                }
            }else {
                if(bitrate>=QUALITY_COMPRESS_BITRATE_GOOD) {
                    metadata.setAudioEncodingQuality(MediaItem.MEDIA_QUALITY_GOOD);
                }else if(bitrate<QUALITY_COMPRESS_BITRATE_LOW) {
                    metadata.setAudioEncodingQuality(MediaItem.MEDIA_QUALITY_LOW);
                }
            }
            metadata.setAudioBitDepth(bitdepth+"bit");
            metadata.setAudioSampleRate(formatAudioSampleRate(sampling)+"kHz");
            metadata.setAudioSampleRate(sampling);
        }catch (Exception ex) {}
    }

    public String buildManagedPath(MediaItem item) {
        // [Hi-Res|Lossless|Compress]/<album|albumartist|artist>/<track no>-<artist>-<title>
        // /format/<album|albumartist|artist>/<track no> <artist>-<title>
        final String ReservedChars = "?|\\*<\":>[]~#%^@.";
        try {
            String musicPath ="/Music/";
            String storeagePath = getRootPath(item.getPath());
            if(storeagePath.endsWith(File.separator)) {
                storeagePath = storeagePath.substring(0,storeagePath.length()-1);
            }
            storeagePath = storeagePath+musicPath; // .../Music

            MediaMetadata tag = item.getMetadata();

            String ext = getExtension(item.getPath());
            StringBuffer filename = new StringBuffer();

            if(item.getMetadata().getAudioEncodingQuality() == MediaItem.MEDIA_QUALITY_HIRES) {
                filename.append("Hi-Res").append(File.separator);
            }else {
                if(item.getMetadata().isLossless()) {
                    filename.append("Lossless").append(File.separator);
                }else {
                    filename.append("Lossy").append(File.separator);
                }
            }

            // albumArtist or artist
            boolean useAlbumArtist = false;
            String title = StringUtils.trimTitle(tag.getTitle());
            String artist = StringUtils.trimTitle(tag.getArtist());
            String album = StringUtils.trimTitle(tag.getAlbum());
            String albumArtist = StringUtils.trimTitle(tag.getAlbumArtist());

            if(!StringUtils.isEmpty(albumArtist)) {
                filename.append(formatTitle(albumArtist)).append(File.separator);
                useAlbumArtist = true;
            }else if(!StringUtils.isEmpty(artist)) {
                filename.append(formatTitle(artist)).append(File.separator);
            }

            // album
            if(!StringUtils.isEmpty(album)) {
                // album!=albumarist, add album as parent folder
                if(!album.equalsIgnoreCase(albumArtist)) {
                    filename.append(formatTitle(album)).append(File.separator);
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
            if((!StringUtils.isEmpty(artist)) && useAlbumArtist && !artist.equalsIgnoreCase(albumArtist)) {
                // add artist to file name only have albumArtist
                if(hasTrackOrArtist) {
                    filename.append(" ");
                }
                filename.append(formatTitle(artist));
                hasTrackOrArtist = true;
            }

            // artist
            if(hasTrackOrArtist) {
                filename.append(" - ");
            }

            // title
            if(!StringUtils.isEmpty(title)) {
                filename.append(formatTitle(title));
            }else {
                filename.append(formatTitle(removeExtension(item.getPath())));
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

    public static boolean isMediaFileExist(MediaItem item) {
        if(item == null || item.getPath()==null) {
            return false;
        }
        File file = new File(item.getPath());
        if(file.exists() && file.length() ==0) {
            return false;
        }
        return file.exists();
    }

    public static boolean isMediaFileExist(String path) {
        if(path == null) {
            return false;
        }
        File file = new File(path);
        if(file.exists() && file.length() ==0) {
            return false;
        }
        return file.exists();
    }

    public static String formatTitle(CharSequence text) {
        // trim space
        // format as word, first letter of word is capital
        if(text==null) {
            return "";
        }

        String str = text.toString().trim();
        if(str.contains("/")) {
            str = str.replace("/","_");
        }
        char [] delimiters = {' ','.','(','['};
        return StringUtils.capitalize(str, delimiters);
    }

    public static File getDownloadPath(String path) {
        File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(download, path);
    }

    public boolean saveMediaArtwork(MediaItem item) {
        if (item == null || item.getPath() == null || item.getPendingArtworkPath()==null) {
            return false;
        }
        try {
            boolean isCacheMode = false;
            File file = new File(item.getPath());

            if(!isWritable(file)) {
                isCacheMode = true;
                file = safToCache(file);
            }

            AudioFile audioFile = buildAudioFile(file.getAbsolutePath(),"rw");
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            if (tag != null) {
                Artwork artwork = MusicMateArtwork.createArtworkFromFile(new File(item.getPendingArtworkPath()));
                tag.deleteArtworkField();
                tag.addField(artwork);
                audioFile.commit();
            }

            if(isCacheMode) {
                safFromCache(file, item.getPath());
            }
        } catch(Exception ex) {
            LogHelper.e(TAG, ex);
        }
        return true;
    }

    public void addToDatabase(MediaMetadata metadata) {
        hasChanged = true;
        MediaMetadataDao dao = MusixMateApp.getDaoSession().getMediaMetadataDao();
        dao.insertOrReplace(metadata);
    }

    public long getLastestModifiedFromDatabase() {
        MediaMetadataDao dao = MusixMateApp.getDaoSession().getMediaMetadataDao();
        List<MediaMetadata> rows = dao.queryBuilder().where(MediaMetadataDao.Properties.LastModified.isNotNull()).orderDesc(MediaMetadataDao.Properties.LastModified).limit(1).list();
        if(rows!=null && rows.size()>0) {
            return rows.get(0).getLastModified();
        }
        return -1;
    }

    public Collection<? extends MediaItem> getHiresMediaItems() {
        List<MediaItem> mediaItems = new ArrayList();
        List<MediaItem> list = getMetadataList();
        for(MediaItem mdata: list) {
            if (mdata.getMetadata().getAudioEncodingQuality() == MediaItem.MEDIA_QUALITY_HIRES) {
                mediaItems.add(mdata);
            }
        }
        return mediaItems;
    }

    private List<MediaItem> getMetadataList() {
        if(hasChanged) {
            hasChanged = false;
            mMediaItems.clear();
            MediaMetadataDao dao = MusixMateApp.getDaoSession().getMediaMetadataDao();
            List<MediaMetadata> list = dao.queryBuilder().orderAsc(MediaMetadataDao.Properties.Title).list();
            for(MediaMetadata mdata: list) {
                mMediaItems.add(new MediaItem(mdata));
                addToArtistList(mdata.getArtist());
                addToAlbumArtistList(mdata.getAlbumArtist());
                addToAlbumList(mdata.getAlbum());
                addToGenreList(mdata.getGenre());
            }
        }
        return mMediaItems;
    }

    private void addToAlbumList(String album) {
        if(!StringUtils.isEmpty(album) && !mMediaAlbums.contains(album)) {
            mMediaAlbums.add(album);
        }
    }

    private void addToGenreList(String genre) {
        if(!StringUtils.isEmpty(genre) && !mMediaGenres.contains(genre)) {
            mMediaGenres.add(genre);
        }
    }

    private void addToArtistList(String artist) {
        if(!StringUtils.isEmpty(artist) && !mMediaArtists.contains(artist)) {
            mMediaArtists.add(artist);
        }
    }

    private void addToAlbumArtistList(String albumArtist) {
        if(!StringUtils.isEmpty(albumArtist) && !mMediaAlbumArtists.contains(albumArtist)) {
            mMediaAlbumArtists.add(albumArtist);
        }
    }

    public Collection<? extends MediaItem> getMediaItems() {
        return getMetadataList();
    }

    public Collection<? extends MediaItem> getNewMediaItems() {
        List<MediaItem> mediaItems = new ArrayList();
        List<MediaItem> list = getMetadataList();
        for(MediaItem mdata: list) {
            if (!mdata.getMetadata().getMediaPath().contains("/Music/") || mdata.getMetadata().getMediaPath().contains("/znew")) {
                mediaItems.add(mdata);
            }
        }
        return mediaItems;
    }

    public Collection<? extends MediaItem> getSimilarTitle() {
        List<MediaItem> similarItems = new ArrayList<>();
        List<MediaItem> list = getMetadataList();
        MediaItem preItem = null;
        boolean preAdded = false;
        for (int i=0; i<list.size();i++) {
            MediaItem item = list.get(i);
            //similarity
            if (preItem!=null && StringUtils.similarity(item.getTitle(), preItem.getTitle())>MIN_TITLE) {
                if(!preAdded && preItem != null) {
                    similarItems.add(preItem);
                }
                similarItems.add(item);
                preAdded = true;
            }else {
                preAdded = false;
            }
            preItem = item;
        }

        return similarItems;
    }

    public Collection<? extends MediaItem> getSimilarArtistAndTitle() {
        List<MediaItem> similarItems = new ArrayList<>();
        List<MediaItem> list = getMetadataList();
        MediaItem preItem = null;
        boolean preAdded = false;
        for (int i=0; i<list.size();i++) {
            MediaItem item = list.get(i);
            //similarity
            if (preItem!=null && (StringUtils.similarity(item.getTitle(), preItem.getTitle())>MIN_TITLE) &&
                    (StringUtils.similarity(item.getMetadata().getArtist(), preItem.getMetadata().getArtist())>MIN_ARTIST)) {
               if(!preAdded && preItem != null) {
                    similarItems.add(preItem);
                }
                similarItems.add(item);
                preAdded = true;
            }else {
                preAdded = false;
            }
            preItem = item;
        }

        return similarItems;
    }

    public List<String> getAllArtists() {
        return mMediaArtists;
    }

    public List<String> getAllAlbumArtists() {
        return mMediaAlbumArtists;
    }

    public List<String> getAllAlbums() {
        return mMediaAlbums;
    }

    public void removeFromDatabase(MediaMetadata metadata) {
        MediaMetadataDao dao = MusixMateApp.getDaoSession().getMediaMetadataDao();
        dao.delete(metadata);
    }

    public boolean moveToManagedDirectory(MediaItem item) {
        String newPath = buildManagedPath(item);
        if(move(item.getPath(), newPath) ) {
            updatePathOnMediaStore(item.getPath(), newPath);
            File file = new File(item.getPath());
            cleanEmptyDirectory(file.getParentFile());
            MediaMetadata metadata = item.getMetadata();
            metadata.setMediaPath(newPath);
            metadata.setDisplayPath(buildDisplayName(newPath));
            addToDatabase(metadata);
            return true;
        }
        return false;
    }

    public List<String> getAllGenres() {
        return mMediaGenres;
    }
}
