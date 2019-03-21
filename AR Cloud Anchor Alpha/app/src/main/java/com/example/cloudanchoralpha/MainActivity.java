package com.example.cloudanchoralpha;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //used to format the number displayed
    NumberFormat numberFormatter = NumberFormat.getInstance();
    //used to format the date
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private Button btn;
    private Button resolveBtn;

    //object holding the phone orientation data
    private Data phoneData;
    //object holding the closest anchor's geolocation and orientation
    private Data anchorPhysicalData = null;
    //closests distance to anchor in meters
    float closestDistance = -1;

    //which state the app is in
    private String appState;

    //Geolocate
    private LocationManager locationManager;
    private LocationListener locationListener;

    //Compass
    private float[] mGravity= new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth=0f;
    private float currectAzimuth = 0f;
    private SensorManager mSensorManager;

    //Accelerometer
    private SensorManager SM;
    private Sensor mySensor;

    //used for Firebase
    private DatabaseReference mDatabase;
    //contains Firebase's data
    Iterable<DataSnapshot> firebaseData;
    //Iterable<Data> firebaseData;


    //ARCore stuff
    private CustomArFragment arFragment;

    //check for ARCore id if it is being hosting
    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED
    }
    private Anchor anchor;
    private AppAnchorState appAnchorState = AppAnchorState.NONE;
    private boolean isPlaced = false;

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    appState = "HOME";
                    restUI();
                    mTextMessage.setText(R.string.title_home);
                    removeOutdatedData();
                    return true;
                case R.id.navigation_place:
                    appState = "PLACE";
                    restUI();
                    mTextMessage.setText(R.string.title_place);
                    return true;
                case R.id.navigation_check:
                    appState = "CHECK";
                    restUI();
                    btn.setOnClickListener(new View.OnClickListener(){
                        public void onClick(View view) {
                            //getClosestAnchor();
                        }
                    });
                    checkLocations();
                    mTextMessage.setText(R.string.title_check);
                    return true;
                case R.id.navigation_resolve:
                    appState = "RESOLVE";
                    //checkLocations();
                    restUI();
                    mTextMessage.setText(R.string.title_resolve);
                    btn.setOnClickListener(new View.OnClickListener(){
                        public void onClick(View view) {
                            //checkLocations();
                            locationOrientationFeedback();
                            //showMessage("RESOLVE", anchorPhysicalData.getAnchorId());
                            //System.out.println("x-axis: " + phoneData.getAccel_x());
                            //System.out.println("y-axis: " + phoneData.getAccel_y());
                            //System.out.println("z-axis: " + phoneData.getAccel_z());
                            //System.out.println("azimuth: " + azimuth);
                        }
                    });
                    resolveBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //checkLocations();
                            resolveCloudAnchor();
                            //showToast("Hello");
                        }
                    });
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        //Get firebase database reference
        mDatabase = FirebaseDatabase.getInstance().getReference("cloud_anchor");
        phoneData = new Data(mDatabase);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        btn = (Button) findViewById(R.id.button);
        resolveBtn = findViewById(R.id.resolveButton);

        //acquire a reference to system's sensor
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        //firebaseData_init();
        geolocate_init();
        accelerometer_init();
        ARFragment_init();
        appState = "HOME";
        showMessage("HOME","Home");

        //remove any outdated data
        //removeOutdatedData();
    }

    /*public void placeArObject(){
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                phoneData.post();
                showToast("Button Clicked - data posted");
            }
        });
    }*/

    //handles initializing geolocation information and acts as the app's ticker for apps
    public void geolocate_init() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                phoneData.setLocation(location.getLatitude(),location.getLongitude());
                String message = "Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude();
                String datetimeNow = "Updated on: " + dateFormat.format(new Date()) + "\n";

                showMessage("PLACE", datetimeNow + message);

                //constantly update the distances to anchors and the closest anchor's physical data
                //checkLocations();
                //getClosestAnchor();

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
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, locationListener);
    }

    //Everytime data change in Firebase, this will fetch it in a different thread
    public void firebaseData_init(){
        ValueEventListener dataListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                firebaseData = dataSnapshot.getChildren();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mDatabase.addValueEventListener(dataListener);
    }

    public void getClosestAnchor(){
        System.out.println("##########################\ngetClosestAnchor call\n#######################");
        if(firebaseData == null){
            showToast("There is no firebase data at this time");
            System.out.println("There is no firebase data at this time");
        }

        String message = "";
        Data closestAnchorData = new Data();
        for(DataSnapshot postedDataSnapshot: firebaseData){
            if(postedDataSnapshot.getKey().compareTo("testHash") != 0){
                message += postedDataSnapshot.getKey() + "\n";
                Data data = postedDataSnapshot.getValue(Data.class);

                System.out.println(postedDataSnapshot.getKey() + "\n");

                //use built in Location.distanceBetween to calculate distance in meters
                float[] distance = new float[1];
                Location.distanceBetween(
                        //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
                        phoneData.getLatitude(),phoneData.getLongitude(),data.getLatitude(),data.getLongitude(),distance);
                //System.out.println(distance[0]);
                message += "\tDistance: " + numberFormatter.format(distance[0]) + " m\n";

                //check if this iteration is the closest distance
                if(closestDistance == -1)
                {
                    closestDistance = distance[0];
                    closestAnchorData = data;
                }
                else if(distance[0] < closestDistance){
                    closestDistance = distance[0];
                    closestAnchorData = data;
                }
            }
        }
        System.out.println("line 276 - Exit for loop Firebase data processing");
        //list the distance from each anchor during CHECK state
        showMessage("CHECK", message);

        //if there is not data assigned to closestAnchorData then don't overwrite the anchorPhysicalData
        if( (closestAnchorData.getAnchorId().compareTo("null")) == 0) {
            //anchorPhysicalData = closestAnchorData;
        }
        else{
            showToast("No anchorPhysicalData is assigned");
        }
        try {
            System.out.println("Anchor id: " + anchorPhysicalData.getAnchorId());
        }catch(Exception e){
            System.out.println("Error in getClosesAnchor");
            System.out.println(e.getMessage());
        }
        System.out.println("##########################\nfinish getClosestAnchor\n#######################");
    }

    //check the closest anchor's location and gets the closest anchor's physical data
    public void checkLocations(){
        System.out.println("##########################\ncheckLocations call\n#######################");
        ValueEventListener dataListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                System.out.println("line 237 - Enter processing Firebase Data");
                String message = "";

                Data closestAnchorData = new Data();
                //System.out.println("Phone Lat: " + phoneData.getLatitude());
                //System.out.println("Phone Long: " + phoneData.getLongitude());
                // Get Post object and use the values to update the UI
                for (DataSnapshot postedDataSnapshot: dataSnapshot.getChildren()) {
                    //System.out.println(postedDataSnapshot.getKey());
                    message += postedDataSnapshot.getKey() + "\n";
                    if(postedDataSnapshot.getKey().compareTo("testHash") != 0){
                        Data data = postedDataSnapshot.getValue(Data.class);
                        //System.out.println("Firebase Lat: " + data.getLatitude());
                        //System.out.println("Firebase Long: " + data.getLongitude());
                        //apply the traditional distance formula
                        //double latitudeDifference = phoneData.getLatitude() - data.getLatitude();
                        //double longitudeDifference = phoneData.getLongitude() - data.getLongitude();
                        //double distance = Math.sqrt(Math.pow(latitudeDifference,2) + Math.pow(longitudeDifference,2));

                        //use built in Location.distanceBetween to calculate distance in meters
                        float[] distance = new float[1];
                        Location.distanceBetween(
                                //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
                                phoneData.getLatitude(),phoneData.getLongitude(),data.getLatitude(),data.getLongitude(),distance);
                        //System.out.println(distance[0]);
                        message += "\tDistance: " + numberFormatter.format(distance[0]) + " m\n";

                        //check if this iteration is the cloest distance
                        if(closestDistance == -1)
                        {
                            closestDistance = distance[0];
                            //closestAnchorData = data;
                            anchorPhysicalData = data;
                        }
                        else if(distance[0] < closestDistance){
                            closestDistance = distance[0];
                            //closestAnchorData = data;
                            anchorPhysicalData = data;
                        }
                    }
                }
                System.out.println("line 276 - Exit for loop Firebase data processing");
                showMessage("CHECK", message);

                //if there is not data assigned to closestAnchorData then don't overwrite the anchorPhysicalData
                if( (anchorPhysicalData.getAnchorId().compareTo("null")) == 0) {
                    //anchorPhysicalData = closestAnchorData;
                    showToast("No anchorPhysicalData is assigned");
                }
                else{
                    //showToast("No anchorPhysicalData is assigned");
                }

                System.out.println("line 239 - Assigned anchorPysicalData");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showToast("Fail at fetching data from firebase");
            }
        };

        //show the list when button is clicked
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                mDatabase.addListenerForSingleValueEvent(dataListener);
                showToast("Button Clicked - checking Firebase");
            }
        });

        System.out.println("line 296 - Before calling mDatabase.addListenerForSingle....");
        mDatabase.addListenerForSingleValueEvent(dataListener);

        System.out.println("line 298 - After calling mDatabase.addListenerForSingle....");
        try {
            //TimeUnit.SECONDS.sleep(1);
            System.out.println(anchorPhysicalData.getAnchorId());
        }catch(Exception e){
            System.out.println("anchorPhysicalData cannot be found");
            System.out.println(e.getMessage());
        }

    }

    public void locationOrientationFeedback(){
        //getClosestAnchor();

        if(anchorPhysicalData == null){
            showToast("No anchor data is available");
            return;
        }

        showMessage("RESOLVE", anchorPhysicalData.getAnchorId());
        //System.out.println("Closes Distance: " + closestDistance);
        if(closestDistance == -1){
            showToast("No data to be found");
        }
        else if(closestDistance > 1){
            showToast(numberFormatter.format(closestDistance) + " m away from anchor");
        }
        //if the phone is not facing within 10 degrees of anchor's direction then advise user
        //(anchorPhysicalData.getAzimuth() - 5)%360 : in case minus 5 results in a negative number
        //(azimuth < (anchorPhysicalData.getAzimuth() + 5 )%360) : in case + 5 results in a number greater than 360
        else if( !(( (anchorPhysicalData.getAzimuth() - 5)%360 < azimuth) && (azimuth < (anchorPhysicalData.getAzimuth() + 5 )%360) ) ){
            //the anchor's degree is greater than phone's azimuth then turn right
            if(azimuth < anchorPhysicalData.getAzimuth()){
                showToast("Turn to the right");
            }
            //the anchor's degree is less than phone's azimuth then turn right
            if(azimuth > anchorPhysicalData.getAzimuth()) {
                showToast("Turn to the left");
            }
        }
        else{
            showToast("You should be at the right spot and orientation");
        }
    }

    public void accelerometer_init() {
        // Create Sensor Manager
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Accelerometer Sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Register sensor Listener
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);


    }

    /*Following code is used to obtained info from Magnetometer and Accelerometer*/
    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);
    }


    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    //the class implements SensorEventListener
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
                phoneData.setAccelerometer(sensorEvent.values[0], sensorEvent.values[1],
                        sensorEvent.values[2]);
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
            }
        }
        phoneData.setAzimuth(azimuth);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void ARFragment_init(){
        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {

            //if the model is placed then don't place model again/user's tap
            if(!isPlaced && appState.compareTo("PLACE") == 0) {
                anchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());
                appAnchorState = AppAnchorState.HOSTING;

                showToast("Hosting....");

                createModel(anchor);
                isPlaced = true;
            }
        }));

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if(appAnchorState != AppAnchorState.HOSTING)
                return;

            Anchor.CloudAnchorState cloudAnchorState = anchor.getCloudAnchorState();

            //check if there is an error in hosting the cloud Anchor
            if(cloudAnchorState.isError()){
                showToast(cloudAnchorState.toString());
            }
            else if(cloudAnchorState == Anchor.CloudAnchorState.SUCCESS){
                appAnchorState = AppAnchorState.HOSTED;
                //get the id associated with the anchor
                //the id is a long string so use a shorter key
                String anchorId = anchor.getCloudAnchorId();
                showToast("Anchor hosted successfully. Anchor id: " + anchorId);
                //post the phone's orientation to Firebase and use anchorId as the key
                phoneData.post(anchorId);
                showToast("Data posted to Firebase");
            }
        });
    }

    public void resolveCloudAnchor(){
        showToast("resolveCloudAnchor");
        //String anchorId = "ua-5e221e9d288596b56144f7eb97164443";

        //if the anchorPhysicalData is null or the AnchorId is blank
        if(anchorPhysicalData == null){
            showToast("No anchor data is available");
            return;
        }

        try {
            String anchorId = anchorPhysicalData.getAnchorId();
            showToast("Got the AnchorID");
            //showToast(anchorId.substring(0,6));

            if (anchorId.equals("null") || anchorId.equals("") || anchorId == null) {
                Toast.makeText(this, "No anchorId found", Toast.LENGTH_LONG).show();
                return;
            }
            showToast(anchorId);
            Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(anchorId);
            createModel(resolvedAnchor);

        }catch(Exception e) {
            System.out.println(e.getMessage());
            showMessage("RESOLVE",e.getMessage());
        }
    }

    //ArcticFox_Posed.sfb
    //Giraffe_01(1).sfb
    private void createModel(Anchor anchor){
        ModelRenderable
                    .builder()
                    .setSource(this, Uri.parse("ArcticFox_Posed.sfb"))
                    .build()
                    .thenAccept(modelRenderable -> placeModel(anchor, modelRenderable));
    }

    private void placeModel(Anchor anchor, ModelRenderable modelRenderable){
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
    }

    public void restUI(){
        btn.setOnClickListener(null);
        resolveBtn.setOnClickListener(null);
    }

    //remove any Firebase data that is 23 hours over
    /*private void removeOutdatedData(){
        System.out.println("removeOutdatedData");
        ValueEventListener dataListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.systemDefault());
                String message = "";
                for (DataSnapshot postedDataSnapshot: dataSnapshot.getChildren()) {
                    if(postedDataSnapshot.getKey().compareTo("testHash") != 0) {
                        String dataKey = postedDataSnapshot.getKey();
                        Data data = postedDataSnapshot.getValue(Data.class);

                        //check if Firebase's timestamp is 23 hours before now then remove the data
                        Instant firebaseTimestamp = Instant.from(formatter.parse(data.getDatetime()));
                        Instant now = Instant.now();
                        Instant twentyThreeHoursEarlier = now.minus( 23 , ChronoUnit.HOURS );

                        if( (firebaseTimestamp.isBefore(twentyThreeHoursEarlier)) &&  firebaseTimestamp.isBefore(now) ){
                            //removeData(dataKey);
                            message += "Key: " + dataKey + " will be removed\n";
                            //System.out.println("Key: " + dataKey + " has been removed");
                        }
                    }
                }
                showMessage("HOME",message);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showToast("Fail at fetching data from firebase");
            }
        };

        mDatabase.addListenerForSingleValueEvent(dataListener);
    }*/

    private void removeOutdatedData(){
        //used to parse the datetime in the yyyy/MM/dd HH:mm:ss format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        ValueEventListener dataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //23 hours in milliseconds
                long twentyThreeHours_MILLIS = 23 * 60 * 60 * 1000L;
                String message = "";
                for (DataSnapshot postedDataSnapshot: dataSnapshot.getChildren()) {
                    if(postedDataSnapshot.getKey().compareTo("testHash") != 0) {
                        String dataKey = postedDataSnapshot.getKey();
                        Data data = postedDataSnapshot.getValue(Data.class);

                        try {
                            //create the date objects
                            Date dataDatetime = sdf.parse(data.getDatetime());
                            Date phoneDatetime = new Date();
                            //if more than 23 hours then delete the record
                            if( Math.abs(phoneDatetime.getTime() - dataDatetime.getTime()) > twentyThreeHours_MILLIS){
                                //removeData(dataKey);
                                message += "Key: " + dataKey + " will be removed\n";
                                //System.out.println("Key: " + dataKey + " has been removed");
                            }
                        }catch (Exception e){
                                message += "Error in removedOutdatedData: " + dataKey + "\n";
                                System.out.println(e.getMessage());

                        }


                    }
                }
                showMessage("HOME",message);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showToast("Fail at fetching data from firebase");
            }
        };

        mDatabase.addListenerForSingleValueEvent(dataListener);
    }

    //delete data in Firebase by giving the key value/path value
    private void removeData(String k){
        String path = "cloud_anchor/" + k;
        FirebaseDatabase.getInstance().getReference(path).removeValue();
    }

    private void showToast(String s) {
        Toast.makeText(this,s, Toast.LENGTH_LONG).show();
    }

    private void showMessage(String state, String message){
        if(state.compareTo(appState) == 0) {
            mTextMessage.setText(message);
        }
    }

}
