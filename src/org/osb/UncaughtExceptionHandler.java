package org.osb;

public class UncaughtExceptionHandler
	implements Thread.UncaughtExceptionHandler
{
	private Thread.UncaughtExceptionHandler handler;
	
	public UncaughtExceptionHandler(Thread.UncaughtExceptionHandler ueh)
	{
		this.handler = ueh;
	}
	
	public void uncaughtException(Thread thread, Throwable ex)
	{
		// TODO Auto-generated method stub
		
	}

}
