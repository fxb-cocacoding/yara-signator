package postgres;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleStructures {
	
	private static final Logger logger = LoggerFactory.getLogger(HandleStructures.class);
	
	private final String createFamiliesTable = "CREATE TABLE IF NOT EXISTS  families ("
			+ "id SERIAL PRIMARY KEY,"
			+ "family TEXT "
			+ "NOT NULL CONSTRAINT family UNIQUE"
			+ ");";
	
	private final String createSamplesTable = "CREATE TABLE IF NOT EXISTS  samples ("
			+ "id SERIAL PRIMARY KEY,"
			+ "family_id INTEGER NOT NULL REFERENCES families(id) ON DELETE CASCADE,"
			+ "architecture TEXT,"
			+ "base_addr BIGINT,"
			+ "bitness BIGINT,"
			+ "binary_size BIGINT,"
			+ "status TEXT,"
			+ "num_api_calls INT,"
			+ "num_basic_blocks INT,"
            + "num_disassembly_errors INT,"
            + "num_function_calls INT,"
            + "num_functions INT,"
            + "num_instructions INT,"
            + "num_leaf_functions INT,"
            + "num_recursive_functions INT,"
            + "timestamp TEXT,"
            + "hash TEXT NOT NULL,"
            + "filename TEXT NOT NULL UNIQUE"
           // + "malpedia_filepath TEXT "
            + ");";
	
	private final String createNgramTable = "CREATE UNLOGGED TABLE IF NOT EXISTS ngrams ("
			+ "score SMALLINT,"
			+ "sample_id INTEGER NOT NULL REFERENCES samples(id) ON DELETE CASCADE,"
			+ "family_id INTEGER NOT NULL REFERENCES families(id) ON DELETE CASCADE,"
			+ "concat TEXT NOT NULL"
			// + ") PARTITION BY HASH (concat);";
			+ ") PARTITION BY RANGE (concat);";

	
	private final String dropFamiliesTable = 	"DROP TABLE IF EXISTS families;";
	private final String dropSamplesTable = 	"DROP TABLE IF EXISTS samples;";
	private final String dropNgramTable = 		"DROP TABLE IF EXISTS ngrams;";
	
	public void dropPartitionedTables(List<Integer> allN, int modulus) throws SQLException {
		
		Statement st;
		boolean ret;
		String statement;
		
		for(int n : allN) {
			/*
			 * Check for hash partitions
			 */
			
			for(int i=0; i<modulus; i++) {
				
				statement = "DROP TABLE IF EXISTS ngrams_" + n + "_part_m" + i + ";";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.info(statement);
				ret = st.execute(statement);				
				st.close();
				
			}
			
			PostgresConnection.INSTANCE.psql_connection.commit();
			
			/*
			 * Check for list partitions:
			 */
			
			for(int i = 0; i< 16; i++) {
				String query = "DROP TABLE IF EXISTS ngrams_" + n + "_part_list_" + Integer.toHexString(i) + ";";
				logger.info(query);
				statement = query;
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.info(statement);
				st.execute(statement);
				st.close();
			}
			String query = "DROP TABLE IF EXISTS ngrams_" + n + "_part_list_rest;";
			logger.info(query);
			statement = query;
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();
			
			/*
			 * Drop the rest
			 */
			
			statement = "DROP TABLE IF EXISTS ngrams_" + n + "_part;";
			
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			ret = st.execute(statement);
			st.close();
			
			statement = "DROP TABLE IF EXISTS aggregated_" + n + "_part;";
			
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			ret = st.execute(statement);
			st.close();
			
			PostgresConnection.INSTANCE.psql_connection.commit();
		}
		
		PostgresConnection.INSTANCE.psql_connection.commit();
	}

	public void createHashPartitionedNgramTable(int modulus, int n) throws SQLException {

		Statement st = null;
		String statement;
			
		for(int i = 0; i< modulus; i++) {
			String query = "CREATE TABLE IF NOT EXISTS ngrams_" + n + "_part_m" + i + " PARTITION OF ngrams_" + n + "_part "
					+ "FOR VALUES WITH (MODULUS " + modulus + ", REMAINDER " + i + ");";
			logger.info(query);
			statement = query;
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();
		}	

		PostgresConnection.INSTANCE.psql_connection.commit();		
	}
	
	public void createRangePartitionedNgramTable(int n) throws SQLException {

		Statement st = null;
		String statement;
			
		for(int i = 0; i< 16; i++) {
			String query = "CREATE UNLOGGED TABLE IF NOT EXISTS ngrams_" + n + "_part_list_" + Integer.toHexString(i) + " PARTITION OF ngrams_" + n + "_part "
					+ "FOR VALUES FROM ('#" + Integer.toHexString(i) +  "') TO ('#" + Integer.toHexString(i) + ""
					+ "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff' );";
			logger.info(query);
			statement = query;
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();
			
			query = "ALTER TABLE ngrams_" + n + "_part_list_" + Integer.toHexString(i) 
					+ " ADD CONSTRAINT ngrams_" + n +  "_part_list_" + Integer.toHexString(i)
					+ " CHECK (concat >= '#" + Integer.toHexString(i) +  "' AND concat <= '#" + Integer.toHexString(i) + ""
					+ "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff' );";

			logger.info(query);
			statement = query;
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();
			
		}
		
		String query = "CREATE UNLOGGED TABLE IF NOT EXISTS ngrams_" + n + "_part_list_rest PARTITION OF ngrams_" + n + "_part "
				+ "FOR VALUES FROM ('#?') TO ('#?ffffffffffffffffffffffffffffffffffffffffffffffffffffffff');";
		logger.info(query);
		statement = query;
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		query = "ALTER TABLE ngrams_" + n + "_part_list_rest"
				+ " ADD CONSTRAINT ngrams_" + n +  "_part_list_rest"
				+ " CHECK (concat >= '#?' AND concat <= '#?"
				+ "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff' );";
		
		logger.info(query);
		statement = query;
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		

		PostgresConnection.INSTANCE.psql_connection.commit();		
	}
		
	public void createRangePartitionedBlacklistTable(String table_name) throws SQLException {

		Statement st = null;
		String statement;
		boolean tablesExist = false;
		try {
			statement = "CREATE TABLE " + table_name + " (concat TEXT UNIQUE NOT NULL) PARTITION BY RANGE (concat);";
			logger.info(statement);
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			st.execute(statement);
			st.close();
			PostgresConnection.INSTANCE.psql_connection.commit();
		} catch(SQLException e) {
			if(e.getSQLState().equalsIgnoreCase("42P07")) {
				logger.info("Blacklist already exists, we skip the rest of the creations...");
				tablesExist = true;
			} else {
				e.printStackTrace();
			}
		}
		if(tablesExist) {
			PostgresConnection.INSTANCE.psql_connection.commit();
			return;
		}
		
		for(int i = 0; i< 16; i++) {
			String query = "CREATE TABLE IF NOT EXISTS " + table_name + "_list_" + Integer.toHexString(i) + " PARTITION OF " + table_name + " "
					+ "FOR VALUES FROM ('" + Integer.toHexString(i) +  "') TO ('" + Integer.toHexString(i) + ""
					+ "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff' );";
			statement = query;
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();
			
			query = "ALTER TABLE " + table_name + "_list_" + Integer.toHexString(i) 
					+ " ADD CONSTRAINT " + table_name + "_list_" + Integer.toHexString(i)
					+ " CHECK (concat >= '" + Integer.toHexString(i) +  "' AND concat <= '" + Integer.toHexString(i) + ""
					+ "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff' );";

			statement = query;
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();
			
		}
		
		String query = "CREATE TABLE IF NOT EXISTS " + table_name + "_list_rest PARTITION OF " + table_name + " "
				+ "FOR VALUES FROM ('?') TO ('?ffffffffffffffffffffffffffffffffffffffffffffffffffffffff');";
		statement = query;
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		query = "ALTER TABLE " + table_name + "_list_rest"
				+ " ADD CONSTRAINT " + table_name + "_list_rest"
				+ " CHECK (concat >= '?' AND concat <= '?"
				+ "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff' );";
		
		statement = query;
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		PostgresConnection.INSTANCE.psql_connection.commit();
	}
	
	public void dropRangePartitionedBlacklistTable(String table_name) throws SQLException {

		Statement st = null;
		String statement;
		
		statement = "DROP TABLE IF EXISTS " + table_name + ";";
		logger.info(statement);
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		for(int i = 0; i< 16; i++) {
			String query = "DROP TABLE IF EXISTS " + table_name + "_list_" + Integer.toHexString(i) + ";";
			logger.info(query);
			statement = query;
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();			
		}
		
		String query = "DROP TABLE IF EXISTS " + table_name + "_list_rest;";
		logger.info(query);
		statement = query;
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		PostgresConnection.INSTANCE.psql_connection.commit();		
	}
	
	public synchronized void init(List<Integer> allN) throws SQLException {
		PostgresConnection.INSTANCE.psql_connection.commit();
		Statement st;
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(createFamiliesTable);
		st.execute(createFamiliesTable);
		PostgresConnection.INSTANCE.psql_connection.commit();
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(createSamplesTable);
		st.execute(createSamplesTable);
		PostgresConnection.INSTANCE.psql_connection.commit();
		
		for (int n: allN) {
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(createNgramTable.replace("ngrams", "ngrams_" + n + "_part"));
			st.execute(createNgramTable.replace("ngrams", "ngrams_" + n + "_part"));
		}
		
		PostgresConnection.INSTANCE.psql_connection.commit();

		for(int n: allN) {
			createRangePartitionedNgramTable(n);
		}
		
		/*
		 * Add functions we needed:
		 * 
		 * The function create_constraint_if_not_exists is copied from the blog entry by Tim Mattison
		 * 
		 * http://blog.timmattison.com/archives/2014/09/02/checking-postgresql-to-see-if-a-constraint-already-exists/
		 *  ( Posted by Tim Mattison Sep 2nd, 2014 )
		 */
		String FUNCTION__create_constraint_if_not_exists =
		
		"\n"
		+ "CREATE OR REPLACE FUNCTION create_constraint_if_not_exists (t_name text, c_name text, constraint_sql text)\n"
		+"  RETURNS void \n"
		+"  AS \n"
		+"  $BODY$ \n"
		+"    begin \n"
		+"      -- Look for our constraint \n"
		+"      if not exists (select constraint_name \n"
		+"                     from information_schema.constraint_column_usage \n"
		+"                     where table_name = t_name  and constraint_name = c_name) then \n"
		+"          execute 'ALTER TABLE ' || t_name || ' ADD CONSTRAINT ' || c_name || ' ' || constraint_sql; \n"
		+"      end if; \n"
		+"  end; \n"
		+"  $BODY$ \n"
		+"  LANGUAGE plpgsql VOLATILE;\n";
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(FUNCTION__create_constraint_if_not_exists);
		st.execute(FUNCTION__create_constraint_if_not_exists);
		PostgresConnection.INSTANCE.psql_connection.commit();
		
		createRangePartitionedBlacklistTable("blacklist");
	}
	
	public synchronized void dropAll() throws SQLException {

		Statement st;

		
		/*
		 * IF PARTITIONING WAS ENABLED:
		 * 
		 * 
		 
		try {
			Map<String, Integer> families = new PostgresRequestUtils().getFamiliesWithIDs();
			dropPartitionedTables(families, allN);
		} catch(Exception e) {
			logger.info("error occured when dropping partitioned tables, normal if they were not created before.");
			logger.debug(e.getMessage());
		}
		
		*/
		
		List<Integer> l = new ArrayList<Integer>();
		l.add(4);
		l.add(5);
		l.add(6);
		l.add(7);
		dropPartitionedTables(l, 256);
		
		PostgresConnection.INSTANCE.psql_connection.commit();
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_4_part"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_4_part"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_5_part"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_5_part"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_6_part"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_6_part"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_7_part"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_7_part"));
		
		PostgresConnection.INSTANCE.psql_connection.commit();
		
		/*
		 * Drop legacy tables which are deprecated
		 */
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_4"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_4"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_5"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_5"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_6"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_6"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropNgramTable.replace("ngrams", "ngrams_7"));
		logger.info(dropNgramTable.replace("ngrams", "ngrams_7"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropSamplesTable);
		logger.info(dropSamplesTable);
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(dropFamiliesTable);
		logger.info(dropFamiliesTable);
		
		//dropRangePartitionedBlacklistTable();
		
		PostgresConnection.INSTANCE.psql_connection.commit();
		
	}
	
}
