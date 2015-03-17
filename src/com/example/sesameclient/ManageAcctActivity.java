/**
 * This Activity appears as a dialog. It shows all your current set up
 * accounts, allows you to select an account to login into, and 
 * allows you set up new accounts, and remove and edit them.
 * 
 * This was created Denise C. Blady for the SEASAME project, known 
 * as DB in comments.
 * 
 * Source for dialog box code:
 *  http://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-in-android
 */

package com.example.sesameclient;

import group.pals.android.sesame.database.SesameDbAdapter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ManageAcctActivity extends Activity {
	// Debugging
	private static final String TAG = "ManageAcctActivity";
	private static final boolean D = true;
	public final static String Acc_Type = "ACCT_TYPE_CHOSEN";
	public static String Acct_Type = "chosen_accct";

	// Send Intent extra
	public static String EXTRA_CHOSEN_ACCT = "chosen_acct_id";

	// Intent request codes- Used by CreateAndEditActivity
	private static final int REQUEST_NEW_ACCT = 1;
	private static final int REQUEST_EDIT_ACCT = 2;
	protected static final int LINK_CONN = 999;

	// Member fields
	Set<String> myAccounts;
	private ArrayAdapter<String> mAccountsAdapter;
	TextView selectedAcct;
	private String noDevices;
	String acctName= ""; //account to edit/remove
	private String acctBeginningString= "Chosen Account: ";
	private SesameDbAdapter mDbHelperMAA;
	
	private AccountActivity mAAct;
	private Long chosenAcctID; //means not set
	private String acctType="";

	private String currentAcct = "";
	private String username = "";
	private String password = "";
	String acctTypeChosen = "";

	private SigninActivity mSAct;
	// Flag to see if an account was selected
	boolean selectedFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_manage_acct);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		Intent intent = getIntent();
		String message = intent.getStringExtra(AccountActivity.Account_Type);
		acctType=message;

		mSAct=new SigninActivity();
		mAAct=new AccountActivity();

		//Instantiate DB
		mDbHelperMAA = new SesameDbAdapter(this);
		mDbHelperMAA.open(SigninActivity.passkey);
		//fillData(); for later

		// Set current selected/picked Account
		try{
			Cursor currCursor= mDbHelperMAA.fetchCurrentAcct();
			acctName= currCursor.getString(2); //get's the pseudonym
			acctTypeChosen=currCursor.getString(5); // get's the acct type
			selectedFlag= true;
		} catch(Exception e){
			Toast.makeText(ManageAcctActivity.this, "Click on account to Edit/Remove !", Toast.LENGTH_SHORT).show();
			acctName = "";
		}
		//selectedAcct = (TextView)findViewById(R.id.textViewpickedAcct);
		//selectedAcct.setText(acctBeginningString + " " + acctName);
		//mAAct.currAct=acctName;

		// Initialize the button to add Accounts
		Button addButton = (Button) findViewById(R.id.button_addAcct);
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent newAccountIntent = new Intent(ManageAcctActivity.this,CreateMailAcctActivity.class);
				newAccountIntent.putExtra(Acct_Type,acctType );
				startActivityForResult(newAccountIntent, REQUEST_NEW_ACCT);
			}
		});


		// Initialize the button to finish the Activity
		Button doneButton = (Button) findViewById(R.id.button_doneAcct);
		doneButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				Intent returnIntent = new Intent();
				returnIntent.putExtra("result","ok");
				setResult(LINK_CONN,returnIntent);     
				finish();

			}
		});

		// Initialize array adapter for all the created accounts.
		mAccountsAdapter = new ArrayAdapter<String>(this, R.layout.acct_name); 

		// Find and set up the ListView for created accounts.
		ListView AccountsListView = (ListView) findViewById(R.id.current_Accounts_List);
		AccountsListView.setAdapter(mAccountsAdapter);
		AccountsListView.setOnItemClickListener(mAccountsClickListener);
		
		
		int noAcctsFlag=0;
		Cursor DBAccounts = mDbHelperMAA.fetchAllAccts();
		if(DBAccounts.getCount()==0)
			noAcctsFlag=1;
		String[] Accounts = { "ubmail", "gmail", "facebook", "twitter", "qq"};
		// Get a set of current Accounts
		for(int i=0;i<=4;i++){
			myAccounts = new HashSet<String>();
			Log.v(TAG, "Herererererere1"+Accounts[i]);
			Log.v(TAG, "Herererererere2"+Accounts[i]);
			Cursor allDBAccounts = mDbHelperMAA.fetchCurrAcct(Accounts[i]);
			Log.v(TAG, "Herererererere3"+Accounts[i]);
			if(allDBAccounts!=null){
				allDBAccounts.moveToFirst();
				while(allDBAccounts.isAfterLast()==false){
					myAccounts.add(allDBAccounts.getString(2));//2 is the pseudonym
					allDBAccounts.moveToNext();
				}
			}
			// If there are Accounts, add each one to the ArrayAdapter
			if (myAccounts.size() > 0) {
				mAccountsAdapter.add("====****    "+Accounts[i].toUpperCase()+"    ****====");
				findViewById(R.id.current_Accounts_List).setVisibility(View.VISIBLE);
				int position = mAccountsAdapter.getPosition("====****    "+Accounts[i].toUpperCase()+"    ****====");
				for (String theAcct : myAccounts) {
					mAccountsAdapter.insert(theAcct, position+1);
				}
			} /*else {
				//noDevices = getResources().getText(R.string.no_accts).toString();
				noAcctsFlag=1;
			}*/
		}
		
		if(noAcctsFlag==1){
			noDevices = "Add GMAIL/ubmail/FACEBOOK/TWITTER/QQ Accounts";
			mAccountsAdapter.add(noDevices);
			noAcctsFlag=0;
		}

	} //end onCreate()


	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_NEW_ACCT:
			// When CreateAcctsActivity returns with a newly created account
			if (resultCode == Activity.RESULT_OK) {
				String uncutString = data.getExtras()
						.getString(CreateMailAcctActivity.EXTRA_C_ACCT_NAME);
				String accttype = data.getExtras()
						.getString(CreateMailAcctActivity.EXTRA_C_ACCT_TYPE);
				String theNewAcctName= uncutString;
				int pos= mAccountsAdapter.getPosition("====****    "+accttype.toUpperCase()+"    ****====");
				mAccountsAdapter.insert(theNewAcctName,pos+1);
				//mAccountsAdapter.add(theNewAcctName);
				if(mAccountsAdapter.getPosition(noDevices)>=0){
					mAccountsAdapter.remove(noDevices);
				}
			}
			break;
		case REQUEST_EDIT_ACCT:
			// When EditAcctsActivity returns with information
			if (resultCode == Activity.RESULT_OK) {
				//Reset Adapters
				myAccounts.clear();
				mAccountsAdapter.clear();
				String[] Accounts = { "ubmail", "gmail", "facebook", "twitter", "qq" };
				// Get a set of current Accounts
				for(int i=0;i<=4;i++){
					//Read from DB the current accounts
					myAccounts = new HashSet<String>();
					Cursor DBAccountsAfterEdit = mDbHelperMAA.fetchCurrAcct(Accounts[i]);
					if(DBAccountsAfterEdit!=null){
						DBAccountsAfterEdit.moveToFirst();
						while(DBAccountsAfterEdit.isAfterLast()==false){
							myAccounts.add(DBAccountsAfterEdit.getString(2));//2 is the pseudonym
							DBAccountsAfterEdit.moveToNext();
						}
					}

					// If there are Accounts, add each one to the ArrayAdapter
					if (myAccounts.size() > 0) {
						findViewById(R.id.current_Accounts_List).setVisibility(View.VISIBLE);
						mAccountsAdapter.add("====****    "+Accounts[i].toUpperCase()+"    ****====");
						int position = mAccountsAdapter.getPosition("====****    "+Accounts[i].toUpperCase()+"    ****====");
						for (String theAcct : myAccounts) {
							mAccountsAdapter.insert(theAcct, position+1);
						}
					}/* else {
						noDevices = getResources().getText(R.string.no_accts).toString();
						mAccountsAdapter.add(noDevices);
					}*/
				}
				//Update the current selection
				// Set current selected/picked Account
				try{
					Cursor currCursor= mDbHelperMAA.fetchCurrentAcct();
					acctName= currCursor.getString(2); //get's the pseudonym
					selectedFlag= true;
				} catch(Exception e){
					Toast.makeText(ManageAcctActivity.this, "No account currently selected!", Toast.LENGTH_LONG).show();
					acctName = "";
				}
				//    selectedAcct = (TextView)findViewById(R.id.textViewpickedAcct);
				//  selectedAcct.setText(acctBeginningString + " " + acctName);
				break;
			}
			else if(resultCode == 999){
				if(selectedFlag){
					Log.v(TAG, "REMOVE ACCT HERE");
					AlertDialog.Builder builder = new AlertDialog.Builder(ManageAcctActivity.this);
					builder.setMessage("Are you sure?").setPositiveButton("Yes", removingDialogClickListener)
					.setNegativeButton("No", removingDialogClickListener).show();
					//Go to removingDialogClickListener to see results of the choice -DB
				} else { //alert user to select an account!
					Toast.makeText(ManageAcctActivity.this, "Please select an Account to remove first!", Toast.LENGTH_SHORT).show();
				}
				break;
			}
			break;
		}
	} //End onActivityResult()

	// The on-click listener for all devices in the ListViews
	//Important to Know: Only one DB acct should ever be set to 1! -DB
	private OnItemClickListener mAccountsClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			// Set pickedAcct to what was pressed!
			String info ="not set yet";
			info = ((TextView) v).getText().toString();
			acctName= info;
			
			//a subtle bug where account type e.g. facebook gets clicked. 
			if (acctName.contains("==")){
				Toast.makeText(ManageAcctActivity.this, "Please select account name to be edited, not the type of the account!", Toast.LENGTH_LONG).show();
				return;
			}
			
			checkCurrent(acctName);
			if(selectedFlag){
				boolean DBflagEditBtn= true;
				//Find acct in DB first!
				try{
					Cursor editCursor = mDbHelperMAA.fetchAcct(acctName);
					String dBId= editCursor.getString(0);
					Log.i(TAG + ": DB check",dBId);
					chosenAcctID = editCursor.getLong(0);
				} catch(Exception e){
					DBflagEditBtn =false; //not in DB
					Toast.makeText(ManageAcctActivity.this, "Couldn't Find Acct! Can't Edit!", Toast.LENGTH_LONG).show();
				}
				if(DBflagEditBtn){
					Intent editAccountIntent = new Intent(ManageAcctActivity.this,EditMailAcctActivity.class);
					editAccountIntent.putExtra(EXTRA_CHOSEN_ACCT, chosenAcctID);
					editAccountIntent.putExtra(Acct_Type,acctType );
					startActivityForResult(editAccountIntent, REQUEST_EDIT_ACCT);
				}
			}
		}
	};

	void checkCurrent(String acctNm){
		if(acctNm.equals(noDevices)){return;}


		Intent intent = new Intent();

		// Set result
		setResult(Activity.RESULT_OK, intent);

		selectedFlag = true;
	}
	//Used if you press remove account.
	DialogInterface.OnClickListener removingDialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which){
			case DialogInterface.BUTTON_POSITIVE:
				//Yes button clicked, so remove
				//Remove from file first.
				boolean removeAcct = false;
				try{
					Cursor toRemove = mDbHelperMAA.fetchAcct(acctName);
					Long toRemoveID= toRemove.getLong(0); //_id stored in col 0
					removeAcct= mDbHelperMAA.deleteAcct(toRemoveID);
				} catch(Exception e){
					Toast.makeText(ManageAcctActivity.this, "Failed to delete selected account.", Toast.LENGTH_LONG).show();
				}

				if(removeAcct){
					//Removed Successfully from DB so remove from view

					mAccountsAdapter.remove(acctName);

					selectedFlag= false;
					Log.i(TAG + ": Acct Deleted",acctName);
				} else {
					Toast.makeText(ManageAcctActivity.this, "Failed to delete selected account.", Toast.LENGTH_LONG).show();
				}

				break;
			case DialogInterface.BUTTON_NEGATIVE:
				//No button clicked
				break;
			}
		}
	};

	//Make sure DB is closed if something weird happens- prevents DB leaks -DB
	@Override
	protected void onStop() {
		//mDbHelperMAA.close();
		super.onStop();
	}
}