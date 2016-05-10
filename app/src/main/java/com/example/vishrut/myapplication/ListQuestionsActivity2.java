package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListQuestionsActivity2 extends Activity {
    private ACUser user;
    private CampusLocationSerializable sCampusLocation;

    private ArrayList<String> questions;
    private HashMap<String, Integer> qids;
    private ArrayAdapter qlAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = (ACUser) getIntent().getSerializableExtra(MapsActivity.AC_USER);
        sCampusLocation = (CampusLocationSerializable) getIntent().getSerializableExtra(MapsActivity.CAMPUS_LOCATION);

        qids = new HashMap<String, Integer>();
        setContentView(R.layout.activity_list_questions);

        questions = new ArrayList<>();
        ListView questionsListView = (ListView) findViewById(R.id.ql_list);
        qlAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, questions);
        questionsListView.setAdapter(qlAdapter);
        questionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String question = (String) parent.getAdapter().getItem(position);
                startListAnswersActivity(qids.get(question), question);
            }
        });

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

    private void startListAnswersActivity(Integer qid, String question) {
        Intent intent = new Intent(getApplicationContext(), ListAnswersActivity.class);
        intent.putExtra(MapsActivity.AC_USER, user);
        intent.putExtra(MapsActivity.QID, qid);
        intent.putExtra(MapsActivity.QUE, question);
        Boolean replyEnabled = new Boolean(false);
        intent.putExtra(MapsActivity.REPLY, replyEnabled);
        startActivity(intent);
    }

    private void startAskNewQuestionActivity() {
        Intent intent = new Intent(getApplicationContext(), AskNewQuestionActivity.class);
        intent.putExtra(MapsActivity.AC_USER, user);
        intent.putExtra(MapsActivity.CAMPUS_LOCATION, sCampusLocation);
        startActivity(intent);
    }

    private class FetchQuestions extends AsyncTask<Void, Void, ArrayList<Map<String, String>>> {

        public FetchQuestions() {
            super();
        }

        @Override
        protected ArrayList<Map<String, String>> doInBackground(Void... params) {
            ArrayList<Map<String, String>> questionsList = new ArrayList<>();

            Map<String, String> questionsInfo = new HashMap<>();
            questionsInfo.put("qid", "0");
            questionsInfo.put("userid", "0");
            questionsInfo.put("content", "When does the gym open on weekends?");

            Map<String, String> questionsInfo2 = new HashMap<>();
            questionsInfo2.put("qid", "1");
            questionsInfo2.put("userid", "0");
            questionsInfo2.put("content", "When does the gym open on weekends 2?");

            questionsList.add(questionsInfo);
            questionsList.add(questionsInfo2);
            return questionsList;
        }

        @Override
        protected void onPostExecute(ArrayList<Map<String, String>> questionsList) {
            for (int i = 0; i < questionsList.size(); i++) {
                questions.add(questionsList.get(i).get("content"));
                qids.put(questions.get(i), Integer.parseInt(questionsList.get(i).get("qid")));
            }
            qlAdapter.notifyDataSetChanged();
        }
    }
}

