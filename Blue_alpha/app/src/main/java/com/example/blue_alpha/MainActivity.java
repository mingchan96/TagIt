package com.example.blue_alpha;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //state of app
    private String appState;
    //used to format the date
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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
    //variable contains the angle in which the image needs to rotate
    private float turningAngle = 0;

    //closest Blue Light Phone
    private BlueLight closestBlueLight = null;

    //ARCore
    private ArFragment arFragment;
    private ModelRenderable arrowRenderable;
    //linked list of anchors to control the number of anchors
    private LinkedList<AnchorNode> anchorNodeLinkedList = new LinkedList<AnchorNode>();

    //Compass
    private float[] mGravity= new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth=0f;
    private float currentAzimuth = 0f;
    private SensorManager mSensorManager;

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
                    //checkClosestBlueLight();
                    showCloestBlueLight();
                    addAR();
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

        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);

        firebaseData_init();
        geolocate_init();
        arCore_init();
        appState = "HOME";


    }

    private void arCore_init(){
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        ModelRenderable.builder()
                // To load as an asset from the 'assets' folder ('src/main/assets/andy.sfb'):
                .setSource(this, Uri.parse("model.sfb"))
                .build()
                .thenAccept(renderable -> arrowRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(throwable.getMessage())
                                    .show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
            if(appState.compareTo("SEARCH") == 0) {
                Anchor anchor0 = hitResult.createAnchor();

                ModelRenderable.builder()
                        .setSource(this, Uri.parse("model.sfb"))
                        .build()
                        .thenAccept(modelRenderable -> addModelToScene(anchor0, modelRenderable))
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
        //transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 235f));
        //transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), turningAngle));
        //rotate the arrow about the x axis
        //transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 1f, 0), 90f));
        transformableNode.setParent(anchorNode);
        //transformableNode.setRenderable(modelRenderable);
        transformableNode.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
    }

    private void addAR(){
        /*//Add an Anchor and a renderable in front of the camera
        Session session = arFragment.getArSceneView().getSession();
        float[] pos = { 0,0,-1 };
        float[] rotation = {0,0,0,1};
        Anchor anchor =  session.createAnchor(new Pose(pos, rotation));
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(arrowRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        ///anchorNode.setParent(arFragment.getArSceneView().getScene());*/

        // Find a position half a meter in front of the user.
        Vector3 cameraPos = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
        Vector3 cameraForward = arFragment.getArSceneView().getScene().getCamera().getForward();
        Vector3 position = Vector3.add(cameraPos, cameraForward.scaled(0.6f));

        // Create an ARCore Anchor at the position.
        Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);

        // Create the Sceneform AnchorNode
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(arrowRenderable);
        anchorNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), turningAngle));
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        anchorNodeLinkedList.add(anchorNode);

        //in case to prevent a large number of anchors
        if(anchorNodeLinkedList.size() > 5)
        {
            removeAnchorNode();
        }

        System.out.println("\n###addAR finish run###\n");
    }

    private void removeAnchorNode(){
        AnchorNode anchorNode = anchorNodeLinkedList.removeFirst();
        anchorNode.getAnchor().detach();
        arFragment.getArSceneView().getScene().onRemoveChild(anchorNode);
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
            for (BlueLight blueLight: blueLights) {
                message += blueLight.getName() + "\n" + blueLight.getLat() + "\n" + blueLight.getLong() + "\n";
                float[] distance = new float[1];
                Location.distanceBetween(
                        //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
                        currentLat,currentLong,blueLight.getLat(),blueLight.getLong(),distance);
                message += "Distance: " + distance[0] + "\n";
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
                //showMessage("HOME",message);
                checkClosestBlueLight();
                showLocation();

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
        String message = "Updated on: " + dateFormat.format(new Date()) +
                "\ncurrentLat: " + currentLat +
                "\ncurrentLong: " + currentLong +
                "\ncurrentAzimuth: " + azimuth;
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
            System.out.println("Name: " + blueLight.getName() + "\tDistance: " + distance[0] + "\n");
        }

        //sort the blueLights by the distance, in ascending order
        Collections.sort(blueLights);
        //get the first element in sorted list
        closestBlueLight = blueLights.get(0);

        //get the angle between the closest blue light and current position
        bearing = angleFromCoordinate(currentLat,currentLong,closestBlueLight.getLat(),closestBlueLight.getLong());
        //calculate the angle to rotate the image
        updateTurningAngle();
        showCloestBlueLight();

        //add new anchor to the scene
        //addAR();
    }

    private void updateTurningAngle(){//calculate the angle to rotate the image
        turningAngle = (float) -((bearing - (azimuth + 235)%360 )%360);
    }

    private void showCloestBlueLight(){
        String message;
        if(closestBlueLight != null) {
            message = "Name: " + closestBlueLight.getName() +
                    "\nLat: " + closestBlueLight.getLat() +
                    "\nLong: " + closestBlueLight.getLong() +
                    "\nDistance: " + closestBlueLight.getDistance() +
                    "\nBearing: " + bearing +
                    "\nHeading: " + azimuth +
                    "\nTurning Degrees: " + turningAngle;
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

        //System.out.println("***bearing: " + brng);
        return brng;
    }

    private void showMessage(String state, String message){
        if(state.compareTo(appState) == 0) {
            mTextMessage.setText(message);
            System.out.println(message);
        }
    }

    //stuff for the compass/azimuth
    @Override
    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];

            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * sensorEvent.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * sensorEvent.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * sensorEvent.values[2];
            }
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;
                //azimuth = azimuth % 360;
            }
        }
        if(Math.abs(currentAzimuth - azimuth) > 2)
        {
            currentAzimuth = azimuth;
            updateTurningAngle();
            showCloestBlueLight();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
