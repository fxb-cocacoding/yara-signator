package statistics.yara_results;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import main.Config;
import mongodb.MongoHandler;
import utils.FileFinder;


public class ExternalRunner {

	private static Map<String, List<String>> getAdvancedSamplePool(List<String> candidatePool, String malpediaPath) {
		/*
		 * converts all files from Malpedia into family/filename format for a comparison between itself and the YARA output
		 */
		Map<String, List<String>> samplePool = new HashMap<>();
		for(String s: candidatePool) {
			String tmp = s.substring(malpediaPath.length() + 1);
			String family = tmp.substring(0, tmp.indexOf("/")).replace('.', '_');
			String filename = tmp.substring(tmp.lastIndexOf("/") + 1);
			if(!samplePool.containsKey(family)) {
				samplePool.put(family, new ArrayList<>());
				//samplePool.get(family).add(family + "/" + filename);
				samplePool.get(family).add(filename);
			} else {
				List<String> tmpVector = samplePool.get(family);
				//tmpVector.add(family + "/" + filename);
				tmpVector.add(filename);
				samplePool.put(family, tmpVector);
			}
		}
		return samplePool;
	}
	
	public static void main(String[] args) {
		
		
		long startTime = System.nanoTime();
	    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
		LocalDateTime now = LocalDateTime.now();

		Config config = new Config();
		config.addN(4);
		config.addN(5);
		config.addN(6);
		config.addN(7);

		String dateFolder = "2018-10-29_21-20-26_ranking_prototype";
		//String dateFolder = "2018-10-24_15-09-13_good_no_real_ranking";
		//String dateFolder = "2018-10-11_09-31-34_the_first_real_yara_rules_but_shuffled_without_score_bad_results";
		//config.malpedia_path = "/malpedia";
		
		String yarac_path = config.output_path + "/" + dtf.format(now) + "/*/* " + config.output_path + "/" + dtf.format(now) + "/" + "yara-compilement";
		String yara_compilement = config.output_path + "/" + dtf.format(now) + "/" + "yara-compilement";
		
		String cmd_yara = "/usr/bin/yara -r " +  yara_compilement + " " + config.malpedia_path;
		String cmd_yarac = "/usr/bin/yarac " + yarac_path;

		cmd_yarac = "/usr/bin/yarac "  + config.output_path + "/" + dateFolder + "/*/* " + config.output_path + "/" + dateFolder + "/yara-compilement";
		cmd_yara = "/usr/bin/yara -r " + config.output_path + "/" + dateFolder + "/yara-compilement " + config.malpedia_path + " | grep -v '.git' | sort";
		List<String> bigFamilies = new ArrayList<String>();
		List<String> emptyFamilies = new ArrayList<String>();
		
		// API Call
		//new YaraStats().validate(dtf, now, config, bigFamilies, emptyFamilies, cmd_yarac, cmd_yara);
		
		HashMap<String, Set<String>> yaraOutput = new YaraRunner().runYara(config, dtf, now, cmd_yarac, cmd_yara);
		/*
		 * rebuild yaraOutput and wrap it around
		 */
		TreeMap<String, TreeSet<String>> yaraStatBase = new TreeMap<String, TreeSet<String>>(); 
		for(Entry<String, Set<String>> entry: yaraOutput.entrySet()) {
			String rule = entry.getKey();
			Set<String> samples = entry.getValue();
			for(String sample: samples) {
				String tmp = sample.substring(config.malpedia_path.length() + 1);
				String family = tmp.substring(0, tmp.indexOf("/")).replace('.', '_');
				String filename = tmp.substring(tmp.lastIndexOf("/") + 1);
				if(yaraStatBase.containsKey(filename)) {
					TreeSet<String> s = yaraStatBase.get(filename);
					//System.out.println("rule added: " + rule + " to Set: " + s.toString());
					s.add(rule);
					yaraStatBase.put(filename, s);
				} else {
					TreeSet<String> s = new TreeSet<>();
					//System.out.println("new sample, rule added: " + rule + " to Set: " + s.toString());
					s.add(rule);
					yaraStatBase.put(filename, s);
				}
			}
		}
		//System.out.println("SIZE: " + yaraStatBase.size());
		List<String> malpediaFiles = null;
		try {
			malpediaFiles = new FileFinder().getMalpediaFiles(config.malpedia_path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(malpediaFiles.size());
		samplesFromDB(malpediaFiles, config, yaraStatBase);
		familiesFromDB(malpediaFiles, config, yaraStatBase);
		samplesFromMalpedia(malpediaFiles, config, yaraStatBase);
		
		/*
		 * yaraStatBase:
		 * filename: [ruleA, ruleB, ruleC, ...]
		 * 
		 */
		
		/*
		for(Entry<String, TreeSet<String>> entry : yaraStatBase.entrySet()) {
			String sample = entry.getKey();
			System.out.println(sample + ": " + entry.getValue());
		}
		*/
		
		/*
		try {
			List<String> allMalpediaFiles = new FileFinder().getMalpediaFiles(config.malpedia_path);
			Collections.sort(allMalpediaFiles);
			for(String s: allMalpediaFiles) {
				System.out.println(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		System.out.println("[INFO] Shutting down.");
		long endTime = System.nanoTime();
		System.out.println("Took " + (endTime - startTime)/(1000*1000*1000) + " seconds"); 

	}
	
	public class Stats {
		public long falsePositives = 0;
		public long falseNegatives = 0;
		public long truePositives = 0;
		public long trueNegatives = 0;
	}
	
	
	
	private static void samplesFromDB(List<String> candidatePool, Config config, TreeMap<String, TreeSet<String>> yaraStatBase) {
		Map<String, List<String>> samplePoolFromMalpedia = getAdvancedSamplePool(candidatePool, config.malpedia_path);
		
		long falsePositives = 0;
		long falseNegatives = 0;
		long truePositives = 0;
		long trueNegatives = 0;

		int sampleSize = 0;
		Map<String, List<String>> samplePoolFromDB = new HashMap<String, List<String>>();
		List<String> coveredFamilies = new MongoHandler().getFamilies();
		//System.out.println(coveredFamilies.size());
		for(int i=0; i<coveredFamilies.size(); i++) {
			String family = coveredFamilies.get(i);
			family = family.replace(".", "_");
			coveredFamilies.set(i, family);
		}
		
		for(String family: coveredFamilies) {
			List<String> SamplesPerFamily = new MongoHandler().getSamplesPerFamily(family.replaceFirst("_", "."));
			//System.out.println("family: " + family + " contains: " + SamplesPerFamily.size());
			for(int i=0; i<SamplesPerFamily.size(); i++) {
				String x = SamplesPerFamily.get(i);
				//x = family + "/" + x;
				SamplesPerFamily.set(i, x);
				sampleSize++;
			}
			samplePoolFromDB.put(family, SamplesPerFamily);
		}
		
		//System.out.println(samplePoolFromDB);
		//System.out.println("size of the sample pool in samples from db: " + samplePoolFromDB.size() + " with samples: " + sampleSize);
		
		/*
		 * samples from DB are correct. family -> [filename, filename, ...]
		 */
		HashMap<String, List<String>> familiesWithSamplesNotInMalpediaButInSMDA = new HashMap<>();
		
		/*
		for(Entry<String, List<String>> entry: samplePoolFromMalpedia.entrySet()) {
			if(entry.getValue() == null || entry.getValue().isEmpty()) {
				System.out.println("family: " + entry.getKey() + " has disappeared from malpedia!");
			} else {
				for(String s: entry.getValue()) {
					if(entry.getKey().contains("unidentified")) System.out.println("family: " + entry.getKey() + " - sample: " + s + " is disappeared!");
				}
			}
		}
		*/
		
		for(Entry<String, List<String>> entry : samplePoolFromDB.entrySet()) {
			String family = entry.getKey();
			
			//if(family.contains("unidentified")) System.out.println(family);
			
			for(String sample: entry.getValue()) {
				boolean hasTP = false;
				if(yaraStatBase.containsKey(sample)) {
					//if(family.contains("unidentified")) System.out.println("family: " + family + "found in yarabase");
					if(yaraStatBase.get(sample).contains(family)) {
						//hasTP = true;
						//truePositives++;
						if(samplePoolFromMalpedia.containsKey(family)) {
							hasTP = true;
							truePositives++;
							System.out.println(family + " [TP] covers: " + yaraStatBase.get(sample));
						}
					}
					if(samplePoolFromMalpedia.containsKey(family)) {
						if(hasTP) {
							falsePositives = falsePositives + yaraStatBase.get(sample).size() - 1;
							if(falsePositives + yaraStatBase.get(sample).size() - 1 != falsePositives)
								System.out.println(family + " [FP] covers: " +  yaraStatBase.get(sample));
						} else {
							falsePositives = falsePositives + yaraStatBase.get(sample).size();
							System.out.println(family  + " [FP] covers: " +  yaraStatBase.get(sample));
						}
						trueNegatives += entry.getValue().size() - yaraStatBase.get(sample).size();
					}
				} else {
					//System.out.println("family: " + family + " - sample: " + sample + " might not included in malpedia?");
					if(samplePoolFromMalpedia.containsKey(family)) {
						//System.out.println("Family in malpedia found: " + family);
						
						if(samplePoolFromMalpedia.get(family).contains(sample)) {
							//System.out.println("sample: " + sample + " found in malpedia!");
							falseNegatives++;
							System.out.println(family + " [FN] covers: " + yaraStatBase.get(sample));
						} else {
							//System.out.println("sample: " + sample + " NOT found in malpedia!");
							
							if(familiesWithSamplesNotInMalpediaButInSMDA.containsKey(family)) {
								List<String> tmp = familiesWithSamplesNotInMalpediaButInSMDA.get(family);
								tmp.add(sample);
								familiesWithSamplesNotInMalpediaButInSMDA.put(family, tmp);
							} else {
								List<String> tmp = new ArrayList<>();
								tmp.add(sample);
								familiesWithSamplesNotInMalpediaButInSMDA.put(family, tmp);
							}
						}
					} else {
						//System.out.println("Family NOT in malpedia found: " + family);
						familiesWithSamplesNotInMalpediaButInSMDA.put(family, null);
						//falseNegatives++;
					}
					/*
					 * Maybe the YARA base does not contain the entry in YARA because the malware was not in malpedia in the first place or with another name.
					 */
					
					//System.out.println("not found: " + "sample: " + sample + "  - " + yaraStatBase.get(sample));
					//falseNegatives++;
					//System.out.println("FN: familiy - " + family + " - " + sample);
				}
				
			}
		}
		
		for(Entry<String, List<String>> entry: familiesWithSamplesNotInMalpediaButInSMDA.entrySet()) {
			if(entry.getValue() == null || entry.getValue().isEmpty()) {
				//System.out.println("family: " + entry.getKey() + " has disappeared from malpedia!");
			} else {
				for(String s: entry.getValue()) {
					//System.out.println("family: " + entry.getKey() + " - sample: " + s + " is disappeared!");
				}
			}
		}
		
		double precision = (double)truePositives/((double)truePositives + (double)falsePositives);
		double recall = (double)truePositives/((double)truePositives + (double)falseNegatives);
		double f1_score = 2 * ( (precision * recall)/(precision + recall) );
		System.out.println("samples" + " & " + falsePositives + " & " + falseNegatives + " & " + truePositives + " & " + trueNegatives + " & " + precision + " & " + recall + " & " + f1_score + "\\\\");
		//System.out.println(yaraStatBase);
	}
	
	
	
	
	private static void familiesFromDB(List<String> candidatePool, Config config, TreeMap<String, TreeSet<String>> yaraStatBase) {
		Map<String, List<String>> samplePoolFromMalpedia = getAdvancedSamplePool(candidatePool, config.malpedia_path);
		
		long falsePositives = 0;
		long falseNegatives = 0;
		long truePositives = 0;
		long trueNegatives = 0;
		
		int samplePoolSize = 0;
		Map<String, List<String>> samplePoolFromDB = new HashMap<String, List<String>>();
		List<String> coveredFamilies = new MongoHandler().getFamilies();
		for(int i=0; i<coveredFamilies.size(); i++) {
			String family = coveredFamilies.get(i);
			family = family.replace(".", "_");
			coveredFamilies.set(i, family);
		}
		
		for(String family: coveredFamilies) {
			List<String> SamplesPerFamily = new MongoHandler().getSamplesPerFamily(family.replaceFirst("_", "."));
			for(int i=0; i<SamplesPerFamily.size(); i++) {
				String x = SamplesPerFamily.get(i);
				//x = family + "/" + x;
				SamplesPerFamily.set(i, x);
				samplePoolSize++;
			}
			samplePoolFromDB.put(family, SamplesPerFamily);
		}
		
		//System.out.println(samplePoolSize);
		//System.out.println(samplePoolFromDB.size());
		
		/*
		 * samples from DB are correct. family -> [filename, filename, ...]
		 */
		for(Entry<String, List<String>> entrySamplePool : samplePoolFromDB.entrySet()) {
			int fam_TPs = 0;
			
			Set<String> fam_FPs = new HashSet<String>();
			String family = entrySamplePool.getKey();
			
			int testing = 0;
			int testing1 = 0;
			
			for(String sample: entrySamplePool.getValue()) {
				if(yaraStatBase.containsKey(sample)) {
					testing++;
					for(String rule: yaraStatBase.get(sample)) {
						//System.out.println("family: " + family + " - rule: " + rule);
						if(family.equals(rule)) {
							fam_TPs++;
						} else {
							fam_FPs.add(rule);
						}
					}
				} else {
					testing1++;
					//System.out.println("family: " + family + " not found in yara output");
					/*
					 * Maybe the YARA base does not contain the entry in YARA because the malware was not in malpedia in the first place or with another name.
					 *
					 * i.e. win.unidentified_008, 5, 2 etc
					 */
				}
			}
			if(testing + testing1 == entrySamplePool.getValue().size()) {
				if(samplePoolFromMalpedia.containsKey(family)) {
					if(testing1 == 0) {
						truePositives++;
						//System.out.println("TP detected: " + family + " covers: " );
						//System.out.println("family: " + family + "(" + testing + "|" + testing1 + ")");
					} else {
						falseNegatives++;
						//System.out.println("family: " + family + "(" + testing + "|" + testing1 + ")");
					}
				} else {
					/*
					System.out.println("FAMILY NOT IN MALPEDIA: " + family );
					System.out.println("family: " + family + "(" + testing + "|" + testing1 + ")");
					if(testing1==0) {
						System.out.println(entrySamplePool.getValue().toString());
						if(testing==1) {
							System.out.println(yaraStatBase.get(entrySamplePool.getValue().get(0)));
						} else {
							System.out.println("MORE SAMPLES INVALID");
						}
					}
					*/
				}
			} else {
				System.out.println("THIS SECTION SHOULD NOT BE REACHED - YOUR DATA MAY BE CORRUPT");
			}
			
			/*
			if(fam_FPs.size() == entrySamplePool.getValue().size()) {
				truePositives++;
				//System.out.println("TRUE POSITIVES " + "family: "  + family + " size:  " + entrySamplePool.getValue().size()  + "  -  FPs: " + fam_FPs.toString());
			} else {
				//System.out.println("FALSE NEGATIVES " + "family: "  + family + " size:  " + entrySamplePool.getValue().size()  + "  -  FPs: " + fam_FPs.toString());
				falseNegatives++;
			}
			*/
			falsePositives = falsePositives + fam_FPs.size();
			trueNegatives = trueNegatives + samplePoolFromDB.size() - 1 - fam_FPs.size();
		}
		double precision = (double)truePositives/((double)truePositives + (double)falsePositives);
		double recall = (double)truePositives/((double)truePositives + (double)falseNegatives);
		double f1_score = 2 * ( (precision * recall)/(precision + recall) );
		System.out.println("families" + " & " + falsePositives + " & " + falseNegatives + " & " + truePositives + " & " + trueNegatives + " & " + precision + " & " + recall + " & " + f1_score + "\\\\");
	}
	
	
	
	
	
	private static void samplesFromMalpedia(List<String> candidatePool, Config config, TreeMap<String, TreeSet<String>> yaraStatBase) {
		long falsePositives = 0;
		long falseNegatives = 0;
		long truePositives = 0;
		long trueNegatives = 0;

		Map<String, List<String>> samplePoolFromMalpedia = getAdvancedSamplePool(candidatePool, config.malpedia_path);
		System.out.println("Malpedia size: "  + samplePoolFromMalpedia.size());
		List<String> coveredFamilies = new MongoHandler().getFamilies();
		
		for(int i=0; i<coveredFamilies.size(); i++) {
			String fam = coveredFamilies.get(i);
			fam = fam.replace(".", "_");
			coveredFamilies.remove(i);
			coveredFamilies.add(i, fam);
		}
		
		//System.out.println(coveredFamilies);
		//System.out.println(samplePoolFromMalpedia);
		//System.out.println(samplePoolFromMalpedia.size());
		
		/*
		 * samples from DB are correct. family -> [filename, filename, ...]
		 */
		int allSamples = 0;
		for(Entry<String, List<String>> entry : samplePoolFromMalpedia.entrySet()) {
			String family = entry.getKey();
			family = family.replace(".", "_");
			System.out.println("family: " + family + " - size: " + entry.getValue().size());
			allSamples += entry.getValue().size();
			for(String sample: entry.getValue()) {
				System.out.println(family + ":" +sample);
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
		System.out.println("all samples: " + allSamples);
		double precision = (double)truePositives/((double)truePositives + (double)falsePositives);
		double recall = (double)truePositives/((double)truePositives + (double)falseNegatives);
		double f1_score = 2 * ( (precision * recall)/(precision + recall) );
		System.out.println("Malpedia" + " & " + falsePositives + " & " + falseNegatives + " & " + truePositives + " & " + trueNegatives + " & " + precision + " & " + recall + " & " + f1_score + "\\\\");
		//System.out.println(yaraStatBase);

	}
	private static void familiesFromMalpedia() {
		/*
		 * OUT OF SCOPE ?
		 */
	}

}
