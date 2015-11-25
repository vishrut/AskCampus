package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class LocationPanelActivity extends Activity {
    private ACUser user;
    private CampusLocationSerializable sCampusLocation;
    private static final String TAG = "LocationPanelActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = (ACUser)getIntent().getSerializableExtra(MapsActivity.AC_USER);
        sCampusLocation = (CampusLocationSerializable)getIntent().getSerializableExtra(MapsActivity.CAMPUS_LOCATION);

        setContentView(R.layout.activity_location_panel);
        getActionBar().hide();
        findViewById(R.id.ask_question_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListQuestionsActivity();
            }
        });
    }

    private void startListQuestionsActivity(){
        Intent intent = new Intent(getApplicationContext(), ListQuestionsActivity.class);
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
}
