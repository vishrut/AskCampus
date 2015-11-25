package com.example.vishrut.myapplication;

import java.io.Serializable;

public class ACUser implements Serializable {
    protected int id;
    protected String email;

    public ACUser(int id, String email){
        this.id = id;
        this.email = email;
    }
}
