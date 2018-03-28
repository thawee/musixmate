package apincer.android.uamp.provider;

import java.io.File;

import apincer.android.uamp.model.MediaItem;
import apincer.android.uamp.model.MediaTag;
import apincer.android.uamp.utils.StringUtils;

public class TagReader {
     public enum READ_MODE {SIMPLE,HIERARCHY,SMART};
     String titleSep="- ";
    String titleSep2=". ";
    String artistBegin="[";
    String artistEnd="]";

    public MediaTag parser(MediaItem item, READ_MODE mode) {
        File file = new File(item.getPath());
        if(!file.exists()) {
            return null;
        }

        MediaTag tag = item.getTag().clone();
        String text = AndroidFile.getNameWithoutExtension(file);
        if(mode==READ_MODE.SIMPLE) {
            // filename
            tag.setTitle(text);
            tag.setAlbum(text);
            tag.setArtist(text);
        }else {
            // <track>.<arist> (<featering>) - <tltle>
            // (<track>) [<arist>] <tltle>
            // track sep can be .,-, <space>
            // title sep is -

            text = parseTrackNumber(tag, text);
            //tag.setTrack(parseTrackNumber(text));
            //if(!StringUtils.isEmpty(tag.getTrack())) {
            //    text = removeTrackNumber(text);
            //}
            text = parseArtist(tag, text);
            //tag.setArtist(parseArtist(text));

            tag.setTitle(parseTitle(text));
            String featuring = parseFeaturing(tag.getArtist());
            if(!StringUtils.isEmpty(featuring)) {
                tag.setArtist(removeFeaturing(tag.getArtist()));
                tag.setTitle(tag.getTitle()+ " " + featuring);
            }

            if(mode==READ_MODE.HIERARCHY && (file!=null)) {
                    //tag.setTrack(parseTrackNumber(text));
                    //tag.setTitle(removeTrackNumber(text));
                    //if(tag.getTitle().indexOf(titleSep)>0) {
                    // get artist from title
                    //    String txt = tag.getTitle();
                    //    tag.setArtist(parseArtist(txt));
                    //    tag.setTitle(parseTitle(txt));
                    //}
                    file = file.getParentFile();
                    tag.setAlbum(file.getName());
                    if(StringUtils.isEmpty(tag.getArtist())) {
                        file = file.getParentFile();
                        tag.setArtist(file.getName());

                    }
            }
        }

        return tag;
    }


    private String parseArtist(MediaTag tag, String text) {
        if(text.startsWith(artistBegin) && text.indexOf(artistEnd)>1) {
            tag.setArtist(text.substring(text.indexOf(artistBegin)+artistBegin.length(), text.indexOf(artistEnd)));
            text = text.substring(text.indexOf(artistEnd)+artistEnd.length(),text.length());
        }else {
            int titleIndx = text.indexOf(titleSep);
            if (titleIndx >= 0) {
                tag.setArtist(StringUtils.trimToEmpty(text.substring(0, titleIndx)));
                text = StringUtils.trimToEmpty(text.substring(titleIndx+titleSep.length(), text.length()));
            }
        }
        return StringUtils.trimToEmpty(text);
    }

    private String parseTrackNumber(MediaTag tag, String text) {String trackNo = "";
        int i =0;
        for(;i<text.length();i++) {
            char ch = text.charAt(i);
            if (Character.isDigit(ch)) {
                trackNo = trackNo + ch;
            }else if('('==ch) {
                continue;
            }else if(')'==ch){
                i++;
                break;
            }else {
                i++; //eat ny none number i.e space or .
                break;
            }
        }
        if(i < text.length()) {
            text = text.substring(i, text.length());
        }
        tag.setTrack(trackNo);
        return StringUtils.trimToEmpty(text);
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
            if(titleIndx>=0) {
                text = StringUtils.trimToEmpty(text.substring(titleIndx+titleSep.length(), text.length()));
            }
            if(text.indexOf("_")>=0) {
                text = text.substring(0, text.indexOf("_"));
            }
            return text;
    }
}
