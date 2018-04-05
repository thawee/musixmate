package apincer.android.uamp.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;
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
import java.util.Locale;

import apincer.android.uamp.MediaItemService;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.model.MediaTag;
import apincer.android.uamp.utils.BitmapHelper;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.MusicMateArtwork;
import apincer.android.uamp.utils.StringUtils;

/**
 * Wrapper class for accessing media information via media store and jaudiotagger
 * Created by e1022387 on 5/10/2017.
 */
public class MediaItemProvider {
    private static MediaItemProvider instance;
    public static int QUALITY_BIT_DEPTH_GOOD = 16;
    public static int QUALITY_SAMPLING_RATE_HIGH = 44100;
    public static int QUALITY_COMPRESS_BITRATE_GOOD = 256;
    public static int QUALITY_COMPRESS_BITRATE_LOW = 128;

    public static void initialize(Context context) {
        if(instance==null) {
            instance = new MediaItemProvider(context);
        }
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
    private Context context;

    private MediaItemProvider(Context context) {
        this.context = context;
    }

    public AudioFile buildAudioFile(String path, String mode) {
            try {
                if(isValidForTag(path)) {
                    AudioFile audioFile = AudioFileIO.read(new File(path));
                    return audioFile;
                }
            } catch (CannotReadException | IOException | TagException |ReadOnlyFileException |InvalidAudioFrameException e) {
                e.printStackTrace();
            }
        return null;
    }

    private static void setupTagOptionsForReading() {
        TagOptionSingleton.getInstance().setId3v23DefaultTextEncoding(TextEncoding.UTF_16);
        TagOptionSingleton.getInstance().setId3v24DefaultTextEncoding(TextEncoding.UTF_8);
        TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_8);

//        TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_8);

        //  TagOptionSingleton.getInstance().setAndroid(true);
    }

