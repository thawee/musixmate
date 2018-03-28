package apincer.android.uamp.musicbrainz;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apincer.android.uamp.model.AlbumInfo;
import apincer.android.uamp.model.RecordingItem;
import apincer.android.uamp.musicbrainz.coverart.Coverart;
import apincer.android.uamp.musicbrainz.coverart.ImagesItem;
import apincer.android.uamp.musicbrainz.recording.Artist;
import apincer.android.uamp.musicbrainz.recording.ArtistCreditItem;
import apincer.android.uamp.musicbrainz.recording.Recording;
import apincer.android.uamp.musicbrainz.recording.RecordingsItem;
import apincer.android.uamp.musicbrainz.recording.ReleasesItem;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Administrator on 12/20/17.
 */

public class MusicBrainz {
    private static final String MB_URL = "http://musicbrainz.org/";
    private static final String COVER_URL = "http://coverartarchive.org/";

    public static Retrofit createMBRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(MB_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    public static Retrofit createCoverRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(COVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static Retrofit createRetrofit() {
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static void main(String [] args) {
        String title = "...Baby One More Time";
        String artist = "Britney Spears";
        String album = null;
        List<RecordingItem> songs = findSongInfo(title,artist,album);
        System.out.println(songs);
        System.out.println(findAlbumArtsxx(songs));
    }
    public static List<RecordingItem> findSongInfo(String title, String artist, String album) {
        final List<RecordingItem> songs = new ArrayList<>();
         try {
             Recording recording = null;
             if(!StringUtils.isEmpty(title)) {
                 recording = fetchRecordings(title, artist, album);
             }else {
                 recording = fetchRecordings(title, artist, album);
             }
            if(!hasRecordings(recording) && !StringUtils.isEmpty(title)) {
                // trying with title only
                recording = fetchRecordings(title,null,null);
            }
            if(!hasRecordings(recording)) {
                return songs;
            }
            LogHelper.logToFile("get Recordings ",  recording.getRecordings().size()+" records; "); //+ recording.getRecordings());
            for(RecordingsItem item  : recording.getRecordings()) {
                        RecordingItem song = new RecordingItem();
                        song.id = item.getId();
                        song.title = item.getTitle();
                        parseArtist(song, item);
                        parseAlbum(song, item);
                        songs.add(song);
            }
        }catch (Exception ex) {
             LogHelper.logToFile("Cannot get musicBrainz ", Log.getStackTraceString(ex));
        }
        return songs;
    }

    private static boolean hasRecordings(Recording recording) {
       return !(recording==null || recording.getRecordings().size()==0);
    }

    private static Recording fetchRecordings(String title, String artist, String album) {
        try {
            String query = createQuery(title, artist, album);
            if(StringUtils.isEmpty(query))  {
                return null;
            }
            Retrofit retrofit = createMBRetrofit();
            EndpointInterface eIntf = retrofit.create(EndpointInterface.class);
            Map<String, String> data = new HashMap<>();
            data.put("fmt", "json");
            data.put("limit", String.valueOf(10));
            data.put("query", query);
            LogHelper.logToFile("musicBrainz", "query by: "+query);
            Call call =  eIntf.findRecordings(data);
            Response response =  call.execute();
            return  (Recording) response.body();
        }catch (Exception ex) {
            //LogHelper.logToFile("GetCoverArt", "No coverart for "+album.name);
            //ex.printStackTrace();
        }
        return null;
    }

    public static List<AlbumInfo> findAlbumArtsxx(List<RecordingItem> songs) {
        List<AlbumInfo> albums = new ArrayList<>();
        Map<String, String> albumMap = new HashMap<>();
        for(RecordingItem song: songs) {
            if(!albumMap.containsKey(song.albumId)) {
                albumMap.put(song.albumId, song.album);
                AlbumInfo album = new AlbumInfo();
                album.id = song.albumId;
                album.name = song.album;
                if(fetchCoverart(album)) {
                    albums.add(album);
                }
            }
        }
        LogHelper.logToFile("get Albums ",  albums.size()+" albums ");
        return albums;
    }

    private static boolean fetchCoverart(AlbumInfo album) {
        try {
            Retrofit retrofit = createCoverRetrofit();
            EndpointInterface eIntf = retrofit.create(EndpointInterface.class);
            Call call = eIntf.getCoverart(album.getId());
            Response response = call.execute();
            Coverart coverart = (Coverart) response.body();
            List<ImagesItem> images = coverart.getImages();
            for(ImagesItem image: images) {
                if(image.isFront()) {
                    if(image.getThumbnails()!=null) {
                        album.smallCoverUrl = image.getThumbnails().getSmall();
                        album.largeCoverUrl = image.getThumbnails().getLarge();
                        return true;
                    }
                }
            }
        }catch (Exception ex) {
            //LogHelper.logToFile("GetCoverArt", "No coverart for "+album.name);
            //ex.printStackTrace();
        }
        return false;
    }

    private static void parseArtist(RecordingItem song, RecordingsItem item) {
        List<ArtistCreditItem> artistCreditList = item.getArtistCredit();
        if(artistCreditList!=null && artistCreditList.size()>0) {
            for(ArtistCreditItem artistCreditItem:artistCreditList) {
                 Artist artist = artistCreditItem.getArtist();
                 if(artist!=null && !StringUtils.isEmpty(artist.getName())) {
                     song.artist = artist.getName();
                     song.artistId = artist.getId();
                     break;
                 }
            }
        }
    }

    private static void parseAlbum(RecordingItem song, RecordingsItem item) {
        List<ReleasesItem> releaseList = item.getReleases();
        if(releaseList!=null && releaseList.size()>0) {
            ReleasesItem release =  releaseList.get(0);
            song.albumId = release.getId();
            song.album = release.getTitle();
            song.year = release.getDate();
        }
    }

    private static String createQuery(String title, String artist, String album) throws UnsupportedEncodingException {
        String query = "";
        if (!StringUtils.isEmpty(title)) {
            //query += URLEncoder.encode(  surroundWithQuotes(title), "UTF-8");
            query += surroundWithQuotes(title);
            //query += surroundWithQuotes(URLEncoder.encode(title, "UTF-8"));
        }
        if (!StringUtils.isEmpty(artist)) {
            //query += URLEncoder.encode(" AND artist:" + surroundWithQuotes(artist) , "UTF-8");
            query += " AND artist:" + surroundWithQuotes(artist);
            //query += " AND artist:" + surroundWithQuotes(URLEncoder.encode(artist, "UTF-8"));
        }
        if (!StringUtils.isEmpty(album)) {
            //query += URLEncoder.encode(" AND release:" + surroundWithQuotes(album), "UTF-8");
            query += " AND release:" + surroundWithQuotes(album);
            //query += " AND release:" + surroundWithQuotes(URLEncoder.encode(album, "UTF-8"));
        }
        return query;
    }

    private static String surroundWithQuotes(String s) {
        String s2 = s.replaceAll("\"", ""); // remove all quotes in between
        return "\"" + s2 + "\"";
    }

    public static AlbumInfo getAlbumArt(AlbumInfo album) {
        if(fetchCoverart(album)) {
            return album;
        }
        return album;
    }

    public static InputStream getInputStream(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            okhttp3.Call call = client.newCall(new Request.Builder().url(url).get().build());
            okhttp3.Response response = call.execute();
            return new BufferedInputStream((response.body()).byteStream(), 1024 * 8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Collection<? extends AlbumInfo> populateAlbumInfo(List<RecordingItem> songs) {
        List<AlbumInfo> albums = new ArrayList<>();
        Map<String, String> albumMap = new HashMap<>();
        for(RecordingItem song: songs) {
            if(!albumMap.containsKey(song.albumId)) {
                albumMap.put(song.albumId, song.album);
                AlbumInfo album = new AlbumInfo();
                album.id = song.albumId;
                album.name = song.album;
                albums.add(album);
            }
        }
        return albums;
    }
}
