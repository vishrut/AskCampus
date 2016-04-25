package com.example.vishrut.myapplication;

import java.io.Serializable;

// Similar to the CampusLocation class but no non-serializable members like position and building polygon
public class CampusLocationSerializable implements Serializable{
    protected int id;
    protected String name;
    protected String description;

    public CampusLocationSerializable() {
    }

    public CampusLocationSerializable(int id, String name, String description){
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public CampusLocationSerializable(CampusLocation campusLocation){
        this.id = campusLocation.id;
        this.name = campusLocation.name;
        this.description = campusLocation.description;
    }
}
