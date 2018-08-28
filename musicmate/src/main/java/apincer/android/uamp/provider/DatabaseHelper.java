package apincer.android.uamp.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import apincer.android.uamp.R;
import apincer.android.uamp.model.MediaMetadata;
import apincer.android.uamp.utils.LogHelper;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    public static final String TAG = LogHelper.makeLogTag(DatabaseHelper.class);

    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "uampdb.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 4;
    private Dao<MediaMetadata, String> mediaDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
       // super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            LogHelper.i(TAG, "onCreate");
            TableUtils.createTable(connectionSource, MediaMetadata.class);
        } catch (SQLException e) {
            LogHelper.e(TAG, "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            LogHelper.i(TAG, "onUpgrade");
            TableUtils.dropTable(connectionSource, MediaMetadata.class, true);
            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            LogHelper.e(TAG, "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<MediaMetadata, String> getMediaDao() throws SQLException {
        if (mediaDao == null) {
            mediaDao = getDao(MediaMetadata.class);
        }
        return mediaDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        mediaDao = null;
    }
}
