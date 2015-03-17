/**
 * This Activity appears as a dialog. It allows you to create
 * new accounts including fields such as email address, password,
 * chosen name (what this application shows it as in the list), and 
 * account type options currently supported!
 * 
 * This was created Denise C. Blady for the SEASAME project, known 
 * as DB in comments.
 * 
 * Source for Spinner functionality help:
 *  http://www.java-samples.com/showtutorial.php?tutorialid=1517
 * Source for dialog box code:
 *  http://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-in-android
 */

package com.example.sesameclient;
import group.pals.android.sesame.database.SesameDbAdapter;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CreateMailAcctActivity extends Activity {
	// Debugging
	private static final String TAG = "CreateAcctsActivity";
	private static final boolean D = true;

	// Return Intent extras
	public static String EXTRA_C_ACCT_NAME = "acct_name";   
	public static String EXTRA_C_ACCT_TYPE = "acct_type"; 
	static int LOGIN_BT=1;
	static int BACKUP=5;
	static int RESTORE=6;
	static final int BACKUP_OK = 7;
	// Member fields
	private EditText mEmailAddy;
	private EditText mPassword;
	private EditText mPasswordConfirm;
	private EditText mChosenName;
	private String theAcctName= "";
	private String mAccountTypePicked= "";
	private Object[] myAcctTypesArray;
	private String theEmailAddy= "";
	private String thePassword= "";
	private String theConfirmedPassword = "";
	private SesameDbAdapter mDbHelperCAA;
	private String mChosenAcct="";
	private Spinner mAcctTypeSpinner;

	// Flag to see if all fields filled in before closing this activity
	boolean allFilledFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_create_mail_acct);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);
		TextView tv = (TextView) findViewById(R.id.emailAddress);
		tv.setTextColor(Color.WHITE);
		TextView tv1 = (TextView) findViewById(R.id.password);
		tv1.setTextColor(Color.WHITE);
		TextView tv2 = (TextView) findViewById(R.id.passwordConfirm);
		tv2.setTextColor(Color.WHITE);
		TextView tv3 = (TextView) findViewById(R.id.chosen_Name_Acct);
		tv3.setTextColor(Color.WHITE);
		TextView tv4 = (TextView) findViewById(R.id.accountType);
		tv4.setTextColor(Color.WHITE);

		//Set my EditText member fields
		mEmailAddy= (EditText) findViewById(R.id.emailAddyBox);
		mPassword = (EditText) findViewById(R.id.passwordBox);
		mPasswordConfirm = (EditText) findViewById(R.id.passwordBoxConfirm);
		mChosenName= (EditText) findViewById(R.id.chosenNameBox);
		/*Intent intent = getIntent();
	    String message = intent.getStringExtra(ManageAcctActivity.Acct_Type);
	    mChosenAcct=message;*/

		// Initialize the button to cancel the Activity
		Button cancelButton = (Button) findViewById(R.id.button_cancelAcct);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(CreateMailAcctActivity.this);
				builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).show();
				//Go to dialogClickListener to see results of the choice -DB
			}
		});

		// Initialize the button to finish the Activity
		Button doneButton = (Button) findViewById(R.id.button_doneAcct);
		doneButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				boolean flag1= true;
				boolean flag2= true;
				boolean flag3= true;
				boolean flag4 =true;
				boolean flag5 =true;
				//Grab all my values(account type already covered in spinner listener)
				theEmailAddy= mEmailAddy.getText().toString();
				thePassword= mPassword.getText().toString();
				theConfirmedPassword = mPasswordConfirm.getText().toString();
				theAcctName= mChosenName.getText().toString();


				String debugResult= theEmailAddy + " " +thePassword + " " +
						theAcctName + " " + mChosenAcct;

				Log.i(TAG, debugResult);

				//Check fields
				if(mChosenAcct.length()==0){flag1=false;}
				if(theEmailAddy.length()==0){flag2=false;}
				if(thePassword.length()==0){flag3=false;}
				if(theAcctName.length()==0){flag4=false;}
				if(theConfirmedPassword.length()==0){flag5=false;}

				//Check if all fields filled
				if(flag1 && flag2 && flag3 && flag4 && flag5){allFilledFlag= true;}

				if(allFilledFlag){
					//First check if password and confirmed password are equal
					if(thePassword.equals(theConfirmedPassword)){
						//save results to the Accounts, useFlag==0 because not selected yet -DB
						Long testDBResult = mDbHelperCAA.createAcct(0, theAcctName, theEmailAddy, thePassword, mChosenAcct);
						if(testDBResult!=-1){ //DB has it, so send back
							Log.v(TAG, testDBResult.toString());
							//Send back acctName
							// Create the result Intent and include the account Name and Account Type
							Intent intent = new Intent();
							intent.putExtra(EXTRA_C_ACCT_NAME, theAcctName);
							intent.putExtra(EXTRA_C_ACCT_TYPE, mChosenAcct);

							// Set result
							setResult(Activity.RESULT_OK, intent);

						} else {
							Toast.makeText(CreateMailAcctActivity.this, "Account Creation Failed, Please Try Again, make sure chosen name is unique.", Toast.LENGTH_SHORT).show();
						}
						mDbHelperCAA.close();
						finish();
					} else {
						Toast.makeText(CreateMailAcctActivity.this, "Password couldn't be confirmed. Make sure both are the same.", Toast.LENGTH_SHORT).show();
					}
				} else { //alert user to fill all fields!
					Toast.makeText(CreateMailAcctActivity.this, "Please fill in all fields first!", Toast.LENGTH_LONG).show();
				}
			}
		});      

		// Initialize array adapter for all the supported Account Types.
		ArrayAdapter<String> myAcctTypeOptions = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		myAcctTypeOptions.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);

		// Find and set up the Spinner for supported Account Types.
		mAcctTypeSpinner = (Spinner) findViewById(R.id.AcctTypeSelector);
		mAcctTypeSpinner.setAdapter(myAcctTypeOptions);
		mAcctTypeSpinner.setOnItemSelectedListener(mAcctsTypeSpinnerClickListener);

		// Set my Account Types, perhaps in future make it easier to update.
		//Only 2 will be supported so far.
		Set<String> myAcctTypes = new HashSet<String>();
		myAcctTypes.add("gmail");
		myAcctTypes.add("ubmail");
		myAcctTypes.add("facebook");
		myAcctTypes.add("twitter");
		myAcctTypes.add("qq");
		myAcctTypesArray = myAcctTypes.toArray();

		// If there are Supported Types, add each one to the Array Adapter
		if (myAcctTypes.size() > 0) {
			for (String theType : myAcctTypes) {
				myAcctTypeOptions.add(theType);
			}
		}
		//Instantiate DB
		mDbHelperCAA = new SesameDbAdapter(this);
		mDbHelperCAA.open(SigninActivity.passkey);        
	} //End OnCreate()

	// The on-click listener for all the Supported Types in the Spinner
	//Main Purpose: Set Account Type Chosen
	private OnItemSelectedListener mAcctsTypeSpinnerClickListener = new OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> av, View v, int arg2, long arg3) {
			((TextView) av.getChildAt(0)).setTextColor(Color.WHITE);
			mChosenAcct = (String) myAcctTypesArray[arg2];
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// Do nothing, as the first element is already selected
			//At the beginning of the activity -DB
		}
	};

	//Used if you press Cancel Account.
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which){
			case DialogInterface.BUTTON_POSITIVE:
				//Yes button clicked
				mDbHelperCAA.close();
				finish();            	
				break;

			case DialogInterface.BUTTON_NEGATIVE:
				//No button clicked
				break;
			}
		}
	};

	//Make sure DB is closed if something weird happens -DB
	@Override
	protected void onDestroy() {
		mDbHelperCAA.close();
		super.onDestroy();
	}
}
