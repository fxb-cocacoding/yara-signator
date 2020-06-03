package postgres;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleStructures {
	
	private static final Logger logger = LoggerFactory.getLogger(HandleStructures.class);
	
	/*
	private final String createTypeInstruction = "CREATE TYPE instruction AS ("
			+ "addr_offset BIGINT,"
			+ "opcodes TEXT,"
			+ "mnemonic_one TEXT,"
			+ "mnemonic_two TEXT"
			+ ");";
	*/
	
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
	
	/*
	private final String createNgramTable = "CREATE TABLE ngrams ("
            + "id serial PRIMARY KEY,"
            + "n SMALLINT,"
            + "score DOUBLE PRECISION,"
            + "family TEXT NOT NULL UNIQUE,"
            + "filename TEXT NOT NULL UNIQUE,"
            + "hash TEXT NOT NULL UNIQUE,"
            + "concat TEXT NOT NULL UNIQUE,"
            + "ngram_instructions instruction ARRAY"
            + ");";
	*/
	
	/*
	private final String createNgramTable = "CREATE TABLE IF NOT EXISTS ngrams ("
            // + "id SERIAL NOT NULL,"
            //+ "n SMALLINT,"
            + "score SMALLINT,"
            //+ "family TEXT NOT NULL,"
            + "sample_id INTEGER NOT NULL REFERENCES samples(id) ON DELETE CASCADE,"// + "filename TEXT NOT NULL,"
           // + "hash TEXT NOT NULL,"
            + "concat TEXT NOT NULL,"
            + "addr_offset BIGINT NOT NULL"
          // + "concat_id INTEGER NOT NULL REFERENCES concat_placeholder(id) ON DELETE CASCADE,"
          //  + "addr_offset BIGINT[],"
          //  + "opcodes TEXT[]," we have all opcodes in concat
		  //+ "mnemonic_one TEXT[],"
			//+ "mnemonic_two TEXT[]"
		 // + "PRIMARY KEY (id, sample_id)"
            + ");";
	
	*/
	
	/*
	 * USING PARTITIONING:
	 * 
	 *
	private final String createNgramTable = "CREATE TABLE IF NOT EXISTS ngrams ("
			+ "score SMALLINT,"
			+ "sample_id INTEGER NOT NULL REFERENCES samples(id) ON DELETE CASCADE,"
			+ "family_id INTEGER NOT NULL REFERENCES families(id) ON DELETE CASCADE,"
			+ "concat TEXT NOT NULL"
	//		+ ",addr_offset BIGINT NOT NULL"
			+ ") PARTITION BY LIST(family_id);";
	
	*/
	
	private final String createNgramTable = "CREATE UNLOGGED TABLE IF NOT EXISTS ngrams ("
			+ "score SMALLINT,"
			+ "sample_id INTEGER NOT NULL REFERENCES samples(id) ON DELETE CASCADE,"
			+ "family_id INTEGER NOT NULL REFERENCES families(id) ON DELETE CASCADE,"
			+ "concat TEXT NOT NULL"
	//		+ ",addr_offset BIGINT NOT NULL"
			+ ");";

	
	private final String createConcatTable = "CREATE TABLE IF NOT EXISTS concat_ ("
			+ "id SERIAL NOT NULL,"
			+ "concat TEXT NOT NULL UNIQUE,"
			+ "mnemonic_one TEXT[],"
			+ "mnemonic_two TEXT[],"
			+ "PRIMARY KEY (id)"
			+ ");";
	
	//private final String dropTypeInstruction = 	"DROP TYPE  IF EXISTS instruction;";
	private final String dropFamiliesTable = 	"DROP TABLE IF EXISTS families;";
	private final String dropSamplesTable = 	"DROP TABLE IF EXISTS samples;";
	private final String dropNgramTable = 		"DROP TABLE IF EXISTS ngrams;";
	private final String dropConcatTable = 		"DROP TABLE IF EXISTS concat;";
	
	public void dropPartitionedTables(Map<String, Integer> families, List<Integer> allN) throws SQLException {
		/*
		 * 
		 * TODO: IMPLEMENT THIS IN THE NGRAM CREATION STEP!
		 * 
		 * 
		 */
		Statement st;
		boolean ret;
		String statement;
		
		for(int n : allN) {
			for(int s: families.values()) {
				
				statement = "DROP TABLE IF EXISTS ngrams_" + n + "_part_" + s + ";";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.info(statement);
				ret = st.execute(statement);				
				st.close();
				
				statement = "DROP TABLE IF EXISTS aggregated_" + n + "_part_" + s + ";";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.info(statement);
				ret = st.execute(statement);				
				st.close();
			}
			
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
			
		}
		
		PostgresConnection.INSTANCE.psql_connection.commit();
	}

	public void createPartinionedNgramTables(List<Integer> allN) throws SQLException {
		
		Statement st;
		boolean ret;

		for(int n : allN) {
			String statement = "";
			
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			ret = st.execute(statement);
			st.close();
		}
		PostgresConnection.INSTANCE.psql_connection.commit();
	}
	
	public synchronized void init() throws SQLException {
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
		
		//st.execute(createConcatTable.replace("concat_", "concat_4"));
		//st.execute(createConcatTable.replace("concat_", "concat_5"));
		//st.execute(createConcatTable.replace("concat_", "concat_6"));
		//st.execute(createConcatTable.replace("concat_", "concat_7"));
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(createNgramTable.replace("ngrams", "ngrams_4_part"));
		st.execute(createNgramTable.replace("ngrams", "ngrams_4_part"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(createNgramTable.replace("ngrams", "ngrams_5_part"));
		st.execute(createNgramTable.replace("ngrams", "ngrams_5_part"));
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(createNgramTable.replace("ngrams", "ngrams_6_part"));
		st.execute(createNgramTable.replace("ngrams", "ngrams_6_part"));

		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(createNgramTable.replace("ngrams", "ngrams_7_part"));
		st.execute(createNgramTable.replace("ngrams", "ngrams_7_part"));
		
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
	}
	
	public synchronized void dropAll() throws SQLException {

		Statement st;

		
		/*
		 * IF PARTITIONING WAS ENABLED:
		 * 
		 * 
		 
		List<Integer> allN = new ArrayList<Integer>();
		allN.add(4);
		allN.add(5);
		allN.add(6);
		allN.add(7);
		
		 
		try {
			Map<String, Integer> families = new PostgresRequestUtils().getFamiliesWithIDs();
			dropPartitionedTables(families, allN);
		} catch(Exception e) {
			logger.info("error occured when dropping partitioned tables, normal if they were not created before.");
			logger.debug(e.getMessage());
		}
		*/
		
		
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
		
		PostgresConnection.INSTANCE.psql_connection.commit();
		
	}
	
}
