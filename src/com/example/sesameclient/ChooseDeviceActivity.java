package com.example.sesameclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class ChooseDeviceActivity extends Activity {

	protected static final String TAG = "ChooseDeviceActivity";

	protected static final int LINK_CONN = 999;

	ArrayAdapter<String> mPairedDevicesArrayAdapter;
	Spinner spinner1;

	String macAddr;

	private BluetoothConnect conn = new BluetoothConnect(this);
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_device);

		spinner1 = (Spinner) findViewById(R.id.spinn1);
		loadSpinnerDevices();

		spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position,long id) {

				//                   String label = parent.getItemAtPosition(position).toString();
				String dev = parent.getItemAtPosition(position).toString();
				macAddr = dev.substring(dev.length() - 17);
				Log.e(TAG, "dev:"+macAddr);
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		Button done = (Button) findViewById(R.id.butt1);
		done.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				connect_device();
			}
		});

	}


	private void loadSpinnerDevices() {
		// TODO Auto-generated method stub
		List<String> paired_devices;

		paired_devices = getPairedDevices();
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,paired_devices);

		mPairedDevicesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spinner1.setAdapter(mPairedDevicesArrayAdapter);

	}

	/**
	 * Getting all labels
	 * returns list of labels
	 * */
	public List<String> getPairedDevices(){
		List<String> devices = new ArrayList<String>();

		// Get the local Bluetooth adapter
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			// findViewById(this).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				devices.add(device.getName() + "\n" + device.getAddress());
			}
		}
		return devices;

	}

	private void connect_device(){

		//Connecting to Accounts Activity
		try{
			BluetoothDevice device= mBluetoothAdapter.getRemoteDevice(macAddr);
			conn.connect(device);
			Toast.makeText(ChooseDeviceActivity.this, "Connected to the device", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Connected");
			Intent intent1 = new Intent (ChooseDeviceActivity.this,AccountActivity.class);
			startActivity(intent1);
		}catch(Exception e){
			Log.e(TAG, e.toString());
			Toast.makeText(ChooseDeviceActivity.this, "Failed to connect to the device!", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.choose_device, menu);
		return true;
	}

}
