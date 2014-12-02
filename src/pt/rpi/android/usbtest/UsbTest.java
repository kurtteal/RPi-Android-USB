package pt.rpi.android.usbtest;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Timer;
import java.util.TimerTask;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UsbTest extends Activity {
	private UsbAccessory mAccessory = null;
	
	private FileOutputStream mFout = null;
	private FileInputStream mFin = null;
	private PendingIntent mPermissionIntent = null;
	
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
        setContentView(R.layout.main);

        mBtSend = (Button)(findViewById(R.id.btSebd));
        mBtSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String s = ((EditText)findViewById(R.id.editText1)).getText().toString();
				queueWrite(s);
			}
		});
        Log.d("USB", "Read called");
        rpiTextView = ((TextView) findViewById(R.id.rpi_text));
        //writeToSDCard("Yeeehaw");
        read(0);
    }
    
    @Override
    protected void onDestroy() {
    	stopRead(0);
    	super.onDestroy();
    }
    
    public void queueWrite(final String data){
    	if(mAccessory == null){
    		return;
    	}
    	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d("USB", "Writing length "+data.length());
					mFout.write(new byte[]{(byte)data.length()});
					Log.d("USB", "Writing data: "+data);
					mFout.write(data.getBytes());
					Log.d("USB","Done writing");
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
			//String readySignal = "a";
			try {
				numBytes = mFin.read(rpiInfo); //blocks reading from USB endpoint
				resp = new String(rpiInfo, 0, numBytes - 1);
				publishProgress(resp); // Calls onProgressUpdate()
				
				while(true){
					runOnUiThread(new Runnable() {//After the first read, it launches a timeout thread for every read
                        @Override
                        public void run() {
                        	setTimeout();
                        }
					});
					numBytes = mFin.read(rpiInfo);
					synchronized(lock){
			    		counter++; //The timertask will check this counter when it awakes
			    	}
					
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

    public void stopRead(int i){
    	showToast("stopRead() " + i);
    	unregisterReceiver(mUsbReceiver);
    	if(readerAsyncTask != null){
	    	readerAsyncTask.cancel(true);
	    	readerAsyncTask = null;
    	}
    }
    
    //Custom made
    public void read(int j){
    	showToast("read() "+j);
    	if(lock == null)
        	lock = new Object();

        IntentFilter i = new IntentFilter();
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        i.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        i.addAction("ch.serverbox.android.usbtest.USBPERMISSION");
        registerReceiver(mUsbReceiver,i);
        
        if(getIntent().getAction().equals("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")){
        	Log.d("USB","Action is usb");
        	//UsbAccessory accessory = UsbManager.getAccessory(getIntent());
        	UsbAccessory accessory = (UsbAccessory) getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        	mAccessory = accessory;
        	FileDescriptor fd = null;
        	try{
        		//fd = UsbManager.getInstance(this).openAccessory(accessory).getFileDescriptor();
        		fd = ((UsbManager) getSystemService(Context.USB_SERVICE)).openAccessory(accessory).getFileDescriptor();
        	}catch(IllegalArgumentException e){
        		finish();
        	}catch(NullPointerException e){
        		finish();
        	}
        	mFout = new FileOutputStream(fd);
        	mFin = new FileInputStream(fd);
        }else{
        	//If action is permission
        	//UsbAccessory[] accessories = UsbManager.getInstance(this).getAccessoryList();
        	UsbAccessory[] accessories = ((UsbManager) getSystemService(Context.USB_SERVICE)).getAccessoryList();
        	for(UsbAccessory a : accessories){
        		l("accessory: "+a.getManufacturer());
        		if(a.getManufacturer().equals("DebugWithPrintf")){
        			mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("ch.serverbox.android.usbtest.USBPERMISSION"),0);
        			//UsbManager.getInstance(this).requestPermission(a,mPermissionIntent);
        			((UsbManager) getSystemService(Context.USB_SERVICE)).requestPermission(a,mPermissionIntent);
        			Log.d("USB", "permission requested");
        			break;
        		}
        	}
        }
    	//final Context context = this;
    	if(mAccessory == null){
    		return;
    	}
    	readerAsyncTask = new AsyncTaskReader();
    	readerAsyncTask.execute("");
    }
    
    //TimeoutTask: if the counter hasnt changed since this task was 'programmed', there was a timeout
    //It means there was a problem with the USB connection, the java side isn't reading what was written
    //by the C code side. We need to cancel the reader asynctask and launch a new one, and register a new
    //broadcast receiver.
    public class TimeoutTask extends TimerTask {
		int startCounter;

		TimeoutTask(int startCounter) {
			this.startCounter = startCounter;
		}

		public void run() {
			int currentCounter;
			synchronized(lock){
	    		currentCounter = counter;
	    	}
	    	if(startCounter == currentCounter){
	    		runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
      	    		stopRead(startCounter);
    	    		read(startCounter); 
                  }
              });
	    	}
		}
	}
    
    //Timeout de 2 secs, ao fim do qual corre a timeoutTask
    public void setTimeout(){
    	Timer timer = new Timer();
    	int currentCounter;
    	synchronized(lock){
    		currentCounter = counter;
    	}
    	TimeoutTask task = new TimeoutTask(currentCounter);
        
        //singleshot delay 2000 ms
        timer.schedule(task, 2000);
    }
    
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
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
					//UsbAccessory accessory = UsbManager.getAccessory(intent);
					UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					Log.d("USB","Attached!");
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						//openAccessory(accessory);
						mAccessory = accessory;
			        	FileDescriptor fd = null;
			        	try{
			        		//fd = UsbManager.getInstance(getApplicationContext()).openAccessory(accessory).getFileDescriptor();
			        		fd = ((UsbManager) getSystemService(Context.USB_SERVICE)).openAccessory(accessory).getFileDescriptor();
			        	}catch(IllegalArgumentException e){
			        		finish();
			        	}catch(NullPointerException e){
			        		finish();
			        	}
			        	mFout = new FileOutputStream(fd);
			        	mFin = new FileInputStream(fd);
			        	mBtSend.setEnabled(true);
					} else {
						Log.d("USB", "permission denied for accessory "
								+ accessory);
					}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				//UsbAccessory accessory = UsbManager.getAccessory(intent);
				UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					//if(mFout != null)
					if(mFin != null)
						try {
							mFout.close();
							mFin.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					mAccessory = null;
					mBtSend.setEnabled(false);
					//How to stop accessory mode without physically resetting?
					onDestroy();
				}
			}else if("ch.serverbox.android.usbtest.USBPERMISSION".equals(action)){
				l("permission answered");
				if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
		        	//UsbAccessory[] accessories = UsbManager.getInstance(getApplicationContext()).getAccessoryList();
		        	UsbAccessory[] accessories = ((UsbManager) getSystemService(Context.USB_SERVICE)).getAccessoryList();
		        	for(UsbAccessory a : accessories){
		        		l("accessory: "+a.getManufacturer());
		        		if(a.getManufacturer().equals("Nexus-Computing GmbH")){
		        			mAccessory = a;
		                	FileDescriptor fd = null;
		                	try{
		                		//fd = UsbManager.getInstance(getApplicationContext()).openAccessory(a).getFileDescriptor();
		                		fd = ((UsbManager) getSystemService(Context.USB_SERVICE)).openAccessory(a).getFileDescriptor();
		                	}catch(IllegalArgumentException e){
		                		finish();
		                	}catch(NullPointerException e){
		                		finish();
		                	}
		                	mFout = new FileOutputStream(fd);
		                	mFin = new FileInputStream(fd);
		        			l("added accessory");
		        			break;
		        		}
		        	}
				}
			}
		}
	};
	
	private void l(String l){
		Log.d("USB", l);
	}
	
	private void writeToSDCard(String dump){
		try {
			File myFile = new File(Environment.getExternalStorageDirectory().getPath() + "/mysdfile.txt");
			myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = 
									new OutputStreamWriter(fOut);
			//myOutWriter.append(e.getStackTrace().toString());
			myOutWriter.append(dump);
			myOutWriter.close();
			fOut.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}