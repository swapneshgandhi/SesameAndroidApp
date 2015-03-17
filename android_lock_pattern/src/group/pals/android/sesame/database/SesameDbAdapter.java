package group.pals.android.sesame.database;

/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.util.Log;

/**
 * SEASAME accounts database access helper class. Defines the basic CRUD operations
 * implemented for SEASAME, and gives the ability to list all accounts as well as
 * retrieve or modify a specific account. Additionally I have added the ability to 
 * retrieve the active account. Modified from the mentioned source below by
 * Denise C. Blady, known as -DB in comments.
 * 
 * Original Source Notes:
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 * 
 * Source: http://developer.android.com/training/notepad/notepad-ex1.html
 * 
 * Format for SEASAME Accounts in Database:
 *  ID useFlag pseudonym email pswd type
 *  
 *  Definitions: 
 *  	ID  (id of row in database)
 *  	useFlag ( 1 == used, 0 == not used)
 *  	pseudonym ( fake name or shortcut name for account)
 *  	type ( type of acct aka gmail or yahoo)
 *  	
 */
public class SesameDbAdapter {

	public static final String KEY_EMAIL = "email";
	public static final String KEY_PSWD = "pswd";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_USEFLAG = "useFlag";
	public static final String KEY_PSEUDONYM = "pseudonym";
	public static final String KEY_TYPEACCT = "type";
	public static final String MODE_PRIVATE = "private";
	private static final String TAG = "SeasameDbAdapter";
	public static final String KEY_PATTERN = "_pattern";
	private DatabaseHelper mDbHelper;
	private static SQLiteDatabase mDb;
	private static 	String dbPath ;
	/**
	 * Database creation SQL statement
	 */
	private static final String DATABASE_CREATE =
			"create table if not exists accts (_id integer primary key autoincrement, "
					+ "useFlag integer, pseudonym text not null unique, "
					+ "email text not null, pswd text not null, type text not null);";

	private static final String PATTERN_TABLE_CREATE =
			"create table if not exists pattern_tab (_id integer primary key autoincrement, useFlag integer, _pattern text not null);";

