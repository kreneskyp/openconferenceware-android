
package org.oscon;

import android.app.Activity;

public class AbstractActivity extends Activity
{

	protected OpenSourceBridgeApplication getApp()
	{
		return (OpenSourceBridgeApplication) this.getApplication();
	}
	
	protected DataService getDataService()
	{
		return getApp().getDataService();
	}
	
	
}
