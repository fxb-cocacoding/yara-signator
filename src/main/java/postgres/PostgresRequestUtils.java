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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import disasm.CapstoneJavaBindings;
import disasm.CapstoneServer;
import disasm.DisassemblerInterface;
import filter.FilterFactory;
import main.Config;
import main.Main;
import main.WildcardConfig;
import main.YaraRuleGenerator;
import ranking_system.RankingSystemFacade;
import smtx_handler.Instruction;
import smtx_handler.SMDA;
import statistics.yara_results.YaraStats;

public class PostgresRequestUtils {

	private static final Logger logger = LoggerFactory.getLogger(PostgresRequestUtils.class);
	
	public boolean isFamilyAlreadyInDatabase(SMDA smda) throws SQLException {
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT family FROM families WHERE family LIKE ?;");
		pst.setString(1, smda.getFamily());
		pst.execute();
		ResultSet rs = pst.getResultSet();
		System.out.println("testing if " + smda.getFamily() + " is already in DB: " + rs.getFetchSize());
		
		if(rs.getFetchSize() > 0 ) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isSampleAlreadyInDatabase(SMDA smda) throws SQLException {
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT filename FROM samples WHERE filename LIKE ?;");
		pst.setString(1, smda.getFilename());
		pst.execute();
		ResultSet rs = pst.getResultSet();
		if(rs.getFetchSize() == 1) return true;
		return false;
	}

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
		
		logger.info(statement);
		pst.execute();
		
		statement = "CREATE INDEX agg" + n + "_btree_index ON aggregated_" + n + " USING btree (family);";
		logger.info(statement);
		pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(statement);
	}
	
