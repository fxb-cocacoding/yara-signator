package mongodb;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
@Deprecated
public enum MongoConnection {
	
	/*
	 * mongoClient should not be public but it is for the statistics in ngram.
	 * bad design ahead
	 */
	
	INSTANCE;
	public MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
}
