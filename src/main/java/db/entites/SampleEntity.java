package db.entites;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import smtx_handler.Statistics;

public class SampleEntity {
	public String family;

	public String architecture;
	public String base_addr;
	public String status;
	
	public Statistics summary;
	
	public String timestamp;
	
	public String hash;
	public String filename;
	
	public String toJson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
	    Gson gson = gsonBuilder.create();
		String json = gson.toJson(this, SampleEntity.class);
		return json;
	}
}