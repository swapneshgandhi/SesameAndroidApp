/**
 * This Activity appears as a dialog. It allows you to edit
 * your accounts including fields such as email address, password,
 * chosen name (what this application shows it as in the list), and 
 * account type options currently supported!
 * 
 * This was created Denise C. Blady for the SEASAME project, known 
 * as DB in comments.
 * 
 * Source for Spinner functionality help used as guidance:
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
import android.database.Cursor;
import android.database.SQLException;
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

public class EditMailAcctActivity extends Activity {
	// Debugging
	private static final String TAG = "EditAcctsActivity";
	private static final boolean D = true;

	// Received Intent extra
	public static String EXTRA_CHOSEN_ACCT = "chosen_acct_id";  
	public static String ACCT_TYPE = "chosen_accct";

	// Return Intent extras
	public static String EXTRA_E_ACCT_NAME = "acct_name";
	protected static final int LINK_CONN = 999;

	// Member fields
	private Spinner mAcctTypeSpinner;
	private EditText mEmailAddy;
	private EditText mPassword;
	private EditText mChosenName;
	private String theAcctName= "";
	private String mAccountTypePicked= "";
	private Object[] myAcctTypesArray;
	private String theEmailAddy= "";
	private String thePassword= "";
	private SesameDbAdapter mDbHelperEAA;
	private boolean DbOpen;
	private Long acctDbID;

	// Flag to see if all fields filled in before closing this activity
	boolean allFilledFlag = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_edit_mail_acct);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		TextView tv = (TextView) findViewById(R.id.emailAddressE);
		tv.setTextColor(Color.WHITE);
		TextView tv1 = (TextView) findViewById(R.id.passwordE);
		tv1.setTextColor(Color.WHITE);
		TextView tv2 = (TextView) findViewById(R.id.passwordConfirmE);
		tv2.setTextColor(Color.WHITE);
		TextView tv3 = (TextView) findViewById(R.id.chosen_Name_AcctE);
		tv3.setTextColor(Color.WHITE);
		TextView tv4 = (TextView) findViewById(R.id.accountTypeE);
		tv4.setTextColor(Color.WHITE);

		//Instantiate DB
		mDbHelperEAA = new SesameDbAdapter(this);
		DbOpen=mDbHelperEAA.open(SigninActivity.passkey);

		//Set my EditText member fields
		mEmailAddy= (EditText) findViewById(R.id.emailAddyBoxE);
		mPassword = (EditText) findViewById(R.id.passwordBoxE);
		mChosenName= (EditText) findViewById(R.id.chosenNameBoxE);
		//mAccountTypePicked = getIntent().getExtras().getString(ACCT_TYPE);
		//Log.v(TAG, mAccountTypePicked);



		// Initialize the button to cancel the Activity
		Button cancelButton = (Button) findViewById(R.id.button_cancelAcctE);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(EditMailAcctActivity.this);
				builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).show();
				//Go to dialogClickListener to see results of the choice -DB
			}
		});

		// Initialize the button to remove Accounts
		Button removeButton = (Button) findViewById(R.id.button_deleteAcctE);
		removeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {   
				Intent returnIntent = new Intent();
				// Set result
				setResult(LINK_CONN, returnIntent);
				finish();
			}
		});

		// Initialize the button to finish the Activity
		Button doneButton = (Button) findViewById(R.id.button_saveAcctE);
		doneButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				boolean flag1= true;
				boolean flag2= true;
				boolean flag3= true;
				boolean flag4 =true;
				//Grab all my values(account type already covered in spinner listener)
				theEmailAddy= mEmailAddy.getText().toString();
				thePassword= mPassword.getText().toString();
				theAcctName= mChosenName.getText().toString();
				String debugResult= theEmailAddy + " " +thePassword + " " +
						theAcctName + " " + mAccountTypePicked;

				Log.i(TAG, debugResult);

				//Check fields
				if(mAccountTypePicked.length()==0){flag1=false;}
				if(theEmailAddy.length()==0){flag2=false;}
				if(thePassword.length()==0){flag3=false;}
				if(theAcctName.length()==0){flag4=false;}

				//Check if all fields filled
				if(flag1 && flag2 && flag3 && flag4){allFilledFlag= true;}

				boolean testDBResult = false;
				if(allFilledFlag){
					try{
						//save results to the Accounts, useFlag==1 since already selected -DB
						testDBResult = mDbHelperEAA.updateAcct(acctDbID, 1, theAcctName, theEmailAddy, thePassword, mAccountTypePicked); 
					} catch (Exception e){
						//If not unique, a SQL exception is thrown on update! -DB
						testDBResult= false; //not a unique pseudonym.
					}
					if(testDBResult==true){ //DB updated properly
						// Create the result Intent
						Intent intent = new Intent();
						// Set result
						setResult(Activity.RESULT_OK, intent);
					} else {
						Toast.makeText(EditMailAcctActivity.this, "Account Editing Failed. Please Try Again, make sure chosen name is unique.", Toast.LENGTH_LONG).show();
					}
					mDbHelperEAA.close();
					finish();
				} else { //alert user to fill all fields!
					Toast.makeText(EditMailAcctActivity.this, "Please fill in all fields first!", Toast.LENGTH_LONG).show();
				}
			}
		});      

		// Initialize array adapter for all the supported Account Types.
		ArrayAdapter<String> myAcctTypeOptions = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		myAcctTypeOptions.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);

		// Find and set up the Spinner for supported Account Types.
		mAcctTypeSpinner = (Spinner) findViewById(R.id.AcctTypeSelectorE);
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

		//Fill in acct data from file to edit
		acctDbID= getIntent().getExtras().getLong(EXTRA_CHOSEN_ACCT);
		try{
			Cursor acctCursor = mDbHelperEAA.fetchAcct(acctDbID);
			mEmailAddy.setText(acctCursor.getString(3));
			mPassword.setText(acctCursor.getString(4));
			mChosenName.setText(acctCursor.getString(2));
			String spinPosition = acctCursor.getString(5);
			if(spinPosition.equals("gmail")){mAcctTypeSpinner.setSelection(0);}
			else if(spinPosition.equals("ubmail")){mAcctTypeSpinner.setSelection(4);}
			else if(spinPosition.equals("facebook")){mAcctTypeSpinner.setSelection(3);}
			else if(spinPosition.equals("twitter")){mAcctTypeSpinner.setSelection(1);}
			else if(spinPosition.equals("qq")){mAcctTypeSpinner.setSelection(2);}

		} catch (SQLException e){
			Toast.makeText(EditMailAcctActivity.this, "Can't find the Account.", Toast.LENGTH_LONG).show();
		}

	} //End OnCreate()

	// The on-click listener for all the Supported Types in the Spinner
	//Main Purpose: Set Account Type Chosen
	private OnItemSelectedListener mAcctsTypeSpinnerClickListener = new OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> av, View v, int arg2, long arg3) {
			((TextView) av.getChildAt(0)).setTextColor(Color.WHITE);
			mAccountTypePicked = (String) myAcctTypesArray[arg2];
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// Do nothing, as the first element is already selected
			//At the beginning of the activity
		}
	};
	//Used if you press Cancel Account.
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which){
			case DialogInterface.BUTTON_POSITIVE:
				//Yes button clicked
				mDbHelperEAA.close();
				finish();
				break;

			case DialogInterface.BUTTON_NEGATIVE:
				//No button clicked
				finish();
				break;
			}
		}
	}; 


	//Make sure DB is closed if something weird happens- prevents DB leaks -DB
	@Override
	protected void onStop() {
		//mDbHelperEAA.close();
		super.onStop();
	}


}