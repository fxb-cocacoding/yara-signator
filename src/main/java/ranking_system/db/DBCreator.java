package ranking_system.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.text.html.parser.Entity;

import org.bson.BSON;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.AggregationOptions.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.util.JSON;

import converters.ngrams.Ngram;
import mongodb.Database;
import mongodb.MongoConnection;
import smtx_handler.Instruction;

@Deprecated
public class DBCreator {
	public static void main(String args[]) {
		/*
		 * Helper class main function.
		 * run to generate a helper database only used for looking up the ngrams in the unique_samples db quick.
		 * this is needed since mongodb architecture requires flattening for fast IO.
		 */
		
		//db.ngrams.aggregate( [ { $project: { family: "$family", filename: "$filename", hash: "$hash", concatedOpcodes: 
		//{ $reduce: {input: {$concatArrays: ["$ngram.ngramInstructions.opcodes"]}, initialValue: "", 
		//in: { $concat : ["$$value", "#", "$$this"] }  } }, ngram: "$ngram" } } ] )
		
		final String readDatabase = "caching_db";
		final String readCollection = "ngrams";
		
		final String writeDatabase = "ranking_db";
		final String writeCollection = "lookup_";
		
		DBCollection ngrams = MongoConnection.INSTANCE.mongoClient.getDB(readDatabase).getCollection(readCollection);
		
		Builder builder = AggregationOptions.builder().allowDiskUse(true);
		
		/*
		 * Monster query! We have currently no index on this one!
		 */
		List<DBObject> aggQuery = Arrays.<DBObject>asList(
				new BasicDBObject("$project", new BasicDBObject("family", "$family")
						.append("filename","$filename")
						.append("hash","$hash")
						.append("concat", 
								new BasicDBObject("$reduce", 
										new BasicDBObject("input", 
												new BasicDBObject("$concatArrays", Arrays.asList("$ngram.ngramInstructions.opcodes"))
												)
										.append("initialValue", "")
										.append("in",
												new BasicDBObject("$concat", Arrays.asList("$$value", "#", "$$this"))
										)
								)
						)
						.append("n", "$ngram.n")
						.append("ngramInstructions", "$ngram.ngramInstructions")
				),
				new BasicDBObject("$project",
						new BasicDBObject("_id", 1)
						.append("concat", 1)
						.append("family", 1)
						.append("filename", 1)
						.append("n", 1)
						.append("ngramInstructions", 1)
						.append("score", null)
				)
		);
		
		Cursor cursor = ngrams.aggregate(aggQuery, builder.build());
		int i = 0;
		
		Builder insertBuilder = AggregationOptions.builder().allowDiskUse(true);
		List<DBObject> aggQueryInsert = Arrays.<DBObject>asList(
			new BasicDBObject()	
		);
		
		DBCollection collection_n4 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "4");
		DBCollection collection_n5 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "5");
		DBCollection collection_n6 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "6");
		DBCollection collection_n7 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "7");
		
    	ArrayList<RankingEntity> entities = new ArrayList<>();
		
	    while(cursor.hasNext()) {
	    	DBObject object = cursor.next();
	    	String family = object.toMap().get("family").toString();
	    	String filename = object.toMap().get("filename").toString();
	    	String concat = object.toMap().get("concat").toString();
	    	int n = (int) object.toMap().get("n");
	    	List<Ngram> ngramInstructions = new ArrayList<Ngram>();
	    	//List<BSON> all = (List<BSON>) object.toMap().get("ngramInstructions");
	    	List<Ngram> all = (List<Ngram>) object.toMap().get("ngramInstructions");
	    	RankingEntity rankingEntity = new RankingEntity();
	    	rankingEntity.concat = concat;
	    	rankingEntity.family = family;
	    	rankingEntity.filename = filename;
	    	rankingEntity.n = n;
	    	rankingEntity.ngrams = all;
	    	
	    	entities.add(rankingEntity);
	    	
	    	//System.out.println(all);
	    	//System.out.println("n: " + n + " concat: " + concat + " nI: " + ngramInstructions);
	    	//System.out.println(object.toMap().toString());
	    	
	    	if(i % 10000 == 0 || !cursor.hasNext()) {
	    		if(i % 1000000 == 0) System.out.println( i + "/" + 306450486 + " - " + (i/306450486.0 * 100) + "%");
	    		insertIntoDB(entities, collection_n4, collection_n5, collection_n6, collection_n7);
	    		entities = new ArrayList<>();
	    	}
	    	

	    	i++;
	    }
	}
	
	private static void insertIntoDB(ArrayList<RankingEntity> entities, DBCollection collection_n4, DBCollection collection_n5, DBCollection collection_n6, DBCollection collection_n7) {
    	for(RankingEntity r : entities) {
    		switch (r.n) {
    		case 4:
				collection_n4.insert((DBObject)JSON.parse(r.toJson()));
				break;
    		case 5:
				collection_n5.insert((DBObject)JSON.parse(r.toJson()));
				break;
    		case 6:
				collection_n6.insert((DBObject)JSON.parse(r.toJson()));
				break;
    		case 7:
				collection_n7.insert((DBObject)JSON.parse(r.toJson()));
				break;

			default:
				continue;
			}
    	}
	}

	public void buildIndexes() {
		
		final String readDatabase = "caching_db";
		final String readCollection = "ngrams";
		
		final String writeDatabase = "ranking_db";
		final String writeCollection = "lookup_";

		
		DBCollection collection_n4 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "4");
		DBCollection collection_n5 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "5");
		DBCollection collection_n6 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "6");
		DBCollection collection_n7 = MongoConnection.INSTANCE.mongoClient.getDB(writeDatabase).getCollection(writeCollection + "7");

		DBObject indexOptions = new BasicDBObject();
		indexOptions.put("concat", 1);
		collection_n4.createIndex(indexOptions);
		
		indexOptions = new BasicDBObject();
		indexOptions.put("concat", 1);
		collection_n4.createIndex(indexOptions);
		
		indexOptions = new BasicDBObject();
		indexOptions.put("concat", 1);
		collection_n4.createIndex(indexOptions);
		
		indexOptions = new BasicDBObject();
		indexOptions.put("concat", 1);
		collection_n4.createIndex(indexOptions);
	}
}
