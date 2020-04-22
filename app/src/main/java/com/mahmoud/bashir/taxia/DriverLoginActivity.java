package com.mahmoud.bashir.taxia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mahmoud.bashir.taxia.Maps.Driver_MapsActivity;
import com.mahmoud.bashir.taxia.Storage.SharedPrefranceManager;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DriverLoginActivity extends AppCompatActivity {


    private static final String TAG = "DriverLoginActivity";

    @BindView(R.id.login_d_btn)Button login_d_btn;
    @BindView(R.id.email_d)EditText email_d;
    @BindView(R.id.pass_d)EditText pass_d;
    @BindView(R.id.register_d)TextView register_d;

    DatabaseReference reference;
    FirebaseAuth auth;
    String CUID="";

    String getemail,getpass;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);
        ButterKnife.bind(this);


        if(SharedPrefranceManager.getInastance(this).isLoggedIn()){
            startActivity(new Intent(this,Driver_MapsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }






        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();


        reference= FirebaseDatabase.getInstance().getReference().child("Drivers");
        CUID=getIntent().getStringExtra("CUID");

        register_d.setOnClickListener(view -> {
            Intent i = new Intent(DriverLoginActivity.this,Register_Activity.class);
            i.putExtra("sort","driver");
            startActivity(i);
        });


        login_d_btn.setOnClickListener(view -> {
            getemail=email_d.getText().toString();
            getpass=pass_d.getText().toString();

            if (getemail.isEmpty() || getpass.isEmpty()){

                email_d.setError("plz enter correct email");
                email_d.requestFocus();


                pass_d.setError("plz enter correct password");
                pass_d.requestFocus();
                return;
            }else {
                login_existinUser(getemail,getpass);
            }

        });


    }

    private void login_existinUser(String email, String pass) {

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = auth.getCurrentUser();

                           /* HashMap<String,String> map=new HashMap<>();
                            map.put("name","Abooshy");
                            map.put("email",email);

                            reference.child(CUID).setValue(map);*/

                            Intent i=new Intent(DriverLoginActivity.this, Driver_MapsActivity.class);
                            startActivity(i);
                            finish();

                            SharedPrefranceManager.getInastance(DriverLoginActivity.this).saveDriver("drivername",email);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(DriverLoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }
}
