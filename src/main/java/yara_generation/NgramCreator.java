package yara_generation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import disasm.capstone.CapstoneServer;
import disasm.capstone.DisassemblerInterface;
import iterative_improvement.IterativeImprovementDataContainer;
import main.Config;
import main.NextGenConfig;
import postgres.PostgresConnection;
import postgres.PostgresRequestUtils;
import ranking_system.RankingSystem;
import ranking_system.RankingSystemFacade;
import smtx_handler.Instruction;
import statistics.malpedia_eval.FalseNegative;
import statistics.malpedia_eval.MalpediaEval;
import statistics.malpedia_eval.ReadMalpediaEval;
import statistics.malpedia_eval.Sequence;
import statistics.malpedia_eval.WrongCoveredSample;
import statistics.malpedia_eval.WrongSignature;
import statistics.malpedia_eval.WrongSignatures;

public class NgramCreator {
	private static final Logger logger = LoggerFactory.getLogger(NgramCreator.class);

	DisassemblerInterface disasm;
	
	public NgramCreator() {
		initDisasm();
	}
	
	private final int sleepTime = 500;
	private final int messageSleepLimit = 5000;

	
	private void initDisasm() {
		//DisassemblerBinding disasm = new DisassemblerBinding();
		disasm = new CapstoneServer();
		/*
		 * Initialize the DisassemblerInterface:
		 */
		boolean barrier = false;
		int sleepCounter = 0;
		while(barrier == false) {
			try {
				disasm.createHandle();
				barrier = true;
			} catch(java.net.SocketException e) {
				if(sleepCounter % messageSleepLimit == 0) {
					logger.error("we should be here, is capstone server running?");
					logger.error(e.getMessage());
				}
				try {
					Thread.sleep(sleepTime);
					sleepCounter += sleepTime;
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	public void cleanDisasm() {
		try {
			disasm.closeHandle();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private ArrayList<Instruction> generateInstructionsFromConcatString(int i, int bitness, String concatFromDB, String[] concat) {
		ArrayList<Instruction> instructions = new ArrayList<>(i);
		for(int j=0; j<i; j++) {
			Instruction ins = new Instruction();
			
			//Skip first element since it will be empty
			int index = j+1;
			/*
			 * TODO: We lost Offset at the GROUP BY Operation. We need to get it back by querying the table -> sample_id with a join?
			 * Still an expensive operation.
			 */
			ins.setOffset(0x00);
			//System.out.println(concat[index]);
			try {
				
				/*
				 * WORKAROUND; REMOVE ME IN NEXT RUN
				 */
				String opcodes = concat[index];
				if((concat[index].length() & 1) != 0) {
					logger.error("Opcode detected which has arbitrary length (not even):");
					opcodes = opcodes + "?";
					logger.error(concat[index] + "    " + opcodes);
					logger.error("we fixed the opcode but this should not have happened!");
				}
				/*
				 * EOF WORKAROUND
				 */
				
				ins.setOpcodes(opcodes);
				if(bitness == 32) {
					List<String> al = null;
					int sleepCounter = 0;
					while(al == null) {
						try {
							// replace all YARA wildcards with a zero to get some disassembly output
							if(concat[index].contains("?")) {
								al = new ArrayList<>(Arrays.asList("", ""));
								break;
							}
							al = disasm.getDisassembly(32, 32, Hex.decodeHex(concat[index]));
						} catch(java.net.SocketException | ExceptionInInitializerError e) {
							if(sleepCounter % messageSleepLimit == 0) {
								logger.info("Is the server online?");
								logger.info(e.getMessage());
							}
							try {
								disasm.closeHandle();
								disasm.createHandle();
							} catch (Exception e2) {
								logger.error("error when reinitializing the socket");
								logger.error(e2.getMessage());
							}
							
							try {
								Thread.sleep(sleepTime);
								sleepCounter += sleepTime;
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

			} catch (ArrayIndexOutOfBoundsException e) {
				logger.error("This should not happen that often, check this one (concat from db): " + concatFromDB.toString());
				ins.setMnemonics(new ArrayList<>(Arrays.asList("", "")));
			}
			
			instructions.add(ins);
		}
		return instructions;
	}

	
	public List<Ngram> getNgramsForFamilyDefault(int family_id, Config config) throws SQLException, DecoderException {
		List<Ngram> ret = new ArrayList<>();
		
		for(int i: config.getNs()) {
			
			/*
			 * Getting all Ngram Candidates from Database for a family:
			 */
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
				
				ArrayList<Instruction> instructions;
				instructions = generateInstructionsFromConcatString(i, bitness, concatFromDB, concat);
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
						ret = new main.Utils().removeDuplicatesLosingOrder(ret);
						
						//old: (new -> run only classic score sorter)
						//ret = ranker.rankingAction(ret, config);
						RankingSystem rank = new RankingSystem(ret);
						ret = rank.rank(ret.size(), "rankPerNgramScore");

						
						new main.Utils().removeStuffFromListUntilBarrier(ret, 5000);
					}
				}
			}
			/*
			 * Ranking all candidates:
			 */
			if(config.rankingOptimizerEnabled == true) {
				/*
				 * TODO:
				 * run filter for old while next db searches for next, multithreading?
				 */
				logger.info(family_id + " - running a ranking step now inside filtering step, keeping the results small...");
				ret = new main.Utils().removeDuplicatesLosingOrder(ret);
				
				//old: (new -> run only classic score sorter)
				//ret = ranker.rankingAction(ret, config);
				RankingSystem rank = new RankingSystem(ret);
				ret = rank.rank(ret.size(), "rankPerNgramScore");
				
				new main.Utils().removeStuffFromListUntilBarrier(ret, 5000);
				
				
			}
			
		}
		
		logger.info(family_id + " done! - running the last ranking step now...");
		ret = new main.Utils().removeDuplicatesLosingOrder(ret);
		
		//old: (new -> run only classic score sorter)
		//ret = ranker.rankingAction(ret, config);
		
		RankingSystemFacade facade = new RankingSystemFacade();
		ret = facade.rankingAction(ret, config, config.getRankingConfig());
		//RankingSystem rank = new RankingSystem(ret);
		//ret = rank.rank(ret.size(), "rankPerNgramScore");
		
		
		if(!config.permitOverlappingNgrams) {
			ret = fixOverlappingNgrams(ret);
		}
		
		new main.Utils().removeStuffFromListUntilBarrier(ret, 10);
		
		return ret;
	}
	
	
	private List<Ngram> fixOverlappingNgrams(List<Ngram> ret) {
		/*
		 * Remove possible duplicates or overlapping ngrams:
		 * 
		 * Create a function and enable/disable it in the config!
		 * 
		 */
		
		if(ret.size() > 5000 ) {
			logger.info("You are removing the overlapping ngrams from a return list > 5000, this might slow down your generation process!");
		} 
		if(ret.size() < 100 ) {
			logger.info("You are removing the overlapping ngrams from a return list < 100, this might corrupt your output rule!");
			logger.info("We only registered as input: " + ret.size());
		} 
		
		List<Ngram> realRet = new ArrayList<>();
		for(Ngram ngram: ret) {
			boolean eq = false;
			int count = 0;
			for(Ngram n: ret) {
				
				List<Instruction> ngramInstructions = ngram.getNgramInstructions();
				List<Instruction> nInstructions = n.getNgramInstructions();
				String ngramOpcodes = ngram.getOpcodes();
				String nOpcodes = n.getOpcodes();
				List<String> ngramOpcodesList = ngram.getOpcodesList();
				List<String> nOpcodesList = n.getOpcodesList();
				
				
				if(nInstructions.equals(ngramInstructions)) {
					// Remove exact equals:
					
					//logger.debug("ngram is equal: ");
					//logger.debug("ngram: " + ngram.toString());
					//logger.debug("n:     " + n.toString());
					if(count > 1) {
						/*
						 * They are obviously always one time equal when we do this double loop!
						 */
						eq = true;
						break;
					} else {
						eq = false;
						break;
					}
				} else if(nOpcodes == ngramOpcodes) {
					//equal opcodes:
					
					//logger.debug("ngram has equal opcodes like the other: ");
					//logger.debug("ngram: " + ngramOpcodes);
					//logger.debug("n:     " + nOpcodes);
					
					eq = true;
					break;
				} /*else if(ngramOpcodes.contains(nOpcodes) || nOpcodes.contains(ngramOpcodes)) {
					//remove partials via contains:
					
					//logger.debug("ngram is partially contained by the other: ");
					//logger.debug("ngram: " + ngramOpcodes);
					//logger.debug("n:     " + nOpcodes);
					
					eq = true;
					break;
				} */else {
				
					/*
					 *  Depending on how much instructions are equal they are also
					 *  similar and we remove them or at least reduce the score.
					 *  
					 *  if the overlapping score is larger than 52% (more than 50,
					 *  but since these are doubles they can make problems in "if"-statements)
					 *  we skip the ngram.
					 *
					 */
					//double overlappingFactor = ngram.getOverlappingFactor(n);
					
					//if( overlappingFactor/((double)ngram.n) > 0.77 ) {
					//	eq = true;
					//	break;
					//}
					if(ngram.isOverlapping(n) || n.isOverlapping(ngram)) {
						//logger.debug("ngram is overlapping: ");
						//logger.debug("ngram: " + ngram.toString());
						//logger.debug("n:     " + n.toString());
						//logger.debug("ngram: " + ngramOpcodes);
						//logger.debug("n:     " + nOpcodes);
						eq = true;
						break;
					}
				}
			}
			if(eq == false) {
				realRet.add(ngram);
			}
		}
		return realRet;
	}

	/*
	 * This method is using all samples in database and tries to find all 
	 */
	public List<Ngram> getNgramsForFamily_NextGen_CandidateOne(int family_id, Config config, NextGenConfig currentNextGen) throws SQLException, DecoderException, IOException {
		
		logger.info("method getNgramsForFamily_NextGen_CandidateOne entered!");
		
		MalpediaEval malpediaEval = null;
		
		try {
			malpediaEval = new ReadMalpediaEval().getFileContent(config.malpediaEvalScriptOutput);
			
			/*
			 * Generate/Increment a blacklist for each sample:
			 */
			for(WrongSignatures i: malpediaEval.fps.wrongSignatures) {
				for(WrongSignature j: i.wrongsignature) {
					for(WrongCoveredSample k: j.wrongSamples) {
						for(Sequence l: k.sequences) {
							String pattern = l.pattern.replaceAll(" ", "");
							pattern = pattern.toLowerCase();
							//IterativeImprovementDataContainer.INSTANCE.blackListedSequenceCandidates.add(pattern);
							try {
								IterativeImprovementDataContainer.INSTANCE.addToBlackList(pattern);
							} catch (SQLException e) {
								if(e.getSQLState().equalsIgnoreCase("23505")) {
									logger.debug("duplicate " + pattern + " was not added.");
								} else {
									e.printStackTrace();
									logger.error(e.getSQLState());
									logger.error("for pattern: " + pattern);
									logger.error(e.getMessage());
									logger.error(e.getLocalizedMessage());
								}
							}
						}
					}
				}
			}
			
			logger.info("Blacklist has been altered, containing now " + IterativeImprovementDataContainer.INSTANCE.getBlacklistSize() + " elements!");

		} catch(FileNotFoundException e) {
			logger.info("Blacklist was not altered since we do not have any evaluation yet");
		} catch(IOException e) {
			e.printStackTrace();
			throw new UnsupportedOperationException("This error is fatal.");
		}
		

		
		List<Ngram> ret = new ArrayList<>();
		List<Integer> sample_ids = new PostgresRequestUtils().getSampleIDsFromFamily(family_id);
		
		/*
		 *  We have a counter for each familiy: initially zero.
		 *  Than we increment it per ngram we got from the database:
		 *  This is our greedy implementation for the set coverage problem:
		 */
		
		Map<Integer, Integer> allSampleCounter = new HashMap<>();
		for(int sample_id: sample_ids) {
			allSampleCounter.put(sample_id, 0);
		}
		
		StringBuilder queryTemplate = new StringBuilder();
		queryTemplate.append("SELECT * FROM (");
		
		for(int i: config.getNs()) {
			queryTemplate.append("SELECT * FROM aggregated_" + i + "_part WHERE family_id = " + family_id + " AND ? = ANY (sample_id)");
			queryTemplate.append(" UNION ");
		}
		
		queryTemplate.setLength(queryTemplate.length() - 6);
		queryTemplate.append(") AS agg_table ORDER BY occurence DESC, sample_id ASC;");
		
		/*
		 * Select all good candidates:
		 */
		for(int sample_id: sample_ids) {
			PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(queryTemplate.toString());
			
			for(int i=1; i<=config.getNs().size(); i++) {
				pst.setInt(i, sample_id);
			}
			
			logger.info("fid-" + family_id + " sid-" + sample_id + 
					" query: " + queryTemplate.toString());
			
			pst.execute();
			ResultSet rs = pst.getResultSet();

			while(rs.next()) {
				int score = rs.getShort("score");
				
				//TODO: find the correct bitness
				int bitness = 32;
				int occurenceFromDB = rs.getInt("occurence");
				String concatFromDB = rs.getString("concat");

				/*
				 * We have an empty entry at position [0] now,
				 * because the structure behind this looks
				 * like that: #e800000000#7505#7403#6c
				 */
				
				String[] concat = concatFromDB.split("#");
				
				ArrayList<Instruction> instructions;
				int i = concat.length - 1;
				Ngram ngram = new Ngram(i);
				
				instructions = generateInstructionsFromConcatString(i, bitness, concatFromDB, concat);
				ngram.setNgramInstructions(instructions);
				
				ngram.score = score + occurenceFromDB * 100;
				
				if(IterativeImprovementDataContainer.INSTANCE.isInBlacklist(ngram.getOpcodes().toLowerCase())) {
					logger.info("[BLACKLIST] We prevented using the blacklist: " + ngram.toString());
					logger.info("[BLACKLIST] Was not added to our target YARA rule.");
					continue;
				}
				
				if(!currentNextGen.permitOverlappingNgrams) {
					
					boolean eq = false;
					for(Ngram n: ret) {
						
						List<Instruction> ngramInstructions = ngram.getNgramInstructions();
						List<Instruction> nInstructions = n.getNgramInstructions();
						String ngramOpcodes = ngram.getOpcodes();
						String nOpcodes = n.getOpcodes();
						List<String> ngramOpcodesList = ngram.getOpcodesList();
						List<String> nOpcodesList = n.getOpcodesList();
						
						
						if(nInstructions.equals(ngramInstructions)) {
							// Remove exact equals:
														
							eq = true;
							break;
						} else if(nOpcodes == ngramOpcodes) {
							//equal opcodes:
														
							eq = true;
							break;
						} /*else if(ngramOpcodes.contains(nOpcodes) || nOpcodes.contains(ngramOpcodes)) {
							//remove partials via contains:
							
							eq = true;
							break;
						} */else {
						
							/*
							 *  Depending on how much instructions are equal they are also
							 *  similar and we remove them or at least reduce the score.
							 *  
							 *  if the overlapping score is larger than 52% (more than 50,
							 *  but since these are doubles they can make problems in "if"-statements)
							 *  we skip the ngram.
							 *
							 */
							//double overlappingFactor = ngram.getOverlappingFactor(n);
							
							//if( overlappingFactor/((double)ngram.n) > 0.77 ) {
							//	eq = true;
							//	break;
							//}
							if(ngram.isOverlapping(n) || n.isOverlapping(ngram)) {
								//logger.debug("ngram is overlapping: ");
								//logger.debug("ngram: " + ngram.toString());
								//logger.debug("n:     " + n.toString());
								//logger.debug("ngram: " + ngramOpcodes);
								//logger.debug("n:     " + nOpcodes);
								eq = true;
								break;
							}
						}
					}
					if(eq == false) {
						if(allSampleCounter.get(sample_id) > 7) {
							break;
						}
						Integer[] sampleIDsFromDB = (Integer[]) rs.getArray("sample_id").getArray();
						
						for(int i1=0; i1<sampleIDsFromDB.length; i1++) {
							int value = allSampleCounter.get(sampleIDsFromDB[i1]);
							value++;
							allSampleCounter.put(sampleIDsFromDB[i1], value);
						}
						ret.add(ngram);
					}
				} else {
					if(allSampleCounter.get(sample_id) > 7) {
						break;
					}
					Integer[] sampleIDsFromDB = (Integer[]) rs.getArray("sample_id").getArray();
					
					for(int i1=0; i1<sampleIDsFromDB.length; i1++) {
						int value = allSampleCounter.get(sampleIDsFromDB[i1]);
						value++;
						allSampleCounter.put(sampleIDsFromDB[i1], value);
					}

					ret.add(ngram);
				}
			}
			pst.close();
		}
		
		
		/*
		 * Monitor all the samples we are not able to cover by the rule:
		 */
		
		for(int sample_id: sample_ids) {
			if(allSampleCounter.get(sample_id) < 7) {
				logger.info("YARA rule for family: " + family_id + " will not cover the sample: " + sample_id + " properly, we have only "
						+ allSampleCounter.get(sample_id) + " results but we needed 7 for a proper rule.");
			}
		}
		
		ret = new main.Utils().removeDuplicatesLosingOrder(ret);
		RankingSystemFacade facade = new RankingSystemFacade();
		ret = facade.rankingAction(ret, config, currentNextGen.getRankingConfig());
		
		logger.info("We have " + ret.size() + " sequences for family: " + family_id);
		
		return ret;
	}
	
	public List<Ngram> getNgramsForFamily_NextGen_ParseMalpediaEval_ReduceFalsePositiveStrategy(int family_id, Config config, NextGenConfig currentNextGen) throws SQLException, DecoderException, IOException {
		logger.info("method getNgramsForFamily_NextGen_ParseMalpediaEval_ReduceFalsePositiveStrategy entered!");
		List<Ngram> ret = new ArrayList<>();
		Map<String, Set<String>> fns = new HashMap<String, Set<String>>();
		
		List<Integer> sample_ids = new PostgresRequestUtils().getSampleIDsFromFamily(family_id);
		//family_name contains dot, like win.trickbot, NOT like win_trickbot !!
		Map<String, Integer> families = new PostgresRequestUtils().getFamiliesWithIDs();
		String family_name = "";
		for(Entry<String, Integer> i : families.entrySet()) {
			if(i.getValue() == family_id) family_name = i.getKey();
		}
		
		Map<Integer, Integer> allSampleCounter = new HashMap<>();
		for(int sample_id: sample_ids) {
			allSampleCounter.put(sample_id, 0);
		}
		
		MalpediaEval malpediaEval = null;
		
		try {
			malpediaEval = new ReadMalpediaEval().getFileContent(config.malpediaEvalScriptOutput);
			
			/*
			 * Generate/Increment a blacklist for each sample:
			 */
			for(WrongSignatures i: malpediaEval.fps.wrongSignatures) {
				for(WrongSignature j: i.wrongsignature) {
					for(WrongCoveredSample k: j.wrongSamples) {
						for(Sequence l: k.sequences) {
							String pattern = l.pattern.replaceAll(" ", "");
							pattern = pattern.toLowerCase();
							try {
								IterativeImprovementDataContainer.INSTANCE.addToBlackList(pattern);
							} catch (SQLException e) {
								if(e.getSQLState().equalsIgnoreCase("23505")) {
									logger.debug("duplicate " + pattern + " was not added.");
								} else {
									e.printStackTrace();
									logger.error(e.getSQLState());
									logger.error("for pattern: " + pattern);
									logger.error(e.getMessage());
									logger.error(e.getLocalizedMessage());
								}
							}
						}
					}
				}
			}
			
			logger.info("Blacklist has been altered, containing now " + IterativeImprovementDataContainer.INSTANCE.getBlacklistSize() + " elements!");

			
		} catch(FileNotFoundException e) {
			logger.info("Blacklist was not altered since we do not have any evaluation yet");
		} catch(IOException e) {
			e.printStackTrace();
			throw new UnsupportedOperationException("This error is fatal.");
		}
		
		
		StringBuilder queryTemplate = new StringBuilder();
		queryTemplate.append("SELECT * FROM (");
		
		for(int i: config.getNs()) {
			queryTemplate.append("SELECT * FROM aggregated_" + i + "_part WHERE family_id = " + family_id + " AND ? = ANY (sample_id)");
			queryTemplate.append(" UNION ");
		}
		// Remove last UNION statement:
		queryTemplate.setLength(queryTemplate.length() - 6);
		queryTemplate.append(") AS agg_table ORDER BY occurence DESC, sample_id ASC;");
		
		/*
		 * Select all good candidates:
		 */
		for(int sample_id: sample_ids) {
			PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(queryTemplate.toString());
			for(int i=1; i<=config.getNs().size(); i++) {
				pst.setInt(i, sample_id);
			}
			logger.info("fid-" + family_id + " sid-" + sample_id + " query: " + queryTemplate.toString());

			pst.execute();
			ResultSet rs = pst.getResultSet();
			
			while(rs.next()) {
				int score = rs.getShort("score");
				
				//TODO: find the correct bitness
				int bitness = 32;
				int occurenceFromDB = rs.getInt("occurence");
				String concatFromDB = rs.getString("concat");

				//We have an empty entry at position [0] now, because the structure behind this looks like that: #e800000000#7505#7403#6c
				String[] concat = concatFromDB.split("#");
				
				ArrayList<Instruction> instructions;
				int i = concat.length - 1;
				Ngram ngram = new Ngram(i);
				
				instructions = generateInstructionsFromConcatString(i, bitness, concatFromDB, concat);
				ngram.setNgramInstructions(instructions);
				
				ngram.score = score + occurenceFromDB * 100;
				//System.out.println(instructions.toString());
				
				//Check against blacklist:
				if(IterativeImprovementDataContainer.INSTANCE.isInBlacklist(ngram.getOpcodes().toLowerCase())) {
					logger.info("[BLACKLIST] We prevented using the blacklist: " + ngram.toString());
					logger.info("[BLACKLIST] Was not added to our target YARA rule.");
					continue;
				}
				
				if(!currentNextGen.permitOverlappingNgrams) {
					boolean eq = false;
					for(Ngram n: ret) {
						
						List<Instruction> ngramInstructions = ngram.getNgramInstructions();
						List<Instruction> nInstructions = n.getNgramInstructions();
						String ngramOpcodes = ngram.getOpcodes();
						String nOpcodes = n.getOpcodes();
						List<String> ngramOpcodesList = ngram.getOpcodesList();
						List<String> nOpcodesList = n.getOpcodesList();
						
						if(nInstructions.equals(ngramInstructions)) {
							eq = true;
							break;
						} else if(nOpcodes == ngramOpcodes) {
							eq = true;
							break;
						} else {
							if(ngram.isOverlapping(n) || n.isOverlapping(ngram)) {
								eq = true;
								break;
							}
						}
					}
					if(eq == false) {
						if(allSampleCounter.get(sample_id) > currentNextGen.yara_condition_limit) {
							break;
						}
						Integer[] sampleIDsFromDB = (Integer[]) rs.getArray("sample_id").getArray();
						
						for(int i1=0; i1<sampleIDsFromDB.length; i1++) {
							int value = allSampleCounter.get(sampleIDsFromDB[i1]);
							value++;
							allSampleCounter.put(sampleIDsFromDB[i1], value);
						}
						ret.add(ngram);
					}
				} else {
					if(allSampleCounter.get(sample_id) > currentNextGen.yara_condition_limit) {
						break;
					}
					Integer[] sampleIDsFromDB = (Integer[]) rs.getArray("sample_id").getArray();
					
					for(int i1=0; i1<sampleIDsFromDB.length; i1++) {
						int value = allSampleCounter.get(sampleIDsFromDB[i1]);
						value++;
						allSampleCounter.put(sampleIDsFromDB[i1], value);
					}
					ret.add(ngram);
				}
			}
			pst.close();
		}
		logger.info("\n\n\n\n");
		logger.info("malpediaEval.fns.content: " + malpediaEval.fns.content.size());
		logger.info("\n\n\n\n");

		
		// EXPERIMENTAL!!!
		// Experimental fix for FNs: 
		
		
		for(FalseNegative i: malpediaEval.fns.content) {
			//System.out.println("i.familyName - " + i.familyName + "    " + "family_name - " + family_name.replace('.', '_'));
			if(i.familyName.replace('.', '_').equalsIgnoreCase(family_name.replace('.', '_'))) {
				logger.info("FIXING FNs for family: " + family_name);
				
				// Experimental fix to remove the last false negatives
				//  We will face a lot of false positives instead of them
				 
				
				StringBuilder queryToFixFNs = new StringBuilder();
				queryToFixFNs.append("SELECT * FROM (");
				
				for(int j: config.getNs()) {
					queryToFixFNs.append("SELECT * FROM aggregated_" + j + "_part WHERE family_id = " + family_id + " AND ? = ANY (sample_id)");
					queryToFixFNs.append(" UNION ");
				}
				
				// Remove last UNION statement:
				queryToFixFNs.setLength(queryToFixFNs.length() - 6);
				queryToFixFNs.append(") AS agg_table WHERE ? = ANY (sample_id);");
				
				
				
				String currentFamilyName = i.familyName.replace('_', '.');
				List<String> samplesFilename = i.sampleNames;
				
				for(String s: samplesFilename) {
					logger.info("FIXING FNs for family: " + family_name + " and sample: " + s);

					int counter = 0;
					
					int sample_id = getSampleIdFromSampleFilename(s);
					
					if(sample_id == 0) {
						logger.info("\n\n\nSkipping " + family_name + " - " + s + " is not in our database, so we have to skip " + "\n\n");
						Set<String> tmp = fns.get(family_name);
						if(tmp == null) {
							tmp = new HashSet<String>();
						}
						tmp.add(s);
						fns.put(family_name, tmp);
						continue;
					}
					
					PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(queryToFixFNs.toString());
					
					for(int k=1; k<=config.getNs().size()+1; k++) {
						pst.setInt(k, sample_id);
					}
					
					
					logger.info("fid-" + family_id + " sid-" + sample_id + " query: " + queryToFixFNs.toString());

					pst.execute();
					ResultSet rs = pst.getResultSet();

					while(rs.next()) {
						int score = rs.getShort("score");
						
						//TODO: find the correct bitness
						int bitness = 32;
						int occurenceFromDB = rs.getInt("occurence");
						String concatFromDB = rs.getString("concat");

						//We have an empty entry at position [0] now, because the structure behind this looks like that: #e800000000#7505#7403#6c
						String[] concat = concatFromDB.split("#");
						
						ArrayList<Instruction> instructions;
						int n_size = concat.length - 1;
						Ngram ngram = new Ngram(n_size);
						
						instructions = generateInstructionsFromConcatString(n_size, bitness, concatFromDB, concat);
						ngram.setNgramInstructions(instructions);
						
						ngram.score = score + occurenceFromDB * 100;
						//System.out.println(instructions.toString());
						
						//Check against blacklist:
						if(IterativeImprovementDataContainer.INSTANCE.isInBlacklist(ngram.getOpcodes().toLowerCase())) {
							logger.info("[BLACKLIST] We prevented using the blacklist: " + ngram.toString());
							logger.info("[BLACKLIST] Was not added to our target YARA rule.");
							continue;
						}
						
						if(!currentNextGen.permitOverlappingNgrams) {
							boolean eq = false;
							for(Ngram n: ret) {
								
								List<Instruction> ngramInstructions = ngram.getNgramInstructions();
								List<Instruction> nInstructions = n.getNgramInstructions();
								String ngramOpcodes = ngram.getOpcodes();
								String nOpcodes = n.getOpcodes();
								List<String> ngramOpcodesList = ngram.getOpcodesList();
								List<String> nOpcodesList = n.getOpcodesList();
								
								if(nInstructions.equals(ngramInstructions)) {
									eq = true;
									break;
								} else if(nOpcodes == ngramOpcodes) {
									eq = true;
									break;
								} else {
									if(ngram.isOverlapping(n) || n.isOverlapping(ngram)) {
										eq = true;
										break;
									}
								}
							}
							if(eq == false) {
								if(counter > currentNextGen.yara_condition_limit) {
									break;
								}
								counter++;
								ret.add(ngram);
							}
						} else {
							if(counter > currentNextGen.yara_condition_limit) {
								break;
							}
							counter++;
							ret.add(ngram);
						}
					}
					pst.close();
				}

			}
		}

		

		logger.info("The following FNs were not found in the database and cannot be fixed easily since these rules lack of proper generalization:");
		logger.info(fns.toString());
		for(Entry<String, Set<String>> i: fns.entrySet()) {
			logger.info(i.getKey());
			logger.info("with elements: " + i.getValue().size());
		}
		logger.info("families (size:) " + fns.size());
		
		
		for(int sample_id: sample_ids) {
			if(allSampleCounter.get(sample_id) < currentNextGen.yara_condition_limit) {
				logger.info("YARA rule for family: " + family_id + " will not cover the sample: " + sample_id + " properly, we have only "
						+ allSampleCounter.get(sample_id) + " results but we needed " + currentNextGen.yara_condition_limit + " for a proper rule.");
			}
		}
		ret = new main.Utils().removeDuplicatesLosingOrder(ret);
		
		RankingSystemFacade facade = new RankingSystemFacade();
		ret = facade.rankingAction(ret, config, currentNextGen.getRankingConfig());
		
		return ret;
	}

	private static int getSampleIdFromSampleFilename(String filename) throws SQLException, UnexpectedException {
		String query = "SELECT id FROM samples WHERE filename = ?";
		PreparedStatement pst = PostgresConnection.INSTANCE.psql_connection.prepareStatement(query);
		pst.setString(1, filename);
		pst.execute();
		ResultSet rs = pst.getResultSet();
		int counter = 0;
		int sample_id = 0;
		while(rs.next()) {
			counter++;
			sample_id = rs.getInt(1);
		}
		if(counter != 1) {
			throw new UnexpectedException("We should not have more than two samples with the same filename in the corpus... " + filename);
		}
		return sample_id;
	}
	
}
