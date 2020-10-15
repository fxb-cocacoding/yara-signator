package postgres;

import java.io.File;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aggregation_filter.FilterFactory;
import converters.ngrams.Ngram;
import main.Config;
import main.WildcardConfig;
import main.YaraRuleGenerator;
import smtx_handler.SMDA;

public class PostgresRequestUtils {

	private static final Logger logger = LoggerFactory.getLogger(PostgresRequestUtils.class);
	
	public boolean isFamilyAlreadyInDatabase(SMDA smda) throws SQLException {
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT family FROM families WHERE family LIKE ?;");
		pst.setString(1, smda.getMetadata().getFamily());
		pst.execute();
		ResultSet rs = pst.getResultSet();
		logger.debug("testing if " + smda.getMetadata().getFamily() + " is already in DB: " + rs.getFetchSize());
		
		if(rs.getFetchSize() > 0 ) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isSampleAlreadyInDatabase(SMDA smda) throws SQLException {
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT filename FROM samples WHERE filename LIKE ?;");
		pst.setString(1, smda.getMetadata().getFilename());
		pst.execute();
		ResultSet rs = pst.getResultSet();
		if(rs.getFetchSize() == 1) return true;
		return false;
	}

	@Deprecated
	public boolean areNgramsAlreadyInDatabase(SMDA smda, int n, Object object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Deprecated
	public void createAgregationTables(int n) throws SQLException {		
		/*
		 * unoptimized:
		 * 
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(""
				+ "CREATE TABLE IF NOT EXISTS aggregated_" + n + " AS (select concat, UNNEST(ARRAY_AGG(DISTINCT family)) AS family, "
				+ "ARRAY_AGG(DISTINCT samples.id) AS sample_id, "
				+ "cardinality(string_to_array(string_agg(DISTINCT filename, ','), ',')) AS occurence "
				+ "FROM ngrams_" + n + " JOIN samples ON samples.id=sample_id "
				+ "GROUP BY concat HAVING COUNT(DISTINCT family) = 1); "
				+ "");
		*/
		
		String statement = ""
				+ "CREATE TABLE IF NOT EXISTS aggregated_" + n + " AS (select concat, UNNEST(ARRAY_AGG(DISTINCT family)) AS family, "
				+ "ARRAY_AGG(DISTINCT samples.id) AS sample_id, "
				+ "cardinality(ARRAY_AGG(DISTINCT samples.id)) AS occurence "
			//	+ "bitness " bitness would be nice, but we do not have it. We lost it when doing the group by and mixing x86 and x64
				+ "FROM ngrams_" + n + " JOIN samples ON samples.id=sample_id "
				+ "GROUP BY concat HAVING COUNT(DISTINCT family) = 1); "
				+ "";
		
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(statement);
		
		logger.debug(statement);
		pst.execute();
		
		statement = "CREATE INDEX agg" + n + "_btree_index ON aggregated_" + n + " USING btree (family);";
		logger.debug(statement);
		pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(statement);
	}
	
	public void insertIntoAgregationTablesPartitioned(int n) throws SQLException {		
		
		String statement = ""
				+ "INSERT INTO aggregated_" + n + "_part ("
				+ "SELECT concat, "
				+ "UNNEST(ARRAY_AGG(DISTINCT score)) AS score, "
				+ "UNNEST(ARRAY_AGG(DISTINCT family_id)) AS family_id, "
				+ "ARRAY_AGG(DISTINCT sample_id) AS sample_id, "
				/*
				 * Next step: remove occurence, we can use the array since the sub-queries are boosted by partitioning
				 * use a computer generated column via function
				 */
				+ "cardinality(ARRAY_AGG(DISTINCT sample_id)) AS occurence "
				
			//	+ "bitness " bitness would be nice, but we do not have it. We lost it when doing the group by and mixing x86 and x64
				+ "FROM ngrams_" + n + "_part "
				+ "GROUP BY concat HAVING COUNT(DISTINCT family_id) = 1) ;"
				+ "";
		
		
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(statement);
		logger.debug(statement);
		pst.execute();
		PostgresConnection.INSTANCE.psql_connection.commit();
		
		String alterStmt = "ALTER TABLE" + " aggregated_" + n + "_part " + "SET LOGGED;";
		Statement st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.debug(alterStmt);
		st.execute(alterStmt);
		PostgresConnection.INSTANCE.psql_connection.commit();
	}

	public List<String> getFamilies() throws SQLException {
		ArrayList<String> ret = new ArrayList<String>();
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT family FROM families ORDER BY family");
		pst.execute();
		ResultSet rs = pst.getResultSet();
		while(rs.next()) {
			ret.add(rs.getString(1));
		}
		return ret;
	}
	
	public List<String> getSamples(String family) throws SQLException {
		List<String> ret = new ArrayList<String>();
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT family, "
				+ "array_agg(filename) as filename, cardinality(array_agg(filename)) AS size "
				+ "FROM families AS F JOIN samples AS S ON F.id=S.family_id WHERE family = ? GROUP BY family");
		pst.setString(1, family);
		pst.execute();
		ResultSet rs = pst.getResultSet();
		while(rs.next()) {
			String[] a = (String[]) rs.getArray("filename").getArray();
			for(int i=0; i<a.length; i++) {
				ret.add(a[i]);
			}
		}
		return ret;
	}
	
	public List<Integer> getSampleIDsFromFamily(int family_id) throws SQLException {
		List<Integer> ret = new ArrayList<Integer>();
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("select id from samples WHERE family_id = ?");
		pst.setInt(1, family_id);
		pst.execute();
		ResultSet rs = pst.getResultSet();
		while(rs.next()) {
			int sample_id = rs.getInt("id");
			ret.add(sample_id);
		}
		return ret;
	}
	
	public Map<String, Integer> getFamiliesWithIDs() throws SQLException {
		Map<String, Integer> ret = new HashMap<>();
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT id, family FROM families ORDER BY family");
		pst.execute();
		ResultSet rs = pst.getResultSet();
		while(rs.next()) {
			ret.put(rs.getString("family"), rs.getInt("id"));
		}
		return ret;
	}

	public long familyGetFilesize(int family_id) throws SQLException {
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT binary_size FROM samples WHERE family_id = ?");
		pst.setInt(1, family_id);
		pst.execute();
		ResultSet rs = pst.getResultSet();
		
		long largest_filesize = 0;
		
		while(rs.next()) {
			long ret = rs.getInt("binary_size");
			
			int cmp = Long.compareUnsigned(ret, largest_filesize);
			
			/* 
			 * 
			 * https://docs.oracle.com/javase/8/docs/api/java/lang/Long.html#compareUnsigned-long-long-
			 * 
			 * 
			    compare
			
			    public static int compare(long x,
			                              long y)
			
			    Compares two long values numerically. The value returned is identical to what would be returned by:
			
			        Long.valueOf(x).compareTo(Long.valueOf(y))
			     
			
			    Parameters:
			        x - the first long to compare
			        y - the second long to compare
			    Returns:
			        the value 0 if x == y; a value less than 0 if x < y; and a value greater than 0 if x > y
			    Since:
			        1.7
			*/


			
			if(cmp > 0) {
				largest_filesize = ret;
			}
			
			/*
			 * This is simulated by using the compare method from long (since we are unsigned here)
			 * 
			if(ret > largest_filesize) {
				largest_filesize = ret;
			}
			*/
		}

		return largest_filesize;
	}

	public void createPartitionedTables(Map<String, Integer> families, List<Integer> allN) throws SQLException {
		Statement st;
		boolean ret;
		
		for(int n : allN) {
			String statement = "CREATE UNLOGGED TABLE IF NOT EXISTS aggregated_" + n 
					+ "_part (concat text, score SMALLINT, family_id integer, sample_id integer[], occurence SMALLINT) PARTITION BY LIST(family_id);";
			
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.debug(statement);
			ret = st.execute(statement);
			st.close();
			PostgresConnection.INSTANCE.psql_connection.commit();
			for(int s: families.values()) {
				
				statement = "CREATE UNLOGGED TABLE IF NOT EXISTS aggregated_" + n + "_part_" + s + " PARTITION OF aggregated_" + n + "_part FOR VALUES IN (" + s + ");";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.debug(statement);
				ret = st.execute(statement);
				st.close();
				
				statement = "ALTER TABLE aggregated_" + n + "_part_" + s + 
						" ADD CONSTRAINT aggregated_" + n + "_part_" + s + " CHECK( family_id = " + s + ");";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.debug(statement);
				ret = st.execute(statement);
				
				st.close();
				PostgresConnection.INSTANCE.psql_connection.commit();
			}
			
		}
		
		
	}

	public void dropPartitionedTables(Map<String, Integer> families, List<Integer> allN) throws SQLException {
		Statement st;
		String statement;
		
		for(int n: allN) {
			for(int s: families.values()) {
				statement = "DROP TABLE IF EXISTS aggregated_" + n + "_part_" + s + ";";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.debug(statement);
				st.execute(statement);
				st.close();
				PostgresConnection.INSTANCE.psql_connection.commit();
			}
			statement = "DROP TABLE IF EXISTS aggregated_" + n + "_part;";
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.debug(statement);
			st.execute(statement);
			st.close();
			PostgresConnection.INSTANCE.psql_connection.commit();
		}
		
		
	}
	
    public void firstInsertions(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion) {
		logger.info("Starting with all initial insertions, dropping all tables. PRESS CTRL-C if you wish to abort!");
		for(int i=0;i<20;i++) {
			logger.info(".");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		doFirstInsert(config, allSmdaFiles, config.minInstructions, firstInsertion);
    }
    
	public void generateUniquePartitionedTables(Config config) {
		logger.info("Filtering step starts, this will take a while.. (estimate around 3-4h with full malpedia)");
		FilterFactory f = new FilterFactory();
		try {
			Map<String, Integer> families = new PostgresRequestUtils().getFamiliesWithIDs();
			f.filterFamiliesUniqueInPostgres(families, config.getNs());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("Filtering is done!");
	}

	public void generateYaraRule(final DateTimeFormatter dtf, LocalDateTime now, Config config) {
		List<String> families = null;
		Map<String, Integer> familiesWithID = null;
		
		try {
			families = new PostgresRequestUtils().getFamilies();
			familiesWithID = new PostgresRequestUtils().getFamiliesWithIDs();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		logger.trace(families.toString());
		int counter = 0;
		
		ExecutorService executorService = Executors.newFixedThreadPool(config.rulebuilder_threads);
		for(String family: families) {
			counter++;
			int family_id = familiesWithID.get(family);
			
			//TODO optimize this via one query!
			
			long filesize = 0;
			try {
				filesize = new PostgresRequestUtils().familyGetFilesize(family_id);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String condition = "7 of them";
			if(filesize > 0) {
				condition += " and filesize < " + Long.toString(filesize*2);
			}
			
			
			List<Ngram> ngrams = null;
			Runnable worker = new YaraRuleGenerator(family, family_id, config, null, dtf, now, counter, families.size(),
					condition, null);
			executorService.execute(worker);
		}
		
		executorService.shutdown();
		while (!executorService.isTerminated()) {
			continue;
		}
		logger.info("\nFinished all threads");
	}

	public void generateYaraRuleNG(Config config, final DateTimeFormatter dtf, LocalDateTime now,
			String dateFolder, List<String> families, String condition, Map<String, List<Ngram>> ngramsForFamilies) {
		
		Map<String, Integer> familiesWithID = null;
		
		try {
			familiesWithID = new PostgresRequestUtils().getFamiliesWithIDs();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		logger.info("Writing YARA rules for these families now...");
		logger.debug(families.toString());
		logger.trace(ngramsForFamilies.toString());
		int counter = 0;
		
		
		ExecutorService executorService = Executors.newFixedThreadPool(config.rulebuilder_threads);
		for(String s: families) {
			counter++;
			int family_id = familiesWithID.get(s);
			List<Ngram> ngrams = ngramsForFamilies.get(s);
			
			//TODO optimize this via one query!
			
			long filesize = 0;
			try {
				filesize = new PostgresRequestUtils().familyGetFilesize(family_id);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			StringBuilder yaraCondition = new StringBuilder();
			yaraCondition.append(condition);
			if(filesize > 0) {
				yaraCondition.append(" and filesize < " + Long.toString(filesize*2));
			}

			
			logger.debug("Generating rule for family: " + family_id + " (" + s + ") - " + counter + "/" + families.size());
			logger.debug("with condition: " + yaraCondition.toString());
			Runnable worker = new YaraRuleGenerator(s, family_id, config, null, dtf, now, counter, families.size(), yaraCondition.toString(), ngrams);
			executorService.execute(worker);
		}
		
		executorService.shutdown();
		while (!executorService.isTerminated()) {
			continue;
		}
		
		logger.info("\nFinished all threads");
		
	}
	
	public static void doFirstInsert(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion) {		
		try {
			new HandleStructures().dropAll();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		logger.info("dropping done");
		
		try {
			new HandleStructures().init(config.getNs());
		} catch(SQLException e) {
			e.printStackTrace();
		}
		logger.info("init done");

		
		ExecutorService executorService = Executors.newFixedThreadPool(config.insertion_threads);
		for(int i=0; i<allSmdaFiles.length; i++) {
			/*
			 * Step 1
			 * Read in each CFG:
			 */
			Runnable worker = new PostgresInsertNgrams(config, allSmdaFiles, config.minInstructions, true, false, i);
			executorService.execute(worker);
		}
		executorService.shutdown();
		while (!executorService.isTerminated()) {
			continue;
		}
		logger.info("\nFinished all threads");

		for(int n: config.getNs()) {
			String alterStmt = "ALTER TABLE" + " ngrams_" + n + "_part " + "SET LOGGED;";
			try {
				Statement st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.trace(alterStmt);
				st.execute(alterStmt);
				PostgresConnection.INSTANCE.psql_connection.commit();
			} catch(SQLException e) {
				logger.error("Exception when setting " + alterStmt + " - this might be fatal.");
				logger.error(e.getLocalizedMessage());
			}
		}
		/*
		 * useful: \d+
		 * 
		 * get the size of all tables in portgres:
		 * 
			SELECT nspname || '.' || relname AS "relation",
			    pg_size_pretty(pg_relation_size(C.oid)) AS "size"
			  FROM pg_class C
			  LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace)
			  WHERE nspname NOT IN ('pg_catalog', 'information_schema')
			  ORDER BY pg_relation_size(C.oid) DESC
			  LIMIT 20;
		 * 
		 */
	}
	
    public void setup_db_handler(Config config) throws SQLException {

    	StringBuilder sb = new StringBuilder();
		if(config.wildcardConfigEnabled) {
			for(WildcardConfig wc : config.getWildcardConfigConfig()) {
				String s = wc.wildcardOperator;
				sb.append("_");
				sb.append(wc.wildcardOperator);
			}
		}
		
		Statement st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		try {
			st.execute("CREATE DATABASE " + config.db_name + sb.toString());
			//PostgresConnection.INSTANCE.psql_connection.commit();
		} catch (SQLException e) {
			logger.error(e.getMessage());
			if(e.getMessage().contains("already exist") || e.getSQLState().equalsIgnoreCase("42P04")) {
				logger.error("ALREADY EXIST IS OKAY");
			}
			if(e.getMessage().contains("does not exist")) {
				logger.error("database was not created");
				throw new UnsupportedOperationException();
			}
		}
		
		PostgresConnection.INSTANCE.setConnection(config.db_user , config.db_password, config.db_connection_string, config.db_name + sb.toString());

		
		/*
		 * testing start
		 */
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute("SELECT current_database();");
		ResultSet rs = st.getResultSet();
		rs.next();
		String test = rs.getString(1);
		logger.debug("we are now in the database: " + test + " and this is unchangeable");
		/*
		 * testing end
		 */
		
		/*
		 * enable these optimizations. (warning, only per session enabled.)
		 */
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute("SET enable_partition_pruning = on;");
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute("SET enable_partitionwise_aggregate = on;");
		
		PostgresConnection.INSTANCE.psql_connection.commit();
	}

	public HashMap<String, List<String>> getSamplesForFamilies() throws SQLException {
		final String query = "SELECT family, array_agg(filename) as filename, cardinality(array_agg(filename)) AS size "
				+ "FROM families AS F JOIN samples AS S ON F.id=S.family_id GROUP BY family ORDER BY size DESC;";
		HashMap<String, List<String>> ret = new HashMap<>();

		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(query);
		pst.execute();
		ResultSet rs = pst.getResultSet();
		while(rs.next()) {
			String family = rs.getString("family");
			Array filename_array = rs.getArray("filename");
			String[] filename = (String[]) filename_array.getArray();
			int size = rs.getInt("size");
			List<String> filename_insertion = Arrays.asList(filename);
			
			ret.put(family, filename_insertion);
		}

		return ret;
	}

	
	public void writeElementToBlacklist(String s) throws SQLException {
		final String query = "INSERT INTO blacklist VALUES (?) ON CONFLICT DO NOTHING;";
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(query);
		pst.setString(1, s.toLowerCase());
		pst.execute();
		PostgresConnection.INSTANCE.psql_connection.commit();
	}

	
	public boolean isStringAlreadyInBlacklist(String s) throws SQLException {
		final String query = "SELECT concat FROM blacklist WHERE concat = ?;";
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(query);
		pst.setString(1, s.toLowerCase());
		pst.execute();
		ResultSet rs = pst.getResultSet();
		if(rs.next() == false) {
			return false;
		} else {
			return true;
		}
	}

	public int getBlacklistSize() throws SQLException {
		final String query = "SELECT COUNT(concat) FROM blacklist;";
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(query);
		pst.execute();
		ResultSet rs = pst.getResultSet();
		rs.next();
		PostgresConnection.INSTANCE.psql_connection.commit();
		return rs.getInt(1);
	}

}
