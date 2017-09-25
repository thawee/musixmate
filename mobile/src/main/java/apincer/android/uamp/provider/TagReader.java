package apincer.android.uamp.provider;

import java.io.File;

import apincer.android.uamp.file.AndroidFile;
import apincer.android.uamp.item.MediaItem;
import apincer.android.uamp.utils.StringUtils;

/**
 * Created by Administrator on 8/28/17.
 */

public class TagReader {
     public enum READ_MODE {SIMPLE,HIERARCHY,SM1,SM2};
     String trackSep="";
     String titleSep=" - ";

    public class Tag {
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

        public String getTrack() {
            return track;
        }

        public void setTrack(String track) {
            this.track = track;
        }

        String title = "";
        String artist = "";
        String album = "";
        String track = "";
    }

    public Tag parser(String mediaPath, READ_MODE mode) {
        File file = new File(mediaPath);
        if(!file.exists()) {
            return null;
        }

        Tag tag = new Tag();
        String text = AndroidFile.getNameWithoutExtension(file);
        if(mode==READ_MODE.SIMPLE) {
            // filename
            tag.title = text;
            tag.album = text;
            tag.artist = text;
        }else if(mode==READ_MODE.HIERARCHY) {
            //artist/album/track artist - title
            //tag.album;
            if(file!=null) {
                tag.track = parseTrackNumber(text);
                tag.title = removeTrackNumber(text);
                if(tag.title.indexOf(titleSep)>0) {
                    // get artist from title
                    String txt = tag.title;
                    tag.artist = parseArtist(txt);
                    tag.title = parseTitle(txt);
                }
                file = file.getParentFile();
                if(file !=null) {
                    tag.album = file.getName();
                    if(StringUtils.isEmpty(tag.artist)) {
                        file = file.getParentFile();
                        if (file != null) {
                            tag.artist = file.getName();
                        }
                    }
                }
            }
        }else {
            // <track>.<arist> (<featering>) - <tltle>
            // track sep can be .,-, <space>
            // title sep is -

            tag.track = parseTrackNumber(text);
            if(!StringUtils.isEmpty(tag.track)) {
                text = removeTrackNumber(text);
            }
            tag.artist = parseArtist(text);
            tag.title = parseTitle(text);
            String featuring = parseFeaturing(tag.artist);
            if(!StringUtils.isEmpty(featuring)) {
                tag.artist = removeFeaturing(tag.artist);
                tag.title = tag.title + " " + featuring;
            }
            if(mode==READ_MODE.SM2) {
                String newArtist = tag.title;
                tag.title = tag.artist;
                tag.artist = newArtist;
            }
        }

        return tag;
    }

    private String removeFeaturing(String artist) {
        if(artist.indexOf("(") >0 && artist.indexOf(")") >0) {
            return artist.substring(0, artist.indexOf("("));
        }
        return "";
    }

    private String parseFeaturing(String artist) {
        if(artist.indexOf("(") >0 && artist.indexOf(")") >0) {
            return artist.substring(artist.indexOf("(")+1, artist.indexOf(")"));
        }
        return "";
    }

    private String parseTitle(String text) {
        int titleIndx = text.indexOf(titleSep);
            if(titleIndx>0) {
                return StringUtils.trimToEmpty(text.substring(titleIndx+titleSep.length(), text.length()));
            }
            return text;
    }

    private String parseArtist(String text) {
        int titleIndx = text.indexOf(titleSep);
        if(titleIndx>0) {
            return StringUtils.trimToEmpty(text.substring(0,titleIndx));
        }
        return "";
    }

    private String removeTrackNumber(String text) {
        return text.substring(text.indexOf(trackSep)+1, text.length());
    }

    private String parseTrackNumber(String text) {
        // <track>.<arist> (<featering>)-<tltle>
        // track sep can be .,-, <space>
        String trackNo = "";
        for(int i=0;i<text.length();i++) {
            char ch = text.charAt(i);
            if(Character.isDigit(ch)) {
                trackNo = trackNo + ch;
            }else {
                trackSep = String.valueOf(ch);
                break;
            }
        }
        return trackNo;
    }
}
