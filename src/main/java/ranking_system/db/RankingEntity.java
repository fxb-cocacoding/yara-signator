package ranking_system.db;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import converters.ngrams.Ngram;
import db.entites.NgramEntity;

@Deprecated
public class RankingEntity {
	String family;
	String concat;
	String filename;
	int n;
	List<Ngram> ngrams;

	public String toJson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
	    Gson gson = gsonBuilder.create();
		String json = gson.toJson(this, RankingEntity.class);
		return json;
	}
}
