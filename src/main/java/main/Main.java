package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.comparator.NameFileComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import iterative_improvement.IterativeImprovementDataContainer;
import iterative_improvement.IterativeImprovementSystemFacade;
import postgres.PostgresRequestUtils;
import utils.FileFinder;
import utils.MalpediaVersion;

/** Main class, entry point for yara-signator.
 * 
 * @author Felix Bilstein
 * @author yara-signator (at) cocacoding (dot) com
 * @version 0.4.0
 * @since 0.1
*/
public class Main {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	/**
	 * This is the main method of yara-signator
	 * 
	 * 0. Sanitize list and remove all without instructions
	 * 1. Read in CFG from smda files
	 * 2. linearize them
	 * 
	 * 3. build n-grams
	 * 3.1 prefilter with wildcard support
	 * 
	 * 4. filter them via aggregation in DB
	 * 5. rank them via RankingSystem
	 * 6. create best YARA rules
	 * 7. validate the rules
	 * 
	 * 7.1 -> NextGen building system for new high-quality rules.
	 * 
	 * 8. profit!
	 * 
	 */
	public static void main(String[] argv) {
				
		final String configFileName = System.getProperty("user.home") + "/.yarasignator.conf";
		long startTime = System.nanoTime();
	    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
		LocalDateTime now = LocalDateTime.now();
		String dateFolder = dtf.format(now);
	    Config config = null;
	    
	    /*
	     * Read the configuration file from: System.getProperty("user.home") + "/.yarasignator.conf";
	     */
	    try {
			config = new Utils().getConfig(configFileName);
			IterativeImprovementDataContainer.INSTANCE.setConfig(config);
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage());
			if(config == null) {
				LOGGER.error("fatal, config file could not be processed!");
			}
			return;
		}
		
	    if(config.yara_signator_version != null && config.yara_signator_version != "") {
	    	Versioning.INSTANCE.VERSION = config.yara_signator_version;
	    	LOGGER.info("VERSION:   " + Versioning.INSTANCE.VERSION);
	    } else {
	    	LOGGER.warn("YARA-Signator version not found, using 0.0.0...");
	    	Versioning.INSTANCE.VERSION = "0.0.0";
	    }
	    
