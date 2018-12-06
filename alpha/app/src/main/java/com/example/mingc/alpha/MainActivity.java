package com.example.mingc.alpha;

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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private TextView mTextTitle;
    private TextView mTextMessage;
    private Button btn;

    //Get firebase database reference
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    //data to be filled in
    private Data data = new Data();

    //Geolocate
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double[] coordinates = new double[2];

    //Accelerometer
    private SensorManager SM;
    private Sensor mySensor;

    //Compass
    private float[] mGravity= new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth=0f;
    private float currectAzimuth = 0f;
    private SensorManager mSensorManager;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    reset_state();
                    mTextTitle.setText(R.string.title_home);
                    return true;
                case R.id.geolocate_home:
                    reset_state();
                    mTextTitle.setText("Display Latitude and Longitude");
                    geolocation_home();
                    return true;
                case R.id.accelerometer_title:
                    reset_state();
                    mTextTitle.setText("Display Accelerometer");
                    accelerometer_home();
                    return true;
                case R.id.compass_home:
                    reset_state();
                    mTextTitle.setText("Display Compass");
                    compass_home();
                    return true;
                case R.id.post_home:
                    reset_state();
                    mTextTitle.setText("Post to Firebase");
                    post_home();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextTitle = (TextView) findViewById(R.id.title);
        mTextMessage = (TextView) findViewById(R.id.message);
        btn = (Button) findViewById(R.id.button);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
    }

    //handles displaying geolocation information
    public void geolocate_init() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                data.setLocation(location.getLatitude(),location.getLongitude());
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

    public void geolocation_home(){
        //configure the location listener and start listening
        geolocate_init();
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                mTextMessage.setText("Latitude: " + data.getLatitude() + "\nLongitude: " + data.getLongitude());
                Toast.makeText(view.getContext(),"Geolocation Fetched!",Toast.LENGTH_LONG).show();
            }
        });
    }
    public void accelerometer_init() {
        // Create Sensor Manager
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Accelerometer Sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Register sensor Listener
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);


    }
    public void accelerometer_home(){
        //configure the location listener and start listening
        accelerometer_init();
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                mTextMessage.setText("X: " + data.Accel_x() + "\nY: " + data.Accel_y()
                        +"\nZ: "+data.Accel_z());
                Toast.makeText(view.getContext(),"Accelerometer Info Fetched!",Toast.LENGTH_LONG).show();
            }
        });
    }
    public void compass_home(){
        //configure the location listener and start listening
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                mTextMessage.setText("Heading: " + data.getAzimuth() + " degrees");
                Toast.makeText(view.getContext(),"Accelerometer Info Fetched!",Toast.LENGTH_LONG).show();
            }
        });
    }

    public void post_home(){
        mTextMessage.setText(mDatabase.getKey());

        //initiate geolocate
        geolocate_init();

        //initiate accelerometer
        accelerometer_init();

        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                //get the most recent entry key
                Query query = mDatabase.child("data").orderByKey().limitToLast(1);
                //query.addListenerForSingleValueEvent(new ValueEventListener() {
                mDatabase.child("data").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        /*DataSnapshot latestEntry = dataSnapshot.getChildren().iterator().next();
                        configureData(latestEntry.getKey());*/

                        List<Integer> dataNums = new ArrayList<>();
                        Iterable<DataSnapshot> data_list = dataSnapshot.getChildren();
                        for(DataSnapshot data: data_list)
                        {
                            String entryId = data.getKey();
                            //System.out.println("*****Key: " + entryId);
                            dataNums.add(Integer.parseInt(entryId.split("-")[1]) + 1);
                        }
                        configureData(Collections.max(dataNums));
                        Map<String,Object> posted_data = new HashMap<String,Object>();
                        posted_data.put(data.getId(),data);

                        mDatabase.child("data").updateChildren(posted_data);
                        Toast.makeText(getApplicationContext(),"Posting to Firebase!",Toast.LENGTH_LONG).show();


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // ...
                        Toast.makeText(getApplicationContext(),"Error in posting to Firebase",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    public void reset_state(){
        mTextMessage.setText("");
        btn.setOnClickListener(null);
        //stop location listening
        if(locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

    }

    //called to make Data object to post to Firebase
    public Data configureData(int id){
        data.setId(id);
        data.setDatetime();
        return data;
    }


    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
                data.setCoordinates(sensorEvent.values[0], sensorEvent.values[1],
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
        data.setAzimuth(azimuth);
    }

    public void onAccuracyChanged(Sensor sensor, int i) {

    }
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

}

