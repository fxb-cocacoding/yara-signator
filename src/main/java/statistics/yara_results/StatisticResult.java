package statistics.yara_results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mongodb.MongoHandler;

public class StatisticResult {
	List<String> falseNegatives = new ArrayList<String>();
	List<String> falsePositives = new ArrayList<String>();
	List<String> truePositives = new ArrayList<String>();
	List<String> trueNegatives = new ArrayList<String>();
	long trueNegativeSize = 0;
	long falseNegativeSize = 0;
	long truePositiveSize = 0;
	long falsePositiveSize = 0;
		
	private Map<String, List<String>> getAdvancedSamplePool(List<String> candidatePool, String malpediaPath) {
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
				samplePool.get(family).add(family + "/" + filename);
			} else {
				List<String> tmpVector = samplePool.get(family);
				tmpVector.add(family + "/" + filename);
				samplePool.put(family, tmpVector);
			}
		}
		return samplePool;
	}
	
	
	
	public void generateSimpleSample(HashMap<String, List<String>> output) {
		
		Map<String, List<String>> samplePoolFromDB = new HashMap<String, List<String>>();
		List<String> familiesInDB = new MongoHandler().getFamilies();
		for(int i=0; i<familiesInDB.size(); i++) {
			String family = familiesInDB.get(i);
			//System.out.println(family);
			family = family.replace(".", "_");
			familiesInDB.set(i, family);
		}
		
		for(String family: familiesInDB) {
			List<String> SamplesPerFamily = new MongoHandler().getSamplesPerFamily(family);
			for(int i=0; i<SamplesPerFamily.size(); i++) {
				String x = SamplesPerFamily.get(i);
				x = family + "/" + x;
				SamplesPerFamily.set(i, x);
			}
			samplePoolFromDB.put(family, SamplesPerFamily);
		}


		for(Entry<String, List<String>> e: samplePoolFromDB.entrySet()) {
			String currentFamily = e.getKey();
			List<String> currentFiles = e.getValue();
			if(!output.containsKey(currentFamily)) {
				System.out.println("family not found in yara output: " + currentFamily);
				continue;
			}
			
			for(String currentFile: currentFiles) {
				
				
				for(Entry<String, List<String>> inner_e: samplePoolFromDB.entrySet()) {
					String fam = inner_e.getKey();
					List<String> sam = e.getValue();
					if(fam.equalsIgnoreCase(currentFamily)) {
						//System.out.println("EQUALS " + fam + "   " + c);
						continue;
					} else {
						for(String i: sam) {
							//trueNegatives.add(familyOutput + " - " + fam + " - " + i + " - " + c);
							trueNegativeSize++;
						}
					}
					
				}
				
				List<String> out = output.get(currentFamily);
				if(out.contains(currentFile)) {
					truePositives.add(currentFile);
				} else {
					falseNegatives.add(currentFile);
				}
				if(!currentFile.contains(currentFamily)){
					falsePositives.add(currentFile);
				}
			}
		}
		int counter = 0;
		for(Entry<String, List<String>> i: samplePoolFromDB.entrySet()) {
			counter += i.getValue().size();
		}
	}
	
	public void generateSimpleFamily() {
		
	}
	
	public void generateAdvancedSample(List<String> candidatePool, String malpediaPath, HashMap<String, List<String>> output) {
		List<String> allSamplesInDatabase = new MongoHandler().getSamples();
		Map<String, List<String>> samplePoolFromMalpedia = getAdvancedSamplePool(candidatePool, malpediaPath);
		
		for(Entry<String, List<String>> e: output.entrySet()) {
			String familyOutput = e.getKey();
			List<String> coveredOutput = e.getValue();
			//coveredOutput.forEach(x -> x = familyOutput + "/" + x);
			List<String> samplesFromMalpedia = null;
			
			if(samplePoolFromMalpedia.containsKey(familyOutput)) {
				samplesFromMalpedia = samplePoolFromMalpedia.get(familyOutput);
			} else {
				continue;
			}
			
			for(String m: samplesFromMalpedia) {
				if(coveredOutput.contains(m)) {
					truePositives.add(familyOutput + " - " + m);
					truePositiveSize++;
				} else {
					falseNegatives.add(familyOutput + " - " + m);
					falseNegativeSize++;
				}
			}
			
			for(String c : coveredOutput) {
				for(Entry<String, List<String>> inner_e: samplePoolFromMalpedia.entrySet()) {
					String fam = inner_e.getKey();
					List<String> sam = e.getValue();
					if(fam.equalsIgnoreCase(familyOutput)) {
						//System.out.println("EQUALS " + fam + "   " + c);
						continue;
					} else {
						for(String i: sam) {
							//trueNegatives.add(familyOutput + " - " + fam + " - " + i + " - " + c);
							trueNegativeSize++;
						}
					}
					
				}
				
				if(!c.contains(familyOutput)) {
					falsePositives.add(familyOutput + " - " + c);
					falsePositiveSize++;
				}
			}
		}
		int counter = 0;
		for(Entry<String, List<String>> i: samplePoolFromMalpedia.entrySet()) {
			counter += i.getValue().size();
		}
	}
	
	public void generateAdvancedFamily() {
		
	}
}
