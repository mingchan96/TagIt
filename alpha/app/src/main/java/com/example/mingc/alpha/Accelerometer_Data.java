package com.example.mingc.alpha;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Accelerometer_Data {
    public String id;
    public double Xcoordinate;
    public double Ycoordinate;
    public double Zcoordinate;
    public String datetime;

    public Accelerometer_Data(){

    }

    public void setCoordinates(double Xcoordinate, double Ycoordinate, double Zcoordinate){
        this.Xcoordinate = Xcoordinate;
        this.Ycoordinate = Ycoordinate;
        this.Zcoordinate = Zcoordinate;

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

    public double getXcoordinate(){
        return this.Xcoordinate;
    }

    public double getYcoordinate(){
        return this.Ycoordinate;
    }

    public double getZcoordinate(){
        return this.Zcoordinate;
    }

}
