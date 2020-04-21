package iterative_improvement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.Config;
import postgres.PostgresRequestUtils;
import statistics.yara_results.YaraStatsCSV;
import statistics.yara_results.YaraEvaluation;
import statistics.yara_results.YaraBashExecutor;

public class IterativeImprovementUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(IterativeImprovementUtils.class);
	
	private Config config;
	private String cmd_yarac;
	private String cmd_yara;
	private String dateFolder;
	
	private HashMap<String, Set<String>> yaraOutput;
	private List<String> familiesInDatabase;
	private HashMap<String, List<String>> samplesForfamilies;
	public Map<String, YaraStatsCSV> alreadyDoneFamilies = null;
	
	
	public IterativeImprovementUtils(Config config, String cmd_yarac, String cmd_yara, String dateFolder) {
		this.config = config;
		this.cmd_yarac = cmd_yarac;
		this.cmd_yara = cmd_yara;
		this.dateFolder = dateFolder;
	}
	
	
	/*
	 * This method finds rules which have either false positives or do not match the complete family.
	 */
	public List<String> findBrokenRules() {
		List<String> fixTheseRules = new ArrayList<String>();
		this.yaraOutput = new YaraBashExecutor().runYara(config, cmd_yarac, cmd_yara);
		
		try {
			this.familiesInDatabase = new PostgresRequestUtils().getFamilies();
			this.samplesForfamilies = new PostgresRequestUtils().getSamplesForFamilies();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		alreadyDoneFamilies = new YaraEvaluation().getFittedRules(familiesInDatabase, samplesForfamilies, yaraOutput);
		
		
		for(Entry<String, YaraStatsCSV> e : alreadyDoneFamilies.entrySet()) {
			String family = e.getKey();
			YaraStatsCSV rs = e.getValue();
			
			if( (rs.coveredCount != rs.samplesCount) || (rs.falsePositiveCount != 0) ) {
				fixTheseRules.add(family);
			}
		}
		return fixTheseRules;
	}

	
	public List<String> getAbsoluteFilenames(List<String> familiesToFix, String dateFolder) {
		List<String> absoluteFilenames = new ArrayList<>();
		
		for(String s: familiesToFix) {
			String output = config.output_path + "/" + dateFolder + "/" + s + "/yara/tlp_white/" + s + "_auto.yar";
		}
		
		return absoluteFilenames;
	}

}
