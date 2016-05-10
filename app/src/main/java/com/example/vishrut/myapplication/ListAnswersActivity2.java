package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListAnswersActivity2 extends Activity {
    private static final String TAG = "ListAnswersActivity";
    private ACUser user;
    private Integer qid;
    private String question;
    private ArrayList<String> answers;
    private Connection conn;
    private ArrayAdapter answerListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = (ACUser) getIntent().getSerializableExtra(MapsActivity.AC_USER);
        qid = (Integer) getIntent().getSerializableExtra(MapsActivity.QID);
        question = (String) getIntent().getSerializableExtra(MapsActivity.QUE);

        setContentView(R.layout.activity_list_answers);

        Boolean replyEnabled = (Boolean) getIntent().getSerializableExtra(MapsActivity.REPLY);
        if (!replyEnabled.booleanValue()) {
            findViewById(R.id.answer_button).setVisibility(View.GONE);
        }


        answers = new ArrayList<>();
        ListView answersListView = (ListView) findViewById(R.id.answer_list);
        answerListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, answers);
        answersListView.setAdapter(answerListAdapter);

        TextView qText = (TextView) findViewById(R.id.question_text);
        qText.setText(question);
        getActionBar().hide();
        new FetchAnswers().execute();

        findViewById(R.id.answer_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startReplyWithAnswerActivity(qid);
            }
        });
    }

    private void startReplyWithAnswerActivity(Integer qid) {
        Intent intent = new Intent(getApplicationContext(), NewAnswerActivity.class);
        intent.putExtra(MapsActivity.AC_USER, user);
        intent.putExtra(MapsActivity.QID, qid);
        intent.putExtra(MapsActivity.QUE, question);
        startActivity(intent);
    }

    private class FetchAnswers extends AsyncTask<Void, Void, ArrayList<Map<String, String>>> {

        public FetchAnswers() {
            super();
        }

        @Override
        protected ArrayList<Map<String, String>> doInBackground(Void... params) {
            ArrayList<Map<String, String>> answersList = new ArrayList<>();

            Map<String, String> answerInfo = new HashMap<>();
            answerInfo.put("answerid", "0");
            answerInfo.put("userid", "0");
            answerInfo.put("content", "I think the gym opens at 9AM on weekends.");

            Map<String, String> answerInfo2 = new HashMap<>();
            answerInfo2.put("answerid", "0");
            answerInfo2.put("userid", "0");
            answerInfo2.put("content", "I think the gym opens at 9AM on weekends 2.");

            answersList.add(answerInfo);
            answersList.add(answerInfo2);

            return answersList;
        }

        @Override
        protected void onPostExecute(ArrayList<Map<String, String>> answersList) {
            for (int i = 0; i < answersList.size(); i++) {
                answers.add(answersList.get(i).get("content"));
            }
            answerListAdapter.notifyDataSetChanged();
        }
    }
}

