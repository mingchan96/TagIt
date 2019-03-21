package com.example.cloudanchoralpha;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

public class Data {

    //Get firebase database reference
    private DatabaseReference mDatabase;

    private String databaseRoot = "cloud_anchor";

    public String anchorId = "null";
    public double latitude;
    public double longitude;
    public double accel_x;
    public double accel_y;
    public double accel_z;
    public float azimuth;
    public String datetime;

    public Data(){

    }

    public Data(DatabaseReference df){
        this.mDatabase = df;
    }

    public Data(String id, DatabaseReference df){
        this.anchorId = id;
        this.mDatabase = df;
    }

    //post to Firebase
    public void post(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.datetime = dateFormat.format(new Date());
        String hashKey;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    (this.datetime).getBytes(StandardCharsets.UTF_8));
            hashKey = bytesToHex(encodedhash);
        }catch(java.security.NoSuchAlgorithmException e){
            hashKey = this.datetime;
        }

        HashMap<String, Object> dataToPost = new HashMap<>();
        dataToPost.put(hashKey, this);
        //mDatabase.child(databaseRoot).updateChildren(dataToPost);
        mDatabase.updateChildren(dataToPost);
    }

    //post to Firebase and provide the anchorID as the key
    public void post(String key){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.datetime = dateFormat.format(new Date());
        HashMap<String, Object> dataToPost = new HashMap<>();
        this.anchorId = key;
        dataToPost.put(key, this);
        //mDatabase.child(databaseRoot).updateChildren(dataToPost);
        mDatabase.updateChildren(dataToPost);
    }

    //set the id
    public void setId(String id){
        this.anchorId = id;
    }

    //set the geolocation latitude and longitude data
    public void setLocation(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    //set accelerometer data
    public void setAccelerometer(double Accel_x, double Accel_y, double Accel_z){
        this.accel_x = Accel_x;
        this.accel_y = Accel_y;
        this.accel_z = Accel_z;
    }

    //set the azimuth data
    public void setAzimuth(float azimuth){
        this.azimuth = azimuth;
    }

    //getters
    public String getAnchorId(){
        return this.anchorId;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude(){
        return this.longitude;
    }

    public double getAccel_x(){
        return this.accel_x;
    }

    public double getAccel_y(){
        return this.accel_y;
    }

    public double getAccel_z(){
        return this.accel_z;
    }

    public float getAzimuth() {
        return this.azimuth;
    }

    public String getDatetime(){ return this.datetime;}

    //used in testing phase
    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
