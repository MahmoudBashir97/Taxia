package com.mahmoud.bashir.taxia;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Thread thread = new Thread() {

            @Override
            public void run() {

                try {
                    sleep(4000);
                    startActivity(new Intent(SplashScreenActivity.this, Welcome_Activity.class));
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();

                }
                }
            };
        thread.start();
        }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
