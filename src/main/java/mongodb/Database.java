package mongodb;

import java.util.List;

@Deprecated
public class Database {
	
	public List<String> getDatabaseNames() {
		return MongoConnection.INSTANCE.mongoClient.getDatabaseNames();
	}
	
	public com.mongodb.client.MongoDatabase getDatabase(String db) {
		return MongoConnection.INSTANCE.mongoClient.getDatabase(db);
	}
	
	
	
}