	public void insertIntoAgregationTablesPartitioned(int n) throws SQLException {		
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
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(true);
		
		String statement = ""
				+ "INSERT INTO aggregated_" + n + "_part (SELECT concat, UNNEST(ARRAY_AGG(DISTINCT score)) AS score, UNNEST(ARRAY_AGG(DISTINCT family_id)) AS family_id, "
				+ "ARRAY_AGG(DISTINCT samples.id) AS sample_id, "
				+ "cardinality(ARRAY_AGG(DISTINCT samples.id)) AS occurence "
			//	+ "bitness " bitness would be nice, but we do not have it. We lost it when doing the group by and mixing x86 and x64
				+ "FROM ngrams_" + n + " JOIN samples ON samples.id=sample_id "
				+ "GROUP BY concat HAVING COUNT(DISTINCT family_id) = 1) ;"
				+ "";
		
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(statement);
		logger.info(statement);
		pst.execute();
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(false);
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

	
	/*
	 * Filter step
	 * TODO: Refactor this into a real filter case, not inside the Database helper class
	 */
	public List<Ngram> getUniqueNgramsForFamily(int family_id, Config config) throws SQLException, DecoderException {
		List<Ngram> ret = new ArrayList<>();
		RankingSystemFacade ranker = new RankingSystemFacade();
		
		for(int i=4;i<=7;i++) {
			//DisassemblerBinding disasm = new DisassemblerBinding();
			DisassemblerInterface disasm = new CapstoneServer();
			
			boolean barrier = false;
			while(barrier == false) {
				try {
					disasm.createHandle();
					barrier = true;
				} catch(java.net.SocketException e) {
					logger.error("we should be here");
					logger.error(e.getMessage());
					try {
						Thread.sleep(5000);
						System.out.println("sleeping");
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement("SELECT * FROM aggregated_" + i + "_part WHERE family_id = ?");
			pst.setInt(1, family_id);
			logger.info(family_id + " - " + pst.toString());
			pst.execute();
			ResultSet rs = pst.getResultSet();
			
			int progress = 0;
			
			while(rs.next()) {
				if(progress == config.instructionLimitPerFamily) {
					logger.warn("family: " + family_id + " ran into the limit of " + config.instructionLimitPerFamily + "! Rule might be useless...");
					break;
				}
				Ngram ngram = new Ngram(i);
				//String familyFromDB = rs.getString("family");
				int score = rs.getShort("score");
				Integer[] filenamesFromDB = (Integer[]) rs.getArray("sample_id").getArray();
				//int bitness = rs.getInt("bitness");
				
				//TODO: find the correct bitness
				int bitness = 32;
				int occurenceFromDB = rs.getInt("occurence");
				String concatFromDB = rs.getString("concat");

				//We have an empty entry at position [0] now, because the structure behind this looks like that: #e800000000#7505#7403#6c
				String[] concat = concatFromDB.split("#");
				
				ArrayList<Instruction> instructions = new ArrayList<>(i);
				for(int j=0; j<i; j++) {
					Instruction ins = new Instruction();
					int index = j+1;
					/*
					 * TODO: We lost Offset at the GROUP BY Operation. We need to get it back by querying the table -> sample_id with a join?
					 * Still an expensive operation.
					 */
					ins.setOffset(0x00);
					//System.out.println(concat[index]);
					ins.setOpcodes(concat[index]);
					
					if(bitness == 32) {
						List<String> al = null;
						
						while(al == null) {
							try {
								// replace all YARA wildcards with a zero to get some disassembly output
								al = disasm.getDisassembly(32, 32, Hex.decodeHex(concat[index].replace('?', '0')));
							} catch(java.net.SocketException | ExceptionInInitializerError e) {
								logger.info("Is the server online?");
								logger.info(e.getMessage());
								try {
									disasm.closeHandle();
									disasm.createHandle();
								} catch (Exception e2) {
									logger.error("error when reinitializing the socket");
									logger.error(e2.getMessage());
								}
								
								try {
									Thread.sleep(5000);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									logger.error(e1.getMessage());
								}
							} catch(Exception e) {
								logger.error("a strange error occured");
								logger.error(e.getMessage());
							}
						}

						
						
						ins.setMnemonics(al);
						//ins.setMnemonics(disasm.getMnemonics32(concat[index], 0x00));
						//ins.setMnemonics(al);
					} else if(bitness == 64) {
						//ins.setMnemonics(disasm.getMnemonics64(concat[index], 0x00));
					} else {
						throw new UnsupportedOperationException();
					}
					//ins.setMnemonics(new ArrayList<>());
					instructions.add(ins);
				}
				ngram.setNgramInstructions(instructions);
				
				ngram.score = score + occurenceFromDB * 100;
				//System.out.println(instructions.toString());
				
				ret.add(ngram);
				progress++;
				if(progress%50000 == 0) {
					logger.info(family_id + " - progress: " + progress);
					if(config.rankingOptimizerEnabled == true) {
						/*
						 * TODO:
						 * run filter for old while next db searches for next, multithreading?
						 */
						logger.info(family_id + " - running a ranking step now inside filtering step, keeping the results small...");
						ret = ranker.rankingAction(ret, config);
					}
				}
			}
			
			try {
				disasm.closeHandle();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(config.rankingOptimizerEnabled == true) {
				/*
				 * TODO:
				 * run filter for old while next db searches for next, multithreading?
				 */
				logger.info(family_id + " - running a ranking step now inside filtering step, keeping the results small...");
				ret = ranker.rankingAction(ret, config);
			}
			
		}
		System.gc();
		return ret;
	}
	
	public void createPartitionedTables(Map<String, Integer> families, List<Integer> allN) throws SQLException {
		Statement st;
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(true);
		boolean ret;
		
		for(int n : allN) {
			String statement = "CREATE TABLE IF NOT EXISTS aggregated_" + n 
					+ "_part (concat text, score SMALLINT, family_id integer, sample_id integer[], occurence SMALLINT) PARTITION BY LIST(family_id);";
			
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			ret = st.execute(statement);
			st.close();
			
			for(int s: families.values()) {
				
				statement = "CREATE TABLE IF NOT EXISTS aggregated_" + n + "_part_" + s + " PARTITION OF aggregated_" + n + "_part FOR VALUES IN (" + s + ");";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.info(statement);
				ret = st.execute(statement);
				st.close();
				
				statement = "ALTER TABLE aggregated_" + n + "_part_" + s + 
						" ADD CONSTRAINT aggregated_" + n + "_part_" + s + " CHECK( family_id = " + s + ");";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.info(statement);
				ret = st.execute(statement);
				
				st.close();
			}
			
		}
		
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(false);
		
	}

	public void dropPartitionedTables(Map<String, Integer> families, List<Integer> allN) throws SQLException {
		Statement st;
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(true);
		String statement;
		
		for(int n: allN) {
			for(int s: families.values()) {
				statement = "DROP TABLE IF EXISTS aggregated_" + n + "_part_" + s + ";";
				st = PostgresConnection.INSTANCE.psql_connection.createStatement();
				logger.info(statement);
				st.execute(statement);
				st.close();
			}
			statement = "DROP TABLE IF EXISTS aggregated_" + n + "_part;";
			st = PostgresConnection.INSTANCE.psql_connection.createStatement();
			logger.info(statement);
			st.execute(statement);
			st.close();
		}
		
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(false);
		
	}

	
    public void firstInsertions(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion) {
    	/*
		 * Write all smda files into ngrams into database
		 */
		logger.info("Starting with all initial insertions, dropping all tables. PRESS CTRL-C if you wish to abort!");
		for(int i=0;i<20;i++) {
			System.out.print(".");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("");
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
		logger.info(families.toString());
		int counter = 0;
		
		ExecutorService executorService = Executors.newFixedThreadPool(config.rulebuilder_threads);
		for(String family: families) {
			counter++;
			int family_id = familiesWithID.get(family);
			Runnable worker = new YaraRuleGenerator(family, family_id, config, dtf, now, counter, families.size());
			executorService.execute(worker);
		}
		
		executorService.shutdown();
		while (!executorService.isTerminated()) {
			continue;
		}
		logger.info("\nFinished all threads");
	}

	public void validateYaraRules(final DateTimeFormatter dtf, LocalDateTime now, Config config,
			String cmd_yarac, String cmd_yara) {
		new YaraStats().validate(dtf, now, config, cmd_yarac, cmd_yara);
	}

	public static void doFirstInsert(Config config, File[] allSmdaFiles, long minInstructions, boolean firstInsertion) {		
		try {
			new HandleStructures().dropAll();
			new HandleStructures().init();
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

		/*
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
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(true);
		Statement st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		try {
			st.execute("CREATE DATABASE " + config.db_name + sb.toString());
		} catch (SQLException e) {
			logger.error(e.getMessage());
			if(e.getMessage().contains("already exist")) logger.error("ALREADY EXIST IS OKAY");
		}
		PostgresConnection.INSTANCE.psql_connection.setAutoCommit(false);
		
		PostgresConnection.INSTANCE.setConnection(config.db_user , config.db_password, config.db_connection_string, config.db_name + sb.toString());
		/*
		 * testing start
		 */
		st = PostgresConnection.INSTANCE.psql_connection.createStatement();
		st.execute("SELECT current_database();");
		ResultSet rs = st.getResultSet();
		rs.next();
		String test = rs.getString(1);
		logger.info("we are now in the database: " + test + " and this is unchangeable");
		/*
		 * testing end
		 */
	}


}
