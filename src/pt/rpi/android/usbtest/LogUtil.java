package pt.rpi.android.usbtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;

public class LogUtil {
	//Logs written to the DCIM storage
	protected static void LogD(String tag, String entry){
		String fileName = "/myLog.txt";
		
		Calendar cal = Calendar.getInstance(); 
		int millisecond = cal.get(Calendar.MILLISECOND);
		int second = cal.get(Calendar.SECOND);
		int minute = cal.get(Calendar.MINUTE);
		//24 hour format
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		String date = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + "." + millisecond;
		
		writeToDCIM(fileName, date + " " + tag + " " + entry);
		Log.d(tag, entry);
	}
	
	//For some reason the file is only visible in Windows explorer after a phone reboot
	protected static void writeToDCIM(String fileName, String dump){
			
		try {
			File myFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + fileName);
			//myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append(dump + "\n");
			myOutWriter.close();
			fOut.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
