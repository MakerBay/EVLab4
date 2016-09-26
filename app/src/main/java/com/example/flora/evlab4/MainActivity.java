package com.example.flora.evlab4;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    //Variables
    String address = null;
    public static String EXTRA_ADDRESSA = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get address from previous activity
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS);

        //Layout
        setContentView(R.layout.activity_main);

        //Switch to Accelerometer Steering Activity
        Button btn_AccSteering = (Button) findViewById(R.id.btn_AccSteering);
        btn_AccSteering.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent k = new Intent(MainActivity.this, AccelerometerSteering.class);
                k.putExtra(EXTRA_ADDRESSA, address);
                startActivity(k);
            }
        });

        //Switch to Slider Activity
        Button btn_Slider = (Button) findViewById(R.id.btn_Slider);
        btn_Slider.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent l = new Intent(MainActivity.this, Sliders.class);
                l.putExtra(EXTRA_ADDRESSA, address);
                startActivity(l);
            }
        });

        //Switch to Accelerometer Throttle Activity
        Button btn_AccThrottle = (Button) findViewById(R.id.btn_AccThrottle);
        btn_AccThrottle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent k = new Intent(MainActivity.this, AccelerometerThrottle.class);
                k.putExtra(EXTRA_ADDRESSA, address);
                startActivity(k);
            }
        });

        Button btn_Trackpad = (Button) findViewById(R.id.btn_Trackpad);
        btn_Trackpad.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent k = new Intent(MainActivity.this, Trackpad.class);
                k.putExtra(EXTRA_ADDRESSA, address);
                startActivity(k);
            }
        });


        //Switch to Data Collection Activity

    }
}

