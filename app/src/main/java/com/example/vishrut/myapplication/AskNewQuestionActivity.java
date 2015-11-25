package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AskNewQuestionActivity extends Activity {
    private ACUser user;
    private CampusLocationSerializable sCampusLocation;
    private static final String TAG = "AskNewQuestionActivity";
    private Connection conn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = (ACUser)getIntent().getSerializableExtra(MapsActivity.AC_USER);
        sCampusLocation = (CampusLocationSerializable)getIntent().getSerializableExtra(MapsActivity.CAMPUS_LOCATION);

        setContentView(R.layout.activity_ask_new_question);

        getActionBar().show();
        getActionBar().setTitle("Ask a new question");

        findViewById(R.id.submit_question_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText qEditText = (EditText)findViewById(R.id.question_field);
                String question = qEditText.getText().toString();
                if(question.length()>0)
                    new SubmitQuestion(question).execute();
            }
        });

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
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

    private class SubmitQuestion extends AsyncTask<Void, Void, Integer> {
        String question;

        public SubmitQuestion(String question){
            super();
            this.question = question;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            ArrayList<Map<String, String>> questionsList = new ArrayList<>();
            try {
                conn = DbHelper.getDatabaseConnection();
                String insertSql = "INSERT INTO askcampus.question(userid, locationid, content) VALUES("+user.id+","+sCampusLocation.id+",'"+question+"');";
                PreparedStatement insertPreparedSt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                insertPreparedSt.executeUpdate();
                conn.close();
            } catch (SQLException e){
                Log.e(TAG, "Exception while submitting question: " + e);
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer status){
            startListQuestionsActivity();
        }
    }
}


