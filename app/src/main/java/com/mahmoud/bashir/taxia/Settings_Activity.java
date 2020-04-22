package com.mahmoud.bashir.taxia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.mahmoud.bashir.taxia.Maps.Customer_MapsActivity;
import com.mahmoud.bashir.taxia.Maps.Driver_MapsActivity;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.sql.Driver;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class Settings_Activity extends AppCompatActivity {

    @BindView(R.id.profile_image)       CircleImageView profileImage ;
    @BindView(R.id.etd_name)            EditText etd_name ;
    @BindView(R.id.etd_phone)           EditText etd_phone ;
    @BindView(R.id.etd_driver_car_Name) EditText etd_driver_car_Name ;
    @BindView(R.id.change_pic)          TextView change_pic ;
    @BindView(R.id.close_button)        ImageView close_button ;
    @BindView(R.id.save_button)         ImageView save_button ;


    String getType , checker="";
    Uri imageUri;
    String myUri="" ;
    StorageReference storageprofileReference;
    StorageTask uploadTask;
    DatabaseReference databaseReference;
    FirebaseAuth auth;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        getType = getIntent().getStringExtra("type");
        auth=FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageprofileReference = FirebaseStorage.getInstance().getReference().child("Profile Pictures");



        if (getType.equals("Drivers")){
            etd_driver_car_Name.setVisibility(View.VISIBLE);
        }


        close_button.setOnClickListener(view -> {
            if (getType.equals("Drivers")){

                startActivity(new Intent(Settings_Activity.this , Driver_MapsActivity.class));
                finish();

            }else{
                startActivity(new Intent(Settings_Activity.this , Customer_MapsActivity.class));
                finish();
            }

        });

        save_button.setOnClickListener(view -> {

            if (checker.equals("clicked")){
                validateControllers();

            }else {
                validateAndSaveOnlyInformations();
            }

        });

        change_pic.setOnClickListener(view -> {
            checker = "clicked";

            CropImage.activity()
                     .setAspectRatio(1,1)
                     .start(Settings_Activity.this);

        });

        getUserInformation();

    }

    private void validateAndSaveOnlyInformations() {

        if (TextUtils.isEmpty(etd_name.getText().toString())){
            etd_name.setError("Plz enter your name!");
            etd_name.isFocusable();
        }else  if (TextUtils.isEmpty(etd_phone.getText().toString())){
            etd_phone.setError("Plz enter your phone!");
            etd_phone.isFocusable();

        }else  if (getType.equals("Drivers") && TextUtils.isEmpty(etd_driver_car_Name.getText().toString())) {
            etd_driver_car_Name.setError("Plz enter your phone!");
            etd_driver_car_Name.isFocusable();
        }else {
            HashMap<String, Object> usermap = new HashMap<>();
            usermap.put("uid", auth.getCurrentUser().getUid());
            usermap.put("name", etd_name.getText().toString());
            usermap.put("phone", etd_phone.getText().toString());

            if (getType.equals("Drivers")) {
                usermap.put("car", etd_driver_car_Name.getText().toString());
            }
            databaseReference.child(auth.getCurrentUser().getUid()).updateChildren(usermap);

            if (getType.equals("Drivers")) {
                startActivity(new Intent(Settings_Activity.this, Driver_MapsActivity.class));
                finish();
            } else {
                startActivity(new Intent(Settings_Activity.this, Customer_MapsActivity.class));
                finish();
            }
        }
    }
    private void getUserInformation(){
        databaseReference.child(auth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                            String name = dataSnapshot.child("name").getValue().toString();
                            String phone = dataSnapshot.child("phone").getValue().toString();


                            etd_name.setText(name);
                            etd_phone.setText(phone);

                            if (getType.equals("Drivers")){
                                String car = dataSnapshot.child("car").getValue().toString();
                                etd_driver_car_Name.setText(car);
                            }
                            if (dataSnapshot.hasChild("image")){
                                String image = dataSnapshot.child("image").getValue().toString();
                                Picasso.get().load(image).into(profileImage);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode==RESULT_OK && data !=null)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();


            profileImage.setImageURI(imageUri);

        }else {

            if (getType.equals("Drivers")){
                startActivity(new Intent(Settings_Activity.this,Driver_MapsActivity.class));
            }else {
                startActivity(new Intent(Settings_Activity.this,Customer_MapsActivity.class));
            }


            Toast.makeText(this, "Error, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateControllers(){
        if (TextUtils.isEmpty(etd_name.getText().toString())){
            etd_name.setError("Plz enter your name!");
            etd_name.isFocusable();
        }else  if (TextUtils.isEmpty(etd_phone.getText().toString())){
            etd_phone.setError("Plz enter your phone!");
            etd_phone.isFocusable();

        }else  if (getType.equals("Drivers") && TextUtils.isEmpty(etd_driver_car_Name.getText().toString())){
            etd_driver_car_Name.setError("Plz enter your phone!");
            etd_driver_car_Name.isFocusable();
        }else if (checker.equals("clicked")){
            uploadProfilePicture();
        }

    }

    private void uploadProfilePicture() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please wait, while we setting our account info!");
        progressDialog.show();


        if (imageUri != null){

            final StorageReference fileRef = storageprofileReference.child(auth.getCurrentUser().getUid() + ".jpg");

            uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                   if (!task.isSuccessful()){
                       throw task.getException();
                   }
                   return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){
                        Uri downloaduri = task.getResult();
                        myUri = downloaduri.toString();


                        HashMap<String,Object> usermap=new HashMap<>();
                        usermap.put("uid",auth.getCurrentUser().getUid());
                        usermap.put("name",etd_name.getText().toString());
                        usermap.put("phone",etd_phone.getText().toString());
                        usermap.put("image",myUri);

                        if (getType.equals("Drivers")){
                            usermap.put("car",etd_driver_car_Name.getText().toString());
                        }
                        databaseReference.child(auth.getCurrentUser().getUid()).updateChildren(usermap);
                        progressDialog.dismiss();

                        if (getType.equals("Drivers")){
                            startActivity(new Intent(Settings_Activity.this,Driver_MapsActivity.class));
                            finish();
                        }else{
                            startActivity(new Intent(Settings_Activity.this,Customer_MapsActivity.class));
                            finish();
                        }
                    }
                }
            })
            ;

        }else {
            Toast.makeText(this, "image is not selected", Toast.LENGTH_SHORT).show();
        }

    }
}
