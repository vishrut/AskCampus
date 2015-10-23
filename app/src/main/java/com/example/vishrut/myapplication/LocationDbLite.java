package com.example.vishrut.myapplication;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * Contains logic to return specific locations from the SQLite location database, and
 * load the locations table when it needs to be created.
 */
public class LocationDbLite {
    private static final String TAG = "LocationDbLite";

    //The columns we'll include in the locations table
    public static final String KEY_LOCATION_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_LOCATIION_DESC = SearchManager.SUGGEST_COLUMN_TEXT_2;

    private static final String DATABASE_NAME = "locationDbLite";
    private static final String FTS_VIRTUAL_TABLE = "FTSlocations";
    private static final int DATABASE_VERSION = 5;

    private final LocationDbLiteHelper mDatabaseOpenHelper;
    private static final HashMap<String,String> mColumnMap = buildColumnMap();

    /**
     * Constructor
     * @param context The Context within which to work, used to create the DB
     */
    public LocationDbLite(Context context) {
        mDatabaseOpenHelper = new LocationDbLiteHelper(context);
    }

    /**
     * Builds a map for all columns that may be requested, which will be given to the
     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include
     * all columns, even if the value is the key. This allows the ContentProvider to request
     * columns w/o the need to know real column names and create the alias itself.
     */
    private static HashMap<String,String> buildColumnMap() {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put(KEY_LOCATION_NAME, KEY_LOCATION_NAME);
        map.put(KEY_LOCATIION_DESC, KEY_LOCATIION_DESC);
        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }

    /**
     * Returns a Cursor positioned at the location specified by rowId
     *
     * @param rowId id of location to retrieve
     * @param columns The columns to include, if null then all are included
     * @return Cursor positioned to matching location, or null if not found.
     */
    public Cursor getLocation(String rowId, String[] columns) {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[] {rowId};

        return query(selection, selectionArgs, columns);

        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE rowid = <rowId>
         */
    }

    /**
     * Returns a Cursor over all locations that match the given query
     *
     * @param query The string to search for
     * @param columns The columns to include, if null then all are included
     * @return Cursor over all locations that match, or null if none found.
     */
    public Cursor getLocationMatches(String query, String[] columns) {
        String selection = KEY_LOCATION_NAME + " MATCH ?";
        String[] selectionArgs = new String[] {query+"*"};

        return query(selection, selectionArgs, columns);

        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE <KEY_LOCATION_NAME> MATCH 'query*'
         * which is an FTS3 search for the query text (plus a wildcard) inside the location name column.
         *
         * - "rowid" is the unique id for all rows but we need this value for the "_id" column in
         *    order for the Adapters to work, so the columns need to make "_id" an alias for "rowid"
         * - "rowid" also needs to be used by the SUGGEST_COLUMN_INTENT_DATA alias in order
         *   for suggestions to carry the proper intent data.
         *   These aliases are defined in the LocationInfoProvider when queries are made.
         * - This can be revised to also search the description text with FTS3 by changing
         *   the selection clause to use FTS_VIRTUAL_TABLE instead of KEY_LOCATION_NAME (to search across
         *   the entire table, but sorting the relevance could be difficult.
         */
    }

    /**
     * Performs a database query.
     * @param selection The selection clause
     * @param selectionArgs Selection arguments for "?" components in the selection
     * @param columns The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        /* The SQLiteBuilder provides a map for all possible columns requested to
         * actual columns in the database, creating a simple column alias mechanism
         * by which the ContentProvider does not need to know the real column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }


    /**
     * This creates/opens the database.
     */
    private static class LocationDbLiteHelper extends SQLiteOpenHelper {

        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

        /* Note that FTS3 does not support column constraints and thus, you cannot
         * declare a primary key. However, "rowid" is automatically used as a unique
         * identifier, so when making requests, we will use "_id" as an alias for "rowid"
         */
        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts3 (" +
                        KEY_LOCATION_NAME + ", " +
                        KEY_LOCATIION_DESC + ");";

        LocationDbLiteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
            loadLocationDbLite();
        }

        /**
         * Starts a thread to load the database table with locations
         */
        private void loadLocationDbLite() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        loadLocations();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        private void loadLocations() throws IOException {
            Log.d(TAG, "Loading locations...");
            ArrayList<String> index = new ArrayList<>();

            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            try {
                String endpoint = "spatialinstance.cm2us8yyqh8k.us-west-2.rds.amazonaws.com";
                String databaseName = "spatialdb";
                String url = "jdbc:postgresql://" + endpoint + "/" + databaseName;

                Properties props = new Properties();
                props.setProperty("user", "spatialuser");
                props.setProperty("password", "spatialdatabase");

                Connection conn = DriverManager.getConnection(url, props);

                Statement st = conn.createStatement();
                String sql;
                sql = "SELECT name, description FROM askcampus.campuslocation;";
                ResultSet rs = st.executeQuery(sql);
                while(rs.next()) {
                    String locationName = rs.getString(1);
                    String description = rs.getString(2);
                    index.add(locationName+":"+description);
                    Log.d(TAG, locationName);
                }
                rs.close();
                st.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            for(String line:index) {
                String[] strings = TextUtils.split(line, ":");
                if (strings.length < 2) continue;
                long id = addLocation(strings[0].trim(), strings[1].trim());
                if (id < 0) {
                    Log.e(TAG, "unable to add location: " + strings[0].trim());
                }
            }

            Log.d(TAG, "DONE loading locations.");
        }

        /**
         * Add a location to the SQLite database.
         * @return rowId or -1 if failed
         */
        public long addLocation(String name, String description) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_LOCATION_NAME, name);
            initialValues.put(KEY_LOCATIION_DESC, description);

            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }
    }
}
