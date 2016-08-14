package com.example.flora.evlab4;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import static java.lang.Math.acos;
import static java.lang.Math.asin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import static java.lang.Math.acos;
import static java.lang.Math.asin;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;


import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.UUID;

public class AccelerometerThrottle extends Activity implements SensorEventListener {

    //Bluetooth settings
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    //Send data via Bluetooth
    String steeringAc = String.valueOf(0);
    String throttleAc = String.valueOf(0);
    String brakeAc = String.valueOf(0);
    String send = String.valueOf(0);
    String checksum = String.valueOf(0);
    int checksum_int = 0;

    //SPP UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float y = 0;
    private float z = 0;

    private int steering = 0;
    private int throttle = 0;
    private int brake = 0;

    int track = 0;

    double convert = 180 / Math.PI;

    public boolean forward = true;
    boolean motorOn = false;
    boolean isCalibrated = false;

    public float angleX_Y, angleX_Z, angleXCalc, angleCalib, angleThrottle, angleBrake;

    final static int middlePoint = 127;
    final static float rangeN = 127;
    final static float rangeP = 128;
    final static float rangeThrottle = 50;
    final static float rangeBrake = 35;
    final static float rangeSteering = 90;
    final static float brakeMax = 255;

    private TextView angleX, steeringValue, throttleValue, brakeValue, trackValue, calibratedAngle;

    Button btn_changeDirection, btn_motor, btn_disconnect;

