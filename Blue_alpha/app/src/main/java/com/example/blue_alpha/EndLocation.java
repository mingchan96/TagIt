package com.example.blue_alpha;

public class EndLocation {
    double Lat;
    double Long;

    public EndLocation(double Lat, double Long){
        this.Lat = Lat;
        this.Long = Long;
    }

    public double getLat(){
        return Lat;
    }
    public double getLong(){
        return Long;
    }
}
