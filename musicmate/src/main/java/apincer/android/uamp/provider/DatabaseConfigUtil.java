package apincer.android.uamp.provider;


import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import apincer.android.uamp.model.MediaMetadata;

public class DatabaseConfigUtil extends OrmLiteConfigUtil {

    /*public static void main(String[] args) throws Exception {
        writeConfigFile("ormlite_config.txt");
    } */

    private static final Class<?>[] classes = new Class[] {
            MediaMetadata.class
    };

    public static void main(String[] args) throws SQLException, IOException {
        writeConfigFile(new File("musicmate/src/main/res/raw/ormlite_config.txt"), classes);
    }
}
