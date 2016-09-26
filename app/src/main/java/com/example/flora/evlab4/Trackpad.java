package com.example.flora.evlab4;


import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class Trackpad extends AppCompatActivity {

    private final static int BIG_CIRCLE_SIZE = 271;
    private final static int FINGER_CIRCLE_SIZE = 15;

    private boolean forward = true;

    private int steering, throttle, brake, checksum;
    private int middlePoint = 127, deadSteering, deadThrottle, deadBrake;
    String send;


    //SPP UUID
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyView v1 = new MyView(this);
        //setContentView(new MyView(this));
        setContentView(v1);

        steering = middlePoint;
        throttle = middlePoint;
        brake = 0;


        Intent newInt = getIntent();
        address = newInt.getStringExtra(MainActivity.EXTRA_ADDRESSA);

        new ConnectBT().execute();

        final Button btn_reverse = new Button(this);
        addContentView(btn_reverse, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        btn_reverse.setText("Car is moving FORWARD!");
        btn_reverse.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                forward = !forward;
                if (forward) {
                    btn_reverse.setText("Car is moving FORWARD");
                } else {
                    btn_reverse.setText("Car is moving BACKWARD");
                }
            }
        });

        mHandler.postDelayed(sRunnable, 600000);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<Trackpad> mActivity;

        public MyHandler(Trackpad activity) {
            mActivity = new WeakReference<Trackpad>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Trackpad activity = mActivity.get();
        }
    }

    private final MyHandler mHandler = new MyHandler(this);

    private final static Runnable sRunnable = new Runnable() {
        public void run() {
        }
    };


    class MyView extends View {

        Paint fingerPaint, borderPaint, textPaint;

        int dispWidth;
        int dispHeight;

        int x;
        int y;

        int xcirc;
        int ycirc;

        String directionL = "";
        String directionR = "";
        //String cmdSend;
        //String temptxtMotor;

        // variables for drag (���������� ��� ��������������)
        boolean drag = false;
        float dragX = 0;
        float dragY = 0;

        public MyView(Trackpad context) {
            super(context);
            fingerPaint = new Paint();
            fingerPaint.setAntiAlias(true);
            fingerPaint.setColor(Color.GREEN);

            borderPaint = new Paint();
            borderPaint.setColor(Color.BLUE);
            borderPaint.setAntiAlias(true);
            borderPaint.setStyle(Style.STROKE);
             borderPaint.setStrokeWidth(3);

            textPaint = new Paint();
            textPaint.setStyle(Style.FILL);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(14);
        }


        protected void onDraw(Canvas canvas) {
            dispWidth = (int) Math.round((this.getRight() - this.getLeft()) / 2.0);
            dispHeight = (int) Math.round((this.getBottom() - this.getTop()) / 2.0);
            if (!drag) {
                x = dispWidth;
                y = dispHeight;
                fingerPaint.setColor(Color.RED);
            }

            canvas.drawCircle(x, y, FINGER_CIRCLE_SIZE, fingerPaint);
            canvas.drawCircle(dispWidth, dispHeight, BIG_CIRCLE_SIZE, borderPaint);

            canvas.drawText(String.valueOf("X:" + xcirc), 10, 75, textPaint);
            canvas.drawText(String.valueOf("Y:" + (-ycirc)), 10, 95, textPaint);
            canvas.drawText(String.valueOf("Steering: " + steering), 10, 115, textPaint);
            canvas.drawText(String.valueOf("Throttle: " + throttle), 10, 135, textPaint);
            canvas.drawText(String.valueOf("Brake: " + brake), 10, 155, textPaint);
            canvas.drawText(String.valueOf("Checksum: " + checksum), 10, 175, textPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            // coordinate of Touch-event ()
            int evX = (int) event.getX();
            int evY = (int) event.getY();

            xcirc = (int) event.getX() - dispWidth;
            ycirc = (int) event.getY() - dispHeight;

            float radius = (float) Math.sqrt(Math.pow(Math.abs(xcirc), 2) + Math.pow(Math.abs(ycirc), 2));

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    if (radius >= 0 && radius <= BIG_CIRCLE_SIZE) {
                        x = evX;
                        y = evY;
                        fingerPaint.setColor(Color.GREEN);
                        controlMotor(xcirc, ycirc);
                        invalidate();
                        drag = true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    // if drag mode is enabled
                    if (drag && radius >= 0 && radius <= BIG_CIRCLE_SIZE) {
                        x = evX;
                        y = evY;
                        fingerPaint.setColor(Color.GREEN);
                        controlMotor(xcirc, ycirc);
                        invalidate();
                    }
                    break;

                // touch completed
                case MotionEvent.ACTION_UP:
                    // turn off the drag mode
                    xcirc = 0;
                    ycirc = 0;
                    drag = false;
                    controlMotor(xcirc, ycirc);
                    invalidate();
                    break;
            }
            return true;
        }
    }

    private void controlMotor(int x, int y) {
          deadSteering = 15;
        deadThrottle = 15;
        deadBrake = 15;



        y = -y;
        x = -x;

        if (Math.abs(x) > 15) {
            if(x < 0){
                steering = middlePoint - (int) ((Math.abs(x) - deadSteering) / 2.0);
            }
            else steering = (int) ((x - deadSteering) / 2.0) + middlePoint;
        } else {
            steering = middlePoint;
            }

        if ((y - 10) > deadBrake) {
            brake = 0;
            if (y > deadThrottle) {
                if (forward) {
                    throttle = ((int) ((y - deadThrottle) / 2.0)) + middlePoint;
                } else {
                    throttle = middlePoint - ((int) ((y - deadThrottle) / 2.0));
                }
            } else {
                throttle = middlePoint;
            }
        } else if (y < -deadBrake) {
            brake = Math.abs(y) - deadBrake;
            throttle = middlePoint;
        }
        else {
            brake = 0;
            throttle = middlePoint;
        }

        if (btSocket != null)

        {
            checksum = steering + throttle + brake;
            send = steering + "," + throttle + "," + brake + "," + checksum + "#";

            try {
                btSocket.getOutputStream().write(send.toString().getBytes());
                btSocket.getOutputStream().flush();
            } catch (IOException e) {
                msg("Error sending to Bluetooth");
            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    //class to connect to Bluetooth
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
                progress = ProgressDialog.show(Trackpad.this, "Connecting...", "Please wait!!!");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
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
}