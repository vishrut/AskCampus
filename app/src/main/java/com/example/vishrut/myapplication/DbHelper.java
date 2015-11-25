package com.example.vishrut.myapplication;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbHelper {
    protected static Connection conn;
    private static final String TAG = "DbHelper";

    protected static Connection getDatabaseConnection(){
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Could not find DB driver");
        }

        String endpoint = "spatialinstance.cm2us8yyqh8k.us-west-2.rds.amazonaws.com";
        String databaseName = "spatialdb";
        String url = "jdbc:postgresql://" + endpoint + "/" + databaseName;

        Properties props = new Properties();
        props.setProperty("user", "spatialuser");
        props.setProperty("password", "spatialdatabase");

        try {
            conn = DriverManager.getConnection(url, props);
            Log.i(TAG, "Connected to the database");
        } catch (SQLException e){
            Log.e(TAG, "Exception while connecting to the database" + e);
        }
        return conn;
    }
}
