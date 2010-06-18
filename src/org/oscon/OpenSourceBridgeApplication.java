
package org.oscon;

import android.util.Log;
import android.widget.Toast;

public class OpenSourceBridgeApplication
	extends android.app.Application
	implements Thread.UncaughtExceptionHandler
{
	private DataService service;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		service = new DataService(this.getFilesDir());
		
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	public DataService getDataService()
	{
		return service;
	}
	
	
	public void uncaughtException(Thread thread, Throwable ex)
	{
		Log.i("osb", "uncaughtException method called");
		if (ex != null)
		{
			String msg = ex.toString();  
			Toast.makeText(this, msg, Toast.LENGTH_LONG);
		}
	}

}
