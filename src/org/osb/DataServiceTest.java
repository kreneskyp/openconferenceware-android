package org.osb;

import java.io.*;

public class DataServiceTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("running...");
		
		File dir = new File(".");
		DataService service = new DataService(dir);

		Schedule s = service.getSchedule();
		
		for (Event e : s.events)
		{
			System.out.println(e);
		}

	}

}
