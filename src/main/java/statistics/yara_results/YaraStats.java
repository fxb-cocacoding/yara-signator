package statistics.yara_results;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import main.Config;
import mongodb.MongoHandler;
import utils.FileFinder;

public class YaraStats {
	public HashMap<String, List<String>> getAdvancedFalsePositiveFamiliesFromYaraOutput(HashMap<String, Set<String>> yaraOutput) {
		HashMap<String, List<String>> falsePositives = new HashMap<>();
		
		for(Entry<String, Set<String>> e: yaraOutput.entrySet()) {
			String family = e.getKey();
			List<String> fps = new ArrayList<>();
			if(falsePositives.get(family) == null) {
				falsePositives.put(family, fps);
			}
			
			for(String s: e.getValue()) {
				if(!e.getValue().contains(family)) {
					int beginIndex, endIndex;
					if(s.contains(".git/objects")) continue;
					
					beginIndex = s.lastIndexOf("/") + "/".length();
					String currentSample = s.substring(beginIndex);
					
					beginIndex = s.indexOf("malpedia/") + "malpedia/".length();
					endIndex = s.indexOf("/", beginIndex);
					String current = s.substring(beginIndex, endIndex).replace(".", "_");
					
					current = current + "/" + currentSample;
					
					fps = falsePositives.get(family);
					fps.add(current);
					falsePositives.replace(family, fps);
				}
			}
		}
		
		/*
		for(Entry<String, List<String>> e: falsePositives.entrySet()) {
			String family = e.getKey();
			List<String> fps = e.getValue();
			
			
			Iterator<String> iterator = fps.iterator();
			while(iterator.hasNext()) {
				String s = iterator.next();
				if(s.contains(family)) iterator.remove();
			}
			
			
			//List<String> removeMe = new ArrayList<>();
			//removeMe.add(family);
			//fps.removeAll(removeMe);
						
			int count = fps.size();
			
			if(count > 1) {
				System.out.println(family + " - " + fps.toString() + " - " + fps.size() );
			}
		}
		*/
				
		return falsePositives;
	}
	
	public HashMap<String, List<String>> getAdvancedFalsePositivesFromYaraOutput(HashMap<String, Set<String>> yaraOutput) {
		/*
		 * Returns a map over  >> yara_rule: [sample filenames, ...] <<
		 * where a false positive has  
		 */
		
		HashMap<String, List<String>> falsePositives = new HashMap<>();
		
		for(Entry<String, Set<String>> e: yaraOutput.entrySet()) {
			String family = e.getKey();
			List<String> samples = new ArrayList<>();
			if(falsePositives.get(family) == null) {
				falsePositives.put(family, samples);
			}
			
			for(String s: e.getValue()) {
				//if(!e.getValue().contains(family)) {
					int beginIndex;
					if(s.contains(".git/objects")) continue;
					beginIndex = s.lastIndexOf("/") + "/".length();
					
					String current = s.substring(beginIndex);

					samples = falsePositives.get(family);
					samples.add(current);
					falsePositives.replace(family, samples);
				//}
			}
		}
		
		/*
		for(Entry<String, List<String>> e: falsePositives.entrySet()) {
			String family = e.getKey();
			List<String> fps = e.getValue();
			
			
			int count = fps.size();
			if(count > 1) {
				System.out.println("has " + fps.toString() + " FPs - " + fps.size() + " counted - " + family);
				System.out.println();
			}
			
		}
		*/
		
		return falsePositives;
	}
	
	
	public HashMap<String, List<String>> getSimpleFalsePositiveFamiliesFromYaraOutput(HashMap<String, Set<String>> yaraOutput) {
		HashMap<String, List<String>> falsePositives = new HashMap<>();
		
		
		Set<String> allSamples = new TreeSet<>();
		allSamples.addAll(new MongoHandler().getSamples());
				
		for(Entry<String, Set<String>> e: yaraOutput.entrySet()) {
			String family = e.getKey();
			List<String> fps = new ArrayList<>();
			if(falsePositives.get(family) == null) {
				falsePositives.put(family, fps);
			}
			
			for(String s: e.getValue()) {
				if(!e.getValue().contains(family)) {
					int beginIndex, endIndex;
					if(s.contains(".git/objects")) continue;
					
					/*
					 * Check if the current sample is in database, else skip.
					 */
					beginIndex = s.lastIndexOf("/") + "/".length();
					String currentSample = s.substring(beginIndex);
					if(!allSamples.contains(currentSample)) continue;
					
					beginIndex = s.indexOf("malpedia/") + "malpedia/".length();
					endIndex = s.indexOf("/", beginIndex);
					String current = s.substring(beginIndex, endIndex).replace(".", "_");

					current = current + "/" + currentSample;
					
					fps = falsePositives.get(family);
					fps.add(current);
					falsePositives.replace(family, fps);
				}
			}
		}
		
		for(Entry<String, List<String>> e: falsePositives.entrySet()) {
			String family = e.getKey();
			List<String> fps = e.getValue();
			
			/*
			Iterator<String> iterator = fps.iterator();
			while(iterator.hasNext()) {
				String s = iterator.next();
				if(s.contains(family)) iterator.remove();
			}
			*/
			
			//List<String> removeMe = new ArrayList<>();
			//removeMe.add(family);
			//fps.removeAll(removeMe);
			
			/*
			int count = fps.size();
			if(count > 1) {
				System.out.println(family + " - " + fps.toString() + " - " + fps.size() );
			}
			*/
			
		}
		return falsePositives;
	}

	
	
