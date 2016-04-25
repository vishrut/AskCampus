package com.example.vishrut.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.sql.Connection;

public class AskNewQuestionActivity extends Activity {
    private static final String TAG = "AskNewQuestionActivity";
    private ACUser user;
    private CampusLocationSerializable sCampusLocation;
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

    private class SubmitQuestion extends AsyncTask<Void, Void, Integer> {
        String question;

        public SubmitQuestion(String question){
            super();
            this.question = question;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return 1;
        }

        @Override
        protected void onPostExecute(Integer status){
            startListQuestionsActivity();
        }
    }
}


