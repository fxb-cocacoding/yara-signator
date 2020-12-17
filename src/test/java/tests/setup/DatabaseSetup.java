package tests.setup;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import main.Config;
import postgres.PostgresConnection;

public class DatabaseSetup {
	
	public static Connection createDBConnection(Config config) {
		System.out.println("creating DBs");
		try {
			Statement st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			st.execute("CREATE DATABASE " + config.db_name + "_test");
			//PostgresConnection.INSTANCE.psql_connection.commit();
		} catch (SQLException e) {
			System.out.println(e.getSQLState());
			if(e.getSQLState().equalsIgnoreCase("42P04")) {
				System.out.println("db already exists");
			} else {
				e.printStackTrace();
			}
		}
		
		PostgresConnection.INSTANCE.setConnection(config.db_user , config.db_password, config.db_connection_string, config.db_name + "_test");
		return PostgresConnection.INSTANCE.psql_connection;
	}
}