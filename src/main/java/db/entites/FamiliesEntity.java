package db.entites;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import converters.ngrams.Ngram;

public class FamiliesEntity {
	public String family;
	
	public String toJson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
	    Gson gson = gsonBuilder.create();
		String json = gson.toJson(this, FamiliesEntity.class);

		return json;
	}
}
