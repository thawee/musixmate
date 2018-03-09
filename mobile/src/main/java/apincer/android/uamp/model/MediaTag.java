package apincer.android.uamp.model;

public class MediaTag implements Cloneable {
     protected int id;
    private String androidTitle;
    private String androidAlbum;
    private String androidArtist;
    private String title;
    private String album;
    private String artist;
    private String albumArtist;
    private String genre;
    private String year;
    private String track;
    private String trackTotal;
    private String disc;
    private String discTotal;
    private String lyrics;
    private String comment;
    private String composer;
    private String grouping;
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
    protected boolean groupingHasChanged;
    protected boolean composerHasChanged;


    public String getAndroidTitle() {
        return androidTitle;
    }

    public void setAndroidTitle(String androidTitle) {
        this.androidTitle = androidTitle;
    }

    public String getAndroidAlbum() {
        return androidAlbum;
    }

    public void setAndroidAlbum(String androidAlbum) {
        this.androidAlbum = androidAlbum;
    }

    public String getAndroidArtist() {
        return androidArtist;
    }

    public void setAndroidArtist(String androidArtist) {
        this.androidArtist = androidArtist;
    }

    MediaTag(int id) {
        this.id = id;
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

    public String getGrouping() {
        return grouping;
    }

    public String getComposer() {
        return composer;
    }
    public void setComposer(String composer) {
        if (composer == null) {
            composer ="";
        }
        if (this.composer == null || !this.composer.equals(composer)) {
            this.composer = composer;
            composerHasChanged = true;
        }
    }

    public void setTitle(String title) {
        if (title == null) {
            title ="";
        }
        if (this.title == null || !this.title.equals(title)) {
            this.title = title;
            titleHasChanged = true;
        }
    }

    public void setAlbum(String album) {
        if (album == null) {
            album = "";
        }
        if (this.album == null || !this.album.equals(album)) {
            this.album = album;
            albumHasChanged = true;
        }
    }

    public void setArtist(String artist) {
        if (artist == null) {
            artist="";
        }
        if (this.artist == null || !this.artist.equals(artist)) {
            this.artist = artist;
            artistHasChanged = true;
        }
    }

    public void setAlbumArtist(String albumArtist) {
        if (albumArtist == null) {
            albumArtist="";
        }
        if (this.albumArtist == null || !this.albumArtist.equals(albumArtist)) {
            this.albumArtist = albumArtist;
            albumArtistHasChanged = true;
        }
    }

    public void setGenre(String genre) {
        if (genre == null) {
            genre ="";
        }
        if (this.genre == null || !this.genre.equals(genre)) {
            this.genre = genre;
            genreHasChanged = true;
        }
    }

    public void setYear(String year) {
        if (year == null) {
            year ="";
        }
        if (this.year == null || !this.year.equals(year)) {
            this.year = year;
            yearHasChanged = true;
        }
    }

    public void setTrack(String track) {
        if (track == null) {
            track="";
        }
        if (this.track == null || !this.track.equals(track)) {
            this.track = track;
            trackHasChanged = true;
        }
    }

    public void setTrackTotal(String trackTotal) {
        if (trackTotal == null) {
            trackTotal="";
        }
        if (this.trackTotal == null || !this.trackTotal.equals(trackTotal)) {
            this.trackTotal = trackTotal;
            trackTotalHasChanged = true;
        }
    }

    public void setDisc(String disc) {
        if (disc == null) {
            disc="";
        }
        if (this.disc == null || !this.disc.equals(disc)) {
            this.disc = disc;
            discHasChanged = true;
        }
    }

    public void setDiscTotal(String discTotal) {
        if (discTotal == null) {
            discTotal="";
        }
        if (this.discTotal == null || !this.discTotal.equals(discTotal)) {
            this.discTotal = discTotal;
            discTotalHasChanged = true;
        }
    }

    public void setLyrics(String lyrics) {
        if (lyrics == null) {
            lyrics="";
        }
        if (this.lyrics == null || !this.lyrics.equals(lyrics)) {
            this.lyrics = lyrics;
            lyricsHasChanged = true;
        }
    }

    public void setComment(String comment) {
        if (comment == null) {
            return;
        }
        if (this.comment == null || !this.comment.equals(comment)) {
            this.comment = comment;
            commentHasChanged = true;
        }
    }

    public void setCountry(String grouping) {
        if (grouping == null) {
            return;
        }
        if (this.grouping == null || !this.grouping.equals(grouping)) {
            this.grouping = grouping;
            groupingHasChanged = true;
        }
    }

    @Override
    public MediaTag clone() {
        MediaTag tag = new MediaTag(id);
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
}
