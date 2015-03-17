package com.example.sesameclient;



import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Window;

public class SetupAccountsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_manage_acct);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_setup_accounts, menu);
		return true;
	}

}
