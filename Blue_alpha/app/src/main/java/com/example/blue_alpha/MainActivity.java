package com.example.blue_alpha;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
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
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.core.Camera;
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
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    public static final String LAT = "com.example.blue_alpha";
    public static final String LNG = "com.example.blue_alpha";
    //state of app
    private String appState;
    //used to format the date
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    //used for Firebase
    private DatabaseReference mDatabase;
    //arraylist of blue light phones
    private LinkedList<BlueLight> blueLights = new LinkedList<BlueLight>();
    //Geolocate
    private LocationManager locationManager;
    private LocationListener locationListener;
    //private double currentLat = -1;
    //private double currentLong = -1;
    private Location currentLocation = null;

    //use the google services(FusedLocationProviderClient) to get user's location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private double bearing = 0;
    //variable contains the angle in which the image needs to rotate
    private float turningAngle = 0;

    //closest Blue Light Phone
    private BlueLight closestBlueLight = null;
    //flag used to limit the call to checking closest blue light
    private boolean checkedClosestBlueLight = false;

    //API Call to Google Maps AIP to get turn by turn directions
    private GetDirectionData getDirectionData = new GetDirectionData(this);
    //contains the coordinates of end locations from Google Maps Directions API
    public LinkedList<Location> checkpoints = new LinkedList<Location>();

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
    private float initialAzimuth = 400;
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
                    showCloestBlueLight();
                    //printDirectionsData();
                    return true;

                case R.id.navigation_map:
                    appState = "MAP";
                    mTextMessage.setText("Map");
                    openNavi();
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

        //geolocate_init();
        //check and request getting user's location and requests location updates
        callLocationPermissions();

        FirebaseApp.initializeApp(this);
        //Get firebase database reference
        mDatabase = FirebaseDatabase.getInstance().getReference("blue light phones");
        firebaseData_init();
        //set up AR environment
        arCore_init();
        //initialized for the magnetometer sensor
        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);

        //initialize the state to home
        appState = "HOME";
    }

    private void arCore_init(){
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        //create the arrow renderable
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

        //allow users to render an arrow if user tap on screen
        /*arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
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
        }));*/

    }

    /*private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
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
    }*/


    //called to render an arrow in session
    private void addAR(ModelRenderable renderable, float angleToTurn){
        //only render the arrow if the mode is in SEARCH
        if (appState.compareTo("SEARCH") == 0) {
            // Find a position half a meter in front of the user.
            Vector3 cameraPos = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            Vector3 cameraForward = arFragment.getArSceneView().getScene().getCamera().getForward();
            Vector3 position = Vector3.add(cameraPos, cameraForward.scaled(0.9f));

            // Create an ARCore Anchor at the position.
            Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
            Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);

            // Create the Sceneform AnchorNode
            AnchorNode anchorNode = new AnchorNode(anchor);

            //create a transformable node to rotate the object
            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
            //new Vector3(axis), degree to turn)
            transformableNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), angleToTurn));
            transformableNode.setParent(anchorNode);
            transformableNode.setRenderable(renderable);

            //anchorNode.setRenderable(renderable);
            arFragment.getArSceneView().getScene().addChild(anchorNode);
            transformableNode.select();
            //add the node to the linked list
            anchorNodeLinkedList.add(anchorNode);

            //in case to prevent a large number of anchors
            if (anchorNodeLinkedList.size() > 5) {
                removeAnchorNode();
            }

            System.out.println("\n###addAR finish run###\n");
        }
    }

    private void removeAnchorNode(){
        AnchorNode anchorNode = anchorNodeLinkedList.removeFirst();
        anchorNode.getAnchor().detach();
        arFragment.getArSceneView().getScene().onRemoveChild(anchorNode);
    }

    //fetch the data from firebase
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

    //list all the available blue lights
    public void listBlueLights(){
        //if the blueLights array is not empty then list the blue lights
        String message = "";
        if(!blueLights.isEmpty()){
            for (BlueLight blueLight: blueLights) {
                message += blueLight.getName() + "\n" + blueLight.getLat() + "\n" + blueLight.getLong() + "\n";
                float[] distance = new float[1];
                //Location.distanceBetween(
                        //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
                //        currentLat,currentLong,blueLight.getLat(),blueLight.getLong(),distance);
                Location.distanceBetween(
                                currentLocation.getLatitude(),currentLocation.getLongitude(),blueLight.getLat(),blueLight.getLong(),distance);
                message += "Distance: " + distance[0] + "\n";
            }
        }

        mTextMessage.setText(message);
    }

    //Old way of obtaining the coordinates
    //handles initializing geolocation information and acts as the app's ticker for apps
    /*public void geolocate_init() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                currentLat = location.getLatitude();
                currentLong = location.getLongitude();
                currentLocation = location;
                //String message = "currentLat: " + currentLat + "\ncurrentLong: " + currentLong;
                //checkClosestBlueLight();
                calculateBearingToCheckpoints();
                //addAR();
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
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, locationListener);
    }*/

    //new methods used to obtain the user's location
    private void location_init(){
        if(fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    //obtain the user's permission to get their location
    private void callLocationPermissions(){

        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        Permissions.check(this/*context*/, permissions, "BluePath: Permission is required", null/*options*/, new PermissionHandler() {
            @Override
            public void onGranted() {
                //get the location if permission was granted
                requestLocationUpdates();
            }
            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions){
                super.onDenied(context, deniedPermissions);
            }
        });
    }

    //updates the current location coordinates
    private void requestLocationUpdates(){
        //if user grants the permissions then proceed with location
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED) {

            fusedLocationProviderClient = new FusedLocationProviderClient(this);

            //configuring the location update
            locationRequest = new LocationRequest();
            //set the accuracy to high
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            //set the interval to 5 seconds
            locationRequest.setInterval(5000);
            //set the intervals of getting location when app is in the background to 2 seconds
            locationRequest.setFastestInterval(2000);

            //intialize what needs to be done once location is obtained
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    currentLocation = locationResult.getLastLocation();
                    //currentLat = currentLocation.getLatitude();
                    //currentLong = currentLocation.getLongitude();

                    //once the asynchronous task is finished then check for the closest blue light
                    //should only need to be called once since the blue lights are stationary
                    //if the blueLights data exist and have not checked closest blue light then execute
                    if(!blueLights.isEmpty() && !checkedClosestBlueLight) {
                        checkClosestBlueLight();
                    }

                    //check the status the next checkpoint
                    calculateBearingToCheckpoints();
                    showLocation();
                    showCloestBlueLight();

                }
            };

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
        else{
            callLocationPermissions();
        }

    }

    private void showLocation(){
        String message = "Updated on: " + dateFormat.format(new Date()) +
                "\ncurrentLat: " + currentLocation.getLatitude() +
                "\ncurrentLong: " + currentLocation.getLongitude() +
                "\ncurrentAzimuth: " + azimuth;
        showMessage("HOME",message);
    }

    public void checkClosestBlueLight(){
        //if the list of blueLights is not empty then do the following operations
        if(!blueLights.isEmpty()) {
            //BlueLight tempClosestBlueLight = null;
            //float tempClosestDistance = -1;

            //for every blue light calculate the distance to them
            for (BlueLight blueLight : blueLights) {
                float[] distance = new float[1];
                /*Location.distanceBetween(
                        //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
                        currentLat, currentLong, blueLight.getLat(), blueLight.getLong(), distance);*/
                Location.distanceBetween(
                        //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
                        currentLocation.getLatitude(), currentLocation.getLongitude(), blueLight.getLat(), blueLight.getLong(), distance);
                blueLight.setDistance(distance[0]);
                System.out.println("Name: " + blueLight.getName() + "\tDistance: " + distance[0] + "\n");
            }

            //sort the blueLights by the distance, in ascending order
            Collections.sort(blueLights);

            //### implemented for checkClosestBlueLight to be called once
            //get the first element in sorted list
            closestBlueLight = blueLights.get(0);
            //call the Google Maps Directions API to get the checkpoints
            callMapAPI();
            //###


            /*//get the first element in sorted list
            tempClosestBlueLight = blueLights.get(0);


            //should only reassign if it is a new closest blue light phone
            //if there is no initial closest blue light then assign it and call map api to get directions
            if(closestBlueLight == null) {
                closestBlueLight = tempClosestBlueLight;
                callMapAPI();
            }
            //if the closest blue light is not the old closest blue light then reassign and call map api
            else if( (closestBlueLight.getName()).compareTo(tempClosestBlueLight.getName()) != 0 ){
                closestBlueLight = tempClosestBlueLight;
                callMapAPI();
            }

            //get the angle between the closest blue light and current position
            //bearing = bearingBetweenCoordinates(currentLat, currentLong, closestBlueLight.getLat(), closestBlueLight.getLong());

            //calculate the angle to rotate the image
            //updateTurningAngle();*/

            //set the chcekedClosestBlueLight to true
            checkedClosestBlueLight = true;
        }
        else{
            Toast.makeText(this,"BlueLights are empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTurningAngle(){//calculate the angle to rotate the image
        //turningAngle = (float) -((bearing - (azimuth + 235)%360 )%360);
        float offset = -125;
        //turningAngle = (offset + (float)(bearing - azimuth))%360;
        //make the arrow point in the straight direction
        //turningAngle = (offset - (float)(azimuth - initialAzimuth))%360;
        turningAngle = (offset - (float)(bearing - initialAzimuth))%360;
        //turningAngle = 0;

    }

    private void showCloestBlueLight(){
        String message;

        if(closestBlueLight != null && appState.compareTo("SEARCH") == 0) {
            message = "Name: " + closestBlueLight.getName() +
                    "\nLat: " + closestBlueLight.getLat() +
                    "\nLong: " + closestBlueLight.getLong() +
                    "\nDistance: " + closestBlueLight.getDistance() +
                    "\nBearing to Checkpoint: " + bearing +
                    "\nHeading: " + azimuth +
                    "\nTurning Degrees: " + turningAngle;
            //If there are still checkpoints left then display the coordinates.
            if(!checkpoints.isEmpty()){
                message += "\nCheckpoint" +
                        "\nLat: " + checkpoints.getFirst().getLatitude() +
                        "\nLong: " + checkpoints.getFirst().getLongitude();
            }
            else{
                message += "\nDestination Reached";
            }
        }
        else{
            message = "no info available yet";
        }
        showMessage("SEARCH", message);
        //mTextMessage.setText(message);

    }

    //gotten code from stack overflow to calculate the angle between two latitude and longitude points
    private double bearingBetweenCoordinates(double lat1, double long1, double lat2, double long2) {

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
        //remove the update when the state is paused
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStart(){
        super.onStart();
        //reinstate the update
        callLocationPermissions();
    }

    @Override
    protected void onStop(){
        //remove the update when the state is in stop
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        //remove the update when app is going to be destroyed
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
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
            //showCloestBlueLight();
        }
        //the initial orientation of the phone
        if(appState.compareTo("HOME") == 0){
            initialAzimuth = azimuth;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void callMapAPI(){
        //create an array of Doubles as inputs
        Double[] coordinates = {currentLocation.getLatitude(),currentLocation.getLongitude(),closestBlueLight.getLat(),closestBlueLight.getLong()};
        getDirectionData.execute(coordinates);
    }

    //used only to test if direction data exists in checkpoints
    public void printDirectionsData(){
        for(Location checkpoint: checkpoints){
            System.out.println("\nMainActivity printDirectionsData: endLat: " + checkpoint.getLatitude() +
                    " endLng: " + checkpoint.getLongitude());
        }
    }

    public void calculateBearingToCheckpoints(){
        //proceed with operations if checkpoints is not empty
        if(!checkpoints.isEmpty()){

            Location checkpoint = checkpoints.getFirst();
            //tell how far from checkpoint
            //Location.distanceBetween(currentLocation.getLatitude(),currentLocation.getLongitude(),checkpoint.getLatitude(),checkpoint.getLongitude(),distance);
            float distance = currentLocation.distanceTo(checkpoint);
            Toast.makeText(this,distance + "m from checkpoint", Toast.LENGTH_SHORT).show();
            //if user is 15m near the checkpoint then remove first checkpoint
            if(distance <= 7){
                checkpoints.removeFirst();
                Toast.makeText(this,"Checkpoint Reached", Toast.LENGTH_SHORT).show();
            }
            //if the checkpoints is still not empty then get first new checkpoint and set the bearings
            if(!checkpoints.isEmpty()){
                checkpoint = checkpoints.getFirst();
                bearing = currentLocation.bearingTo(checkpoint);
                //bearing = bearingBetweenCoordinates(currentLat,currentLong,checkpoint.getLat(),checkpoint.getLong());
                //bearing = bearingBetweenCoordinates(checkpoint.getLat(),checkpoint.getLong(),currentLat,currentLong);
                updateTurningAngle();
                addAR(arrowRenderable,turningAngle);
            }
            else{
                Toast.makeText(this,"No more checkpoints", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void openNavi(){
        Intent intent = new Intent(this, Navi.class);
        intent.putExtra(LAT, 10);
        intent.putExtra(LNG, 10);

        startActivity(intent);
    }

}
