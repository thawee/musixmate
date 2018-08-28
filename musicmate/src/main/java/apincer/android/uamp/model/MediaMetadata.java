package apincer.android.uamp.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.StringUtils;

@DatabaseTable(tableName = "MediaItem")
public class MediaMetadata implements Cloneable {

    // file information
    @DatabaseField(id = true)
    protected String mediaPath;
    @DatabaseField(columnName = "lastModified")
    protected long lastModified = 0;

    protected String obsoletePath;

    // tags information
    @DatabaseField(columnName = "title")
    protected String title;
    @DatabaseField
    protected String artist;
    @DatabaseField
    protected String album;
    @DatabaseField
    private String year;
    @DatabaseField
    private String genre;
    @DatabaseField
    private String track;
    @DatabaseField
    private String disc;
    @DatabaseField
    private String comment;
    @DatabaseField
    private String grouping;
    @DatabaseField
    private String composer;
    private String lyrics;
    @DatabaseField
    protected String albumArtist;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @DatabaseField
    protected long size;
    protected String mediaSize;
    @DatabaseField
    protected long audioDuration;
    @DatabaseField
    protected String mediaType;
    @DatabaseField
    protected boolean lossless;

    // audio information
    @DatabaseField
    protected int audioEncodingQuality = 1;
    @DatabaseField
    protected String audioBitCount; // 16/24/32 (bits)
    @DatabaseField
    protected String audioSamplingrate; //44.1,48,88.2,96,192 kHz

    public String getAudioBitCount() {
        return audioBitCount;
    }

    public String getAudioBitRate() {
        return audioBitRate;
    }

    @DatabaseField
    protected String audioBitRate; //128, 256, 320 kbps
//    @DatabaseField
//    protected String audioFormatInfo; //MP3/xxxkbps, FLAC/xxxkbps, ALAC/xxxkbps, AAC/xxxkbps
    @DatabaseField String audioFormat; // MP3, FLAC, ALAC, AAC

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioCodec(String audioFormat) {
        this.audioFormat = audioFormat;
    }



    public MediaMetadata() {
    }

    public String getObsoletePath() {
        return obsoletePath;
    }

    public void setObsoletePath(String obsoletePath) {
        this.obsoletePath = obsoletePath;
    }

    public int getAudioEncodingQuality() {
        return audioEncodingQuality;
    }

    public void setAudioEncodingQuality(int audioEncodingQuality) {
        this.audioEncodingQuality = audioEncodingQuality;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDisc() {
        return disc;
    }

    public void setDisc(String disc) {
        this.disc = disc;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getGrouping() {
        return grouping;
    }

    public void setGrouping(String grouping) {
        this.grouping = grouping;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setAudioDuration(long audioDuration) {
        this.audioDuration = audioDuration;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public void setMediaSize(String mediaSize) {
        this.mediaSize = mediaSize;
    }

    public boolean isLossless() {
        return lossless;
    }

    public void setLossless(boolean lossless) {
        this.lossless = lossless;
    }

    public void setAudioBitCount(String audioBitCount) {
        this.audioBitCount = audioBitCount;
    }

    public void setAudioSamplingrate(String audioSamplingrate) {
        this.audioSamplingrate = audioSamplingrate;
    }

    public String getAudioSamplingrate() {
        return audioSamplingrate;
    }

    public void setAudioBitRate(String audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public String getAudioBitCountAndSamplingrate() {
        if(!StringUtils.isEmpty(audioBitCount)) {
            return audioBitCount+"/"+audioSamplingrate;
        }else {
            return audioSamplingrate;
        }
    }

    public String getMediaSize() {
        if(mediaSize==null) {
            mediaSize = StringUtils.formatStorageSize(size);
        }
        return mediaSize;
    }

    public String getAudioDurationAsString() {
            return MediaItemProvider.formatDuration(audioDuration, false);
    }

    public long getAudioDuration() {
        return audioDuration;
    }

    @Override
    public MediaMetadata clone() {
        MediaMetadata tag = new MediaMetadata();
        tag.mediaPath = mediaPath;
        tag.mediaSize = mediaSize;
        tag.mediaType = mediaType;

        tag.audioBitCount=audioBitCount;
        tag.audioBitRate=audioBitRate;
        tag.audioDuration=audioDuration;
        tag.audioSamplingrate=audioSamplingrate;
        tag.audioEncodingQuality=audioEncodingQuality;
        tag.size = size;

        tag.title=title;
        tag.album=album;
        tag.artist=artist;
        tag.albumArtist=albumArtist;
        tag.genre=genre;
        tag.year=year;
        tag.track=track;
        tag.disc=disc;
        tag.lyrics=lyrics;
        tag.comment=comment;
        tag.grouping=grouping;
        tag.composer =composer;
        return tag;
    }
}