	public HashMap<String, List<String>> getSimpleFalsePositivesFromYaraOutput(HashMap<String, Set<String>> yaraOutput) {
		/*
		 * Returns a map over  >> yara_rule: [sample filenames, ...] <<
		 * where a false positive has  
		 */
		Set<String> allSamples = new TreeSet<>();
		allSamples.addAll(new MongoHandler().getSamples());
				
		HashMap<String, List<String>> falsePositives = new HashMap<>();
		
		for(Entry<String, Set<String>> e: yaraOutput.entrySet()) {
			String family = e.getKey();
			List<String> samples = new ArrayList<>();
			if(falsePositives.get(family) == null) {
				falsePositives.put(family, samples);
			}
			
			for(String s: e.getValue()) {
				//if(!e.getValue().contains(family)) {
					int beginIndex;
					if(s.contains(".git/objects")) continue;
					if(!allSamples.contains(s)) continue;
					beginIndex = s.lastIndexOf("/") + "/".length();
					
					String current = s.substring(beginIndex);

					samples = falsePositives.get(family);
					samples.add(current);
					falsePositives.replace(family, samples);
				//}
			}
		}
		
		/*
		for(Entry<String, List<String>> e: falsePositives.entrySet()) {
			String family = e.getKey();
			List<String> fps = e.getValue();
			
			int count = fps.size();
			if(count > 1) {
				System.out.println("has " + fps.toString() + " FPs - " + fps.size() + " counted - " + family);
				System.out.println();
			}
		}
		*/
		
		return falsePositives;
	}
	
	
	
	private List<StatisticResult> generateStatisticalData(List<String> candidatePool, String malpediaPath, HashMap<String, List<String>> output) {
		List<StatisticResult> allResults = new LinkedList<>();
		StatisticResult advancedSample = new StatisticResult();
		StatisticResult simpleSample = new StatisticResult();
		advancedSample.generateAdvancedSample(candidatePool, malpediaPath, output);
		simpleSample.generateSimpleSample(output);
		System.out.println(simpleSample.falsePositiveSize);
		System.out.println(simpleSample.falsePositives);
		System.out.println(advancedSample.trueNegativeSize);
		return allResults;
	}
	
	public void validate(final DateTimeFormatter dtf, LocalDateTime now, Config config, String cmd_yarac, String cmd_yara) {
				
		HashMap<String, Set<String>> yaraOutput = new YaraRunner().runYara(config, dtf, now, cmd_yarac, cmd_yara);
		//System.out.println(yaraOutput.toString());
		//System.out.println("\n\nAdvanced + Samples\n");
		//falsePositives = new YaraStats().getAdvancedFalsePositivesFromYaraOutput(yaraOutput);
		System.out.println("\n\nSimple + Families\n");
		HashMap<String, List<String>> simpleOut = new YaraStats().getSimpleFalsePositiveFamiliesFromYaraOutput(yaraOutput);
		System.out.println("\n\nAdvanced + Families\n");
		HashMap<String, List<String>> advancedOut = new YaraStats().getAdvancedFalsePositiveFamiliesFromYaraOutput(yaraOutput);
		//System.out.println("\n\nSimple + Samples\n");
		//falsePositives = new YaraStats().getSimpleFalsePositiveFamiliesFromYaraOutput(yaraOutput);
		try {
			generateStatisticalData(new FileFinder().getMalpediaFiles(config.malpedia_path), config.malpedia_path, advancedOut);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		System.out.println(advancedOut.toString());
		
		int fp_counter = 0;
		for(Entry<String, List<String>> e: advancedOut.entrySet()) {
			String family = e.getKey();
			List<String> fps = e.getValue();
			
			int count = fps.size();
			if(count > 1) {
				System.out.println("has " + fps.toString() + " FPs - " + fps.size() + " counted - " + family);
				fp_counter++;
			}
		}
		
		System.out.println("we need to rebuild " + fp_counter + " yara rules");
		System.out.println("Still open families: empty: " + emptyFamilies.size() + " big: " + bigFamilies.size());
		
		*/
	}

}
