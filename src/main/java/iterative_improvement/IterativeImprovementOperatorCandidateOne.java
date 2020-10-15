package iterative_improvement;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import main.Config;
import main.NextGenConfig;
import postgres.PostgresRequestUtils;
import statistics.malpedia_eval.MalpediaEval;
import statistics.malpedia_eval.ReadMalpediaEval;
import utils.SystemExec;
import yara_generation.NgramCreator;

public class IterativeImprovementOperatorCandidateOne extends IterativeImprovementOperator {

	private static final Logger logger = LoggerFactory.getLogger(IterativeImprovementOperatorCandidateOne.class);
	
	private int countOfStringsPerSample;
	
	public IterativeImprovementOperatorCandidateOne(int countOfStringsPerSample) {
		this.countOfStringsPerSample = countOfStringsPerSample;
	}

	@Override
	public List<String> operate(List<String> fixTheseRules, Config config, NextGenConfig currentNGConfig, String dateFolder, DateTimeFormatter dtf, LocalDateTime now) {
		//List<String> rulesToFix = new ArrayList<String>();
		Map<String, Integer> allFamiliesWithIDs = null;
		try {
			allFamiliesWithIDs = new PostgresRequestUtils().getFamiliesWithIDs();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Map<String, List<Ngram>> ngramsForFamilies = new HashMap<String, List<Ngram>>();
		for(String family: fixTheseRules) {
			int family_id = allFamiliesWithIDs.get(family);
			
			try {
				NgramCreator nyg = new NgramCreator();
				List<Ngram> ngrams = nyg.getNgramsForFamily_NextGen_CandidateOne(family_id, config, currentNGConfig);
				nyg.cleanDisasm();
				ngramsForFamilies.put(family, ngrams);
				//NextGenDataContainer.INSTANCE.currentSignatures.put(family, ngrams);
			} catch (SQLException | DecoderException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		new PostgresRequestUtils().generateYaraRuleNG(config, dtf, now, dateFolder, fixTheseRules, currentNGConfig.yara_condition, ngramsForFamilies);
		
		
		/*
		 * Add the result to the datastore:
		 */
		try {
			String[] args = {"/usr/bin/python3", config.malpediaEvalScript, config.yaraBinary, config.yaracBinary, config.output_path + "/" + dateFolder, config.malpedia_path, config.malpediaEvalScriptOutput};
			logger.info(Arrays.toString(args));
			new SystemExec().executeCommand(args);
			MalpediaEval malpediaEval = new ReadMalpediaEval().getFileContent(config.malpediaEvalScriptOutput);
			IterativeImprovementRuleCollectionEntity n = new IterativeImprovementRuleCollectionEntity();
			n.f1 = Double.parseDouble(malpediaEval.statistics.f1_score);
			n.precision = Double.parseDouble(malpediaEval.statistics.f1_precision);
			n.recall = Double.parseDouble(malpediaEval.statistics.f1_recall);
			n.currentSignatures = ngramsForFamilies;
			n.families = fixTheseRules;
			n.currentNGConfig = currentNGConfig;
			n.config = config;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return null;
	}

}
