package db.entites;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import converters.ngrams.Ngram;

public class NgramEntity {
	public int n;

	public String family;
	public String filename;
	public String hash;
	
	public Ngram ngram;

	public String concat;
	
	public String toJson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
	    Gson gson = gsonBuilder.create();
		String json = gson.toJson(this, NgramEntity.class);
		return json;
	}
}
