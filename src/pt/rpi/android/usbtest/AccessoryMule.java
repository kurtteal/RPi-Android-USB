package pt.rpi.android.usbtest;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;

public class AccessoryMule {

	//Accessible everywhere
	private static UsbAccessory accessory;
	private static UsbManager usbManager;
	//If we hold a reference to the PFD there is no hang in USB comms!
	//Otherwise the GC seems to be recycling too soon...
	private static ParcelFileDescriptor parcelFD;
	
	public static boolean hasAccessory(){ 
		if(accessory != null)
			return true;
		return false;
	}
	
	public static void setAccessory(UsbAccessory ac){ accessory = ac; }
	public static void setUsbManager(UsbManager um){ usbManager = um; }
	public static void setPFD(ParcelFileDescriptor pfd){ parcelFD = pfd; }
	
	public static UsbAccessory getAccessory(){ return accessory; }
	public static UsbManager getUsbManager(){ return usbManager; }
	public static ParcelFileDescriptor getPFD(){ return parcelFD; }
}
