package com.example.blue_alpha;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
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
    private double bearing = 0;

    //closest Blue Light Phone
    private BlueLight closestBlueLight = null;

    //ARCore
    private ArFragment arFragment;

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
        System.out.println("****onCreate called****");
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
        arCore_init();
        appState = "HOME";


    }

    private void arCore_init(){
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
            if(appState.compareTo("SEARCH") == 0) {
                Anchor anchor = hitResult.createAnchor();

                ModelRenderable.builder()
                        .setSource(this, Uri.parse("arrow.sfb"))
                        .build()
                        .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable))
                        .exceptionally(throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(throwable.getMessage())
                                    .show();
                            return null;
                        });
            }
        }));

    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        //rotate the arrow about the z axis
        //transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 0, 1f), 90f));
        //rotate the arrow about the x axis
        transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 0, 1f), 90f));
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
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

        //get the angle between the closest blue light and current position
        //bearing = angleFromCoordinate(currentLat,currentLong,closestBlueLight.getLat(),closestBlueLight.getLong());

        showCloestBlueLight();
    }

    private void showCloestBlueLight(){
        String message;
        if(closestBlueLight != null) {
            message = "Name: " + closestBlueLight.getName() + "\nLat: " + closestBlueLight.getLat() +
                    "\nLong: " + closestBlueLight.getLong() + "\nDistance: " + closestBlueLight.getDistance() +
                    "\nBearing: " + bearing;
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
        //brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        System.out.println("***bearing: " + brng);
        return brng;
    }

    private void showMessage(String state, String message){
        if(state.compareTo(appState) == 0) {
            mTextMessage.setText(message);
            System.out.println(message);
        }
    }
}
