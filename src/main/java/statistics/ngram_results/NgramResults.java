package statistics.ngram_results;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import mongodb.MongoConnection;

public class NgramResults {
	
	/*
	 * Run me with:
	 * 
	 * java -cp target/yara-signator-0.0.1-SNAPSHOT-jar-with-dependencies.jar statistics.ngram_results.NgramResults > ngram_stats.csv
	 * 
	 */
	
	private static DBCursor getCursorOnUniqueNgramCollection() {
		MongoClient mongoClient = MongoConnection.INSTANCE.mongoClient;
		DBCollection collection = mongoClient.getDB("caching_db").getCollection("unique_samples");
		DBCursor cursor = collection.find();
		return cursor;
	}
	
	private static TreeMap<String, TreeMap<Integer, Double>> familyMap_4 = new TreeMap<String, TreeMap<Integer, Double>>();
	private static TreeMap<String, TreeMap<Integer, Double>> familyMap_5 = new TreeMap<String, TreeMap<Integer, Double>>();
	private static TreeMap<String, TreeMap<Integer, Double>> familyMap_6 = new TreeMap<String, TreeMap<Integer, Double>>();
	private static TreeMap<String, TreeMap<Integer, Double>> familyMap_7 = new TreeMap<String, TreeMap<Integer, Double>>();
	private static TreeMap<String, TreeMap<Integer, Double>> familyMap_4_7 = new TreeMap<String, TreeMap<Integer, Double>>();
	
	private static final String outputDir = "thesis/ngram_stats/";
	
