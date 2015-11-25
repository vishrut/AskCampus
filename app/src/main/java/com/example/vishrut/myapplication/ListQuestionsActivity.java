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
import android.widget.ListView;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListQuestionsActivity extends Activity {
    private ACUser user;
    private CampusLocationSerializable sCampusLocation;
    private static final String TAG = "ListQuestionsActivity";

    private ArrayList<String> questions;
    private Connection conn;
    private ArrayAdapter qlAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = (ACUser)getIntent().getSerializableExtra(MapsActivity.AC_USER);
        sCampusLocation = (CampusLocationSerializable)getIntent().getSerializableExtra(MapsActivity.CAMPUS_LOCATION);

        setContentView(R.layout.activity_list_questions);

        questions = new ArrayList<>();
        ListView questionsListView = (ListView) findViewById(R.id.ql_list);
        qlAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, questions);
        questionsListView.setAdapter(qlAdapter);

        getActionBar().show();
        getActionBar().setTitle("Previously Asked Questions");
        new FetchQuestions().execute();

        findViewById(R.id.ask_new_question_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAskNewQuestionActivity();
            }
        });
    }

    private void startAskNewQuestionActivity(){
        Intent intent = new Intent(getApplicationContext(), AskNewQuestionActivity.class);
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

    private class FetchQuestions extends AsyncTask<Void, Void, ArrayList<Map<String, String>>> {

        public FetchQuestions(){
            super();
        }

        @Override
        protected ArrayList<Map<String, String>> doInBackground(Void... params) {
            ArrayList<Map<String, String>> questionsList = new ArrayList<>();
            try {
                conn = DbHelper.getDatabaseConnection();
                Statement st = conn.createStatement();
                String fetchSql = "SELECT id, userid, content from askcampus.question where locationid="+sCampusLocation.id+";";
                ResultSet rs = st.executeQuery(fetchSql);
                while(rs.next()) {
                    Map<String, String> questionsInfo = new HashMap<>();
                    questionsInfo.put("qid", rs.getString(1));
                    questionsInfo.put("userid", rs.getString(2));
                    questionsInfo.put("content", rs.getString(3));

                    questionsList.add(questionsInfo);
                }
                rs.close();
                st.close();
                conn.close();
            } catch (SQLException e){
                Log.e(TAG, "Exception while retrieving questions: " + e);
            }
            return questionsList;
        }

        @Override
        protected void onPostExecute(ArrayList<Map<String, String>> questionsList){
            for(int i=0; i<questionsList.size(); i++){
                questions.add(questionsList.get(i).get("content"));
            }
            qlAdapter.notifyDataSetChanged();
        }
    }
}

