package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class NewAnswerActivity extends Activity {
    private ACUser user;
    private Integer qid;
    private String question;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = (ACUser) getIntent().getSerializableExtra(MapsActivity.AC_USER);
        qid = (Integer) getIntent().getSerializableExtra(MapsActivity.QID);
        question = (String) getIntent().getSerializableExtra(MapsActivity.QUE);

        setContentView(R.layout.activity_new_answer);

        getActionBar().show();
        getActionBar().setTitle("Reply");

        findViewById(R.id.submit_answer_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText answerEditText = (EditText) findViewById(R.id.answer_field);
                String answer = answerEditText.getText().toString();
                new SubmitAnswer(answer).execute();
            }
        });

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListAnswersActivity(qid, question);
            }
        });
    }

    private void startListAnswersActivity(Integer qid, String question) {
        Intent intent = new Intent(getApplicationContext(), ListAnswersActivity2.class);
        intent.putExtra(MapsActivity.AC_USER, user);
        intent.putExtra(MapsActivity.QID, qid);
        intent.putExtra(MapsActivity.QUE, question);
        Boolean replyEnabled = true;
        intent.putExtra(MapsActivity.REPLY, replyEnabled);
        startActivity(intent);
    }

    private class SubmitAnswer extends AsyncTask<Void, Void, Integer> {
        String answer;

        public SubmitAnswer(String answer) {
            super();
            this.answer = answer;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return 1;
        }

        @Override
        protected void onPostExecute(Integer status) {
            startListAnswersActivity(qid, question);
        }
    }
}


