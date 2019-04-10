package com.example.blue_alpha;

import static java.lang.Float.compare;

//class used to represent each blue light phone
public class BlueLight implements Comparable <BlueLight>{
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

    //used in the Collections.sort() to sort by the distance
    @Override
    public int compareTo(BlueLight blueLight){
        return compare(this.getDistance(), blueLight.getDistance());
    }
}
