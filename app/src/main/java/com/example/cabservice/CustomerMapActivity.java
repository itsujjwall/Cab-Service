package com.example.cabservice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private Button LogoutBtn,FindCabBtn;
    private Boolean requestBol=false;

    private LatLng PickupLocation;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    final int LocationRequestCode=1;

    private Marker pickupMarker;

    GeoQuery geoQuery;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LocationRequestCode);
        }
        else {
            mapFragment.getMapAsync(this);
        }

        LogoutBtn=findViewById(R.id.driver_btn_logout);
        FindCabBtn=findViewById(R.id.find_Cab_btn);


        LogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMapActivity.this,MainActivity.class));
                finish();
            }
        });

        FindCabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestBol){
                    requestBol=false;
                    geoQuery.removeAllListeners();
                    driverlocationRef.removeEventListener(driverlocationRefListner);

                    if (DriverFoundId !=null){
                        DatabaseReference driverRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(DriverFoundId);
                        driverRef.setValue(true);
                        DriverFoundId=null;
                    }
                    DriverFound=false;
                    radius=1;
                    String User_id=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference reference=FirebaseDatabase.getInstance().getReference("CustomerRequest");
                    GeoFire geoFire=new GeoFire(reference);
                    geoFire.removeLocation(User_id);

                    if (pickupMarker!=null){
                        pickupMarker.remove();
                    }
                    FindCabBtn.setText("Find Cab ");

                }
                else {
                    requestBol=true;
                    String User_id=FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference reference=FirebaseDatabase.getInstance().getReference("CustomerRequest");
                    GeoFire geoFire=new GeoFire(reference);
                    geoFire.setLocation(User_id,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                    PickupLocation= new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                    pickupMarker= mMap.addMarker(new MarkerOptions().position(PickupLocation).title("I'm  here"));
                    FindCabBtn.setText("Getting Your Driver...");

                    GetClosestDriver();
                }

            }
        });
    }






    private int radius=1;
    private Boolean DriverFound=false;
    private String DriverFoundId;


    private void GetClosestDriver() {
        DatabaseReference driverLocation=FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        GeoFire geoFire=new GeoFire(driverLocation);

        geoQuery=geoFire.queryAtLocation(new GeoLocation(PickupLocation.latitude,PickupLocation.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!DriverFound && requestBol) {
                    DriverFound = true;
                    DriverFoundId=key;

                    DatabaseReference driverRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(DriverFoundId);
                    String CustomerId=FirebaseAuth.getInstance().getCurrentUser().getUid();

                    HashMap map=new HashMap();
                    map.put("CustomerRideId",CustomerId);
                    driverRef.updateChildren(map);

                    //for getting driver location on the customer map
                    getDriverLocation();
                    FindCabBtn.setText("Looking for the Driver Location");

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            //***
            @Override
            public void onGeoQueryReady() {
                if (!DriverFound && radius<=10){
                    radius++;
                    GetClosestDriver();

                }
                if (radius>10){
                    FindCabBtn.setText("No Drivers found at the moment, Find Again...");
                    FindCabBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            radius=1;
                            FindCabBtn.setText("Getting Your Driver...");
                            GetClosestDriver();

                        }
                    });
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }






    private Marker mDriverLocationMarker;
    private DatabaseReference driverlocationRef;
    private ValueEventListener driverlocationRefListner;



    private void getDriverLocation() {
        driverlocationRef=FirebaseDatabase.getInstance().getReference().child("DriversWorking").child(DriverFoundId).child("l");
        driverlocationRefListner=driverlocationRef.addValueEventListener(new ValueEventListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists() && requestBol){
                    List<Object> map=(List<Object>) snapshot.getValue();
                    double LocationLat=0;
                    double LocationLon=0;
                    FindCabBtn.setText("Driver Location Found");

                    //for not getting location error
                    if (map.get(0)!=null){
                        LocationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1)!=null){
                        LocationLon=Double.parseDouble(map.get(1).toString());
                    }

                    //marking driver location on the customer map
                    LatLng DriverLatLong=new LatLng(LocationLat,LocationLon);
                    if (mDriverLocationMarker!=null){
                        mDriverLocationMarker.remove();
                    }
                    Location loc1=new Location("");
                    loc1.setLatitude(PickupLocation.latitude);
                    loc1.setLongitude(PickupLocation.longitude);

                    Location loc2=new Location("");
                    loc2.setLatitude(DriverLatLong.latitude);
                    loc2.setLongitude(DriverLatLong.longitude);

                    int distance= (int) loc1.distanceTo(loc2);

                    if (distance<80){
                        FindCabBtn.setText("Driver Arrived ");
                        Toast.makeText(CustomerMapActivity.this, "Driver Arrived at the Location", Toast.LENGTH_SHORT).show();
                    }
                    else
                    FindCabBtn.setText("Driver Location Found "+String.valueOf(distance)+" m");
                    if (mDriverLocationMarker==null) {
                        mDriverLocationMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLong).title("Your Driver Location")
                        );
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }






    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LocationRequestCode);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    private synchronized void buildGoogleApiClient() {

        mGoogleApiClient=new GoogleApiClient.Builder(this).
                addConnectionCallbacks(this).addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation=location;

        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LocationRequestCode);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LocationRequestCode:{
                if (grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                    MapFragment mapFragment = null;
                    mapFragment.getMapAsync(this);
                }
                else {
                    Toast.makeText(this, "Please Provide the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}