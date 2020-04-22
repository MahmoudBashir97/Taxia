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
import com.mahmoud.bashir.taxia.Maps.Customer_MapsActivity;
import com.mahmoud.bashir.taxia.Maps.Driver_MapsActivity;
import com.mahmoud.bashir.taxia.Storage.SharedPrefranceManager;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CustomerLoginActivity extends AppCompatActivity {

    private static final String TAG = "CustomerLoginActivity";

    @BindView(R.id.login_c_btn)Button login_c_btn;
    @BindView(R.id.email_c)EditText email_c;
    @BindView(R.id.pass_c)EditText pass_c;
    @BindView(R.id.register_c)
    TextView register_c;

    DatabaseReference reference;
    FirebaseAuth auth;
    String CUID="";

    String getemail,getpass;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);
        ButterKnife.bind(this);


        if(SharedPrefranceManager.getInastance(this).isLoggedIn()){
            startActivity(new Intent(this,Customer_MapsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }



        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();


        reference= FirebaseDatabase.getInstance().getReference().child("Customers");
        CUID=getIntent().getStringExtra("CUID");

        register_c.setOnClickListener(view -> {
            Intent i = new Intent(CustomerLoginActivity.this,Register_Activity.class);
            i.putExtra("sort","customer");
            startActivity(i);
        });




        login_c_btn.setOnClickListener(view -> {
            getemail=email_c.getText().toString();
            getpass=pass_c.getText().toString();

            if (getemail.isEmpty() || getpass.isEmpty()){

                email_c.setError("plz enter correct email");
                email_c.requestFocus();


                pass_c.setError("plz enter correct password");
                pass_c.requestFocus();
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
                            CUID = user.getUid();

                            HashMap<String,String> map=new HashMap<>();
                            map.put("name","Mahmoud Bashir");
                            map.put("email",email);

                            reference.child(CUID).setValue(map);

                            Intent i=new Intent(CustomerLoginActivity.this, Customer_MapsActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                            finish();

                            SharedPrefranceManager.getInastance(CustomerLoginActivity.this).saveCustomer("Mahmoud",email);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(CustomerLoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }
}
