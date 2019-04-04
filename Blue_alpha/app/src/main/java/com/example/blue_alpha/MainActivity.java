package com.example.blue_alpha;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //state of app
    private String appState;
    //used for Firebase
    private DatabaseReference mDatabase;
    //arraylist of blue light phones
    private ArrayList<BlueLight> blueLights = new ArrayList <BlueLight>();
    //Geolocate
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double currentLat = -1;
    private double currentLong = -1;

    //closest Blue Light Phone
    private BlueLight closestBlueLight = null;

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    appState = "HOME";
                    showLocation();
                    return true;
                case R.id.navigation_list:
                    mTextMessage.setText(R.string.title_list);
                    appState = "LIST";
                    listBlueLights();
                    return true;
                case R.id.navigation_search:
                    appState = "SEARCH";
                    mTextMessage.setText(R.string.title_search);
                    showCloestBlueLight();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        FirebaseApp.initializeApp(this);
        //Get firebase database reference
        mDatabase = FirebaseDatabase.getInstance().getReference("blue light phones");
        firebaseData_init();
        geolocate_init();
        appState = "HOME";
    }

    public void firebaseData_init(){
        ValueEventListener dataListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot blueLightData: dataSnapshot.getChildren()) {
                    blueLights.add(blueLightData.getValue(BlueLight.class));
                    //BlueLight blueLight = blueLightData.getValue(BlueLight.class);
                    //System.out.println(blueLight.getName() + "\n");
                    //System.out.println(blueLight.getLat() + "\n");
                    //System.out.println(blueLight.getLong() + "\n");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mDatabase.addListenerForSingleValueEvent(dataListener);
    }

    public void listBlueLights(){
        //if the blueLights array is not empty then list the blue lights
        String message = "";
        if(!blueLights.isEmpty()){
            for (BlueLight bluelight: blueLights) {
                message += bluelight.getName() + "\n" + bluelight.getLat() + "\n" + bluelight.getLong() + "\n";
            }
        }

        mTextMessage.setText(message);
    }

    //handles initializing geolocation information and acts as the app's ticker for apps
    public void geolocate_init() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                currentLat = location.getLatitude();
                currentLong = location.getLongitude();
                String message = "currentLat: " + currentLat + "\ncurrentLong: " + currentLong;
                showMessage("HOME",message);
                checkClosestBlueLight();

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET
            }, 10);
            return;
        }
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    private void showLocation(){
        String message = "currentLat: " + currentLat + "\ncurrentLong: " + currentLong;
        showMessage("HOME",message);
    }

    public void checkClosestBlueLight(){
        BlueLight tempClosestBlueLight = closestBlueLight;
        float tempClosestDistance = -1;

        for (BlueLight blueLight: blueLights) {
            float[] distance = new float[1];
            Location.distanceBetween(
                    //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
                    currentLat,currentLong,blueLight.getLat(),blueLight.getLong(),distance);
            blueLight.setDistance(distance[0]);
            if(closestBlueLight == null){
                tempClosestBlueLight = blueLight;
                tempClosestDistance = distance[0];
            }
            //if closest distance is greater than or equal to the next distance then replace
            else if(tempClosestDistance >= distance[0]){
                tempClosestBlueLight = blueLight;
                tempClosestDistance = distance[0];
            }
        }

        closestBlueLight = tempClosestBlueLight;
        showCloestBlueLight();
    }

    private void showCloestBlueLight(){
        String message;
        if(closestBlueLight != null) {
            message = "Name: " + closestBlueLight.getName() + "\nLat: " + closestBlueLight.getLat() +
                    "\nLong: " + closestBlueLight.getLong() + "\nDistance: " + closestBlueLight.getDistance();
        }
        else{
            message = "no info available yet";
        }
        showMessage("SEARCH", message);
        //mTextMessage.setText(message);

    }

    //gotten code from stack overflow to calculate the angle between two latitude and longitude points
    private double angleFromCoordinate(double lat1, double long1, double lat2, double long2) {

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }

    private void showMessage(String state, String message){
        if(state.compareTo(appState) == 0) {
            mTextMessage.setText(message);
        }
    }
}