    ImageView logo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer_throttle);
        initializeViews();
        initializeButtons();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void initializeViews() {
        angleX = (TextView) findViewById(R.id.angleX);
        steeringValue = (TextView) findViewById(R.id.steering);
        throttleValue = (TextView) findViewById(R.id.throttle);
        brakeValue = (TextView) findViewById(R.id.brake);
        trackValue = (TextView) findViewById(R.id.track);
        calibratedAngle = (TextView) findViewById(R.id.angleCalib);

        logo = (ImageView) findViewById(R.id.makerbay_logo);
    }

    public void initializeButtons() {
        btn_changeDirection = (Button) findViewById(R.id.btn_changeDirection);
        btn_changeDirection.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                forward = !forward;
                if (forward) {
                    btn_changeDirection.setText("Going forward - press to go backward");
                } else btn_changeDirection.setText("Going backward - press to go forward");
            }
        });
        btn_motor = (Button) findViewById(R.id.btn_motor);
        btn_motor.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                motorOn = !motorOn;
                calibrate();
                if (motorOn) {
                    btn_motor.setText("Motor is ON");
                } else btn_motor.setText("Motor is OFF");
            }//hallo
        });
        btn_disconnect = (Button) findViewById(R.id.btn_disconnect);
        btn_disconnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
            disconnect();
            }
        });
    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        displayValues();
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        y = event.values[1];//always configure it according to device, the following code is tailored to a landscape tablet
        z = event.values[2];//x-axis along long side (positive direction is left <-- right), y-axis along short side (90° CCW to x)
        //z-axis through display plane (positive direction is out of display)

        ///call methods only when certain threshold in change is reached to save computing power?

        calcangleX_YZ();
        calcThrottle();

        displayValues();
    }


    void calcangleX_YZ() {
        float yBaseP = (float) 9.65;
        float yBaseN = (float) -9.9;
        float zBaseP = (float) 10.1;
        float zBaseN = (float) -9.9;

        float yPrec1 = (float) 3.5;
        float yPrec2 = (float) -3.5;
        float yPrec3 = (float) -3.5;
        float yPrec4 = (float) 3.2;

        float zPrec1 = (float) 2.2;
        float zPrec2 = (float) 3.4;
        float zPrec3 = (float) -3;
        float zPrec4 = (float) -3;

        if (y > 0 && z > 0) {//0°-90° of rotation
            if (y < yBaseP) {
                angleX_Y = (float) (acos(y / yBaseP) * convert);
            } else angleX_Y = 0;
            if (z < zBaseP) {
                angleX_Z = (float) (asin(z / zBaseP) * convert);
            } else angleX_Z = 90;

            if (y < yPrec1) {
                angleXCalc = angleX_Y;
            } else if (z < zPrec1) {
                angleXCalc = angleX_Z;
            } else angleXCalc = (angleX_Y + angleX_Z) / 2;

        } else if (y < 0 && z > 0) {//90°-180°
            if (y > yBaseN) {
                angleX_Y = (float) (acos(y / (-yBaseN)) * convert);
            } else angleX_Y = 180;
            if (z < zBaseP) {
                angleX_Z = (float) (180 - (asin(z / zBaseP) * convert));
            } else angleX_Z = 90;

            if (y > yPrec2) {
                angleXCalc = angleX_Y;
            } else if (z < zPrec2) {
                angleXCalc = angleX_Z;
            } else angleXCalc = (angleX_Y + angleX_Z) / 2;

        } else if (y < 0 && z < 0) {//180° - (-90°)
            if (y > yBaseN) {
                angleX_Y = (float) (-(acos(y / (-yBaseN)) * convert));
            } else angleX_Y = -180;
            if (z > zBaseN) {
                angleX_Z = (float) (-180 - (asin(z / (-zBaseN)) * convert));
            } else angleX_Z = -90;

            if (y > yPrec3) {
                angleXCalc = angleX_Y;
            } else if (z > zPrec3) {
                angleXCalc = angleX_Z;
            } else angleXCalc = (angleX_Y + angleX_Z) / 2;

        } else if (y > 0 && z < 0) {//-90° - 0°
            if (y < yBaseP) {
                angleX_Y = (float) (-(acos(y / yBaseP) * convert));
            } else angleX_Y = 0;
            if (z > zBaseN) {
                angleX_Z = (float) (asin(z / (-zBaseN)) * convert);
            } else angleX_Z = -90;

            if (y < yPrec4) {
                angleXCalc = angleX_Y;
            } else if (z > zPrec4) {
                angleXCalc = angleX_Z;
            } else angleXCalc = (angleX_Y + angleX_Z) / 2;
        }
    }

    void calcThrottle() {
        int noiseCalib = 5;
        int noiseEdge = 15;

        if (angleXCalc > angleCalib + noiseCalib && angleXCalc < angleThrottle) { //see sketch -> 1: normal acceleration, tilt away from user
            track = 1;
            brake = 0;
            if (forward) {
                throttle = middlePoint + Math.round((rangeP / rangeThrottle) * (angleXCalc - angleCalib));
            } else
                throttle = middlePoint - Math.round((rangeP / rangeThrottle) * (angleXCalc - angleCalib));
        } else if (angleXCalc < angleCalib - noiseCalib && angleXCalc > angleBrake) { //see sketch -> 2: normal brake, tilt towards user
            track = 2;
            throttle = middlePoint;
            brake = Math.round((brakeMax / rangeBrake) * (angleCalib - angleXCalc));
        } else if (angleXCalc < angleCalib + noiseCalib && angleXCalc > angleCalib - noiseCalib) { //see sketch -> 3: inside corridor, no changes to be transmitted
            track = 3;
            brake = 0;
            throttle = middlePoint;
        } else if (angleXCalc > angleThrottle && angleXCalc < angleThrottle + noiseEdge) { //see sketch -> 4: outside acceleration, still inside noise area
            track = 4;
            throttle = middlePoint + (int) rangeP;
        } else if (angleXCalc < angleBrake && angleXCalc > angleBrake - noiseEdge) { //see sketch -> 5: outside brake, still inside noise area
            track = 5;
            brake = (int) brakeMax;
        } else if (angleXCalc > angleThrottle + noiseEdge || angleXCalc < angleBrake - noiseEdge) { //see sketch -> 6: outside of any noise area
            track = 6;
            brake = (int) brakeMax;
            throttle = middlePoint;
        }
    }

    void changeDirection() {
        forward = !forward;
        if (forward) {
            ((Button) findViewById(R.id.btn_changeDirection)).setText("Direction: forward");
        } else ((Button) findViewById(R.id.btn_changeDirection)).setText("Direction: backward");
    }

    void calibrate() {
        calcangleX_YZ();
        if (angleXCalc + rangeThrottle > 180 || angleXCalc - rangeBrake < -180) {
            Toast.makeText(getApplicationContext(), "Cannot calibrate in this position, please change position and try again", Toast.LENGTH_SHORT).show();
            return;
        }
        angleCalib = angleXCalc;
        angleThrottle = angleCalib + rangeThrottle;
        angleBrake = angleCalib - rangeBrake;
        isCalibrated = true;

        calibratedAngle.setText(String.valueOf(angleCalib));

    }

    void displayValues() {
        angleX.setText(String.valueOf(angleXCalc));
        steeringValue.setText(String.valueOf(steering));
        throttleValue.setText(String.valueOf(throttle));
        brakeValue.setText(String.valueOf(brake));
        trackValue.setText(String.valueOf(track));
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    //Send data via Bluetooth
    private void addValues() {
        if (btSocket != null) {

            steeringAc = Float.toString(steering);
            throttleAc = Float.toString(throttle);
            brakeAc = Float.toString(brake);

            checksum_int = Integer.parseInt(steeringAc) + Integer.parseInt(throttleAc) + Integer.parseInt(brakeAc);
            checksum = String.valueOf(checksum);

            send = steeringAc + "," + throttleAc + "," + brakeAc + "," + checksum + "#";


            try {
                btSocket.getOutputStream().write(send.toString().getBytes());
                btSocket.getOutputStream().flush();
            } catch (IOException e) {
                msg("Error");
            }
        }
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(AccelerometerThrottle.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        //while the progress dialog is shown, the connection is done in background
        protected Void doInBackground(Void... devices) {
            try {

                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }

            return null;
        }

        @Override
        //after the doInBackground, it checks if everything went fine
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    private void disconnect() {
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                msg("Error Disconnect");
            }
        }
        finish();
    }


}
