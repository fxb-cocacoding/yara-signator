package iterative_improvement;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.Config;
import main.NextGenConfig;
import main.RankingConfig;
import main.Versioning;
import main.WildcardConfig;
import statistics.yara_results.YaraRuleChecker;
import statistics.yara_results.YaraStatsCSV;

public class IterativeImprovementSystemFacade {
	
	private static final Logger logger = LoggerFactory.getLogger(IterativeImprovementSystemFacade.class);
	
	public IterativeImprovementSystemFacade(DateTimeFormatter dtf, LocalDateTime now, Config config, String dateFolder, String cmd_yarac, String cmd_yara) {
		this.config = config;
		this.dateFolder = dateFolder;
		this.dtf = dtf;
		this.now = now;
		this.cmd_yarac = cmd_yarac;
		this.cmd_yara = cmd_yara;
		this.ng = new IterativeImprovementUtils(config, cmd_yarac, cmd_yara, dateFolder);
	}
	
	private Config config;
	private String dateFolder;
	private List<String> fixTheseRules;
	private DateTimeFormatter dtf;
	private LocalDateTime now;
	private String cmd_yarac;
	private String cmd_yara;
	private IterativeImprovementUtils ng;
	
	public void ngAction() {
		IterativeImprovementSystem ngSystem = new IterativeImprovementSystem();
		int barrier_limit = config.ng_recursion_limit;
		for(int i=1; i<=barrier_limit; i++) {
			for(NextGenConfig ngConfig: config.getNextGenConfig()) {
				for(int j=1; j<=ngConfig.rounds; j++) {
					fixTheseRules = ng.findBrokenRules();
					String stats = "";
					
					logger.info("config.ng_recursion_limit: " + i + "/" + barrier_limit +
							" - #families to fix: " + fixTheseRules.size() + " - " + " - operator: " + ngConfig.nextGenOperator 
							+ " - ngConfig.rounds: " + j + "/" + ngConfig.rounds
							+ " - breakout_criteria: " + ngConfig.nextGenBreakout);
					try {
						ngSystem.runner(ngConfig.nextGenOperator, fixTheseRules, config, ngConfig, dateFolder, dtf, now);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
	
					if(!config.skipRuleValidation) {
						stats = verify();
						stats = stats + "\n\n";
						logger.info("Stats: " + stats);
					}
	
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
							info.append("    " + c.ranker + " - limit: " + c.limit + "\n");
						}
						info.append("NextGenConfig:\n");
						info.append("SystemStep:    " + i + " / " + barrier_limit + "\n");
						info.append("GeneratorType: " + ngConfig.nextGenOperator + "\n");
						info.append("BreakOut:      " + ngConfig.nextGenBreakout.toString() + "\n");
						info.append("Step:          " + j + " / " + ngConfig.rounds + "\n");
						
						info.append("\n\n\n");
						
						String csv = getCSV();
						FileWriter fw;
						try {
							fw = new FileWriter(config.reportFileName, true);
							fw.write(info.toString() + csv + stats);
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.error(e.getLocalizedMessage());
							logger.error("We were unable to write the following CSV to the file: \n\n" + info.toString() + csv + stats + "\n\n");
						}
					}
				}
			}
		}
		
		/*
		 * This can be removed or implemented as a special option:
		 *
		NextGenRuleCollectionEntity f1 = NextGenDataContainer.INSTANCE.getBestResultByF1();
		NextGenRuleCollectionEntity prec = NextGenDataContainer.INSTANCE.getBestResultByPrecision();
		NextGenRuleCollectionEntity recall = NextGenDataContainer.INSTANCE.getBestResultByRecall();

		writeRules(f1, config.output_path + "/" + dateFolder + "_best_f1");
		writeRules(prec, config.output_path + "/" + dateFolder + "_best_precision");
		writeRules(recall, config.output_path + "/" + dateFolder + "_best_recall");
		
		 *
		 */
	}
	
	/*
	private void writeRules(NextGenRuleCollectionEntity rulesPacket, String output_folder) {
		logger.info("NextGen Finished!");
		logger.info("Reading out the data store:");
		logger.info("storing at: " + output_folder);
		int counter = 1;
		for(Entry<String, List<Ngram>> i: rulesPacket.currentSignatures.entrySet()) {
			new YaraRuleGenerator(i.getKey(), 0, this.config, output_folder, this.dtf,
				this.now, counter, i.getValue().size(), rulesPacket.currentNGConfig.yara_condition, i.getValue())
			.generateRule(i.getKey(), 0, this.config, this.dtf, this.now, i.getValue());
			counter++;
		}
		logger.info("\n\n");

	}
	*/
	
	public String getCSV() {
		StringBuilder sb = new StringBuilder();
		sb.append("family;samplesCount;coveredCount;diff;falsePositiveCount;alternative_counter;alternative_entries_list" + "\n");
		if(ng.alreadyDoneFamilies == null) {
			ng.findBrokenRules();
		}
		for(Entry<String, YaraStatsCSV> e : ng.alreadyDoneFamilies.entrySet()) {
			sb.append(e.getKey() + ";" + e.getValue().toCSV() + "\n");
		}
		sb.append("\n\n");
		return sb.toString();
	}
	
	public String verify() {
		String stats = "";
		YaraRuleChecker yc = new YaraRuleChecker(config, cmd_yarac, cmd_yara);
		try {
			yc.init();
			stats = yc.verifyOutputResults();
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}
		return stats;
	}
	

}