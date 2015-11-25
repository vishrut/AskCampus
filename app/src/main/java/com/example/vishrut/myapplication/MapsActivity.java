package com.example.vishrut.myapplication;

import android.app.SearchManager;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.plus.Plus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "MapsActivity";
    public static final String AC_USER = "ACUSER";
    public static final String CAMPUS_LOCATION = "CAMPUS_LOCATION";


    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    private Connection conn;

    // ...
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in_button) {
            onSignInClicked();
        }
    }

    private void handleIntent(Intent intent) {
        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // manually launch the real search activity
            final Intent searchIntent = new Intent(getApplicationContext(),
                    SearchableActivity.class);
            // add query to the Intent Extras
            searchIntent.setAction(Intent.ACTION_SEARCH);
            searchIntent.putExtra(SearchManager.QUERY, query);
            startActivityForResult(searchIntent, 100);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void onSignInClicked() {
        // ACUser clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();

        // Show a message to the user that we are signing in.
        // mStatus.setText(R.string.signing_in);
    }

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Map markerInfoMap;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ACUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        getActionBar().hide();
        // setting up drawer
        String[] mDrawerItems = getResources().getStringArray(R.array.drawer_items);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        setUpMapIfNeeded();

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addApi(LocationServices.API)
                .addScope(new Scope(Scopes.PROFILE))
                .addScope(new Scope(Scopes.EMAIL))
                .build();

        onSignInClicked();

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchRequested();
            }
        });
        findViewById(R.id.sign_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignOutClicked();
            }
        });
        markerInfoMap = new HashMap<Marker, CampusLocation>();
        onSearchRequested();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectDrawerItem(position);
        }
    }

    private void selectDrawerItem(int position) {
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    CampusLocation cl = (CampusLocation) markerInfoMap.get(marker);
                    Polygon bPolygon = cl.bPolygon;
                    if(bPolygon.isVisible()) {
                        bPolygon.setVisible(false);
                    }
                    else {
                        bPolygon.setVisible(true);
                    }
                    return false;
                }
            });

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    Intent intent = new Intent(getApplicationContext(), LocationPanelActivity.class);
                    intent.putExtra(MapsActivity.AC_USER, user);
                    intent.putExtra(MapsActivity.CAMPUS_LOCATION, new CampusLocationSerializable((CampusLocation) markerInfoMap.get(marker)));
                    startActivity(intent);
                }
            });
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);

        LatLng USC =new LatLng(34.021766, -118.285993);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(USC, 15));

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        Log.d(TAG, "onConnected:" + bundle);
        mShouldResolve = false;
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);

        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
            new FetchUserInfo(email).execute();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e("", "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                Log.e(TAG,"Could not resolve the connection result");
            }
        } else {
            Log.i(TAG, "The user has signed out.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (requestCode == RC_SIGN_IN) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();
        }

        if(resultCode==100) {
            findViewById(R.id.search_button).setVisibility(View.VISIBLE);
            if (data.getExtras() != null) {
                String locationName = data.getExtras().getString("locationName");
                if (locationName != null)
                    try {
                        new FetchLocationInfo(locationName).execute();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while finding location.");
                    }
            }
        }
    }

    private void onSignOutClicked() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        mMap.clear();
    }

    private class FetchUserInfo extends AsyncTask<Void, Void, ArrayList<String>>{
        String email;

        public FetchUserInfo(String email){
            super();
            this.email = email;
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> userInfo = new ArrayList<>();
            try {
                conn = DbHelper.getDatabaseConnection();
                Statement st = conn.createStatement();
                String fetchSql = "SELECT id from askcampus.user where email='"+email+"' limit 1;";
                ResultSet fetchRS = st.executeQuery(fetchSql);
                if(!fetchRS.next()){
                    Log.i(TAG, "No existing user with the given email found. Creating new user.");
                    String insertSql = "INSERT INTO askcampus.user(email) VALUES('"+email+"');";
                    PreparedStatement insertPreparedSt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                    insertPreparedSt.executeUpdate();
                    ResultSet insertRS = insertPreparedSt.getGeneratedKeys();
                    if (insertRS.next()) {
                        userInfo.add(insertRS.getString(1));
                    }
                    insertRS.close();
                } else {
                    userInfo.add(fetchRS.getString(1));
                    fetchRS.close();
                }
                st.close();
                conn.close();
            } catch (SQLException e){
                Log.e(TAG, "Exception while retrieving user info: " + e);
            }
            return userInfo;
        }

        @Override
        protected void onPostExecute(ArrayList<String> userInfo){
            int userId = Integer.parseInt(userInfo.get(0));
            user = new ACUser(userId, email);
            Log.i(TAG, "ACUser ID: "+userId);
        }
    }

    private class FetchLocationInfo extends AsyncTask<Void, Void, Map<String, String>> {
        String locationName;

        public FetchLocationInfo(String locationName) {
            super();
            this.locationName = locationName;
        }

        @Override
        protected Map<String, String> doInBackground(Void... params) {
            Map<String, String> locationInfo = new HashMap<>();
            try {
                conn = DbHelper.getDatabaseConnection();
                Statement st = conn.createStatement();
                String sql;
                sql = "SELECT id, name, description, ST_AsGeoJSON(position), ST_AsGeoJSON(bgeom) from askcampus.campuslocation where name='"+locationName+"' limit 1;";
                ResultSet rs = st.executeQuery(sql);
                if(rs.next()) {
                    locationInfo.put("id", rs.getString(1));
                    locationInfo.put("name", rs.getString(2));
                    locationInfo.put("description", rs.getString(3));
                    locationInfo.put("position", rs.getString(4));
                    locationInfo.put("bgeom", rs.getString(5));
                }
                rs.close();
                st.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return locationInfo;
        }

        @Override
        protected void onPostExecute(Map<String, String> locationInfo) {
            try {
                mMap.clear();

                // Create marker
                JSONObject positionJsonData = new JSONObject(locationInfo.get("position"));
                JSONArray positionCoords = positionJsonData.getJSONArray("coordinates");
                LatLng position = new LatLng(positionCoords.getDouble(0), positionCoords.getDouble(1));

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(locationInfo.get("name"))
                        .snippet(locationInfo.get("description")));

                // Create polygon
                JSONObject bGeomJsonData = new JSONObject(locationInfo.get("bgeom"));
                JSONArray bGeomCoords = bGeomJsonData.getJSONArray("coordinates").getJSONArray(0);

                PolygonOptions bPolygonOptions = new PolygonOptions()
                                                    .strokeColor(Color.MAGENTA)
                                                    .fillColor(Color.argb(20, 50, 0, 255));

                for(int i=0; i<bGeomCoords.length(); i++){
                    bPolygonOptions.add(new LatLng( bGeomCoords.getJSONArray(i).getDouble(0),
                                                    bGeomCoords.getJSONArray(i).getDouble(1)));
                }

                Polygon bPolygon = mMap.addPolygon(bPolygonOptions);
                bPolygon.setVisible(true);

                // CampusLocation object for easier access
                CampusLocation cl = new CampusLocation( Integer.parseInt(locationInfo.get("id")),
                                                        locationInfo.get("name"),
                                                        locationInfo.get("description"),
                                                        position,
                                                        bPolygon);

                markerInfoMap.put(marker, cl);
            } catch (Exception e){
                Log.e(TAG, "Exception while adding location marker - " + e);
            }
        }
    }
}