	public static final String DATABASE_NAME = "sesame";
	private static final String DATABASE_TABLE = "accts";
	private static final String PATTERN_TABLE = "pattern_tab";
	private static final int DATABASE_VERSION = 10;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
			db.execSQL(PATTERN_TABLE_CREATE);
			Log.e("Db","Created");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS accts");
			onCreate(db);
		}
	}

	public boolean ComparePattern(String passkey){
		
		if (mDbHelper == null ){ 
			mDbHelper = new DatabaseHelper(mCtx);
			}

		if(mDb !=null && mDb.isOpen()){
			Log.e("compareDb","connection is already open");
			return true;
		}

		try{//try writing to database if works out fine then
			//return true false otherwise
			Log.e("comparePatternDb",String.valueOf(passkey));
			mDb=mDbHelper.getWritableDatabase(passkey);
			mDbHelper.close();
			return true;
		}
		catch(Exception e){

			Log.e("comparePatternDb",e.toString());
			return false;	
		}

	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx the Context within which to work
	 */
	public SesameDbAdapter(Context ctx) {
		this.mCtx = ctx;
		dbPath = ctx.getDatabasePath(SesameDbAdapter.DATABASE_NAME).getPath();
	}

	public void getExistDataBase(String passKey){
		mDb=getExistDataBaseFile(passKey);
	}

	//// this function to open an Exist database- used while restoring a database file
	private SQLiteDatabase getExistDataBaseFile(String passKey) {

		SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
			public void preKey(SQLiteDatabase database) {
			}

			public void postKey(SQLiteDatabase database) {
				database.rawExecSQL("PRAGMA cipher_migrate;");

			}
		};
		return SQLiteDatabase.openOrCreateDatabase(dbPath, passKey,
				null, hook);
	}

	//might be useful func in 
	//case we wan to restore db at a different path
	public void getExistDataBase2(String passKey, String dbpath2){
		mDb=getExistDataBaseFile2(passKey,dbpath2);
	}


	private SQLiteDatabase getExistDataBaseFile2(String passKey, String dbPath2) {// this function to open an Exist database

		SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
			public void preKey(SQLiteDatabase database) {
			}

			public void postKey(SQLiteDatabase database) {
				database.rawExecSQL("PRAGMA cipher_migrate;");

			}
		};
		return SQLiteDatabase.openOrCreateDatabase(dbPath2, passKey,
				null, hook);
	}



	/**
	 * Open the accts database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	
	//updated open function to take passkey as the parameter, which will return true 
	//on opening db successfully 
	public boolean open(String passKey) throws SQLException {

		Log.e("OpenDb passkey :",passKey);

		if (mDbHelper == null ){ 
		mDbHelper = new DatabaseHelper(mCtx);
		}
		
		if(mDb !=null && mDb.isOpen()){
			Log.e("compareDb","connection is already open");
			return true;
		}

		if(passKey.length()>0){
			try{				
				mDb = mDbHelper.getWritableDatabase(passKey);
				Log.e("OpenDb","returning true");
				return true;
			}
			catch(Exception e){
				Log.e("OPEN","Exception");
			}
		}
		Log.e("OpenDb","returning FALSE");
		return false;
	}

	public void close() {
		mDbHelper.close();
	}


	/**
	 * Create a new acct using the useFlag, pseudonym, email, pswd, and type provided.
	 *  If the acct is successfully created return the new rowId for that acct,
	 *   otherwise return a -1 to indicate failure.
	 * 
	 * @param useFlag the flag that indicates current use 0- no 1- yes
	 * @param pseudo the fake name for the acct
	 * @param email the email acct
	 * @param pswd the pswd
	 * @param type the type of account (yahoo, gmail)
	 * @return rowId or -1 if failed
	 */
	public long createAcct(Integer useFlag, String pseudo, String email, String pswd, String type) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_USEFLAG, useFlag);
		initialValues.put(KEY_PSEUDONYM, pseudo);
		initialValues.put(KEY_EMAIL, email);
		initialValues.put(KEY_PSWD, pswd);
		initialValues.put(KEY_TYPEACCT, type);
		Log.v(TAG, "Inserted into DB");
		//I want the value below null so no columns get inserted! -DB
		return mDb.insert(DATABASE_TABLE, null, initialValues);

	}

	public long storePwd(Integer useFlag, String pwd) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_USEFLAG, useFlag);
		initialValues.put(KEY_PATTERN, pwd);
		Log.v(TAG, "Inserted into DB");
		//I want the value below null so no columns get inserted! -DB
		return mDb.insert(PATTERN_TABLE, null, initialValues);
	}

	/**
	 * Delete the acct with the given rowId
	 * 
	 * @param rowId id of acct to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteAcct(long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all accts in the database
	 * 
	 * @return Cursor over all accts
	 */
	public Cursor fetchAllAccts() {
		//nulls mean in order: selection, selectionArgs, groupBy,having, and orderBy
		return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_USEFLAG, 
				KEY_PSEUDONYM, KEY_EMAIL, KEY_PSWD, KEY_TYPEACCT}, 
				null, null, null, null, null);
	}

	/**
	 * Return a Cursor positioned at the acct that matches the given rowId
	 * 
	 * @param rowId id of acct to retrieve
	 * @return Cursor positioned to matching acct, if found
	 * @throws SQLException if acct could not be found/retrieved
	 */
	public Cursor fetchAcct(long rowId) throws SQLException {

		Cursor mCursor =

				mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_USEFLAG, 
						KEY_PSEUDONYM, KEY_EMAIL, KEY_PSWD, KEY_TYPEACCT}, KEY_ROWID + "=" + rowId,
						null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * Return a Cursor positioned at the acct that matches the given pseudonym
	 * 
	 * @param pseudo pseudonym of acct to retrieve
	 * @return Cursor positioned to matching acct, if found
	 * @throws SQLException if acct could not be found/retrieved
	 */
	public Cursor fetchAcct(String pseudo) throws SQLException {
		Log.i("DB Debug", pseudo);
		String selectQuery = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_PSEUDONYM + "=?";
		Cursor mCursor = mDb.rawQuery(selectQuery, new String[] {pseudo});

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * Return a Cursor positioned at the acct that matches the given pseudonym
	 * 
	 * @param pseudo pseudonym of acct to retrieve
	 * @return Cursor positioned to matching acct, if found
	 * @throws SQLException if acct could not be found/retrieved
	 */
	public Cursor fetchCurrAcct(String acct_type) throws SQLException {
		Log.i("DB Debug", acct_type);
		String selectQuery = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_TYPEACCT + "=?";
		Cursor mCursor = mDb.rawQuery(selectQuery, new String[] {acct_type});

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	public Cursor fetchPwd(String flag) throws SQLException, NullPointerException{

		if (mDb==null){

		}

		String selectQuery = "SELECT " +KEY_PATTERN+" FROM " + PATTERN_TABLE + " WHERE " + KEY_USEFLAG + "=?";
		Cursor mCursor = mDb.rawQuery(selectQuery, new String[] {flag});

		if (mCursor.moveToFirst()) {
			Log.v(TAG, "cursor::::"+mCursor.getString(0));
		}
		return mCursor;

	}

	/**
	 * Getting all labels
	 * returns list of labels
	 * */
	public List<String> getSpinnerAccounts(String acct_type){
		List<String> labels = new ArrayList<String>();
		SQLiteDatabase mDb1;
		String selectQuery = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_TYPEACCT + "=?";

		if (mDb==null){
			Log.e("getSpinnerAccounts","mDb is null");	
		}

		Cursor cursor = mDb.rawQuery(selectQuery, new String[] {acct_type});
		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Log.v(TAG, "RETEREIVED ACCOUNT::"+cursor.getString(0)+cursor.getString(1)+cursor.getString(2));
				labels.add(cursor.getString(2));
			} while (cursor.moveToNext());
		}
		// closing connection
		cursor.close();
		//mDb.close();
		// returning lables
		return labels;
	}

	/**
	 * Return a Cursor positioned at the acct that is currently active
	 * aka USEFLAG==1, at most one has this quality!
	 *
	 * @return Cursor positioned to matching acct, if found
	 * @throws SQLException if acct could not be found/retrieved
	 */
	public Cursor fetchCurrentAcct() throws SQLException {
		Log.v(TAG, "CHECKSSS 11");
		Integer current = 1; //value if current acct
		//Log.v(TAG, "CHECKSSS 11");
		Cursor mCursor =

				mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_USEFLAG, 
						KEY_PSEUDONYM, KEY_EMAIL, KEY_PSWD, KEY_TYPEACCT}, KEY_USEFLAG + "=" + current,
						null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		Log.v(TAG,"CHECKSS 22");
		return mCursor;

	}

	public void deleteRow() 
	{
		mDb.execSQL("delete from "+PATTERN_TABLE);
	}

	/**
	 * Update the acct using the details provided. The acct to be updated is
	 * specified using the rowId, and it is altered to use 
	 * values passed in
	 * 
	 * @param rowId id of acct to update
	 * @param useFlag the flag that indicates current use 0- no 1- yes
	 * @param pseudo the fake name for the acct
	 * @param email the email acct
	 * @param pswd the pswd
	 * @param type the account type (yahoo/gmail)
	 * @return true if the acct was successfully updated, false otherwise
	 */
	public boolean updateAcct(long rowId, Integer useFlag, String pseudo, String email, String pswd, String type) {
		ContentValues args = new ContentValues();
		args.put(KEY_USEFLAG, useFlag);
		args.put(KEY_PSEUDONYM, pseudo);
		args.put(KEY_EMAIL, email);
		args.put(KEY_PSWD, pswd);
		args.put(KEY_TYPEACCT, type);

		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}

