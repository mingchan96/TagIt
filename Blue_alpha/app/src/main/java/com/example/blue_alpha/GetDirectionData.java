package com.example.blue_alpha;

import android.location.Location;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.location.LocationManager.NETWORK_PROVIDER;

public class GetDirectionData extends AsyncTask <Double, String, String> {

    //urlString for the REST request
    String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=#ORIGINLAT#,#ORIGINLONG#&destination=#DESTLAT#,#DESTLONG#&mode=walking&key=#KEY#";
    String key = "AIzaSyCBJ37KNTVC9FI2kb4Q1iy2JGMRo9vtT-c";

    HttpURLConnection httpURLConnection = null;
    String data = "";
    InputStream inputStream = null;

    MainActivity mainActivity;

    public GetDirectionData(MainActivity mainActivity){this.mainActivity = mainActivity;}

    @Override
    protected String doInBackground(Double... coordinates) {
        //replace the coordinates in the urlString with the origin coordinates and destination coordinates
        urlString = urlString.replace("#ORIGINLAT#",coordinates[0].toString());
        urlString = urlString.replace("#ORIGINLONG#",coordinates[1].toString());
        urlString = urlString.replace("#DESTLAT#",coordinates[2].toString());
        urlString = urlString.replace("#DESTLONG#",coordinates[3].toString());
        //insert in the api key
        urlString = urlString.replace("#KEY#",key);

        try{
            URL url = new URL(urlString);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb =  new StringBuffer();
            String line = "";
            while((line=bufferedReader.readLine()) != null){
                sb.append(line);
            }
            data = sb.toString();
            bufferedReader.close();

        }catch(MalformedURLException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

        return data;
    }

    @Override
    protected void onPostExecute(String s){
        try{
            JSONObject jsonObject = new JSONObject(s);
            JSONArray jsonArray = jsonObject.getJSONArray("routes").getJSONObject(0)
                                    .getJSONArray("legs").getJSONObject(0).getJSONArray("steps");

            JSONObject tempJSONObject;
            int count = jsonArray.length();

            mainActivity.checkpoints.clear();

            for(int i = 0; i < count; i++){
                tempJSONObject = jsonArray.getJSONObject(i);
                double endLat = tempJSONObject.getJSONObject("end_location").getDouble("lat");
                double endLong = tempJSONObject.getJSONObject("end_location").getDouble("lng");
                //EndLocation endLocation = new EndLocation(endLat,endLong);

                //store the coordinates as Location objects
                Location endLocation = new Location(NETWORK_PROVIDER);
                endLocation.setLatitude(endLat);
                endLocation.setLongitude(endLong);

                mainActivity.checkpoints.add(endLocation);
                System.out.println("\nGetDirectionData onPostExecute: endLat: " + endLat + " endLng: " + endLong);
            }
            Toast.makeText(mainActivity,"Directions API Response Processed", Toast.LENGTH_SHORT).show();
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
}
