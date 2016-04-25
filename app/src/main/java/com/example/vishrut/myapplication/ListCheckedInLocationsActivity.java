package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;

public class ListCheckedInLocationsActivity extends Activity {
    private ACUser user;

    private ArrayList<String> locationNames;

    private ListView cilListView;
    private ArrayAdapter locationNamesListAdapter;

    private HashMap<String, CampusLocationSerializable> locationIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = (ACUser) getIntent().getSerializableExtra(MapsActivity.AC_USER);

        setContentView(R.layout.activity_list_checked_in_locations);

        cilListView = (ListView) findViewById(R.id.locations_list);

        locationNames = new ArrayList<>();
        locationIndex = new HashMap<>();

        locationNamesListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, locationNames);
        cilListView.setAdapter(locationNamesListAdapter);
        cilListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String locationName = (String) parent.getAdapter().getItem(position);
                startListAnswerablesActivity(locationIndex.get(locationName));
            }
        });

        getActionBar().show();
        getActionBar().setTitle("Currently Checked In Locations");
        new FetchCheckedInLocations().execute();
    }

    private void startListAnswerablesActivity(CampusLocationSerializable sCampusLocation) {
        Intent intent = new Intent(getApplicationContext(), ListAnswerablesActivity.class);
        intent.putExtra(MapsActivity.AC_USER, user);
        intent.putExtra(MapsActivity.CAMPUS_LOCATION, sCampusLocation);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_location_panel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class FetchCheckedInLocations extends AsyncTask<Void, Void, ArrayList<CampusLocationSerializable>> {

        public FetchCheckedInLocations() {
            super();
        }

        @Override
        protected ArrayList<CampusLocationSerializable> doInBackground(Void... params) {
            ArrayList<CampusLocationSerializable> checkedInLocationsList = new ArrayList<>();
            CampusLocationSerializable checkedInLocation = new CampusLocationSerializable(
                    0, "LRC", "Lyon Resource Center"
            );
            checkedInLocationsList.add(checkedInLocation);
            return checkedInLocationsList;
        }

        @Override
        protected void onPostExecute(ArrayList<CampusLocationSerializable> checkedInLocationsList) {
            for (int i = 0; i < checkedInLocationsList.size(); i++) {
                locationNames.add(checkedInLocationsList.get(i).name);
                locationIndex.put(checkedInLocationsList.get(i).name, checkedInLocationsList.get(i));
            }

            locationNamesListAdapter.notifyDataSetChanged();
        }
    }
}

