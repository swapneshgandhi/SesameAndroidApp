package com.example.sesameclient;

import group.pals.android.sesame.database.SesameDbAdapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AccountActivity extends Activity {

	public final static String Account_Type = "edu.example.sesameclient.MESSAGE";
	private static final String TAG = "AccountActivity";
	// public final static int REQ_STARTB;

	// Current Account information for login
	private String currentAcct = "";
	private String username = "";
	private String password = "";
	private String acctType = "";
	String AcctClicked = "";
	String currAct = "";
	static String dbPath;
	// Name of the connected device
	private String mConnectedDeviceName = null;

	// Message types sent from the BluetoothLoginService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	public static final int MESSAGE_ACCT_TYPE = 1;

	// Key names received from the BluetoothLoginService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	private static final int EXIT_CMD = -1;
	private static final int LINK_ = 999;

	private SesameDbAdapter mDbHelperAA;
	private boolean DbOpen=false;
	private SigninActivity mSAct;
	private ManageAcctActivity mActA;
	String acctSelected = "";
	public static String ubmail_acct = "";
	public static String gmail_acct = "";
	public static String fb_acct = "";
	public static String twitter_acct = "";
	public static String qq_acct = "";
	ImageView image;
	ImageView image1;
	static Object RestoreLock=new Object();
	static Object BackupLock=new Object();
	static boolean RestoreDB=false;
	static boolean BackupDb=false;

	// Spinner element
	Spinner spinner1, spinner2, spinner3, spinner4, spinner5;

	String acctBeginningString = "Chosen Account: ";

	boolean listen_ubmail = false;
	boolean listen_gmail = false;
	boolean listen_twitter = false;
	boolean listen_fb = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account);

		loadActivity();
		// mDbHelperAA.close();

		//backup button 
		Button backup = (Button) findViewById(R.id.backup);
		backup.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				FileInputStream fis = null;
				BufferedInputStream bis = null;
				Cursor Check=mDbHelperAA.fetchAllAccts();

				if(Check==null){
					Toast.makeText(AccountActivity.this,
							"NO Accounts present to backup", Toast.LENGTH_SHORT)
							.show();
					return;
				}

				else{
					try {
						//Database path
						dbPath = getApplicationContext().getDatabasePath(SesameDbAdapter.DATABASE_NAME).getPath();
						//getDatabasePath("ignored").getParentFile();
						File myFile = new File (dbPath);
						
						//send backup message to sesame server
						final String message = CreateMailAcctActivity.BACKUP + " "
								+myFile.length();
						mSAct.sendMessage(message);

						byte [] mybytearray  = new byte [(int)myFile.length()];
						fis = new FileInputStream(myFile);
						bis = new BufferedInputStream(fis);
						bis.read(mybytearray,0,mybytearray.length);
						Log.e(TAG, "Sending DB file, size is :"+mybytearray.length);

						bis.close();
						fis.close();
						//make sure the send message and database go in different packets by waiting
						Thread.sleep(500);
						mSAct.sendBytes(mybytearray);
						BackupDb=false;
						//wait to get successful response form sesame server
						//and handle UI responses accordingly
						synchronized (BackupLock) {
							try {
								BackupLock.wait(1500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	
						}

						if (BackupDb){
							Toast.makeText(AccountActivity.this,
									"Accounts data sent to sesame server successfully!", Toast.LENGTH_SHORT)
									.show();
						}
						else{
							Toast.makeText(AccountActivity.this,
									"An error occured during sending data to sesame server", Toast.LENGTH_SHORT)
									.show();
						}

					}
					catch(NullPointerException e){
						Toast.makeText(AccountActivity.this,
								"Looks like you are not connected to sesame server, please connect first", Toast.LENGTH_SHORT)
								.show();
						Intent intent = new Intent(getApplicationContext(), SigninActivity.class);
						startActivity(intent);
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						Toast.makeText(AccountActivity.this,
								"An error occured during sending data to sesame server", Toast.LENGTH_SHORT)
								.show();
						Log.e("backup Exception",e.getLocalizedMessage());
					}
				}
			}
		});

		//Restore button code
		Button Restore = (Button) findViewById(R.id.restore);
		Restore.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//send restore message to sesame server
				final String message = CreateMailAcctActivity.RESTORE + " "
						+"Send me the database";
				Log.e(TAG, message);

				try {
					dbPath = getApplicationContext().getDatabasePath(SesameDbAdapter.DATABASE_NAME).getPath();
					File Db= new File(dbPath);
					if (Db.exists()){
						//delete database- so restore will simply use server database
						//as the final database.
						getApplicationContext().deleteDatabase(SesameDbAdapter.DATABASE_NAME);
					}
					Log.e("dbpath",dbPath);
					mSAct.sendMessage(message);
					RestoreDB=false;

					//all of the database receiving work will be done on
					//BluetoothConnect class and will notify us when it's done
					synchronized (RestoreLock) {
						try {
							RestoreLock.wait(1500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
					}

					//Handle UI messages to display depending on whether things went well. 
					if (RestoreDB){
						Toast.makeText(AccountActivity.this,
								"Database restored", Toast.LENGTH_SHORT)
								.show();
						Log.e("dbpath",SigninActivity.passkey);
						mDbHelperAA.close();
						//try opening database
						mDbHelperAA.getExistDataBase(SigninActivity.passkey);
						mDbHelperAA.open(SigninActivity.passkey);
						loadSpinnerData();
						Toast.makeText(getApplicationContext(), "Accounts Restored successfully", Toast.LENGTH_SHORT).show();
					}

					else{
						Toast.makeText(AccountActivity.this,
								"Error ocuured while restroing accounts", Toast.LENGTH_SHORT)
								.show();
					}

				} catch (Exception e) {

					if (e!=null) Log.e("Restore Exceptionn",Log.getStackTraceString(e));

					Toast.makeText(getApplicationContext(), "Database couldn't be logged into " +
							"Let's get you logged in to try again", Toast.LENGTH_SHORT).show();
					//If we cound not log in to data base try to log in again
					Intent intent = new Intent(getApplicationContext(), SigninActivity.class);

					startActivity(intent);
				}
			}
		});
	}

	private void loadActivity() {
		// TODO Auto-generated method stub
		// Show the Up button in the action bar.
		// getActionBar().setDisplayHomeAsUpEnabled(true);

		mDbHelperAA = new SesameDbAdapter(this);

		if (!DbOpen){
			Log.e("AccountActivity passkey:",SigninActivity.passkey);
			DbOpen=mDbHelperAA.open(SigninActivity.passkey);
		}

		mSAct = new SigninActivity();
		mActA = new ManageAcctActivity();

		spinner1 = (Spinner) findViewById(R.id.spinner1);
		spinner2 = (Spinner) findViewById(R.id.spinner2);
		spinner3 = (Spinner) findViewById(R.id.spinner3);
		spinner4 = (Spinner) findViewById(R.id.spinner4);
		spinner5 = (Spinner) findViewById(R.id.spinner5);

		loadSpinnerData();

		spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
				((TextView) parent.getChildAt(0)).setGravity(Gravity.CENTER);

				String label = parent.getItemAtPosition(position).toString();

				ubmail_acct = label;

			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		spinner2.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {

				((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
				((TextView) parent.getChildAt(0)).setGravity(Gravity.CENTER);
				String label1 = parent.getItemAtPosition(position).toString();
				// Showing selected spinner item
				Log.v(TAG, "acc is::" + label1);
				// if(listen_gmail!=false){
				String label = parent.getItemAtPosition(position).toString();

				gmail_acct = label;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		spinner3.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {

				((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
				((TextView) parent.getChildAt(0)).setGravity(Gravity.CENTER);
				String label = parent.getItemAtPosition(position).toString();
				// Showing selected spinner item
				Log.v(TAG, "acc is::" + label);

				fb_acct = label;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		spinner4.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
				((TextView) parent.getChildAt(0)).setGravity(Gravity.CENTER);
				String label = parent.getItemAtPosition(position).toString();
				Log.v(TAG, "acc is::" + label);
				twitter_acct = label;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		spinner5.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
				((TextView) parent.getChildAt(0)).setGravity(Gravity.CENTER);
				String label = parent.getItemAtPosition(position).toString();
				Log.v(TAG, "acc is::" + label);
				qq_acct = label;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		// Initialize the ubmail button
		image = (ImageView) findViewById(R.id.imageView1);
		Button ubmail = (Button) findViewById(R.id.button1);
		ubmail.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Intent newAccountIntent = new
				// Intent(AccountActivity.this,ManageAcctActivity.class);
				// newAccountIntent.putExtra(Account_Type, "ubmail");
				// startActivity(newAccountIntent);
				Log.v(TAG, "Button:" + ubmail_acct);
				gotoCurrentAcct(ubmail_acct);

			}
		});

		// Initialize the gmail button
		image1 = (ImageView) findViewById(R.id.imageView2);
		Button gmail = (Button) findViewById(R.id.button2);
		gmail.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				gotoCurrentAcct(gmail_acct);
			}
		});

		// Initialize the facebook button
		image = (ImageView) findViewById(R.id.imageView3);
		Button fb = (Button) findViewById(R.id.button3);
		fb.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				gotoCurrentAcct(fb_acct);
			}
		});

		// Initialize the twitter button
		image = (ImageView) findViewById(R.id.imageView4);
		Button twitter = (Button) findViewById(R.id.button4);
		twitter.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				gotoCurrentAcct(twitter_acct);
			}
		});

		// Initialize the qq button
		image = (ImageView) findViewById(R.id.imageView5);
		Button qq = (Button) findViewById(R.id.button5);
		qq.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				gotoCurrentAcct(qq_acct);
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			acctSelected = msg.obj.toString();
		}
	};

	/**
	 * Function to load the spinner data from SQLite database
	 * */
	void loadSpinnerData() {

		Log.v(TAG, "no problem with cursor");
		String ubmail_acct_request = "ubmail";
		String gmail_acct_request = "gmail";
		String fb_acct_request = "facebook";
		String twitter_acct_request = "twitter";
		String qq_acct_request = "qq";
		List<String> ubmail_lables, gmail_lables, fb_lables, twitter_lables, qq_lables;
		// if(!acctSelected.isEmpty()){
		ubmail_lables = mDbHelperAA.getSpinnerAccounts(ubmail_acct_request);
		gmail_lables = mDbHelperAA.getSpinnerAccounts(gmail_acct_request);
		fb_lables = mDbHelperAA.getSpinnerAccounts(fb_acct_request);
		twitter_lables = mDbHelperAA.getSpinnerAccounts(twitter_acct_request);
		qq_lables = mDbHelperAA.getSpinnerAccounts(qq_acct_request);
		// Creating adapter for spinner
		ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, ubmail_lables);
		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, gmail_lables);
		ArrayAdapter<String> dataAdapter3 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, fb_lables);
		ArrayAdapter<String> dataAdapter4 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, twitter_lables);
		ArrayAdapter<String> dataAdapter5 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, qq_lables);

		// Drop down layout style - list view with radio button
		dataAdapter1
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataAdapter2
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataAdapter3
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataAdapter4
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataAdapter5
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// attaching data adapter to spinner
		spinner1.setAdapter(dataAdapter1);
		spinner2.setAdapter(dataAdapter2);
		spinner3.setAdapter(dataAdapter3);
		spinner4.setAdapter(dataAdapter4);
		spinner5.setAdapter(dataAdapter5);

	}

	private List<String> setEmptySpinner() {
		// TODO Auto-generated method stub
		List<String> labels = new ArrayList<String>();
		labels.add("EMPTY");
		return labels;
	}

	void gotoCurrentAcct(String acct_clicked) {
		// Initialize the BluetoothLoginService to perform Bluetooth login

		try {
			Log.v(TAG, "cChemkjds");
			Log.v(TAG, "current account is" + acct_clicked);
			Cursor currCursor = mDbHelperAA.fetchAcct(acct_clicked);
			currentAcct = currCursor.getString(2); // get the pseudonym
			Log.v(TAG, currentAcct);
			username = currCursor.getString(3); // get the email addy
			Log.v(TAG, username);
			password = currCursor.getString(4); // get the pswd
			Log.v(TAG, password);
			acctType = currCursor.getString(5); // get the acct type
			Log.v(TAG, acctType);
			if (acctType.equals("qq")) {
				if (username.length() - 7 > 0) {
					Log.v(TAG, username.substring(username.length() - 7));
					if (username.substring(username.length() - 7).equals(
							"@qq.com"))
						username = username.substring(0, username.length() - 7);
				}
			}
			Log.v(TAG, currentAcct + username + password + acctType);
		} catch (Exception e) {
			Log.v(TAG, e.toString());
			Toast.makeText(AccountActivity.this,
					"No account currently selected.", Toast.LENGTH_SHORT)
					.show();
			currentAcct = "";
		}
		if (currentAcct.length() > 0) {
			// Send a login message as we have an account!
			final String message = CreateMailAcctActivity.LOGIN_BT + " "
					+ acctType + " ^|^ " + username + " ^|^ " + password;
			Log.v(TAG, message);
			try {
				mSAct.sendMessage(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	void getCurrAccountInfo(String acct_info) {
		acctSelected = acct_info.substring(1);
		if (acct_info.substring(0, 1).equals("0")) {
			ubmail_acct = acctSelected;
			// loadSpinnerData();
			Log.v(TAG, "Selected account" + ubmail_acct);
		} else if (acct_info.substring(0, 1).equals("1")) {
			gmail_acct = acctSelected;
			// loadSpinnerData();
		} else if (acct_info.substring(0, 1).equals("2")) {
			fb_acct = acctSelected;
			// loadSpinnerData();
		} else if (acct_info.substring(0, 1).equals("3")) {
			twitter_acct = acctSelected;
			// loadSpinnerData();
		} else if (acct_info.substring(0, 1).equals("4")) {
			qq_acct = acctSelected;
			// loadSpinnerData();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my_options_menu, menu);
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
		} else if (itemId == R.id.idManageAccts) {
			Intent accountIntent = new Intent(this, ManageAcctActivity.class);
			// startActivityForResult(accountIntent,REQ_STARTB);
			startActivityForResult(accountIntent, LINK_);
			return true;
		} else if (itemId == R.id.idKillConnection) {
			// Send a kill message
			final String message = AccountActivity.EXIT_CMD + " exit server";
			try {
				mSAct.killConn(message);
				// setResult(100);
				onBackPressed();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == 999) {

			Log.v(TAG, "Got Reply from Activity");
			loadActivity();
		}
	}

	protected void onResume() {
		super.onResume();
		try{loadSpinnerData();}
		catch(Exception e){

		}
		Log.e(TAG, "ON RESUME IS CALLED");
	}

}