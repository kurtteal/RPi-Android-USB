package pt.rpi.android.usbtest;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class USBActivity extends Activity {
	private UsbAccessory mAccessory = null;
	
	private FileOutputStream mFout = null;
	private FileInputStream mFin = null;
	
	private String data;

	private static Object lock; //for the concurrent access to the counter
	
	private TextView rpiTextView;
	private Button mBtSend = null;
	
	private static AsyncTaskReader readerAsyncTask;
	public int counter=0;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);

        mBtSend = (Button)(findViewById(R.id.btSebd));
        mBtSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String s = ((EditText)findViewById(R.id.editText1)).getText().toString();
				queueWrite(s);
			}
		});
        LogUtil.LogD("USB", "Read called");
        rpiTextView = ((TextView) findViewById(R.id.rpi_text));
        read(0);
    }
    
    @Override
    protected void onDestroy() {
    	stopRead(0);
    	super.onDestroy();
    }
    
    //Custom made
    public void read(int j){
    	if(lock == null)
        	lock = new Object();

        IntentFilter i = new IntentFilter();
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED); //This is application only... not general broadcast
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        //i.addAction("pt.rpi.android.usbtest.USB"); //Custom action...deprecated now
        registerReceiver(mUsbReceiver,i);
        
        if(AccessoryMule.hasAccessory()){
        	ParcelFileDescriptor pfd = AccessoryMule.getPFD();
	    	//OPEN streams
	    	mFout = new FileOutputStream(pfd.getFileDescriptor());
	    	mFin = new FileInputStream(pfd.getFileDescriptor());
	        
	    	readerAsyncTask = new AsyncTaskReader();
	    	readerAsyncTask.execute("");

	    	showToast("read() through accessory mule "+j);
        }
    }
    
    public void stopRead(int i){
    	showToast("stopRead() " + i);
    	unregisterReceiver(mUsbReceiver);
    	if(readerAsyncTask != null){
	    	readerAsyncTask.cancel(true);
	    	readerAsyncTask = null;
    	}
    	try{
	    	if(mFout != null) mFout.close();
	    	if(mFin != null) mFin.close();
    	}catch(IOException e){
    		showToast("Error closing fds");
    	}
    }
    
    public void queueWrite(final String data){
    	if(mAccessory == null){
    		return;
    	}
    	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					LogUtil.LogD("USB", "Writing length "+data.length());
					mFout.write(new byte[]{(byte)data.length()});
					LogUtil.LogD("USB", "Writing data: "+data);
					mFout.write(data.getBytes());
					LogUtil.LogD("USB","Done writing");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
    }
    
    
    private class AsyncTaskReader extends AsyncTask<String, String, String> {

		private String resp;
		//The Android accessory protocol supports packet buffers up to 16384 bytes
		byte[] rpiInfo = new byte[16384]; 
		int numBytes = 0;
		
		@Override
		protected String doInBackground(String... params) {
			try {
//				numBytes = mFin.read(rpiInfo); //blocks reading from USB endpoint
//				resp = new String(rpiInfo, 0, numBytes - 1);
//				publishProgress(resp); // Calls onProgressUpdate()
				
				while(true){
//					runOnUiThread(new Runnable() {//After the first read, it launches a timeout thread for every read
//                        @Override
//                        public void run() {
//                        	setTimeout();
//                        }
//					});
					numBytes = mFin.read(rpiInfo);
//					synchronized(lock){
//			    		counter++; //The timertask will check this counter when it awakes
//			    	}
					
					resp = new String(rpiInfo, 0, numBytes - 1);
					publishProgress(resp); // Calls onProgressUpdate()
					if (isCancelled())
					    break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				resp = e.getMessage();
			}
			return resp;
		}
	    	  
		@Override
		protected void onProgressUpdate(String... values) { //This runs on UI thread
			data = values[0];
			updateUI();
		}
	}
    
    
//    //TimeoutTask: if the counter hasn't changed since this task was 'programmed', there was a timeout
//    //It means there was a problem with the USB connection, the java side isn't reading what was written
//    //by the C code side. We need to cancel the reader asynctask and launch a new one, and register a new
//    //broadcast receiver.
//    public class TimeoutTask extends TimerTask {
//		int startCounter;
//
//		TimeoutTask(int startCounter) {
//			this.startCounter = startCounter;
//		}
//
//		public void run() {
//			int currentCounter;
//			synchronized(lock){
//	    		currentCounter = counter;
//	    	}
//	    	if(startCounter == currentCounter){
//	    		runOnUiThread(new Runnable() {
//                  @Override
//                  public void run() {
//      	    		stopRead(startCounter);
//    	    		read(startCounter); 
//                  }
//              });
//	    	}
//		}
//	}
    
//    //Timeout of 2 secs, after which the timeoutTask runs
//    public void setTimeout(){
//    	Timer timer = new Timer();
//    	int currentCounter;
//    	synchronized(lock){
//    		currentCounter = counter;
//    	}
//    	TimeoutTask task = new TimeoutTask(currentCounter);
//        
//        //singleshot delay 2000 ms
//        timer.schedule(task, 2000);
//    }
    
    //Aux method to update UI given an update from the rpi
    private final void updateUI(){
    	rpiTextView.setText(rpiTextView.getText() + data + "\n");
    }
    
    private final void showToast(String msg){
    	Toast.makeText(this, msg,Toast.LENGTH_SHORT).show();
    }
    
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			/*
				ACTION_USB_DEVICE_ATTACHED can only be a intent filter for an Activity 
				and not a BroadCastReciever. Android Developer site does say its a broadcast, 
				but Android Source is only resolving activities. Though you can use 
				ACTION_USB_ACCESSORY_DETACHED for detach updates in receiver.
			*/
			if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				//UsbAccessory accessory = UsbManager.getAccessory(intent);
				UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					mAccessory = null;
					mBtSend.setEnabled(false);
					//How to stop accessory mode without physically resetting?
					finish();
				}
			}
		}
	};

}



