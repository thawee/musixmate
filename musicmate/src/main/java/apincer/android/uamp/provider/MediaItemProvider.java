package apincer.android.uamp.provider;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.MimeTypeMap;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;

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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import apincer.android.provider.StorageProvider;
import apincer.android.storage.StorageUtils;
import apincer.android.uamp.Constants;
import apincer.android.uamp.R;
import apincer.android.uamp.model.HeaderItem;
import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.service.MusicListeningService;
import apincer.android.uamp.utils.BitmapHelper;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.Util;

/**
 * Wrapper class for accessing media information via media store and jaudiotagger
 * Created by e1022387 on 5/10/2017.
 */
public class  MediaItemProvider extends MediaFileProvider {
    public static final String ACTION = "com.apincer.uamp.provider.MediaItemProvider";
    public static final String SIMILAR_TITLE_ARTIST = "Similar Songs";
    public static final String SIMILAR_TITLE = "Similar Titles";
    public static final String INCOMING_SONGS = "Download Songs";
    public static final String ALL_SONGS = "All Songs";
    public static final String OTHER_AUDIO_FORMAT = "Other Audio Format";
    public static final String LOSSLESS_AUDIO_FORMAT = "Lossless Audio Format";
    private static MediaItemProvider instance;
    private DatabaseHelper databaseHelper;
    public static int QUALITY_BIT_DEPTH_GOOD = 16;
    public static int QUALITY_SAMPLING_RATE_HIGH = 44100;
    public static int QUALITY_COMPRESS_BITRATE_GOOD = 192; //256;
    public static int QUALITY_COMPRESS_BITRATE_AVERAGE = 128;

    private final List<String> mMediaArtists = new ArrayList<>();
    private final List<String> mMediaAlbums = new ArrayList<>();
    private final List<String> mMediaAlbumArtists = new ArrayList<>();
    private final List<String> mMediaGenres = new ArrayList<>();

    private static double MIN_TITLE_ONLY = 0.80;
    private static double MIN_TITLE = 0.70;
    private static double MIN_ARTIST = 0.60;

    public MediaItemProvider() {
        super();
        instance = this;
    }

    public static MediaItemProvider getInstance() {
        return instance;
    }

