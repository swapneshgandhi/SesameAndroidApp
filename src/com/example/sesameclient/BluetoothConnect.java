/*
 * The purpose of this class is to separate the core functionality of the client,
 * and the App/GUI that is used to control the client. As the name suggests, this
 * class services BluetoothLogin. BluetoothLogin instantiates this class and calls
 * its methods, as needed. The real functionality of the client is all stored here
 * including but not limited to:
 *    -Setting up connections
 *    -Handling connected threads
 *    -Manage the current state of the connection and alert the application
 *    -Providing the main functionality for the client.
 * 
 * This class was created by Denise Blady, known as DB in comments,
 * for the SEASAME project. Inspiration for this class's design came from
 * the Android Open Source Project 'BluetoothChat', specifically the client end.
 * It can be freely accessed by anyone with Android installed on their system
 * by looking through the sample projects. */

package com.example.sesameclient;

import group.pals.android.sesame.database.SesameDbAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.google.common.base.Splitter;
import com.google.common.primitives.Bytes;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

@SuppressLint("NewApi")
public class BluetoothConnect {
	// Debugging
	private static final String TAG = "BluetoothCOnnect";
	private static final boolean D = true;

	// Unique UUID for this application
	private static final UUID MY_UUID = UUID.fromString("191a3720-61cb-11e2-bcfd-0800200c9a66");


	// Member fields
	private final BluetoothAdapter mAdapter;
	private static ConnectThread mConnectThread;
	private static ConnectedThread mConnectedThread;
	private int mState;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	// Constants that indicate command to computer
	public static final int PING_SERVER = 3;
	public static final int LOGIN_BT = 1;
	public static final int EXIT_CMD = -1;
	
	private int FILE_SIZE=10000000;

	public BluetoothSocket tempSock;
	/**
	 * Constructor. Prepares a new BluetoothLogin session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 */
	public BluetoothConnect(Context context) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
	}

	/**
	 * Set the current state of the login connection
	 * @param state  An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D) Log.e(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		//mHandler.obtainMessage(AccountActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state. */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Setup the Service by clearing out old threads. */
	public synchronized static void start() {
		if (D) Log.e(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * @param device  The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D) Log.e(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (D) Log.e(TAG, "connected");

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		setState(STATE_CONNECTED);
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D) Log.e(TAG, "stop");
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * @param out The bytes to write
	 * @throws IOException 
	 * @see ConnectedThread#write(byte[])
	 */

	public void writeByte(byte[] out) throws IOException {
		// Create temporary object
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			// if (mState != STATE_CONNECTED) return;
		}
		ConnectedThread r;
		r=mConnectedThread;
		r.write(out);

	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_NONE);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		// Send a failure message back to the Activity
		setState(STATE_NONE);
	}

	/**
	 * This thread runs while attempting to make an outgoing connection
	 * with a device. It runs straight through; the connection either
	 * succeeds or fails.
	 */
	class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.e(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				Log.e(TAG, "Getting COnnected");
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				// Close the socket
				Log.e(TAG, "Exception "+e);
				try {
					Log.e("WHHYYY","trying to close socket, making connection failed.");
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}
				// Kill old threads that didn't work, essentially resetting the service -DB
				BluetoothConnect.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothConnect.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	} //End of ConnectThread

	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	public class ConnectedThread extends Thread {
		//private final BluetoothSocket mmSocket; //connected socket
		public BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private LeashTimerTask leashTask;
		private Timer leashTimer;

		public ConnectedThread(BluetoothSocket socket) {
			Log.e(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;


			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;

			// Set up timer to fire off automatic ping messages to server
			leashTask = new LeashTimerTask();
			leashTimer = new Timer(); //timer not scheduled yet!
		}

		public void run() {
			Log.e(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			//Start timer task- after 3secs do initial ping
			// and then continue pinging on a period of 5secs
			leashTimer.schedule(leashTask, 3000, 5000);

			// Keep listening to the InputStream while connected
			while (true) { //SHOULD NEVER GET A MSG. -DB
				try {
					// Read from the InputStream
					Log.e("server","running");
					//read data from server 
					mmInStream.read(buffer);
					
					String message=new String (buffer,"ISO-8859-1");
					
					String [] spilt= message.split(" ",2);
					//If message is restore data- then receive the database data.
					if (message.indexOf(String.valueOf(CreateMailAcctActivity.RESTORE))==0){
						ReceiveDBFile(Integer.parseInt(spilt[1].trim()));			
					}
					
					//If server sends backup ok - then accept it and notify the thread.
					else if (message.indexOf(String.valueOf(CreateMailAcctActivity.BACKUP_OK))==0){
						
						synchronized (AccountActivity.BackupLock) {
							AccountActivity.BackupDb=true;
							AccountActivity.BackupLock.notify();
						}			
					}
					
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					cancel(); //ONLY NEEDED HERE! if thread dying need to kill timer -DB
					break;
				}
			} //End While Loop
		}

		public void ReceiveDBFile(int total){

			int bytesRead;
			int current = 0;
			FileOutputStream fos = null;
			BufferedOutputStream bos = null;
			try {
				System.out.println("Connecting...");
				
				// receive file
				byte [] mybytearray  = new byte [FILE_SIZE];
				
				Charset ISO = Charset.forName("ISO-8859-1");

				fos = new FileOutputStream(AccountActivity.dbPath);
				bos = new BufferedOutputStream(fos);
				//read data from instream upto database length we got from server earlier
				do {
					bytesRead =
							mmInStream.read(mybytearray, current, (total-current));
		
					if(bytesRead >= 0) current += bytesRead;

				} while(current < total);

				bos.write(mybytearray, 0 , current);
				bos.flush();
				Log.e("File ", SesameDbAdapter.DATABASE_NAME
						+ " downloaded (" + current + " bytes read)");
				//let account activity know things went well by trueing this boolean.
				AccountActivity.RestoreDB=true;
			}
			catch(Exception e){
						Log.e("FileReceive",e.getLocalizedMessage());
			}
			finally {
				try {
					fos.close();
					bos.close();
					synchronized (AccountActivity.RestoreLock) {
						AccountActivity.RestoreLock.notify();
					}			
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * @param buffer  The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				Log.e(TAG, "IM SENDINGG");
				String fileString = new String(buffer,"ISO-8859-1");
				Log.e(TAG, fileString);
				mmOutStream.write(buffer);
				Log.e(TAG, "SENTTT");
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		/**Leash TimerTask defined- tell Timer what to do when time==0
		 * Usable only by this ConnectedThread only.*/
		private class LeashTimerTask extends TimerTask {
			public void run() {
				/*final String leashMsg = PING_SERVER + " ping";
				byte[] leashSend = leashMsg.getBytes();
				write(leashSend);*/
			}
		}

		/** Cancels the connected thread and the timer associated with it.*/
		public void cancel() {
			Log.e("KILLThD","killing thread & timer");
			try {
				mmSocket.close();
				//stop timer events
				leashTimer.cancel();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	} //end ConnectedThread class

}