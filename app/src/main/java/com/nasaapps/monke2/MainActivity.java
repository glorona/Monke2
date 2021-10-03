package com.nasaapps.monke2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.SearchView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.nasaapps.monke2.modelo.*;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    GoogleMap map;
    SupportMapFragment mapFragment;
    SearchView searchView;
    FusedLocationProviderClient client;
    private static ArrayList<Locacion> locSelectBoxData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchView = findViewById(R.id.sv_location);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        client = LocationServices.getFusedLocationProviderClient(this);

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }
        else{
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},44);

        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                String location = searchView.getQuery().toString();
                map.clear();

                locSelectBoxData = null;
                System.out.println(location);

                if(location != null || location.equals("")){
                    String col = consumoWebCol(location);
                    consumoWebVen(col);

                    for (Locacion loc:locSelectBoxData){
                        LatLng latLng = new LatLng(Double.parseDouble(loc.getLon()),Double.parseDouble(loc.getLat()));
                        map.addMarker(new MarkerOptions().position(latLng).title(location));
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,5));
                    }


                }


                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        mapFragment.getMapAsync(this);
    }

    private void getCurrentLocation() {
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){
                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                            MarkerOptions options = new MarkerOptions().position(latLng).title("Tu");


                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));
                            googleMap.addMarker(options);


                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 44){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation();
            }

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;


    }

    private String consumoWebCol(String q){
        String collection="";
        try{
            StrictMode.ThreadPolicy p=new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(p);
            HttpRequest request=new HttpRequest("https://besttime.app/api/v1/venues/search?api_key_private=pri_1d7e7bc2477a476b9eacdbba90bb4a87&q=" + q +
                    "&lat=40.7521&lng=-73.9827&radius=5"
                    ,false,HttpRequest.POST_METHOD);
            HttpResponse response=request.execute();
            int status=response.getStatusCode();
            String body=response.getBody();
            JSONObject json=new JSONObject(body);
            collection=json.getString("collection_id");

        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        return collection;

    }


    private void consumoWebVen(String col){
        ArrayList<String> final_lis=null;
        JSONArray venues =null;
        try{
            StrictMode.ThreadPolicy p=new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(p);
            HttpRequest request=new HttpRequest("https://besttime.app/api/v1/collection/"+ col +
                    "?api_key_private=pri_1d7e7bc2477a476b9eacdbba90bb4a87"
                    ,false,HttpRequest.GET_METHOD);
            HttpResponse response=request.execute();
            locSelectBoxData = new ArrayList<>();
            int status=response.getStatusCode();
            String body=response.getBody();
            JSONObject json=new JSONObject(body);
            venues=json.getJSONArray("venue_ids");
            for(int x=0; x<venues.length();x++){
                locSelectBoxData.add(consumoWebLoc((String) venues.get(x)));
            }


        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }

    }

    private Locacion consumoWebLoc(String ven){
        JSONObject venues =null;
        Locacion ubicacion = null;
        try{
            StrictMode.ThreadPolicy p=new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(p);
            HttpRequest request=new HttpRequest("https://besttime.app/api/v1/venues/"+ ven + "?api_key_public=pub_1dc779251cdf48f39731b0d510cc885e",
                    false,HttpRequest.GET_METHOD);
            HttpResponse response=request.execute();
            int status=response.getStatusCode();
            String body=response.getBody();
            JSONObject json=new JSONObject(body);
            venues=json.getJSONObject("venue_info");

            String name = venues.getString("venue_name");
            String latitud = venues.getString("venue_lng");
            String longitud = venues.getString("venue_lat");
            String id = venues.getString("venue_id");

            ubicacion = new Locacion(id,name,latitud,longitud);

        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        return ubicacion;

    }
}