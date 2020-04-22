package com.mahmoud.bashir.taxia.Maps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
import com.mahmoud.bashir.taxia.DriverLoginActivity;
import com.mahmoud.bashir.taxia.R;
import com.mahmoud.bashir.taxia.Settings_Activity;
import com.mahmoud.bashir.taxia.Storage.SharedPrefranceManager;
import com.mahmoud.bashir.taxia.Welcome_Activity;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class Driver_MapsActivity extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    public static final int PERMISSION_REQUEST_LOCATION_CODE=99;
    @BindView(R.id.driver_logout)Button logoutbtn;
    @BindView(R.id.driver_sett)Button driver_sett;
    @BindView(R.id.rel2) RelativeLayout rel2;
    @BindView(R.id.name_customer) TextView name_customer;
    @BindView(R.id.phone_customer) TextView phone_customer;
    @BindView(R.id.Ring_to_customer) ImageView Ring_to_customer;
    @BindView(R.id.profile_image_customer) CircleImageView profile_image_customer;


    FirebaseAuth auth;
    FirebaseUser currentUser;
    DatabaseReference assignedcustomerRef , assignedcustomerPickUpRef;

    private boolean currentdriverlogoutStatus=false;

    String DriverID,CustomerID="";
    Marker PickUpMarker;
    ValueEventListener assignedcustomerPickUpRefListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver__maps);
        ButterKnife.bind(this);

        auth=FirebaseAuth.getInstance();
        currentUser=auth.getCurrentUser();
        DriverID=currentUser.getUid();

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            CheckLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        driver_sett.setOnClickListener(view -> {
            Intent intent = new Intent(Driver_MapsActivity.this, Settings_Activity.class);
            intent.putExtra("type","Drivers");
            startActivity(intent);
        });

        logoutbtn.setOnClickListener(view -> {

            currentdriverlogoutStatus = true;
            DisconnectDriver();

            auth.signOut();
            LogoutDriver();

            SharedPrefranceManager.getInastance(Driver_MapsActivity.this).clearUser();
        });

        GetAssignedCustomerRequest();
    }



    private void getAssignedCustomerInformation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(CustomerID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {

                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();


                    name_customer.setText(name);
                    phone_customer.setText(phone);


                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profile_image_customer);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
        private void GetAssignedCustomerRequest() {

        assignedcustomerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                              .child("Drivers").child(DriverID).child("CustomerRideID");

          assignedcustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    CustomerID = dataSnapshot.getValue().toString();
                    GetAssignedCustomerPickUpLocation();

                    rel2.setVisibility(View.VISIBLE);
                    getAssignedCustomerInformation();

                }else {
                    CustomerID = "";
                    if (PickUpMarker != null){
                        PickUpMarker.remove();
                    }

                    if (assignedcustomerPickUpRefListener != null){
                        assignedcustomerPickUpRef.removeEventListener(assignedcustomerPickUpRefListener);
                    }

                    rel2.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }

    private void GetAssignedCustomerPickUpLocation() {

        assignedcustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Customers Request")
                                    .child(CustomerID).child("1");

        assignedcustomerPickUpRefListener = assignedcustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double customerlocationlatitude = 0;
                    double customerlocationlongtude = 0;


                    if (customerLocationMap.get(0) != null){
                        customerlocationlatitude = Double.parseDouble(customerLocationMap.get(0).toString());

                    }
                    if (customerLocationMap.get(1) != null){
                        customerlocationlatitude = Double.parseDouble(customerLocationMap.get(1).toString());

                    }

                    LatLng DriverLng = new LatLng(customerlocationlatitude,customerlocationlongtude);
                    mMap.addMarker(new MarkerOptions().position(DriverLng).title("Customer Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));



                }/*else {
                    CustomerID = "";
                    if (PickUpMarker != null){

                    }

                    if (assignedcustomerPickUpRef != null){
                        assignedcustomerPickUpRef.removeEventListener(assignedcustomerPickUpRefListener);
                    }
                }*/
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



    public boolean CheckLocationPermission(){

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_LOCATION_CODE);
            }else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION_CODE);
            }
            return false;
        }
        else
            return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISSION_REQUEST_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permisiion is granted
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        if (googleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else //permission is denied
                {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    // if (getApplicationContext() != null){
         lastLocation = location;

         LatLng latLng= new LatLng(location.getLatitude(),location.getLongitude());
         mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
         mMap.animateCamera(CameraUpdateFactory.zoomTo(18));


         String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
         DatabaseReference driverRefAvailability = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
         DatabaseReference driverworkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");


         // update user current location every second to firebase
         GeoFire geoFiredriverworking=new GeoFire(driverworkingRef);
         GeoFire geoFiredriveravailability=new GeoFire(driverRefAvailability);

         geoFiredriverworking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
             @Override
             public void onComplete(String key, DatabaseError error) {

             }
         });

         switch (CustomerID){
             case "":
                 geoFiredriverworking.removeLocation(userID, new GeoFire.CompletionListener() {
                     @Override
                     public void onComplete(String key, DatabaseError error) {

                     }
                 });
                 geoFiredriveravailability.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                     @Override
                     public void onComplete(String key, DatabaseError error) {

                     }
                 });
                   break;
             default:
                 geoFiredriveravailability.removeLocation(userID, new GeoFire.CompletionListener() {
                     @Override
                     public void onComplete(String key, DatabaseError error) {

                     }
                 });
                 geoFiredriverworking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                     @Override
                     public void onComplete(String key, DatabaseError error) {

                     }
                 });
                     break;
         }





     //}



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
    protected void onStop() {
        super.onStop();

        if (!currentdriverlogoutStatus){
            DisconnectDriver();
        }


    }

    private void DisconnectDriver() {

        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRefAvailability = FirebaseDatabase.getInstance().getReference().child("Drivers Available");

        GeoFire geoFire=new GeoFire(driverRefAvailability);
        geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });
    }

    private void LogoutDriver() {
        Intent towelcome=new Intent(Driver_MapsActivity.this, Welcome_Activity.class);
        towelcome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(towelcome);
        finish();

    }
}
