package com.example.vishrut.myapplication;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationPanelActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LocationPanelActivity";
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    private ACUser user;
    private CampusLocationSerializable sCampusLocation;
    private Connection conn;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildGoogleApiClient();

        user = (ACUser)getIntent().getSerializableExtra(MapsActivity.AC_USER);
        sCampusLocation = (CampusLocationSerializable)getIntent().getSerializableExtra(MapsActivity.CAMPUS_LOCATION);

        setContentView(R.layout.activity_location_panel);
        getActionBar().hide();

        TextView lName = (TextView) findViewById(R.id.location_name);
        lName.setText(sCampusLocation.name);

        TextView lDesc = (TextView) findViewById(R.id.location_description);
        lDesc.setText(sCampusLocation.description);

        if (sCampusLocation.id != 1) {
            TextView lRush = (TextView) findViewById(R.id.rushHour);
            lRush.setText("Current Status: Not Rushed");
            lRush.setTextColor(Color.GREEN);
        }

        findViewById(R.id.check_in_button).setVisibility(View.GONE);

        findViewById(R.id.ask_question_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListQuestionsActivity();
            }
        });

        findViewById(R.id.check_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CheckInUser().execute();
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.i(TAG, "Building API client");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            new CalculateCheckInViability(sCampusLocation.name).execute();
        } else {
            Log.i(TAG, "Location not found");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    // Source for point in polygon algorithm is accredited to http://stackoverflow.com/users/3809834/ylag75
    public boolean pointInPolygon(LatLng point, PolygonOptions polygonOptions) {
        // ray casting alogrithm http://rosettacode.org/wiki/Ray-casting_algorithm
        int crossings = 0;
        List<LatLng> path = polygonOptions.getPoints();
        path.remove(path.size() - 1); //remove the last point that is added automatically by getPoints()

        // for each edge
        for (int i = 0; i < path.size(); i++) {
            LatLng a = path.get(i);
            int j = i + 1;
            //to close the last edge, you have to take the first point of your polygon
            if (j >= path.size()) {
                j = 0;
            }
            LatLng b = path.get(j);
            if (rayCrossesSegment(point, a, b)) {
                crossings++;
            }
        }

        // odd number of crossings?
        return (crossings % 2 == 1);
    }

    public boolean rayCrossesSegment(LatLng point, LatLng a, LatLng b) {
        // Ray Casting algorithm checks, for each segment, if the point is 1) to the left of the segment and 2) not above nor below the segment. If these two conditions are met, it returns true
        double px = point.longitude,
                py = point.latitude,
                ax = a.longitude,
                ay = a.latitude,
                bx = b.longitude,
                by = b.latitude;
        if (ay > by) {
            ax = b.longitude;
            ay = b.latitude;
            bx = a.longitude;
            by = a.latitude;
        }
        // alter longitude to cater for 180 degree crossings
        if (px < 0 || ax < 0 || bx < 0) {
            px += 360;
            ax += 360;
            bx += 360;
        }
        // if the point has the same latitude as a or b, increase slightly py
        if (py == ay || py == by) py += 0.00000001;

        // if the point is above, below or to the right of the segment, it returns false
        if ((py > by || py < ay) || (px > Math.max(ax, bx))) {
            return false;
        }
        // if the point is not above, below or to the right and is to the left, return true
        else if (px < Math.min(ax, bx)) {
            return true;
        }
        // if the two above conditions are not met, you have to compare the slope of segment [a,b] (the red one here) and segment [a,p] (the blue one here) to see if your point is to the left of segment [a,b] or not
        else {
            double red = (ax != bx) ? ((by - ay) / (bx - ax)) : Double.POSITIVE_INFINITY;
            double blue = (ax != px) ? ((py - ay) / (px - ax)) : Double.POSITIVE_INFINITY;
            return (blue >= red);
        }
    }

    private void startListQuestionsActivity(){
        Intent intent = new Intent(getApplicationContext(), ListQuestionsActivity.class);
        intent.putExtra(MapsActivity.AC_USER, user);
        intent.putExtra(MapsActivity.CAMPUS_LOCATION, sCampusLocation);
        startActivity(intent);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_location_panel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    private class CheckInUser extends AsyncTask<Void, Void, Integer> {

        public CheckInUser() {
            super();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return 1;
        }

        @Override
        protected void onPostExecute(Integer status) {
            findViewById(R.id.check_in_button).setVisibility(View.GONE);
            DialogFragment dialog = new CheckedInDialog();
            dialog.show(getFragmentManager(), TAG);
        }
    }

    private class CalculateCheckInViability extends AsyncTask<Void, Void, Map<String, String>> {
        String locationName;

        public CalculateCheckInViability(String locationName) {
            super();
            this.locationName = locationName;
        }

        @Override
        protected Map<String, String> doInBackground(Void... params) {
            Map<String, String> locationInfo = new HashMap<>();

            return locationInfo;
        }

        @Override
        protected void onPostExecute(Map<String, String> locationInfo) {
            /*
            JSONObject bGeomJsonData = new JSONObject(locationInfo.get("bgeom"));
            JSONArray bGeomCoords = bGeomJsonData.getJSONArray("coordinates").getJSONArray(0);

            PolygonOptions bPolygonOptions = new PolygonOptions();

            for(int i=0; i<bGeomCoords.length(); i++){
                bPolygonOptions.add(new LatLng( bGeomCoords.getJSONArray(i).getDouble(0),
                        bGeomCoords.getJSONArray(i).getDouble(1)));
            }

            LatLng currentUserLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            if(pointInPolygon(currentUserLocation, bPolygonOptions)){
                findViewById(R.id.check_in_button).setVisibility(View.VISIBLE);
            }*/
            findViewById(R.id.check_in_button).setVisibility(View.VISIBLE);
        }
    }
}

