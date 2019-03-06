package db.entites;

import java.util.List;
import java.util.Set;

@Deprecated
public class NgramUniqueEntity {
	/*
	       				new BasicDBObject("$project",
						new BasicDBObject("_id", 1)
						.append("concat", 1)
						.append("families", 1)
						.append("families_size", new BasicDBObject("$size", "$families"))
						.append("samples", 1)
						.append("samples_size", new BasicDBObject("$size", "$samples"))
						.append("ngramsCounted", 1)
						.append("n", 1)
						.append("score", null)

	 */
	
	public String concat;
	public Set<String> families;
	public int families_size;
	public Set<String> samples;
	public int samples_size;
	public int ngramsCounted;
	public int n;
	public double score;
}
