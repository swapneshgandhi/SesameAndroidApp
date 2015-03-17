package com.example.sesameclient;


import group.pals.android.lib.ui.lockpattern.LockPatternActivity;
import group.pals.android.sesame.database.*;
import java.io.IOException;
import java.nio.charset.Charset;
import net.sqlcipher.database.SQLiteDatabase;
import com.example.sesameclient.SigninActivity;
import com.example.sesameclient.PairedBTDevice;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SigninActivity extends Activity {

	// Debugging
	private static final String TAG = "LoginActivity";
	private static final int LINK_ = 999;

	private PairedBTDevice dev=null;
	private BluetoothConnect conn = new BluetoothConnect(this);
	static SesameDbAdapter mDbHelper;
	private String StdPatternStr="init";

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	//will be updated when user authenticates himself.
	static String passkey= "";

	// This is your preferred flag
	private static final int REQ_CREATE_PATTERN = 1;
	private static final int REQ_ENTER_PATTERN = 2;

	// Flag to see if all fields filled in before closing this activity
	boolean allFilledFlag = false;
	char[] savedPattern=null;
	char[] pattern;

	Button createPatt; 
	Button enterPatt;
	//handle pattern creation preferences
	public static final String MyPREFERENCES = "MyPrefs" ;
	private static final int REQUEST_ENABLE_BT = 10;
	SharedPreferences PatternStatus;
	final String Entry = "PatternCreated";
	final String Initial= "false";
	final String AfterCreation= "true";
	Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("Signin","onCreate");
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_signin);

		PatternStatus = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

		//Instantiate DB
		//sql cipher libs load
		SQLiteDatabase.loadLibs(this);
		mDbHelper = new SesameDbAdapter(this);
		mDbHelper.open(passkey);

		//if getString returns NULL that means it's the first time we are running
		//the app so make entry false on whether pattern is created.
		if(PatternStatus.getString(Entry, "null").equals("null")){	
			editor = PatternStatus.edit(); 
			editor.putString(Entry, "false");
			editor.commit(); 
		}

		createPatt = (Button) findViewById(R.id.button1);
		createPatt.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//Run lockpattern activity in the pattern library code to create the pattern
				Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
						SigninActivity.this, LockPatternActivity.class);
				startActivityForResult(intent, REQ_CREATE_PATTERN);
			}
		});

		enterPatt = (Button) findViewById(R.id.button2);
		enterPatt.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//run lockpattern activity in the pattern library code to verify the pattern
				Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
						SigninActivity.this, LockPatternActivity.class);
				intent.putExtra(LockPatternActivity.EXTRA_PATTERN, StdPatternStr);
				startActivityForResult(intent, REQ_ENTER_PATTERN);
			}
		});
	}

	private void enableBlueTooth(){
		if (!mBluetoothAdapter.isEnabled()) {
			Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			Log.e("starting BT",""+REQUEST_ENABLE_BT);
			startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
		}
		else{
			setConn();
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		String address;

		Log.e("onActivityRes",""+requestCode);

		switch (requestCode) {

		case REQUEST_ENABLE_BT:
			Log.e("onActivityRes",""+"HERE");

			if(mBluetoothAdapter.isEnabled()) {
				setConn();
			} else {  
				Toast.makeText(this, "Please turn Bluetooth ON", Toast.LENGTH_SHORT);
			}
			break;

		case REQ_CREATE_PATTERN: {
			if (resultCode == RESULT_OK) {
				pattern = data.getCharArrayExtra(
						LockPatternActivity.EXTRA_PATTERN);
				savedPattern=pattern;
				passkey=String.valueOf(savedPattern);
				try{//open the database with the verified passkey
					mDbHelper.open(passkey);
				}
				catch(Exception e){
					Toast.makeText(SigninActivity.this, "Wrong Pattern!", Toast.LENGTH_LONG).show();
					return;
				}
				
				Log.e(TAG, "patternb4resume "+passkey);
				editor.putString(Entry, AfterCreation);
				editor.commit();
				enableBlueTooth();
				Toast.makeText(SigninActivity.this, "Pattern Saved and being connected...", Toast.LENGTH_SHORT).show();
			}
			break;
		}// REQ_CREATE_PATTERN
		case REQ_ENTER_PATTERN: {
			/*
			 * NOTE that there are 4 possible result codes!!!
			 */
			switch (resultCode) {
			case RESULT_OK:
				// The user passed
				pattern = data.getCharArrayExtra(
						LockPatternActivity.EXTRA_PATTERN);
				passkey=String.valueOf(pattern);
				Log.e("LoginActivity new pass",passkey);
				try{
					mDbHelper.getExistDataBase(SigninActivity.passkey);
				}
				catch(Exception e){
					Toast.makeText(SigninActivity.this, "Wrong Pattern!", Toast.LENGTH_LONG).show();
					return;
				}
				
				enableBlueTooth();
				break;
			case RESULT_CANCELED:
				// The user cancelled the task
				Log.e(TAG, "RESULT CANCELLED");
				Toast.makeText(SigninActivity.this, "You cancelled the login!", Toast.LENGTH_LONG).show();
				break;
			case LockPatternActivity.RESULT_FAILED:
				// The user failed to enter the pattern
				Log.e(TAG, "RESULT Failed");
				Toast.makeText(SigninActivity.this, "Wrong Pattern!", Toast.LENGTH_LONG).show();
				break;
			case LockPatternActivity.RESULT_FORGOT_PATTERN:
				// The user forgot the pattern and invoked your recovery Activity.
				Log.e(TAG, "RESULT Forgot Pattern");
				break;
			}

			/*
			 * In any case, there's always a key EXTRA_RETRY_COUNT, which holds
			 * the number of tries that the user did.
			 */
			data.getIntExtra(
					LockPatternActivity.EXTRA_RETRY_COUNT, 0);

			break;
		}// REQ_ENTER_PATTERN
		case LINK_:
			Intent addr = getIntent();
			address = addr.getStringExtra("chosen_address");
			Log.e(TAG, "RCVD INTENT ADDR:"+address);
		}
	}

	public void sendMessage(String message) throws IOException, NullPointerException{
		// Check that there's actually something to send

		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothLoginService to write
			Charset ISO = Charset.forName("ISO-8859-1");
			byte[] send = message.getBytes(ISO);

			Log.i(TAG,"numbytes: " + send.length);
			Log.e(TAG, message);

			conn.writeByte(send);
		}
	}

	public void sendBytes(byte [] FileBuffer) throws IOException, NullPointerException{
		// Check that there's actually something to send

		if (FileBuffer.length > 0) {

			conn.writeByte(FileBuffer);
			// finishActivity(temp);
		}
	}

	void setConn(){

		dev= new PairedBTDevice();
		String address = dev.getMACAddress();
		Log.e(TAG, "ADDRESS:"+address);

		if(address.equals("CHOOSE")){
			Log.e(TAG, "MORE THAN ONE PAIRED DEVICE");
			Intent accountIntent = new Intent(getBaseContext(),ChooseDeviceActivity.class);
			startActivityForResult(accountIntent,LINK_);
		}

		else{
			try{
				BluetoothDevice device= mBluetoothAdapter.getRemoteDevice(address);
				conn.connect(device);
				Toast.makeText(SigninActivity.this, "Connected to the device", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Connected");
				//Connecting to Accounts Activity
				Intent intent1 = new Intent (SigninActivity.this,AccountActivity.class);
				startActivity(intent1);
			}
			catch(Exception e){
				Toast.makeText(SigninActivity.this, "Failed to connect to the device!", Toast.LENGTH_SHORT).show();
			}
		}
	}

	void killConn(String msg) throws IOException{
		sendMessage(msg);
		// finishActivity(10);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.signin_options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		} else if (itemId == R.id.idForgotPattern) {
			mDbHelper.deleteRow();
			/*if(testDBResult!=-1){ //DB has it, so send back
    			Log.e(TAG, testDBResult.toString());
    			Log.e(TAG, "Rows deleted ");
    		}*/
			return true;
		} 
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		try{
			//if pattern is not created then disable the enter pattern button
			if(PatternStatus.getString(Entry, "false").equals("false")){
				enterPatt.setVisibility(View.INVISIBLE);
			}
			//if pattern is already created then disable the create pattern button
			else if(PatternStatus.getString(Entry, "false").equals("true")){
				createPatt.setVisibility(View.INVISIBLE);
				enterPatt.setVisibility(View.VISIBLE);
			}
		}
		catch(Exception e){
			Log.e("onResume","NULLLL");
			enterPatt.setVisibility(View.INVISIBLE);
			enterPatt.setVisibility(View.VISIBLE);
		}
	}
}
