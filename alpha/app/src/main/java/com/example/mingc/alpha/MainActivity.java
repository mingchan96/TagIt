package com.example.mingc.alpha;

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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private TextView mTextTitle;
    private TextView mTextMessage;
    private Button btn;

    //Get firebase database reference
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();


    //Geolocate
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double[] coordinates = new double[2];

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
                    return true;
                case R.id.compass_home:
                    reset_state();
                    mTextTitle.setText("Display Compass");
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

    }

    //handles displaying geolocation information
    public void geolocate_init() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                coordinates[0] = location.getLatitude();
                coordinates[1] = location.getLongitude();
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    public void geolocation_home(){
        //configure the location listener and start listening
        geolocate_init();
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                mTextMessage.setText("Latitude: " + coordinates[0] + "\nLongitude: " + coordinates[1]);
                Toast.makeText(view.getContext(),"Geolocation Fetched!",Toast.LENGTH_LONG).show();
            }
        });
    }

    public void post_home(){
        mTextMessage.setText(mDatabase.getKey());
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                //mDatabase.child("people").setValue(new Data());
                mDatabase.child("data").setValue("Message:Finally posting to firebase");
                Toast.makeText(view.getContext(),"Posting to Firebase!",Toast.LENGTH_LONG).show();
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
}