    public DatabaseHelper getDatabaseHelper() {
        if(databaseHelper==null) {
            databaseHelper = new DatabaseHelper(getContext());
        }
        return databaseHelper;
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
                if(isMediaFileExist(path) && isValidForTag(path)) {
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

    }

    private static void setupTagOptionsForWriting() {
        TagOptionSingleton.getInstance().setResetTextEncodingForExistingFrames(true);
        TagOptionSingleton.getInstance().setID3V2Version(ID3V2Version.ID3_V24);
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
        TagOptionSingleton.getInstance().setPadNumbers(true);
        TagOptionSingleton.getInstance().setRemoveTrailingTerminatorOnWrite(true);
        TagOptionSingleton.getInstance().setRemoveID3FromFlacOnSave(true);
        TagOptionSingleton.getInstance().setLyrics3Save(true);
        TagOptionSingleton.getInstance().setVorbisAlbumArtistSaveOptions(VorbisAlbumArtistSaveOptions.WRITE_ALBUMARTIST_AND_DELETE_JRIVER_ALBUMARTIST);
    }

    public MediaItem searchMediaItem(String currentTitle, String currentArtist, String currentAlbum) {
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            QueryBuilder<MediaMetadata, String> qb = dao.queryBuilder();
            SelectArg selectTitle = new SelectArg();
            SelectArg selectPath = new SelectArg();
            qb.where().like("title", selectTitle).or().like("mediaPath", selectPath);
            // prepare it so it is ready for later query or iterator calls
            PreparedQuery<MediaMetadata> preparedQuery = qb.prepare();

            selectTitle.setValue(StringUtils.trimToEmpty(currentTitle));
            selectPath.setValue(StringUtils.trimToEmpty(currentTitle));
            List<MediaMetadata> list = dao.query(preparedQuery);

            double prvTitleScore = 0.0;
            double prvArtistScore = 0.0;
            double prvAlbumScore = 0.0;
            double titleScore = 0.0;
            double artistScore = 0.0;
            double albumScore = 0.0;
            MediaMetadata matchedMeta = null;

            for (MediaMetadata metadata : list) {
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
            if (matchedMeta != null) {
                return new MediaItem(matchedMeta);
            }
        }catch (SQLException sqle) {
            LogHelper.e(TAG, sqle);
        }
        return null;
    }

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
            byte[] artwork = getArtworkAsByte(item);
            if(artwork!=null) {
                File f = new File(filePath);
                if (f.exists()) {
                    f.delete();
                }
                f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(artwork);
                fos.flush();
                fos.close();
                isFileSaved = true;
            }
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
/*
    private void deleteFromMediaStore(String pathToDelete) {
        try {
            ContentResolver contentResolver = getContext().getContentResolver();
            if (contentResolver != null) {
                Cursor query = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_data"}, "_data = ?", new String[]{pathToDelete}, null);
                if (query != null && query.getCount() > 0) {
                    query.moveToFirst();
                    while (!query.isAfterLast()) {
                        getContext().getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, (long) query.getInt(query.getColumnIndex(MediaStore.Audio.Media._ID))), null, null);
                        query.moveToNext();
                    }
                }
                Util.closeSilently(query);
            }
        }catch (Exception ex) {
            // ignore
            LogHelper.e(TAG, ex);
        }
    } */

    public static String formatDuration(long milliseconds, boolean withUnit) {
        long s = milliseconds / 1000 % 60;
        long m = milliseconds / 1000 / 60 % 60;
        long h = milliseconds / 1000 / 60 / 60 % 24;
        long d = milliseconds / 1000 / 60 / 60 / 24;
        //String format = "%02d:%02d";
        String format = "%02d:%02d:%02d:%02d";
        String formatDayUnit = "%2d days";
        String formatHrsUnit = "%2d hrs";
        String formatMinuteUnit = "%2d mins";
        //String formatUnits = "%2d days, %2d hrs, %2d mins.";
        String formatText = "";

        if(withUnit) {
            if(d>0) {
                formatText = String.format(Locale.getDefault(), formatDayUnit, d);
            }
            if(h >0) {
                if(!StringUtils.isEmpty(formatText)) {
                    formatText = formatText+", ";
                }
                formatText = formatText + String.format(Locale.getDefault(), formatHrsUnit, h);
            }
            if(m >0) {
                if(!StringUtils.isEmpty(formatText)) {
                    formatText = formatText+", ";
                }
                formatText = formatText + String.format(Locale.getDefault(), formatMinuteUnit, m);
            }
            formatText = StringUtils.trimToEmpty(formatText);
        }else {
            formatText = String.format(Locale.getDefault(), format, d,h,m, s);
            while(formatText.startsWith("00:")) {
                formatText = formatText.substring(formatText.indexOf("00:")+("00:".length()), formatText.length());
            }
        }
        return formatText;
    }

    public static String formatAudioSampleRate(long rate) {
        double s = rate / 1000.00;
        String str = String.format(Locale.getDefault(),"%.1f", s);
        str = str.replace(".0", "");
        return str;
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
/*
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
    } */

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

    /*
    public boolean cleanMediaTag(String mediaPath) throws Exception{
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
    } */

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
        item.resetPendingMetadata(true); // reset pending tag

        updateToDatabase(item.getMetadata());
        updateToMediaStore(item.getMetadata());
        return true;
    }

    private void updateToDatabase(MediaMetadata metadata) {
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            // remove old record
            if (metadata.getObsoletePath() != null && dao.idExists(metadata.getObsoletePath())) {
                dao.deleteById(metadata.getObsoletePath());
                //metadata.setObsoletePath(null);
            }
            dao.createOrUpdate(metadata);
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
    }

    private void updateToMediaStore(MediaMetadata metadata) {
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
        setTagField(audioFile,tags,FieldKey.DISC_NO, pendingMetadata.getDisc());
        setTagField(audioFile,tags,FieldKey.LYRICS, pendingMetadata.getLyrics());
        setTagField(audioFile,tags,FieldKey.COMMENT, pendingMetadata.getComment());
        setTagField(audioFile,tags,FieldKey.GROUPING, pendingMetadata.getGrouping());
        setTagField(audioFile,tags,FieldKey.COMPOSER, pendingMetadata.getComposer());
    }

    public boolean deleteMediaFile(String mediaPath) {
        File file = new File(mediaPath);
        File directory = file.getParentFile();
        if (delete(file)) {
            cleanEmptyDirectory(directory);
            deleteFromDatabase(mediaPath);
//            deleteFromMediaStore(mediaPath);
            return true;
        }
        return false;
    }

    private void deleteFromDatabase(String mediaPath) {
        try {
            if(getDatabaseHelper().getMediaDao().deleteById(mediaPath) > 0) {
                //hasNewItems = true;
            }
        } catch (SQLException sqle) {
            LogHelper.i(TAG, sqle);
        }
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
            //deleteFromMediaStore(path);
            LogHelper.e(TAG, ex);
        }
        return true;
    }

