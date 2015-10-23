package com.example.vishrut.myapplication;

import android.app.SearchManager;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "MapsActivity";

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

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
            Log.i("&&&&&",intent.toString());
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
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();

        // Show a message to the user that we are signing in.
        // mStatus.setText(R.string.signing_in);
    }

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Map markerInfoMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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

        //new FetchSQL(uri.toString()).execute();//resume here
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchRequested();
                //v.setVisibility(View.GONE);
            }
        });
        findViewById(R.id.sign_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignOutClicked();
            }
        });
        markerInfoMap = new WeakHashMap<Marker, ArrayList<String>>();
        onSearchRequested();
        //handleIntent(getIntent());
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
                    return false;
                }
            });

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    Intent intent = new Intent(getApplicationContext(), LocationPanelActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near USC.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
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
            Log.i(TAG, "email: " + email);
            //onSignOutClicked();

            /*
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .title("Current location"));*/
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
                // showErrorDialog(connectionResult);
            }
        } else {
            Log.i(TAG, "The user has signed out.");
            // showSignedOutUI();
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
                Log.i("***", locationName);
                if (locationName != null)
                    try {
                        new FetchSQL(locationName).execute();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while finding location.");
                    }
            }
        }
    }

    private void onSignOutClicked() {
        // Clear the default account so that GoogleApiClient will not automatically
        // connect in the future.
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        mMap.clear();
    }

    private class FetchSQL extends AsyncTask<Void,Void,ArrayList<String>> {
        String locationName;

        public FetchSQL(String locationName) {
            super();
            this.locationName = locationName;
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> locationInfo = new ArrayList<>();
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
                sql = "SELECT name, description, ST_AsGeoJSON(bgeom) from askcampus.campuslocation where name='"+locationName+"' limit 1;";
                ResultSet rs = st.executeQuery(sql);
                while(rs.next()) {
                    locationInfo.add(rs.getString(1));
                    locationInfo.add(rs.getString(2));
                    locationInfo.add(rs.getString(3));
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
        protected void onPostExecute(ArrayList<String> locationInfo) {
            Log.i("&&&locationInfo", locationInfo.toString());
            try {
                JSONObject geoJsonData = new JSONObject(locationInfo.get(2));
                JSONArray coordArray = geoJsonData.getJSONArray("coordinates");
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(coordArray.getDouble(0), coordArray.getDouble(1)))
                        .title(locationInfo.get(0))
                        .snippet(locationInfo.get(1)));

                markerInfoMap.put(marker, locationInfo);
            } catch (Exception e){
                Log.e(TAG, "Exception while adding location marker");
            }
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(0, 0))
                    .title("Search location"));
        }
    }
}
