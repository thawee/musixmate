package apincer.android.uamp.provider;

import java.io.File;

import apincer.android.uamp.utils.StringUtils;

public class TagReader {
     public enum READ_MODE {SIMPLE,HIERARCHY,SM1,SM2};
     String trackSep="";
     String titleSep=" - ";

    public MediaTag parser(String mediaPath, READ_MODE mode) {
        File file = new File(mediaPath);
        if(!file.exists()) {
            return null;
        }

        MediaTag tag = new MediaTag(mediaPath);
        String text = AndroidFile.getNameWithoutExtension(file);
        if(mode==READ_MODE.SIMPLE) {
            // filename
            tag.setTitle(text);
            tag.setAlbum(text);
            tag.setArtist(text);
        }else if(mode==READ_MODE.HIERARCHY) {
            //artist/album/track artist - title
            //tag.album;
            if(file!=null) {
                tag.setTrack(parseTrackNumber(text));
                tag.setTitle(removeTrackNumber(text));
                if(tag.getTitle().indexOf(titleSep)>0) {
                    // get artist from title
                    String txt = tag.getTitle();
                    tag.setArtist(parseArtist(txt));
                    tag.setTitle(parseTitle(txt));
                }
                file = file.getParentFile();
                if(file !=null) {
                    tag.setAlbum(file.getName());
                    if(StringUtils.isEmpty(tag.getArtist())) {
                        file = file.getParentFile();
                        if (file != null) {
                            tag.setArtist(file.getName());
                        }
                    }
                }
            }
        }else {
            // <track>.<arist> (<featering>) - <tltle>
            // track sep can be .,-, <space>
            // title sep is -

            tag.setTrack(parseTrackNumber(text));
            if(!StringUtils.isEmpty(tag.getTrack())) {
                text = removeTrackNumber(text);
            }
            tag.setArtist(parseArtist(text));
            tag.setTitle(parseTitle(text));
            String featuring = parseFeaturing(tag.getArtist());
            if(!StringUtils.isEmpty(featuring)) {
                tag.setArtist(removeFeaturing(tag.getArtist()));
                tag.setTitle(tag.getTitle()+ " " + featuring);
            }
            if(mode==READ_MODE.SM2) {
                String newArtist = tag.getTitle();
                tag.setTitle(tag.getArtist());
                tag.setArtist(newArtist);
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