    public boolean readMetadata(MediaMetadata metadata) {
        try {

            String path = metadata.getMediaPath();
            AudioFile audioFile = buildAudioFile(path, "r");

            if(audioFile==null) {
                return false;
            }

            readAudioHeader(audioFile,metadata); //16/24/32 bit and 44.1/48/96/192 kHz
            //metadata.setMediaSize(getMediaSize(audioFile));
            metadata.setSize(audioFile.getFile().length());
            metadata.setLossless(audioFile.getAudioHeader().isLossless());

            if(readMetadata(audioFile, metadata)) {
                metadata.setLastModified(audioFile.getFile().lastModified());
            }
            return true;
        } catch (Exception |OutOfMemoryError oom) {
            LogHelper.e(TAG, oom);
        }
        return false;
    }

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
            mediaTag.setLyrics(getId3TagValue(tag, FieldKey.LYRICS));
            mediaTag.setComment(getId3TagValue(tag, FieldKey.COMMENT));
            mediaTag.setGrouping(getId3TagValue(tag, FieldKey.GROUPING));
            return true;
        }
        return false;
    }

    /*
    public Bitmap getArtwork(MediaItem mediaItem) {
            AudioFile audioFile = buildAudioFile(mediaItem.getPath(),"r");

            if(audioFile==null) {
                return null;
            }
            return getArtwork(audioFile.getTagOrCreateDefault(),false);
    } */

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
                bitmap = BitmapHelper.decodeBitmap(artworkData, 1024,1024);
            }
        }
        return bitmap;
    }

    /*
    private String getMediaSize(AudioFile audioFile) {
        try {
            double length = audioFile.getFile().length();
            length = (length/(1024*1024));
            StringUtils.formatStorageSize(length);
          //  return String.format("%.2f", length)+ " MB";
        }catch (Exception ex) {
            return "";
        }
    } */

    private void readAudioHeader(AudioFile read, MediaMetadata metadata) {
        try {
            long sampling = read.getAudioHeader().getSampleRateAsNumber(); //44100/48000 Hz
            long bitdepth = read.getAudioHeader().getBitsPerSample(); //16/24/32
            long bitrate = read.getAudioHeader().getBitRateAsNumber(); //128/256/320
            int ch = 2;
            String codec = getEncodingType(read);  read.getAudioHeader().getEncodingType();

            if(metadata.getAudioDuration()==0.00) {
                metadata.setAudioDuration(read.getAudioHeader().getTrackLength());
            }
            if(metadata.getAudioDuration()==0.00) {
                // calculate duration
                //duration = filesize in bytes / (samplerate * #of channels * (bitspersample / 8 ))
                long duration = metadata.getSize() / (bitdepth+ch) * (bitrate);
                metadata.setAudioDuration(duration);
            }
            metadata.setAudioBitRate(String.format(Locale.getDefault(),"%dKbps",new Object[]{bitrate}));
            metadata.setAudioCodec(codec.toUpperCase());
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
                }else if(bitrate<QUALITY_COMPRESS_BITRATE_AVERAGE) {
                    metadata.setAudioEncodingQuality(MediaItem.MEDIA_QUALITY_LOW);
                }
            }
            metadata.setAudioBitCount(bitdepth+"bit");
            metadata.setAudioSamplingrate(formatAudioSampleRate(sampling)+"kHz");
           // metadata.setAudioSampleRate(sampling);
        }catch (Exception ex) {}
    }

    private String getEncodingType(AudioFile read) {
        String ext = read.getExt();
        if(StringUtils.isEmpty(ext)) return "";

        if("m4a".equalsIgnoreCase(ext)) {
            if(read.getAudioHeader().isLossless()) {
                return Constants.MEDIA_ENC_ALAC;
            }
            return Constants.MEDIA_ENC_AAC;
        }
        return  ext.toUpperCase();
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
                    filename.append("LLAC").append(File.separator);
                }else {
                    filename.append("LSAC").append(File.separator);
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

    public boolean saveMediaArtwork(MediaItem item, Artwork artwork) {
        if (item == null || item.getPath() == null || artwork==null) {
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

    protected void addToDatabase(MediaMetadata metadata) {
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            dao.createOrUpdate(metadata);
           // hasNewItems = true;
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
    }

    public long getLastestModifiedFromDatabase() {
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            QueryBuilder<MediaMetadata, String> qb = dao.queryBuilder();
            qb.selectRaw("MAX(lastModified)");

            return dao.queryRawValue(qb.prepareStatementString());

        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        return -1;
    }

    /*
    public Collection<? extends MediaItem> loadHiResMediaItems() {
        List<MediaItem> mediaItems = new ArrayList();
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            long duration = 0;
            long size=0;
            HeaderItem header = new HeaderItem("Hi-Res Audio");
            header.setTitle("High-Resolution Audio");
            List<MediaMetadata> list = dao.queryBuilder().orderBy("title", true).orderBy("artist", true).query();
            for(MediaMetadata mdata: list) {
                if (mdata.getAudioEncodingQuality() == MediaItem.MEDIA_QUALITY_HIRES) {
                    mediaItems.add(new MediaItem(mdata, header));
                    duration = duration+mdata.getAudioDuration();
                    size = size+mdata.getSize();
                }
            }
            updateHeader(header, mediaItems.size(), size, duration);
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        return mediaItems;
    } */

    public Collection<? extends MediaItem> queryLLACMediaItems() {
        List<MediaItem> mediaItems = new ArrayList();
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            long duration = 0;
            long size =0;
            HeaderItem header = new HeaderItem("Lossless Audio Format");
            header.setTitle(LOSSLESS_AUDIO_FORMAT);
            List<MediaMetadata> list = dao.queryBuilder().orderBy("title", true).orderBy("artist", true).query();
            for(MediaMetadata mdata: list) {
                if (mdata.isLossless()) {
                    mediaItems.add(new MediaItem(mdata, header));
                    duration = duration+mdata.getAudioDuration();
                    size = size+mdata.getSize();
                }
            }
            updateHeader(header, mediaItems.size(), size, duration);
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        return mediaItems;
    }

    public Collection<? extends MediaItem> queryLSACMediaItems() {
        List<MediaItem> mediaItems = new ArrayList();
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            long duration = 0;
            long size = 0;
            HeaderItem header = new HeaderItem("Other Audio Format");
            header.setTitle(OTHER_AUDIO_FORMAT);
            List<MediaMetadata> list = dao.queryBuilder().orderBy("title", true).orderBy("artist", true).query();
            for(MediaMetadata mdata: list) {
                if (!mdata.isLossless()) {
                    mediaItems.add(new MediaItem(mdata, header));
                    duration = duration+mdata.getAudioDuration();
                    size = size+mdata.getSize();
                }
            }
            updateHeader(header, mediaItems.size(), size, duration);
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        return mediaItems;
    }

    public List<MediaItem> queryMediaItems() {
        List<MediaItem> mediaItems = new ArrayList();
        HeaderItem header = new HeaderItem("Library");
        long duration = 0;
        long size = 0;
       try {
                header.setTitle(ALL_SONGS);
                Dao dao = getDatabaseHelper().getMediaDao();
                List<MediaMetadata> list = dao.queryBuilder().orderBy("title", true).orderBy("artist", true).query();
                for(MediaMetadata mdata: list) {
                    mediaItems.add(new MediaItem(mdata, header));
                    duration = duration+mdata.getAudioDuration();
                    size = size+mdata.getSize();
                    updateCollections(mdata);
                }
       }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
       }

       updateHeader(header, mediaItems.size(), size, duration);
       return mediaItems;
    }

    private void updateCollections(MediaMetadata mdata) {
        addToArtistList(mdata.getArtist());
        addToAlbumArtistList(mdata.getAlbumArtist());
        addToAlbumList(mdata.getAlbum());
        addToGenreList(mdata.getGenre());
    }

    private void updateHeader(HeaderItem header, int listSize, long storageSize, long duration) {
        //String title = StringUtils.formatSongSize(listSize) + " | "+StringUtils.formatStorageSize(storageSize);
        String title = StringUtils.formatSongSize(listSize);
        title = title+" | "+ MediaItemProvider.formatDuration(duration, true);
        header.setSubtitle(title);
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

    public Collection<? extends MediaItem> queryNewMediaItems() {
        List<MediaItem> mediaItems = new ArrayList();
        HeaderItem header = new HeaderItem("NewMusic");
        long duration = 0;
        long size = 0;
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            header.setTitle(INCOMING_SONGS);
            List<MediaMetadata> list = dao.queryBuilder().orderBy("title", true).orderBy("artist", true).query();
            for(MediaMetadata mdata: list) {
                if (!mdata.getMediaPath().contains("/Music/") || mdata.getMediaPath().contains("/znew")) {
                    mediaItems.add(new MediaItem(mdata, header));
                    duration = duration+mdata.getAudioDuration();
                    size = size + mdata.getSize();
                }
            }
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        updateHeader(header, mediaItems.size(), size, duration);
        return mediaItems;
    }

    public Collection<? extends MediaItem> querySimilarTitle() {
        List<MediaItem> mediaItems = new ArrayList();
        HeaderItem header = new HeaderItem("Similar");
        long duration =0;
        long size=0;
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            header.setTitle(SIMILAR_TITLE);
            List<MediaMetadata> list = dao.queryBuilder().orderBy("title", true).orderBy("artist", true).query();
            MediaMetadata pmdata = null;
            boolean preAdded = false;
            for(MediaMetadata mdata: list) {
                //similarity
                if (pmdata!=null && (StringUtils.similarity(mdata.getTitle(), pmdata.getTitle())>MIN_TITLE_ONLY)) {
                    if(!preAdded && pmdata != null) {
                        mediaItems.add(new MediaItem(pmdata,header));
                        duration = duration+ pmdata.getAudioDuration();
                        size = size+pmdata.getSize();
                    }
                    mediaItems.add(new MediaItem(mdata, header));
                    duration = duration+mdata.getAudioDuration();
                    size = size+mdata.getSize();
                    preAdded = true;
                }else {
                    preAdded = false;
                }
                pmdata = mdata;
            }
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        updateHeader(header, mediaItems.size(), size, duration);
        return mediaItems;
    }

    public Collection<? extends MediaItem> querySimilarArtistAndTitle() {
        List<MediaItem> mediaItems = new ArrayList();
        long duration = 0;
        long size = 0;
        HeaderItem header = new HeaderItem("Similar");
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            header.setTitle(SIMILAR_TITLE_ARTIST);
            List<MediaMetadata> list = dao.queryBuilder().orderBy("title", true).orderBy("artist", true).query();
            MediaMetadata pmdata = null;
            boolean preAdded = false;
            for(MediaMetadata mdata: list) {
                //similarity
                if (pmdata!=null && (StringUtils.similarity(mdata.getTitle(), pmdata.getTitle())>MIN_TITLE) &&
                        (StringUtils.similarity(mdata.getArtist(), pmdata.getArtist())>MIN_ARTIST)) {
                    if(!preAdded && pmdata != null) {
                        mediaItems.add(new MediaItem(pmdata, header));
                        duration = duration+pmdata.getAudioDuration();
                        size = size + pmdata.getSize();
                    }
                    mediaItems.add(new MediaItem(mdata, header));
                    duration = duration+mdata.getAudioDuration();
                    size = size + mdata.getSize();
                    preAdded = true;
                }else {
                    preAdded = false;
                }
                pmdata = mdata;
            }
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }

        updateHeader(header, mediaItems.size(), size, duration);
        return mediaItems;
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
        try {
            getDatabaseHelper().getMediaDao().deleteById(metadata.getMediaPath());
           // hasNewItems = true;
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
    }

    public void removeFromDatabase(String mediaPath) {
        try {
            getDatabaseHelper().getMediaDao().deleteById(mediaPath);
            //hasNewItems = true;
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
    }

    public boolean moveToManagedDirectory(MediaItem item) {
        if(readMetadata(item.getMetadata())) {
            String newPath = buildManagedPath(item);
            if (move(item.getPath(), newPath)) {
                updatePathOnMediaStore(item.getPath(), newPath);
                File file = new File(item.getPath());
                cleanEmptyDirectory(file.getParentFile());
                MediaMetadata metadata = item.getMetadata();
                metadata.setObsoletePath(metadata.getMediaPath());
                metadata.setMediaPath(newPath);
                updateToDatabase(metadata);
                return true;
            }
        }
        return false;
    }

    public List<String> getAllGenres() {
        return mMediaGenres;
    }

    public boolean scanFromMediaStore() {
        Cursor cur = null;
        String []paths = {"/Music/", "/Download/"};
        try {
            ContentResolver mContentResolver = getContext().getContentResolver();
            Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            // Perform a query on the content resolver. The URI we're passing specifies that we
            // want to query for all audio media on external storage (e.g. SD card)
            // String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1";
            String selection = buildMediaSelectionForPaths(paths);
            cur = mContentResolver.query(uri, null, selection, null, "LOWER (" + MediaStore.Audio.Media.TITLE + ") ASC");
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
                String mediaPath = cur.getString(dataColumn);
                String mediaTitle = cur.getString(titleColumn);
                String mediaAlbum = cur.getString(albumColumn);
                String mediaArtist = cur.getString(artistColumn);
                long mediaDuration = cur.getLong(durationColumn);
                if(isDatabaseOutdated(mediaPath)) {
                    scanMediaItem(id, mediaTitle, mediaArtist, mediaAlbum, mediaPath, mediaDuration);
                }
            } while (cur.moveToNext());
        } catch (Exception ex) {
            LogHelper.e(TAG, ex);
        } finally {
            Util.closeSilently(cur);
        }

        return true;
    }

    private boolean isDatabaseOutdated(String mediaPath) {
        try {
            File file = new File(mediaPath);
            if (file.exists()) {
                MediaMetadata mdata = databaseHelper.getMediaDao().queryForId(mediaPath);
                if (mdata == null) {
                    return true; // need insert
                } else if (mdata.getLastModified() < file.lastModified()) {
                    return true; // need update database
                }
            }
        }catch (Exception ex) {
            LogHelper.e(TAG, ex);
        }
        return false;
    }

    private void scanMediaItem(final int id, final String mediaTitle, final String mediaArtist, final String mediaAlbum, final String mediaPath, final long mediaDuration) {
        startScan(new Runnable() {
            @Override
            public void run() {
                MediaMetadata metadata = buildMediaMetadata(id, mediaTitle, mediaArtist, mediaAlbum, mediaPath, mediaDuration);
                if(metadata!=null && !StringUtils.isEmpty(metadata.getMediaType())) {
                    addToDatabase(metadata);
                }
            }
        });
    }

    protected void sendBroadcast(final String command, final MediaItem item, final String status, final String message){
        // Fire the broadcast with intent packaged
        // Construct our Intent specifying the Service
        Intent intent = new Intent(ACTION);
        // Add extras to the bundle
        intent.putExtra(Constants.KEY_RESULT_CODE, Activity.RESULT_OK);
        intent.putExtra(Constants.KEY_COMMAND, command);
        intent.putExtra(Constants.KEY_STATUS, status);
        intent.putExtra(Constants.KEY_MESSAGE, message);
        intent.putExtra(Constants.KEY_PENDING_COUNT, getPendingOperation());
        //intent.putExtra("currentItem", index); // index start from 0
        if(item !=null) {
            intent.putExtra(Constants.KEY_MEDIA_PATH, item.getPath());
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    protected void sendBroadcast(final String command, final String status){
        // Fire the broadcast with intent packaged
        // Construct our Intent specifying the Service
        Intent intent = new Intent(ACTION);
        // Add extras to the bundle
        intent.putExtra(Constants.KEY_RESULT_CODE, Activity.RESULT_OK);
        intent.putExtra(Constants.KEY_COMMAND, command);
        intent.putExtra(Constants.KEY_STATUS, status);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    public void manageMediaItem(MediaItem item) {
        boolean status = false;
        // String indexStr = String.valueOf(index);
        // String totalStr = String.valueOf(moveItems.size());
        try {
            String msg = getContext().getString(R.string.alert_organize_start, item.getTitle());
            //if(moveItems.size()>1) {
            //    msg = getString(R.string.alert_many, indexStr, totalStr, msg);
            //}
            sendBroadcast(Constants.COMMAND_MOVE, item, Constants.STATUS_START, msg);
            if(moveToManagedDirectory(item)) {
                MusicListeningService.getInstance().playNextSongifMatched(item);
            }
            status = true;
        }catch(Exception|OutOfMemoryError ex) {
            status = false;
        }

        String msg = status?getContext().getString(R.string.alert_organize_success, item.getTitle()):getContext().getString(R.string.alert_organize_fail, item.getTitle());
        String statusStr = status?Constants.STATUS_SUCCESS:Constants.STATUS_FAIL;
        //if(moveItems.size()>1) {
        //    msg = getString(R.string.alert_many, indexStr, totalStr, msg);
        //}
        sendBroadcast(Constants.COMMAND_MOVE, item, statusStr, msg);
    }

    public void deleteMediaItem(MediaItem item) {
        boolean status = false;
        // String indexStr = String.valueOf(index);
        // String totalStr = String.valueOf(deleteItems.size());
        try {
            String msg = getContext().getString(R.string.alert_delete_start, item.getTitle());
            //if(deleteItems.size()>1) {
            //    msg = getString(R.string.alert_many, indexStr, totalStr, msg);
            //}
            sendBroadcast(Constants.COMMAND_DELETE,item, "start", msg);
            status = MediaItemProvider.getInstance().deleteMediaFile(item.getPath());
            MusicListeningService.getInstance().playNextSongifMatched(item);
        } catch (Exception|OutOfMemoryError ex) {
            status = false;
        }

        String msg = status?getContext().getString(R.string.alert_delete_success, item.getTitle()):getContext().getString(R.string.alert_delete_fail, item.getTitle());
        String statusStr = status?"success":"fail";
        //if(deleteItems.size()>1) {
        //    msg = getString(R.string.alert_many, indexStr, totalStr, msg);
        //}
        sendBroadcast(Constants.COMMAND_DELETE,item, statusStr, msg);
    }

    public void saveMediaItem(MediaItem item, Artwork artwork) {
        boolean status = false;
        // String indexStr = String.valueOf(index);
        // String totalStr = String.valueOf(saveItems.size());
        try {
            String msg = getContext().getString(R.string.alert_write_tag_start, item.getTitle());
            //if(saveItems.size()>1) {
            //    msg = getString(R.string.alert_many, indexStr, totalStr, msg);
            //}
            sendBroadcast(Constants.COMMAND_SAVE, item, "start", msg);

            // call new API
            saveMetadata(item);
            //readMetadata(item.getMetadata());
            saveMediaArtwork(item, artwork);
            status = true;
        }catch (Exception|OutOfMemoryError ex) {
            status = false;
        }

        String msg = status?getContext().getString(R.string.alert_write_tag_success, item.getTitle()):getContext().getString(R.string.alert_write_tag_fail, item.getTitle());
        String statusStr = status?"success":"fail";
        //if(saveItems.size()>1) {
        //    msg = getString(R.string.alert_many, indexStr, totalStr, msg);
        // }
        sendBroadcast(Constants.COMMAND_SAVE, item, statusStr, msg);
    }

    /*
     * Creates a normal item with a Header linked.
     */
    protected MediaMetadata buildMediaMetadata(long id, String mediaTitle, String mediaArtist, String mediaAlbum, String mediaPath, long mediaDuration) {
        if(isMediaFileExist(mediaPath)) {
            MediaMetadata metadata = new MediaMetadata();
            metadata.setMediaPath(mediaPath);
            metadata.setMediaType(getExtension(mediaPath).toUpperCase());
            metadata.setTitle(mediaTitle);
            metadata.setAlbum(mediaAlbum);
            metadata.setArtist(mediaArtist);
            metadata.setAudioDuration(mediaDuration);
            //metadata.setAudioFormat(metadata.getMediaType() == null ? "" : metadata.getMediaType().toUpperCase());
            readMetadata(metadata);
            return metadata;
        }
        return null;
    }

    public static String buildMediaSelection(long dateModified){
        StringBuilder selection=new StringBuilder();
        selection.append("(");
        for(SupportedFileFormat format : SupportedFileFormat.values()) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format.name().toLowerCase());
            selection.append("(" + MediaStore.Files.FileColumns.MIME_TYPE + "=='"+ mimeType+ "') OR ");
        }
        selection.append(" ("+MediaStore.Audio.Media.IS_MUSIC + " = 1 )) ");
        if(dateModified>0) {
            selection.append(" AND ("+MediaStore.Audio.Media.DATE_MODIFIED + " >= "+dateModified+" ) ");
        }
       // return selection.substring(0,selection.lastIndexOf(")") + 1);
        return selection.toString();
    }

    private String buildMediaSelectionForPaths(String[] paths) {
        StringBuilder selection=new StringBuilder();
        selection.append("(");
        for(String path: paths) {
            selection.append("(" + MediaStore.Files.FileColumns.DATA + " like '%"+ path+ "%') OR ");
        }
        selection.append(" ("+MediaStore.Audio.Media.IS_MUSIC + " = 1 )) ");
        return selection.toString();
    }

    public void cleanDatabase() {
        try {
            List<MediaMetadata> list = databaseHelper.getMediaDao().queryForAll();
            for(int i=0; i<list.size();i++) {
                MediaMetadata mdata = list.get(i);
                if(!isMediaFileExist(mdata.getMediaPath())) {
                    deleteFromDatabase(mdata.getMediaPath());
                }
            }
        }catch (Exception ex) {
            LogHelper.e(TAG, ex);
        }
    }

    public List<String> getSamplingRates() {
        List<String> rates = new ArrayList<>();
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            GenericRawResults<String[]> rawResults =
                    dao.queryRaw("SELECT DISTINCT audioBitCount,audioSamplingrate FROM MediaItem");
            for (String[] resultColumns : rawResults) {
                String bitDepth = resultColumns[0];
                String sampleRate = resultColumns[1];
                rates.add(bitDepth + "/" + sampleRate);
            }
        }catch (SQLException ex) {
        }
       Collections.sort(rates, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int bit1 = parseNumber(o1,true);
                int srate1 = parseNumber(o1,false);
                int bit2 = parseNumber(o2,true);
                int srate2 = parseNumber(o2,false);
                if(bit1 == bit2) {
                    return srate2<srate1?-1:1;
                }else {
                    return bit2<bit1?-1:1;
                }
            }
        });
        return rates;
    }

    private int parseNumber(String samplingRate, boolean bitdept) {
        int sampleRate = 0;
        String str = "";
        boolean start = bitdept;
        for(char ch: samplingRate.toCharArray()) {
            if(start && ch=='/') {
                break; //
            }else if(!start && ch=='/') {
                start = true;
                continue;
            }
            if(start && Character.isDigit(ch)) {
                str = str+ch;
            }else if(start && !Character.isDigit(ch)){
                break;
            }
        }
        try {
            sampleRate = Integer.parseInt(str);
        }catch (Exception ex){}
      //  LogHelper.d(TAG,samplingRate+":"+sampleRate);
        return sampleRate;
    }

    public Collection<? extends MediaItem> queryMediaItemsBySamplingRate(String title) {
        List<MediaItem> mediaItems = new ArrayList();
        HeaderItem header = new HeaderItem(title);
        long duration = 0;
        long size = 0;

        try {
            if(title==null) return mediaItems;

            int indx = title.indexOf("/");
            indx = indx<0?0:indx;
            String bitdepht = title.substring(0,indx);
            String sampleRate = title.substring(indx+1, title.length());
            header.setTitle(title);
            Dao dao = getDatabaseHelper().getMediaDao();
            QueryBuilder qb =dao.queryBuilder();
            qb.where().eq("audioBitCount",bitdepht).and().eq("audioSamplingrate",sampleRate);
            List<MediaMetadata> list = qb.orderBy("title", true).orderBy("artist", true).query();
            for(MediaMetadata mdata: list) {
                mediaItems.add(new MediaItem(mdata, header));
                duration = duration+mdata.getAudioDuration();
                size = size+mdata.getSize();
                updateCollections(mdata);
            }
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        updateHeader(header, mediaItems.size(), size, duration);
        return mediaItems;
    }

    public String getStorageInfo() {
        Map<String, RootInfo> infos = getRootPaths();
        StorageUtils utils = new StorageUtils(getContext());
        long free = 0;
        long total = 0;
        for(StorageProvider.RootInfo info: infos.values()) {
            if(!MediaItemProvider.isDeviceStorage(info)){
                free = free+utils.getPartitionSize(info.path.getAbsolutePath(), false);
                total = total+utils.getPartitionSize(info.path.getAbsolutePath(), true);
            }
        }
        return StringUtils.trimToEmpty(StringUtils.formatStorageSize(free) + " free of " + StringUtils.formatStorageSize(total));
    }

    public List<String> getAudioFormats() {
        List<String> formats = new ArrayList<>();
        try {
            Dao dao = getDatabaseHelper().getMediaDao();
            GenericRawResults<String[]> rawResults =
                    dao.queryRaw("SELECT DISTINCT audioFormat FROM MediaItem order by audioFormat");
            for (String[] resultColumns : rawResults) {
                String format = resultColumns[0];
                formats.add(format + " Format");
            }
        }catch (SQLException ex) {
        }
        return formats;
    }

    public Collection<? extends MediaItem> queryMediaItemsByAudioFormat(String format) {
        List<MediaItem> mediaItems = new ArrayList();
        HeaderItem header = new HeaderItem(format);
        long duration = 0;
        long size = 0;

        try {
            if(format==null) return mediaItems;

            int indx = format.indexOf(" ");
            indx = indx<0?0:indx;
            String audioFormat = format.substring(0,indx);
            header.setTitle(format);
            Dao dao = getDatabaseHelper().getMediaDao();
            QueryBuilder qb =dao.queryBuilder();
            qb.where().eq("audioFormat",audioFormat);
            List<MediaMetadata> list = qb.orderBy("title", true).orderBy("artist", true).query();
            for(MediaMetadata mdata: list) {
                mediaItems.add(new MediaItem(mdata, header));
                duration = duration+mdata.getAudioDuration();
                size = size+mdata.getSize();
                updateCollections(mdata);
            }
        }catch (SQLException sqle) {
            LogHelper.e(TAG,sqle);
        }
        updateHeader(header, mediaItems.size(), size, duration);
        return mediaItems;
    }
}