package com.mahmoud.bashir.taxia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Register_Activity extends AppCompatActivity {

    @BindView(R.id.register_btn)Button register_btn;
    @BindView(R.id.email_regist)EditText email_regist;
    @BindView(R.id.pass_regist)EditText pass_regist;

    String getemail,getpass,getSort;

    private FirebaseAuth mAuth;
    String Customer_UID="",Driver_UID;

    DatabaseReference Customer_Info_Ref,Driver_Info_Ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);



        getSort=getIntent().getStringExtra("sort");


        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();


        Customer_Info_Ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers");
        Driver_Info_Ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers");

        register_btn.setOnClickListener(view -> {

            getemail=email_regist.getText().toString();
            getpass=pass_regist.getText().toString();

            if (getemail.isEmpty() || getpass.isEmpty()){
                email_regist.setError("Please enter ur email!");
                email_regist.requestFocus();


                pass_regist.setError("Please enter ur password!");
                pass_regist.requestFocus();
                return;
            }else{
                signin_auth(getemail,getpass,getSort);
            }

        });
    }

    private void signin_auth(String email, String pass,String sort) {

        Toast.makeText(this, ""+sort, Toast.LENGTH_SHORT).show();

        if (sort.equals("customer")){
            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this , new OnCompleteListener<AuthResult>(){
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                FirebaseUser user = mAuth.getCurrentUser();
                                Customer_UID = user.getUid();

                                Customer_Info_Ref.child(Customer_UID).setValue(true);



                                Intent i = new Intent(Register_Activity.this, CustomerLoginActivity.class);
                                i.putExtra("CUID", Customer_UID);
                                startActivity(i);
                                finish();


                            }
                        }
                    });
        }else if(sort.equals("driver")){
            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this , new OnCompleteListener<AuthResult>(){
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){

                                FirebaseUser user = mAuth.getCurrentUser();
                                Driver_UID=user.getUid();
                                Driver_Info_Ref.child(Driver_UID).setValue(true);

                                    Intent i =new Intent(Register_Activity.this,DriverLoginActivity.class);
                                    i.putExtra("CUID",Driver_UID);
                                    startActivity(i);
                                    finish();

                            }
                        }
                    });
        }

    }
}
