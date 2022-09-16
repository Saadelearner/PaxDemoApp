/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.paxdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class BluetoothDeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "extra_device_address";
    public static String ALL_DEVICE_ADDRESS = "all_device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.bt_device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.bt_device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.bt_device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.bt_pairedDevices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.bt_newDevices);

        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Register for updating list to friendly device name
        filter = new IntentFilter(BluetoothDevice.ACTION_NAME_CHANGED);
        this.registerReceiver(mReceiver, filter);
                
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            String noDevices = "Bluetooth is not available";
            mPairedDevicesArrayAdapter.add(noDevices);
        } else {
            // Get a set of currently paired devices
            @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size() > 0) {
                findViewById(R.id.bt_title_pairedDevices).setVisibility(View.VISIBLE);
                for (BluetoothDevice device : pairedDevices) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                String noDevices = "No paired device";
                mPairedDevicesArrayAdapter.add(noDevices);
            }

            // start discovery automatically.
            if (checkPermission(this)) {
                doDiscovery();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle("Scanning...");

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    private static boolean checkPermission(Context context) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        //API LEVEL 18
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions((Activity)context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            } else{
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1001:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doDiscovery();
                } else {
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            
            if (info.equals("No paired device")) {
            	return;
            }

            if (info.equals("No paired device")) {
                return;
            }
            
            String address = info.substring(info.length() - 17);

            Log.d(TAG, "selected bt addr is: " + address);
            
            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            
            ArrayList<String> devices = new ArrayList<String>();
            /*
            for (int i = 0, size = mPairedDevicesArrayAdapter.getCount(); i < size; i++) {
            	devices.add(mPairedDevicesArrayAdapter.getItem(i));
            }
            */
            for (int i = 0, size = mNewDevicesArrayAdapter.getCount(); i < size; i++) {
            	String item = mNewDevicesArrayAdapter.getItem(i);
                if (item.equals("No paired device")) {
                	continue;
                }
            	devices.add(item);
            }
            
            intent.putStringArrayListExtra(ALL_DEVICE_ADDRESS, devices);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                	Log.i(TAG, "add:" + device.getName() + "\n" + device.getAddress());
                    boolean bFound = false;
                    for (int i = 0; i < mNewDevicesArrayAdapter.getCount(); i++) {
                        String info = mNewDevicesArrayAdapter.getItem(i);
                        String[] s = info.split("\n");
                        if (s.length > 2 && s[1].equals(device.getAddress())) {
                            bFound = true;
                            break;
                        }
                    }
                    if(!bFound)
                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("Select a device to connect");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "No paired device";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
            else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                	for (int i = 0; i < mNewDevicesArrayAdapter.getCount(); i++) {
                        String info = mNewDevicesArrayAdapter.getItem(i);
                		String[] s = info.split("\n");
                		if (s[1].equals(device.getAddress())) {
                			String newName = device.getName() + "\n" + s[1];
                            mNewDevicesArrayAdapter.remove(info);
                            mNewDevicesArrayAdapter.insert(newName, i);
                            mNewDevicesArrayAdapter.notifyDataSetChanged();
                			break;
                		}
                	}
                }
            }
        }
    };

}
