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

public class IterativeImprovementOperatorParseMalpediaEval extends IterativeImprovementOperator {

	private static final Logger logger = LoggerFactory.getLogger(IterativeImprovementOperatorParseMalpediaEval.class);
	
	private int countOfStringsPerSample;
	private Map<String, List<Ngram>> ngramsForFamilies;
	
	public IterativeImprovementOperatorParseMalpediaEval(int countOfStringsPerSample) throws Exception {
		this.countOfStringsPerSample = countOfStringsPerSample;
		//this.ngramsForFamilies = NextGenDataContainer.INSTANCE.currentSignatures;
		this.ngramsForFamilies = new HashMap<String, List<Ngram>>();
		if(this.ngramsForFamilies == null) {
			throw new Exception("Error, NextGenDataContainer is null but should dontain elements.\n"
					+ "Never call this operator without a prior generation operator!");
		}
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
		
		logger.info("Current config: \n"  + currentNGConfig.toString());
		
		String[] args = {"/usr/bin/python3", config.malpediaEvalScript, config.yaraBinary, config.yaracBinary, config.output_path + "/" + dateFolder, config.malpedia_path, config.malpediaEvalScriptOutput};
		System.out.println(Arrays.toString(args));
		MalpediaEval malpediaEval = null;
		try {
			malpediaEval = new ReadMalpediaEval().getFileContent(config.malpediaEvalScriptOutput);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		try {
			new SystemExec().executeCommand(args);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(String family: fixTheseRules) {
			int family_id = allFamiliesWithIDs.get(family);
			
			try {
				
				NgramCreator nyg = new NgramCreator(config.capstone_host, config.capstone_port);
				
				List<Ngram> ngrams = nyg.getNgramsForFamily_NextGen_ParseMalpediaEval_ReduceFalsePositiveStrategy(family_id, config, currentNGConfig);
				
				nyg.cleanDisasm();
				
				ngramsForFamilies.put(family, ngrams);
				
			} catch (SQLException | DecoderException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		new PostgresRequestUtils().generateYaraRuleNG(config, dtf, now, dateFolder, fixTheseRules, currentNGConfig.yara_condition, ngramsForFamilies);
		
		IterativeImprovementRuleCollectionEntity n = new IterativeImprovementRuleCollectionEntity();
		n.f1 = Double.parseDouble(malpediaEval.statistics.f1_score);
		n.precision = Double.parseDouble(malpediaEval.statistics.f1_precision);
		n.recall = Double.parseDouble(malpediaEval.statistics.f1_recall);
		n.currentSignatures = ngramsForFamilies;
		n.families = fixTheseRules;
		n.currentNGConfig = currentNGConfig;
		n.config = config;
		IterativeImprovementDataContainer.INSTANCE.candidates.add(n);
		
		
		return null;
	}

}
