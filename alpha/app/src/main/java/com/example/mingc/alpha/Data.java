package com.example.mingc.alpha;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Data {
    public String latitude;
    public String longitude;
    public String datetime;

    public Data(){
        this.latitude = "0";
        this.longitude = "0";
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        datetime = dateFormat.format(new Date());
    }

    public Data(double latitude, double longitude){
        this.latitude = Double.toString(latitude);
        this.longitude = Double.toString(longitude);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        datetime = dateFormat.format(new Date());
    }
}
