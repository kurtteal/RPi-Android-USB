package pt.rpi.android.usbtest;

import java.io.FileDescriptor;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Toast;

public class UsbTest extends Activity {
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        LogUtil.LogD("USB", "App started");
        getAccessory();
    }
    
    @Override
    protected void onDestroy() {
    	showToast("Main app destroyed");
    	super.onDestroy();
    }
    
    
    public void getAccessory(){   
        //The launcher was used (the app must be started by the usb attach event)
        if(!AccessoryMule.hasAccessory() && getIntent().getAction().equals("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")){
        	LogUtil.LogD("USB","Action is usb");
        	//Obtains accessory info
        	UsbAccessory accessory = (UsbAccessory) getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        	UsbManager um = null;
        	ParcelFileDescriptor pfd = null;
        	try{
        		um = (UsbManager) getSystemService(Context.USB_SERVICE);
        		//OPEN accessory
            	pfd = um.openAccessory(accessory);
        	}catch(IllegalArgumentException e){
        		finish();
        	}catch(NullPointerException e){
        		finish();
        	}
        	AccessoryMule.setAccessory(accessory);
        	AccessoryMule.setUsbManager(um);
        	AccessoryMule.setPFD(pfd);
        	LogUtil.LogD("USB", "Accessory obtained");
        	showToast("Accessory obtained");
        }
    }
    
    
	public void next(View v) {
		Intent intent = new Intent(UsbTest.this, USBActivity.class);
		startActivity(intent);		
	}
    
    
   
    private final void showToast(String msg){
    	Toast.makeText(this, msg,Toast.LENGTH_SHORT).show();
    }
    
}