	    if(config.malpediaVersioningFile != null && config.malpediaVersioningFile != "") {
	    	MalpediaVersion mv;
	    	try {
	    		mv = new Utils().getVersioning(config.malpediaVersioningFile);
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    		LOGGER.error("versioning file is broken.");
	    		return;
	    	}
	    	LOGGER.info("mv.commit: " + mv.commit);
	    	LOGGER.info("mv.date:   " + mv.date);
	    	Versioning.INSTANCE.MALPEDIA_COMMIT = mv.commit;
	    	Versioning.INSTANCE.MALPEDIA_DATE = mv.date;
	    }
	    
	    
	    /*
	     * Processing variables from configuration file for building path etc
	     */
	    if( config.resumeFolder != null && !config.resumeFolder.isEmpty() && !config.resumeFolder.equalsIgnoreCase("") ) {
	    	/*
	    	 * if resumeFolder is set, override the dateFolder which defaults to the current time and date -> to prevent overriding
	    	 */
	    	dateFolder = config.resumeFolder;
	    	LOGGER.warn("RESUME FOLDER ACTIVATED: This will override all your results in this folder!"
	    			+ "\n" + config.output_path + "/" + dateFolder + "\n PRESS CTRL+C if you wish to abort!");
	    	try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				LOGGER.error("Main thread was interrupted while waiting for user.");
				LOGGER.error("dateFolder was not updated, it points to: " + dateFolder
						+ " - but should point to this value from the config: " + config.resumeFolder);
				LOGGER.error(e.getLocalizedMessage());
				return;
			}
	    }
	    
	    
		final String yaracPath = config.output_path + "/" + dateFolder + "/*/*/*/* " + config.output_path + "/" + dateFolder + "/" + "yara-compilement";
		final String yaraCompilement = config.output_path + "/" + dateFolder + "/" + "yara-compilement";
		final String cmdYara = config.yaraBinary + " -r " +  yaraCompilement + " " + config.malpedia_path;
		final String cmdYarac = config.yaracBinary + " " + yaracPath; // NOPMD by fxb on 08.05.20 10:51
		final String copyConfigTarget = config.output_path + "/" + dateFolder + "/" + "configFile";
		
		if(!config.reportFileName.startsWith("/")) {
			String newFileName = config.output_path + "/" + dateFolder + "/" + config.reportFileName;
			LOGGER.info("Changed the reportFileName from >>" + config.reportFileName + "<< to >>" + newFileName + "<<");
			config.reportFileName = newFileName;
		}
		
		//File[] allSmdaFiles = new File(config.smda_path).listFiles();
		File[] allSmdaFiles = null;
		try {
			List<String> allSmdaFilesList = new FileFinder().getFiles(config.smda_path);
			List<File> tmpSmdaFiles = new ArrayList<File>();
			for(String s : allSmdaFilesList) {
				tmpSmdaFiles.add(new File(s));
			}
			allSmdaFiles = tmpSmdaFiles.toArray(new File[tmpSmdaFiles.size()]);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		
		
		if(config.reduceInputForDebugging == true) {
			/*
			 * If debugging is enabled, we shrink the input files to 150 for debugging:
			 */
			LOGGER.warn("REDUCED THE INPUT FILES TO 350, DEBUGGING WAS ACTIVATED! (config.reduceInputForDebugging: "
					+ config.reduceInputForDebugging + " )");
			Arrays.sort(allSmdaFiles, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
			ArrayList<File> files = new ArrayList<File>();
			for(int i=0;i<350; i++) {
				files.add(allSmdaFiles[i]);
			}
			allSmdaFiles = new File[files.size()];
			allSmdaFiles = files.toArray(allSmdaFiles);
		}
		LOGGER.info("Found " + allSmdaFiles.length + " files which will be processed!");
		
		
		/*
		 * Setting up the database singleton
		 */
		LOGGER.info("Setting up the database connection...");
		try {
			new PostgresRequestUtils().setup_db_handler(config);
		} catch (SQLException e) {
			LOGGER.error(e.getLocalizedMessage());
			return;
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage());
			return;
		}
		
		LOGGER.info("Creating a copy of the config file in the target location: " + copyConfigTarget);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			File file = new File(copyConfigTarget);
			file.getParentFile().mkdirs();
			FileWriter fw;
			fw = new FileWriter(file, false);
			fw.write(gson.toJson(config));
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e.getLocalizedMessage());
			LOGGER.error("We were unable to write the following config file to the file: \n" + gson.toJson(config) + "\n");
			return;
		}
		
		
		LOGGER.info("Starting the step: " + "SMDAInsertions");
		if(!config.skipSMDAInsertions) {
			try {
				new PostgresRequestUtils().firstInsertions(config, allSmdaFiles, config.minInstructions, true);
			} catch (Exception e) {
				LOGGER.error(e.getLocalizedMessage());
			}
		}
		
		LOGGER.info("Starting the step: " + "UniqueNgramTableCreation");
		if(!config.skipUniqueNgramTableCreation) {
			try {
				new PostgresRequestUtils().generateUniquePartitionedTables(config);
			} catch (Exception e) {
				LOGGER.error(e.getLocalizedMessage());
			}
		}
		
		LOGGER.info("Starting the step: " + "YaraRuleGeneration");
		if(!config.skipYaraRuleGeneration) {
			try {
				new PostgresRequestUtils().generateYaraRule(dtf, now, config);
			} catch (Exception e) {
				LOGGER.error(e.getLocalizedMessage());
			}
		}
		
		
		
		IterativeImprovementSystemFacade ngFacade = new IterativeImprovementSystemFacade(dtf, now, config, dateFolder, cmdYarac, cmdYara);
		
		LOGGER.info("Starting the step: " + "RuleValidation");
		String stats = "";
		if(!config.skipRuleValidation) {
			stats = ngFacade.verify();
			LOGGER.info("Stats: " + stats);
			stats = stats + "\n\n";
		}
		
		LOGGER.info("Starting the step: " + "reportStatistics");
		if(config.reportStatistics == true) {
			
			StringBuilder info = new StringBuilder();
			
			info.append("YARA-SIGNATOR " + Versioning.INSTANCE.VERSION + " CSV REPORT " + dateFolder + "\n");
			info.append("Results are compiled with the config:\n");
			info.append("WildcardConfig:\n");
			
			for(WildcardConfig c: config.getWildcardConfigConfig()) {
				info.append("    " + c.wildcardOperator + "\n");
			}
			
			info.append("RankingConfig:\n");
			
			for(RankingConfig c: config.getRankingConfig()) {
				info.append("    " + c.ranker + "\n");
			}
			
			info.append("\n\n\n");
			
			String csv = ngFacade.getCSV();
			/*
			 * Wrtie csv and then stats to file
			 */
			try {
				FileWriter fw;
				fw = new FileWriter(config.reportFileName, false);
				fw.write(info.toString() + csv + stats);
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error(e.getLocalizedMessage());
				LOGGER.error("We were unable to write the following CSV to the file: \n\n" + info.toString() + csv + stats + "\n\n");
			}
		}
		
		
		/*
		 * The magic happens here:
		 * 
		 * The next gen step will try to optimize rules which cover not already
		 * a family completely without being free of false positives.
		 * 
		 * fixTheseRules Set will contain all rules for families which do not:
		 *  # cover every sample in the family
		 *  # do not raise any false positives.
		 * 
		 */
		
		
		
		LOGGER.info("Starting the step: " + "NextGen");
		if(!config.skipNextGen) {
			LOGGER.info("Running now the experimental NextGeneration approach to fix the remaining rule candidates!");
			LOGGER.info("Finding the families which have to be fixed...");
			LOGGER.info("Starting NextGenSystem... Maximum Iteration Level: " + config.ng_recursion_limit);
			
			ngFacade.ngAction();
		}
		
		
		LOGGER.info("Shutting down.");
		long endTime = System.nanoTime();
		LOGGER.info("Took " + (endTime - startTime)/(1000*1000*1000) + " seconds");
	}    
	
		
}