	public static void main(String[] args) {
		DBCursor cursor = getCursorOnUniqueNgramCollection();
		int c = 0;
		while(cursor.hasNext()) {
			DBObject next = cursor.next();
			Set<String> keyset = next.keySet();
			/*
			 * Get all values from DB
			 */
			
			String concat = (String) ((DBObject) next.toMap().get("_id")).get("concat");
			List<String> families = (List<String>) next.toMap().get("families");
			List<String> samples = (List<String>) next.toMap().get("samples");
			int ngramsCounted = (int) next.toMap().get("ngramsCounted");
			int n = (int) next.toMap().get("n");
			int families_size = (int) next.toMap().get("families_size");
			int samples_size = (int) next.toMap().get("samples_size");
			int score = 0;
			if(next.toMap().get("score") != null) score = (int) next.toMap().get("score");
			
			/*
			 * Logic happens here
			 */
			switch(n) {
				case 4: worker(familyMap_4, concat, families, samples, ngramsCounted, n, families_size, samples_size, score);
						break;
				case 5: worker(familyMap_5, concat, families, samples, ngramsCounted, n, families_size, samples_size, score);
						break;
				case 6: worker(familyMap_6, concat, families, samples, ngramsCounted, n, families_size, samples_size, score);
						break;
				case 7: worker(familyMap_7, concat, families, samples, ngramsCounted, n, families_size, samples_size, score);
						break;
				
				default: throw new UnsupportedOperationException();
			}
			
			worker(familyMap_4_7, concat, families, samples, ngramsCounted, n, families_size, samples_size, score);
			
			c++;
			if(c % 2000000 == 0) System.out.println((c/163418221.0)*100 + "% done");
		}
		fillWithZeros(familyMap_4);
		fillWithZeros(familyMap_5);
		fillWithZeros(familyMap_6);
		fillWithZeros(familyMap_7);
		fillWithZeros(familyMap_4_7);
		
		TreeMap<String, TreeMap<Integer, Double>> familyMap_4_ratio = ngramAverageRatioConstructor(familyMap_4);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_5_ratio = ngramAverageRatioConstructor(familyMap_5);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_6_ratio = ngramAverageRatioConstructor(familyMap_6);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_7_ratio = ngramAverageRatioConstructor(familyMap_7);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_4_7_ratio = ngramAverageRatioConstructor(familyMap_4_7);

		writeMap(familyMap_4_ratio, outputDir + "ngram_regular_rationized.csv", false);
		writeMap(familyMap_5_ratio, outputDir + "ngram_regular_rationized.csv", true);
		writeMap(familyMap_6_ratio, outputDir + "ngram_regular_rationized.csv", true);
		writeMap(familyMap_7_ratio, outputDir + "ngram_regular_rationized.csv", true);
		writeMap(familyMap_4_7_ratio, outputDir + "ngram_regular_rationized.csv", true);
		
		writeAndFilterOneNgramCandidates(familyMap_4_ratio, outputDir + "ngram_regular_rationized_one_filter.csv", false);
		writeAndFilterOneNgramCandidates(familyMap_5_ratio, outputDir + "ngram_regular_rationized_one_filter.csv", true);
		writeAndFilterOneNgramCandidates(familyMap_6_ratio, outputDir + "ngram_regular_rationized_one_filter.csv", true);
		writeAndFilterOneNgramCandidates(familyMap_7_ratio, outputDir + "ngram_regular_rationized_one_filter.csv", true);
		writeAndFilterOneNgramCandidates(familyMap_4_7_ratio, outputDir + "ngram_regular_rationized_one_filter.csv", true);
		
		writeMap(familyMap_4, outputDir + "ngram_regular.csv", false);
		writeMap(familyMap_5, outputDir + "ngram_regular.csv", true);
		writeMap(familyMap_6, outputDir + "ngram_regular.csv", true);
		writeMap(familyMap_7, outputDir + "ngram_regular.csv", true);
		writeMap(familyMap_4_7, outputDir + "ngram_regular.csv", true);
		
		TreeMap<String, TreeMap<Integer, Double>> familyMap_4_copy = deepCopy(familyMap_4);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_5_copy = deepCopy(familyMap_5);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_6_copy = deepCopy(familyMap_6);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_7_copy = deepCopy(familyMap_7);
		TreeMap<String, TreeMap<Integer, Double>> familyMap_4_7_copy = deepCopy(familyMap_4_7);
		
		TreeMap<Integer, Double> avg_4 = calculateSimpleAverage(familyMap_4_ratio);
		TreeMap<Integer, Double> avg_5 = calculateSimpleAverage(familyMap_5_ratio);
		TreeMap<Integer, Double> avg_6 = calculateSimpleAverage(familyMap_6_ratio);
		TreeMap<Integer, Double> avg_7 = calculateSimpleAverage(familyMap_7_ratio);
		TreeMap<Integer, Double> avg_4_7 = calculateSimpleAverage(familyMap_4_7_ratio);
				
		writeAvg(avg_4, "avg_4", outputDir + "ngram_avg_rationized.csv", false);
		writeAvg(avg_5, "avg_5", outputDir + "ngram_avg_rationized.csv", true);
		writeAvg(avg_6, "avg_6", outputDir + "ngram_avg_rationized.csv", true);
		writeAvg(avg_7, "avg_7", outputDir + "ngram_avg_rationized.csv", true);
		writeAvg(avg_4_7, "avg_4_7", outputDir + "ngram_avg_rationized.csv", true);
		
		TreeMap<Integer, Double> avg_4_normalized = calculateAverageSizeNormalized(familyMap_4_copy);
		TreeMap<Integer, Double> avg_5_normalized = calculateAverageSizeNormalized(familyMap_5_copy);
		TreeMap<Integer, Double> avg_6_normalized = calculateAverageSizeNormalized(familyMap_6_copy);
		TreeMap<Integer, Double> avg_7_normalized = calculateAverageSizeNormalized(familyMap_7_copy);
		TreeMap<Integer, Double> avg_4_7_normalized = calculateAverageSizeNormalized(familyMap_4_7_copy);
		
		writeAvg(avg_4_normalized, "avg_4_normalized", outputDir + "ngram_avg_normalized.csv", false);
		writeAvg(avg_5_normalized, "avg_5_normalized", outputDir + "ngram_avg_normalized.csv", true);
		writeAvg(avg_6_normalized, "avg_6_normalized", outputDir + "ngram_avg_normalized.csv", true);
		writeAvg(avg_7_normalized, "avg_7_normalized", outputDir + "ngram_avg_normalized.csv", true);
		writeAvg(avg_4_7_normalized, "avg_4_7_normalized", outputDir + "ngram_avg_normalized.csv", true);
	}
	
	private static void printAvg(TreeMap<Integer, Double> avg, String name) {
		for(Entry<Integer, Double> entry: avg.entrySet()) {
			System.out.println(name + ";" + entry.getKey() + ";" + entry.getValue());
		}
	}
	
