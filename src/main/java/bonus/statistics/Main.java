package bonus.statistics;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import postgres.PostgresConnection;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		
		logger.info("...Statistics Evaluater...");
		
		final String db_connection_string = "jdbc:postgresql://127.0.0.1/";
		final String db_user = "postgres";
		final String db_password = "";
		final String db_name = "botconf_paper_callsandjumps_datarefs_binvalue";
		
		PostgresConnection.INSTANCE.setConnection(db_user, db_password, db_connection_string, db_name);
		Connection connection = PostgresConnection.INSTANCE.psql_connection;
		
		try {
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StringBuilder sb = new StringBuilder();
		Map<Integer, Map<Integer, Integer>> aggregatedData = getStatsTable4(connection, "aggregated_<n>_part");
		Map<Integer, Map<Integer, Integer>> ngramTableData = getStatsTable4(connection, "ngrams_<n>_part");
		Map<Integer, String> families = null;
		
		try {
			families = MapUtils.invertMap(new postgres.PostgresRequestUtils().getFamiliesWithIDs());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sb.append("Table: " + "\"aggregated_<n>_part\"" + "\n\n");
		sb.append("ngram,family_id,family_name,counted\n");
		for(int n: aggregatedData.keySet()) {
			for(Entry<Integer, Integer> entry: aggregatedData.get(n).entrySet()) {
				int family_id = entry.getKey();
				int counter = entry.getValue();
				String family_name = families.get(family_id);
				String result = n + "," + family_id + "," + family_name + "," + counter + "\n";
				sb.append(result);
				logger.info(result);
			}
		}
		
		sb.append("\n\n\n\nTable: " + "\"ngrams_<n>_part\"" + "\n\n");
		sb.append("ngram,family_id,family_name,counted\n");
		for(int n: ngramTableData.keySet()) {
			for(Entry<Integer, Integer> entry: ngramTableData.get(n).entrySet()) {
				int family_id = entry.getKey();
				int counter = entry.getValue();
				String family_name = families.get(family_id);
				String result = n + "," + family_id + "," + family_name + "," + counter + "\n";
				sb.append(result);
				logger.info(result);
			}
		}

		try {
			FileWriter fw = new FileWriter("/tmp/table4.csv");
			fw.write(sb.toString());
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Set to true if you have the table not created yet.
		String table3csv = getTable3CSV(connection, false);
		
		try {
			FileWriter fw = new FileWriter("/tmp/table3.csv");
			fw.write(table3csv);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static Map<Integer, Map<Integer, Integer>> getStatsTable4(Connection con, String tablePrefix) {

		HashMap<Integer, Map<Integer, Integer>> ret = new HashMap<Integer, Map<Integer, Integer>>();
		
		for(int n = 4; n<=7; n++) {
			
			HashMap<Integer, Integer> m = new HashMap<Integer, Integer>();
			
			final String tableName = tablePrefix.replace("<n>", Integer.toString(n));

			final String queryTable = "select count(DISTINCT concat) AS counter from " + tableName + " WHERE family_id=";

			
			try {
				List<Integer> familyIDs = new ArrayList<Integer>(new postgres.PostgresRequestUtils().getFamiliesWithIDs().values());
				Collections.sort(familyIDs);
				
				for(int i: familyIDs) {
					Statement stmt = con.createStatement();
					logger.info(queryTable + i);
					stmt.execute(queryTable + i);
					ResultSet rs = stmt.getResultSet();
					rs.next();
					int counter = rs.getInt("counter");
					int family_id = i;
					m.put(family_id, counter);
				}
				ret.put(n, m);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return ret;
		
	}
	
	private static String getTable3CSV(Connection con, boolean createTables) {
		StringBuilder sb = new StringBuilder();
		
		for(int n = 4; n<=7; n++) {
		
			final String tableName = "stats_aggregated_" + n + "_part";
			
			final String createStatsTable = "CREATE UNLOGGED TABLE IF NOT EXISTS "
					+ tableName
					+ " (concat text, "
					+ "family_id integer[], "
					+ "sample_id integer[] "
					+ ");";
			
			final String coOccurencesOfNgramsAcrossFamiliesQuery = ""
							+ "INSERT INTO " + tableName + " ("
							+ "SELECT concat, "
							+ "ARRAY_AGG(DISTINCT family_id) AS family_id, "
							+ "ARRAY_AGG(DISTINCT sample_id) AS sample_id "
							/*
							 * Next step: remove occurence, we can use the array since the sub-queries are boosted by partitioning
							 * use a computer generated column via function
							 */
							+ "FROM ngrams_" + n + "_part "
							+ "GROUP BY concat);"
							+ "";
			
			
			if(createTables) {
		
				try {
					Statement stmt = con.createStatement();
					logger.info("\n\nExecuting the query:\n\n\n" + createStatsTable + "\n\n");
					stmt.execute(createStatsTable);
					logger.info("Table created!");
				} catch (SQLException e) {
					e.printStackTrace();
				}
	
				try {
					Statement stmt = con.createStatement();
					logger.info("\n\nExecuting the query:\n\n\n" + coOccurencesOfNgramsAcrossFamiliesQuery + "\n\n");
					stmt.execute(coOccurencesOfNgramsAcrossFamiliesQuery);
					logger.info("Table filled!");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
			final String queryNgramTable = "SELECT COUNT(DISTINCT concat) AS counter FROM ngrams_" + n + "_part";
			final String queryAggTable = "SELECT COUNT(DISTINCT concat) AS counter FROM aggregated_" + n + "_part";
			
			sb.append("query,table_name,n,i,counter\n");
			try {
				Statement stmt = con.createStatement();
				logger.info("\n\nExecuting the query:\n\n\n" + queryNgramTable + "\n\n");
				stmt.execute(queryNgramTable);
				ResultSet rs = stmt.getResultSet();
				rs.next();
				int counter = rs.getInt("counter");
				sb.append(queryNgramTable + "," + "ngrams_" + n + "_part" + "," + n + "," + "" + "," + counter + "\n");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			try {
				Statement stmt = con.createStatement();
				logger.info("\n\nExecuting the query:\n\n\n" + queryAggTable + "\n\n");
				stmt.execute(queryAggTable);
				ResultSet rs = stmt.getResultSet();
				rs.next();
				int counter = rs.getInt("counter");
				sb.append(queryAggTable + "," + "aggregated_" + n + "_part" + "," + n + "," + "" + "," + counter + "\n");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			for(int i=1; i<=5; i++) {
				final String queryStatsTable = "SELECT COUNT(DISTINCT concat) AS counter from " + tableName + " WHERE cardinality(family_id)=" + i;
				
				try {
					Statement stmt = con.createStatement();
					logger.info("\n\nExecuting the query:\n\n\n" + queryStatsTable + "\n\n");
					stmt.execute(queryStatsTable);
					ResultSet rs = stmt.getResultSet();
					rs.next();
					int counter = rs.getInt("counter");
					sb.append(queryStatsTable + "," + tableName + "," + n + "," + i + "," + counter + "\n");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		return sb.toString();
	}
}