    private static void setupTagOptionsForWriting() {
       // TagOptionSingleton.getInstance().setAndroid(true);
      //  TagOptionSingleton.getInstance().setId3v23DefaultTextEncoding(TextEncoding.UTF_8);
       // TagOptionSingleton.getInstance().setId3v24DefaultTextEncoding(TextEncoding.UTF_8);
       // TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_8);
        TagOptionSingleton.getInstance().setId3v23DefaultTextEncoding(TextEncoding.UTF_16);
        TagOptionSingleton.getInstance().setId3v24DefaultTextEncoding(TextEncoding.UTF_8);
        TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_8);
        TagOptionSingleton.getInstance().setResetTextEncodingForExistingFrames(true);
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V24);
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
        TagOptionSingleton.getInstance().setPadNumbers(true);
        TagOptionSingleton.getInstance().setId3v1Save(true);
        TagOptionSingleton.getInstance().setLyrics3Save(true);
        TagOptionSingleton.getInstance().setVorbisAlbumArtistSaveOptions(VorbisAlbumArtistSaveOptions.WRITE_BOTH);
    }

    public static String buildListeningSelection(String currentTitle){
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

    public MediaItem searchListeningMediaItem(String currentTitle, String currentArtist, String currentAlbum) {
        ContentResolver mContentResolver = context.getContentResolver();
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
        StringBuilder selection=new StringBuilder(buildListeningSelection(currentTitle));
        selection.append(" AND ");
        selection.append( "( " +MediaItemService.buildMediaSelection() +" )");
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

         //   if(StringUtils.compare(newTitle, currentTitle) && StringUtils.compare(newArtist, currentArtist) && StringUtils.compare(newAlbum, currentAlbum)) {
            if(getSimilarScore(titleScore,artistScore,albumScore) > getSimilarScore(prvTitleScore,prvArtistScore,prvAlbumScore)) {
                //titleScore>=prvTitleScore && artistScore>=prvArtistScore && albumScore>=prvAlbumScore
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
        metadata.setDisplayPath(MediaFileProvider.getInstance().getDisplayName(item.getPath()));
        metadata.setMediaPath(item.getPath());
        readId3Tag(item,null); // pending for read tags
        return item;
    }

    private double getSimilarScore(double titleScore, double artistScore, double albumScore) {
        return (titleScore*60)+(artistScore*20)+(albumScore*20);
    }

    public MediaItem searchListeningMediaItemMusic(String currentTitle, String currentArtist, String currentAlbum) {
        ContentResolver mContentResolver = context.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        if(StringUtils.isEmpty(currentTitle)) {
            return null;
        }

        String query = "";
        String [] texts = TextUtils.split(currentTitle, " ");
        if(texts!=null) {
            for(int i=0; i< texts.length;i++) {
                if(i>0) {
                    query = query + " OR ";
                }
                query = query + MediaStore.Audio.Media.TITLE +" LIKE "+ DatabaseUtils.sqlEscapeString("%"+texts[i]+"%");
            }
        }
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

            //   if(StringUtils.compare(newTitle, currentTitle) && StringUtils.compare(newArtist, currentArtist) && StringUtils.compare(newAlbum, currentAlbum)) {
            if(titleScore>=prvTitleScore && artistScore>=prvArtistScore && albumScore>=prvAlbumScore) {
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
        metadata.setDisplayPath(MediaFileProvider.getInstance().getDisplayName(item.getPath()));
        metadata.setMediaPath(item.getPath());
        readId3Tag(item,null); // pending for read tags
        return item;
    }

    private static boolean isValidForTag(String path) {
        String ext = MediaFileProvider.getExtension(path);
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
    }

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

    public boolean saveMediaArtwork(String mediaPath, String artworkPath) {
        if (mediaPath == null || artworkPath == null) {
            return false;
        }
        try {
            boolean isCacheMode = false;
            File file = new File(mediaPath);

            if(!MediaFileProvider.getInstance().isWritable(file)) {
                isCacheMode = true;
                file = MediaFileProvider.getInstance().safToCache(file);
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
                MediaFileProvider.getInstance().safFromCache(file, mediaPath);
            }
        } catch(Exception ex) {
            LogHelper.e(TAG, ex);
        }
        return true;
    }

    public boolean saveMediaTag(String mediaPath,MediaTag changedTag, String artworkFile) throws Exception{
        if (mediaPath == null) {
            return false;
        }

            boolean isCacheMode = false;
            File file = new File(mediaPath);
            // TODO test trickRandomAccessFile
            if(!MediaFileProvider.getInstance().isWritable(file)) {
                isCacheMode = true;
                file = MediaFileProvider.getInstance().safToCache(file);
            }
            setupTagOptionsForWriting();
            AudioFile audioFile = buildAudioFile(file.getAbsolutePath(),"rw");

           // save id3v2
         /*   if(audioFile instanceof MP3File) {
                // also save id3v2 tags
                MP3File mp3 = (MP3File) audioFile;

                //Delete ID3v1 Tag
                ID3v1Tag v1Tag = mp3.getID3v1Tag();
                try {
                    if(mp3.hasID3v1Tag()) {
                        mp3.delete(v1Tag);
                    }
                } catch (IOException e) {
                    LogHelper.e(TAG, e);
                }

                //  save id3v2
                AbstractID3v2Tag id3v2Tag = null;
                if (mp3.hasID3v2Tag()) {
                    id3v2Tag = mp3.getID3v2TagAsv24();
                    updateTag(audioFile, changedTag, id3v2Tag);
                }else {
                    id3v2Tag = new ID3v24Tag();
                    updateTag(audioFile, changedTag, id3v2Tag);
                    mp3.setID3v2Tag(id3v2Tag);
                }
                if(artworkFile!=null) {
                    saveMediaArtwork(audioFile, id3v2Tag, artworkFile);
                }

            }else { */
                // save default tags
                Tag existingTags = audioFile.getTagOrCreateAndSetDefault();
                updateTag(audioFile, changedTag, existingTags);
                if(artworkFile!=null) {
                    saveMediaArtwork(audioFile, existingTags, artworkFile);
                }
          //  }
            updateOnMediaStore(mediaPath, changedTag);

            if(isCacheMode) {
                MediaFileProvider.getInstance().safFromCache(file, mediaPath);
            }
        return true;
    }

    public boolean deleteMediaFile(String mediaPath) {
        File file = new File(mediaPath);
        File directory = file.getParentFile();
        if (MediaFileProvider.getInstance().delete(file)) {
            MediaFileProvider.getInstance().cleanEmptyDirectory(directory);
                deleteFromMediaStore(mediaPath);
                return true;
        }
        return false;
    }

    public boolean moveMediaFile(String path, String organizedPath)  throws Exception{
        if(MediaFileProvider.getInstance().move(path, organizedPath) ) {
                updatePathOnMediaStore(path, organizedPath);
                File file = new File(path);
            MediaFileProvider.getInstance().cleanEmptyDirectory(file.getParentFile());
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

    public void readId3Tag(MediaItem mediaItem, String path) {
        try {
            if(mediaItem.isLoadedEncoding()) return;
            mediaItem.setNewTag(null); // reset updated tag
            MediaTag mediaTag = mediaItem.getTag();
            MediaMetadata metadata = mediaItem.getMetadata();

            if(path == null) {
                path = mediaItem.getPath();
            }else {
                mediaItem.setPath(path);
            }
            metadata.setDisplayPath(MediaFileProvider.getInstance().getDisplayName(path));
            setupTagOptionsForReading();
            AudioFile audioFile = buildAudioFile(path, "r");

            if(audioFile==null) {
                mediaItem.setIdv3Tag(false);
                return;
            }

            readAudioCoding(audioFile,metadata); //16/24/32 bit and 44.1/48/96/192 kHz
            metadata.setMediaSize(getMediaSize(audioFile));
            metadata.setLossless(audioFile.getAudioHeader().isLossless());

            metadata.setLastModified(audioFile.getFile().lastModified());

            if(!readId3Tag(audioFile, mediaTag)) {
                mediaItem.setIdv3Tag(false);
            }

            mediaItem.setLoadedEncoding(true);
            mediaItem.setIdv3Tag(true);
        } catch (Exception |OutOfMemoryError oom) {
            LogHelper.e(TAG, oom);
        }
    }

    private boolean readId3Tag(AudioFile audioFile, MediaTag mediaTag) {
        Tag tag = audioFile.getTag(); //TagOrCreateDefault();
        if (tag != null && !tag.isEmpty()) {
            mediaTag.setTitle(getId3TagValue(tag,FieldKey.TITLE));
            if(StringUtils.isEmpty(mediaTag.getTitle())) {
                //default to file name
                mediaTag.setTitle(MediaFileProvider.removeExtension(audioFile.getFile()));
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
    }

    public Bitmap getArtwork(MediaItem mediaItem) {
            AudioFile audioFile = buildAudioFile(mediaItem.getPath(),"r");

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
           // long bitrate = audioFile.getAudioHeader().getBitRateAsNumber();

            if(metadata.getAudioDuration()==0.00) {
                metadata.setAudioDuration(read.getAudioHeader().getTrackLength());
            }
            metadata.setAudioBitRate(String.format(Locale.getDefault(),"%dkbps",new Object[]{bitrate}));
            metadata.setAudioFormatInfo(String.format(Locale.getDefault(), "%s/%dkbps", new Object[]{metadata.getMediaType(), bitrate}));
            metadata.setAudioCodecInfo(String.format(Locale.getDefault(), "%s/%dkbps", new Object[]{codec.toUpperCase(), bitrate}));
            metadata.setQuality(MediaMetadata.MediaQuality.NORMAL);
            if(sampling<QUALITY_SAMPLING_RATE_HIGH || bitdepth < QUALITY_BIT_DEPTH_GOOD) {
                metadata.setQuality(MediaMetadata.MediaQuality.LOW);
            }else if(read.getAudioHeader().isLossless()) {
                if(sampling > QUALITY_SAMPLING_RATE_HIGH || bitdepth > QUALITY_BIT_DEPTH_GOOD) {
                    metadata.setQuality(MediaMetadata.MediaQuality.HIRES);
                }else {
                    metadata.setQuality(MediaMetadata.MediaQuality.HIGH);
                }
            }else {
                if(bitrate>=QUALITY_COMPRESS_BITRATE_GOOD) {
                    metadata.setQuality(MediaMetadata.MediaQuality.GOOD);
                }else if(bitrate<QUALITY_COMPRESS_BITRATE_LOW) {
                    metadata.setQuality(MediaMetadata.MediaQuality.LOW);
                }
            }
            metadata.setAudioBitDepth(bitdepth+"bit");
            metadata.setAudioSampleRate(formatAudioSampleRate(sampling)+"kHz");
            metadata.setAudioSampleRate(sampling);
        }catch (Exception ex) {}
    }

    public String getOrganizedPath(MediaItem item) {
        // [Hi-Res|Lossless|Compress]/<album|albumartist|artist>/<track no>-<artist>-<title>
        // /format/<album|albumartist|artist>/<track no> <artist>-<title>
        final String ReservedChars = "?|\\*<\":>[]~#%^@.";
        try {
            String musicPath ="/Music/";
            String storeagePath =  MediaFileProvider.getInstance().getRootPath(item.getPath());//androidFile.getStoragePath(context, item.getPath());
            if(storeagePath.endsWith(File.separator)) {
                storeagePath = storeagePath.substring(0,storeagePath.length()-1);
            }
            storeagePath = storeagePath+musicPath; // .../Music

            MediaTag tag = item.getTag();

            String ext = MediaFileProvider.getExtension(item.getPath());
            StringBuffer filename = new StringBuffer();

            if(item.getMetadata().getQuality()== MediaMetadata.MediaQuality.HIRES) {
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
                filename.append(formatTitle(MediaFileProvider.removeExtension(item.getPath())));
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

    public String formatTitle(CharSequence text) {
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

    public File getDownloadPath(String path) {
        File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(download, path);
    }
}
