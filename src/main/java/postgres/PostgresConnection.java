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
	private int initialized = 0;
	
	PostgresConnection() {
		final String url = "jdbc:postgresql://127.0.0.1/postgres";
		Properties props = new Properties();
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
	
	public synchronized void setConnection(String user, String passwd, String dbString,  String db) throws UnsupportedOperationException {
		initialized++;
		if(initialized != 1) {
			throw new UnsupportedOperationException("You already initialized the PostgresConnection enum!");
		}
		final String url = dbString + db;
		Properties props = new Properties();
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
}
