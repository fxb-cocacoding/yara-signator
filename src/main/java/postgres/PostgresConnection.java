package postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public enum PostgresConnection {
	
	/*
	 * mongoClient should not be public but it is for the statistics in ngram.
	 * bad design ahead
	 */
	
	INSTANCE;
	public Connection psql_connection;
	private int initializedBefore = 0;
	private int initializedAfter = 0;
	
	
	/*
	PostgresConnection() {
		final String url = "jdbc:postgresql://127.0.0.1/postgres";
		Properties props = new Properties();
		
		
		//Added rewrite for batch queries:
		props.setProperty("defaultRowFetchSize", "65536");
		props.setProperty("reWriteBatchedInserts", "true");
		
		props.setProperty("user","postgres");
		try {
			//Connection conn = DriverManager.getConnection(url, props);
			psql_connection = DriverManager.getConnection(url, props);
			//psql_connection.setAutoCommit(false);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("Error in Postgres Connection, this might be fatal.");
			e.printStackTrace();
		}
	}
	*/
	
	PostgresConnection() {
		
	}
	
	public synchronized void setConnectionBeforeDBinit(String user, String passwd, String dbString) throws UnsupportedOperationException {
		final String db = "postgres";
		initializedBefore++;
		if(initializedBefore != 1) {
			throw new UnsupportedOperationException("You already initialized the PostgresConnection enum *before* new database was created!");
		}
		final String url = dbString + db;
		Properties props = new Properties();
		
		/*
		 * Added rewrite for batch queries:
		 */
		props.setProperty("defaultRowFetchSize", "65536");
		props.setProperty("reWriteBatchedInserts", "true");
		
		props.setProperty("user",user);
		if(passwd != null && !passwd.isEmpty()) {
			props.setProperty("password", passwd);
		}
		
		try {			
			psql_connection = DriverManager.getConnection(url, props);
		} catch (SQLException e) {
			System.out.println("Error in Postgres setConnection, this might be fatal.");
			e.printStackTrace();
		}
	}
	
	
	public synchronized void setConnectionAfterDBinit(String user, String passwd, String dbString,  String db) throws UnsupportedOperationException {
		initializedAfter++;
		if(initializedAfter != 1) {
			throw new UnsupportedOperationException("You already initialized the PostgresConnection enum *after* db init!");
		}
		final String url = dbString + db;
		Properties props = new Properties();
		
		/*
		 * Added rewrite for batch queries:
		 */
		props.setProperty("defaultRowFetchSize", "65536");
		props.setProperty("reWriteBatchedInserts", "true");
		
		props.setProperty("user",user);
		if(passwd != null && !passwd.isEmpty()) {
			props.setProperty("password", passwd);
		}
		try {
			//Connection conn = DriverManager.getConnection(url, props);
			psql_connection.close();
			psql_connection = DriverManager.getConnection(url, props);
			psql_connection.setAutoCommit(false);
		} catch (SQLException e) {
			System.out.println("Error in Postgres setConnection, this might be fatal.");
			e.printStackTrace();
		}
	}

	public synchronized void overwriteConnection(String user, String passwd, String dbString) throws UnsupportedOperationException {
		final String url = dbString + "postgres";
		Properties props = new Properties();
		
		/*
		 * Added rewrite for batch queries:
		 */
		props.setProperty("defaultRowFetchSize", "65536");
		props.setProperty("reWriteBatchedInserts", "true");
		
		props.setProperty("user",user);
		if(passwd != null && !passwd.isEmpty()) {
			props.setProperty("password", passwd);
		}
		try {
			psql_connection.close();
			psql_connection = DriverManager.getConnection(url, props);
			psql_connection.setAutoCommit(false);
		} catch(SQLException e) {
			System.out.println("Error in Postgres overwriteConnection, this might be fatal.");
			e.printStackTrace();
		}
	}
}
