package com.embeddedcentric.nrf5touchscreendemo1;

/* Copyright (c) 2019, EmbeddedCentric.com
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
Company : EmbeddedCentric.com
Date : 19 April 2019
Title : nRF5 TouchScreen Demo1
A simple Android App for EmbeddedCentric Touch Screen Demo 1
This demo is part of Nordic nRF5x BLE In-Depth Training Course -Intermediate Level ( Optional extracurricular activity after lesson 12) by EmbeddedCentric
 */
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;


import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    public enum AppLogFontType {APP_NORMAL, APP_ERROR, PEER_NORMAL, PEER_ERROR};
    private String mLogMessage = "";
    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();

    // Variables to access objects from the layout such as buttons, switches, values
    private static Button search_button;
    private static Button disconnect_button;
    private static Switch led_switch;
    private static TextView mTextViewLog;

    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static NUSControl mNUSControl;

    private static final int REQUEST_ENABLE_BLE = 1;

    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the NUSControl is connected
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mNUSControl = ((NUSControl.LocalBinder) service).getService();
            mServiceConnected = true;
            mNUSControl.initialize();
        }

        /**
         * This is called when the NUSControl service is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mNUSControl = null;
        }
    };

    /**
     * This is called when the main activity is first created
     *
     * @param savedInstanceState is any state saved from prior creations of this activity
     */
    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up variables for accessing buttons and slide switches
        search_button = (Button) findViewById(R.id.search_button);
        disconnect_button = (Button) findViewById(R.id.disconnect_button);
        led_switch = (Switch) findViewById(R.id.led_switch);
        mTextViewLog = (TextView)findViewById(R.id.ConLogtextView);
        // Initialize service and connection state variable
        mServiceConnected = false;
        mConnectState = false;

        //This section required for Android 6.0 (Marshmallow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access ");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        } //End of section for Android 6.0 (Marshmallow)

        /* This will be called when the LED On/Off switch is touched */
        led_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Turn the LED on or OFF based on the state of the switch
                mNUSControl.writeRxCharacteristic(isChecked);
            }
        });



        // Find BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, NUSControl.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // turn on the search  button
        search_button.setEnabled(true);
        Log.d(TAG, "Bluetooth is Enabled");
    }

    //This method required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                    writeToLog("Coarse location permission granted",AppLogFontType.APP_NORMAL);
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    writeToLog("Coarse location permission NOT granted!",AppLogFontType.APP_ERROR);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver. This specified the messages the main activity looks for from the NUSControl
        final IntentFilter filter = new IntentFilter();
        filter.addAction(NUSControl.ACTION_BLESCAN_CALLBACK);
        filter.addAction(NUSControl.ACTION_CONNECTED);
        filter.addAction(NUSControl.ACTION_DISCONNECTED);
        filter.addAction(NUSControl.ACTION_SERVICES_DISCOVERED);
        filter.addAction(NUSControl.ACTION_DATA_RECEIVED);
        filter.addAction(NUSControl.ACTION_BLESCAN_FAILED);
        registerReceiver(mBleUpdateReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close and unbind the service when the activity goes away
        mNUSControl.close();
        unbindService(mServiceConnection);
        mNUSControl = null;
        mServiceConnected = false;
    }



    /**
     * This method handles the Search for Device button
     *
     * @param view the view object
     */
    public void searchBluetooth(View view) {
        if(mServiceConnected) {
            mNUSControl.scan();
            writeToLog("Scanning for device",AppLogFontType.APP_NORMAL);

        }

        /* After this we wait for the scan callback to detect that a device has been found */
        /* The callback broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Disconnect button
     *
     * @param view the view object
     */
    public void Disconnect(View view) {
        mNUSControl.disconnect();

        /* After this we wait for the gatt callback to report the device is disconnected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case NUSControl.ACTION_BLESCAN_CALLBACK:
                    search_button.setEnabled(false);
                    Log.d(TAG, "Device Found");
                    writeToLog("Device Found",AppLogFontType.PEER_NORMAL);
                    //Connect to device
                    mNUSControl.connect();
                    break;
                case NUSControl.ACTION_CONNECTED:
                    if (!mConnectState) {
                        disconnect_button.setEnabled(true);
                        mConnectState = true;
                        Log.d(TAG, "Connected to Device");
                        writeToLog("Connected to Device",AppLogFontType.PEER_NORMAL);
                        mNUSControl.discoverServices();
                    }
                    break;
                case NUSControl.ACTION_DISCONNECTED:
                    disconnect_button.setEnabled(false);
                    search_button.setEnabled(true);
                    led_switch.setChecked(false);
                    led_switch.setEnabled(false);
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    writeToLog("Device Disconnected",AppLogFontType.PEER_ERROR);
                    break;
                case NUSControl.ACTION_SERVICES_DISCOVERED:
                    led_switch.setEnabled(true);
                    Log.d(TAG, "Services Discovered");
                    writeToLog("Services Discovered",AppLogFontType.PEER_NORMAL);
                    break;
                case NUSControl.ACTION_DATA_RECEIVED:
                    writeToLog("Data Received",AppLogFontType.PEER_NORMAL);
                    if(mNUSControl.getLedState()){
                        led_switch.setChecked(true);
                    } else {
                        led_switch.setChecked(false);
                    }
                    String status = mNUSControl.getButtonState();
                    writeToLog(status,AppLogFontType.PEER_NORMAL);
                    break;
                case NUSControl.ACTION_BLESCAN_FAILED:
                    writeToLog("Failed to find device",AppLogFontType.PEER_ERROR);
                    break;
                default:
                    break;
            }
        }
    };

    private void writeToLog(String message, AppLogFontType msgType){
        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        String newMessage = currentDateTimeString + " - " + message;
        String fontHtmlTag;
        switch(msgType){
            case APP_NORMAL:
                fontHtmlTag = "<font color='#000000'>";
                break;
            case APP_ERROR:
                fontHtmlTag = "<font color='#AA0000'>";
                break;
            case PEER_NORMAL:
                fontHtmlTag = "<font color='#0000AA'>";
                break;
            case PEER_ERROR:
                fontHtmlTag = "<font color='#FF00AA'>";
                break;
            default:
                fontHtmlTag = "<font>";
                break;
        }
        mLogMessage = fontHtmlTag + newMessage + "</font>" + "<br>" + mLogMessage;
        mTextViewLog.setText(Html.fromHtml(mLogMessage));
    }
}

