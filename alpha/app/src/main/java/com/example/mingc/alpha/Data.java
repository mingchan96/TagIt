package com.example.mingc.alpha;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Data {
    public String id;
    public double latitude;
    public double longitude;
<<<<<<< HEAD
    public float z_orient;
    public float x_orient;
    public float y_orient;
=======
    public double Accel_x;
    public double Accel_y;
    public double Accel_z;
    public float azimuth;
>>>>>>> 089066f8e6b90ae60de16d9cc27dd645c5dc0ab7
    public String datetime;



    public Data(){

    }

    public void setLocation(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public void setCoordinates(double Accel_x, double Accel_y, double Accel_z){
        this.Accel_x = Accel_x;
        this.Accel_y = Accel_y;
        this.Accel_z = Accel_z;

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

    public double Accel_x(){
        return this.Accel_x;
    }

    public double Accel_y(){
        return this.Accel_y;
    }

    public double Accel_z(){
        return this.Accel_z;
    }


    public void setAzimuth(float azimuth){
        this.azimuth = azimuth;
    }

    public float getAzimuth() {
        return this.azimuth;
    }
}
