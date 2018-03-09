package apincer.android.uamp.model;

import apincer.android.uamp.provider.MediaProvider;
import apincer.android.uamp.utils.StringUtils;

public class MediaMetadata  {
    public long getAudioSampleRateAsInt() {
        return audioSampleRateInt;
    }

    public void setAudioSampleRate(long sampling) {
        audioSampleRateInt = sampling;
    }

    public enum MediaQuality {HIRES,HIGH,GOOD,NORMAL,LOW}

    protected int id;
    protected String mediaPath;
    protected String mediaSize;
    protected String displayPath;
    protected transient String audioBitDepth; // 16/24/32 bits
    protected transient String audioSampleRate; //44.1, 48,96, kHz
    protected transient long audioSampleRateInt;

    protected String audioBitRate; //128, 256, 320 kbps
    protected transient String audioCodingFormat; // MP3, FLAC
    private MediaQuality quality = MediaQuality.NORMAL;
    private long audioDuration;

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    private long lastModified = 0;

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    private String mediaType;
    private boolean lossless;

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

    public void setAudioCodingFormat(String audioCodingFormat) {
        this.audioCodingFormat = audioCodingFormat;
    }

    public String getAudioBitRate() {
        return audioBitRate;
    }

    public String getAudioCodingFormat() {
        return audioCodingFormat;
    }

    MediaMetadata(int id) {
        this.id = id;
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
            return MediaProvider.formatDuration(audioDuration);
    }

    public long getAudioDuration() {
        return audioDuration;
    }

    public MediaQuality getQuality() {
        return quality;
    }

    public void setQuality(MediaQuality quality) {
        this.quality = quality;
    }

}
