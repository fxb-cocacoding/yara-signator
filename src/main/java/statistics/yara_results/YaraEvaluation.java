package statistics.yara_results;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import main.Config;
import postgres.PostgresRequestUtils;

public class YaraEvaluation {
	
	public Map<String, List<String>> getAdvancedSamplePool(List<String> candidatePool, String malpediaPath) {
		/*
		 * converts all files from Malpedia into family/filename format for a comparison between itself and the YARA output
		 */
		Map<String, List<String>> samplePool = new HashMap<>();
		for(String s: candidatePool) {
			String tmp = s.substring(malpediaPath.length());
			
			String family = "";
			
			if(-1 != tmp.indexOf("/")) {
				family = tmp.substring(0, tmp.indexOf("/"));
			} else {
				//TODO: Throw a new Exception type
				family = tmp;
				System.out.println("family name not found in string!");
			}
			
			String filename = tmp.substring(tmp.lastIndexOf("/") + 1);
			if(!samplePool.containsKey(family)) {
				samplePool.put(family, new ArrayList<>());
				samplePool.get(family).add(filename);
			} else {
				List<String> tmpVector = samplePool.get(family);
				tmpVector.add(filename);
				samplePool.put(family, tmpVector);
			}
		}
		return samplePool;
	}

	public Map<String, YaraStatsCSV> getFittedRules(List<String> familiesInDatabase, HashMap<String, List<String>> samplesForfamilies, HashMap<String, Set<String>> yaraOutput) {

		Map<String, YaraStatsCSV> ret = new HashMap<>();
		
		for(String family : familiesInDatabase) {
			List<String> samplesfromDB = samplesForfamilies.get(family);
			String family_yara_notation = family.replace('.', '_');
			if(yaraOutput.containsKey(family_yara_notation)) {
				int samplesOfTheFamily = samplesfromDB.size();
				int counter = 0;
				int fp_counter = 0;
				int alternative_counter = 0;
				List<String> alternative_entries = new LinkedList<>();
				
				/*
				 * The number of samples we want to cover is in the database present.
				 * We filter now all in yara output detected, correctly covered samples and count them
				 */
				
				List<String> fromYara = new ArrayList<>(yaraOutput.get(family_yara_notation));
				for(int i=0;i<fromYara.size();i++) {
					String sampleFromYara = fromYara.get(i).substring(fromYara.get(i).lastIndexOf('/')+1, fromYara.get(i).length());
					if(fromYara.get(i).contains(family) && samplesfromDB.contains(sampleFromYara)) {
						counter++;
					} else if(!fromYara.get(i).contains(family)) {
						fp_counter++;
					} else {
						/*
						 * samples_from_db do not contain the sample from yara:
						 */
						alternative_counter++;
						alternative_entries.add(fromYara.get(i));
					}
				}
				YaraStatsCSV fs = new YaraStatsCSV(samplesOfTheFamily, counter, samplesOfTheFamily - counter, fp_counter, alternative_counter, alternative_entries);
				ret.put(family, fs);
			} else {
				/*
				 * The family we want to cover is in the database, but not in the yara Output
				 */
				int samplesOfTheFamily = samplesfromDB.size();
				YaraStatsCSV fs = new YaraStatsCSV(samplesOfTheFamily,-1,-1,-1,-1, null);
				ret.put(family, fs);
			}
		}
		return ret;
	}
	
	public String samplesFromMalpediaTest(List<String> candidatePool, Config config, TreeMap<String, TreeSet<String>> yaraStatBase) throws SQLException {
		long falsePositives = 0;
		long falseNegatives = 0;
		long truePositives = 0;
		long trueNegatives = 0;

		Map<String, List<String>> samplePoolFromMalpedia = getAdvancedSamplePool(candidatePool, config.malpedia_path);
		System.out.println("Families found in Malpedia: "  + samplePoolFromMalpedia.size());
		List<String> coveredFamilies = new PostgresRequestUtils().getFamilies();
		
		for(int i=0; i<coveredFamilies.size(); i++) {
			String fam = coveredFamilies.get(i);
			//System.out.println("from db: " + fam);
			fam = fam.replace(".", "_");
			//System.out.println("from db: " + fam);
			coveredFamilies.remove(i);
			coveredFamilies.add(i, fam);
		}
		
		for(Entry<String, List<String>> entry : samplePoolFromMalpedia.entrySet()) {
			String family = entry.getKey();
			family = family.replace(".", "_");
			for(String sample: entry.getValue()) {
				boolean hasTP = false;
				if(yaraStatBase.containsKey(sample)) {
					for(String rule: yaraStatBase.get(sample)) {
						if(coveredFamilies.contains(family)) {
							if(rule.equalsIgnoreCase(family)) {
								truePositives++;
							} else {
								falsePositives++;
							}
						} else {
							falsePositives++;
						}
					}
				} else {
					if(coveredFamilies.contains(family)) {
						if(!(sample.contains("dump") || sample.contains("unpacked"))) {
							trueNegatives++;
						} else {
							falseNegatives++;
						}
					} else {
						trueNegatives++;
					}
				}
				
			}
		}
		double precision = (double)truePositives/((double)truePositives + (double)falsePositives);
		double recall = (double)truePositives/((double)truePositives + (double)falseNegatives);
		double f1_score = 2 * ( (precision * recall)/(precision + recall) );
		return "Malpedia" + " FP: " + falsePositives
				+ " FN: " + falseNegatives + " TP: " + truePositives + " TN " 
				+ trueNegatives + " Precision: " + precision + " Recall: " + recall + " F-Score: " + f1_score;
	}


}
