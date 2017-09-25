package apincer.android.uamp.provider;

import android.graphics.Bitmap;

public class MediaTag {
    public String getMediaBitsPerSample() {
        return mediaBitsPerSample;
    }

    public String mediaBitsPerSample;

    public String getDisplayPath() {
        return displayPlath;
    }

    public enum MediaTypes {SONGS,SIMILARITY,FILES}
    protected String mediaPath;
    protected String displayPlath;

    public String getMediaFormat() {
        return mediaFormat;
    }

    protected String mediaFormat;
    protected String mediaSampleRate;
    protected String mediaBitrate;
    protected String mediaSize;
    protected String mediaDuration;
    protected String title;
    protected String album;
    protected String artist;
    protected String albumArtist;
    protected String genre;
    protected String year;
    protected String track;
    protected String trackTotal;
    protected String disc;
    protected String discTotal;
    protected String lyrics;
    protected String comment;
    protected String country;
    protected Bitmap artwork;
    protected boolean titleHasChanged;
    protected boolean albumHasChanged;
    protected boolean artistHasChanged;
    protected boolean albumArtistHasChanged;
    protected boolean genreHasChanged;
    protected boolean yearHasChanged;
    protected boolean trackHasChanged;
    protected boolean trackTotalHasChanged;
    protected boolean discHasChanged;
    protected boolean discTotalHasChanged;
    protected boolean lyricsHasChanged;
    protected boolean commentHasChanged;
    protected boolean countryHasChanged;
    protected boolean isArtworkChanged;

    public MediaTag(String path) {
        mediaPath = path;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public String getMediaSampleRate() {
        return mediaSampleRate;
    }

    public String getMediaBitrate() {
        return mediaBitrate;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public String getGenre() {
        return genre;
    }

    public String getYear() {
        return year;
    }

    public String getTrack() {
        return track;
    }

    public String getTrackTotal() {
        return trackTotal;
    }

    public String getDisc() {
        return disc;
    }

    public String getDiscTotal() {
        return discTotal;
    }

    public String getLyrics() {
        return lyrics;
    }

    public String getComment() {
        return comment;
    }

    public String getCountry() {
        return country;
    }

    public Bitmap getArtwork() {
        return artwork;
    }

    public void softSetTitle(String title) {
        if (title == null) {
            return;
        }
        if (this.title == null || !this.title.equals(title)) {
            this.title = title;
            titleHasChanged = true;
        }
    }

    public void softSetAlbum(String album) {
        if (album == null) {
            return;
        }
        if (this.album == null || !this.album.equals(album)) {
            this.album = album;
            albumHasChanged = true;
        }
    }

    public void softSetArtist(String artist) {
        if (artist == null) {
            return;
        }
        if (this.artist == null || !this.artist.equals(artist)) {
            this.artist = artist;
            artistHasChanged = true;
        }
    }

    public void softSetAlbumArtist(String albumArtist) {
        if (albumArtist == null) {
            return;
        }
        if (this.albumArtist == null || !this.albumArtist.equals(albumArtist)) {
            this.albumArtist = albumArtist;
            albumArtistHasChanged = true;
        }
    }

    public void softSetGenre(String genre) {
        if (genre == null) {
            return;
        }
        if (this.genre == null || !this.genre.equals(genre)) {
            this.genre = genre;
            genreHasChanged = true;
        }
    }

    public void softSetYear(String year) {
        if (year == null) {
            return;
        }
        if (this.year == null || !this.year.equals(year)) {
            this.year = year;
            yearHasChanged = true;
        }
    }

    public void softSetTrack(String track) {
        if (track == null) {
            return;
        }
        if (this.track == null || !this.track.equals(track)) {
            this.track = track;
            trackHasChanged = true;
        }
    }

    public void softSetTrackTotal(String trackTotal) {
        if (trackTotal == null) {
            return;
        }
        if (this.trackTotal == null || !this.trackTotal.equals(trackTotal)) {
            this.trackTotal = trackTotal;
            trackTotalHasChanged = true;
        }
    }

    public void softSetDisc(String disc) {
        if (disc == null) {
            return;
        }
        if (this.disc == null || !this.disc.equals(disc)) {
            this.disc = disc;
            discHasChanged = true;
        }
    }

    public void softSetDiscTotal(String discTotal) {
        if (discTotal == null) {
            return;
        }
        if (this.discTotal == null || !this.discTotal.equals(discTotal)) {
            this.discTotal = discTotal;
            discTotalHasChanged = true;
        }
    }

    public void softSetLyrics(String lyrics) {
        if (lyrics == null) {
            return;
        }
        if (this.lyrics == null || !this.lyrics.equals(lyrics)) {
            this.lyrics = lyrics;
            lyricsHasChanged = true;
        }
    }

    public void softSetComment(String comment) {
        if (comment == null) {
            return;
        }
        if (this.comment == null || !this.comment.equals(comment)) {
            this.comment = comment;
            commentHasChanged = true;
        }
    }

    public void softSetCountry(String country) {
        if (country == null) {
            return;
        }
        if (this.country == null || !this.country.equals(country)) {
            this.country = country;
            countryHasChanged = true;
        }
    }

    public String getMediaSize() {
        return mediaSize;
    }

    public String getMediaDuration() {
        return mediaDuration;
    }
}
