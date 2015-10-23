package com.example.vishrut.myapplication;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Provides access to the locationDbLite database.
 */
public class LocationInfoProvider extends ContentProvider {
    String TAG = "LocationInfoProvider";

    public static String AUTHORITY = "com.example.vishrut.myapplication.LocationInfoProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/locationDbLite");

    // MIME types used for searching locations or looking up a single description
    public static final String LOCATION_NAME_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
            "/vnd.example.vishrut.myapplication";
    public static final String LOCATION_DESC_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
            "/vnd.example.vishrut.myapplication";

    private LocationDbLite mLocationDbLite;

    // UriMatcher stuff
    private static final int SEARCH_LOCATION = 0;
    private static final int GET_LOCATION = 1;
    private static final int SEARCH_SUGGEST = 2;
    private static final int REFRESH_SHORTCUT = 3;
    private static final UriMatcher sURIMatcher = buildUriMatcher();

    /**
     * Builds up a UriMatcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        // to get descriptions...
        matcher.addURI(AUTHORITY, "locationDbLite", SEARCH_LOCATION);
        matcher.addURI(AUTHORITY, "locationDbLite/#", GET_LOCATION);
        // to get suggestions...
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

        /* The following are unused in this implementation, but if we include
         * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our suggestions table, we
         * could expect to receive refresh queries when a shortcutted suggestion is displayed in
         * Quick Search Box, in which case, the following Uris would be provided and we
         * would return a cursor with a single item representing the refreshed suggestion data.
         */
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, REFRESH_SHORTCUT);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", REFRESH_SHORTCUT);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mLocationDbLite = new LocationDbLite(getContext());
        return true;
    }

    /**
     * Handles all the locationDbLite searches and suggestion queries from the Search Manager.
     * When requesting a specific location, the uri alone is required.
     * When searching all of the locationDbLite for matches, the selectionArgs argument must carry
     * the search query as the first element.
     * All other arguments are ignored.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Use the UriMatcher to see what kind of query we have and format the db query accordingly
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                            "selectionArgs must be provided for the Uri: " + uri);
                }
                return getSuggestions(selectionArgs[0]);
            case SEARCH_LOCATION:
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                            "selectionArgs must be provided for the Uri: " + uri);
                }
                return search(selectionArgs[0]);
            case GET_LOCATION:
                return getLocation(uri);
            case REFRESH_SHORTCUT:
                return refreshShortcut(uri);
            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    private Cursor getSuggestions(String query) {
        query = query.toLowerCase();
        String[] columns = new String[] {
                BaseColumns._ID,
                LocationDbLite.KEY_LOCATION_NAME,
                LocationDbLite.KEY_LOCATIION_DESC,
       /* SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
                        (only if you want to refresh shortcuts) */
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

        return mLocationDbLite.getLocationMatches(query, columns);
    }

    private Cursor search(String query) {
        query = query.toLowerCase();
        String[] columns = new String[] {
                BaseColumns._ID,
                LocationDbLite.KEY_LOCATION_NAME,
                LocationDbLite.KEY_LOCATIION_DESC};

        return mLocationDbLite.getLocationMatches(query, columns);
    }

    private Cursor getLocation(Uri uri) {
        String rowId = uri.getLastPathSegment();
        String[] columns = new String[] {
                LocationDbLite.KEY_LOCATION_NAME,
                LocationDbLite.KEY_LOCATIION_DESC};

        return mLocationDbLite.getLocation(rowId, columns);
    }

    private Cursor refreshShortcut(Uri uri) {
      /* This won't be called with the current implementation, but if we include
       * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our suggestions table, we
       * could expect to receive refresh queries when a shortcutted suggestion is displayed in
       * Quick Search Box. In which case, this method will query the table for the specific
       * location, using the given item Uri and provide all the columns originally provided with the
       * suggestion query.
       */
        String rowId = uri.getLastPathSegment();
        String[] columns = new String[] {
                BaseColumns._ID,
                LocationDbLite.KEY_LOCATION_NAME,
                LocationDbLite.KEY_LOCATIION_DESC,
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

        return mLocationDbLite.getLocation(rowId, columns);
    }

    /**
     * This method is required in order to query the supported types.
     * It's also useful in our own query() method to determine the type of Uri received.
     */
    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case SEARCH_LOCATION:
                return LOCATION_NAME_MIME_TYPE;
            case GET_LOCATION:
                return LOCATION_DESC_MIME_TYPE;
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    // Other required implementations...

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}