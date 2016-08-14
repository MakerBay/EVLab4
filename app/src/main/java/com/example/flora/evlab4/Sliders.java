package com.example.flora.evlab4;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.UUID;


public class Sliders extends Activity {
    
    //Variables and Widgets
    Button btn_front, btn_back, btn_disconnect;
    SeekBar speed, steering, brake;
    TextView sp, st, br;
    String value1 = String.valueOf(0);
    String value2 = String.valueOf(0);
    String value3 = String.valueOf(0);
    String values = String.valueOf(0);
    String valuefull1 = String.valueOf(0);
    int valuefull = 0;


    //Bluetooth setup
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        //Get address from previous activity
        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESSA); 

        //Layout
        setContentView(R.layout.activity_sliders);

        //call the widgtes
        btn_front = (Button)findViewById(R.id.btn_front);
        btn_back = (Button)findViewById(R.id.btn_back);
        btn_disconnect = (Button)findViewById(R.id.btn_disconnect);
        speed = (SeekBar)findViewById(R.id.seekBar);
        steering = (SeekBar)findViewById(R.id.seekBar2);
        brake = (SeekBar)findViewById(R.id.seekBar3);
        sp = (TextView)findViewById(R.id.sp);
        st = (TextView)findViewById(R.id.st);
        br = (TextView)findViewById(R.id.br);

        //Connect to Bluetooth
        new ConnectBT().execute(); 

        //Front
        btn_front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnFront();
            }
        });

        //Back
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnBack();
            }
        });

        //Disconnect
        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        //Brake Control
        brake.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar3, int progress3, boolean fromUser) {
                if (fromUser==true) {
                    br.setText(String.valueOf(progress3));
                    value3 = String.valueOf(progress3);
                    addValues();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar3) {    }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar3) {     }
        });

        //Steering Control
        steering.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar2, int progress2, boolean fromUser) {
                if (fromUser==true) {
                    st.setText(String.valueOf(progress2));
                    value2 = String.valueOf(progress2);
                    addValues();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar2) {    }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar2) {    }
        });

        //Speed Control
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress1, boolean fromUser) {
                if (fromUser==true) {
                    sp.setText(String.valueOf(progress1));
                    value1 = String.valueOf(progress1);
                    addValues();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {     }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {    }
        });


    }

    //Disconnect() to close bluetooth socket
    private void disconnect() {
        if (btSocket!=null) {
            try {
                btSocket.close();
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish();
    }

    //turnBack() to switch relay
    private void turnBack() {
        if (btSocket!=null) {
            try {
                btSocket.getOutputStream().write("TF".toString().getBytes());
            }
            catch (IOException e) {
                msg("Error");
            }
        }
    }

    //turnFront() to switch relay
    private void turnFront() {
        if (btSocket!=null) {
            try {
                btSocket.getOutputStream().write("TO".toString().getBytes());
            }
            catch (IOException e) {
                msg("Error");
            }
        }
    }

    //addValues() to send data to Arduino via Bluetooth
    private void addValues() {
        if (btSocket!=null) {
            valuefull = Integer.parseInt(value1)+Integer.parseInt(value2)+Integer.parseInt(value3);
            valuefull1 = String.valueOf(valuefull);

            values = value1+","+value2+","+value3+","+valuefull1+"#";

            try {
                btSocket.getOutputStream().write(values.toString().getBytes());
                btSocket.getOutputStream().flush();
            } catch (IOException e) {
                msg("Error");
            }
        }
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //class to connect to Bluetooth
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(Sliders.this, "Connecting...", "Please wait!!!");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            }
            catch (IOException e) {
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
            }
            else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}