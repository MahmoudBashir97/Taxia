package com.mahmoud.bashir.taxia;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Welcome_Activity extends AppCompatActivity {


    @BindView(R.id.driver_btn) Button driver_btn ;
    @BindView(R.id.customer_btn) Button customer_btn ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ButterKnife.bind(this);


        driver_btn.setOnClickListener(v ->{
            startActivity(new Intent(Welcome_Activity.this,DriverLoginActivity.class));

        });

        customer_btn.setOnClickListener(v ->{
            startActivity(new Intent(Welcome_Activity.this,CustomerLoginActivity.class));
        });


    }
}
