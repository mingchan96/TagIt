package com.example.blue_alpha;

//class used to represent each blue light phone
public class BlueLight {
    private String name;
    private double Lat;
    private double Long;
    private float distance = -1;
    private double angle;

    public BlueLight(){

    }
    public BlueLight(String name, double Lat, double Long){
        this.name = name;
        this.Lat = Lat;
        this.Long = Long;
    }

    public String getName() {
        return name;
    }
    public double getLat(){
        return Lat;
    }
    public double getLong(){
        return Long;
    }
    public float getDistance(){
        return distance;
    }

    public void setDistance(float distance){
        this.distance = distance;
    }
}
