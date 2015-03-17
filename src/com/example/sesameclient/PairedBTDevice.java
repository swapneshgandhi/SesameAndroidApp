package com.example.sesameclient;

import java.util.Set;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.widget.ArrayAdapter;

public class PairedBTDevice extends Activity{
  
	// Debugging
    private static final String TAG = "DeviceAddress";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    
    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    String macAddr = null;
    
    // Get MAC Address of the Paired Device
    
    public String getMACAddress(){
    
    	// Get the local Bluetooth adapter
    	  mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    	    Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
    	    
    	    if (pairedDevices.size() == 1) {
    	      
    	    	for (BluetoothDevice device : pairedDevices) {
    	    		macAddr= device.getAddress();
    	    		Log.e(TAG, macAddr);
                }
    	    }
    	    else{
    	    	macAddr = "CHOOSE";
    	    }
    	    
    	  return macAddr;
    }
}
