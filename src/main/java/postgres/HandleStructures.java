package postgres;

import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.Main;

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
            + "filename TEXT NOT NULL UNIQUE "
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
	private final String createNgramTable = "CREATE UNLOGGED TABLE IF NOT EXISTS ngrams ("
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
	
	private final String createConcatTable = "CREATE UNLOGGED TABLE IF NOT EXISTS concat_ ("
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
	
	public synchronized void init() throws SQLException {
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(true);
		Statement st;
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		//st.execute(createTypeInstruction);
		st.execute(createFamiliesTable);
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(createSamplesTable);
		//st.execute(createConcatTable.replace("concat_", "concat_4"));
		//st.execute(createConcatTable.replace("concat_", "concat_5"));
		//st.execute(createConcatTable.replace("concat_", "concat_6"));
		//st.execute(createConcatTable.replace("concat_", "concat_7"));
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(createNgramTable.replace("ngrams", "ngrams_4"));
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(createNgramTable.replace("ngrams", "ngrams_5"));
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(createNgramTable.replace("ngrams", "ngrams_6"));
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute(createNgramTable.replace("ngrams", "ngrams_7"));
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(false);
}
	
	public synchronized void dropAll() throws SQLException {
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(true);

		Statement st;
		
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
		
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(false);
	}
	
}
