package com.mahmoud.bashir.taxia.Maps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.JetPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mahmoud.bashir.taxia.R;
import com.mahmoud.bashir.taxia.Settings_Activity;
import com.mahmoud.bashir.taxia.Storage.SharedPrefranceManager;
import com.mahmoud.bashir.taxia.Welcome_Activity;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class Customer_MapsActivity extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    @BindView(R.id.customer_logout)Button customer_logout;
    @BindView(R.id.customer_sett)Button customer_sett;
    @BindView(R.id.call_captain)Button call_captain;
    @BindView(R.id.rel1)RelativeLayout rel1;
    @BindView(R.id.name_driver) TextView name_driver;
    @BindView(R.id.phone_driver) TextView phone_driver;
    @BindView(R.id.car_name_driver) TextView car_name_driver;
    @BindView(R.id.Ring_to_captain) ImageView Ring_to_captain;
    @BindView(R.id.profile_image_driver) CircleImageView profile_image_driver;




    FirebaseAuth auth;
    FirebaseUser currentUser;
    DatabaseReference Customer_dbReference , Drivers_AvailableReference , DriverReference , DriverLocationRef ;

    Marker DriverMarker,PickUpMarker;

    String Customer_ID;
    LatLng CustomerPickUpLocation;


    int radius = 1;
    Boolean driverfound = false , requestType = false;
    String driverfoundID;

    ValueEventListener DriverLocationListenerRef;
    GeoQuery geoQuery;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer__maps);

        ButterKnife.bind(this);


        auth=FirebaseAuth.getInstance();
        currentUser=auth.getCurrentUser();
        currentUser = auth.getCurrentUser();
        Customer_ID = currentUser.getUid();

        Customer_dbReference = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
        Drivers_AvailableReference = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        customer_sett.setOnClickListener(view -> {
            Intent intent = new Intent(Customer_MapsActivity.this, Settings_Activity.class);
            intent.putExtra("type","Customers");
            startActivity(intent);
        });

        customer_logout.setOnClickListener(view -> {

            auth.signOut();
            logout_customer();
            SharedPrefranceManager.getInastance(Customer_MapsActivity.this).clearUser();
        });

        call_captain.setOnClickListener(view -> {


            if (!lastLocation.equals(null)) {

                if (requestType) {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriverLocationRef.removeEventListener(DriverLocationListenerRef);

                    if (driverfound != null) {

                        DriverReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverfoundID)
                                .child("CustomerRideID");
                        DriverReference.removeValue();

                        driverfoundID = null;
                    }

                    driverfound = false;
                    radius = 1;
                    GeoFire geoFire = new GeoFire(Customer_dbReference);
                    geoFire.removeLocation(Customer_ID, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {

                        }
                    });

                    //to cancel request of car
                    if (PickUpMarker != null) {
                        PickUpMarker.remove();
                    }

                    if (DriverMarker != null) {
                        DriverMarker.remove();
                    }
                    call_captain.setText("Call a captain");
                    rel1.setVisibility(View.GONE);

                } else {

                    requestType = true;


                    GeoFire geoFire = new GeoFire(Customer_dbReference);
                    geoFire.setLocation(Customer_ID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()),
                            new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {

                                }
                            });

                    CustomerPickUpLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(CustomerPickUpLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));


                    call_captain.setText("Getting your Driver...");
                    GetAvailableNearbyDriverCap();
                }

            }

        });

    }

    private void getAssignedDriverInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverfoundID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){

                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String car = dataSnapshot.child("car").getValue().toString();


                    name_driver.setText(name);
                    phone_driver.setText(phone);
                    car_name_driver.setText(car);


                    if (dataSnapshot.hasChild("image")){
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profile_image_driver);
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void GetAvailableNearbyDriverCap() {

        GeoFire geoFire = new GeoFire(Drivers_AvailableReference);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPickUpLocation.latitude,CustomerPickUpLocation.longitude),
                radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            // important
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                //anytime the driver is called this method will be called
                //key = driverID and the location
                if (!driverfound && requestType){
                    driverfound = true;
                    driverfoundID = key;

                    DriverReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                            .child(driverfoundID);
                    HashMap drivermap = new HashMap();
                    drivermap.put("CustomerRideID",Customer_ID);
                    DriverReference.updateChildren(drivermap);


                    GettingDriverLocation();
                    call_captain.setText("Looking For driver Location");

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            // important
            @Override
            public void onGeoQueryReady() {

                if (!driverfound){
                   radius = radius + 1;
                   GetAvailableNearbyDriverCap();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });


    }

    private void GettingDriverLocation() {

       DriverLocationListenerRef =  DriverLocationRef.child(driverfoundID).child("l")
                         .addValueEventListener(new ValueEventListener() {
                             @Override
                             public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                 if (dataSnapshot.exists() && requestType){

                                     List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();

                                     double locationlatitude = 0;
                                     double locationlongtude = 0;

                                     call_captain.setText("Driver Found");


                                     rel1.setVisibility(View.VISIBLE);
                                     getAssignedDriverInformation();

                                     //call_captain.setEnabled(false);

                                     if (driverLocationMap.get(0) != null){
                                         locationlatitude = Double.parseDouble(driverLocationMap.get(0).toString());

                                     }
                                     if (driverLocationMap.get(1) != null){
                                         locationlongtude = Double.parseDouble(driverLocationMap.get(1).toString());

                                     }
                                     LatLng DriverLng = new LatLng(locationlatitude,locationlongtude);
                                     if (DriverMarker != null){
                                         DriverMarker.remove();
                                     }

                                     Location location1 = new Location("");
                                     location1.setAltitude(CustomerPickUpLocation.latitude);
                                     location1.setAltitude(CustomerPickUpLocation.longitude);

                                     Location location2 = new Location("");
                                     location2.setAltitude(DriverLng.latitude);
                                     location2.setAltitude(DriverLng.longitude);

                                     //calculate distance among location1 and 2
                                     float distance = location1.distanceTo(location2);

                                     if (distance <90){
                                         call_captain.setText("Driver Arrived");
                                     }else {
                                         call_captain.setText("Driver found at : " + String.valueOf(distance));
                                     }



                                     DriverMarker = mMap.addMarker(new MarkerOptions().position(DriverLng).title("Driver Here!").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_car)));

                                 }

                             }

                             @Override
                             public void onCancelled(@NonNull DatabaseError databaseError) {

                             }
                         });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            // get current location

        }
    }


    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // بيعمل تحديث للوكيشن كل كام ثانية
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); // 1 second
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY); // بتخليه يقرا اللوكيشن اسرع

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION )== PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,this);}

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocation = location;

        LatLng latLng= new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
        
    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    private void logout_customer() {

        Intent towelcome=new Intent(Customer_MapsActivity.this, Welcome_Activity.class);
        towelcome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(towelcome);
        finish();
    }
}