	private static void writeAndFilterOneNgramCandidates(TreeMap<String, TreeMap<Integer, Double>> familyMap, String filename, boolean append) {
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append), "utf-8"));
			
			for(Entry<String, TreeMap<Integer, Double>> a: familyMap.entrySet()) {
				String currentFamily = a.getKey();
				TreeMap<Integer, Double> currentIntMapping = a.getValue();
				if(currentIntMapping.containsKey((int) 1)) {
					double value = currentIntMapping.get((int)1);
					writer.write(currentFamily + ";" + 1 + ";" + value + "\n");
				}
			}
			writer.close();
			
		} catch(IOException e) {
			System.out.println(e);
		}

	}
	
	private static void writeAvg(TreeMap<Integer, Double> avg, String name, String filename, boolean append) {
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append), "utf-8"));

			for(Entry<Integer, Double> entry: avg.entrySet()) {
				writer.write(name + ";" + entry.getKey() + ";" + entry.getValue() + "\n");
			}
			
			writer.close();
		} catch(IOException e) {
			System.out.println(e);
		}
	}
	
	private static void printMap(TreeMap<String, TreeMap<Integer, Double>> familyMap) {
		for(Entry<String, TreeMap<Integer, Double>> a: familyMap.entrySet()) {
			String currentFamily = a.getKey();
			TreeMap<Integer, Double> currentIntMapping = a.getValue();
			
			for(Entry<Integer, Double> i: currentIntMapping.entrySet()) {
				int currentFamilySize = i.getKey();
				double counter = i.getValue();
				System.out.println(currentFamily + ";" + currentFamilySize + ";" + counter);
			}
		}
	}
	
	private static void writeMap(TreeMap<String, TreeMap<Integer, Double>> familyMap, String filename, boolean append) {
		try {
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append), "utf-8"));
			
			for(Entry<String, TreeMap<Integer, Double>> a: familyMap.entrySet()) {
				String currentFamily = a.getKey();
				TreeMap<Integer, Double> currentIntMapping = a.getValue();
				
				for(Entry<Integer, Double> i: currentIntMapping.entrySet()) {
					int currentFamilySize = i.getKey();
					double counter = i.getValue();
					writer.write(currentFamily + ";" + currentFamilySize + ";" + counter + "\n");
				}
			}

			writer.close();
		} catch(IOException e) {
			System.out.println(e);
		}
	}
	
	private static void worker(TreeMap<String, TreeMap<Integer, Double>> familyMap, String concat, List<String> families, List<String> samples, int ngramsCounted, int n, int families_size, int samples_size, int score) {		
		for(String s: families) {
			if(!familyMap.containsKey(s)) {
				TreeMap<Integer, Double> value = new TreeMap<>();
				value.put(families_size, 1.0);
				familyMap.put(s, value);
			} else {
				TreeMap<Integer, Double> currentMap = familyMap.get(s);
				
				if(currentMap.containsKey(families_size)) {
					double counter = currentMap.get(families_size);
					counter++;
					currentMap.put(families_size, counter);
				} else {
					currentMap.put(families_size, 1.0);
				}
			}
		}
	}
	
	private static synchronized void fillWithZeros(TreeMap<String, TreeMap<Integer, Double>> familyMap) {
		/*
		 * Fills all ngram occured counters with the corresponding numbers and the value for them as zero.
		 * We now the max value is 475
		 */
		Map<String, List<Integer>> addThem = new HashMap<>();
		for(Entry<String, TreeMap<Integer, Double>> entry : familyMap.entrySet()) {
			
			TreeMap<Integer, Double> currentMap = entry.getValue();
			int counter = 0;
			int before = 0;
			int test = 0;
			List<Integer> fillMe = new ArrayList();
			
			for(Entry<Integer, Double> currentEntry : currentMap.entrySet()) {
				counter++;
				if(counter != 1) {
					if(!(currentEntry.getKey() -1 == before) && before != 0) {
						for(int i=before+1; i<=currentEntry.getKey(); i++) {
							fillMe.add(i);
							test++;
						}
					}
					before = currentEntry.getKey();
					if(counter==currentMap.size() && currentMap.size() < 475) {
						for(int i = currentEntry.getKey()+1; i<=475; i++) {
							fillMe.add(i);
							test++;
						}
					}
				}
			}
			counter = 0;
			addThem.put(entry.getKey(), fillMe);
		}
		for(Entry<String, TreeMap<Integer, Double>> e: familyMap.entrySet()) {
			TreeMap<Integer, Double> tmp = e.getValue();
			String family = e.getKey();
			List<Integer> values = addThem.get(family);
			for(int i: values) {
				tmp.put(i, 0.0);
			}
			familyMap.put(family, tmp);
		}
		
	}
	
	@Deprecated
	private static synchronized TreeMap<Integer, Double> calculateSimpleAverage(TreeMap<String, TreeMap<Integer, Double>> familyMap) {
		TreeMap<Integer, Double> ret = new TreeMap<>();
		for(Entry<String, TreeMap<Integer, Double>> a: familyMap.entrySet()) {
			String currentFamily = a.getKey();
			TreeMap<Integer, Double> currentIntMapping = a.getValue();
			
			for(Entry<Integer, Double> i: currentIntMapping.entrySet()) {
				int currentFamilyN = i.getKey();
				double counter = i.getValue();
				if(ret.containsKey(currentFamilyN)) {
					double tmp = ret.get(currentFamilyN);
					tmp = tmp + counter;
					ret.put(currentFamilyN, tmp);
				} else {
					ret.put(currentFamilyN, (double) counter);
				}
			}
		}
		for(Entry<Integer, Double> a : ret.entrySet()) {
			double old = a.getValue();
			a.setValue(old/familyMap.size());
		}
		return ret;
	}
	
	private static synchronized TreeMap<Integer, Double> calculateAverageSizeNormalized(TreeMap<String, TreeMap<Integer, Double>> familyMap) {
		TreeMap<Integer, Double> ret = new TreeMap<>();
		HashMap<String, Double> divisors = new HashMap<String, Double>();
		
		for(Entry<String, TreeMap<Integer, Double>> a: familyMap.entrySet()) {
			String currentFamily = a.getKey();
			TreeMap<Integer, Double> currentIntMapping = a.getValue();
			double divisor = 0;
			
			for(Entry<Integer, Double> i: currentIntMapping.entrySet()) {
				int currentFamilyN = i.getKey();
				double counter = i.getValue();
				if(ret.containsKey(currentFamilyN)) {
					double tmp = ret.get(currentFamilyN);
					tmp = tmp + counter;
					ret.put(currentFamilyN, tmp);
				} else {
					ret.put(currentFamilyN, counter);
				}
				divisor = divisor + counter;
			}
			
			divisors.put(currentFamily, divisor);
			currentIntMapping = a.getValue();
			for(Entry<Integer, Double> entry: currentIntMapping.entrySet()) {
				entry.setValue(entry.getValue()/ (double) divisor);
			}
			
		}
		/*
		for(Entry<String, TreeMap<Integer, Double>> a: familyMap.entrySet()) {
			String family = a.getKey();
			TreeMap<Integer, Double> family_map = a.getValue();
			for(Entry<Integer, Double> entry : family_map.entrySet()) {
				double currentValue = entry.getValue();
				double newValue = currentValue/(divisors.get(family));
				entry.setValue(newValue);
			}
		}
		*/
		for(Entry<Integer, Double> a : ret.entrySet()) {
			double old = a.getValue();
			a.setValue(old/familyMap.size());
		}
		return ret;
	}
	
	private static synchronized TreeMap<String, TreeMap<Integer, Double>> ngramAverageRatioConstructor(TreeMap<String, TreeMap<Integer, Double>> familyMap) {
		TreeMap<String, TreeMap<Integer, Double>> familyMapper = deepCopy(familyMap);
		for(Entry<String, TreeMap<Integer, Double>> families: familyMapper.entrySet()) {
			String family = families.getKey();
			TreeMap<Integer, Double> familyValue = families.getValue();
			double divisor = 0;
			for(Entry<Integer, Double> i: familyValue.entrySet()) {
				divisor = divisor + i.getValue();
			}
			familyValue = families.getValue();
			for(Entry<Integer, Double> i: familyValue.entrySet()) {
				double oldValue = i.getValue();
				if(divisor != 0) {
					double newValue = oldValue/divisor;
					i.setValue(newValue);
				}
			}
		}
		return familyMapper;
	}
	
	private static TreeMap<String, TreeMap<Integer, Double>> deepCopy(TreeMap<String, TreeMap<Integer, Double>> in) {
		TreeMap<String, TreeMap<Integer, Double>> ret = new TreeMap<String, TreeMap<Integer,Double>>();
		for(Entry<String, TreeMap<Integer, Double>> entry: in.entrySet()) {
			String family = entry.getKey();
			TreeMap<Integer, Double> value = entry.getValue();
			TreeMap<Integer, Double> ret_map = new TreeMap<Integer, Double>();
			
			for(Entry<Integer, Double> i: value.entrySet()) {
				int ret_key = i.getKey();
				double ret_value = i.getValue();
				ret_map.put(ret_key, ret_value);
			}
			
			ret.put(family, ret_map);
		}
		return ret;
	}
}
