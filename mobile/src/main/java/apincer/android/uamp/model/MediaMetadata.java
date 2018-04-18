package apincer.android.uamp.model;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;

import apincer.android.uamp.provider.MediaItemProvider;
import apincer.android.uamp.utils.StringUtils;

@Entity(nameInDb = "media_metadata")
public class MediaMetadata implements Cloneable {
    @Id
    protected long id;

    // file information
    @Index
    protected String mediaPath;
    protected String displayPath;
    protected long lastModified = 0;

    // tags information
    protected String title;
    protected String artist;
    protected String album;
    private String year;
    private String genre;
    private String track;
    private String trackTotal;
    private String disc;
    private String discTotal;
    private String comment;
    private String grouping;
    private String composer;
    private String lyrics;
    protected String albumArtist;
    protected String mediaSize;
    protected long audioDuration;
    protected String mediaType;
    protected boolean lossless;
    protected boolean hires;

    // audio information
    protected int audioEncodingQuality = 1;
    protected String audioBitDepth; // 16/24/32 bits
    protected String audioSampleRate; //44.1,48,96,192 kHz
    protected long audioSampleRateInt; //
    protected String audioBitRate; //128, 256, 320 kbps
    protected String audioFormatInfo; // MP3, FLAC
    protected String audioCodecInfo; // Mpeg Layer 3, Free Lossless

    public MediaMetadata() {
    }

    @Generated(hash = 1721537058)
    public MediaMetadata(long id, String mediaPath, String displayPath,
            long lastModified, String title, String artist, String album,
            String year, String genre, String track, String trackTotal, String disc,
            String discTotal, String comment, String grouping, String composer,
            String lyrics, String albumArtist, String mediaSize, long audioDuration,
            String mediaType, boolean lossless, boolean hires,
            int audioEncodingQuality, String audioBitDepth, String audioSampleRate,
            long audioSampleRateInt, String audioBitRate, String audioFormatInfo,
            String audioCodecInfo) {
        this.id = id;
        this.mediaPath = mediaPath;
        this.displayPath = displayPath;
        this.lastModified = lastModified;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.year = year;
        this.genre = genre;
        this.track = track;
        this.trackTotal = trackTotal;
        this.disc = disc;
        this.discTotal = discTotal;
        this.comment = comment;
        this.grouping = grouping;
        this.composer = composer;
        this.lyrics = lyrics;
        this.albumArtist = albumArtist;
        this.mediaSize = mediaSize;
        this.audioDuration = audioDuration;
        this.mediaType = mediaType;
        this.lossless = lossless;
        this.hires = hires;
        this.audioEncodingQuality = audioEncodingQuality;
        this.audioBitDepth = audioBitDepth;
        this.audioSampleRate = audioSampleRate;
        this.audioSampleRateInt = audioSampleRateInt;
        this.audioBitRate = audioBitRate;
        this.audioFormatInfo = audioFormatInfo;
        this.audioCodecInfo = audioCodecInfo;
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


    public boolean isHires() {
        return hires;
    }

    public void setHires(boolean hires) {
        this.hires = hires;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getTrackTotal() {
        return trackTotal;
    }

    public void setTrackTotal(String trackTotal) {
        this.trackTotal = trackTotal;
    }

    public String getDisc() {
        return disc;
    }

    public void setDisc(String disc) {
        this.disc = disc;
    }

    public String getDiscTotal() {
        return discTotal;
    }

    public void setDiscTotal(String discTotal) {
        this.discTotal = discTotal;
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

    public long getAudioSampleRateInt() {
        return audioSampleRateInt;
    }

    public void setAudioSampleRateInt(long audioSampleRateInt) {
        this.audioSampleRateInt = audioSampleRateInt;
    }

    public long getAudioSampleRateAsInt() {
        return audioSampleRateInt;
    }

    public void setAudioSampleRate(long sampling) {
        audioSampleRateInt = sampling;
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

    public String getDisplayPath() {
        return displayPath;
    }

    public void setDisplayPath(String displayPath) {
        this.displayPath = displayPath;
    }

    public boolean isLossless() {
        return lossless;
    }

    public void setLossless(boolean lossless) {
        this.lossless = lossless;
    }

    public String getAudioBitDepth() {
        return audioBitDepth;
    }

    public void setAudioBitDepth(String audioBitDepth) {
        this.audioBitDepth = audioBitDepth;
    }

    public String getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(String audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public void setAudioBitRate(String audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public void setAudioFormatInfo(String audioFormatInfo) {
        this.audioFormatInfo = audioFormatInfo;
    }

    public String getAudioBitRate() {
        return audioBitRate;
    }

    public String getAudioFormatInfo() {
        return audioFormatInfo;
    }

    public String getAudioCodecInfo() {
        return audioCodecInfo;
    }

    public void setAudioCodecInfo(String audioCodecInfo) {
        this.audioCodecInfo = audioCodecInfo;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public String getAudioCoding() {
        if(!StringUtils.isEmpty(audioBitDepth)) {
            return audioBitDepth+"/"+audioSampleRate;
        }else {
            return audioSampleRate;
        }
    }

    public String getMediaSize() {
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
        tag.mediaType =mediaType;
        tag.displayPath = displayPath;

        tag.audioBitDepth=audioBitDepth;
        tag.audioBitRate=audioBitRate;
        tag.audioCodecInfo=audioCodecInfo;
        tag.audioDuration=audioDuration;
        tag.audioFormatInfo=audioFormatInfo;
        tag.audioSampleRate=audioSampleRate;
        tag.audioSampleRateInt=audioSampleRateInt;
        tag.audioEncodingQuality=audioEncodingQuality;

        tag.title=title;
        tag.album=album;
        tag.artist=artist;
        tag.albumArtist=albumArtist;
        tag.genre=genre;
        tag.year=year;
        tag.track=track;
        tag.trackTotal=trackTotal;
        tag.disc=disc;
        tag.discTotal=discTotal;
        tag.lyrics=lyrics;
        tag.comment=comment;
        tag.grouping=grouping;
        tag.composer =composer;
        return tag;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean getLossless() {
        return this.lossless;
    }

    public boolean getHires() {
        return this.hires;
    }
}
