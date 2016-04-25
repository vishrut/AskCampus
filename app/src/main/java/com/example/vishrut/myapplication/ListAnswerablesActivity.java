package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListAnswerablesActivity extends Activity {
    private static final String TAG = "ListAnswerablesActivity";
    private ACUser user;
    private CampusLocationSerializable sCampusLocation;
    private ArrayList<String> questions;
    private HashMap<String, Integer> qids;
    private ArrayAdapter alAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = (ACUser) getIntent().getSerializableExtra(MapsActivity.AC_USER);
        sCampusLocation = (CampusLocationSerializable) getIntent().getSerializableExtra(MapsActivity.CAMPUS_LOCATION);

        qids = new HashMap<String, Integer>();
        setContentView(R.layout.activity_list_answerables);

        questions = new ArrayList<>();
        ListView questionsListView = (ListView) findViewById(R.id.al_list);
        alAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, questions);
        questionsListView.setAdapter(alAdapter);
        questionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String question = (String) parent.getAdapter().getItem(position);
                startListAnswersActivity(qids.get(question), question);
            }
        });

        getActionBar().show();
        getActionBar().setTitle("Select a question to answer");
        new FetchAnswerables().execute();
    }

    private void startListAnswersActivity(Integer qid, String question) {
        Intent intent = new Intent(getApplicationContext(), ListAnswersActivity.class);
        intent.putExtra(MapsActivity.AC_USER, user);
        intent.putExtra(MapsActivity.QID, qid);
        intent.putExtra(MapsActivity.QUE, question);
        Boolean replyEnabled = new Boolean(true);
        intent.putExtra(MapsActivity.REPLY, replyEnabled);
        startActivity(intent);
    }

    private class FetchAnswerables extends AsyncTask<Void, Void, ArrayList<Map<String, String>>> {

        public FetchAnswerables() {
            super();
        }

        @Override
        protected ArrayList<Map<String, String>> doInBackground(Void... params) {
            Log.i(TAG, "Fetching answerables");
            ArrayList<Map<String, String>> questionsList = new ArrayList<>();

            Map<String, String> questionsInfo = new HashMap<>();
            questionsInfo.put("qid", "0");
            questionsInfo.put("userid", "0");
            questionsInfo.put("content", "When does the gym open on weekends?");

            questionsList.add(questionsInfo);

            return questionsList;
        }

        @Override
        protected void onPostExecute(ArrayList<Map<String, String>> questionsList) {
            for (int i = 0; i < questionsList.size(); i++) {
                Log.i(TAG, questionsList.get(i).get("content"));
                questions.add(questionsList.get(i).get("content"));
                qids.put(questions.get(i), Integer.parseInt(questionsList.get(i).get("qid")));
            }
            alAdapter.notifyDataSetChanged();
            Log.i(TAG, "updated adapter");
        }
    }
}

