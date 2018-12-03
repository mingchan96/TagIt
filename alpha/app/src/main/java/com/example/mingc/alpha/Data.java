package com.example.mingc.alpha;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Data {
    public String id;
    public double latitude;
    public double longitude;
    public String datetime;

    public Data(){

    }

    public void setLocation(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setIdFromPreviousId(String entryId){
        String[] prevId = entryId.split("-");
        int num = Integer.parseInt(prevId[1]) + 1;
        this.id = "entry-"+num;
    }

    public void setId(int id){
        this.id = "entry-" + id;
    }

    public void setDatetime(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        datetime = dateFormat.format(new Date());
    }

    public String getId(){
        return this.id;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude(){
        return this.longitude;
    }
}
