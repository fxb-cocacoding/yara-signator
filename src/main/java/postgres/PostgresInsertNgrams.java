package postgres;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import converters.ConverterFactory;
import converters.linearization.Linearization;
import converters.ngrams.Ngram;
import json.Generator;
import main.Config;
import prefiltering.PrefilterFacade;
import smtx_handler.Instruction;
import smtx_handler.Meta;
import smtx_handler.SMDA;

public class PostgresInsertNgrams implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(PostgresInsertNgrams.class);
	
	public PostgresInsertNgrams(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion,
			boolean atLeastOneElementInNgramCollection, int i) {
		this.config=config;
		this.i=i;
		this.firstInsertion = firstInsertion;
		this.allSmdaFiles = allSmdaFiles;
		this.atLeastOneElementInNgramCollection = atLeastOneElementInNgramCollection;
		this.minInstructions = minInstructions;
	}
	
	private Config config;
	private File[] allSmdaFiles;
	private long minInstructions;
	private boolean firstInsertion;
	private boolean atLeastOneElementInNgramCollection;
	private int i;
	
	public void insertSmdaElement(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion,
			boolean atLeastOneElementInNgramCollection, int i) throws IllegalStateException, SQLException {
		
		SMDA smda = null;
		try {
			smda = new Generator().generateSMDA(allSmdaFiles[i].getAbsolutePath());
			
			String family;
			
			if(smda.getMeta() == null) {
				logger.error("No MetaData found. Faulty smda report!");
				logger.error("smda: " + smda.toString());
				return;
			}
			
			if(smda.getMeta().getFamily() != null) {
				family = smda.getMeta().getFamily();
			} else if(smda.getFamily() != null) {
				family = smda.getFamily();
			} else {
				logger.error("No family name found. Faulty smda report!");
				logger.error("smda: " + smda.toString());
				return;
			}
			
			//hooking the field for legacy reasons since SMDA has new format.
			smda.setFamily(family);
			
			logger.info(smda.getMeta().toString());
			
			// Fix Filenames:
			if(smda.getFilename() == null || smda.getFilename().isEmpty()) {
				
				if(smda.getMeta().getMalpedia_filepath() != null && !smda.getMeta().getMalpedia_filepath().isEmpty()) {
					
					String f = smda.getMeta().getMalpedia_filepath();
					f = f.substring(f.lastIndexOf("/")+1, f.length());
					smda.setFilename(f);
					logger.info("sample for family " + smda.getFamily() 
							+ " has an empty filename, we fixed it using the malpedia_filepath entry " 
							+ f);
				
				} else {
					String f = allSmdaFiles[i].getAbsolutePath();
					String filename = f.substring(f.lastIndexOf("/")+1, f.length());
					
					smda.setFilename(filename);
					
					logger.info("fix filename: " + filename + "  smda: " + smda.getSummary().toString());
					logger.warn("sample for family " + smda.getFamily()
							+ " has no valid name info in both fields. use the file system name: " 
							+ f);
				}
				
			}
			
			//Fix or handle malpedia_path
			if(smda.getMeta().getMalpedia_filepath() == null || smda.getMeta().getMalpedia_filepath().isEmpty()) {
				
				Meta m = smda.getMeta();
				m.setMalpedia_filepath("");
				
				logger.warn("the following smda file is not from malpedia or is faulty: " + smda.getSummary().toString());
			}
			
		} catch(IllegalStateException | JsonSyntaxException | NullPointerException e) {
			e.printStackTrace();
			logger.debug(e.getLocalizedMessage());
			logger.debug("smda: " + smda.toString());
			return;
		}
		/*
		 * Step 0
		 * Sanitize the input:
		 */
		if(smda == null || smda.getFilename() == null || smda.getFilename().isEmpty()) {
			System.out.println("null pointer in smda creation, no valid file");
		} else if(smda.getSummary() == null) {
			System.out.println("CONTINUE: NO SUMMARY DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getXcfg() == null) {
			System.out.println("CONTINUE: NO CFG DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getXcfg().getFunctions() == null) {
			System.out.println("CONTINUE: NO FUNCTIONS DETECTED in " + smda.getFamily() + " - " + smda.getFilename() );
			return;
		} else if(smda.getSummary().getNum_instructions() < minInstructions) {
			System.out.println("CONTINUE: NOT ENOUGH INSTRUCTIONS FOUND in " + smda.getFamily() + " - " + smda.getFilename() + " - " + smda.getSummary().getNum_instructions() + "/" + minInstructions);
			return;
		}
		
		/*
		 * Linearize the disassembly.
		 */
		Linearization linearized = new ConverterFactory().getLinearized(smda);
		
		
		/*
		 * Build the n-grams for each n:
		 */
		ArrayList<Integer> allN = config.getNs();
		List<Ngram> ngrams = null;
		
		int family_id = writeFamilyToDatabase(smda);
		
		
		int sample_id = writeSampleToDatabase(smda, family_id);
		
		
		
		if(sample_id == 0) {
			System.out.println("Error: sample_id should never be zero!");
		}
		
		// Debugging the count of ngrams for various n
		Map<Integer, Integer> counter = new HashMap<Integer, Integer>();
		
		//System.out.println("start n");
		for(int n : allN) {
			
			/*
			 * ACTIVATE ONLY IF YOU WANT A PARTITIONED NGRAM_4 TABLE.
			 */
			//createPartitionedNgramTable(family_id, n);
			
			ngrams = new ConverterFactory().calculateNgrams("createWithoutOverlappingCodeCaves", linearized, n);
			
			if(config.wildcardConfigEnabled) {
				PrefilterFacade pre = new PrefilterFacade();
				logger.info("running the prefilter engine");
				ngrams = pre.prefilterAction(ngrams, config);
			}
			
			if(!config.duplicatesInsideSamplesEnabled) {
				int sizeBefore = ngrams.size();
				HashSet<Ngram> s = new HashSet<>();
				s.addAll(ngrams);
				ngrams = new ArrayList<>(s);
				int sizeAfter = ngrams.size();
				logger.info(smda.getFamily() + " - " + smda.getFilename() + " - size_before: " + sizeBefore + " - size_after: " + sizeAfter);
			}

			if(ngrams.isEmpty()) {
				System.out.println("no ngrams detected, got null in " + smda.getFilename());
				continue;
			}
			logger.info("Writing ngrams_" + n + " (size: " + ngrams.size() + ") for " + smda.getFamily() + " - " + smda.getFilename() + " into db.");
			counter.put(n, ngrams.size());
			writeNgramsToDatabase(smda, ngrams, n, config.batchSize, sample_id, family_id);
		}
		
		logger.info("[INSERTION_STEP] Progress: " + (int)(( (float) (i + 1) / allSmdaFiles.length)*100.0) + "% - " + "Step: " + (i + 1) + "/" + allSmdaFiles.length +
				" - Sample: " + smda.getFamily() +
				" " + smda.getFilename() + " " + smda.getArchitecture() + " " + smda.getBitness());
		logger.info("Ngram stats: " + counter.toString());
	}

	public void createPartitionedNgramTable(int family_id, int n) throws SQLException {
		/*
		 * 
		 * TODO: IMPLEMENT THIS IN THE NGRAM CREATION STEP!
		 * 
		 * 
		 */
		Statement st;
		String statement;
			
				
		statement = "CREATE TABLE IF NOT EXISTS ngrams_" + n + "_part_" + family_id + " PARTITION OF ngrams_" + n + "_part FOR VALUES IN (" + family_id + ");";
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		//statement = "ALTER TABLE ngrams_" + n + "_part_" + family_id + 
		//		" ADD CONSTRAINT ngrams_" + n + "_part_" + family_id + " CHECK( family_id = " + family_id + ");";
		
		//create_constraint_if_not_exists (t_name text, c_name text, constraint_sql text)
		statement = "SELECT create_constraint_if_not_exists('ngrams_" + n + "_part_" + family_id + "',"
						+ "'ngrams_" + n + "_part_" + family_id + "',"
						+ "'CHECK( family_id = " + family_id + ");');";
		
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		logger.info(statement);
		st.execute(statement);
		st.close();
		
		PostgresConnection.INSTANCE.psql_connection.commit();		
	}

	
	private int writeSampleToDatabase(SMDA smda, int family_id) throws SQLException {
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("INSERT INTO samples ("
				+ "family_id , architecture , base_addr , status"
				+ ", num_api_calls , num_basic_blocks , num_disassembly_errors , num_function_calls , num_functions"
				+ ", num_instructions , num_leaf_functions , num_recursive_functions ,"
				+ "timestamp , hash , filename, bitness, buffersize, malpedia_filepath) VALUES (?,?,?,?"
				+ ",?,?,?,?,?"
				+ ",?,?,?"
				+ ",?,?,?,?,?,?) ON CONFLICT DO NOTHING RETURNING id;", Statement.RETURN_GENERATED_KEYS);
				
		//pst.setString(1, smda.getFamily());
		pst.setInt(1, family_id);
		pst.setString(2, smda.getArchitecture());
		pst.setLong(3, smda.getBase_addr());
		pst.setString(4, smda.getStatus());
		
		pst.setLong(5, smda.getSummary().getNum_api_calls());
		pst.setLong(6, smda.getSummary().getNum_basic_blocks());
		
		/*
		 * We are currently only interested in the disassembly failed functions,
		 * although interested is not really the right word, we just save them.
		 */
		
		pst.setLong(7, smda.getSummary().getNum_disassembly_failed_functions());
		
		pst.setLong(8, smda.getSummary().getNum_function_calls());
		pst.setLong(9, smda.getSummary().getNum_functions());
		pst.setLong(10, smda.getSummary().getNum_instructions());
		pst.setLong(11, smda.getSummary().getNum_leaf_functions());
		pst.setLong(12, smda.getSummary().getNum_recursive_functions());
		
		pst.setString(13, smda.getTimestamp());
		pst.setString(14, smda.getSha256());
		pst.setString(15, smda.getFilename());
		pst.setLong(16, smda.getBitness());
		pst.setLong(17, smda.getBuffer_size());
		pst.setString(18, smda.getMeta().getMalpedia_filepath());
		
		int rowsModified = pst.executeUpdate();
		
		ResultSet rs = pst.getGeneratedKeys();
		int returnID = 0;
		if(rs.next()) {
			returnID = (int) rs.getObject(1);
		} else {
			throw new SQLException("no return ID could be found for " + smda.getFamily() + " - " + smda.getFilename());
		}
		//PostgresConnection.INSTANCE.psql_connection.commit();
		
		return returnID;
	}

	private int writeFamilyToDatabase(SMDA smda) throws SQLException {
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("INSERT INTO families (family) VALUES (?) ON CONFLICT DO NOTHING RETURNING id;", Statement.RETURN_GENERATED_KEYS);
		pst.setString(1, smda.getFamily());
		
		int rowsModified = pst.executeUpdate();
		
		ResultSet rs = pst.getGeneratedKeys();
		int returnID = 0;
		if(rs.next()) {
			returnID = (int) rs.getObject(1);
		} else {
			pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT * FROM families WHERE family = ?");
			pst.setString(1, smda.getFamily());
			pst.execute();
			rs = pst.getResultSet();
			if(rs.next()) {
				returnID = rs.getInt("id");
				System.out.println("id for family " + smda.getFamily() + " is " + returnID);
			} else {
				throw new SQLException("no return ID could be found for " + smda.getFamily() + " - " + smda.getFilename());
			}
		}
		if(returnID == 0) {
			throw new SQLException("no return ID could be found for " + smda.getFamily() + " - " + smda.getFilename());
		}
		
		return returnID;
	}

	private void writeNgramsToDatabase(SMDA smda, List<Ngram> ngrams, int n, int batchSize, int sample_id, int family_id) throws SQLException {
		
		
		/*
		 * Table Design:
		 * 
		 * 	
		 	
		 	private final String createNgramTable = "CREATE TABLE IF NOT EXISTS ngrams ("
			+ "score SMALLINT,"
			+ "sample_id INTEGER NOT NULL REFERENCES samples(id) ON DELETE CASCADE,"
			+ "family_id INTEGER NOT NULL REFERENCES samples(family_id) ON DELETE CASCADE,"
			+ "concat TEXT NOT NULL,"
			+ "addr_offset BIGINT NOT NULL"
			+ ") PARTITION BY LIST(family_id);";
			
			
		 * 
		 */
		
		
		String insertIntoNgrams = "INSERT INTO "			//					ARRAY		ARRAY			ARRAY
				+ "ngrams (concat, sample_id, family_id, score) "
				+ "VALUES (?,?,?,?)";

		insertIntoNgrams = insertIntoNgrams.replace("ngrams", "ngrams_" + n + "_part");
		
		//PostgresConnection.INSTANCE.psql_connection.setAutoCommit(false);
		PreparedStatement pstIntoNgramsTable = PostgresConnection.INSTANCE.psql_connection.prepareStatement(insertIntoNgrams);
		
		int batchCounter=0;
		for(Ngram ngram: ngrams) {
			batchCounter++;
			
			String concat = "";
			//Long[] addr_offset = new Long[n];
			
			long addr_offset = ngram.getNgramInstructions().get(0).getOffset();
			
			//String[] mnemonic_one = new String[n];
			//String[] mnemonic_two = new String[n];
			
			int count = 0;
			
			for(Instruction i: ngram.getNgramInstructions()) {
				concat += "#" + i.getOpcodes();
				//addr_offset[count] = i.getOffset();
				//mnemonic_one[count] = i.getMnemonics().get(0);
				//mnemonic_two[count] = i.getMnemonics().get(1);
				count++;
			}
			
			pstIntoNgramsTable.setString(1, concat);
			//pst.setString(2, smda.getFamily());
			//pst.setString(2, smda.getFilename());
			//pst.setString(4, smda.getSha256());
			//pst.setShort(4, (short) n);
			pstIntoNgramsTable.setInt(2, sample_id);
			pstIntoNgramsTable.setInt(3, family_id);
			pstIntoNgramsTable.setShort(4, (short)0);
			//pstIntoNgramsTable.setLong(5, addr_offset);
			//pst.setArray(6,  PostgresConnection.INSTANCE.psql_connection.createArrayOf("bigint", addr_offset));
			//pst.setArray(7,  PostgresConnection.INSTANCE.psql_connection.createArrayOf("text", mnemonic_one));
			//pst.setArray(8,  PostgresConnection.INSTANCE.psql_connection.createArrayOf("text", mnemonic_two));
			
			try {
				pstIntoNgramsTable.addBatch();
			} catch(SQLException e) {
				e.printStackTrace();
			}
			
			if(batchCounter%10000 == 0) {
				pstIntoNgramsTable.executeBatch();
				PostgresConnection.INSTANCE.psql_connection.commit();
			}
			
		}
		
		pstIntoNgramsTable.executeBatch();
		PostgresConnection.INSTANCE.psql_connection.commit();
	}

	@Override
	public void run() {
		try {
			insertSmdaElement(config, allSmdaFiles, minInstructions, firstInsertion,
					atLeastOneElementInNgramCollection, i);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("We have an illegal state exception in the file: " + allSmdaFiles[i].getAbsolutePath());
			System.out.println("Further information could not be retrieved because the file was unparseable for us.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("We have an illegal state exception in the file: " + allSmdaFiles[i].getAbsolutePath());
			System.out.println("Further information could not be retrieved because the file was unparseable for us.");
		}
	}

}
