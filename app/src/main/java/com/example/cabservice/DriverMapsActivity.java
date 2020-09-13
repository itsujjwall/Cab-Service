package com.example.cabservice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private Button LogoutBtn;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    final int LocationRequestCode = 1;

    private Boolean isloggingout=false;
    private String CustomerId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LocationRequestCode);
        } else {
            mapFragment.getMapAsync(this);
        }

        LogoutBtn = findViewById(R.id.driver_btn_logout);

        LogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isloggingout=true;

                disconnecteddriver();

                FirebaseAuth.getInstance().signOut();
              startActivity(new Intent(DriverMapsActivity.this, MainActivity.class));
                finish();
            }
        });

        getAssignedCustomer();
    }

    private void getAssignedCustomer() {
        String DriverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(DriverId).child("CustomerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                        CustomerId = Objects.requireNonNull(snapshot.getValue()).toString();

                        //getting customer pickup location
                        getAssignedCustomerPickupLocation();
                }
                else {
                    CustomerId="";
                    if (pickupMarker!=null){
                        pickupMarker.remove();
                    }
                    if (assignedCustomerPickupLocationListner!=null) {
                        assignedCustomerPickupLocation.removeEventListener(assignedCustomerPickupLocationListner);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMapsActivity.this, "Error Occur: " + error.getMessage() + "\n Try Again...", Toast.LENGTH_SHORT).show();
            }
        });

    }








    Marker pickupMarker;
    private DatabaseReference assignedCustomerPickupLocation;
    private ValueEventListener assignedCustomerPickupLocationListner;



    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocation=FirebaseDatabase.getInstance().getReference().child("CustomerRequest").child(CustomerId).child("l");
        assignedCustomerPickupLocationListner= assignedCustomerPickupLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() &&  !CustomerId.equals("")) {
                    List<Object> map=(List<Object>) snapshot.getValue();
                    double LocationLat=0;
                    double LocationLon=0;


                    //for not getting location error
                    if (map.get(0)!=null){
                        LocationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1)!=null){
                        LocationLon=Double.parseDouble(map.get(1).toString());
                    }

                    //marking driver location on the customer map
                    LatLng DriverLatLong=new LatLng(LocationLat,LocationLon);

                    pickupMarker=mMap.addMarker(new MarkerOptions().position(DriverLatLong).title("Customer Pickup Location"));
//                    pickupMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_customer));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMapsActivity.this, "Error Occur: " + error.getMessage() + "\n Try Again...", Toast.LENGTH_SHORT).show();
            }
        });
    }








    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LocationRequestCode);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    private synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this).
                addConnectionCallbacks(this).addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext()!=null) {
            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

            //error check krna haa app logout krne pe crash ho raha haa
            
            String User_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference referenceAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
            DatabaseReference referenceworking = FirebaseDatabase.getInstance().getReference("DriversWorking");
            GeoFire geoFireAvailable = new GeoFire(referenceAvailable);
            GeoFire geoFireWorking = new GeoFire(referenceworking);

            switch (CustomerId) {
                case "":
                    geoFireWorking.removeLocation(User_id);
                    geoFireAvailable.setLocation(User_id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailable.removeLocation(User_id);
                    geoFireWorking.setLocation(User_id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }


        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LocationRequestCode);
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
        switch (requestCode) {
            case LocationRequestCode: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MapFragment mapFragment = null;
                    mapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(this, "Please Provide the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void disconnecteddriver(){
        String User_id = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("DriversAvailable");

        GeoFire geoFire = new GeoFire(reference);
        geoFire.removeLocation(User_id);
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (!isloggingout){
            disconnecteddriver();
        }

    }
}