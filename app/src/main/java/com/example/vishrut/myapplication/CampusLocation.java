package com.example.vishrut.myapplication;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.io.Serializable;

public class CampusLocation implements Serializable {
    protected int id;
    protected String name;
    protected String description;
    protected LatLng position;
    protected Polygon bPolygon;

    public CampusLocation(int id, String name, String description, LatLng position, Polygon bPolygon){
        this.id = id;
        this.name = name;
        this.description = description;
        this.position = position;
        this.bPolygon = bPolygon;
    }

    public CampusLocationSerializable serialized(){
        return new CampusLocationSerializable(this);
    }
}
