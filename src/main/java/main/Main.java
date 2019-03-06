package main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.ObjectInputStream.GetField;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.SizeLimitExceededException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import controller.Controller;
import converters.ConverterFactory;
import converters.ngrams.Ngram;
import filter.FilterFactory;
import json.Generator;
import mongodb.MongoConnection;
import mongodb.MongoDataAdapter;
import mongodb.MongoHandler;
import postgres.HandleStructures;
import postgres.PostgresConnection;
import postgres.PostgresInsertNgrams;
import postgres.PostgresRequestUtils;
import ranking_system.RankingSystem;
import ranking_system.RankingSystemFacade;
import ranking_system.db.DBCreator;
import smtx_handler.Instruction;
import smtx_handler.SMDA;
import statistics.yara_results.YaraStats;
import yara.Rule;
import yara.RuleCondition;
import yara.RuleMeta;
import yara.RuleStrings;
import yara.YaraRule;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] argv) {
		
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
		 * 4. filter them
		 * 5. rank them
		 * 6. create best YARA rules
		 * 7. validate the rules
		 * 8. profit!
		 * 
		 */
		
		final String configFileName = System.getProperty("user.home") + "/.yarasignator.conf";
		long startTime = System.nanoTime();
	    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	    
		LocalDateTime now = LocalDateTime.now();
		Config config = new Utils().getConfig(configFileName);
		
		try {
			new PostgresRequestUtils().setup_db_handler(config);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File[] allSmdaFiles = new File(config.smda_path).listFiles();
		
		/*
		 * debug only, to reduce the input set so we finish faster:
		 *
		Arrays.sort(allSmdaFiles, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
		ArrayList<File> files = new ArrayList<File>();
		for(int i=0;i<250; i++) {
			files.add(allSmdaFiles[i]);
		}
		allSmdaFiles = new File[files.size()];
		allSmdaFiles = files.toArray(allSmdaFiles);
		/*
		 * real program
		 */

		
		logger.info("Found " + allSmdaFiles.length + " files!");
		
		if(!config.skipSMDAInsertions) {
			new PostgresRequestUtils().firstInsertions(config, allSmdaFiles, config.minInstructions, true);
		}
		
		if(!config.skipUniqueNgramTableCreation) {
			new PostgresRequestUtils().generateUniquePartitionedTables(config);
		}
		if(!config.skipYaraRuleGeneration) {
			new PostgresRequestUtils().generateYaraRule(dtf, now, config);
		}
		
		if(!config.skipRuleValidation) {
			String yarac_path = config.output_path + "/" + dtf.format(now) + "/*/* " + config.output_path + "/" + dtf.format(now) + "/" + "yara-compilement";
			String yara_compilement = config.output_path + "/" + dtf.format(now) + "/" + "yara-compilement";
			String yaraBinary = config.yaraBinary;
			String yaracBinary = config.yaracBinary;
			
			String cmd_yara = yaraBinary + " -r " +  yara_compilement + " " + config.malpedia_path;
			String cmd_yarac = yaracBinary + " " + yarac_path;

			//UNDER CONSTRUCTION
			//new Main().validateYaraRules(dtf, now, config, cmd_yarac, cmd_yara);
		}
		
		logger.info("Shutting down.");
		long endTime = System.nanoTime();
		logger.info("Took " + (endTime - startTime)/(1000*1000*1000) + " seconds");
	}    
}
