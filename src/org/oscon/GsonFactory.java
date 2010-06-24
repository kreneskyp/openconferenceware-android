
package org.oscon;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory
{
	static public Gson createGson()
	{
		final GsonBuilder builder = new GsonBuilder();
		final Gson gson = builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss-'07:00'")
						.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
						.create();
		return gson;
	}
}
